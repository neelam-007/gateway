package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.util.Functions;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
@ProcessesResponse
public class ResponseWssConfidentiality extends XmlSecurityAssertionBase {
    public ResponseWssConfidentiality() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public ResponseWssConfidentiality(XpathExpression xpath) {
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

        meta.put(AssertionMetadata.SHORT_NAME, "Encrypt Response Element");
        meta.put(AssertionMetadata.DESCRIPTION, "Server will encrypt an element of the SOAP response");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, ResponseWssConfidentiality>() {
            @Override
            public String call( final ResponseWssConfidentiality responseWssConfidentiality ) {
                StringBuilder name = new StringBuilder("Encrypt response element ");
                if (responseWssConfidentiality.getXpathExpression() == null) {
                    name .append("[XPath expression not set]");
                } else {
                    name.append(responseWssConfidentiality.getXpathExpression().getExpression());
                }
                name.append(SecurityHeaderAddressableSupport.getActorSuffix(responseWssConfidentiality));
                return name.toString();
            }
        });

        return meta;
    }

    private String xencAlgorithm = XencUtil.AES_128_CBC;
    private String xencKeyAlgorithm = null;
}
