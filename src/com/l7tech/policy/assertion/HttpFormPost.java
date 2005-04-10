package com.l7tech.policy.assertion;

import java.io.Serializable;

/**
 * Extracts fields from HTTP POSTed forms and replaces MIME parts in the current message
 */
public class HttpFormPost extends Assertion {
    private FieldInfo[] fieldInfos = new FieldInfo[0];

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
}
