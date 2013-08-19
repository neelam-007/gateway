package com.l7tech.xml;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.PrivateKeyableSupport;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.NameValuePair;

import java.io.Serializable;
import java.util.*;

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
    private String faultTemplateContentType;
    private String faultTemplateHttpStatus;
    private boolean includePolicyDownloadURL = true;
    private boolean signSoapFault = false;
    private boolean alwaysReturnSoapFault = false;
    private PrivateKeyableSupport privatekeyableSupport = new PrivateKeyableSupport();
    private String[] variablesUsed = new String[0];
    private NameValuePair[] extraHeaders = null;

    private boolean useClientFault = false;

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
            this.alwaysReturnSoapFault = soapFaultLevel.isAlwaysReturnSoapFault();
            this.extraHeaders = copyHeaders(soapFaultLevel.extraHeaders);
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
        updateVariableUse();
    }

    /**
     * @return extra headers to include in the response, or null.  May use variables in both header names and values.
     */
    public NameValuePair[] getExtraHeaders() {
        return extraHeaders;
    }

    /**
     * @param extraHeaders extra headers to include in the response, or null.  May use variables in both header names and values.
     */
    public void setExtraHeaders(NameValuePair[] extraHeaders) {
        this.extraHeaders = extraHeaders;
        updateVariableUse();
    }

    /**
     * Get the content type for the fault response.
     *
     * <p>This value may contain variables that must be evaluated.</p>
     *
     * @return The content type (may be null)
     */
    public String getFaultTemplateContentType() {
        return faultTemplateContentType;
    }

    /**
     * Set the content type to use for the fault response.
     *
     * @param faultTemplateContentType The content type to use
     */
    public void setFaultTemplateContentType( final String faultTemplateContentType ) {
        this.faultTemplateContentType = faultTemplateContentType;
        updateVariableUse();
    }

    /**
     * Get the HTTP status code to use for the fault response.
     *
     * <p>This value may contain variables that must be evaluated.</p>
     *
     * @return The HTTP status (may be null)
     */
    public String getFaultTemplateHttpStatus() {
        return faultTemplateHttpStatus;
    }

    /**
     * Set the HTTP status code to use for the fault response.
     *
     * @param faultTemplateHttpStatus The HTTP status code to use.
     */
    public void setFaultTemplateHttpStatus( final String faultTemplateHttpStatus ) {
        this.faultTemplateHttpStatus = faultTemplateHttpStatus;
        updateVariableUse();
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

    public boolean isAlwaysReturnSoapFault() {
        return alwaysReturnSoapFault;
    }

    public void setAlwaysReturnSoapFault(boolean alwaysReturnSoapFault) {
        this.alwaysReturnSoapFault = alwaysReturnSoapFault;
    }

    @Override
    public String getKeyAlias() {
        return privatekeyableSupport.getKeyAlias();
    }

    @Override
    public void setKeyAlias( final String keyAlias ) {
        privatekeyableSupport.setKeyAlias(keyAlias);
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public Goid getNonDefaultKeystoreId() {
        return privatekeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId( final Goid nonDefaultKeystoreId ) {
        privatekeyableSupport.setNonDefaultKeystoreId(nonDefaultKeystoreId);
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

    private void updateVariableUse() {
        Set<String> variables = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        addVariables( variables, faultTemplateHttpStatus );
        addVariables( variables, faultTemplateContentType );
        addVariables( variables, faultTemplate );
        if (extraHeaders != null) for (NameValuePair extraHeader : extraHeaders) {
            addVariables( variables, extraHeader.getKey() );
            addVariables( variables, extraHeader.getValue() );
        }
        variablesUsed = variables.toArray( new String[variables.size()] );
    }

    private void addVariables( final Set<String> variables, final String text ) {
        if ( text != null ) {
            String[] referencedNames = Syntax.getReferencedNames( text );
            if ( referencedNames != null ) {
                variables.addAll( Arrays.asList( referencedNames ) );
            }
        }
    }

    private NameValuePair[] copyHeaders(NameValuePair[] headers) {
        if (headers == null) return null;
        List<NameValuePair> ret = new ArrayList<NameValuePair>();
        for (NameValuePair header : headers) {
            ret.add(new NameValuePair(header.getKey(), header.getValue()));
        }
        return ret.toArray(new NameValuePair[ret.size()]);
    }

    public boolean isUseClientFault() {
        return useClientFault;
    }

    public void setUseClientFault(final boolean useClientFault) {
        this.useClientFault = useClientFault;
    }
}
