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
