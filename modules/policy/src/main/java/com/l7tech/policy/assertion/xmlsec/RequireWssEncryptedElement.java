package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Functions;
import com.l7tech.xml.xpath.XpathExpression;

import java.util.*;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Enforces the XML security on the message elements or entire message
 * 
 * @author flascell
 * @version Aug 27, 2003<br/>
 */
public class RequireWssEncryptedElement extends XmlSecurityAssertionBase {
    public RequireWssEncryptedElement() {
        this(compatOrigDefaultXpathValue());
    }

    public RequireWssEncryptedElement(XpathExpression xpath) {
        super(TargetMessageType.REQUEST, false);
        setXpathExpression(xpath);
    }

    /**
     * Get the requested xml encryption algorithm with the highest preference from the list of encryption algorithms.
     * The default one is http://www.w3.org/2001/04/xmlenc#aes128-cbc.
     * @return the encrytion algorithm with the highest preference.
     */
    public String getXEncAlgorithm() {
        return xEncAlgorithmList == null || xEncAlgorithmList.isEmpty() ? XencUtil.AES_128_CBC : xEncAlgorithmList.iterator().next();
    }

    public List<String> getXEncAlgorithmList() {
        return xEncAlgorithmList == null ? null : new ArrayList<String>(xEncAlgorithmList);
    }

    /**
     * Set the xml encryption algorithm with the highest preference.
     * @param xEncAlgorithm the algorithm URI to set as highest-preference.  Required.
     */
    public void setXEncAlgorithm(String xEncAlgorithm) {
        if (xEncAlgorithm == null) throw new IllegalArgumentException();
        if (xEncAlgorithmList == null) {
            // Set list to single algorithm
            initXEncAlgorithmList(Arrays.asList(xEncAlgorithm));
        } else {
            // Bring preferred algorithm to front of list
            xEncAlgorithmList.remove(xEncAlgorithm);
            initXEncAlgorithmList(Arrays.asList(ArrayUtils.unshift(xEncAlgorithmList.toArray(new String[xEncAlgorithmList.size()]), xEncAlgorithm)));
        }
    }

    /**
     * Update the encryption algorithm list.  It is important to update the algorithm with the highest preference.
     * @param newList the new list of algorithms to use.  Required.
     */
    public void setXEncAlgorithmList(List<String> newList) {
        if (newList == null) throw new IllegalArgumentException();
        initXEncAlgorithmList(newList);
    }

    private void initXEncAlgorithmList(Collection<String> newList) {
        xEncAlgorithmList = new LinkedHashSet<String>(newList);
    }

    public String getKeyEncryptionAlgorithm() {
        return xencKeyAlgorithm;
    }
                    
    public void setKeyEncryptionAlgorithm(String keyEncryptionAlgorithm) {
        this.xencKeyAlgorithm = keyEncryptionAlgorithm;
    }

    public boolean isEncryptContentsOnly() {
        return encryptContentsOnly;
    }

    public void setEncryptContentsOnly(boolean encryptContentsOnly) {
        this.encryptContentsOnly = encryptContentsOnly;
    }

    final static String baseName = "Require Encrypted Element";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RequireWssEncryptedElement>(){
        @Override
        public String getAssertionName( final RequireWssEncryptedElement assertion, final boolean decorate) {
            StringBuilder name = new StringBuilder(baseName + " ");
            if (assertion.getXpathExpression() == null) {
                name.append("[XPath expression not set]");
            } else {
                name.append(assertion.getXpathExpression().getExpression());
            }
            return (decorate) ? AssertionUtils.decorateName(assertion, name) : baseName;
        }
    };
    
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "The message must contain one or more encrypted elements.");
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_SORT_PRIORITY, 90000);
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Encrypted Element Properties");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, RequireWssEncryptedElement>(){
            @Override
            public Set<ValidatorFlag> call(RequireWssEncryptedElement assertion) {
                return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION);
            }
        });
        meta.put(AssertionMetadata.ASSERTION_FACTORY, new XpathBasedAssertionFactory<RequireWssEncryptedElement>(RequireWssEncryptedElement.class));
        meta.put(CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssConfidentiality");
        meta.put(CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/xmlencryption.gif");
        meta.put(USED_BY_CLIENT, Boolean.TRUE);
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.RequireWssEncryptedElementValidator");

        return meta;
    }

    private String xencKeyAlgorithm;
    private Set<String> xEncAlgorithmList; // Store all encrption algorithms user chose
    private boolean encryptContentsOnly = true;
}
