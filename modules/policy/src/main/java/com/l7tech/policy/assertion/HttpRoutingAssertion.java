/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.*;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.HTTP_URL;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.HTTP_URL_ARRAY;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.IP_ADDRESS_ARRAY;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import com.l7tech.common.http.HttpMethod;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 *
 * @author mike
 * @version 1.0
 */
public class HttpRoutingAssertion extends RoutingAssertion implements UsesVariables, SetsVariables, PrivateKeyable
{
    public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = -1;
    public static final String VAR_ROUTING_LATENCY = "httpRouting.latency";
    public static final String VAR_HTTP_ROUTING_URL = "httpRouting.url";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_HOST = "host";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_PROTOCOL = "protocol";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_PORT = "port";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_FILE = "file";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_PATH = "path";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_QUERY = "query";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_FRAGMENT = "fragment";
    public static final String PROP_SSL_SESSION_TIMEOUT = HttpRoutingAssertion.class.getName() + ".sslSessionTimeoutSeconds";
    public static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;

    @Deprecated 
    public static final String VAR_SERVICE_URL = "service.url";

    // WARNING
    // WARNING : If you add properties, update the copyFrom method
    // WARNING

    protected String protectedServiceUrl;
    protected String login;
    protected String password;
    protected String realm;
    protected String ntlmHost;
    protected String userAgent;
    protected int maxConnections;
    protected boolean passthroughHttpAuthentication;
    protected String[] customIpAddresses = null;
    protected String[] customURLs = null;
    protected String failoverStrategyName = "ordered";
    private boolean taiCredentialChaining = false;
    protected Integer connectionTimeout;
    protected Integer timeout;
    protected String requestMsgSrc;
    protected String responseMsgDest;
    protected HttpPassthroughRuleSet responseHeaderRules = new HttpPassthroughRuleSet(false,
                                                            new HttpPassthroughRule[]{
                                                             new HttpPassthroughRule("Set-Cookie", false, null)});
    protected HttpPassthroughRuleSet requestHeaderRules = new HttpPassthroughRuleSet(false,
                                                           new HttpPassthroughRule[]{
                                                             new HttpPassthroughRule("Cookie", false, null),
                                                             new HttpPassthroughRule("SOAPAction", false, null)});
    protected HttpPassthroughRuleSet requestParamRules = new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{});
    protected boolean followRedirects = false;
    protected boolean failOnErrorStatus = true;
    protected boolean gzipEncodeDownstream;

    protected boolean krbDelegatedAuthentication;
    protected boolean krbUseGatewayKeytab;
    protected String krbConfiguredAccount;
    protected String krbConfiguredPassword;

    protected boolean usesDefaultKeyStore = true;
    protected long nonDefaultKeystoreId;
    protected String keyId;
    private HttpMethod httpMethod;

    // WARNING
    // WARNING : If you add properties, update the copyFrom method
    // WARNING

    public HttpRoutingAssertion() {
        this(null, null, null, null);
    }

    public HttpRoutingAssertion(String protectedServiceUrl) {
        this(protectedServiceUrl, null, null, null);
    }


    public HttpRoutingAssertion(String protectedServiceUrl, String login, String password, String realm) {
        this(protectedServiceUrl, login, password, realm, DEFAULT_MAX_CONNECTIONS_PER_HOST);
    }

    /** Subclasses can choose to offer this functionality by adding a public method that chains to this one. */
    protected void copyFrom(HttpRoutingAssertion source) {
        super.copyFrom(source);
        this.setCustomIpAddresses(source.getCustomIpAddresses());
        this.setFailoverStrategyName(source.getFailoverStrategyName());
        this.setFailOnErrorStatus(source.isFailOnErrorStatus());
        this.setFollowRedirects(source.isFollowRedirects());
        this.setLogin(source.getLogin());
        this.setMaxConnections(source.getMaxConnections());
        this.setPassword(source.getPassword());
        this.setProtectedServiceUrl(source.getProtectedServiceUrl());
        this.setPassthroughHttpAuthentication(source.isPassthroughHttpAuthentication());
        this.setRealm(source.getRealm());
        this.setUserAgent(source.getUserAgent());
        this.setTaiCredentialChaining(source.isTaiCredentialChaining());
        this.setNtlmHost(source.getNtlmHost());
        this.setConnectionTimeout(source.getConnectionTimeout());
        this.setTimeout(source.getTimeout());
        this.setRequestMsgSrc(source.getRequestMsgSrc());
        this.setRequestHeaderRules(source.getRequestHeaderRules());
        this.setRequestParamRules(source.getRequestParamRules());
        this.setResponseMsgDest(source.getResponseMsgDest());
        this.setResponseHeaderRules(source.getResponseHeaderRules());
        this.setKrbConfiguredAccount(source.getKrbConfiguredAccount());
        this.setKrbConfiguredPassword(source.getKrbConfiguredPassword());
        this.setKrbDelegatedAuthentication(source.isKrbDelegatedAuthentication());
        this.setKrbUseGatewayKeytab(source.isKrbUseGatewayKeytab());
        this.setGzipEncodeDownstream(source.isGzipEncodeDownstream());
        this.setCustomURLs(source.getCustomURLs());
        this.setKeyAlias(source.getKeyAlias());
        this.setUsesDefaultKeyStore(source.isUsesDefaultKeyStore());
        this.setNonDefaultKeystoreId(source.getNonDefaultKeystoreId());
        this.setHttpMethod(source.getHttpMethod());
    }

    @Override
    public HttpRoutingAssertion clone() {
        HttpRoutingAssertion hra = (HttpRoutingAssertion) super.clone();

        hra.responseHeaderRules = responseHeaderRules.clone();
        hra.requestHeaderRules = requestHeaderRules.clone();
        hra.requestParamRules = requestParamRules.clone();

        return hra;
    }

    /**
     * Full constructor.
     *
     * @param protectedServiceUrl the service url
     * @param login               protected service login
     * @param password            protected service password
     * @param realm               protected servcie realm
     */
    public HttpRoutingAssertion(String protectedServiceUrl, String login, String password, String realm, int maxConnections) {
        this.protectedServiceUrl = protectedServiceUrl;
        this.login = login;
        this.password = password;
        this.realm = realm;
        this.passthroughHttpAuthentication = false;
        this.maxConnections = maxConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public boolean isFailOnErrorStatus() {
        return failOnErrorStatus;
    }

    public void setFailOnErrorStatus(boolean failOnErrorStatus) {
        this.failOnErrorStatus = failOnErrorStatus;
    }

    /**
     * @return <code>null</code> for default request; otherwise name of a message type context variable
     */
    public String getRequestMsgSrc() {
        return requestMsgSrc;
    }

    /**
     * Sets the request message source.
     *
     * @param variableName <code>null</code> for default request; otherwise name of a message type context variable
     */
    public void setRequestMsgSrc(String variableName) {
        requestMsgSrc = variableName;
    }

    /**
     * @return <code>null</code> for default request; otherwise name of a message type context variable
     */
    public String getResponseMsgDest() {
        return responseMsgDest;
    }

    /**
     * Sets the response message source.
     *
     * @param variableName  <code>null</code> for default request; otherwise name of a message type context variable (either existing or to be created)
     */
    public void setResponseMsgDest(String variableName) {
        responseMsgDest = variableName;
    }

    public HttpPassthroughRuleSet getResponseHeaderRules() {
        return responseHeaderRules;
    }

    public void setResponseHeaderRules(HttpPassthroughRuleSet responseHeaderRules) {
        this.responseHeaderRules = responseHeaderRules;
    }

    public HttpPassthroughRuleSet getRequestHeaderRules() {
        return requestHeaderRules;
    }

    public void setRequestHeaderRules(HttpPassthroughRuleSet requestHeaderRules) {
        this.requestHeaderRules = requestHeaderRules;
    }

    public HttpPassthroughRuleSet getRequestParamRules() {
        return requestParamRules;
    }

    public void setRequestParamRules(HttpPassthroughRuleSet requestParamRules) {
        this.requestParamRules = requestParamRules;
    }


    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public boolean isPassthroughHttpAuthentication() {
        return passthroughHttpAuthentication;
    }

    public void setPassthroughHttpAuthentication(boolean passthroughHttpAuthentication) {
        this.passthroughHttpAuthentication = passthroughHttpAuthentication;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * This is really the NTLM domain--the client never actually sends the realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * This is really the NTLM domain--the client never actually sends the realm
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Migration(mapName = NONE, mapValue = OPTIONAL, valueType = HTTP_URL, resolver = PropertyResolver.Type.VALUE_REFERENCE)
    public String getProtectedServiceUrl() {
        return protectedServiceUrl;
    }

    public void setProtectedServiceUrl(String protectedServiceUrl) {
        this.protectedServiceUrl = protectedServiceUrl;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public String toString() {
        return super.toString() + " url=" + getProtectedServiceUrl() + " login=" + getLogin() + " realm=" + getRealm();
    }

    /** @return the custom IP addresses to use as an array of String, or null if no custom IP address list is configured. */
    @Migration(mapName = NONE, mapValue = OPTIONAL, valueType = IP_ADDRESS_ARRAY, resolver = PropertyResolver.Type.VALUE_REFERENCE)
    public String[] getCustomIpAddresses() {
        return customIpAddresses;
    }


    @Migration(mapName = NONE, mapValue = OPTIONAL, valueType = HTTP_URL_ARRAY, resolver = PropertyResolver.Type.VALUE_REFERENCE)
    public String[] getCustomURLs() {
        return customURLs;
    }

    public void setCustomURLs(String[] customURLs) {
        this.customURLs = customURLs;
    }

    /** @param customIpAddresses custom addresses to use, or null if no custom addresses should be used. */
    public void setCustomIpAddresses(String[] customIpAddresses) {
        this.customIpAddresses = customIpAddresses;
    }

    /**
     * @return the name of the FailoverStrategy to use with the CustomIpAddresses.
     * @see com.l7tech.common.io.failover.FailoverStrategyFactory
     */
    public String getFailoverStrategyName() {
        return failoverStrategyName;
    }

    /**
     * @param failoverStrategyName the name of the FailoverStrategy to use with the CustomIpAddresses.
     * @see com.l7tech.common.io.failover.FailoverStrategyFactory
     */
    public void setFailoverStrategyName(String failoverStrategyName) {
        this.failoverStrategyName = failoverStrategyName;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        StringBuffer tmp = new StringBuffer();
        if (!StringUtils.isEmpty(login)) tmp.append(login);
        if (!StringUtils.isEmpty(password)) tmp.append(password);
        if (!StringUtils.isEmpty(protectedServiceUrl)) tmp.append(protectedServiceUrl);
        if (!StringUtils.isEmpty(ntlmHost)) tmp.append(ntlmHost);
        if (!StringUtils.isEmpty(realm)) tmp.append(realm);
        if (!StringUtils.isEmpty(krbConfiguredAccount)) tmp.append(krbConfiguredAccount);
        if (!StringUtils.isEmpty(krbConfiguredPassword)) tmp.append(krbConfiguredPassword);
        if (customURLs != null) tmp.append(Arrays.toString(customURLs));
        if (customIpAddresses != null) tmp.append(Arrays.toString(customIpAddresses));

        if (requestMsgSrc != null) tmp.append(Syntax.SYNTAX_PREFIX).append(requestMsgSrc).append(Syntax.SYNTAX_SUFFIX);

        HttpPassthroughRuleSet[] ruleset = {responseHeaderRules, requestHeaderRules, requestParamRules};
        for( HttpPassthroughRuleSet rules : ruleset ) {
            if( !rules.isForwardAll() ) {
                for( HttpPassthroughRule rule : rules.getRules() ) {
                    if( !StringUtils.isEmpty( rule.getCustomizeValue() ) ) {
                        tmp.append( rule.getCustomizeValue() );
                    }
                }
            }
        }
        return Syntax.getReferencedNames(tmp.toString());
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        final List<VariableMetadata> vars = new ArrayList<VariableMetadata>();
        vars.add(new VariableMetadata(VAR_ROUTING_LATENCY, false, false, VAR_ROUTING_LATENCY, false));
        vars.add(new VariableMetadata(VAR_HTTP_ROUTING_URL, false, false, VAR_HTTP_ROUTING_URL, false));
        vars.add(new VariableMetadata(VAR_SERVICE_URL, false, false, VAR_SERVICE_URL, false));
        if (responseMsgDest != null) {
            vars.add(new VariableMetadata(responseMsgDest, false, false, responseMsgDest, true, DataType.MESSAGE));
        }
        return vars.toArray(new VariableMetadata[vars.size()]);
    }

    public boolean isTaiCredentialChaining() {
        return taiCredentialChaining;
    }

    public void setTaiCredentialChaining(boolean taiCredentialChaining) {
        this.taiCredentialChaining = taiCredentialChaining;
    }

    public String getNtlmHost() {
        return ntlmHost;
    }

    public void setNtlmHost(String ntlmHost) {
        this.ntlmHost = ntlmHost;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        this.usesDefaultKeyStore = usesDefault;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public long getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    @Override
    public String getKeyAlias() {
        return keyId;
    }

    @Override
    public void setKeyAlias(String keyid) {
        this.keyId = keyid;
    }

    public boolean isGzipEncodeDownstream() {
        return gzipEncodeDownstream;
    }

    public void setGzipEncodeDownstream(boolean gzipEncodeDownstream) {
        this.gzipEncodeDownstream = gzipEncodeDownstream;
    }

    public boolean isKrbDelegatedAuthentication() {
        return krbDelegatedAuthentication;
    }

    public void setKrbDelegatedAuthentication(boolean krbDelegatedAuthentication) {
        this.krbDelegatedAuthentication = krbDelegatedAuthentication;
    }

    public String getKrbConfiguredPassword() {
        return krbConfiguredPassword;
    }

    public void setKrbConfiguredPassword(String krbConfiguredPassword) {
        this.krbConfiguredPassword = krbConfiguredPassword;
    }

    public String getKrbConfiguredAccount() {
        return krbConfiguredAccount;
    }

    public void setKrbConfiguredAccount(String krbConfiguredAccount) {
        this.krbConfiguredAccount = krbConfiguredAccount;
    }

    public boolean isKrbUseGatewayKeytab() {
        return krbUseGatewayKeytab;
    }

    public void setKrbUseGatewayKeytab(boolean krbUseGatewayKeytab) {
        this.krbUseGatewayKeytab = krbUseGatewayKeytab;
    }

    /**
     * @return overridden HTTP method, or null to use default behavior.
     */
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * @param httpMethod an HTTP method to force, or null to allow the assertion to choose one automatically.
     */
    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    final static String baseName = "Route via HTTP(S)";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<HttpRoutingAssertion>(){
        @Override
        public String getAssertionName( final HttpRoutingAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            StringBuffer assertionName = new StringBuffer("Route via HTTP");
            String url = assertion.getProtectedServiceUrl();
            if(url != null){
                if(url.startsWith("https")) assertionName.append("S");
                assertionName.append(" to ").append(url);
            }
            return AssertionUtils.decorateName(assertion, assertionName.toString());
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"routing"});
        
        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "The incoming message will be routed via http(S) to the protected service at the designated URL.      ");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.HttpRoutingAssertionPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "HTTP(S) Routing Properties");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(HttpMethod.class, "httpMethod")
        )));

        return meta;
    }

    public static String getVarHttpRoutingUrlHost() {
        return VAR_HTTP_ROUTING_URL + "." + VAR_HTTP_ROUTING_URL_SUFFIX_HOST;
    }

    public static String getVarHttpRoutingUrlProtocol() {
        return VAR_HTTP_ROUTING_URL + "." + VAR_HTTP_ROUTING_URL_SUFFIX_PROTOCOL;
    }

    public static String getVarHttpRoutingUrlPort() {
        return VAR_HTTP_ROUTING_URL + "." + VAR_HTTP_ROUTING_URL_SUFFIX_PORT;
    }

    public static String getVarHttpRoutingUrlFile() {
        return VAR_HTTP_ROUTING_URL + "." + VAR_HTTP_ROUTING_URL_SUFFIX_FILE;
    }

    public static String getVarHttpRoutingUrlPath() {
        return VAR_HTTP_ROUTING_URL + "." + VAR_HTTP_ROUTING_URL_SUFFIX_PATH;
    }

    public static String getVarHttpRoutingUrlQuery() {
        return VAR_HTTP_ROUTING_URL + "." + VAR_HTTP_ROUTING_URL_SUFFIX_QUERY;
    }

    public static String getVarHttpRoutingUrlFragment() {
        return VAR_HTTP_ROUTING_URL + "." + VAR_HTTP_ROUTING_URL_SUFFIX_FRAGMENT;
    }
}
