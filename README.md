  @Override
    public String processTag(String apiType, Map<String, String> params) {
        LOGGER.trace("params: {}", params);

        String mainTagValue = params.get(getTagName());
        LOGGER.trace("mainTagValue: {}", mainTagValue);

        boolean switchContext = params.containsKey(SWITCH_CONTEXT) && Boolean.parseBoolean(params.get(SWITCH_CONTEXT));
        LOGGER.trace("switchContext: {}", switchContext);

        boolean isRelativePath = params.containsKey(RELATIVE_PATH) && Boolean.parseBoolean(params.get(RELATIVE_PATH));
        LOGGER.trace("relativePath: {}", isRelativePath);

        return Optional.ofNullable(mainTagValue)
                .map(tagValue -> {
                    Map<String, Object> contentObject = getContentManagementService().getImageContentObject(apiType, tagValue);
                    return buildMarkupFromContent(contentObject, tagValue, switchContext, isRelativePath);
                })
                .orElseGet(() -> Optional.ofNullable(getString(params, makeTargetedTagName(PATH)))
                        .map(path -> getUrlForDocument(path, switchContext, isRelativePath))
                        .orElse(null));
    }

    protected String buildMarkupFromContent(Map<String, Object> contentObject, String mainTagValue, boolean switchContext, boolean isRelativePath) {
        LOGGER.trace("contentObject: {}", contentObject);
        return Optional.ofNullable(contentObject)
                .filter(this::isNotEmpty)
                .map(content -> {
                    String description = getString(content, DESCRIPTION, "");
                    LOGGER.trace("description: {}", description);

                    Map<String, String> mainContentObj = (Map<String, String>) getMap(content, CONTENT_KEY);
                    LOGGER.trace("mainContentObj: {}", mainContentObj);
                    String cssClasses = getString(mainContentObj, "classes");
                    LOGGER.trace("cssClasses: {}", cssClasses);

                    return buildImageMarkup(mainTagValue, description, cssClasses, switchContext, isRelativePath);
                })
                .orElse(null);
    }

    protected String buildImageMarkup(String mainTagValue, String description, String cssClasses, boolean switchContext, boolean isRelativePath) {
        LOGGER.trace("mainTagValue: {}", mainTagValue);
        LOGGER.trace("description: {}", description);
        LOGGER.trace("cssClasses: {}", cssClasses);
        String path = getUrlForDocument(mainTagValue, switchContext, isRelativePath);
        LOGGER.trace("path: {}", path);

        String markup = Optional.ofNullable(cssClasses)
                .map(classes -> String.format(CSS_TEMPLATE, path, description, classes))
                .orElse(String.format(TEMPLATE, path, description));

        LOGGER.trace("markup: {}", markup);
        return markup;
    }
}
