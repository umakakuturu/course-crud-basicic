package com.bcbsm.mbp.cms.parser.impl;

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.collections4.MapUtils.getString;

@Component
public class CmsDocumentParserStrategy extends AbstractCmsContentParserStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmsDocumentParserStrategy.class);

    private static final String PATH = "path";
    private static final String SWITCH_CONTEXT = "email";
    private static final String RELATIVE_PATH = "relative-path";
    private static final String DISPLAY_NAME = "title";
    private static final String TEMPLATE = "<a href=\"%s\" target=\"_blank\">%s</a>";

    @PostConstruct
    public void init() {
        setTagName("doc");
    }

    @Override
    public String processTag(String apiType, Map<String, String> params) {
        return Optional.ofNullable(params.get(getTagName()))
                .map(mainTagValue -> {
                    boolean switchContext = Boolean.parseBoolean(params.getOrDefault(SWITCH_CONTEXT, "false"));
                    boolean isRelativePath = Boolean.parseBoolean(params.getOrDefault(RELATIVE_PATH, "false"));

                    return Optional.ofNullable(getContentManagementService().getFileContentObject(apiType, mainTagValue))
                            .map(content -> makeMarkupForDocument(params, mainTagValue, content, switchContext, isRelativePath))
                            .orElse(null);
                })
                .orElseGet(() -> Optional.ofNullable(getString(params, makeTargetedTagName(PATH)))
                        .map(path -> getUrlForDocument(path, Boolean.parseBoolean(params.getOrDefault(SWITCH_CONTEXT, "false")), Boolean.parseBoolean(params.getOrDefault(RELATIVE_PATH, "false"))))
                        .orElse(null));
    }

    private String makeMarkupForDocument(Map<String, String> params, String documentId, Map<String, Object> content, boolean switchContext, boolean isRelativePath) {
        String displayName = getDisplayName(params, content);
        String url = getUrlForDocument(documentId, switchContext, isRelativePath);
        return String.format(TEMPLATE, url, displayName);
    }

    private String getDisplayName(Map<String, String> params, Map<String, Object> content) {
        return StringUtils.isEmpty(getString(params, DISPLAY_NAME)) ? getString(content, DISPLAY_NAME) : getString(params, DISPLAY_NAME);
    }
}
