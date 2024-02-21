

@Service
public class ContentManagementServiceImpl implements ContentManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentManagementServiceImpl.class);

    @Autowired
    private RequestBuilder cmsRequestBuilder;
    @Autowired
    private CmsConfigService cmsConfigService;
   /* @Autowired
    private MimeTypeMapper mimeTypeMapper;*/
    /*@Autowired
    @Qualifier("filteredMembershipService")
    @Lazy
    private MembershipService membershipService;

    @Autowired
    @Qualifier("ssoMembershipService")
    @Lazy
    private MembershipService ssoMembershipService;

    @Autowired
    private PersonService personService;
    @Autowired
    private UserProfileDAO userProfileDAO;*/
 /*   @Autowired
    private GroupsService groupsService;
    @Autowired
    private ParserConfig parserConfig;
    @Autowired
    @Qualifier("objectMapper")
    private ObjectMapper objectMapper;*/

    @Autowired
    private Parser cmsParser;
    @Value("${services.cs.username}")
    private String dataPowerUser;
    @Value("${services.cs.password}")
    private String dataPowerPass;
   /* @Autowired
    @Qualifier("restTemplate")
    RestTemplate restTemplate;


    @Autowired
    private CacheProxyManager cacheProxyManager;*/
    private Map<String, Map<String, Map<String, Object>>> tagCache;


    public void initializeCache(Long personId) {
        LOGGER.trace("Initialize segments cache");
        clearSegments(personId);
        WrappedResponse<List<Membership>> memberships = membershipService.getAllRelatedMemberships(personId);
        Set<String> sorted = new LinkedHashSet<String>();

        List<Membership> membershipList = memberships.getResponse();
        try {
            Collections.sort(membershipList, MembershipImpl.getComparator());
        } catch (Exception e) {
            LOGGER.error("Could not sort memberships: {}", membershipList);
        }

        Map<String, List<Membership>> plans = new LinkedHashMap<String, List<Membership>>();
        for (Membership membership : membershipList) {
            String planKey = getPlanKey(membership);
            if (!plans.containsKey(planKey)) {
                plans.put(planKey, new ArrayList<Membership>());
            }
            List<Membership> list = plans.get(planKey);
            list.add(membership);
            plans.put(planKey, list);
        }

        String cmsResponse = null;
        try {
            FunctionalArea fa = getFunctionalAreaFromConfig(ContentConstants.SEGMENT_FUNC, true);
            Map<String, Target> targets = fa.getTargets();
            if (targets != null && targets.containsKey(ContentConstants.SEGMENT_TARGET)) {
                Target t = targets.get(ContentConstants.SEGMENT_TARGET);
                Person person = getPerson(personId);
                UserProfile userProfile = userProfileDAO.findByPersonId(personId);
                for (List<Membership> planMembershipList : plans.values()) {
                    String cmsRequest = cmsRequestBuilder.buildCmsSearchRequestForAllMemberships(t.getRequest(), t.getRequestOptions(), person, planMembershipList);
                    LOGGER.trace(ContentConstants.MAKING_CMS_REQUEST, cmsRequest);
                    cmsResponse = fetchCmsResponse(cmsRequest);
                    if (null != cmsResponse && !cmsResponse.isEmpty()) {
                        Map<String, Object> responseMap = objectMapper.readValue(cmsResponse, Map.class);
                        List<String> segmentList = new ArrayList<String>();
                        if (MapUtils.isNotEmpty(responseMap) && responseMap.containsKey(ContentConstants.SEGMENT_TARGET)) {
                            segmentList = (List<String>) responseMap.get(ContentConstants.SEGMENT_TARGET);
                        }
                        for (String segment : segmentList) {
                            if (StringUtils.isNotBlank(segment)) {
                                sorted.add(segment);
                            }
                        }
                        String segments = buildSegments(segmentList);
                        if (StringUtils.isNotBlank(segments)) {
                            Map<String, Object> segmentMap = userProfile.getSegments();
                            for (Membership membership : planMembershipList) {
                                LOGGER.trace("Caching segments '{}' for membershipId {}", segments, membership.getId());

                                if (segmentMap == null) {
                                    segmentMap = new HashMap<String, Object>();
                                }
                                segmentMap.put(membership.getId().toString(), segments);
                            }
                            saveSegments(userProfile, segmentMap);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not create Map from {} : {}", cmsResponse, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not handle CMS request", e);
        }

        LOGGER.debug("Ordered segments: {}", sorted);
        saveSegmentOrder(personId, new ArrayList<String>(sorted));
    }

    private String buildSegments(List<String> segmentList) {
        Set<String> segmentSet = new HashSet<String>();
        if (segmentList != null && !segmentList.isEmpty()) {
            segmentSet.addAll(segmentList);
        }
        return StringUtils.join(segmentSet, ContentConstants.SEGMENT_AND);
    }

    private void clearSegments(Long personId) {
        UserProfile userProfile = userProfileDAO.findByPersonId(personId);
        if (userProfile != null) {
            userProfile.setSegments(null);
            userProfileDAO.save(userProfile);
        }
    }

    private void saveSegments(UserProfile userProfile, Map<String, Object> segmentMap) {
        if (userProfile != null) {
            userProfile.setSegments(segmentMap);
            userProfileDAO.save(userProfile);
        }
    }

    private void saveSegmentOrder(Long personId, List<String> segmentOrder) {
        UserProfile userProfile = userProfileDAO.findByPersonId(personId);
        if (userProfile != null) {
            userProfile.setSegmentOrder(segmentOrder);
            userProfileDAO.save(userProfile);
        }
    }

    private String getPlanKey(Membership membership) {
        List<String> key = new ArrayList<String>();
        key.add(membership.getEnrolleeId());
        key.add(membership.getContractNumber());
        key.add(membership.getGroupNumber());
        key.add(membership.getGroupSuffix());
        return StringUtils.join(key, "-");
    }

    @Override
    public Map<String, Object> getJsonContentMap(String functionalArea, Long personId, Long memberId) {
        String contentString = getJsonContentString(functionalArea, personId, memberId);

        try {
            return objectMapper.readValue(contentString, Map.class);
        } catch (IOException e) {
            LOGGER.error("Error creating a JsonContentMap from content string: {} : {}", contentString, e.getMessage());
        }

        return null;
    }

    @Override
    public String getJsonContentString(String functionalArea, Long personId, Long memberId) {
        Assert.notNull(memberId, "memberId cannot be null");
        Assert.notNull(personId, "personId cannot be null");

        return doContentFetch(functionalArea, personId, memberId);
    }

    @Override
    public String getJsonContentStringMedicareAdvantageDocuments(String functionalArea, Long personId, Long memberId) {
        Assert.notNull(memberId, "memberId cannot be null");
        Assert.notNull(personId, "personId cannot be null");

        return doContentFetchForMobileMAPlanDocuments(functionalArea, personId, memberId);
    }

    @Override
    public String getJsonContentStringByContentId(String functionalArea, String apiType, String contentId) {
        Assert.notNull(functionalArea, ContentConstants.FUNCTIONAL_AREA_CANNOT_NULL);
        Assert.notNull(apiType, "apiType cannot be null");
        Assert.notNull(contentId, "contentId cannot be null");

        return doContentFetchByContentId(functionalArea, apiType, contentId);
    }

    public String getJsonContentStringBySlug(String apiType, String slug) {
        Map<String, Object> jsonContent = getContentObject(apiType, slug);
        try {
            return objectMapper.writeValueAsString(jsonContent);
        } catch (IOException e) {
            return null;
        }
    }

    private FunctionalArea getFunctionalAreaFromConfig(String functionalArea, boolean includeSegments) {
        Map<String, Object> mapperConfig = cmsConfigService.getConfigAsMap(CmsConfigTypes.MAPPER_CONTENT_KEY);

        FunctionalArea fa = null;
        if (mapperConfig != null && !mapperConfig.isEmpty()) {
            try {
                fa = objectMapper.readValue(objectMapper.writeValueAsString(mapperConfig.get(functionalArea.toUpperCase())), FunctionalArea.class);
            } catch (IOException e) {
                LOGGER.error("Error retrieving FunctionalAreaFromConfig: {} : {}", functionalArea, e.getMessage());
            }
        }

        if (fa != null && !includeSegments) {
            fa.getTargets().remove(ContentConstants.SEGMENT_TARGET);
        }

        return fa;
    }

    private String buildFunctionalAreaCacheKey(String functionalArea, String key) {
        StringBuilder sb = new StringBuilder();
        sb.append(functionalArea);
        sb.append(key);
        return sb.toString();
    }

    private String buildFunctionalAreaCacheKey(String functionalArea, Long personId) {
        StringBuilder sb = new StringBuilder();
        sb.append(functionalArea);
        sb.append(personId);
        return sb.toString();
    }

    @Override
    public String getFunctionalArea(String functionalArea, Long personId, String ip) {
        Assert.notNull(functionalArea, ContentConstants.FUNCTIONAL_AREA_CANNOT_NULL);
        String key = personId == null ? ip : String.valueOf(personId);
        String cacheKey = buildFunctionalAreaCacheKey(functionalArea, key);
        LOGGER.debug(ContentConstants.CACHE_KEY_FUNCTIONAL_AREA_FROM_CMS_CACHE, cacheKey);
        Map<String, Map<String, Object>> targetMap = getCmsCache().get(cacheKey);
        try {
            if (isEmpty(targetMap)) {
                targetMap = new HashMap<String, Map<String, Object>>();
                String cmsRequest = cmsRequestBuilder.buildFunctionalAreaRequest(functionalArea, personId);
                LOGGER.debug(ContentConstants.MAKING_CMS_REQUEST_FOR_FUNCTION, functionalArea, cmsRequest);
                String cmsResponse = fetchCmsResponse(cmsRequest);
                LOGGER.debug("returned response from resttemplate in getFunctionalArea");
                if (null != cmsResponse && !cmsResponse.isEmpty()) {
                    targetMap.putAll(parseTargetsFromResponse(functionalArea, cmsResponse));
                    if (!functionalArea.equalsIgnoreCase(ContentConstants.ALL_LOWER)) {
                        targetMap.putAll(parseTargetsFromResponse(ContentConstants.ALL_LOWER, cmsResponse));
                    }
                    if (!isEmpty(targetMap)) {
                        LOGGER.debug(ContentConstants.CACHE_KEY_FOR_PUT_FUNCTIONAL_AREA_CMS_CACHE, cacheKey);
                        getCmsCache().put(cacheKey, targetMap);
                        LOGGER.debug("After CacheKey for put FunctionalArea into cmsCache: [{}]", cacheKey);
                    }
                }
            } else {
                LOGGER.debug("getFunctionalArea -> returning from Cache for functional area {}", functionalArea);
            }
            return objectMapper.writeValueAsString(targetMap);
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_WHILE_PARSING_CMS_RESPONSE, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not get functional area {}: {}", functionalArea, e);
        }
        return null;
    }

    @Override
    public String getFunctionalAreaResponsive(String functionalArea, Long personId, String ip) {
        Assert.notNull(functionalArea, ContentConstants.FUNCTIONAL_AREA_CANNOT_NULL);
        String key = personId == null ? ip : String.valueOf(personId);
        String cacheKey = buildFunctionalAreaCacheKey(functionalArea, key);
        LOGGER.debug("CacheKey for get FunctionalAreaResponsive from cmsCache: [{}]", cacheKey);
        Map<String, Map<String, Object>> targetMap = getCmsCache().get(cacheKey);
        try {
            if (isEmpty(targetMap)) {
                targetMap = new HashMap<String, Map<String, Object>>();
                String cmsRequest = cmsRequestBuilder.buildFunctionalAreaRequest(functionalArea, personId);
                LOGGER.debug("Making CMS request for functional area Responsive {} with url: {}", functionalArea, cmsRequest);
                String cmsResponse = fetchCmsResponse(cmsRequest);
                if (null != cmsResponse && !cmsResponse.isEmpty()) {
                    if (cmsResponse != null) {
                        targetMap.putAll(parseTargetsFromResponse(functionalArea, cmsResponse));
                        if (!isEmpty(targetMap)) {
                            LOGGER.debug("CacheKey for put FunctionalArea responsive into cmsCache: [{}]", cacheKey);
                            getCmsCache().put(cacheKey, targetMap);
                            LOGGER.debug("After CacheKey for put FunctionalArea responsive into cmsCache: [{}]", cacheKey);
                        }
                    }
                } else {
                    LOGGER.debug("getFunctionalAreaResponsive -> returning from Cache for functional area {}", functionalArea);
                }
            }
            return objectMapper.writeValueAsString(targetMap);
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_WHILE_PARSING_CMS_RESPONSE, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not get functional area Responsive {}: {}", functionalArea, e);
        }
        return null;
    }

    @Override
    public String getFunctionalArea(String functionalArea, Long personId) {
        Assert.notNull(functionalArea, ContentConstants.FUNCTIONAL_AREA_CANNOT_NULL);
        String cacheKey = buildFunctionalAreaCacheKey(functionalArea, personId);
        LOGGER.debug(ContentConstants.CACHE_KEY_FUNCTIONAL_AREA_FROM_CMS_CACHE, cacheKey);
        Map<String, Map<String, Object>> targetMap = getCmsCache().get(cacheKey);
        try {
            if (isEmpty(targetMap)) {
                targetMap = new HashMap<String, Map<String, Object>>();
                String cmsRequest = cmsRequestBuilder.buildFunctionalAreaRequest(functionalArea, personId);
                LOGGER.debug(ContentConstants.MAKING_CMS_REQUEST_FOR_FUNCTION, functionalArea, cmsRequest);
                String cmsResponse = fetchCmsResponse(cmsRequest);
                LOGGER.debug("returned response from resttemplate in getFunctionalArea");
                if (null != cmsResponse && !cmsResponse.isEmpty()) {
                    targetMap.putAll(parseTargetsFromResponse(functionalArea, cmsResponse));
                    if (!functionalArea.equalsIgnoreCase(ContentConstants.ALL_LOWER)) {
                        targetMap.putAll(parseTargetsFromResponse(ContentConstants.ALL_LOWER, cmsResponse));
                    }
                    if (!isEmpty(targetMap)) {
                        LOGGER.debug(ContentConstants.CACHE_KEY_FOR_PUT_FUNCTIONAL_AREA_CMS_CACHE, cacheKey);
                        getCmsCache().put(cacheKey, targetMap);
                        LOGGER.debug("After CacheKey for put FunctionalArea into cmsCache: [{}]", cacheKey);
                    }
                }
            } else {
                LOGGER.debug("getFunctionalArea -> returning from Cache for functional area {}", functionalArea);
            }
            return objectMapper.writeValueAsString(targetMap);
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_WHILE_PARSING_CMS_RESPONSE, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not get functional area {}: {}", functionalArea, e);
        }
        return null;
    }

    @Override
    public String getFunctionalAreaResponsive(String functionalArea, Long personId) {
        Assert.notNull(functionalArea, ContentConstants.FUNCTIONAL_AREA_CANNOT_NULL);
        String cacheKey = buildFunctionalAreaCacheKey(functionalArea, personId);
        LOGGER.debug("CacheKey for get FunctionalAreaResponsive from cmsCache: [{}]", cacheKey);
        Map<String, Map<String, Object>> targetMap = getCmsCache().get(cacheKey);
        try {
            if (isEmpty(targetMap)) {
                targetMap = new HashMap<String, Map<String, Object>>();
                String cmsRequest = cmsRequestBuilder.buildFunctionalAreaRequest(functionalArea, personId);
                LOGGER.debug("Making CMS request for functional area Responsive {} with url: {}", functionalArea, cmsRequest);
                String cmsResponse = fetchCmsResponse(cmsRequest);
                if (null != cmsResponse && !cmsResponse.isEmpty()) {
                    if (cmsResponse != null) {
                        targetMap.putAll(parseTargetsFromResponse(functionalArea, cmsResponse));
                        if (!isEmpty(targetMap)) {
                            LOGGER.debug("CacheKey for put FunctionalArea responsive into cmsCache: [{}]", cacheKey);
                            getCmsCache().put(cacheKey, targetMap);
                            LOGGER.debug("After CacheKey for put FunctionalArea responsive into cmsCache: [{}]", cacheKey);
                        }
                    }
                } else {
                    LOGGER.debug("getFunctionalAreaResponsive -> returning from Cache for functional area {}", functionalArea);
                }
            }
            return objectMapper.writeValueAsString(targetMap);
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_WHILE_PARSING_CMS_RESPONSE, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not get functional area Responsive {}: {}", functionalArea, e);
        }
        return null;
    }

    public Map<String, Map<String, Object>> getFunctionalAreaResponsiveMap(String functionalArea, Long personId) {
        Assert.notNull(functionalArea, ContentConstants.FUNCTIONAL_AREA_CANNOT_NULL);
        String cacheKey = buildFunctionalAreaCacheKey(functionalArea, String.valueOf(personId));
        LOGGER.debug(ContentConstants.CACHE_KEY_FUNCTIONAL_AREA_FROM_CMS_CACHE, cacheKey);
        Map<String, Map<String, Object>> targetMap = getCmsCache().get(cacheKey);
        try {
            if (isEmpty(targetMap)) {
                targetMap = new HashMap<String, Map<String, Object>>();
                String cmsRequest = cmsRequestBuilder.buildFunctionalAreaRequest(functionalArea, personId);
                LOGGER.trace(ContentConstants.MAKING_CMS_REQUEST_FOR_FUNCTION, functionalArea, cmsRequest);
                String cmsResponse = fetchCmsResponse(cmsRequest);
                if (null != cmsResponse && !cmsResponse.isEmpty()) {
                    targetMap.putAll(parseTargetsFromResponse(functionalArea, cmsResponse));
                    if (!isEmpty(targetMap)) {
                        LOGGER.debug(ContentConstants.CACHE_KEY_FOR_PUT_FUNCTIONAL_AREA_CMS_CACHE, cacheKey);
                        getCmsCache().put(cacheKey, targetMap);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not get functional area Responsive Map {}: {}", functionalArea, e);
            targetMap = null;
        }

        return targetMap;
    }

    public Map<String, LinkedHashMap> getStructuredMessages(String functionalArea, Long personId) {
        return getMessages(functionalArea, personId, ContentConstants.STRUCTURED_MESSAGES_KEY);
    }

    public Map<String, LinkedHashMap> getMessages(String functionalArea, Long personId, String messageStructure) {
        Map<String, Map<String, Object>> response = getFunctionalAreaResponsiveMap(functionalArea, personId);

        if (StringUtils.isEmpty(functionalArea)) {
            return null;
        }
        if (response == null || response.get(functionalArea) == null || response.get(functionalArea).get("all") == null) {
            return null;
        }
        if (StringUtils.isBlank(messageStructure)) {
            messageStructure = ContentConstants.STRUCTURED_MESSAGES_KEY;
        }

        Object all = response.get(functionalArea).get("all");

        if (all == null) {
            return null;
        }

        HashMap allMap = (HashMap) all;
        Object messages = allMap.get(messageStructure);

        if (messages == null) {
            return null;
        }

        List messagesList = (ArrayList) messages;

        if (messagesList == null || messagesList.size() == 0 || messagesList.get(0) == null) {
            return null;
        }

        LinkedHashMap contentMap = null;
        for (Object object : messagesList) {
            LinkedHashMap messageMap = (LinkedHashMap) object;
            if (messageMap == null || messageMap.get(ContentConstants.CONTENT) == null) {
                continue;
            }
            if (contentMap == null) {
                contentMap = (LinkedHashMap) messageMap.get(ContentConstants.CONTENT);
            } else {
                contentMap.putAll((LinkedHashMap) messageMap.get(ContentConstants.CONTENT));
            }
        }

        if (contentMap == null || contentMap.size() == 0) {
            return null;
        }

        return contentMap;

    }

    public String getSlugValue(Map<String, String> contentMap, String slug) {
        if (contentMap != null && contentMap.get(slug) != null) {
            return contentMap.get(slug);
        }

        return null;
    }

    private Map<String, Map<String, Object>> parseTargetsFromResponse(String selectedFunctionalArea, String cmsResponse) {
        Map<String, Map<String, Object>> targetMap = new HashMap<String, Map<String, Object>>();
        targetMap.put(selectedFunctionalArea, new HashMap<String, Object>());
        try {
            Map<String, Object> responseMap = objectMapper.readValue(cmsResponse, Map.class);
            if (!responseMap.isEmpty()) {
                expireTagCache();

                Map<String, Map<String, List<Object>>> builtMap = new HashMap<String, Map<String, List<Object>>>();

                for (String contentTypeKey : responseMap.keySet()) {
                    List<Map<String, Object>> contentTypeList = (List<Map<String, Object>>) responseMap.get(contentTypeKey);

                    for (Map<String, Object> contentObject : contentTypeList) {
                        List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObject.get(ContentConstants.CONTENT);

                        Map<String, Object> targeting = (Map<String, Object>) contentObject.get(ContentConstants.TARGETING);
                        List<String> funcList = new ArrayList<String>();
                        if (!targeting.isEmpty() && targeting.containsKey(ContentConstants.FUNC_KEY)) {
                            funcList = (List<String>) targeting.get(ContentConstants.FUNC_KEY);
                        }

                        if (funcList.contains(selectedFunctionalArea)) {
                            List<String> targetLocationList = Arrays.asList(ContentConstants.ALL_LOWER);
                            if (!targeting.isEmpty() && targeting.containsKey(ContentConstants.TARGET_LOCATION)) {
                                List<String> tmp = (List<String>) targeting.get(ContentConstants.TARGET_LOCATION);
                                if (tmp != null && tmp.size() > 0) {
                                    targetLocationList = tmp;
                                }
                            }

                            List<String> segmentList = new ArrayList<String>();
                            if (!targeting.isEmpty() && targeting.containsKey(ContentConstants.SEGMENT_KEY)) {
                                segmentList = (List<String>) targeting.get(ContentConstants.SEGMENT_KEY);
                            }

                            List<Object> applicableContentPieces = new ArrayList<Object>();
                            for (Map<String, Object> content : contentList) {
                                content = cmsParser.parse(ApiEndpointMapImpl.CMS_API_TYPE, content);
                                content.put(ContentConstants.FUNC_KEY, funcList);
                                content.put(ContentConstants.TARGET_LOCATION, targetLocationList);
                                content.put(ContentConstants.SEGMENT_KEY, segmentList);
                                content.put(ContentConstants.CONTENT_TYPE, contentTypeKey);
                                content = hoistAttributes(content);
                                applicableContentPieces.add(content);
                            }
                            applicableContentPieces = dedupeContent(applicableContentPieces);

                            for (String targetLocation : targetLocationList) {
                                if (!builtMap.containsKey(targetLocation)) {
                                    builtMap.put(targetLocation, new HashMap<String, List<Object>>());
                                }

                                Map<String, List<Object>> targetBuiltMap = builtMap.get(targetLocation);
                                if (!targetBuiltMap.containsKey(contentTypeKey)) {
                                    targetBuiltMap.put(contentTypeKey, new ArrayList<Object>());
                                }
                                targetBuiltMap.get(contentTypeKey).addAll(applicableContentPieces);

                                builtMap.put(targetLocation, targetBuiltMap);
                            }
                        }
                    }
                }

                targetMap.get(selectedFunctionalArea).putAll(builtMap);
            }
        } catch (IOException e) {
            LOGGER.error("Could not get targets from cmsResponse: '{}': {}", cmsResponse, e);
        }

        return targetMap;
    }


    // We heard you like fetching content.  These two methods could be merged but there is already
    // parameter overload going on in the actual fetch method so I kind of want to keep these around
    // for the ease of use.
    private String doContentFetch(String functionalArea, Long personId, Long memberId) {
        Map<String, Map<String, Object>> targetMap = buildRequestAndHandleResponse(ApiEndpointMapImpl.CMS_API_TYPE, null, functionalArea, personId, memberId, false);
        try {
            return objectMapper.writeValueAsString(targetMap);
        } catch (IOException e) {
            return null;
        }
    }

    private String doContentFetchForMobileMAPlanDocuments(String functionalArea, Long personId, Long memberId) {

        Map<String, Map<String, Object>> targetMap = buildRequestAndHandleResponse(ApiEndpointMapImpl.CMS_API_TYPE, null, functionalArea, personId, memberId, false);
        Map<String, List<Map<String, Object>>> simplifiedMap = new HashMap<String, List<Map<String, Object>>>();

        if (targetMap.containsKey(ContentConstants.COVERAGES_FUNCTIONAL_AREA)) {
            Map<String, Object> coveragesMap = targetMap.get(ContentConstants.COVERAGES_FUNCTIONAL_AREA);

            if (coveragesMap.containsKey(ContentConstants.PLANDOCS)) {
                Map<String, Object> plandocsMap = (Map<String, Object>) coveragesMap.get(ContentConstants.PLANDOCS);
                //should be list
                List<Map<String, Object>> processedFilesList = new ArrayList();

                if (plandocsMap.containsKey(ContentConstants.FILES)) {
                    List<Map<String, Object>> filesList = formatedPlanDocs((List<Map<String, Object>>) plandocsMap.get(ContentConstants.FILES));

                    //loop over files from target map:
                    for (int i = 0; i < filesList.size(); i++) {
                        //get the wanted keys
                        Object id = filesList.get(i).get(ContentConstants.ID);
                        Object contentSubtype = filesList.get(i).get(ContentConstants.CONTENT_SUB_TYPE);
                        Object title = filesList.get(i).get(ContentConstants.TITLE);
                        Object path = filesList.get(i).get(ContentConstants.PATH_KEY);

                        //put in new map
                        Map<String, Object> processedFilesMap = new HashMap<String, Object>();
                        processedFilesMap.put(ContentConstants.ID, id);
                        processedFilesMap.put(ContentConstants.CONTENT_SUB_TYPE, contentSubtype);
                        processedFilesMap.put(ContentConstants.TITLE, title);
                        processedFilesMap.put(ContentConstants.PATH_KEY, path);
                        if (contentSubtype.equals(ContentConstants.SBC)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 1);
                        }
                        if (contentSubtype.equals(ContentConstants.ANOCLETTER)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 2);
                        }
                        if (contentSubtype.equals(ContentConstants.EOC)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 3);
                        }
                        if (contentSubtype.equals(ContentConstants.BENEFITCHART)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 4);
                        }
                        if (contentSubtype.equals(ContentConstants.BENEFITSATAGLANCE)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 5);
                        }
                        if (contentSubtype.equals(ContentConstants.FORMULARY)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 6);
                        }
                        if (contentSubtype.equals(ContentConstants.RESOURCEGUIDE)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 7);
                        }
                        if (contentSubtype.equals(ContentConstants.PROVIDERDIRECTORY)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 8);
                        }
                        if (contentSubtype.equals(ContentConstants.PHARMACYDIRECTORY)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 9);
                        }
                        if (contentSubtype.equals(ContentConstants.PROVIDERPHARMACYDIRECTORY)) {
                            processedFilesMap.put(ContentConstants.SORTORDER, 10);
                        }
                        processedFilesList.add(processedFilesMap);
                    }
                    simplifiedMap.put(ContentConstants.PLANDOCS, processedFilesList);
                }
            }
        }

        try {
            return objectMapper.writeValueAsString(simplifiedMap);
        } catch (IOException e) {
            return null;
        }
    }

    private List<Map<String, Object>> formatedPlanDocs(List<Map<String, Object>> filesList) {
        List<Map<String, Object>> processedFilesList = new ArrayList();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (Map<String, Object> file : filesList) {
            String[] fileNameSplit = file.get("path").toString().split("/");
            if (fileNameSplit != null && fileNameSplit.length > 0) {
                String year = fileNameSplit[fileNameSplit.length - 1].substring(0, 4);
                if (StringUtils.isNumeric(year) && (Integer.parseInt(year) == currentYear || Integer.parseInt(year) == currentYear + 1)) {
                    processedFilesList.add(file);
                }
            }
        }
        return processedFilesList;
    }

    private String doContentFetchByContentId(String functionalArea, String apiType, String contentId) {
        Map<String, Map<String, Object>> targetMap = buildRequestAndHandleResponse(apiType, contentId, functionalArea, null, null, false);
        try {
            return objectMapper.writeValueAsString(targetMap);
        } catch (IOException e) {
            return null;
        }
    }

    private Map<String, Map<String, Object>> buildRequestAndHandleResponse(String apiType, String contentId, String functionalArea,
                                                                           Long personId, Long memberId, boolean segmentRequest) {
        Assert.notNull(functionalArea, ContentConstants.FUNCTIONAL_AREA_CANNOT_NULL);
        FunctionalArea fa = getFunctionalAreaFromConfig(functionalArea, segmentRequest);

        String cacheKey = buildCmsRequestCacheKey(apiType, contentId, functionalArea, personId, memberId, segmentRequest);
        Map<String, Map<String, Object>> targetMap = getCmsCache().get(cacheKey);
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
                        LOGGER.trace(ContentConstants.MAKING_CMS_REQUEST, cmsRequest);

                        String cmsResponse = null;
                        if (AppConfigServiceImpl.isNewMBAEnabled() && t != null && t.getRequestOptions().size() > 0 && !StringUtils.isEmpty(t.getRequestOptions().get(0).getValue()) && StringUtils.equalsIgnoreCase(t.getRequestOptions().get(0).getValue(), ApiEndpointMapImpl.BENEFITS_API_TYPE)) {
                            try (InputStream inputStream = FileUtils.getInputStreamObject(cmsRequest, getDataPowerUser(), getDataPowerPass());) {     // get input stream via DataPower
                                if (null != inputStream)
                                    cmsResponse = IOUtils.toString(inputStream);
                            }
                        } else {
                            cmsResponse = fetchCmsResponse(cmsRequest);
                        }
                        if (null != cmsResponse && !cmsResponse.isEmpty()) {
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
                                if (!isEmpty(targetMap)) {
                                    LOGGER.debug("CacheKey:[{}] for put FunctionalArea into cmsCache while fetching content: [{}]", cacheKey, contentId);
                                    getCmsCache().put(cacheKey, targetMap);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not handle CMS request", e);
            targetMap = null;
        }

        return targetMap;
    }

    private String buildCmsRequestCacheKey(String apiType, String contentId, String functionalArea, Long personId, Long memberId, boolean handleResponseAsMap) {
        StringBuilder sb = new StringBuilder();
        sb.append(apiType);
        sb.append(contentId);
        sb.append(functionalArea);
        sb.append(personId);
        sb.append(memberId);
        sb.append(handleResponseAsMap);
        return sb.toString();
    }

*
     * Decides what kind of cms request to build based on apiType.
     * <p/>
     * Only calls to the cms endpoint make user of the person and member


    private String createCmsRequest(String apiType, String contentId, Long personId, Long memberId, Target t) {
        Person person = getPerson(personId);
        Membership membership = getMembership(personId, memberId);
        if (StringUtils.equals(ApiEndpointMapImpl.BENEFITS_API_TYPE, apiType)) {
            return cmsRequestBuilder.buildCmsDocumentRequestById(apiType, contentId, person, membership);
        } else {
            return cmsRequestBuilder.buildCmsSearchRequest(t.getRequest(), t.getRequestOptions(), person, membership);
        }
    }

*
     * Maps a cms response and parses through it calling the applicable mappers.


    private Map<String, Object> doResponseHandling(String jsonString, Target target, String targetKey) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(jsonString, Map.class);
            if (!responseMap.isEmpty()) {
                expireTagCache();

                Map<String, Object> builtMap = new HashMap<String, Object>();
                ResponseOptions responseOptions = target.getResponseOptions();

                Map<String, List<Object>> builtContentTypesMap = new HashMap<String, List<Object>>();
                for (String contentTypeKey : responseMap.keySet()) {
                    List<Map<String, Object>> contentTypeList = (List<Map<String, Object>>) responseMap.get(contentTypeKey);

                    List<Object> applicableContentPieces = new ArrayList<Object>();
                    for (Map<String, Object> contentObject : contentTypeList) {
                        List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObject.get(ContentConstants.CONTENT);

                        Map<String, Object> targeting = (Map<String, Object>) contentObject.get(ContentConstants.TARGETING);
                        List<String> funcList = new ArrayList<String>();
                        if (!targeting.isEmpty() && targeting.containsKey(ContentConstants.FUNC_KEY)) {
                            funcList = (List<String>) targeting.get(ContentConstants.FUNC_KEY);
                        }

                        List<String> targetLocationList = Arrays.asList(ContentConstants.ALL_LOWER);
                        if (!targeting.isEmpty() && targeting.containsKey(ContentConstants.TARGET_LOCATION)) {
                            List<String> tmp = (List<String>) targeting.get(ContentConstants.TARGET_LOCATION);
                            if (tmp != null && tmp.size() > 0) {
                                targetLocationList = tmp;
                            }
                        }

                        List<String> segmentList = new ArrayList<String>();
                        if (!targeting.isEmpty() && targeting.containsKey(ContentConstants.SEGMENT_KEY)) {
                            segmentList = (List<String>) targeting.get(ContentConstants.SEGMENT_KEY);
                        }

                        for (Map<String, Object> content : contentList) {
                            String apiType = cmsRequestBuilder.getApiType(target.getRequestOptions());
                            content = cmsParser.parse(apiType, content);
                            content.put(ContentConstants.FUNC_KEY, funcList);
                            content.put(ContentConstants.TARGET_LOCATION, targetLocationList);
                            content.put(ContentConstants.SEGMENT_KEY, segmentList);
                            content.put(ContentConstants.CONTENT_TYPE, contentTypeKey);
                            content = hoistAttributes(content);
                            applicableContentPieces.add(content);
                        }
                    }

                    if (responseOptions.getDedupe()) {
                        applicableContentPieces = dedupeContent(applicableContentPieces);
                    }

                    builtContentTypesMap.put(contentTypeKey, applicableContentPieces);

                }

                builtMap.put(targetKey, builtContentTypesMap);
                return builtMap;
            }
        } catch (IOException e) {
            LOGGER.error("Error handling resposne properly", e);
        }

        return null;
    }

    private Map<String, Object> hoistAttributes(Map<String, Object> content) {
        List<String> attrsToHoist = Arrays.asList(
                ContentConstants.ACTIVE_DATE_END,
                ContentConstants.ACTIVE_DATE_START,
                "alt",
                ContentConstants.CONTENT_TYPE,
                ContentConstants.CONTENT_SUB_TYPE,
                "description",
                ContentConstants.MODIFIED_DATE_KEY,
                "path",
                "sortpriority",
                ContentConstants.TITLE,
                "aria_label"
        );
        if (content != null && content.containsKey(ContentConstants.CONTENT)) {
            Map<String, Object> contentObj = (Map<String, Object>) content.get(ContentConstants.CONTENT);
            if (contentObj != null && !contentObj.isEmpty()) {
                for (String attrToHoist : attrsToHoist) {
                    if (contentObj.containsKey(attrToHoist)) {
                        content.put(attrToHoist, contentObj.get(attrToHoist));
                    }
                }
            }
        }
        return content;
    }

    public Map<String, Object> getParsedContentBySlug(String slug) {

        String key = "getParsedContentBySlug:".concat(slug);
        Map<String, Object> response = getCmsSlugCache().get(key);
        if(null != response){
            return response.containsKey("NO_DATA") ? null : response;

        }

        String cmsRequest = cmsRequestBuilder.buildCmsDocumentRequestBySlug(ApiEndpointMapImpl.CMS_API_TYPE, slug);
        LOGGER.debug("Making CMS request for parsed content by slug with url: [{}]", cmsRequest);

        try {
            String cmsResponse = fetchCmsResponse(cmsRequest);
            if (StringUtils.isBlank(cmsResponse) || StringUtils.equals(cmsResponse, "{}")) {
                LOGGER.debug("No data found in cms for slug: [{}]", slug);
                response = new HashMap<String, Object>();
                response.put("NO_DATA", "NO_DATA");
            } else {
                Map<String, Object> responseMap = objectMapper.readValue(cmsResponse, Map.class);
                expireTagCache();
                for (String contentTypeKey : responseMap.keySet()) {
                    List<Map<String, Object>> contentTypeList = (List<Map<String, Object>>) responseMap.get(contentTypeKey);
                    for (Map<String, Object> contentType : contentTypeList) {
                        List<Map<String, Object>> contentPieces = (List<Map<String, Object>>) contentType.get(ContentConstants.CONTENT);
                        for (Map<String, Object> content : contentPieces) {
                            response = cmsParser.parse(ApiEndpointMapImpl.CMS_API_TYPE, content);

                            return response;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing request for: [{}] : {}", cmsRequest, e);
        }  finally {
            if(null != response){
                getCmsSlugCache().put(key, response);
            }
        }
        return null;
    }


    public CmsWrapper.SSOUrlsData getContentBySlug(String slug) {

        String cmsRequest = cmsRequestBuilder.buildCmsDocumentRequestBySlug(ApiEndpointMapImpl.CMS_API_TYPE, slug);
        CmsWrapper cmsWrapper;
        CmsWrapper.SSOUrlsData ssoUrlsData;
        try {
//            objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            cmsWrapper = objectMapper.readValue(new URL(cmsRequest), CmsWrapper.class); // converting itssoUrls json data to CmsWrapper POJO.
            ssoUrlsData = cmsWrapper.getProperties().get(0).getContent().get(0).getContent();
        } catch (MalformedURLException e) {
            LOGGER.error("ContentBySlug This given URL was not formed correctly for the file path {} : {}", cmsRequest, e.getMessage());
            return null;
        } catch (IOException e) {
            LOGGER.error("ContentBySlug There was an IO error for file path {} : {}", cmsRequest, e.getMessage());
            return null;
        }
        return ssoUrlsData;
    }

    public PlannedOutageCmsWrapper.PlannedOutageData getContentByFunctionalArea(String functionalArea, Long personId) {
        String cmsRequest = cmsRequestBuilder.buildFunctionalAreaRequest(functionalArea, personId);

        LOGGER.trace(ContentConstants.MAKING_CMS_REQUEST_FOR_FUNCTION, functionalArea, cmsRequest);
        PlannedOutageCmsWrapper plannedOutageCmsWrapper;
        PlannedOutageCmsWrapper.PlannedOutageData plannedOutageData = null;
        try {
            //objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            plannedOutageCmsWrapper = objectMapper.readValue(new URL(cmsRequest), PlannedOutageCmsWrapper.class);
            plannedOutageData = plannedOutageCmsWrapper.getStructuredmessages().get(0).getContent().get(0).getContent();
        } catch (MalformedURLException e) {
            LOGGER.error("ContentByFunctionalArea This given URL was not formed correctly for the file path {} : {}", cmsRequest, e.getMessage());
            return null;
        } catch (IOException e) {
            LOGGER.error("ContentByFunctionalArea There was an IO error for file path {} : {}", cmsRequest, e.getMessage());
            return null;
        }
        return plannedOutageData;
    }

    // Cms parsing methods

    public Map<String, Object> getContentObject(String apiType, String slug) {
        String cmsRequest = cmsRequestBuilder.buildCmsDocumentRequestBySlug(apiType, slug);
        Map<String, Object> cachedContentObject = getCachedContentObject(cmsRequest, slug, slug);
        return hoistAttributes(cachedContentObject);
    }

    @Override
    public Map<String, Object> getFileContentObject(String apiType, String contentId) {
        Request request = new RequestImpl();
        List<Attribute> constantVariables = new ArrayList<Attribute>();
        constantVariables.add(new AttributeImpl(ContentConstants.CONTENT_TYPE, ContentConstants.FILES));
        request.setConstantValues(constantVariables);

        List<Attribute> requestOptions = new ArrayList<Attribute>();
        requestOptions.add(new AttributeImpl(RequestBuilderImpl.ENDPOINT_KEY, apiType));

        String cmsRequest = cmsRequestBuilder.buildCmsSearchRequest(request, requestOptions, null, null);

        Map<String, Object> cachedContentObject = getCachedContentObject(cmsRequest, ContentConstants.CONTENT_OBJECT_CACHE_KEY, ContentConstants.FILES);
        return getCachedObjectBySlug(cachedContentObject, ContentConstants.FILES, contentId);
    }


    public Map<String, Object> getLinkContentObject(String apiType, String contentId) {
        Request request = new RequestImpl();
        List<Attribute> constantVariables = new ArrayList<Attribute>();
        constantVariables.add(new AttributeImpl(ContentConstants.CONTENT_TYPE, ContentConstants.STRUCTURED_MESSAGES_KEY));
        constantVariables.add(new AttributeImpl(ContentConstants.CONTENT_SUB_TYPE, ContentConstants.LINK_KEY));
        request.setConstantValues(constantVariables);

        List<Attribute> requestOptions = new ArrayList<Attribute>();
        requestOptions.add(new AttributeImpl(RequestBuilderImpl.ENDPOINT_KEY, apiType));

        String cmsRequest = cmsRequestBuilder.buildCmsSearchRequest(request, requestOptions, null, null);

        Map<String, Object> cachedContentObject = getCachedContentObject(cmsRequest, ContentConstants.CONTENT_OBJECT_CACHE_KEY, ContentConstants.LINK_KEY);
        return getCachedObjectBySlug(cachedContentObject, ContentConstants.STRUCTURED_MESSAGES_KEY, contentId);
    }

    @Override
    public Map<String, Object> getImageContentObject(String apiType, String contentId) {
        Request request = new RequestImpl();
        List<Attribute> constantVariables = new ArrayList<Attribute>();
        constantVariables.add(new AttributeImpl(ContentConstants.CONTENT_TYPE, ContentConstants.IMAGES));
        request.setConstantValues(constantVariables);

        List<Attribute> requestOptions = new ArrayList<Attribute>();
        requestOptions.add(new AttributeImpl(RequestBuilderImpl.ENDPOINT_KEY, apiType));

        String cmsRequest = cmsRequestBuilder.buildCmsSearchRequest(request, requestOptions, null, null);

        Map<String, Object> cachedContentObject = getCachedContentObject(cmsRequest, ContentConstants.CONTENT_OBJECT_CACHE_KEY, ContentConstants.IMAGES);
        return getCachedObjectBySlug(cachedContentObject, ContentConstants.IMAGES, contentId);
    }

    private Map<String, Object> getCachedObjectBySlug(Map<String, Object> cachedContentObject, String contentType, String slug) {
        Map<String, Object> result = new HashMap<String, Object>();
        if (isNotEmpty(cachedContentObject) && cachedContentObject.containsKey(contentType)) {
            List<Map<String, Object>> structuredMessages = (List<Map<String, Object>>) cachedContentObject.get(contentType);
            for (Map<String, Object> structuredMessage : structuredMessages) {
                if (isNotEmpty(structuredMessage) && structuredMessage.containsKey(ContentConstants.CONTENT)) {
                    List<Map<String, Object>> contentList = (List<Map<String, Object>>) structuredMessage.get(ContentConstants.CONTENT);
                    for (Map<String, Object> contentObject : contentList) {
                        if (isNotEmpty(contentObject) && contentObject.containsKey(ContentConstants.SLUG)) {
                            String contentSlug = (String) contentObject.get(ContentConstants.SLUG);
                            if (contentSlug != null && contentSlug.equalsIgnoreCase(slug)) {
                                contentObject.put(ContentConstants.CONTENT_TYPE, contentType);
                                return hoistAttributes(contentObject);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private Map<String, Map<String, Map<String, Object>>> getTagCache() {
        if (tagCache == null) {
            tagCache = new HashMap<String, Map<String, Map<String, Object>>>();
        }
        return tagCache;
    }

    private void expireTagCache() {
        tagCache = new HashMap<String, Map<String, Map<String, Object>>>();
    }

    private Map<String, Object> getCachedContentObject(String cmsRequest, String cacheKey, String objectKey) {
        Map<String, Object> cachedContentObject = new HashMap<String, Object>();
        Map<String, Map<String, Object>> cachedContentObjects = getTagCache().get(cacheKey);
        try {
            if (cachedContentObjects == null || !cachedContentObjects.containsKey(objectKey)) {
                LOGGER.trace("Cached object {} not found.", objectKey);
                String cmsResponse = fetchCmsResponse(cmsRequest);
                try {
                    if (null != cmsResponse && !cmsResponse.isEmpty()) {
                        cachedContentObject = objectMapper.readValue(cmsResponse, Map.class);
                    }
                } catch (IOException e) {
                    LOGGER.error("Error parsing request for: {}", cmsRequest);
                }
                if (cachedContentObjects == null) {
                    cachedContentObjects = new HashMap<String, Map<String, Object>>();
                }
                cachedContentObjects.put(objectKey, cachedContentObject);
                LOGGER.debug("CacheKey for put cachedContentObjects into cmsCache: [{}]", cacheKey);
                getTagCache().put(cacheKey, cachedContentObjects);
            } else if (cachedContentObjects.containsKey(objectKey)) {
                LOGGER.trace("Found cached object {}", objectKey);
                cachedContentObject = cachedContentObjects.get(objectKey);
            }
        } catch (Exception e) {
            LOGGER.error("Could not get cached content object '{}': {}", objectKey, e);
            cachedContentObjects = null;
        }
        return cachedContentObject;
    }

    // End CMS Parsing


    public List<Map<String, Object>> getContentList(Map<String, Object> response,
                                                    String functionalArea,
                                                    String target,
                                                    String contentType) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (response.containsKey(functionalArea)) {
            Map<String, Object> targets = (Map<String, Object>) response.get(functionalArea);
            if (targets.containsKey(target)) {
                Map<String, Object> contentTypes = (Map<String, Object>) targets.get(target);
                if (contentTypes.containsKey(contentType)) {
                    List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentTypes.get(contentType);
                    for (Map<String, Object> content : contentList) {
                        content.put(ContentConstants.CONTENT_TYPE, contentType);
                        result.add(hoistAttributes(content));
                    }
                }
            }
        }
        return result;
    }


    private List<Object> dedupeContent(List<Object> content) {
        Set<Object> dedupedContent = new HashSet<Object>();
        for (Object object : content) {
            dedupedContent.add(object);
        }
        return new ArrayList<Object>(dedupedContent);
    }

*
     * Presumably CQ will be able to directly stream files back, we don't want to put them in mongo so it is
     * currently mocked out as a two step process.  A request is made for the logo functional area and the
     * id is then used to stream the file back.


    public ContentWrapper getImageByTargetAsWrappedContent(String selectedTarget, String functionalArea, Long personId) {
        LOGGER.debug("Requesting image ({}) for functional area [{}] and person [{}]",
                selectedTarget, functionalArea, personId);
        Map<String, Object> contentMap = null;

        try {
            contentMap = objectMapper.readValue(doContentFetch(functionalArea, personId, null), Map.class);
        } catch (IOException e) {
            LOGGER.error("Could not retrieve functional area [{}] for person [{}]", functionalArea, personId);
        }

        if (isNotEmpty(contentMap) && contentMap.containsKey(functionalArea)) {
            Map<String, Object> allMap = (Map<String, Object>) contentMap.get(functionalArea);

            if (allMap.containsKey(ContentConstants.LOGO)) {
                Map<String, Object> imageMap = (Map<String, Object>) allMap.get(ContentConstants.LOGO);

                if (imageMap.containsKey(ContentConstants.IMAGES)) {
                    List<Map<String, Object>> images = (List<Map<String, Object>>) imageMap.get(ContentConstants.IMAGES);

                    Map<String, Object> logoToUse = null;

                    List<Map<String, Object>> logos = new ArrayList<Map<String, Object>>();

                    for (Map<String, Object> image : images) {
                        if (image.containsKey(ContentConstants.TARGET_LOCATION)) {
                            List<String> targetLocations = (List<String>) image.get(ContentConstants.TARGET_LOCATION);
                            if (targetLocations.contains(selectedTarget)) {
                                logos.add(image);
                            }
                        }
                    }

                    if (logos.size() > 0) {
                        logoToUse = getLogoToUse(logos, personId);
                    }

                    //request the logo
                    if (logoToUse != null) {
                        logoToUse.put(ContentConstants.CONTENT_TYPE, ContentConstants.IMAGES);
                        logoToUse = hoistAttributes(logoToUse);
                        String filePath = cmsRequestBuilder.buildCmsFileRequest(ApiEndpointMapImpl.CMS_API_TYPE, (String) logoToUse.get(ContentConstants.PATH_KEY));
                        InputStream inputStream = fetchCmsResponseStream(filePath);
                        if (null != inputStream) {
                            return new ContentWrapperImpl(inputStream, filePath, mimeTypeMapper);
                        }
                    }
                }
            }
        }

        return null;
    }

    protected Map<String, Object> getLogoToUse(List<Map<String, Object>> logos, Long personId) {
        Map<String, Object> logoToUse = null;

        if (logos != null) {
            // Look for segments
            List<String> userSegments = new ArrayList<String>();
            if (personId != null) {
                UserProfile userProfile = userProfileDAO.findByPersonId(personId);
                if (userProfile != null) {
                    userSegments = userProfile.getSegmentOrder();
                }
            }

            // Look for segment-specific logo
            if (userSegments != null && !userSegments.isEmpty()) {
                for (Map<String, Object> logo : logos) {
                    List<String> segments = (List<String>) logo.get(ContentConstants.SEGMENT_KEY);
                    if (segments != null && !ListUtils.intersection(userSegments, segments).isEmpty()) {
                        logoToUse = logo;
                        break;
                    }
                }
            }

            // Couldn't find a segment-specific logo so look for a default logo
            if (logoToUse == null) {
                for (Map<String, Object> logo : logos) {
                    String slug = (String) logo.get(ContentConstants.SLUG);
                    List<String> segments = (List<String>) logo.get(ContentConstants.SEGMENT_KEY);
                    if (slug.equalsIgnoreCase(ContentConstants.LOGO) && (segments == null || segments.isEmpty())) {
                        logoToUse = logo;
                        break;
                    }
                }
            }
        }

        return logoToUse;
    }

    @Override
    public ContentWrapper getFileAsWrappedContentById(String contentId, Long personId, Long memberId, String apiType) {
        Person person = getPerson(personId);
        Membership membership = getMembership(personId, memberId);
        if (StringUtils.isNotBlank(apiType) && apiType.contains("CMS.pdf")) {
            apiType = "CMS";
        }
        String documentPath = cmsRequestBuilder.buildCmsDocumentRequestById(apiType, contentId, person, membership);
        LOGGER.debug("Requesting CMS document by ID: {}", documentPath);
        return getWrappedFileFromPath(documentPath, apiType, contentId);
    }

    public ContentWrapper getFileAsWrappedContentBySlug(String slug, Long personId, Long memberId, String apiType) {
        String documentPath = cmsRequestBuilder.buildCmsDocumentRequestBySlug(apiType, slug);
        LOGGER.debug("Requesting CMS document by slug: {}", documentPath);
        return getWrappedFileFromPath(documentPath, apiType, "");
    }

    private ContentWrapper getWrappedFileFromPath(String path, String apiType, String contentId) {
        try {
            InputStream inputStream = null;

            if (AppConfigServiceImpl.isNewMBAEnabled() && StringUtils.equalsIgnoreCase(apiType, ApiEndpointMapImpl.BENEFITS_API_TYPE)) {
                inputStream = FileUtils.getInputStreamObject(path, getDataPowerUser(), getDataPowerPass());        // get input stream via DataPower
            } else {
                inputStream = fetchCmsResponseStream(path);
            }
            if (inputStream != null) {
                Map<String, Object> document = objectMapper.readValue(IOUtils.toString(inputStream), Map.class);
                inputStream.close();
                String responseDocumentKey = getResponseDocumentKey(document);
                if (null != responseDocumentKey) {
                    List<Map<String, Object>> files = (List<Map<String, Object>>) document.get(responseDocumentKey);
                    for (Map<String, Object> file : files) {
                        if (file.containsKey(ContentConstants.CONTENT)) {
                            List<Map<String, Object>> contents = (List<Map<String, Object>>) file.get(ContentConstants.CONTENT);
                            for (Map<String, Object> content : contents) {
                                content.put(ContentConstants.CONTENT_TYPE, responseDocumentKey);
                                content = hoistAttributes(content);
                                if (content.containsKey(ContentConstants.PATH_KEY)) {
                                    String mbaContentPath = (String) content.get(ContentConstants.PATH_KEY);
                                    if (StringUtils.equalsIgnoreCase(apiType, ApiEndpointMapImpl.BENEFITS_API_TYPE) && !mbaContentPath.contains(contentId)) {
                                        continue;
                                    }
                                    String filePath = cmsRequestBuilder.buildCmsFileRequest(apiType, (String) content.get(ContentConstants.PATH_KEY));
                                    if (AppConfigServiceImpl.isNewMBAEnabled() && StringUtils.equalsIgnoreCase(apiType, ApiEndpointMapImpl.BENEFITS_API_TYPE)) {
                                        LOGGER.debug("IBM-CMS/DataPower file path - ", filePath);
                                        inputStream = FileUtils.getInputStreamObject(filePath, getDataPowerUser(), getDataPowerPass());   // get input stream via DataPower
                                    } else {
                                        inputStream = fetchCmsResponseStream(filePath);
                                    }
                                    if (null != inputStream) {
                                        return new ContentWrapperImpl(inputStream, filePath, mimeTypeMapper);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error loading document from WrappedFileFromPath : {} with exception : {}", path, e.getMessage());
        }
 finally {
            IOUtils.closeQuietly(inputStream);
        }


        return null;
    }

    private String getResponseDocumentKey(Map<String, Object> document) {
        String key = null;
        if (document.containsKey(ContentConstants.FILES)) {
            key = ContentConstants.FILES;
        } else if (document.containsKey(ContentConstants.IMAGES)) {
            key = ContentConstants.IMAGES;
        }

        return key;
    }

    public String getDesktopCss(String ip) {
        String css = "";
        String contentString = getFunctionalArea(ContentConstants.ALL_LOWER, null, ip);
        try {
            Map<String, Object> functionalArea = objectMapper.readValue(contentString, Map.class);
            css = getContentByContentType(ContentConstants.ALL_LOWER, functionalArea, ContentConstants.CSS_KEY);
            if (css != null) {
                css = compressCSS(css);
            }
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_CREATING_A_MAP_CONTENT, contentString);
        }
        return css;
    }

    public String getMobileCss(String ip) {
        String css = "";
        String contentString = getFunctionalArea(ContentConstants.MOBILE, null, ip);
        try {
            Map<String, Object> functionalArea = objectMapper.readValue(contentString, Map.class);
            css = getContentByContentType(ContentConstants.MOBILE, functionalArea, ContentConstants.CSS_KEY);
            if (css != null) {
                css = compressCSS(css);
            }
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_CREATING_A_MAP_CONTENT, contentString);
        }
        return css;
    }

    public String getDesktopJavascript(String ip) {
        String js = "";
        String contentString = getFunctionalArea(ContentConstants.ALL_LOWER, null, ip);
        try {
            Map<String, Object> functionalArea = objectMapper.readValue(contentString, Map.class);
            js = getContentByContentType(ContentConstants.ALL_LOWER, functionalArea, ContentConstants.JS_KEY);
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_CREATING_A_MAP_CONTENT, contentString);
        }
        return js;
    }

    public String getMobileJavascript(String ip) {
        StringBuffer js = new StringBuffer();
        String contentString = getFunctionalArea(ContentConstants.MOBILE, null, ip);
        try {
            Map<String, Object> functionalArea = objectMapper.readValue(contentString, Map.class);
            js.append(getContentByContentType(ContentConstants.ALL_LOWER, functionalArea, ContentConstants.JS_KEY));
            js.append(getContentByContentType(ContentConstants.MOBILE, functionalArea, ContentConstants.JS_KEY));
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_CREATING_A_MAP_CONTENT, contentString);
        }
        return js.toString();
    }

    public String getDesktopCss() {
        String css = "";
        String contentString = getFunctionalArea(ContentConstants.ALL_LOWER, null);
        try {
            Map<String, Object> functionalArea = objectMapper.readValue(contentString, Map.class);
            css = getContentByContentType(ContentConstants.ALL_LOWER, functionalArea, ContentConstants.CSS_KEY);
            if (css != null) {
                css = compressCSS(css);
            }
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_CREATING_A_MAP_CONTENT, contentString);
        }
        return css;
    }

    public String getMobileCss() {
        String css = "";
        String contentString = getFunctionalArea(ContentConstants.MOBILE, null);
        try {
            Map<String, Object> functionalArea = objectMapper.readValue(contentString, Map.class);
            css = getContentByContentType(ContentConstants.MOBILE, functionalArea, ContentConstants.CSS_KEY);
            if (css != null) {
                css = compressCSS(css);
            }
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_CREATING_A_MAP_CONTENT, contentString);
        }
        return css;
    }

    public String getDesktopJavascript() {
        String js = "";
        String contentString = getFunctionalArea(ContentConstants.ALL_LOWER, null);
        try {
            Map<String, Object> functionalArea = objectMapper.readValue(contentString, Map.class);
            js = getContentByContentType(ContentConstants.ALL_LOWER, functionalArea, ContentConstants.JS_KEY);
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_CREATING_A_MAP_CONTENT, contentString);
        }
        return js;
    }

    public String getMobileJavascript() {
        StringBuffer js = new StringBuffer();
        String contentString = getFunctionalArea(ContentConstants.MOBILE, null);
        try {
            Map<String, Object> functionalArea = objectMapper.readValue(contentString, Map.class);
            js.append(getContentByContentType(ContentConstants.ALL_LOWER, functionalArea, ContentConstants.JS_KEY));
            js.append(getContentByContentType(ContentConstants.MOBILE, functionalArea, ContentConstants.JS_KEY));
        } catch (IOException e) {
            LOGGER.error(ContentConstants.ERROR_CREATING_A_MAP_CONTENT, contentString);
        }
        return js.toString();
    }

    protected String getContentByContentType(String func, Map<String, Object> functionalArea, String contentTypeKey) {
        StringBuffer result = new StringBuffer();
        if (functionalArea.containsKey(func)) {
            expireTagCache();
            functionalArea = getCmsParser().parse(ApiEndpointMapImpl.CMS_API_TYPE, functionalArea);
            Map<String, Object> target = (Map<String, Object>) functionalArea.get(func);
            if (target.containsKey(ContentConstants.ALL_LOWER)) {
                Map<String, Object> contentType = (Map<String, Object>) target.get(ContentConstants.ALL_LOWER);
                if (contentType.containsKey(contentTypeKey)) {
                    List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentType.get(contentTypeKey);
                    for (Map<String, Object> content : contentList) {
                        if (content.containsKey(ContentConstants.CONTENT) && content.containsKey(ContentConstants.FUNC_KEY)
                                && ((List<String>) content.get(ContentConstants.FUNC_KEY)).contains(func)) {
                            Map<String, Object> contentObj = (Map<String, Object>) content.get(ContentConstants.CONTENT);
                            if (contentObj.containsKey(ContentConstants.CONTENT)) {
                                result.append((String) contentObj.get(ContentConstants.CONTENT));
                            }
                        }
                    }
                }
            }
        }
        return result.toString();
    }

    protected String compressCSS(String css) {
        css = css.replaceAll("\\s+\\{", "{");
        css = css.replaceAll("\\{\\s+", "{");
        css = css.replaceAll(":\\s+", ":");
        css = css.replaceAll(";\\s+", ";");
        css = css.replaceAll("\\}\\s+", "}");
        css = css.replaceAll("\\s+\\}", "}");
        return css;
    }

    private Membership getMembership(Long personId, Long memberId) {
        Membership membership = null;
        if (personId != null && memberId != null) {
            if (membershipService.isTeamster(personId)) {
               membership = ssoMembershipService.findByMemberIdAndPersonId(memberId,personId);
            } else {
                membership = membershipService.findByMemberIdAndPersonId(memberId,personId);
            }
        }
        return membership;
    }

    private Person getPerson(Long personId) {
        Person p = null;

        if (personId != null) {
            p = personService.findById(personId);
        }

        return p;
    }

    @Override
    public String getBaseUrl() {
        String baseUrl = null;
        Map<String, Object> baseUrlMap = getLinkContentObject(ApiEndpointMapImpl.CMS_API_TYPE, ContentConstants.BASE_URL_SLUG);
        if (isNotEmpty(baseUrlMap) && baseUrlMap.containsKey(ContentConstants.CONTENT)) {
            Map<String, Object> content = (Map<String, Object>) baseUrlMap.get(ContentConstants.CONTENT);
            if (isNotEmpty(content) && content.containsKey(ContentConstants.URL)) {
                baseUrl = (String) content.get(ContentConstants.URL);
            }
        }
        return baseUrl;
    }

    @Override
    public String getContextPath() {
        String contextPath = null;
        Map<String, Object> contextPathMap = getLinkContentObject(ApiEndpointMapImpl.CMS_API_TYPE, ContentConstants.CONTEXT_PATH_SLUG);
        if (isNotEmpty(contextPathMap) && contextPathMap.containsKey(ContentConstants.CONTENT)) {
            Map<String, Object> content = (Map<String, Object>) contextPathMap.get(ContentConstants.CONTENT);
            if (isNotEmpty(content) && content.containsKey(ContentConstants.URL)) {
                contextPath = (String) content.get(ContentConstants.URL);
            }
        }
        return contextPath;
    }

    @Override
    public String getMessageCenterLink() {
        Map<String, Object> contextPathMap = getLinkContentObject(ApiEndpointMapImpl.CMS_API_TYPE, ContentConstants.MESSAGE_CENTER_LINK_SLUG);
        if (isNotEmpty(contextPathMap) && contextPathMap.containsKey(ContentConstants.CONTENT)) {
            Map<String, Object> content = (Map<String, Object>) contextPathMap.get(ContentConstants.CONTENT);
            if (isNotEmpty(content) && content.containsKey(ContentConstants.URL)) {
                return (String) content.get(ContentConstants.URL);
            }
        }
        return null;
    }

    public String getRegistrationInviteLink() {
        Map<String, Object> contextPathMap = getLinkContentObject(ApiEndpointMapImpl.CMS_API_TYPE, ContentConstants.REGISTRATION_INVITE_SLUG);
        if (isNotEmpty(contextPathMap) && contextPathMap.containsKey(ContentConstants.CONTENT)) {
            Map<String, Object> content = (Map<String, Object>) contextPathMap.get(ContentConstants.CONTENT);
            if (isNotEmpty(content) && content.containsKey(ContentConstants.URL)) {
                return (String) content.get(ContentConstants.URL);
            }
        }
        return null;
    }

    public List<Map<String, Object>> getBenefitsFormDocuments(Long personId, Long memberId) {
        return getBenefitsDocuments(personId, memberId, ContentConstants.FORMS_FUNCTIONAL_AREA, ContentConstants.BENEFITS_DOCS_TARGET);
    }

    public List<Map<String, Object>> getBenefitsPlanDocuments(Long personId, Long memberId) {
        return getBenefitsDocuments(personId, memberId, ContentConstants.COVERAGES_FUNCTIONAL_AREA, ContentConstants.BENEFITS_DOCS_TARGET);
    }

    private List<Map<String, Object>> getBenefitsDocuments(Long personId, Long memberId,
                                                           String functionalArea, String targetLocation) {
        List<Map<String, Object>> formsList = new ArrayList<Map<String, Object>>();
        String cacheKey = buildCmsRequestCacheKey(ApiEndpointMapImpl.BENEFITS_API_TYPE, targetLocation, functionalArea, personId, memberId, false);
        Map<String, Map<String, Object>> cachedForms = getCmsCache().get(cacheKey);
        try {
            if (isEmpty(cachedForms)) {
                if (cachedForms == null) {
                    cachedForms = new HashMap<String, Map<String, Object>>();
                }

                FunctionalArea fa = getFunctionalAreaFromConfig(functionalArea, false);
                if (fa != null) {
                    Map<String, Target> targets = fa.getTargets();
                    if (isNotEmpty(targets) && targets.containsKey(targetLocation)) {
                        Target target = targets.get(targetLocation);
                        if (target != null) {
                            Request request = target.getRequest();
                            List<Attribute> requestOptions = target.getRequestOptions();

                            Person person = getPerson(personId);
                            Membership membership = getMembership(personId, memberId);

                            if (person != null && membership != null) {
                                String cmsRequest = cmsRequestBuilder.buildCmsSearchRequest(request, requestOptions, person, membership);
                                LOGGER.trace(ContentConstants.MAKING_CMS_REQUEST, cmsRequest);

                                try {
                                    String cmsResponse = fetchCmsResponse(cmsRequest);
                                    if (null != cmsResponse && !cmsResponse.isEmpty()) {
                                        Map<String, Object> responseMap = doResponseHandling(cmsResponse, target, targetLocation);
                                        if (isNotEmpty(responseMap)) {
                                            cachedForms.put(functionalArea, responseMap);
                                            LOGGER.debug("CacheKey for put cachedForms into cmsCache: [{}]", cacheKey);
                                            getCmsCache().put(cacheKey, cachedForms);
                                        }
                                    }
                                } catch (RuntimeException e) {
                                    LOGGER.error("Error parsing BenefitsDocuments request for: {} : {}", cmsRequest, e.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            if (cachedForms.containsKey(functionalArea)) {
                Map<String, Object> responseMap = cachedForms.get(functionalArea);
                if (MapUtils.isNotEmpty(responseMap) && responseMap.containsKey(targetLocation)) {
                    Map<String, Object> contentType = (Map<String, Object>) responseMap.get(targetLocation);
                    if (MapUtils.isNotEmpty(contentType) && contentType.containsKey(ContentConstants.FILES)) {
                        List<Map<String, Object>> contentTypeList = (List<Map<String, Object>>) contentType.get(ContentConstants.FILES);
                        formsList.addAll(contentTypeList);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not handle CMS request for benefits forms: {}", e.getMessage());
        }

        return formsList;
    }

    public List<Link> getBenefitsFormLinks(Long personId, Long memberId) {
        return getBenefitsLinks(personId, memberId, ContentConstants.FORMS_FUNCTIONAL_AREA, ContentConstants.BENEFITS_LINKS_TARGET);
    }

    public List<Link> getBenefitsPlanLinks(Long personId, Long memberId) {
        return getBenefitsLinks(personId, memberId, ContentConstants.COVERAGES_FUNCTIONAL_AREA, ContentConstants.BENEFITS_LINKS_TARGET);
    }

    private List<Link> getBenefitsLinks(Long personId, Long memberId,
                                        String functionalArea, String targetLocation) {
        List<Link> linksList = new ArrayList<Link>();

        String cacheKey = buildCmsRequestCacheKey(ApiEndpointMapImpl.BENEFITS_API_TYPE, targetLocation, functionalArea, personId, memberId, false);
        Map<String, Map<String, Object>> cachedLinks = getCmsCache().get(cacheKey);
        try {
            if (isEmpty(cachedLinks)) {
                if (cachedLinks == null) {
                    cachedLinks = new HashMap<String, Map<String, Object>>();
                }
                FunctionalArea fa = getFunctionalAreaFromConfig(functionalArea, false);
                if (fa != null) {
                    Map<String, Target> targets = fa.getTargets();
                    if (isNotEmpty(targets) && targets.containsKey(targetLocation)) {
                        Target target = targets.get(targetLocation);
                        if (target != null) {
                            Request request = target.getRequest();
                            List<Attribute> requestOptions = target.getRequestOptions();

                            Person person = getPerson(personId);
                            Membership membership = getMembership(personId, memberId);

                            if (person != null && membership != null) {
                                String cmsRequest = cmsRequestBuilder.buildCmsSearchRequest(request, requestOptions, person, membership);
                                LOGGER.trace(ContentConstants.MAKING_CMS_REQUEST, cmsRequest);

                                String cmsResponse = fetchCmsResponse(cmsRequest);
                                if (StringUtils.isNotBlank(cmsResponse)) {
                                    Map<String, Object> responseMap = doResponseHandling(cmsResponse, target, targetLocation);

                                    if (responseMap != null && responseMap.containsKey(targetLocation)) {
                                        Map<String, Object> contentType = (Map<String, Object>) responseMap.get(targetLocation);

                                        if (contentType != null && contentType.containsKey(ContentConstants.FILES)) {
                                            List<Map<String, Object>> contentTypeList = (List<Map<String, Object>>) contentType.get(ContentConstants.FILES);
                                            Map<String, Object> cacheMap = new HashMap<String, Object>();

                                            for (Map<String, Object> contentObject : contentTypeList) {
                                                if (contentObject != null && contentObject.containsKey(ContentConstants.CONTENT)) {
                                                    Map<String, Object> contentMap = (Map<String, Object>) contentObject.get(ContentConstants.CONTENT);

                                                    if (contentMap != null && contentMap.containsKey(ContentConstants.PATH_KEY)) {
                                                        String filePath = cmsRequestBuilder.buildCmsFileRequest(ApiEndpointMapImpl.BENEFITS_API_TYPE, (String) contentMap.get(ContentConstants.PATH_KEY));

                                                        if (!cacheMap.containsKey(filePath)) {
                                                            InputStream fileStream = null;
                                                            try {
                                                                fileStream = fetchCmsResponseStream(filePath);
                                                                String[] classList = {"com.bcbsm.mpa.domain.jaxb.mba.Link"};
                                                                GenericXmlDAO<com.bcbsm.mpa.domain.jaxb.mba.Link> linkDAO = new JaxbGenericXMLBindingDAO<com.bcbsm.mpa.domain.jaxb.mba.Link>(classList, fileStream);
                                                                com.bcbsm.mpa.domain.jaxb.mba.Link linkData = linkDAO.getData();
                                                                cacheMap.put(filePath, createLink(linkData, contentMap));
                                                            } catch (Exception e) {
                                                                LOGGER.error("Could not parse link XML: {}", filePath);
                                                            } finally {
                                                                IOUtils.closeQuietly(fileStream);
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (MapUtils.isNotEmpty(cacheMap)) {
                                                cachedLinks.put(functionalArea, cacheMap);
                                                LOGGER.debug("CacheKey for put cachedLinks into cmsCache: [{}]", cacheKey);
                                                getCmsCache().put(cacheKey, cachedLinks);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (cachedLinks.containsKey(functionalArea)) {
                Map<String, Object> cacheMap = cachedLinks.get(functionalArea);
                if (MapUtils.isNotEmpty(cacheMap)) {
                    for (String key : cacheMap.keySet()) {
                        Link link = (Link) cacheMap.get(key);
                        linksList.add(link);
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Could not handle CMS request for benefits links: {}", e.getMessage());
        }
        return linksList;
    }

    protected Link createLink(com.bcbsm.mpa.domain.jaxb.mba.Link linkData, Map<String, Object> contentMap) {
        LinkImpl link = new LinkImpl();

        link.setContentType(ContentConstants.BENEFITS_LINKS_CONTENT_TYPE);
        link.setContentSubType(ContentConstants.LINK_KEY);
        if (contentMap.containsKey(ContentConstants.MODIFIED_DATE_KEY)) {
            link.setModifiedDate((String) contentMap.get(ContentConstants.MODIFIED_DATE_KEY));
        }
        link.setDescription(linkData.getDescription());
        link.setName(linkData.getName());
        link.setSortOrder(linkData.getSortOrder());

        LinkContentImpl linkContent = new LinkContentImpl();
        linkContent.setClasses(linkData.getCss());
        linkContent.setExternal(BooleanUtils.toBoolean(linkData.getExternalWarning()));
        linkContent.setTarget(linkData.getTarget());
        linkContent.setUrl(linkData.getHref());
        linkContent.setId(linkData.getId());
        linkContent.setName(linkData.getName());
        linkContent.setTitle(linkData.getTitle());

        link.setContent(linkContent);

        return link;
    }

    // Spring
    public void setMimeTypeMapper(MimeTypeMapper mimeTypeMapper) {
        this.mimeTypeMapper = mimeTypeMapper;
    }

    public void setCmsRequestBuilder(RequestBuilder cmsRequestBuilder) {
        this.cmsRequestBuilder = cmsRequestBuilder;
    }

    public void setMembershipService(MembershipService membershipService) {
        this.membershipService = membershipService;
    }


    public void setssoMembershipService(MembershipService membershipService) {
        this.ssoMembershipService = membershipService;
    }

    public void setUserProfileDAO(UserProfileDAO userProfileDAO) {
        this.userProfileDAO = userProfileDAO;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setGroupsService(GroupsService groupsService) {
        this.groupsService = groupsService;
    }

    public void setParserConfig(ParserConfig parserConfig) {
        this.parserConfig = parserConfig;
    }

    public void setCmsConfigService(CmsConfigService cmsConfigService) {
        this.cmsConfigService = cmsConfigService;
    }

    public void setCmsParser(Parser cmsParser) {
        this.cmsParser = cmsParser;
    }

    public Parser getCmsParser() {
        return this.cmsParser;
    }

    public void setCacheProxyManager(CacheProxyManager cacheProxyManager) {
        this.cacheProxyManager = cacheProxyManager;
    }

    protected CacheObject<Map<String, Map<String, Object>>> getCmsCache() {
        return cacheProxyManager.getCacheObject(CacheNames.CMS_REQUEST_CACHE);
    }

    protected CacheObject<Map<String, Object>> getCmsSlugCache() {
        return cacheProxyManager.getCacheObject(CacheNames.CMS_SLUG_CACHE);
    }

    public String getDataPowerPass() {
        return dataPowerPass;
    }

    public void setDataPowerPass(String pass) {
        this.dataPowerPass = pass;
    }

    public String getDataPowerUser() {
        return dataPowerUser;
    }

    public void setDataPowerUser(String user) {
        this.dataPowerUser = user;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String fetchCmsResponse(String cmsRequest) {
        String cmsResponse = null;
        try {
            cmsRequest = URLDecoder.decode(cmsRequest, "UTF-8");
            cmsResponse = restTemplate.getForObject(cmsRequest, String.class);
        } catch (Exception e) {
            LOGGER.error("Error while fetching cms response {}", e.getMessage());
        }
        return cmsResponse;
    }

    public InputStream fetchCmsResponseStream(String cmsRequest) {
        InputStream responseInputStream = null;
        try {
            cmsRequest = URLDecoder.decode(cmsRequest, "UTF-8");
            ResponseEntity<Resource> responseEntity = restTemplate.exchange(cmsRequest, HttpMethod.GET, HttpEntity.EMPTY, Resource.class);
            responseInputStream = responseEntity.getBody().getInputStream();
        } catch (IOException e) {
            LOGGER.error("Error while fetching cms response stream {}", e.getMessage());
        }
        return responseInputStream;
    }

    public RequestBuilder getCmsRequestBuilder() {
        return cmsRequestBuilder;
    }

}

