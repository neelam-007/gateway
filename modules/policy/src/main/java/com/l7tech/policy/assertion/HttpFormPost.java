package com.l7tech.policy.assertion;

import java.io.Serializable;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 * Extracts fields from an HTML form submission and constructs MIME parts in the current
 * request out of them.  The request must have been received via HTTP.
 * <p>
 * <b>NOTE</b>: This assertion destroys the current request and replaces it
 * with new content!
 */
@ProcessesRequest
public class HttpFormPost extends Assertion {
    private FieldInfo[] fieldInfos = new FieldInfo[0];
    public static final String X_WWW_FORM_URLENCODED = "x-www-form-urlencoded";

    /**
     * @return the array of {@link FieldInfo}s for this assertion. Never null.
     */
    public FieldInfo[] getFieldInfos() {
        return fieldInfos;
    }

    /**
     * @param fieldInfos the array of {@link FieldInfo}s for this assertion. Must be non-null.
     */
    public void setFieldInfos(FieldInfo[] fieldInfos) {
        if (fieldInfos == null) throw new IllegalArgumentException("fieldInfos must be non-null");
        this.fieldInfos = fieldInfos;
    }

    public static class FieldInfo implements Serializable {
        protected String fieldname;
        protected String contentType;

        public FieldInfo() {
        }

        public FieldInfo(String fieldname, String contentType) {
            this.fieldname = fieldname;
            this.contentType = contentType;
        }

        public String getFieldname() {
            return fieldname;
        }

        public void setFieldname(String fieldname) {
            this.fieldname = fieldname;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }

    private final static String baseName = "Translate HTTP Form to MIME";
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<HttpFormPost>(){
        @Override
        public String getAssertionName( final HttpFormPost assertion, final boolean decorate) {
            return baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Translate a POSTed HTTP Form submission into a multipart MIME message suitable for processing by the Gateway.");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/network.gif");

        //not needed at the moment, but setting up in case this assertion's name needs to be decorated
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.HttpFormPostPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "HTTP Form to MIME Translation Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/network.gif");

        return meta;
    }
}
