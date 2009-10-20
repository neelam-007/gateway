package com.l7tech.xml;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.PrivateKeyableSupport;

import java.io.Serializable;

/**
 * Attached to a PolicyEnforcementContext and overridable through the FaultLevel assertion,
 * such an object tells the SSG what the soap fault returned to a requestor should look like
 * when a policy evaluation fails.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 4, 2006<br/>
 *
 * @see com.l7tech.policy.assertion.FaultLevel
 */
public class SoapFaultLevel implements PrivateKeyable, Serializable {
    public static final int DROP_CONNECTION = 0;
    public static final int TEMPLATE_FAULT = 1;
    public static final int GENERIC_FAULT = 2;
    public static final int MEDIUM_DETAIL_FAULT = 3;
    public static final int FULL_TRACE_FAULT = 4;

    private int level = GENERIC_FAULT;
    private String faultTemplate;
    private boolean includePolicyDownloadURL = true;
    private boolean signSoapFault = false;
    private PrivateKeyableSupport privatekeyableSupport = new PrivateKeyableSupport();
    private String[] variablesUsed = new String[0];

    public SoapFaultLevel() {
    }

    public SoapFaultLevel( final SoapFaultLevel soapFaultLevel ) {
        if ( soapFaultLevel != null ) {
            this.level = soapFaultLevel.level;
            this.faultTemplate = soapFaultLevel.faultTemplate;
            this.includePolicyDownloadURL = soapFaultLevel.includePolicyDownloadURL;
            this.signSoapFault = soapFaultLevel.signSoapFault;
            this.variablesUsed = soapFaultLevel.variablesUsed;
            this.privatekeyableSupport = new PrivateKeyableSupport( soapFaultLevel.privatekeyableSupport );
        }
    }

    /**
     * @return the level of the fault that should be returned to requestor in case of a policy evaluation failure
     */
    public int getLevel() {
        return level;
    }

    /**
     * set the level of the fault that should be returned to requestor in case of a policy evaluation failure
     * @param level DROP_CONNECTION or TEMPLATE_FAULT or GENERIC_FAULT or MEDIUM_DETAIL_FAULT or FULL_TRACE_FAULT
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * the template that serves as a basis for the fault that should be returned to requestor in case of a
     * policy evaluation failure. it may contain context variables that need to be resolved before the fault
     * is actually produced
     */
    public String getFaultTemplate() {
        return faultTemplate;
    }

    /**
     * the template that serves as a basis for the fault that should be returned to requestor in case of a
     * policy evaluation failure. it may contain context variables that need to be resolved before the fault
     * is actually produced
     */
    public void setFaultTemplate(String faultTemplate) {
        this.faultTemplate = faultTemplate;
        if (faultTemplate != null) {
            variablesUsed = Syntax.getReferencedNames(faultTemplate);
        } else {
            variablesUsed = new String[0];
        }
    }

    /**
     * whether or not the returned soap fault should include a special http header whose value is the url to download
     * the policy which was violated.
     */
    public boolean isIncludePolicyDownloadURL() {
        return includePolicyDownloadURL;
    }

    /**
     * whether or not the returned soap fault should include a special http header whose value is the url to download
     * the policy which was violated.
     */
    public void setIncludePolicyDownloadURL(boolean includePolicyDownloadURL) {
        this.includePolicyDownloadURL = includePolicyDownloadURL;
    }

    public boolean isSignSoapFault() {
        return signSoapFault;
    }

    public void setSignSoapFault( final boolean signSoapFault ) {
        this.signSoapFault = signSoapFault;
    }

    @Override
    public String getKeyAlias() {
        return privatekeyableSupport.getKeyAlias();
    }

    @Override
    public void setKeyAlias( final String keyAlias ) {
        privatekeyableSupport.setKeyAlias( keyAlias );
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return privatekeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId( final long nonDefaultKeystoreId ) {
        privatekeyableSupport.setNonDefaultKeystoreId( nonDefaultKeystoreId );
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return privatekeyableSupport.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore( final boolean usesDefaultKeyStore ) {
        privatekeyableSupport.setUsesDefaultKeyStore( usesDefaultKeyStore );
    }

    @Override
    public String toString() {
        return getClass().getName() + ". Level: " + level +
                                      ", Include URL: " + includePolicyDownloadURL +
                                      ", Template: " + faultTemplate;
    }

    public String[] getVariablesUsed() {
        return variablesUsed;
    }
}
