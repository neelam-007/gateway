package com.ca.siteminder;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ca.siteminder.util.SiteMinderUtil;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.util.Pair;
import netegrity.siteminder.javaagent.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import static com.ca.siteminder.SiteMinderAgentContextCacheManager.*;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/26/13
 */
public class SiteMinderHighLevelAgent {
    private static final Logger logger = Logger.getLogger(SiteMinderHighLevelAgent.class.getName());
    //TODO: token reuse value needs to be moved into the configuration gui.
    private int tokenReuseFactor = 25;

    /**
     * @return true if the resource is protected
     * @throws SiteMinderApiClassException
     */
    public boolean checkProtected(final String userIp,
                                  String smAgentName, final String resource,
                                  final String action,
                                  SiteMinderContext context) throws SiteMinderApiClassException {
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");//should never happen

        SiteMinderLowLevelAgent agent = context.getAgent();
        if(agent == null) throw new SiteMinderApiClassException("Unable to find CA Single Sign-On Agent");

        final SiteMinderContextCache cache = getCache(context.getConfig(), smAgentName).getResourceCache();
        final String cacheKey = smAgentName + resource + action;
        SiteMinderContext cachedContext = null;

        //Check the cache or call isProtected to initialize the Resource and Realm Definition in the context (SMContext) in the event a cache miss occurs
        SiteMinderContextCache.Entry entry;
        if ( ( entry = cache.lookup( cacheKey ) ) != null ) {

            final String transactionId = UUID.randomUUID().toString();//generate SiteMinder transaction id.
            cachedContext = entry.getSmContext();
            context.setAgent(cachedContext.getAgent());
            //generate a new SiteMinder TransactionID as this is a per-request value
            context.setTransactionId( transactionId );
            //TODO: Is the AuthScheme required?
            context.setAuthSchemes(cachedContext.getAuthSchemes());
            context.setRealmDef(cachedContext.getRealmDef());
            context.setResContextDef(cachedContext.getResContextDef());

        } else {
            // The realmDef object will contain the realm handle for the resource if the resource is protected.
            ResourceContextDef resCtxDef = new ResourceContextDef(smAgentName, "", resource, action);
            RealmDef realmDef = new RealmDef();

            // check the requested resource/action is actually protected by SiteMinder
            boolean isProtected = agent.isProtected(userIp, resCtxDef, realmDef, context);

            //now set the context
            context.setResContextDef(new SiteMinderContext.ResourceContextDef(resCtxDef.agent, resCtxDef.server, resCtxDef.resource, resCtxDef.action));
            context.setRealmDef(new SiteMinderContext.RealmDef(realmDef.name, realmDef.oid, realmDef.domOid, realmDef.credentials, realmDef.formLocation));
            //determine which authentication scheme to use

            buildAuthenticationSchemes(context, realmDef.credentials);

            if (!isProtected) {
                logger.log(Level.INFO,"The resource/action '" + resource + "/" + action + "' is not protected by CA Single Sign-On.  Access cannot be authorized.");
                return false;
            }

            //Populate the isProtected Cache as the resource is protected
            cachedContext = new SiteMinderContext();
            cachedContext.setAgent( context.getAgent() );
            cachedContext.setTransactionId( context.getTransactionId() );
            //TODO: Is the AuthScheme required?
            cachedContext.setAuthSchemes(context.getAuthSchemes());
            cachedContext.setRealmDef(context.getRealmDef());
            cachedContext.setResContextDef(context.getResContextDef());
            cachedContext.setConfig(context.getConfig());

            //final String secondaryCacheKey = context.getRealmDef().getOid();

            //cache.store( cacheKey, secondaryCacheKey, cachedContext );
            cache.store( cacheKey, cachedContext );
        }
        return true;
    }

    private void buildAuthenticationSchemes(SiteMinderContext context, int credentials) {
        final Set<SiteMinderContext.AuthenticationScheme> authSchemes = new HashSet<>();
        if(credentials != AgentAPI.CRED_NONE) {
            if((credentials & AgentAPI.CRED_BASIC) == AgentAPI.CRED_BASIC ){
               authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
            }
            if((credentials & AgentAPI.CRED_X509CERT_ISSUERDN) == AgentAPI.CRED_X509CERT_ISSUERDN) {
               authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTISSUEDN);
            }
            if((credentials & AgentAPI.CRED_X509CERT_USERDN) == AgentAPI.CRED_X509CERT_USERDN) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTUSERDN);
            }
            if((credentials & AgentAPI.CRED_X509CERT) == AgentAPI.CRED_X509CERT) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERT);
            }
            if((credentials & AgentAPI.CRED_CERT_OR_BASIC) == AgentAPI.CRED_CERT_OR_BASIC) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERT);
                authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
            }
            if((credentials & AgentAPI.CRED_DIGEST) == AgentAPI.CRED_DIGEST) {
               authSchemes.add(SiteMinderContext.AuthenticationScheme.DIGEST);
            }
            if((credentials & AgentAPI.CRED_FORMREQUIRED) == AgentAPI.CRED_FORMREQUIRED) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.FORM);
            }
            if((credentials & AgentAPI.CRED_CERT_OR_FORM) == AgentAPI.CRED_CERT_OR_FORM) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERT);
                authSchemes.add(SiteMinderContext.AuthenticationScheme.FORM);
            }
            if((credentials & AgentAPI.CRED_METADATA_REQUIRED) == AgentAPI.CRED_METADATA_REQUIRED) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);//custom authentication scheme?
            }
            if((credentials & AgentAPI.CRED_NT_CHAL_RESP) == AgentAPI.CRED_NT_CHAL_RESP) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.NTCHALLENGE);//NTLM challenge supported?
            }
            if((credentials & AgentAPI.CRED_SAML) == AgentAPI.CRED_SAML) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.SAML);
            }
            if((credentials & AgentAPI.CRED_SSLREQUIRED) == AgentAPI.CRED_SSLREQUIRED) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.SSL);
            }
            if((credentials & AgentAPI.CRED_XML_DOCUMENT_MAPPED) == AgentAPI.CRED_XML_DOCUMENT_MAPPED) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.XMLDOC);
            }
            if((credentials & AgentAPI.CRED_XML_DSIG) == AgentAPI.CRED_XML_DSIG) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.XMLDSIG);
            }
            if((credentials & AgentAPI.CRED_XML_DSIG_XKMS) == AgentAPI.CRED_XML_DSIG_XKMS) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.XKMS);
            }
            if((credentials & AgentAPI.CRED_XML_WSSEC) == AgentAPI.CRED_XML_WSSEC) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.XMLWSSEC);
            }
            if((credentials & AgentAPI.CRED_ALLOWSAVECREDS) == AgentAPI.CRED_ALLOWSAVECREDS) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.ALLOWSAVE);//not sure what it does
            }
        }
        else {
            authSchemes.add(SiteMinderContext.AuthenticationScheme.NONE); // anonymous auth scheme
        }
        context.setAuthSchemes(new ArrayList<>(authSchemes));
    }

    public int processAuthorizationRequest(final String userIp, final String ssoCookie,final SiteMinderContext context) throws SiteMinderApiClassException {
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");//should never happen

        SiteMinderLowLevelAgent agent = context.getAgent();
        if(agent == null) throw new SiteMinderApiClassException("Unable to find CA Single Sign-On Agent");

        final String smAgentName = context.getResContextDef().getAgent();
        final String reqResource = context.getResContextDef().getResource();
        final String action = context.getResContextDef().getAction();
        final int currentAgentTimeSeconds = safeLongToInt(System.currentTimeMillis() / 1000);
        final SiteMinderContextCache cache = getCache(context.getConfig(), smAgentName).getAuthorizationCache();
        SiteMinderContext cachedContext;
        SiteMinderContext.SessionDef sessionDef = null != context.getSessionDef() ? context.getSessionDef() : null;
        String sessionId;
        String cacheKey;

        boolean updateSsoToken;
        int result;

        if (null != ssoCookie && ssoCookie.trim().length() > 0) {
            if ( null == sessionDef ){
                //decode the ssoToken
                result = agent.decodeSessionToken( context, ssoCookie );

                if (result != AgentAPI.SUCCESS) {
                    logger.log(Level.FINE, "CA Single Sign-On  Authorization attempt - CA Single Sign-On  is unable to decode the token '" + SiteMinderUtil.safeNull(ssoCookie) + "'");
                    return result;
                }
                sessionDef = context.getSessionDef();
            }
        } else if ( null == context.getSessionDef() ){ // ensure that a valid SessionDef exits
            throw new SiteMinderApiClassException("SiteMinder Session Definition object is null");
        }

        sessionId = sessionDef.getId();
        cacheKey = smAgentName + reqResource + action + sessionId;

        //Perform Session Validation
        if ( ! validateDecodedSession( context.getSessionDef(), currentAgentTimeSeconds ) ){
            //cache.remove( cacheKey, "" );
            cache.remove( cacheKey );
            logger.log(Level.WARNING, "Session validation failed for the following SsoToken: " + ssoCookie);
            return AgentAPI.CHALLENGE;

        }

        // TODO: Implement a tokenReuse threshold check, as a full SiteMinder Agent is expected to perform this operation
        // updateSsoToken = tokenReuseThresholdExceeded( context.getSessionDef().getSessionLastTime(),
        //        context.getSessionDef().getIdleTimeout(),
        //        currentAgentTimeSeconds );
        updateSsoToken = true;

        //lookup in cache
        if ( ( cache.lookup( cacheKey ) ) == null ){
            result = agent.authorize(ssoCookie, userIp, context.getTransactionId(), context);

            if ( result != AgentAPI.YES ) {
                logger.log(Level.FINE, "SiteMinder authorization attempt - SiteMinder is unable to decode the token '" + SiteMinderUtil.safeNull(ssoCookie) + "'");
                return AgentAPI.FAILURE;
            }

            cachedContext = context;
            sessionId = cachedContext.getSessionDef().getId();
            cacheKey = smAgentName + reqResource + action + sessionId;
            //final String secondaryCacheKey = (String) SiteMinderUtil.getAttribute( context.getAttrList(), ATTR_USERDN );
            //cache.store( cacheKey, secondaryCacheKey, cachedContext );
            cache.store( cacheKey, cachedContext );
        }

        if ( ( agent.getUpdateCookieStatus() && updateSsoToken ) && null == context.getSsoToken() ){ // create a new ssoToken

            byte[] currentAgentTimeByte = String.valueOf( currentAgentTimeSeconds ).getBytes();

            AttributeList updatedAttrList = agent.getSMAttributeListFromAttributeList( context.getAttrList() );

            Attribute lastSessionTime = getAttrForId( updatedAttrList, AgentAPI.ATTR_LASTSESSIONTIME );
            lastSessionTime = new Attribute( lastSessionTime.id , lastSessionTime.ttl,
                    lastSessionTime.flags, lastSessionTime.oid, currentAgentTimeByte );

            updatedAttrList.removeAttribute( getAttrForId( updatedAttrList, AgentAPI.ATTR_LASTSESSIONTIME ));
            updatedAttrList.addAttribute( lastSessionTime );
            //ATTR_CURRENTSERVERTIME = int 153
            Attribute currentServerTime = new Attribute( SiteMinderAgentConstants.ATTR_CURRENTAGENTTIME , 0, 0, "", currentAgentTimeByte );
            updatedAttrList.addAttribute( currentServerTime );

            //populate sessionDef from attributes
            SessionDef smSessionDef = getSiteMinderSessionDefFromAttrList( updatedAttrList );

            //TODO: Change the decode method to always pass the updateCookie boolean as false;
            result = agent.getSsoToken( smSessionDef, updatedAttrList, context);
            if ( result != AgentAPI.YES ){
                logger.log(Level.FINE, "Unable to create a new SsoToken for the following SessionId: " + smSessionDef.id );
                return AgentAPI.FAILURE;
            }
            //Update the currentServerTime within the context, allowing for session validation within policy
            context.getSessionDef().setCurrentServerTime( smSessionDef.currentServerTime );

            //ensure the sessionDef values are used to update the Attribute list contained within the context
            addSessionAttributes( context, context.getSessionDef() );

        } else {
            if (! "".equals( ssoCookie ) && ssoCookie != null ){
                context.setSsoToken( ssoCookie );
            }
            //ensure the sessionDef values are used to update the Attribute list contained within the context
            //set currentServerTime
            context.getSessionDef().setCurrentServerTime( currentAgentTimeSeconds );
            addSessionAttributes( context, context.getSessionDef() );

            logger.log(Level.FINE, "Authorized user using cookie:" + ssoCookie);
            result = AgentAPI.YES;
        }

        return result;
    }

    /**
     * Perform authentication if required, and authorize the session against the specified resource.
     *
     *
     * @param credentials the user credentials
     * @param userIp    the client IP address
     * @param ssoCookie the SiteMinder SSO Token cookie
     * @param context the SiteMinder context
     * @return the value of new (or updated) SiteMinder SSO Token cookie
     * @throws SiteMinderApiClassException
     */
    public int processAuthenticationRequest(SiteMinderCredentials credentials,
                                            final String userIp,
                                            @Nullable final String ssoCookie,
                                            final SiteMinderContext context)
        throws SiteMinderApiClassException {
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");//should never happen

        SiteMinderLowLevelAgent agent = context.getAgent();
        if(agent == null) throw new SiteMinderApiClassException("Unable to find CA Single Sign-On Agent");

        final SiteMinderContextCache cache = getCache(context.getConfig(), context.getResContextDef().getAgent()).getAuthenticationCache();

        // check for some kind of credential
        UserCredentials userCreds;

        if(credentials == null) {
            userCreds = new UserCredentials();
        } else {
            userCreds = credentials.getUserCredentials();
        }

        if( (ssoCookie == null || ssoCookie.trim().length() == 0)
                && ((userCreds.name == null || userCreds.name.length() < 1) && (userCreds.password == null || userCreds.password.length() < 1)
                    && (userCreds.certBinary == null || userCreds.certBinary.length == 0))) {
            logger.log(Level.WARNING, "Credentials missing in service request.");
            return AgentAPI.CHALLENGE;
        }

        boolean updateSsoToken;
        int result;
        final int currentAgentTimeSeconds = safeLongToInt(System.currentTimeMillis() / 1000);

        if ( null != ssoCookie && ssoCookie.trim().length() > 0 ) {
            // Attempt to authenticate with an existing cookie/SsoToken
            // Decode the SSO token and ensure the session has not expired by
            // checking for idle timeout and max timeout based on the last session/access time.

            result = agent.decodeSessionToken( context, ssoCookie );

            if ( result != AgentAPI.SUCCESS ) {
                    logger.log(Level.FINE, "CA Single Sign-On  Authentication attempt - CA Single Sign-On  is unable to decode the token '" + SiteMinderUtil.safeNull(ssoCookie) + "'");
                    return result;
            }

            String sessionId = context.getSessionDef().getId();
            String realmOid = context.getRealmDef().getOid();
            //AuthDirOid is not available in the decoded SsoToken Attributes
            //String authDirOid = (String) SiteMinderUtil.getAttribute( context.getAttrList(), ATTR_AUTH_DIR_OID );
            //String cacheKey = sessionId + realmOid + authDirOid;
            String cacheKey = sessionId + realmOid;

            if ( ! validateDecodedSession( context.getSessionDef(), currentAgentTimeSeconds ) ){
                //cache.remove( cacheKey, "" );
                cache.remove( cacheKey );
                logger.log(Level.WARNING, "Session validation failed for the following SsoToken: " + ssoCookie);
                return AgentAPI.CHALLENGE;

            }

            // TODO: Implement a tokenReuse threshold check, as a full SiteMinder Agent is expected to perform this operation
            // updateSsoToken = tokenReuseThresholdExceeded( context.getSessionDef().getSessionLastTime(),
            //        context.getSessionDef().getIdleTimeout(),
            //        currentAgentTimeSeconds );
            updateSsoToken = true;


            byte[] currentAgentTimeByte = String.valueOf( currentAgentTimeSeconds ).getBytes();
            SessionDef sessionDef;
            Attribute lastSessionTime;
            AttributeList updatedAttrList;

            if ( ( cache.lookup( cacheKey ) ) == null ) {
                logger.log(Level.FINE, "SiteMinder Authentication - cache missed");

                // Cache miss occurred we need to Validate the decoded ssoToken/session
                //ValidateSession calls AgentApi.login, thus we must create a new ssoToken?
                result = agent.validateSession( userCreds, userIp, ssoCookie,  context.getTransactionId(), context );
                if( result != AgentAPI.YES ) {
                    //TODO: should we logout the session?
                    logger.log(Level.FINE, "Unable to validate user session!");
                    return AgentAPI.FAILURE;
                }

                updateSsoToken = true; //We do not have any insight into who created the token.
            }

            logger.log(Level.FINE, "SiteMinder Authentication - Agent update cookie status: " + agent.getUpdateCookieStatus());
            logger.log(Level.FINE, "SiteMinder Authentication - Context SSO Token: " + context.getSsoToken());

            //create a new SsoToken, with the lastSessionTime, and CurrentServerTime Attributes
            // updated to the current server/agent Time.
            if ( ( agent.getUpdateCookieStatus() && updateSsoToken ) && null == context.getSsoToken() ){
                //Validation of the session passed or cacheHit, we need to create a new token and cache the result
                updatedAttrList = agent.getSMAttributeListFromAttributeList( context.getAttrList() );

                lastSessionTime = getAttrForId( updatedAttrList, AgentAPI.ATTR_LASTSESSIONTIME );
                lastSessionTime = new Attribute( lastSessionTime.id , lastSessionTime.ttl,
                        lastSessionTime.flags, lastSessionTime.oid, currentAgentTimeByte );

                updatedAttrList.removeAttribute( getAttrForId( updatedAttrList, AgentAPI.ATTR_LASTSESSIONTIME ));
                updatedAttrList.addAttribute( lastSessionTime );
                //ATTR_CURRENTSERVERTIME = int 153
                Attribute currentServerTime = new Attribute( SiteMinderAgentConstants.ATTR_CURRENTAGENTTIME , 0, 0, "", currentAgentTimeByte );
                updatedAttrList.addAttribute( currentServerTime );

                //populate sessionDef from attributes
                sessionDef = getSiteMinderSessionDefFromAttrList( updatedAttrList );

                //Assuming that if a cacheHit occurs that this is equal to a login call, and we simply create a new ssoToken
                //In the case of a cacheMiss we call AgentApi.login
                logger.log(Level.FINE, "SiteMinder Authentication - Attempt to obtain a new SSO token.");

                result = agent.getSsoToken( sessionDef, updatedAttrList, context );
                if ( result != AgentAPI.YES ){
                    logger.log(Level.FINE, "Unable to create a new SsoToken for the following SessionId: " + sessionDef.id );
                    return AgentAPI.FAILURE;
                }

                context.getSessionDef().setCurrentServerTime( sessionDef.currentServerTime );

                context.setSessionDef(new SiteMinderContext.SessionDef( context.getSessionDef().getReason(),
                        context.getSessionDef().getIdleTimeout(),
                        context.getSessionDef().getMaxTimeout(),
                        context.getSessionDef().getCurrentServerTime(),
                        context.getSessionDef().getSessionStartTime(),
                        context.getSessionDef().getSessionLastTime(),
                        context.getSessionDef().getId(),
                        context.getSessionDef().getSpec() ));

                //ensure the sessionDef is updated in the context
                addSessionAttributes( context, context.getSessionDef() );

                sessionId = context.getSessionDef().getId();
                realmOid = context.getRealmDef().getOid();
                //AuthDirOid is available as we called AgentApi.login
                //authDirOid = (String) SiteMinderUtil.getAttribute( context.getAttrList(), ATTR_AUTH_DIR_OID );
                //cacheKey = sessionId + realmOid + authDirOid;
                cacheKey = sessionId + realmOid;
                //User DN Value, extracted from the users authenticated response AttributeList
                //final String secondaryCacheKey = (String) SiteMinderUtil.getAttribute( context.getAttrList(), ATTR_USERDN );

                //store the smContext to the isAuth Cache
                //cache.store( cacheKey, secondaryCacheKey, context );
                cache.store( cacheKey, context );

            } else {
                if (! "".equals( ssoCookie ) && ssoCookie != null ){
                    context.setSsoToken( ssoCookie );
                }
                //ensure the sessionDef is updated in the context
                //set currentServerTime
                context.getSessionDef().setCurrentServerTime( currentAgentTimeSeconds );
                addSessionAttributes( context, context.getSessionDef() );

            }

            //Currently the sessionDef and sessionAttributes associated with the token obtained from the request message
            // will be available in policy, allowing for the customer to perform session validation.
            // Be aware that the new ssoToken has been placed into the context.
            logger.log(Level.FINE, "Authenticated user using cookie:" + ssoCookie);
            result = AgentAPI.YES;

        } else {
            // authenticate user using credentials
            result = agent.authenticate( userCreds, userIp, context.getTransactionId(), context );
            if(result != AgentAPI.YES) {
               logger.log(Level.FINE, "Unable to authenticate user: " + SiteMinderUtil.getCredentialsAsString(userCreds));
            }
            else {
                logger.log(Level.FINE, "Authenticated user via user credentials" + SiteMinderUtil.getCredentialsAsString(userCreds));

                final String sessionId = context.getSessionDef().getId();
                final String realmOid = context.getRealmDef().getOid();
                //final String authDirOid = (String) SiteMinderUtil.getAttribute( context.getAttrList(), ATTR_AUTH_DIR_OID );
                //final String cacheKey = sessionId + realmOid + authDirOid;
                final String cacheKey = sessionId + realmOid;
                //User DN Value, extracted from the users authenticated response AttributeList
                //final String secondaryCacheKey = (String) SiteMinderUtil.getAttribute( context.getAttrList(), ATTR_USERDN );

                //store the smContext to the isAuth Cache
                //cache.store( cacheKey, secondaryCacheKey, context );
                cache.store( cacheKey, context );
                context.getAttrList().add( new Pair<String, Object>(SiteMinderAgentConstants.ATTR_CURRENTSERVERTIME, String.valueOf( currentAgentTimeSeconds ) ));
                context.getSessionDef().setCurrentServerTime(currentAgentTimeSeconds);
            }
        }

        return result;
    }

    /**
     * Perform session validation against a decoded SessionDef
     *
     *
     * @param sessionDef the SiteMinderContext SessionDef
     * @param serverTimeSeconds is the current server time in seconds
     * @return true if both idleTimeOut and MaxTimeOut conditions have not been met
     */
    private boolean validateDecodedSession( SiteMinderContext.SessionDef sessionDef, final int serverTimeSeconds ) {

        if ( sessionDef == null ) { return false;}

        boolean validSession = true;
        final int idleTimeOut = sessionDef.getIdleTimeout();
        final int maxTimeOut = sessionDef.getMaxTimeout();
        final int sessionStartTime = sessionDef.getSessionStartTime();
        final int lastSessionTime = sessionDef.getSessionLastTime();

        if ( ( serverTimeSeconds - lastSessionTime ) >= idleTimeOut ){ //IdleTimeOut Check
            validSession = false;
        } else if ( ( serverTimeSeconds - sessionStartTime ) >= maxTimeOut ){//MaxTimeOut Check
            validSession = false;
        }

        return validSession;
    }

    private boolean tokenReuseThresholdExceeded ( final int lastSessionTime, final int idleTimeOut, final int serverTimeSeconds ) {
        boolean usageExceeded = false;

        if ( serverTimeSeconds - lastSessionTime >= ( tokenReuseFactor * idleTimeOut )/100 ) {
            usageExceeded = true;
        }
        return usageExceeded;
    }

    private List<Pair<String, Object>> addSessionAttributes(SiteMinderContext context, SiteMinderContext.SessionDef sd) {

        //remove any duplicate session Attributes that may exist.
        List<Pair<String, Object>> attributes = context.getAttrList();
        for (Iterator<Pair<String, Object>> iter = attributes.iterator(); iter.hasNext(); ) {
            String attr = iter.next().left;
            switch( attr ) {
                case SiteMinderAgentConstants.ATTR_SESSIONID:
                    iter.remove();
                    break;
                case SiteMinderAgentConstants.ATTR_SESSIONSPEC:
                    iter.remove();
                    break;
                case SiteMinderAgentConstants.ATTR_IDLESESSIONTIMEOUT:
                    iter.remove();
                    break;
                case SiteMinderAgentConstants.ATTR_MAXSESSIONTIMEOUT:
                    iter.remove();
                    break;
                case SiteMinderAgentConstants.ATTR_STARTSESSIONTIME:
                    iter.remove();
                    break;
                case SiteMinderAgentConstants.ATTR_LASTSESSIONTIME:
                    iter.remove();
                    break;
            }
        }

        attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_SESSIONID, sd.getId()));
        attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_SESSIONSPEC, sd.getSpec()));
        attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_STARTSESSIONTIME, sd.getSessionStartTime()));
        attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_LASTSESSIONTIME, sd.getSessionLastTime()));
        attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_CURRENTSERVERTIME, sd.getCurrentServerTime())); //added
        attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_IDLESESSIONTIMEOUT, sd.getIdleTimeout()));
        attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_MAXSESSIONTIMEOUT, sd.getMaxTimeout()));

        return attributes;
    }

    private Attribute getAttrForId( AttributeList attrList, int id ) {
        for (int i = 0; i < attrList.getAttributeCount(); i++) {
            Attribute attr = attrList.getAttributeAt(i);
            if ( attr.id == id ){
                return attr;
            }
        }
        return null;
    }

    private SessionDef getSiteMinderSessionDefFromAttrList( AttributeList attrList ) {

        SessionDef sessionDef = new SessionDef();
        int attrCount = attrList.getAttributeCount();

        for (int i = 0; i < attrCount; i++) {
            Attribute attr = attrList.getAttributeAt(i);
            String value = SiteMinderUtil.chopNull( new String( attr.value ) );
            switch(attr.id) {
                case AgentAPI.ATTR_SESSIONID:
                    sessionDef.id = value;
                    break;
                case AgentAPI.ATTR_SESSIONSPEC:
                    sessionDef.spec = value;
                    break;
                case AgentAPI.ATTR_IDLESESSIONTIMEOUT:
                    sessionDef.idleTimeout = Integer.parseInt(value);
                    break;
                case AgentAPI.ATTR_MAXSESSIONTIMEOUT:
                    sessionDef.maxTimeout = Integer.parseInt(value);
                    break;
                case AgentAPI.ATTR_STARTSESSIONTIME:
                    sessionDef.sessionStartTime = Integer.parseInt(value);
                    break;
                case AgentAPI.ATTR_LASTSESSIONTIME:
                    sessionDef.sessionLastTime = Integer.parseInt(value);
                    break;
                case SiteMinderAgentConstants.ATTR_CURRENTAGENTTIME:
                    sessionDef.currentServerTime = Integer.parseInt(value);
                    break;
            }

        }
        return sessionDef;
    }

    private int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    /**
     * Get the SiteMinderAgentContextCache
     *
     * @param smConfig the SiteMinderConfiguration
     * @param agentName the agent name
     * @return the cache
     */
    private SiteMinderAgentContextCache getCache(SiteMinderConfiguration smConfig, String agentName) {
        // format of properties agent cache settings:
        //   agent.<agent_name>.<cache_setting_name>
        String prefix = AGENT_CACHE_PREFIX + agentName;

        // Determine if the global cache should be used
        if (SiteMinderAgentContextCacheManager.INSTANCE.isUseGlobalCache()) {
            boolean useAgentCache = false;

            // Check whether the agent is configured to override the global cache
            String useAgentCacheStr = smConfig.getProperties().get(prefix + AGENT_USE_AGENT_CACHE_SUFFIX);
            if (StringUtils.isNotEmpty(useAgentCacheStr)) {
                useAgentCache = Boolean.parseBoolean(useAgentCacheStr);
            }

            // Return global cache if not overridden by the per agent configuration
            if (!useAgentCache) {
                return SiteMinderAgentContextCacheManager.INSTANCE.getGlobalCache();
            }
        }

        // Retrieve per agent cache
        SiteMinderAgentContextCache cache = SiteMinderAgentContextCacheManager.INSTANCE.getCache(smConfig.getGoid(), agentName);

        if (cache != null) {
            return cache;
        }

        Integer resourceSize = null, authnSize = null, authzSize = null;
        Long resourceAge = null, authnAge = null, authzAge = null;

        String resourceSizeStr = smConfig.getProperties().get(prefix + AGENT_RESOURCE_CACHE_SIZE_SUFFIX);
        if (StringUtils.isNotEmpty(resourceSizeStr)) {
            resourceSize = new Integer(resourceSizeStr);
        }

        String resourceAgeStr = smConfig.getProperties().get(prefix + AGENT_RESOURCE_CACHE_MAX_AGE_SUFFIX);
        if (StringUtils.isNotEmpty(resourceAgeStr)) {
            resourceAge = new Long(resourceAgeStr);
        }

        String authnSizeStr = smConfig.getProperties().get(prefix + AGENT_AUTHENTICATION_CACHE_SIZE_SUFFIX);
        if (StringUtils.isNotEmpty(authnSizeStr)) {
            authnSize = new Integer(authnSizeStr);
        }

        String authnAgeStr = smConfig.getProperties().get(prefix + AGENT_AUTHENTICATION_CACHE_MAX_AGE_SUFFIX);
        if (StringUtils.isNotEmpty(authnAgeStr)) {
            authnAge = new Long(authnAgeStr);
        }

        String authzSizeStr = smConfig.getProperties().get(prefix + AGENT_AUTHORIZATION_CACHE_SIZE_SUFFIX);
        if (StringUtils.isNotEmpty(authzSizeStr)) {
            authzSize = new Integer(authzSizeStr);
        }

        String authzAgeStr = smConfig.getProperties().get(prefix + AGENT_AUTHORIZATION_CACHE_MAX_AGE_SUFFIX);
        if (StringUtils.isNotEmpty(authzAgeStr)) {
            authzAge = new Long(authzAgeStr);
        }

        return SiteMinderAgentContextCacheManager.INSTANCE.getCache(
                smConfig.getGoid(), agentName, resourceSize, resourceAge, authnSize, authnAge, authzSize, authzAge);
    }
}
