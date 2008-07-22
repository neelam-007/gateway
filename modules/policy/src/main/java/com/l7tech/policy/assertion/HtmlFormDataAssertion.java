/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import java.io.Serializable;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.ProcessesMultipart;

/**
 * Provides validation of HTML Form data set in an incoming request.
 *
 * <p><b>Note:</b> Use the comparison assertion (revised in 3.7) to constrain values.
 *
 * <p><i>JDK compatibility:</i> 1.4
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
@ProcessesMultipart
@ProcessesRequest
public class HtmlFormDataAssertion extends Assertion {

    /**
     * A FieldSpec specifies the constraints on a HTML Form field.
     */
    public static class FieldSpec implements Serializable {
        private String _name;
        private HtmlFormDataType _dataType;
        private int _minOccurs;
        private int _maxOccurs;
        private HtmlFormDataLocation _allowedLocation = HtmlFormDataLocation.ANYWHERE;

        public FieldSpec() {
        }

        public FieldSpec(final String name,
                         final HtmlFormDataType dataType,
                         final int minOccurs,
                         final int maxOccurs,
                         final HtmlFormDataLocation allowedLocation) {
            _name = name;
            _dataType = dataType;
            _minOccurs = minOccurs;
            _maxOccurs = maxOccurs;
            _allowedLocation = allowedLocation;
        }

        public String getName() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }

        public HtmlFormDataType getDataType() {
            return _dataType;
        }

        public void setDataType(HtmlFormDataType dataType) {
            _dataType = dataType;
        }

        public int getMinOccurs() {
            return _minOccurs;
        }

        public void setMinOccurs(final int n) {
            _minOccurs = n;
        }

        public int getMaxOccurs() {
            return _maxOccurs;
        }

        public void setMaxOccurs(final int n) {
            _maxOccurs = n;
        }

        public HtmlFormDataLocation getAllowedLocation() {
            return _allowedLocation;
        }

        public void setAllowedLocation(final HtmlFormDataLocation allowedLocation) {
            _allowedLocation = allowedLocation;
        }
    }

    /** Whether HTTP GET method is allowed. */
    private boolean _allowGet;

    /** Whether HTTP POST method is allowed. */
    private boolean _allowPost;

    /** All specified Form fields; as a set of {@link FieldSpec} objects. */
    private FieldSpec[] _fieldSpecs = new FieldSpec[0];

    /** Whether to disallow fields not specified here. */
    private boolean _disallowOtherFields;

    public boolean isAllowGet() {
        return _allowGet;
    }

    public void setAllowGet(boolean allowGet) {
        _allowGet = allowGet;
    }

    public boolean isAllowPost() {
        return _allowPost;
    }

    public void setAllowPost(boolean allowPost) {
        _allowPost = allowPost;
    }

    /** @return true if disallow fields not specified here */
    public boolean isDisallowOtherFields() {
        return _disallowOtherFields;
    }

    /**
     * Sets whether to disallow fields not specified here
     * @param b     true if disallow fields not specified here
     */
    public void setDisallowOtherFields(final boolean b) {
        _disallowOtherFields = b;
    }

    /** @return an array of {@link FieldSpec}s backed by the assertion; never null */
    public FieldSpec[] getFieldSpecs() {
        return _fieldSpecs;
    }

    /** @param fieldSpecs   array of {@link FieldSpec}s for this assertion; must be non-null */
    public void setFieldSpecs(final FieldSpec[] fieldSpecs) {
        if (fieldSpecs == null)
            throw new IllegalArgumentException("fields must be non-null");
        _fieldSpecs = fieldSpecs;
    }
}
