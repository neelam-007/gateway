/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.security.xml.XencUtil;
import com.l7tech.util.TextUtils;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.xmlsec.*;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Gathers state for a single policy branch (ie, an All assertion in a normalized policy).
 * An top-level All assertion in a normalized policy may contain zero or one security binding,
 * zero or one Trust10, zero or one Wss10, zero or one SignedParts, and zero or one EncryptedParts.
 */
class TopLevelAll extends WsspVisitor {
    // Ciphers Martha can decorate with
    private static final Map CIPHERS_OUT = new LinkedHashMap();
    static {
        CIPHERS_OUT.put("Basic256Rsa15", XencUtil.AES_256_CBC);
        CIPHERS_OUT.put("Basic192Rsa15", XencUtil.AES_192_CBC);
        CIPHERS_OUT.put("Basic128Rsa15", XencUtil.AES_256_CBC);
        CIPHERS_OUT.put("TripleDesRsa15", XencUtil.TRIPLE_DES_CBC);
    }

    // Ciphers Trogdor can undecorate
    private static final Map CIPHERS_IN = new LinkedHashMap();
    static {
        // Trogdor can accept anything Martha can produce
        CIPHERS_IN.putAll(CIPHERS_OUT);

        // Additionally, RSA-OAEP is supported (read-only)
        CIPHERS_IN.put("Basic256", XencUtil.AES_256_CBC);
        CIPHERS_IN.put("Basic192", XencUtil.AES_192_CBC);
        CIPHERS_IN.put("Basic128", XencUtil.AES_256_CBC);
        CIPHERS_IN.put("TripleDes", XencUtil.TRIPLE_DES_CBC);
    }

    private Set algorithmSuites = new HashSet();
    private boolean timestamp = false;
    private boolean entireHeaderAndBodySignatures = false;
    private boolean signBody = false;
    private boolean encryptBody = false;

    protected TopLevelAll(WsspVisitor parent) {
        super(parent);
    }

    protected Map getConverterMap() {
        return getParent().getConverterMap();
    }

    protected boolean maybeAddPropertyQnameValue(String propName, QName propValue) {
        if ("AlgorithmSuite".equals(propName)) {
            algorithmSuites.add(propValue.getLocalPart());
            return true;
        }
        return super.maybeAddPropertyQnameValue(propName, propValue);
    }

    protected boolean maybeSetSimpleProperty(String propName, boolean propValue) {
        if ("Timestamp".equals(propName)) {
            timestamp = propValue;
            return true;
        } else if ("EntireHeaderAndBodySignatures".equals(propName)) {
            entireHeaderAndBodySignatures = propValue;
            return true;
        } else if ("SignBody".equals(propName)) {
            signBody = propValue;
            return true;
        } else if ("EncryptBody".equals(propName)) {
            encryptBody = propValue;
            return true;
        }
        return super.maybeSetSimpleProperty(propName, propValue);
    }

    protected Assertion recursiveConvertAll(org.apache.ws.policy.Assertion p) throws PolicyConversionException {
        Assertion converted = super.recursiveConvertAll(p);
        final AllAssertion all;
        if (converted instanceof AllAssertion) {
            all = (AllAssertion)converted;
        } else {
            all = new AllAssertion();
            if (converted != null)
                all.addChild(converted);
        }

        boolean isRequest = isSimpleProperty(IS_REQUEST);

        // Now mix in the state we gathered while doing the conversion
        if (signBody || entireHeaderAndBodySignatures) {
            if (isRequest)
                all.addChild(new RequireWssSignedElement());
            else
                all.addChild(new WssSignElement());
        }
        if (encryptBody) {
            Assertion conf = isRequest ? makeRequestConfidentiality(null) : makeResponseConfidentiality(null);
            all.addChild(conf);
        }

        if (isRequest && timestamp) {
            final RequireWssTimestamp ta = new RequireWssTimestamp();
            ta.setSignatureRequired(signBody || entireHeaderAndBodySignatures);
            all.addChild(ta);
        }

        return collapse(all);
    }

    private Assertion makeRequestConfidentiality(XpathExpression xpath) throws PolicyConversionException {
        String alg = null;
        Set entries = CIPHERS_OUT.entrySet();
        for (Iterator i = entries.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            if (algorithmSuites.contains(entry.getKey())) {
                alg = entry.getValue().toString();
                break;
            }
        }
        if (alg == null) {
            throw new PolicyConversionException(
                    "Unable to comply with this policy -- request encryption is required, but no compatible AlgorithmSuite" +
                            " was specified.  Supported (for outgoing requests): " +
                    TextUtils.join(" ", (String[])CIPHERS_OUT.keySet().toArray(new String[0])));
        }

        RequireWssEncryptedElement conf = xpath == null ? new RequireWssEncryptedElement() : new RequireWssEncryptedElement(xpath);
        conf.setXEncAlgorithm(alg);
        return conf;
    }

    private Assertion makeResponseConfidentiality(XpathExpression xpath) throws PolicyConversionException {
        boolean foundOne = false;
        OneOrMoreAssertion crypt = new OneOrMoreAssertion();
        for (Iterator i = algorithmSuites.iterator(); i.hasNext();) {
            String localName = (String)i.next();
            String alg = (String)CIPHERS_IN.get(localName);
            if (alg != null) {
                foundOne = true;
                WssEncryptElement conf = xpath == null ? new WssEncryptElement() : new WssEncryptElement(xpath);
                conf.setXEncAlgorithm(alg);
                crypt.addChild(conf);
            }
        }
        if (!foundOne) {
            throw new PolicyConversionException(
                    "Unable to comply with this policy -- response encryptiong is required, but no compatible AlgorithmSuite" +
                            " was specified.  Supported (for incoming responses): " +
                            TextUtils.join(" ", (String[])CIPHERS_IN.keySet().toArray(new String[0])));
        }
        return collapse(crypt);
    }
}
