Set<String> sorted = memberships.getResponse().stream()
        .sorted(MembershipImpl.getComparator())
        .collect(Collectors.groupingBy(this::getPlanKey, LinkedHashMap::new, Collectors.toList()))
        .values().stream()
        .flatMap(planMembershipList ->
                getFunctionalAreaFromConfig(ContentConstants.SEGMENT_FUNC, true).getTargets()
                        .values().stream()
                        .filter(target -> ContentConstants.SEGMENT_TARGET.equals(target.getName()))
                        .findFirst()
                        .map(target -> {
                            Person person = getPerson(personId);
                            UserProfile userProfile = userProfileDAO.findByPersonId(personId);
                            return planMembershipList.stream()
                                    .map(membership -> {
                                        String cmsRequest = cmsRequestBuilder.buildCmsSearchRequestForAllMemberships(
                                                target.getRequest(), target.getRequestOptions(), person, List.of(membership));
                                        LOGGER.trace(ContentConstants.MAKING_CMS_REQUEST, cmsRequest);
                                        return fetchCmsResponse(cmsRequest);
                                    })
                                    .filter(StringUtils::isNotBlank)
                                    .map(cmsResponse -> {
                                        try {
                                            Map<String, Object> responseMap = objectMapper.readValue(cmsResponse, Map.class);
                                            List<String> segmentList = Optional.ofNullable(responseMap.get(ContentConstants.SEGMENT_TARGET))
                                                    .map(obj -> (List<String>) obj)
                                                    .orElseGet(ArrayList::new);

                                            segmentList.stream()
                                                    .filter(StringUtils::isNotBlank)
                                                    .forEach(sorted::add);

                                            String segments = buildSegments(segmentList);

                                            Optional.ofNullable(userProfile.getSegments())
                                                    .orElseGet(HashMap::new)
                                                    .putAll(planMembershipList.stream()
                                                            .collect(Collectors.toMap(
                                                                    membership -> membership.getId().toString(),
                                                                    membership -> segments)));

                                            saveSegments(userProfile, userProfile.getSegments());
                                        } catch (IOException e) {
                                            LOGGER.error("Could not create Map from {} : {}", cmsResponse, e.getMessage());
                                        }
                                        return null;
                                    });
                        })
                        .orElse(Stream.empty()))
        .collect(Collectors.toSet());

LOGGER.debug("Ordered segments: {}", sorted);
saveSegmentOrder(personId, new ArrayList<>(sorted));
