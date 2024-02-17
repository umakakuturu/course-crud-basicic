package com.bcbsm.mbp.cms.parser.impl;

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

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
    public void init(){
        setTagName("doc");
    }

    @Override
    public String processTag(String apiType, Map<String, String> params) {
        String mainTagValue = params.get(getTagName());
        Boolean switchContext = params.containsKey(SWITCH_CONTEXT) && params.get(SWITCH_CONTEXT).equals("true");
        Boolean isRelativePath = params.containsKey(RELATIVE_PATH) && params.get(RELATIVE_PATH).equals("true");

        if (null != mainTagValue) {
            Map<String, Object> content = getContentManagementService().getFileContentObject(apiType, mainTagValue);
            if (null != content) {
                return makeMarkupForDocument( params, mainTagValue, content, switchContext, isRelativePath);
            }
        } else if( targetedNameExists( params, PATH ) ) {
            return getUrlForDocument( getString( params, makeTargetedTagName(PATH)), switchContext, isRelativePath);
        }

        return null;
    }

    private String makeMarkupForDocument( Map<String,String> params, String documentId, Map<String, Object> content, Boolean switchContext, Boolean isRelativePath) {
        String displayName = getDisplayName(params, content);
        String url = getUrlForDocument(documentId, switchContext, isRelativePath);
        return String.format(TEMPLATE, url, displayName);
    }

    private String getDisplayName( Map<String,String> params, Map<String,Object> content ) {
        String displayName = getString( params, DISPLAY_NAME );
        if( StringUtils.isEmpty(displayName) ) {
            displayName = getString( content, DISPLAY_NAME );
        }

        return displayName;
    }

}
