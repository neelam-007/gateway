/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import org.apache.ws.policy.PrimitiveAssertion;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.logging.Logger;

/**
 * Superclass for a WS-SP security binding.
 */
abstract class SecurityBinding extends WsspVisitor {
    private static final Logger logger = Logger.getLogger(SecurityBinding.class.getName());
    private static final Map bindingProperties = new HashMap();
    static {
        bindingProperties.put("TransportToken", new QnamePropertyGatherer("TransportToken", new String[] {
                "HttpsToken",  // TODO support RequireClientCertificate attribute
        }));
        bindingProperties.put("AlgorithmSuite", new QnamePropertyGatherer("AlgorithmSuite", new String[] {
                "Basic256Rsa15",
                "Basic192Rsa15",
                "Basic128Rsa15",
                "TripleDesRsa15",
        }));
        bindingProperties.put("Layout", new QnamePropertyGatherer("Layout", new String[] {
                "Strict",
                "Lax",
                "LaxTimestampFirst",
        }));
        bindingProperties.put("IncludeTimestamp", new SimplePropertySetter("Timestamp", true));
        bindingProperties.put("InitiatorToken", new InitiatorToken("InitiatorToken."));
        bindingProperties.put("RecipientToken", new PrimitiveAssertionIgnorer());
        bindingProperties.put("OnlySignEntireHeadersAndBody", new SimplePropertySetter("EntireHeaderAndBodySignatures", true));
    }

    private final PrimitiveAssertion primitiveAssertion;
    private final Map converterMap;
    private boolean gatheredProperties = false;
    private Set transportTokens = new HashSet();
    private Set initiatorTokenTypes = new HashSet();
    private Set recipientTokenTypes = new HashSet();
    private Set layouts = new HashSet();

    public SecurityBinding(WsspVisitor parent, PrimitiveAssertion primitiveAssertion) {
        super(parent);
        this.primitiveAssertion = primitiveAssertion;

        // Initialize the property setters.  Subclasses can override this.
        converterMap = new HashMap();
        converterMap.putAll(bindingProperties);
    }

    protected Map getConverterMap() {
        return converterMap;
    }

    protected final void onGatheredProperties() {
        gatheredProperties = true;
    }

    public Assertion toLayer7Policy() throws PolicyConversionException {
        if (!gatheredProperties) {
            gatherPropertiesFromSubPolicy(primitiveAssertion);
            onGatheredProperties();
        }

        boolean isRequest = isSimpleProperty(IS_REQUEST);

        AllAssertion all = new AllAssertion();

        // populate all from currently set properties

        // TODO we currently use this only as a hint to enable X509
        boolean signWithX509 = setContainsQnameWhoseLocalNameStartsWith(initiatorTokenTypes, "WssX509");
        boolean encryptForX509 = setContainsQnameWhoseLocalNameStartsWith(recipientTokenTypes, "WssX509");
        if (encryptForX509 || signWithX509) all.addChild(new RequireWssX509Cert());

        // TODO we are currently ignoring layouts

        // TODO support requireClientCert attribute
        boolean useSsl = setContainsQnameWhoseLocalNameStartsWith(transportTokens, "HttpsToken");
        if (isRequest && useSsl) all.addChild(new SslAssertion(false));

        return all.isEmpty() ? null : all;
    }

    private static boolean setContainsQnameWhoseLocalNameStartsWith(Set set, String prefix) {
        for (Iterator i = set.iterator(); i.hasNext();) {
            QName n = (QName)i.next();
            if (n.getLocalPart().startsWith(prefix))
                return true;
        }
        return false;
    }

    protected boolean maybeAddPropertyQnameValue(String propName, QName propValue) {
        if ("InitiatorToken.X509Token".equals(propName)) {
            initiatorTokenTypes.add(propValue);
            return true;
        } else if ("RecipientToken.X509Token".equals(propName)) {
            recipientTokenTypes.add(propValue);
            return true;
        } else if ("TransportToken".equals(propName)) {
            transportTokens.add(propValue);
            return true;
        } else if ("Layout".equals(propName)) {
            layouts.add(propValue);
            return true;
        }
        return super.maybeAddPropertyQnameValue(propName, propValue);
    }

    private static class InitiatorToken implements PrimitiveAssertionConverter {
        private final String prefix;

        public InitiatorToken(String prefix) {
            this.prefix = prefix;
        }

        public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
            final Map tokenTypes = new HashMap();
            tokenTypes.put("X509Token", new QnamePropertyGatherer(prefix + "X509Token",
                                                                  new String[] {
                                                                          "WssX509V3Token10"
                                                                  })
            {
                protected void preconvert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
                    String includeToken = p.getAttribute(new QName(p.getName().getNamespaceURI(), "IncludeToken"));
                    if (!("http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient".equals(includeToken)))
                        throw new PolicyConversionException("X509Token must have IncludeToken of AlwaysToRecipient");
                }
            });

            v = new WsspVisitor(v) {
                protected Map getConverterMap() {
                    return tokenTypes;
                }
            };

            v.gatherPropertiesFromSubPolicy(p);
            return null;
        }

    }

    private static class QnamePropertyGatherer implements PrimitiveAssertionConverter {
        private final String prefix;
        private final String[] localNames;

        public QnamePropertyGatherer(String prefix, String[] localNames) {
            this.prefix = prefix;
            this.localNames = localNames;
        }

        /** Subclasses override this to do any checks they want to do before convert proceeds. */
        protected void preconvert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
        }

        public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
            final Map tokenValues = new HashMap();
            for (int i = 0; i < localNames.length; i++) {
                tokenValues.put(localNames[i], new PrimitiveAssertionConverter() {
                    public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
                        v.addPropertyQnameValue(prefix, p.getName());
                        return null;
                    }
                });
            }

            v = new WsspVisitor(v) {
                protected Map getConverterMap() {
                    return tokenValues;
                }
            };

            v.gatherPropertiesFromSubPolicy(p);
            return null;
        }
    }

    static class UnsupportedSecurityBindingProperty implements PrimitiveAssertionConverter {
        public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
            throw new PolicyConversionException("Security binding property not yet supported: " + p.getName());
        }
    }
}
