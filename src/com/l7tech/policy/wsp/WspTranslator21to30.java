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
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Translator capable of translating a 2.1 policy into a 3.0 policy.
 */
public class WspTranslator21to30 implements WspTranslator {
    private static final Logger log = Logger.getLogger(WspTranslator21to30.class.getName());
    static final WspTranslator21to30 INSTANCE = new WspTranslator21to30();

    private interface LookupEntry {
        Object getKey();
    }

    private abstract static class ElementTranslator { // class, rather than interface, so we can make its members private
        abstract Element translateElement(Element in) throws InvalidPolicyStreamException;
    }

    private abstract static class TranslatorEntry extends ElementTranslator implements LookupEntry {
        private final String localName;
        private TranslatorEntry(String localName) {
            this.localName = localName;
        }
        public Object getKey() { return localName; }
    }

    private abstract static class PropertyTranslator {
        abstract void setProperty(Object target, String propertyName, TypedReference value) throws InvalidPolicyStreamException;
    }

    private abstract static class PropertyTranslatorEntry extends PropertyTranslator implements LookupEntry {
        protected final Class targetClass;
        protected PropertyTranslatorEntry(Class targetClass) {
            this.targetClass = targetClass;
        }
        public Object getKey() { return targetClass; }
    }

    private static final Map PROPERTY_MAP = makeMap(new PropertyTranslatorEntry[] {
        new XpathBasedAssertionTranslator(RequestXpathAssertion.class),
    });

    private static final Map ELEMENT_MAP = makeMap(new TranslatorEntry[] {
        new XmlSecurityTranslator("XmlRequestSecurity"),
        new XmlSecurityTranslator("XmlResponseSecurity"),
    });

    private static Map makeMap(LookupEntry[] in) {
        Map out = new HashMap();
        for (int i = 0; i < in.length; i++)
            out.put(in[i].getKey(), in[i]);
        return out;
    }

    private static class XpathBasedAssertionTranslator extends PropertyTranslatorEntry {
        public XpathBasedAssertionTranslator(Class targetClass) {
            super(targetClass);
        }

        void setProperty(Object target, String propertyName, TypedReference value) throws InvalidPolicyStreamException {
            if (!(target.getClass() == targetClass))
                throw new InvalidPolicyStreamException("Internal error -- incorrect target class"); // can't happen

            XpathBasedAssertion xba = (XpathBasedAssertion)target;
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

            throw new InvalidPolicyStreamException("Unable to translate property " + propertyName + " of " + targetClass);
        }
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
    private static class XmlSecurityTranslator extends TranslatorEntry  {
        public XmlSecurityTranslator(String localName) {
            super(localName);
        }

        public Element translateElement(Element in) throws InvalidPolicyStreamException {
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
                    TypedReference pxref = WspConstants.thawElement(preconditionXpath, StrictWspVisitor.INSTANCE);
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

                    TypedReference exref = WspConstants.thawElement(elementXpath, StrictWspVisitor.INSTANCE);
                    if (exref.target == null)
                        throw new InvalidPolicyStreamException("Invalid 2.1 policy - " + origName + " ElementXpath may not be null");

                    if (!(exref.target instanceof XpathExpression))
                        throw new InvalidPolicyStreamException("Invalid 2.1 policy - " + origName + " ElementXpath must be an XpathExpression");
                    XpathExpression expath = (XpathExpression)exref.target;
                    if (expath.getNamespaces() != null)
                        seenNamespaceMap = expath.getNamespaces();

                    if (SoapUtil.SOAP_ENVELOPE_XPATH.equals(expath.getExpression()))
                        if (pxref.target == null)
                            isCredentialSource = true;

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
    }

    public Element translatePolicy(Element input) throws InvalidPolicyStreamException {
        Assertion root = WspReader.parse(input, new PermissiveWspVisitor() {
            public void unknownProperty(Element originalObject,
                                        Element problematicParameter,
                                        Object deserializedObject,
                                        String parameterName,
                                        TypedReference parameterValue,
                                        Exception problemEncountered)
                    throws InvalidPolicyStreamException
            {
                log.info("Attempting to interpret invalid property " + parameterName + " of " + deserializedObject.getClass());
                PropertyTranslatorEntry pte = (PropertyTranslatorEntry)PROPERTY_MAP.get(deserializedObject.getClass());
                if (pte != null) {
                    pte.setProperty(deserializedObject, parameterName, parameterValue);
                    return;
                }

                final String msg = "Unable to interpret unknown property " + parameterName + " of " + deserializedObject.getClass();
                log.severe(msg);
                throw new InvalidPolicyStreamException(msg, problemEncountered);
                /*super.unknownProperty(originalObject,
                                      problematicParameter,
                                      deserializedObject,
                                      parameterName,
                                      parameterValue,
                                      problemEncountered);*/
            }

            public Element invalidElement(Element problematicElement, Exception problemEncountered) throws InvalidPolicyStreamException {
                log.info("Attempting to interpret invalid element " + problematicElement.getLocalName());
                TranslatorEntry te = (TranslatorEntry)ELEMENT_MAP.get(problematicElement.getLocalName());
                if (te != null)
                    return te.translateElement(problematicElement);
                return super.invalidElement(problematicElement, problemEncountered);
            }
        });
        try {
            return XmlUtil.stringToDocument(WspWriter.getPolicyXml(root)).getDocumentElement();
        } catch (IOException e) {
            throw new InvalidPolicyStreamException("Unable tp parse converted policy XML", e); // can't happen
        } catch (SAXException e) {
            throw new InvalidPolicyStreamException("Unable tp parse converted policy XML", e);
        }
    }
}
