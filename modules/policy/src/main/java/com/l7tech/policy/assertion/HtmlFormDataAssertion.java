/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.ProcessesMultipart;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides validation of HTML Form data set in an incoming request.
 * <p/>
 * <p><b>Note:</b> Use the comparison assertion (revised in 3.7) to constrain values.
 * <p/>
 * <p><i>JDK compatibility:</i> 1.4
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
@ProcessesMultipart
@ProcessesRequest
public class HtmlFormDataAssertion extends Assertion {

    /**
     * For supporting backwards compatibility - before allowEmpty field was added, only number data type restricted empty values
     */
    public static final Map<HtmlFormDataType, Boolean> ALLOW_EMPTY_BY_DATA_TYPE;

    static {
        ALLOW_EMPTY_BY_DATA_TYPE = new HashMap<HtmlFormDataType, Boolean>();
        ALLOW_EMPTY_BY_DATA_TYPE.put(HtmlFormDataType.ANY, true);
        ALLOW_EMPTY_BY_DATA_TYPE.put(HtmlFormDataType.STRING, true);
        ALLOW_EMPTY_BY_DATA_TYPE.put(HtmlFormDataType.FILE, true);
        ALLOW_EMPTY_BY_DATA_TYPE.put(HtmlFormDataType.NUMBER, false);
    }

    /**
     * A FieldSpec specifies the constraints on a HTML Form field.
     */
    public static class FieldSpec implements Serializable, Cloneable {
        private String name;
        private HtmlFormDataType dataType;
        private int minOccurs;
        private int maxOccurs;
        private HtmlFormDataLocation allowedLocation = HtmlFormDataLocation.ANYWHERE;
        // Set to true if the field must be present but can have an empty value.
        // allowEmpty is nullable to support backwards compatibility
        private Boolean allowEmpty;

        public FieldSpec() {
        }

        public FieldSpec(final String name,
                         final HtmlFormDataType dataType,
                         final int minOccurs,
                         final int maxOccurs,
                         final HtmlFormDataLocation allowedLocation, final Boolean allowEmpty) {
            this.name = name;
            this.dataType = dataType;
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
            this.allowedLocation = allowedLocation;
            this.allowEmpty = allowEmpty;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public HtmlFormDataType getDataType() {
            return dataType;
        }

        public void setDataType(HtmlFormDataType dataType) {
            this.dataType = dataType;
        }

        public int getMinOccurs() {
            return minOccurs;
        }

        public void setMinOccurs(final int n) {
            minOccurs = n;
        }

        public int getMaxOccurs() {
            return maxOccurs;
        }

        public void setMaxOccurs(final int n) {
            maxOccurs = n;
        }

        public HtmlFormDataLocation getAllowedLocation() {
            return allowedLocation;
        }

        public void setAllowedLocation(final HtmlFormDataLocation allowedLocation) {
            this.allowedLocation = allowedLocation;
        }

        public Boolean getAllowEmpty() {
            return allowEmpty;
        }

        public void setAllowEmpty(final Boolean allowEmpty) {
            this.allowEmpty = allowEmpty;
        }
    }

    /**
     * Whether HTTP GET method is allowed.
     */
    private boolean allowGet;

    /**
     * Whether HTTP POST method is allowed.
     */
    private boolean allowPost;

    /**
     * All specified Form fields; as a set of {@link FieldSpec} objects.
     */
    private FieldSpec[] fieldSpecs = new FieldSpec[0];

    /**
     * Whether to disallow fields not specified here.
     */
    private boolean disallowOtherFields;

    @Override
    public Object clone() {
        final HtmlFormDataAssertion clone = (HtmlFormDataAssertion) super.clone();
        clone.fieldSpecs = fieldSpecs.clone();
        return clone;
    }

    public boolean isAllowGet() {
        return allowGet;
    }

    public void setAllowGet(boolean allowGet) {
        this.allowGet = allowGet;
    }

    public boolean isAllowPost() {
        return allowPost;
    }

    public void setAllowPost(boolean allowPost) {
        this.allowPost = allowPost;
    }

    /**
     * @return true if disallow fields not specified here
     */
    public boolean isDisallowOtherFields() {
        return disallowOtherFields;
    }

    /**
     * Sets whether to disallow fields not specified here
     *
     * @param b true if disallow fields not specified here
     */
    public void setDisallowOtherFields(final boolean b) {
        disallowOtherFields = b;
    }

    /**
     * @return an array of {@link FieldSpec}s backed by the assertion; never null
     */
    public FieldSpec[] getFieldSpecs() {
        return fieldSpecs;
    }

    /**
     * @param fieldSpecs array of {@link FieldSpec}s for this assertion; must be non-null
     */
    public void setFieldSpecs(final FieldSpec[] fieldSpecs) {
        if (fieldSpecs == null)
            throw new IllegalArgumentException("fields must be non-null");
        this.fieldSpecs = fieldSpecs;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");
        meta.put(AssertionMetadata.SHORT_NAME, "Validate HTML Form Data");
        meta.put(AssertionMetadata.DESCRIPTION, "Validate an HTML Form data set. Works only on HTTP requests.");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "HTML Form Data Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.HtmlFormDataAssertionPropertiesAction");


        return meta;
    }
}
