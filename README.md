  public String processTag(String apiType, Map<String, String> params) {
        LOGGER.trace("params: {}", params);
        String mainTagValue = params.get(getTagName());
        LOGGER.trace("mainTagValue: {}", mainTagValue);
        Boolean switchContext = params.containsKey(SWITCH_CONTEXT) && params.get(SWITCH_CONTEXT).equals("true");
        LOGGER.trace("switchContext: {}", switchContext);
        Boolean isRelativePath = params.containsKey(RELATIVE_PATH) && params.get(RELATIVE_PATH).equals("true");
        LOGGER.trace("relativePath: {}", isRelativePath);


        if (null != mainTagValue) {
            Map<String, Object> contentObject = getContentManagementService().getImageContentObject(apiType, mainTagValue);
            return buildMarkupFromContent(contentObject, mainTagValue, switchContext, isRelativePath);
        } else if (targetedNameExists(params, PATH)) {
            return getUrlForDocument(getString(params, makeTargetedTagName(PATH)), switchContext, isRelativePath);
        }

        return null;
    }

    /**
     * WARNING: Scope is intentionally increased to protected only to facilitate testing. This method is not meant to be called
     * outside of this class.
     */
    protected String buildMarkupFromContent(Map<String, Object> contentObject, String mainTagValue, Boolean switchContext, Boolean isRelativePath) {
        LOGGER.trace("contentObject: {}", contentObject);
        if (isNotEmpty(contentObject)) {
            String description = getString(contentObject, DESCRIPTION);
            if (description == null) {
                description = "";
            }
            LOGGER.trace("description: {}", description);

            Map<String, String> mainContentObj = (Map<String, String>) getMap(contentObject, CONTENT_KEY);
            LOGGER.trace("mainContentObj: {}", mainContentObj);
            String cssClasses = getString(mainContentObj, "classes");
            LOGGER.trace("cssClasses: {}", cssClasses);

            return buildImageMarkup(mainTagValue, description, cssClasses, switchContext, isRelativePath);
        }

        return null;
    }

    /**
     * WARNING: Scope is intentionally increased to protected only to facilitate testing. This method is not meant to be called
     * outside of this class.
     */
    protected String buildImageMarkup(String mainTagValue, String description, String cssClasses, Boolean switchContext, Boolean isRelativePath) {
        LOGGER.trace("mainTagValue: {}", mainTagValue);
        LOGGER.trace("description: {}", description);
        LOGGER.trace("cssClasses: {}", cssClasses);
        String path = getUrlForDocument(mainTagValue, switchContext, isRelativePath);
        LOGGER.trace("path: {}", path);

        String markup;
        if (null != cssClasses) {
            markup = String.format(CSS_TEMPLATE, path, description, cssClasses);
        } else {
            markup = String.format(TEMPLATE, path, description);
        }
        LOGGER.trace("markup: {}", markup);
        return markup;
    }

}
