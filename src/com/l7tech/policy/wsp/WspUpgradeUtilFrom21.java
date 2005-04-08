/*
* Copyright (C) 2004 Layer 7 Technologies Inc.
*
* $Id$
*/

package com.l7tech.policy.wsp;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.xmlsec.*;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Utilities for reading a SecureSpan 2.1 policy, which might refer to assertions whose classes no longer exist.
 */
class WspUpgradeUtilFrom21 {

    static final TypeMapping xmlRequestSecurityCompatibilityMapping = new XmlSecurityTypeMapping("XmlRequestSecurity");
    static final TypeMapping xmlResponseSecurityCompatibilityMapping = new XmlSecurityTypeMapping("XmlResponseSecurity");

    static class XmlSecurityTypeMapping implements TypeMapping {
        private final String externalName;

        public XmlSecurityTypeMapping(String externalName) {
            this.externalName = externalName;
        }

        public Class getMappedClass() {
            return Object.class;
        }

        public String getExternalName() {
            return externalName;
        }

        public Element freeze(TypedReference object, Element container) {
            // This can't actually happen
            throw new InvalidPolicyTreeException("Can only read old XmlRequestSecurity and XmlResponseSecurity assertions -- unable to create them");
        }

        public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
            Element translated = WspUpgradeUtilFrom21.translateOldXmlSecurityElement(source);
            return TypeMappingUtils.thawElement(translated, visitor);
        }
    }

    /** Upgrade utilities for SSG 2.1 XmlRequestSecurity and XmlResponseSecurity. */
    static void setOldXpathProperty(XpathBasedAssertion xba, String propertyName, TypedReference value) throws InvalidPolicyStreamException {

        throw new InvalidPolicyStreamException("Unable to translate property " + propertyName + " of " + xba.getClass());
    }


    /*
    When translating XML security from 2.1 to 3.0, things get a little bit complicated.

    Something like this:

    XmlReqSecurity
    /buystock -> sign/encrypt /buystock/buystockrequest
    /getbalance-> sign /getbalance/getbalancerequest

    translates into this:

    or
    all
    requestxpath /buystock
    requestintegrity /buystock/buystockrequest
    requestconfidentiality /buystock/buystockrequest
    all
    requestxpath /getbalance
    requestintegrity /getbalance/getbalancerequest
    requestxpath not((/buystock)|(/getbalance))

    BUT, something like this:

    XmlReqSecurity
    null -> sign/encrypt /Foo

    translates into this:

    all
    requestWssx509Cert
    or
    all
    requestintegrity /Foo
    requestconfidentiality /Foo
    requestxpath not((/Foo))

    */

    /**
     * Converts the specified element, which represents a
     * @param in
     * @return
     * @throws InvalidPolicyStreamException
     */
    static Element translateOldXmlSecurityElement(Element in) throws InvalidPolicyStreamException {
        final boolean isResponse;
        final String origName = in.getLocalName();
        if ("XmlRequestSecurity".equals(origName)) {
            isResponse = false;
        } else if ("XmlResponseSecurity".equals(origName)) {
            isResponse = true;
        } else
            throw new RuntimeException("XmlSecurityTranslator cannot handle element " + origName); // can't happen

        // Prepare a no-op
        OneOrMoreAssertion enforcementOr = new OneOrMoreAssertion();
        boolean isCredentialSource = false;

        try {
            List seenPreconditions = new ArrayList();
            Map seenNamespaceMap = null;
            Element elements = XmlUtil.findOnlyOneChildElementByName(in, in.getNamespaceURI(), "Elements");
            List items = elements == null ? Collections.EMPTY_LIST :
                    XmlUtil.findChildElementsByName(elements, elements.getNamespaceURI(), "item");
            for (Iterator i = items.iterator(); i.hasNext();) {
                Element item = (Element)i.next();
                // Ignore Cipher and KeyLength -- was always AES128 in 2.1
                Element encryptionEl = XmlUtil.findOnlyOneChildElementByName(item, item.getNamespaceURI(), "Encryption");
                boolean encryption = encryptionEl != null && "true".equalsIgnoreCase(encryptionEl.getAttribute("booleanValue").trim());
                Element preconditionXpath = XmlUtil.findOnlyOneChildElementByName(item, item.getNamespaceURI(), "PreconditionXpath");
                Element elementXpath = XmlUtil.findOnlyOneChildElementByName(item, item.getNamespaceURI(), "ElementXpath");
                if (preconditionXpath == null || elementXpath == null)
                    throw new InvalidPolicyStreamException("Invalid 2.1 policy - " + origName + " requires both PreconditionXpath and ElementXpath");
                TypedReference pxref = TypeMappingUtils.thawElement(preconditionXpath, StrictWspVisitor.INSTANCE);
                final AllAssertion itemAll = new AllAssertion();
                enforcementOr.addChild(itemAll);
                if (pxref.target != null) {
                    // We get precondition
                    if (!(pxref.target instanceof XpathExpression))
                        throw new InvalidPolicyStreamException("Invalid 2.1 policy - " + origName + " PreconditionXpath must be an XpathExpression");
                    XpathExpression pxpath = (XpathExpression)pxref.target;

                    final XpathBasedAssertion preconditionAss;
                    if (isResponse)
                        preconditionAss = new ResponseXpathAssertion();
                    else
                        preconditionAss = new RequestXpathAssertion();
                    preconditionAss.setXpathExpression(pxpath);
                    itemAll.addChild(preconditionAss);
                    seenPreconditions.add(pxpath.getExpression());
                    if (pxpath.getNamespaces() != null)
                        seenNamespaceMap = pxpath.getNamespaces();
                }

                TypedReference exref = TypeMappingUtils.thawElement(elementXpath, StrictWspVisitor.INSTANCE);
                if (exref.target == null)
                    throw new InvalidPolicyStreamException("Invalid 2.1 policy - " + origName + " ElementXpath may not be null");

                if (!(exref.target instanceof XpathExpression))
                    throw new InvalidPolicyStreamException("Invalid 2.1 policy - " + origName + " ElementXpath must be an XpathExpression");
                XpathExpression expath = (XpathExpression)exref.target;
                if (expath.getNamespaces() != null)
                    seenNamespaceMap = expath.getNamespaces();

                boolean expathIsSoapenv = SoapUtil.SOAP_ENVELOPE_XPATH.equals(expath.getExpression());
                if (expathIsSoapenv) {
                    if (pxref.target == null)
                        isCredentialSource = true;
                }

                final XpathBasedAssertion elementSignatureAss;
                if (isResponse)
                    elementSignatureAss = new ResponseWssIntegrity();
                else
                    elementSignatureAss = new RequestWssIntegrity();
                elementSignatureAss.setXpathExpression(expath);
                itemAll.addChild(elementSignatureAss);

                if (encryption) {
                    final XpathBasedAssertion elementEncryptionAss;
                    if (isResponse)
                        elementEncryptionAss = new ResponseWssConfidentiality();
                    else
                        elementEncryptionAss = new RequestWssConfidentiality();
                    if (expathIsSoapenv) {
                        // Bug #1310: Head off attempts to encrypt the entire envelope.
                        // In version 2.1, sign+encrypt /soapenv:Envelope actually
                        // meant to encrypt the Body and sign the Envelope.
                        elementEncryptionAss.setXpathExpression(new XpathExpression(SoapUtil.SOAP_BODY_XPATH,
                                                                                    expath.getNamespaces()));
                    } else
                        elementEncryptionAss.setXpathExpression(expath);
                    itemAll.addChild(elementEncryptionAss);
                }
            }

            if (seenPreconditions.size() > 0) {
                final XpathBasedAssertion notGate;
                if (isResponse)
                    notGate = new ResponseXpathAssertion();
                else
                    notGate = new RequestXpathAssertion();
                String expr = HexUtils.join(new StringBuffer("not(("), ")|(",
                                            (CharSequence[])seenPreconditions.toArray(new String[0])).append("))").toString();
                XpathExpression xp = new XpathExpression(expr, seenNamespaceMap);
                notGate.setXpathExpression(xp);
                enforcementOr.addChild(notGate);
            }
        } catch (TooManyChildElementsException e) {
            throw new InvalidPolicyStreamException("Invalid 2.1 policy", e);
        }

        // Prevent empty OR assertion
        Assertion enforcement = enforcementOr.getChildren().size() > 0 ? (Assertion)enforcementOr : new TrueAssertion();

        AllAssertion root = new AllAssertion();
        if (isCredentialSource && !isResponse)
            root.addChild(new RequestWssX509Cert());
        root.addChild(enforcement);
        return WspWriter.toElement(root);
    }

    /** A wrapper visitor that knows how to correct for the old 2.1 property names on RequestXpathAssertion. */
    static class RequestXpathAssertionPropertyVisitor implements WspVisitor {
        private final WspVisitor originalVisitor;

        public RequestXpathAssertionPropertyVisitor(WspVisitor originalVisitor) {
            this.originalVisitor = originalVisitor;
        }

        public Element invalidElement(Element problematicElement, Exception problemEncountered) throws InvalidPolicyStreamException {
            return originalVisitor.invalidElement(problematicElement, problemEncountered);
        }

        public void unknownProperty(Element originalObject,
                                    Element problematicParameter,
                                    Object deserializedObject,
                                    String propertyName,
                                    TypedReference value,
                                    Exception problemEncountered)
                throws InvalidPolicyStreamException
        {
            if (deserializedObject instanceof XpathBasedAssertion) {
                XpathBasedAssertion xba = (XpathBasedAssertion)deserializedObject;
                if ("NamespaceMap".equals(propertyName)) {
                    if (value.type != Map.class)
                        throw new InvalidPolicyStreamException("NamespaceMap in 2.1 can only be a Map");
                    Map namespaceMap = (Map)value.target;
                    XpathExpression xe = xba.getXpathExpression();
                    if (xe == null)
                        xba.setXpathExpression(xe = new XpathExpression(null, namespaceMap));
                    else
                        xe.setNamespaces(namespaceMap);
                    return;
                } else if ("Pattern".equals(propertyName)) {
                    if (value.type != String.class)
                        throw new InvalidPolicyStreamException("Pattern in 2.1 can only be a String");
                    String pattern = (String)value.target;
                    XpathExpression xe = xba.getXpathExpression();
                    if (xe == null)
                        xba.setXpathExpression(xe = new XpathExpression(pattern));
                    else
                        xe.setExpression(pattern);
                    return;
                }
            }

            originalVisitor.unknownProperty(originalObject, problematicParameter, deserializedObject, propertyName, value, problemEncountered);
            return;
        }
    }
}
