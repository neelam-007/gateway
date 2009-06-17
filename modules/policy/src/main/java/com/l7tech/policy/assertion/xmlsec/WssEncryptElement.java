package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.util.Functions;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell
 */
public class WssEncryptElement extends XmlSecurityAssertionBase {
    public WssEncryptElement() {
        this(XpathExpression.soapBodyXpathValue());
    }

    public WssEncryptElement(final XpathExpression xpath) {
        super(TargetMessageType.RESPONSE);
        setXpathExpression(xpath);
    }

    /**
     * Get the requested xml encrytpion algorithm. Defaults to http://www.w3.org/2001/04/xmlenc#aes128-cbc
     * @return the encrytion algorithm requested
     */
    public String getXEncAlgorithm() {
        return xencAlgorithm;
    }

    /**
     * Set the xml encryption algorithm.
     * @param xencAlgorithm
     */
    public void setXEncAlgorithm(String xencAlgorithm) {
        if (xencAlgorithm == null) {
            throw new IllegalArgumentException();
        }
        this.xencAlgorithm = xencAlgorithm;
    }

    public String getKeyEncryptionAlgorithm() {
        return xencKeyAlgorithm;
    }

    public void setKeyEncryptionAlgorithm(String keyEncryptionAlgorithm) {
        this.xencKeyAlgorithm = keyEncryptionAlgorithm;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "Encrypt Element");
        meta.put(AssertionMetadata.DESCRIPTION, "Encrypt one or more elements of the message.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 70000);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, WssEncryptElement>() {
            @Override
            public String call( final WssEncryptElement responseWssConfidentiality ) {
                StringBuilder name = new StringBuilder("Encrypt element ");
                if (responseWssConfidentiality.getXpathExpression() == null) {
                    name .append("[XPath expression not set]");
                } else {
                    name.append(responseWssConfidentiality.getXpathExpression().getExpression());
                }
                return AssertionUtils.decorateName(responseWssConfidentiality, name);
            }
        });
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientResponseWssConfidentiality");
        meta.put( AssertionMetadata.CLIENT_ASSERTION_TARGETS, new String[]{"response"} );
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WssEncryptElementValidator");

        return meta;
    }

    private String xencAlgorithm = XencUtil.AES_128_CBC;
    private String xencKeyAlgorithm = null;
}
