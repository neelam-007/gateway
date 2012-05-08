package com.l7tech.external.assertions.ntlm.server;


import com.l7tech.common.http.HttpConstants;
import com.l7tech.external.assertions.ntlm.NtlmAuthenticationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.ntlm.netlogon.NetLogon;
import com.l7tech.ntlm.protocol.AuthenticationManagerException;
import com.l7tech.ntlm.protocol.AuthenticationProvider;
import com.l7tech.ntlm.protocol.NtlmAuthenticationProvider;
import com.l7tech.ntlm.protocol.NtlmAuthenticationServer;
import com.l7tech.objectmodel.FindException;
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
import com.l7tech.util.HexUtils;
import com.l7tech.util.NameValuePair;
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

    public static final String SCHEME = "NTLM";
    
    protected final ThreadLocal<Pair<Object, AuthenticationProvider>> ntlmAuthenticationProviderThreadLocal =  new ThreadLocal<Pair<Object,AuthenticationProvider>>();

    private final String[] variablesUsed;

    private final IdentityProviderFactory identityProviderFactory;


    public ServerNtlmAuthenticationAssertion(final NtlmAuthenticationAssertion assertion, ApplicationContext ctx) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        identityProviderFactory = ctx.getBean("identityProviderFactory", IdentityProviderFactory.class);
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
        StringBuilder challengeHeader = new StringBuilder(scheme());
        if(authParams.containsKey(scheme())) {
            challengeHeader.append(" ");
            challengeHeader.append(authParams.get(scheme()));
        }

        String challenge = challengeHeader.toString();

        logAndAudit(AssertionMessages.HTTPCREDS_CHALLENGING, challenge);
        HttpResponseKnob httpResponse = context.getResponse().getHttpResponseKnob();
        httpResponse.addChallenge(challenge);
    }

    @Override
    protected String scheme() {
        return SCHEME;
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
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
        //get prefix the default on is protocol
        String variablePrefix = ExpandVariables.process(assertion.getVariablePrefix(), vars, getAudit());
        variablePrefix = variablePrefix != null ? variablePrefix : NtlmAuthenticationAssertion.DEFAULT_PREFIX;
        HashMap userAccountInfo = (HashMap) securityToken.getParams().get("account.info");
        if (userAccountInfo != null) {
            if(userAccountInfo.containsKey(NtlmAuthenticationAssertion.USER_LOGIN_NAME)) {
                context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.USER_LOGIN_NAME, userAccountInfo.get(NtlmAuthenticationAssertion.USER_LOGIN_NAME));
            }
            else {
                logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_MISSING_AUTHORIZATION_ATTRIBUTE, NtlmAuthenticationAssertion.USER_LOGIN_NAME);
                throw new PolicyAssertionException(assertion, "sAMAccountName attribute is null!");
            }
            if(userAccountInfo.containsKey("primaryGroupSid")){
                SID primaryGroupSID = (SID) userAccountInfo.get("primaryGroupSid");
                context.setVariable(variablePrefix + ".sid", primaryGroupSID.toDisplayString());
            }
            if(userAccountInfo.containsKey(NtlmAuthenticationAssertion.ACCOUNT_FLAGS)) {
                context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_FLAGS, userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_FLAGS));
            }
            if(userAccountInfo.containsKey(NtlmAuthenticationAssertion.DOMAIN_NAME)) {
                context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.DOMAIN_NAME, userAccountInfo.get(NtlmAuthenticationAssertion.DOMAIN_NAME));
            }
            if(userAccountInfo.containsKey(NtlmAuthenticationAssertion.SESSION_KEY)) {
                context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.SESSION_KEY, userAccountInfo.get(NtlmAuthenticationAssertion.SESSION_KEY));
            }
            if(userAccountInfo.containsKey(NtlmAuthenticationAssertion.ACCOUNT_FULL_NAME)) {
                context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_FULL_NAME, userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_FULL_NAME));
            }
            if(userAccountInfo.containsKey(NtlmAuthenticationAssertion.ACCOUNT_HOME_DIR)) {
                context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_HOME_DIR, userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_HOME_DIR));
            }
            if(userAccountInfo.containsKey(NtlmAuthenticationAssertion.ACCOUNT_DIR_DRIVE)) {
                context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_DIR_DRIVE, userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_DIR_DRIVE));
            }
            if(userAccountInfo.containsKey(NtlmAuthenticationAssertion.ACCOUNT_SIDS)){
                Set<SID> groupSet = (Set<SID>)userAccountInfo.get(NtlmAuthenticationAssertion.ACCOUNT_SIDS);
                List<String> sidGroups = new ArrayList<String>();
                for(SID group : groupSet){
                    sidGroups.add(group.toDisplayString());
                }
                context.setVariable(variablePrefix + "." + NtlmAuthenticationAssertion.ACCOUNT_SIDS, sidGroups.toArray(new String[0]));
            }
            
        }
        else {
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
        if(token == null) {
            byte[] authorizationData = getAuthorizationData(request);
            if(authorizationData == null) {
                //needs authentication
                //the challenge will be send to the client requesting NTLM authntication
                return null;
            }
            token = authenticateClient(authorizationData, authParams, authenticationProvider);
        }
        else if(!checkConnectionTimeouts(token, connectionId)) {
            //create a new provider that will reset the state
            log.log(Level.FINE, "Connection has expired. Recessing the provider status");
            authenticationProvider.resetAuthentication();

            return null;
        }
        if (token != null) {
            loginCredentials = LoginCredentials.makeLoginCredentials(token, assertion.getClass());
            if(token.getParams() != null && token.getParams().containsKey("sAMAccountName")) {
                logAndAudit(AssertionMessages.HTTPCREDS_FOUND_USER, (String)token.getParams().get("sAMAccountName"));
            }
            return loginCredentials;
        }
        return null;
    }

    private AuthenticationProvider getOrCreateAuthenticationProvider(Object connectionId) throws CredentialFinderException {
        AuthenticationProvider authenticationProvider = null;
        Pair<Object,AuthenticationProvider> pair = ntlmAuthenticationProviderThreadLocal.get();
        //in the existing implementation connection ID is tied to a processing thread this means, the connection id remains the same for the duration of a
        //client connection. Once the connection ID changes the provider should be reset
        if(pair == null || !connectionId.equals(pair.left)) {
            authenticationProvider = createAuthenticationProvider();
            Pair<Object,AuthenticationProvider> newPair = new Pair<Object, AuthenticationProvider>(connectionId, authenticationProvider);
            ntlmAuthenticationProviderThreadLocal.set(newPair);
        }
        else {
            authenticationProvider = pair.right;
        }

        return authenticationProvider;
    }

    protected AuthenticationProvider createAuthenticationProvider() throws CredentialFinderException {
        AuthenticationProvider authenticationProvider;
        try {
            LdapIdentityProvider ldapProvider = (LdapIdentityProvider)identityProviderFactory.getProvider(assertion.getLdapProviderOid());

            LdapIdentityProviderConfig providerConfig = ldapProvider.getConfig();

            Map<String, String> props = new HashMap(providerConfig.getNtlmAuthenticationProviderProperties());
            if(props.size() == 0 || !Boolean.TRUE.toString().equals(props.get("enabled"))) {
                throw new FindException("NTLM Configuration is disabled");
            }
            
            //find password property and replace it if password is stored as a secure password
            if(props.containsKey("service.secure.password")) {
                String pass = ServerVariables.expandPasswordOnlyVariable(new LoggingAudit(logger), props.get("service.secure.password"));
                props.put("service.password", pass);
            }

            authenticationProvider = new NtlmAuthenticationServer(props, new NetLogon(props));
        } catch (FindException e) {
            final String errorMsg = "Unable to create NtlmAuthenticationServer instance";
            logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_FAILED, new String[]{errorMsg} , e);
            throw new CredentialFinderException(errorMsg, e, AssertionStatus.FAILED);
        }
        return authenticationProvider;
    }


    private byte[] getAuthorizationData(Message request) throws IOException {
        String authorization = request.getHttpRequestKnob().getHeaderSingleValue(HttpConstants.HEADER_AUTHORIZATION);

        if (authorization == null || authorization.length() == 0) {
            logAndAudit(AssertionMessages.HTTPCREDS_NO_AUTHN_HEADER);
            return null;
        }
        if (authorization.startsWith(SCHEME)) {
            return HexUtils.decodeBase64(authorization.substring(5));
        }
        return null;
    }


    private NtlmToken authenticateClient(byte[] encryptedNtlmData, Map<String, String> authParams, AuthenticationProvider authenticationProvider) throws CredentialFinderException {
        NtlmToken securityToken = null;
        //decode NTLM protocol data and check user credentials against AD
        try {
            byte[] ntlmToken = authenticationProvider.processAuthentication(encryptedNtlmData);
            securityToken = checkProviderStatus(authenticationProvider);
            if(securityToken == null) {
                authParams.put(NtlmAuthenticationAssertion.NTLM, HexUtils.encodeBase64(ntlmToken, true));
            }
        } catch (AuthenticationManagerException e) {
            final String errMessage = "NTLM Authentication failed";
            log.log(Level.WARNING, errMessage + ": "  + e.getMessage());
            logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_FAILED, new String[]{errMessage}, e);
            throw new CredentialFinderException(errMessage, e, AssertionStatus.AUTH_FAILED);
        }
        finally {
            if(authenticationProvider.getNtlmAuthenticationState().getState() == NtlmAuthenticationProvider.State.FAILED) {
                authenticationProvider.resetAuthentication();
            }
        }

        return securityToken;
    }

    private NtlmToken checkProviderStatus(AuthenticationProvider authenticationProvider) throws CredentialFinderException {
        String ntlmData = "";
        NtlmToken securityToken = null;
        Map<String, Object> params = new HashMap<String, Object>();
        if(authenticationProvider.getNtlmAuthenticationState().isComplete()){
            Map<String, Object> accountInfo = authenticationProvider.getNtlmAuthenticationState().getAccountInfo();

            if(accountInfo == null) {
                throw new CredentialFinderException("NTLM account info cannot be null!", AssertionStatus.FAILED);
            }
            ntlmData = HexUtils.encodeBase64(authenticationProvider.getNtlmAuthenticationState().getSessionKey(), true);
            //params.put(NtlmAuthenticationAssertion.USER_LOGIN_NAME, accountInfo.get("sAMAccountName"));

            long currentMills = System.currentTimeMillis();
            if(!accountInfo.containsKey("session.authenticate.time")) {
                accountInfo.put("session.authenticate.time", currentMills);
            }
            accountInfo.put("request.idle.time", currentMills);
            params.put("account.info", accountInfo);
            securityToken = new NtlmToken(ntlmData, null, params);
        }

        return securityToken;
    }

    protected boolean checkConnectionTimeouts(NtlmToken securityToken, Object connectionId) {
        long maxConnectionDuration = assertion.getMaxConnectionDuration();
        long maxIdleTimeout =  assertion.getMaxConnectionIdleTime();
        if(securityToken != null) {
            Map<String, Object> accountInfo = (Map)securityToken.getParams().get("account.info");
            if(accountInfo ==  null) {
                return false;
            }
            long currentMills = System.currentTimeMillis();
            Long sessionTime = (Long)accountInfo.get("session.authenticate.time");
            log.log(Level.FINE, "Checking session expiry...");
            if(maxConnectionDuration == NtlmAuthenticationAssertion.DEFAULT_MAX_CONNECTION_DURATION || sessionTime != null && currentMills - sessionTime < maxConnectionDuration * 1000) {
                Long requestIdleTime = (Long)accountInfo.get("request.idle.time");
                if(maxIdleTimeout == NtlmAuthenticationAssertion.DEFAULT_MAX_IDLE_TIMEOUT || requestIdleTime != null && currentMills - requestIdleTime < maxIdleTimeout * 1000) {
                    return true;
                }
                else{
                    logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_IDLE_TIMEOUT_EXPIRED, (String)connectionId);
                    log.log(Level.FINE, "Connection idle timeout expired for connection " + connectionId);
                }
            }
            else {
                logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_CONNECTION_EXPIRED, (String)connectionId);
                log.log(Level.FINE, "Connection " + connectionId + " has expired!");
            }
        }

        return false;
    }
    

}
