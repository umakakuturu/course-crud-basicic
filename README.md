public ContentWrapper getImageByTargetAsWrappedContent(String selectedTarget, String functionalArea, Long personId) {
    LOGGER.debug("Requesting image ({}) for functional area [{}] and person [{}]",
            selectedTarget, functionalArea, personId);

    try {
        Map<String, Object> contentMap = objectMapper.readValue(doContentFetch(functionalArea, personId, null), new TypeReference<Map<String, Object>>() {});

        if (isNotEmpty(contentMap) && contentMap.containsKey(functionalArea)) {
            Map<String, Object> allMap = (Map<String, Object>) contentMap.get(functionalArea);

            return Optional.ofNullable(allMap.get(ContentConstants.LOGO))
                    .map(logoMap -> ((Map<String, Object>) logoMap).get(ContentConstants.IMAGES))
                    .filter(images -> images instanceof List)
                    .map(images -> ((List<Map<String, Object>>) images).stream()
                            .filter(image -> Optional.ofNullable(image.get(ContentConstants.TARGET_LOCATION))
                                    .map(locations -> ((List<String>) locations).contains(selectedTarget))
                                    .orElse(false))
                            .collect(Collectors.toList()))
                    .filter(logos -> !logos.isEmpty())
                    .map(logos -> getLogoToUse(logos, personId))
                    .map(logoToUse -> {
                        logoToUse.put(ContentConstants.CONTENT_TYPE, ContentConstants.IMAGES);
                        logoToUse = hoistAttributes(logoToUse);
                        String filePath = cmsRequestBuilder.buildCmsFileRequest(ApiEndpointMapImpl.CMS_API_TYPE, (String) logoToUse.get(ContentConstants.PATH_KEY));
                        InputStream inputStream = fetchCmsResponseStream(filePath);
                        return (null != inputStream) ? new ContentWrapperImpl(inputStream, filePath, mimeTypeMapper) : null;
                    })
                    .orElse(null);
        }
    } catch (IOException e) {
        LOGGER.error("Could not retrieve functional area [{}] for person [{}]", functionalArea, personId);
    }

    return null;
}
