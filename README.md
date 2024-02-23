private Map<String, Object> doResponseHandling(String jsonString, Target target, String targetKey) {
    try {
        Map<String, Object> responseMap = objectMapper.readValue(jsonString, Map.class);

        return Optional.of(responseMap)
                .filter(map -> !map.isEmpty())
                .map(map -> {
                    expireTagCache();

                    ResponseOptions responseOptions = target.getResponseOptions();

                    Map<String, Object> builtMap = new HashMap<>();
                    Map<String, List<Object>> builtContentTypesMap = responseMap.entrySet().stream()
                            .parallel()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> ((List<Map<String, Object>>) entry.getValue()).stream()
                                            .flatMap(contentObject -> {
                                                Map<String, Object> targeting = (Map<String, Object>) contentObject.get(ContentConstants.TARGETING);
                                                List<String> funcList = targeting.getOrDefault(ContentConstants.FUNC_KEY, Collections.emptyList());
                                                List<String> targetLocationList = targeting.getOrDefault(ContentConstants.TARGET_LOCATION, Collections.singletonList(ContentConstants.ALL_LOWER));
                                                List<String> segmentList = targeting.getOrDefault(ContentConstants.SEGMENT_KEY, Collections.emptyList());

                                                return ((List<Map<String, Object>>) contentObject.get(ContentConstants.CONTENT)).stream()
                                                        .map(content -> {
                                                            String apiType = cmsRequestBuilder.getApiType(target.getRequestOptions());
                                                            content = cmsParser.parse(apiType, content);
                                                            content.put(ContentConstants.FUNC_KEY, funcList);
                                                            content.put(ContentConstants.TARGET_LOCATION, targetLocationList);
                                                            content.put(ContentConstants.SEGMENT_KEY, segmentList);
                                                            content.put(ContentConstants.CONTENT_TYPE, entry.getKey());
                                                            return hoistAttributes(content);
                                                        });
                                            })
                                            .collect(Collectors.toList())
                            ));

                    if (responseOptions.getDedupe()) {
                        builtContentTypesMap.replaceAll((key, value) -> dedupeContent(value));
                    }

                    builtMap.put(targetKey, builtContentTypesMap);
                    return builtMap;
                })
                .orElse(null);
    } catch (IOException e) {
        LOGGER.error("Error handling response properly", e);
        return null;
    }
}

// Other methods...
