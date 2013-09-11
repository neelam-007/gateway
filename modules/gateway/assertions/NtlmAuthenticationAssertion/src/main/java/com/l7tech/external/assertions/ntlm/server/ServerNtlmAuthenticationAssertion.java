package com.l7tech.external.assertions.ntlm.server;


import com.l7tech.common.http.HttpConstants;
import com.l7tech.external.assertions.ntlm.NtlmAuthenticationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.ntlm.adapter.NetlogonAdapter;
import com.l7tech.ntlm.protocol.AuthenticationManagerException;
import com.l7tech.ntlm.protocol.AuthenticationProvider;
import com.l7tech.ntlm.protocol.NtlmAuthenticationProvider;
import com.l7tech.ntlm.protocol.NtlmAuthenticationServer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.NtlmToken;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpCredentialSource;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import jcifs.smb.SID;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Server side implementation of the NtlmAuthenticationAssertion.
 *
 * @see com.l7tech.external.assertions.ntlm.NtlmAuthenticationAssertion
 */
public class ServerNtlmAuthenticationAssertion extends ServerHttpCredentialSource<NtlmAuthenticationAssertion> {
    private static final Logger log = Logger.getLogger(ServerNtlmAuthenticationAssertion.class.getName());

    public static final String NTLM_SCHEME = "NTLM";
    public static final String NEGOTIATE_SCHEME = "Negotiate";

    protected final ThreadLocal<Pair<Object, AuthenticationProvider>> ntlmAuthenticationProviderThreadLocal = new ThreadLocal<Pair<Object, AuthenticationProvider>>();

    private final String[] variablesUsed;

    private final IdentityProviderFactory identityProviderFactory;


    public ServerNtlmAuthenticationAssertion(final NtlmAuthenticationAssertion assertion, ApplicationContext ctx) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        identityProviderFactory = ctx.getBean("identityProviderFactory", IdentityProviderFactory.class);
        checkIdentityProviderExists();
    }

    private void checkIdentityProviderExists() throws PolicyAssertionException {
        try {
            if( identityProviderFactory.getProvider(assertion.getLdapProviderOid()) ==null)
                throw new PolicyAssertionException(assertion, "Identity Provider Instance not Found");
        } catch (FindException e) {
            throw new PolicyAssertionException(assertion, ExceptionUtils.getMessage(e));
        }
    }

    @Override
    protected String realm() {
        return "";
    }

    @Override
    protected AssertionStatus checkAuthParams(Map<String, String> authParams) {
        return AssertionStatus.NONE;
    }

    @Override
    protected Map<String, String> findCredentialAuthParams(LoginCredentials pc, Map<String, String> authParam) {
        return authParam;
    }

    @Override
    protected Map<String, String> challengeParams(Message request, Map<String, String> authParams) {
        return null;
    }


    /**
     * There are 2 types of challenge for NTLM, depends on the state of request.
     * 1. Return "NTLM" when there is no authorization header
     * 2. Return "NTLM" with a challenge or nonce in the format of NTLM ${server_challenge}
     *
     * @param context
     * @param authParams
     */
    @Override
    protected void challenge(PolicyEnforcementContext context, Map<String, String> authParams) {
        StringBuilder challengeHeader = new StringBuilder();
        String scheme = null;
        try {
            scheme = getAuthorizationScheme(context.getRequest());
        } catch (IOException e) {
           scheme = scheme();
        }
        if(scheme == null){
            scheme = scheme();
        }
        if (authParams.containsKey(scheme)) {
            challengeHeader.append(scheme);
            challengeHeader.append(" ");
            challengeHeader.append(authParams.get(scheme));
        }
        else{
            challengeHeader.append(scheme);
        }

        String challenge = challengeHeader.toString();
        if(challenge.length() > 0) {
            logAndAudit(AssertionMessages.HTTPCREDS_CHALLENGING, challenge);
            HttpResponseKnob httpResponse = context.getResponse().getHttpResponseKnob();
            httpResponse.addChallenge(challenge);
        }
    }

    @Override
    protected String scheme() {
        return NTLM_SCHEME;
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        checkIdentityProviderExists();
        AssertionStatus status = super.checkRequest(context);
        if (status != AssertionStatus.NONE)
            return status;


        AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        LoginCredentials credential = authContext.getLastCredentials();
        if (credential != null) {
            if (credential.getSecurityToken() instanceof NtlmToken) {
                NtlmToken securityToken = (NtlmToken) credential.getSecurityToken();
                setContextVariables(context, securityToken);
            }
        }

        return status;
    }

    private void setContextVariables(PolicyEnforcementContext context, NtlmToken securityToken) throws PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());
        //get context variable prefix
        String variablePrefix = ExpandVariables.process(assertion.getVariablePrefix(), vars, getAudit());
        variablePrefix = variablePrefix != null ? variablePrefix : NtlmAuthenticationAssertion.DEFAULT_PREFIX;
        HashMap userAccountInfo = (HashMap) securityToken.getParams().get("account.info");
        if (userAccountInfo != null) {
            if (userAccountInfo.containsKey(NtlmAuthenticationAssertion.USER_LOGIN_NAME)) {
                final String userLoginName = (String) userAccountInfo.get(NtlmAuthenticationAssertion.USER_LOGIN_NAME);
                context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.USER_LOGIN_NAME, userLoginName);
                logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_USER_AUTHENTICATED, userLoginName);
            } else {
                logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_MISSING_AUTHORIZATION_ATTRIBUTE, NtlmAuthenticationAssertion.USER_LOGIN_NAME);
                throw new PolicyAssertionException(assertion, "sAMAccountName attribute is null!");
            }
            String sid = null;
            if (userAccountInfo.containsKey("primaryGroupSid")) {
                SID primaryGroupSID = (SID) userAccountInfo.get("primaryGroupSid");
                sid = primaryGroupSID.toDisplayString();
            }
            context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.SID, sid);
            context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_FLAGS, userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_FLAGS));
            context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.DOMAIN_NAME, userAccountInfo.get(NtlmAuthenticationAssertion.DOMAIN_NAME));
            String sessionKey = null;
            if (userAccountInfo.containsKey(NtlmAuthenticationAssertion.SESSION_KEY)) {
                sessionKey = HexUtils.encodeBase64((byte[]) userAccountInfo.get(NtlmAuthenticationAssertion.SESSION_KEY), true);
            }
            context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.SESSION_KEY, sessionKey);
            context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_FULL_NAME, userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_FULL_NAME));
            context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_HOME_DIR, userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_HOME_DIR));
            context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_DIR_DRIVE, userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_DIR_DRIVE));
            String[] sidGroups = null;
            if (userAccountInfo.containsKey(NtlmAuthenticationAssertion.ACCOUNT_SIDS)) {
                Set<SID> groupSet = (Set<SID>) userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_SIDS);
                List<String> sidGroupList = new ArrayList<String>();
                for (SID group : groupSet) {
                    sidGroupList.add(group.toDisplayString());
                }
                sidGroups = sidGroupList.toArray(new String[0]);
            }
            context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_SIDS, sidGroups);

        } else {
            logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_MISSING_ACCOUNT_INFO);
            throw new PolicyAssertionException(assertion, "Account Info of the authenticated account is null");
        }
    }


    @Override
    protected LoginCredentials findCredentials(Message request, Map<String, String> authParams)
            throws IOException, CredentialFinderException {

        Object connectionId = request.getHttpRequestKnob().getConnectionIdentifier();
        log.log(Level.FINE, "Connection ID=" + connectionId);

        LoginCredentials loginCredentials = null;
        AuthenticationProvider authenticationProvider = getOrCreateAuthenticationProvider(connectionId);

        NtlmToken token = checkProviderStatus(authenticationProvider);
        if (token == null) {
            String authorizationScheme = getAuthorizationScheme(request);
            if(authorizationScheme == null) {
                return null;
            }
            byte[] authorizationData = getAuthorizationData(request, authorizationScheme);
            if (authorizationData == null) {
                //needs authentication
                //the challenge will be send to the client requesting NTLM authntication
                return null;
            }
            token = authenticateClient(authorizationScheme, authorizationData, authParams, authenticationProvider);
        } else if (!checkConnectionTimeouts(token, connectionId)) {
            //create a new provider that will reset the state
            log.log(Level.FINE, "Connection has expired. Recessing the provider status");
            authenticationProvider.resetAuthentication();

            return null;
        }
        if (token != null) {
            loginCredentials = LoginCredentials.makeLoginCredentials(token, assertion.getClass());
            if (token.getParams() != null && token.getParams().containsKey("sAMAccountName")) {
                logAndAudit(AssertionMessages.HTTPCREDS_FOUND_USER, (String) token.getParams().get("sAMAccountName"));
            }
            return loginCredentials;
        }
        return null;
    }

    private AuthenticationProvider getOrCreateAuthenticationProvider(Object connectionId) throws CredentialFinderException {
        AuthenticationProvider authenticationProvider = null;
        Pair<Object, AuthenticationProvider> pair = ntlmAuthenticationProviderThreadLocal.get();
        //in the existing implementation connection ID is tied to a processing thread this means, the connection id remains the same for the duration of a
        //client connection. Once the connection ID changes the provider should be reset
        if (pair == null || !connectionId.equals(pair.left)) {
            authenticationProvider = createAuthenticationProvider();
            Pair<Object, AuthenticationProvider> newPair = new Pair<Object, AuthenticationProvider>(connectionId, authenticationProvider);
            ntlmAuthenticationProviderThreadLocal.set(newPair);
        } else {
            authenticationProvider = pair.right;
        }

        return authenticationProvider;
    }

    protected AuthenticationProvider createAuthenticationProvider() throws CredentialFinderException {
        AuthenticationProvider authenticationProvider;
        try {
            LdapIdentityProvider ldapProvider = (LdapIdentityProvider) identityProviderFactory.getProvider(assertion.getLdapProviderOid());
            if (ldapProvider == null) {
                throw new FindException("Identity Provider with oid=" + String.valueOf(assertion.getLdapProviderOid()) + " does not exist");
            }
            LdapIdentityProviderConfig providerConfig = ldapProvider.getConfig();

            Map<String, String> props = new HashMap(providerConfig.getNtlmAuthenticationProviderProperties());
            if (props.size() == 0 || !Boolean.TRUE.toString().equals(props.get("enabled"))) {
                throw new FindException("NTLM Configuration is disabled or missing");
            }

            //find password property and replace it if password is stored as a secure password
            if (props.containsKey("service.passwordOid")) {
                try {
                    Goid goid = GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, props.get("service.passwordOid"));
                    String plaintextPassword = ServerVariables.getSecurePasswordByGoid(new LoggingAudit(logger), goid);
                    props.put("service.password", plaintextPassword);
                } catch (FindException | NumberFormatException e) {
                    throw new CredentialFinderException("Password is invalid");
                }
            }

            authenticationProvider = new NtlmAuthenticationServer(props, new NetlogonAdapter(props));
        } catch (FindException e) {
            final String errorMsg = "Unable to find the Identity Provider instance";
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, errorMsg, e);
            }
            logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_IDENTITY_PROVIDER_CONFIG_FAILURE, new String[]{errorMsg}, ExceptionUtils.getDebugException(e));
            throw new CredentialFinderException(errorMsg);
        } catch (AuthenticationManagerException ame) {
            final String errorMsg = "Invalid NETLOGON credentials";
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, errorMsg, ame);
            }
            logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_IDENTITY_PROVIDER_CONFIG_FAILURE, new String[]{errorMsg}, ExceptionUtils.getDebugException(ame));
            throw new CredentialFinderException(errorMsg);
        }
        return authenticationProvider;
    }

    private String getAuthorizationScheme(Message request) throws  IOException {
        String authorization = request.getHttpRequestKnob().getHeaderSingleValue(HttpConstants.HEADER_AUTHORIZATION);

        if (authorization == null || authorization.length() == 0) {
            logAndAudit(AssertionMessages.HTTPCREDS_NO_AUTHN_HEADER);
            return null;
        }
        int index = authorization.indexOf(' ');
        if (index != -1) {
            String scheme = authorization.substring(0, index);
            if(NTLM_SCHEME.equalsIgnoreCase(scheme) || NEGOTIATE_SCHEME.equalsIgnoreCase(scheme)) {  //only known authorization schemes returned
                return scheme;
            }
        }
        return null;
    }


    private byte[] getAuthorizationData(Message request, String scheme) throws IOException {
        String authorization = request.getHttpRequestKnob().getHeaderSingleValue(HttpConstants.HEADER_AUTHORIZATION);

        if(authorization.length() > scheme.length() + 1) {
            return HexUtils.decodeBase64(authorization.substring(scheme.length() + 1));
        }
        return null;
    }


    private NtlmToken authenticateClient(String authorizationScheme, byte[] encryptedNtlmData, Map<String, String> authParams, AuthenticationProvider authenticationProvider) throws CredentialFinderException {
        NtlmToken securityToken = null;
        //decode NTLM protocol data and check user credentials against AD
        try {
            byte[] ntlmToken = authenticationProvider.processAuthentication(encryptedNtlmData);
            securityToken = checkProviderStatus(authenticationProvider);
            if (securityToken == null) {
                authParams.put(authorizationScheme, HexUtils.encodeBase64(ntlmToken, true));
            }
        } catch (AuthenticationManagerException e) {
            final String errMessage = "NTLM Authentication failed";
            log.log(Level.FINE, errMessage + ": " + e.getMessage());
            logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_FAILED, new String[]{errMessage}, ExceptionUtils.getDebugException(e));
            throw new CredentialFinderException(errMessage, e, AssertionStatus.AUTH_FAILED);
        } finally {
            if (authenticationProvider.getNtlmAuthenticationState().getState() == NtlmAuthenticationProvider.State.FAILED) {
                authenticationProvider.resetAuthentication();
            }
        }

        return securityToken;
    }

    private NtlmToken checkProviderStatus(AuthenticationProvider authenticationProvider) throws CredentialFinderException {
        String ntlmData = "";
        NtlmToken securityToken = null;
        Map<String, Object> params = new HashMap<String, Object>();
        if (authenticationProvider.getNtlmAuthenticationState().isComplete()) {
            Map<String, Object> accountInfo = authenticationProvider.getNtlmAuthenticationState().getAccountInfo();

            if (accountInfo == null) {
                throw new CredentialFinderException("NTLM account info cannot be null!", AssertionStatus.FAILED);
            }
            ntlmData = HexUtils.encodeBase64(authenticationProvider.getNtlmAuthenticationState().getSessionKey(), true);

            long currentMills = System.currentTimeMillis();
            if (!accountInfo.containsKey("session.authenticate.time")) {
                accountInfo.put("session.authenticate.time", currentMills);
            }
            accountInfo.put("request.idle.time", currentMills);
            params.put("account.info", accountInfo);
            securityToken = new NtlmToken(ntlmData, null, params);
        }

        return securityToken;
    }

    protected boolean checkConnectionTimeouts(final NtlmToken securityToken, final Object connectionId) {
        long maxConnectionDuration = assertion.getMaxConnectionDuration();
        long maxIdleTimeout = assertion.getMaxConnectionIdleTime();
        if (securityToken != null) {
            Map<String, Object> accountInfo = (Map) securityToken.getParams().get("account.info");
            if (accountInfo == null) {
                return false;
            }
            long currentMills = System.currentTimeMillis();
            Long sessionTime = (Long) accountInfo.get("session.authenticate.time");
            log.log(Level.FINE, "Checking session expiry...");
            if (maxConnectionDuration == NtlmAuthenticationAssertion.DEFAULT_MAX_CONNECTION_DURATION || sessionTime != null && currentMills - sessionTime < maxConnectionDuration * 1000) {
                Long requestIdleTime = (Long) accountInfo.get("request.idle.time");
                if (maxIdleTimeout == NtlmAuthenticationAssertion.DEFAULT_MAX_IDLE_TIMEOUT || requestIdleTime != null && currentMills - requestIdleTime < maxIdleTimeout * 1000) {
                    return true;
                } else {
                    logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_IDLE_TIMEOUT_EXPIRED, connectionId.toString());
                    log.log(Level.FINE, "Connection idle timeout expired for connection " + connectionId);
                }
            } else {
                logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_CONNECTION_EXPIRED, connectionId.toString());
                log.log(Level.FINE, "Connection " + connectionId + " has expired!");
            }
        }

        return false;
    }


}
