/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.*;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.OPTIONAL;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

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
public class HttpRoutingAssertion extends RoutingAssertionWithSamlSV implements UsesEntities, UsesVariables, SetsVariables, OptionalPrivateKeyable
{
    public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = -1;
    public static final String VAR_ROUTING_LATENCY = "httpRouting.latency";
    public static final String VAR_HTTP_ROUTING_URL = "httpRouting.url";
    public static final String VAR_HTTP_ROUTING_REASON_CODE = "httpRouting.reasonCode";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_HOST = "host";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_PROTOCOL = "protocol";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_PORT = "port";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_FILE = "file";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_PATH = "path";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_QUERY = "query";
    public static final String VAR_HTTP_ROUTING_URL_SUFFIX_FRAGMENT = "fragment";
    public static final String VAR_HTTP_ROUTING_URL_BLACKLIST = "blacklist";
    public static final String PROP_SSL_SESSION_TIMEOUT = HttpRoutingAssertion.class.getName() + ".sslSessionTimeoutSeconds";
    public static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;
    public static final String KERBEROS_DATA = "kerberos.data";
    
    private static final String META_INITIALIZED = HttpRoutingAssertion.class.getName() + ".metadataInitialized";

    @Deprecated 
    public static final String VAR_SERVICE_URL = "service.url";

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
    protected String connectionTimeout;
    protected String timeout;
    protected int maxRetries = -1;
    protected String requestMsgSrc;
    protected String responseMsgDest;
    protected String responseSize = null;
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
    protected boolean passThroughSoapFaults = true;
    protected boolean gzipEncodeDownstream;

    protected boolean krbDelegatedAuthentication;
    protected boolean krbUseGatewayKeytab;
    protected String krbConfiguredAccount;
    protected String krbConfiguredPassword;

    protected boolean usesDefaultKeyStore = true;
    protected boolean usesNoKey = false;
    protected Goid nonDefaultKeystoreId;
    protected String keyId;
    private HttpMethod httpMethod;
    private GenericHttpRequestParams.HttpVersion httpVersion;
    private boolean useKeepAlives = true;
    private String proxyHost = null;
    private int proxyPort = -1;
    private String proxyUsername = "";
    private String proxyPassword = "";

    protected String tlsVersion;
    protected String tlsCipherSuites;
    protected Goid[] tlsTrustedCertGoids;
    protected String[] tlsTrustedCertNames;

    private boolean forceIncludeRequestBody = false;
    private String httpMethodAsString;

    public HttpRoutingAssertion() {
        this(null, null, null, null);
    }

    public HttpRoutingAssertion(String protectedServiceUrl) {
        this(protectedServiceUrl, null, null, null);
    }


    public HttpRoutingAssertion(String protectedServiceUrl, String login, String password, String realm) {
        this(protectedServiceUrl, login, password, realm, DEFAULT_MAX_CONNECTIONS_PER_HOST);
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

    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Deprecated
    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout.toString();
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    @Deprecated
    public void setTimeout(Integer timeout) {
        this.timeout = timeout.toString();
    }

    /**
     * Get the maximum number of retries (-1 if not specified)
     *
     * @return The maximum number of retries.
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries( final int maxRetries ) {
        this.maxRetries = maxRetries;
    }

    public boolean isFailOnErrorStatus() {
        return failOnErrorStatus;
    }

    public void setFailOnErrorStatus(boolean failOnErrorStatus) {
        this.failOnErrorStatus = failOnErrorStatus;
    }

    public boolean isPassThroughSoapFaults() {
        return passThroughSoapFaults;
    }

    public void setPassThroughSoapFaults(boolean passThroughSoapFaults) {
        this.passThroughSoapFaults = passThroughSoapFaults;
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

    public String getResponseSize(){
        return responseSize;
    }

    public void setResponseSize(String responseSize){
        this.responseSize = responseSize;
    }

    @Override
    public boolean initializesRequest() {
        return false;
    }

    @Override
    public boolean needsInitializedRequest() {
        return null == requestMsgSrc;
    }

    @Override
    public boolean initializesResponse() {
        return null == responseMsgDest;
    }

    @Override
    public boolean needsInitializedResponse() {
        return false;
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

    @WspSensitive
    @Dependency(methodReturnType = Dependency.MethodReturnType.VARIABLE, type = Dependency.DependencyType.SECURE_PASSWORD)
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

    /**
     * @return GOIDs of a subset of trusted certs to trust as server certs for outbound TLS, or null.
     */
    public Goid[] getTlsTrustedCertGoids() {
        return tlsTrustedCertGoids;
    }

    public void setTlsTrustedCertGoids(@Nullable Goid[] tlsTrustedCertGoids) {
        this.tlsTrustedCertGoids = tlsTrustedCertGoids;
    }

    // For backward compat while parsing pre-GOID policies.  Not needed for new assertions.
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setTlsTrustedCertOids(@Nullable Long[] oids) {
        this.tlsTrustedCertGoids = GoidUpgradeMapper.mapOids(EntityType.TRUSTED_CERT, oids);
    }

    /**
     * @return names corresponding to elements of tlsTrustedCertOids, or null.
     *         names array is not guaranteed to be present even if OIDs array is present.
     *         if names array is present, it is not guaranteed to be the same size as (or even as large as) the OIDs array.
     */
    public String[] getTlsTrustedCertNames() {
        return tlsTrustedCertNames;
    }

    /**
     * Utility method for looking up a cert name, respecting the condition that the cert names array may not exist
     * or may be smaller than the OIDs array.
     *
     * @param i index of OID whose name to look up
     * @return the corresponding name from TlsTrustedCertNames, or null if not available.
     */
    public String certName(int i) {
        return tlsTrustedCertNames == null || i >= tlsTrustedCertNames.length ? null : tlsTrustedCertNames[i];
    }

    public void setTlsTrustedCertNames(@Nullable String[] tlsTrustedCertNames) {
        this.tlsTrustedCertNames = tlsTrustedCertNames;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        if (tlsTrustedCertGoids == null || tlsTrustedCertGoids.length < 1)
            return new EntityHeader[0];
        
        EntityHeader[] ret = new EntityHeader[tlsTrustedCertGoids.length];
        for (int i = 0; i < tlsTrustedCertGoids.length; i++) {
            Goid oid = tlsTrustedCertGoids[i];
            String name = certName(i);
            ret[i] = new EntityHeader(oid, EntityType.TRUSTED_CERT, name, "Server certificate trusted for outbound SSL/TLS connection");
        }
        return ret;
    }
    
    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (tlsTrustedCertGoids == null ||
                !EntityType.TRUSTED_CERT.equals(oldEntityHeader.getType()) || 
                !EntityType.TRUSTED_CERT.equals(newEntityHeader.getType()))
            return;

        for (int i = 0; i < tlsTrustedCertGoids.length; i++) {
            if (Goid.equals(tlsTrustedCertGoids[i], oldEntityHeader.getGoid())) {
                tlsTrustedCertGoids[i] = newEntityHeader.getGoid();
                if (tlsTrustedCertNames != null && tlsTrustedCertNames.length > i)
                    tlsTrustedCertNames[i] = newEntityHeader.getName();
            }
        }
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

    /**
     * @return TLS version, ie "TLSv1.2", or null to use the socket factory's default.
     */
    public String getTlsVersion() {
        return tlsVersion;
    }

    public void setTlsVersion(String tlsVersion) {
        this.tlsVersion = tlsVersion;
    }

    /**
     * @return TLS cipher suites, as comma-delimited string, or null to use the socket factory's defaults.
     */
    public String getTlsCipherSuites() {
        return tlsCipherSuites;
    }

    public void setTlsCipherSuites(String tlsCipherSuites) {
        this.tlsCipherSuites = tlsCipherSuites;
    }

    /**
     * @return The Http protocol version, or null to use the default http version.
     */
    public GenericHttpRequestParams.HttpVersion getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(GenericHttpRequestParams.HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * @return true if an entity body should be included with the request even if the HTTP method is one
     *          that would normally not include one (GET, HEAD, DELETE, etc).
     */
    public boolean isForceIncludeRequestBody() {
        return forceIncludeRequestBody;
    }

    /**
     * @param forceIncludeRequestBody true if an entity body should be included with the request even if the HTTP method
     *                                is one that would normally not include one (GET, HEAD, DELETE, etc).
     */
    public void setForceIncludeRequestBody(boolean forceIncludeRequestBody) {
        this.forceIncludeRequestBody = forceIncludeRequestBody;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        final List<String> expressions = new ArrayList<String>();
        expressions.add(login);
        expressions.add(password);
        expressions.add(protectedServiceUrl);
        expressions.add(ntlmHost);
        expressions.add(realm);
        expressions.add(krbConfiguredAccount);
        expressions.add(krbConfiguredPassword);
        expressions.add(proxyPassword);
        expressions.add(responseSize);
        expressions.add(timeout);
        expressions.add(connectionTimeout);
        expressions.add(httpMethodAsString);
        if (customURLs != null) expressions.addAll( Arrays.asList( customURLs ) );
        if (customIpAddresses != null) expressions.addAll( Arrays.asList( customIpAddresses ) );
        expressions.add(Syntax.getVariableExpression(requestMsgSrc));

        HttpPassthroughRuleSet[] ruleset = {responseHeaderRules, requestHeaderRules, requestParamRules};
        for( HttpPassthroughRuleSet rules : ruleset ) {
            if( !rules.isForwardAll() ) {
                for( HttpPassthroughRule rule : rules.getRules() ) {
                    expressions.add( rule.getCustomizeValue() );
                }
            }
        }
        return Syntax.getReferencedNames( expressions.toArray( new String[ expressions.size() ] ) );
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        final List<VariableMetadata> vars = new ArrayList<VariableMetadata>();
        vars.add(new VariableMetadata(VAR_ROUTING_LATENCY, false, false, VAR_ROUTING_LATENCY, false));
        vars.add(new VariableMetadata(VAR_HTTP_ROUTING_URL, false, false, VAR_HTTP_ROUTING_URL, false));
        if (responseMsgDest != null) {
            vars.add(new VariableMetadata(responseMsgDest, false, false, responseMsgDest, true, DataType.MESSAGE));
        }
        if(krbUseGatewayKeytab || krbDelegatedAuthentication || krbConfiguredAccount != null ) {
            // kerberos data may be set
            vars.add(new VariableMetadata(KERBEROS_DATA, false, false, null, false, DataType.UNKNOWN));
        }
        vars.add(new VariableMetadata(VAR_HTTP_ROUTING_REASON_CODE, false, false, VAR_HTTP_ROUTING_REASON_CODE, false, DataType.INTEGER));
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
    public Goid getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId(Goid nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    @Deprecated
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, nonDefaultId);
    }

    @Override
    public String getKeyAlias() {
        return keyId;
    }

    @Override
    public void setKeyAlias(String keyid) {
        this.keyId = keyid;
    }

    @Override
    public boolean isUsesNoKeyAllowed() {
        return true;
    }

    @Override
    public boolean isUsesNoKey() {
        return usesNoKey;
    }

    @Override
    public void setUsesNoKey(boolean usesNoKey) {
        this.usesNoKey = usesNoKey;
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

    @WspSensitive
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

    /**
     * @return the custom method name for HttpMethod.OTHER, or null.
     */
    public String getHttpMethodAsString() {
        return httpMethodAsString;
    }

    /**
     * @param httpMethodAsString a custom method name for HttpMethod.OTHER, or null.
     */
    public void setHttpMethodAsString(String httpMethodAsString) {
        this.httpMethodAsString = httpMethodAsString;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    @WspSensitive
    @Dependency(methodReturnType = Dependency.MethodReturnType.VARIABLE, type = Dependency.DependencyType.SECURE_PASSWORD)
    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public boolean isUseKeepAlives() {
        return useKeepAlives;
    }

    public void setUseKeepAlives(boolean useKeepAlives) {
        this.useKeepAlives = useKeepAlives;
    }

    final static String baseName = "Route via HTTP(S)";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<HttpRoutingAssertion>(){
        @Override
        public String getAssertionName( final HttpRoutingAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            StringBuilder assertionName = new StringBuilder( "Route via HTTP" );
            String url = assertion.getProtectedServiceUrl();
            if(url != null){
                if(url.startsWith("https")) assertionName.append("S");
                if (!url.trim().isEmpty()) assertionName.append(" to ").append(url);
            }
            return AssertionUtils.decorateName(assertion, assertionName.toString());
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(PALETTE_FOLDERS, new String[]{"routing"});
        
        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "The incoming message will be routed via http(S) to the protected service at the designated URL.      ");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.HttpRoutingAssertionPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "HTTP(S) Routing Properties");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(HttpMethod.class, "httpMethod"),
                new Java5EnumTypeMapping(GenericHttpRequestParams.HttpVersion.class, "httpVersion")
        )));

        meta.put(POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.AddHttpRoutingAssertionAdvice");

        meta.put(META_INITIALIZED, Boolean.TRUE);
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

    public static String getVarHttpRoutingUrlBlacklist(){
        return VAR_HTTP_ROUTING_URL + "." + VAR_HTTP_ROUTING_URL_BLACKLIST;
    }
}
