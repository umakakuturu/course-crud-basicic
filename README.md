 private Map<String, Map<String, Object>> buildRequestAndHandleResponse(String apiType, String contentId, String functionalArea,
                                                                           Long personId, Long memberId, boolean segmentRequest) {
        Assert.notNull(functionalArea, "functionalArea cannot be null");
        FunctionalArea fa = getFunctionalAreaFromConfig(functionalArea, segmentRequest);

        String cacheKey = buildCmsRequestCacheKey(apiType, contentId, functionalArea, personId, memberId, segmentRequest);
        Map<String, Map<String, Object>> targetMap = getCmsCache().get(cacheKey);
        boolean notReadFromCache = false;
        try {
            if (isEmpty(targetMap) && fa != null) {
                if (targetMap == null) {
                    targetMap = new HashMap<String, Map<String, Object>>();
                }
                targetMap.put(functionalArea, new HashMap<String, Object>());
                Map<String, Target> targets = fa.getTargets();
                if (targets != null) {
                    for (String key : targets.keySet()) {
                        Target t = targets.get(key);

                        String cmsRequest = createCmsRequest(apiType, contentId, personId, memberId, t);
                        LOGGER.trace("Making CMS request for: {}", cmsRequest);

                        InputStream inputStream = null;
                        try {
                            inputStream = FileUtils.getInputStream(cmsRequest);
                            if (inputStream != null) {
                                String cmsResponse = IOUtils.toString(inputStream);
                                Map<String, Object> responseMap = null;
                                if (segmentRequest) {
                                    try {
                                        responseMap = objectMapper.readValue(cmsResponse, Map.class);
                                    } catch (IOException e) {
                                        LOGGER.error("Could not create Map from {}", cmsResponse);
                                    }
                                } else {
                                    responseMap = doResponseHandling(cmsResponse, t, key);
                                }

                                if (responseMap != null && !responseMap.isEmpty()) {
                                    targetMap.get(functionalArea).putAll(responseMap);
                                    notReadFromCache = true;
                                }
                            }
                        } catch (IOException e) {
                            LOGGER.error("Error parsing request for: {}", cmsRequest);
                        } finally {
                            IOUtils.closeQuietly(inputStream);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Could not handle CMS request", e);
            targetMap = null;
        } finally {
            if (notReadFromCache) {
                LOGGER.debug("CacheKey:[{}] for put FunctionalArea into cmsCache while fetching content: [{}]", cacheKey, contentId);
                getCmsCache().put(cacheKey, targetMap);
            }
        }

        return targetMap;
    }
	
	=============================
	private Map<String, Map<String, Object>> buildRequestAndHandleResponse(String apiType, String contentId, String functionalAreaType,
                                                                           Person person, Membership membership, boolean segmentRequest) {
        Assert.notNull(functionalAreaType, FUNCTIONAL_AREA_CANNOT_NULL);
        doProcess(MapperRequest.builder().functionalAreaType(FunctionalAreaType.fromType(functionalAreaType)).
                includeSegments(segmentRequest).build())
                .map(FunctionalArea::getTargets).stream().findAny().ifPresent(targets -> {
                    //used parallel stream procesing targets are concurrently appropriate
                    targets.entrySet().stream().parallel()
                            .forEach(entry-> {
                                String key = entry.getKey();
                                Target t = entry.getValue();
                                String cmsRequest = createCmsRequest(apiType, contentId, person, membership, t);

                                String cmsResponse =
                            });

                });}
