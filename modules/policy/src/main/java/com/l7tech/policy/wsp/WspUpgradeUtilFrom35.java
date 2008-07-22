/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.wsp;

import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import org.w3c.dom.Element;

/**
 * Utilities for reading a SecureSpan 3.5 policy, which might refer to assertions whose classes no longer exist.
 */
class WspUpgradeUtilFrom35 {
    /**
     * A wrapper visitor that knows how to correct for the old 3.5 property names on XslTransformation.
     */
    static class XslTransformationPropertyVisitor extends WspVisitorWrapper {
        public XslTransformationPropertyVisitor(WspVisitor originalVisitor) {
            super(originalVisitor);
        }

        @Override
        public void unknownProperty(Element originalObject,
                                    Element problematicParameter,
                                    Object deserializedObject,
                                    String propertyName,
                                    TypedReference value,
                                    Throwable problemEncountered)
                throws InvalidPolicyStreamException {
            if (deserializedObject instanceof XslTransformation) {
                XslTransformation xslt = (XslTransformation) deserializedObject;
                if ("XslSrc".equals(propertyName)) {
                    if (value.type != String.class)
                        throw new InvalidPolicyStreamException("XslSrc in 3.5 can only be a String");

                    String doc = (String) value.target;
                    if (doc != null && doc.length() > 0) {
                        AssertionResourceInfo rinfo = xslt.getResourceInfo();
                        StaticResourceInfo sri;
                        if (rinfo instanceof StaticResourceInfo) {
                            sri = (StaticResourceInfo) rinfo;
                        } else {
                            sri = new StaticResourceInfo();
                            xslt.setResourceInfo(sri);
                        }
                        sri.setDocument(doc);
                    }
                    return;
                } else if ("FetchUrlRegexes".equals(propertyName)) {
                    if (value.type != String[].class)
                        throw new InvalidPolicyStreamException("FetchUrlRegexes in 3.5 can only be String[]");

                    String[] regexes = (String[]) value.target;
                    if (regexes != null && regexes.length > 0) {
                        AssertionResourceInfo rinfo = xslt.getResourceInfo();
                        MessageUrlResourceInfo muri;
                        if (rinfo instanceof MessageUrlResourceInfo) {
                            muri = (MessageUrlResourceInfo) rinfo;
                        } else {
                            muri = new MessageUrlResourceInfo(regexes);
                            xslt.setResourceInfo(muri);
                        }
                        muri.setUrlRegexes(regexes);
                    }
                    return;
                } else if ("FetchAllowWithoutStylesheet".equals(propertyName)) {
                    if (value.type != Boolean.TYPE)
                        throw new InvalidPolicyStreamException("FetchAllowWithoutStylesheet in 3.5 can only be boolean");

                    Boolean allow = (Boolean) value.target;
                    // defaults to false
                    if (allow != null && allow.booleanValue()) {
                        AssertionResourceInfo rinfo = xslt.getResourceInfo();
                        MessageUrlResourceInfo muri;
                        if (rinfo instanceof MessageUrlResourceInfo) {
                            muri = (MessageUrlResourceInfo) rinfo;
                        } else {
                            muri = new MessageUrlResourceInfo();
                            xslt.setResourceInfo(muri);
                        }
                        muri.setAllowMessagesWithoutUrl(allow.booleanValue());
                    }
                    return;
                } else if ("FetchXsltFromMessageUrls".equals(propertyName)) {
                    if (value.type != Boolean.TYPE)
                        throw new InvalidPolicyStreamException("FetchXsltFromMessageUrls in 3.5 can only be boolean");

                    Boolean fetch = (Boolean) value.target;
                    // defaults to false
                    if (fetch != null && fetch.booleanValue()) {
                        AssertionResourceInfo rinfo = xslt.getResourceInfo();
                        MessageUrlResourceInfo muri;
                        if (!(rinfo instanceof MessageUrlResourceInfo)) {
                            muri = new MessageUrlResourceInfo();
                            xslt.setResourceInfo(muri);
                        }
                    }
                    return;
                }
            }

            super.unknownProperty(originalObject, problematicParameter, deserializedObject, propertyName, value, problemEncountered);
        }
    }

    /**
     * A wrapper visitor that knows how to correct for the old 3.5 property names on SchemaValidation.
     */
    static class SchemaValidationPropertyVisitor extends WspVisitorWrapper {

        public SchemaValidationPropertyVisitor(WspVisitor originalVisitor) {
            super(originalVisitor);
        }

        @Override
        public void unknownProperty(Element originalObject,
                                    Element problematicParameter,
                                    Object deserializedObject,
                                    String propertyName,
                                    TypedReference value,
                                    Throwable problemEncountered)
                throws InvalidPolicyStreamException
        {
            if (deserializedObject instanceof SchemaValidation) {
                SchemaValidation sv = (SchemaValidation) deserializedObject;
                if ("Schema".equals(propertyName)) {
                    if (value.type != String.class)
                        throw new InvalidPolicyStreamException("Schema in 3.5 can only be a String");

                    String doc = (String) value.target;
                    if (doc != null && doc.length() > 0) {
                        AssertionResourceInfo rinfo = sv.getResourceInfo();
                        StaticResourceInfo sri;
                        if (rinfo instanceof StaticResourceInfo) {
                            sri = (StaticResourceInfo) rinfo;
                        } else {
                            sri = new StaticResourceInfo();
                            sv.setResourceInfo(sri);
                        }
                        sri.setDocument(doc);
                    }
                    return;
                }
            }
            super.unknownProperty(originalObject, problematicParameter, deserializedObject, propertyName, value, problemEncountered);
        }
    }
}
