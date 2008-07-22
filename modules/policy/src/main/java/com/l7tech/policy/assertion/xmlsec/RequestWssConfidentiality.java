package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.validator.RequestWssConfidentialityValidator;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.security.xml.XencUtil;

import java.util.List;
/**
 * Enforces the XML security on the message elements or entire message
 * 
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
@ProcessesRequest
public class RequestWssConfidentiality extends XmlSecurityAssertionBase {
    public RequestWssConfidentiality() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public RequestWssConfidentiality(XpathExpression xpath) {
        setXpathExpression(xpath);
    }

    /**
     * Get the requested xml encryption algorithm with the highest preference from the list of encryption algorithms.
     * The default one is http://www.w3.org/2001/04/xmlenc#aes128-cbc.
     * @return the encrytion algorithm with the highest preference.
     */
    public String getXEncAlgorithm() {
        // It is guranteed that xEncAlgorithmWithHighestPref is the one with highest preference, since the
        // methods setXEncAlgorithm and setXEncAlgorithmList always update the new  xEncAlgorithmWithHighestPref.
        return xEncAlgorithmWithHighestPref;
    }

    public List<String> getXEncAlgorithmList() {
        return xEncAlgorithmList;
    }

    /**
     * Set the xml encryption algorithm with the highest preference.
     * @param xEncAlgorithm
     */
    public void setXEncAlgorithm(String xEncAlgorithm) {
        if (xEncAlgorithm == null) throw new IllegalArgumentException();
        if (xEncAlgorithmList != null) {
            // Add the algorithm into the list if the algorithm is not in the list.
            if (! xEncAlgorithmList.contains(xEncAlgorithm)) xEncAlgorithmList.add(xEncAlgorithm);
            // Update the encryption algorithm with the highest preference
            xEncAlgorithmWithHighestPref = findAlgWithHighestPreference();
        } else {
            xEncAlgorithmWithHighestPref = xEncAlgorithm;
        }
    }

    /**
     * Update the encryption algorithm list.  It is important to update the algorithm with the highest preference.
     * @param newList
     */
    public void setXEncAlgorithmList(List<String> newList) {
        if (newList == null) throw new IllegalArgumentException();
        xEncAlgorithmList = newList;
        // Update the encryption algorithm with the highest preference
        xEncAlgorithmWithHighestPref = findAlgWithHighestPreference();
    }

    public String getKeyEncryptionAlgorithm() {
        return xencKeyAlgorithm;
    }
                    
    public void setKeyEncryptionAlgorithm(String keyEncryptionAlgorithm) {
        this.xencKeyAlgorithm = keyEncryptionAlgorithm;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, RequestWssConfidentialityValidator.class.getName());

        return meta;
    }

    private String findAlgWithHighestPreference() {
        if (xEncAlgorithmList.isEmpty()) {
            xEncAlgorithmWithHighestPref = XencUtil.AES_128_CBC;
            xEncAlgorithmList.add(XencUtil.AES_128_CBC);
            return XencUtil.AES_128_CBC;
        }

        if (xEncAlgorithmList.contains(XencUtil.AES_128_CBC)) {
            return XencUtil.AES_128_CBC;
        } else if (xEncAlgorithmList.contains(XencUtil.AES_192_CBC)) {
            return XencUtil.AES_192_CBC;
        }
         else if (xEncAlgorithmList.contains(XencUtil.AES_256_CBC)) {
            return XencUtil.AES_256_CBC;
        }
         else if (xEncAlgorithmList.contains(XencUtil.TRIPLE_DES_CBC)) {
            return XencUtil.TRIPLE_DES_CBC;
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    private String xEncAlgorithmWithHighestPref = XencUtil.AES_128_CBC; // The encryption algorithm wiht the highese preference
    private String xencKeyAlgorithm;
    private List<String> xEncAlgorithmList; // Store all encrption algorithms user chose
}
