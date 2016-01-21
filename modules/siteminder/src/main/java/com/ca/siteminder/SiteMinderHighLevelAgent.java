package com.ca.siteminder;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ca.siteminder.util.SiteMinderUtil;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.util.Pair;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import static com.ca.siteminder.SiteMinderAgentContextCacheManager.*;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/26/13
 */
public class SiteMinderHighLevelAgent {
    public static final int FAILURE = -1;
    public static final int SUCCESS = 0;
    public static final int YES = 1;
    public static final int NO = 2;
    public static final int CHALLENGE = 3;

    private static final Logger logger = Logger.getLogger(SiteMinderHighLevelAgent.class.getName());
    //TODO: token reuse value needs to be moved into the configuration gui.
    private int tokenReuseFactor = 25;

    private final SiteMinderAgentContextCacheManager cacheManager;

    public SiteMinderHighLevelAgent(final SiteMinderAgentContextCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * @return true if the resource is protected
     * @throws SiteMinderApiClassException
     */
    public boolean checkProtected(final String userIp,
                                  String smAgentName,
                                  final String serverName,
                                  final String resource,
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
            /*// In order to preserve previous behavior of setting an empty string when the serverName is undefined
            final String server = serverName != null ? serverName : "";
            // The resCtxDef holds the agent, server, resource and action
            ResourceContextDef resCtxDef = new ResourceContextDef(smAgentName, server, resource, action);
            // The realmDef object will contain the realm handle for the resource if the resource is protected.
            RealmDef realmDef = new RealmDef();

            // check the requested resource/action is actually protected by SiteMinder
            boolean isProtected = agent.isProtected(userIp, resCtxDef, realmDef, context);

            //now set the context
            context.setResContextDef(new SiteMinderContext.ResourceContextDef(resCtxDef.agent, resCtxDef.server, resCtxDef.resource, resCtxDef.action));
            context.setRealmDef(new SiteMinderContext.RealmDef(realmDef.name, realmDef.oid, realmDef.domOid, realmDef.credentials, realmDef.formLocation));
            //determine which authentication scheme to use

            buildAuthenticationSchemes(context, realmDef.credentials);
*/
            // check the requested resource/action is actually protected by SiteMinder
            boolean isProtected = agent.isProtected(userIp, smAgentName, serverName, resource, action, context);
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

            cache.store( cacheKey, cachedContext );
        }
        return true;
    }



    public int processAuthorizationRequest(final String userIp, final String ssoCookie,final SiteMinderContext context) throws SiteMinderApiClassException {
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");//should never happen

        SiteMinderLowLevelAgent agent = context.getAgent();
        if(agent == null) throw new SiteMinderApiClassException("Unable to find CA Single Sign-On Agent");

        final String smAgentName = context.getResContextDef().getAgent();
        final String reqResource = context.getResContextDef().getResource();
        final String action = context.getResContextDef().getAction();
        final int currentAgentTimeSeconds = SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000);
        final SiteMinderContextCache cache = getCache(context.getConfig(), smAgentName).getAuthorizationCache();
        SiteMinderContext cachedContext;
        //Obtain the AttributeList encase isAuthN was called before
        List<Pair<String, Object>> attrList = context.getAttrList();
        // Ensure that the Attribute List is empty, as the
        // context could contain cached attributes from an isAuthN call
        context.setAttrList(new ArrayList<Pair<String, Object>>());
        SiteMinderContext.SessionDef sessionDef = null != context.getSessionDef() ? context.getSessionDef() : null;
        String sessionId;
        String cacheKey;

        boolean updateSsoToken;
        int result;

        if (null != ssoCookie && ssoCookie.trim().length() > 0) {
            if ( null == sessionDef ){
                //decode the ssoToken
                result = agent.decodeSessionToken( context, ssoCookie, false );

                if (result != SUCCESS) {
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
            return CHALLENGE;

        }

        // TODO: Implement a tokenReuse threshold check, as a full SiteMinder Agent is expected to perform this operation
        // updateSsoToken = tokenReuseThresholdExceeded( context.getSessionDef().getSessionLastTime(),
        //        context.getSessionDef().getIdleTimeout(),
        //        currentAgentTimeSeconds );
        updateSsoToken = true;
        SiteMinderContextCache.Entry cachedSMEntry = cache.lookup( cacheKey );

        //lookup in cache
        if ( cachedSMEntry == null ){
            logger.log(Level.FINE, "SiteMinder Authorization - cache missed");
            // The context will contain attributes from the decode ( UserDn, UserName, ClientIP )
            // Note: Only the ClientIP (dotted-quad notation) is needed, by the authorize API call.
            result = agent.authorize(ssoCookie, userIp, context.getTransactionId(), context);

            if ( result != YES ) {
                logger.log(Level.FINE, "SiteMinder authorization attempt - SiteMinder is unable to decode the token '" + SiteMinderUtil.safeNull(ssoCookie) + "'");
                return FAILURE;
            }

            cachedContext = context;
            sessionId = cachedContext.getSessionDef().getId();
            cacheKey = smAgentName + reqResource + action + sessionId;
            SiteMinderContext smContext = new SiteMinderContext( context );
            cache.store( cacheKey, smContext );
        }

        if ( ( agent.getUpdateCookieStatus() && updateSsoToken ) && null == context.getSsoToken() ){ // create a new ssoToken

            logger.log(Level.FINEST, "SiteMinder Authorization - Setting Session and Attributes for new SsoToken.");
            //Update the SmContext.SessionDef CurrentServerTime, as this will become the lastSessionTime value
            context.getSessionDef().setCurrentServerTime( currentAgentTimeSeconds );

            logger.log(Level.FINE, "SiteMinder Authorization - Attempt to obtain a new SSO token.");
            result = agent.getSsoToken(context);
            if ( result != YES ){
                logger.log(Level.FINE, "Unable to create a new SsoToken for the following SessionId: " + sessionDef.getId() );
                return FAILURE;
            }
            //Update the currentServerTime within the context, allowing for session validation within policy
            //Note: SessionDef object is modified by the getSsoToken call and could update the current serverTime.
            context.getSessionDef().setCurrentServerTime( sessionDef.getCurrentServerTime() );

        } else {
            //Handle when the ssoToken is passed in, and not obtained from the context, but the context's
            // ssoToken was updated by isAuthN.
            if (! "".equals( ssoCookie ) && ssoCookie != null ){
                if (null == context.getSsoToken() ) {
                    context.setSsoToken( ssoCookie );
                }
            }
            //ensure the sessionDef values are used to update the Attribute list contained within the context
            //set currentServerTime
            context.getSessionDef().setCurrentServerTime( currentAgentTimeSeconds );

            logger.log(Level.FINE, "Authorized user using cookie: " + ssoCookie);
            result = YES;
        }

        //In both cases the attribute list should contain the UserDn, UserName, ClientIP
        if ( cachedSMEntry != null ) {
            // Ensure the cached SMContext Attributes are available, if a cache hit occurs
            // Don't clear the attributeList as it will contain the decoded
            // UserDn, UserName, ClientIP
            attrList.addAll( cachedSMEntry.getSmContext().getAttrList() );
            context.getAttrList().addAll( new ArrayList<>( attrList ));
        } else if ( context != null ) {
            context.getAttrList().addAll( attrList );
        }
        //Remove possible duplicate attributes
        context.setAttrList( SiteMinderUtil.removeDuplicateAttributes(context.getAttrList()));
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

       //Obtain the AttributeList encase isAuthN was called before
        List<Pair<String, Object>> attrList = context.getAttrList();
        // Ensure that the Attribute List is empty, as the
        // context could contain cached attributes from an isAuthN call
        context.setAttrList(new ArrayList<Pair<String, Object>>());

        boolean updateSsoToken;
        // check for some kind of credential
        int result = agent.checkUserCredentials(credentials, ssoCookie);
        if(result == CHALLENGE) {
            return result;
        }

        final int currentAgentTimeSeconds = SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000);

        if ( null != ssoCookie && ssoCookie.trim().length() > 0 ) {
            // Attempt to authenticate with an existing cookie/SsoToken
            // Decode the SSO token and ensure the session has not expired by
            // checking for idle timeout and max timeout based on the last session/access time.

            result = agent.decodeSessionToken(context, ssoCookie, false);

            if ( result != SUCCESS ) {
                    logger.log(Level.FINE, "CA Single Sign-On  Authentication attempt - CA Single Sign-On  is unable to decode the token '" + SiteMinderUtil.safeNull(ssoCookie) + "'");
                    return result;
            }

            String sessionId = context.getSessionDef().getId();
            String realmOid = context.getRealmDef().getOid();
            String cacheKey = sessionId + realmOid;

            if ( ! validateDecodedSession( context.getSessionDef(), currentAgentTimeSeconds ) ){
                cache.remove( cacheKey );
                logger.log(Level.WARNING, "Session validation failed for the following SsoToken: " + ssoCookie);
                return CHALLENGE;

            }

            // TODO: Implement a tokenReuse threshold check, as a full SiteMinder Agent is expected to perform this operation
            // updateSsoToken = tokenReuseThresholdExceeded( context.getSessionDef().getSessionLastTime(),
            //        context.getSessionDef().getIdleTimeout(),
            //        currentAgentTimeSeconds );
            updateSsoToken = true;

            SiteMinderContext.SessionDef sessionDef;
            SiteMinderContextCache.Entry cachedSMEntry = cache.lookup( cacheKey );

            if ( cachedSMEntry == null ) {
                logger.log(Level.FINE, "SiteMinder Authentication - cache missed");

                // Cache miss occurred we need to Validate the decoded ssoToken/session
                // ValidateSession calls AgentApi.login, thus we can cache this token.
                result = agent.validateSession( credentials, userIp, ssoCookie,  context.getTransactionId(), context );
                if( result != YES ) {
                    //TODO: should we logout the session?
                    logger.log(Level.FINE, "Unable to validate user session!");
                    return FAILURE;
                }

                sessionId = context.getSessionDef().getId();
                realmOid = context.getRealmDef().getOid();

                cacheKey = sessionId + realmOid;
                //Duplicate UserDN value will occur as the decode and login API's both return these values
                context.setAttrList( SiteMinderUtil.removeDuplicateAttributes( context.getAttrList() ));
                cache.store( cacheKey, new SiteMinderContext( context ) );

                updateSsoToken = true; //We do not have any insight into who created the token.
            }

            logger.log(Level.FINE, "SiteMinder Authentication - Agent update cookie status: " + agent.getUpdateCookieStatus());
            logger.log(Level.FINE, "SiteMinder Authentication - Context SSO Token: " + context.getSsoToken());

            //create a new SsoToken, with the lastSessionTime, and CurrentServerTime Attributes
            // updated to the current server/agent Time.
            if ( ( agent.getUpdateCookieStatus() && updateSsoToken ) && null == context.getSsoToken() ){
                //Validation of the session passed or cacheHit, we need to create a new token and cache the result
                //Assuming that if a cacheHit occurs that this is equal to a login call, and we simply create a new ssoToken
                //In the case of a cacheMiss we must call AgentApi.login

                //Update the context.SessionDef CurrentServerTime, as this will be come the lastSessionTime value
                context.getSessionDef().setCurrentServerTime( currentAgentTimeSeconds );
                //Init a sessionDef object from the current context which was populated by the Token Decode method
                sessionDef = context.getSessionDef();

                logger.log(Level.FINE, "SiteMinder Authentication - Attempt to obtain a new SSO token.");

                result = agent.getSsoToken(context);
                if ( result != YES ){
                    logger.log(Level.FINE, "Unable to create a new SsoToken for the following SessionId: " + sessionDef.getId() );
                    return FAILURE;
                }
                logger.log(Level.FINEST, "SiteMinder Authentication - Issued new ssoToken:" + context.getSsoToken() );

                context.getSessionDef().setCurrentServerTime( sessionDef.getCurrentServerTime() );

                context.setSessionDef(new SiteMinderContext.SessionDef( context.getSessionDef().getReason(),
                        context.getSessionDef().getIdleTimeout(),
                        context.getSessionDef().getMaxTimeout(),
                        context.getSessionDef().getCurrentServerTime(),
                        context.getSessionDef().getSessionStartTime(),
                        context.getSessionDef().getSessionLastTime(),
                        context.getSessionDef().getId(),
                        context.getSessionDef().getSpec() ));

                //ensure the cached AttributeList is available in policy
                if( cachedSMEntry != null ){
                    context.getAttrList().addAll( new ArrayList<>( cachedSMEntry.getSmContext().getAttrList() ));
                }
                //TODO: check if attributes from the previous authentication are indeed required.
                if( !attrList.isEmpty() ){
                    context.getAttrList().addAll( attrList );
                }
                logger.log(Level.FINE, "Authentication user using cookie:" + ssoCookie);
            } else {
                if (! "".equals( ssoCookie ) && ssoCookie != null ){
                    context.setSsoToken( ssoCookie );
                }
                //ensure the sessionDef is updated in the context
                //set currentServerTime
                context.getSessionDef().setCurrentServerTime( currentAgentTimeSeconds );

                if ( cachedSMEntry != null ) {
                    //ensure the cached SMContext Attributes are available, if a cache hit occurs
                    attrList.addAll( cachedSMEntry.getSmContext().getAttrList() );
                    context.setAttrList( attrList );
                } else if ( !attrList.isEmpty() ) {
                    //Ensure that if any Attributes from an isAuthZ call are available in policy
                    context.getAttrList().addAll( attrList );
                }
            }

            //Currently the sessionDef and sessionAttributes associated with the token obtained from the request message
            // will be available in policy, allowing for the customer to perform session validation.
            // Be aware that the new ssoToken has been placed into the context.
            logger.log(Level.FINE, "Authenticated user using cookie:" + ssoCookie);
            result = YES;

        } else {
            // authenticate user using credentials
            result = agent.authenticate( credentials, userIp, context.getTransactionId(), context );
            if(result != YES) {
               logger.log(Level.FINE, "Unable to authenticate user: " + SiteMinderUtil.getCredentialsAsString(credentials));
            } else {
                logger.log(Level.FINE, "Authenticated user via user credentials: " + SiteMinderUtil.getCredentialsAsString(credentials));

                final String sessionId = context.getSessionDef().getId();
                final String realmOid = context.getRealmDef().getOid();
                final String cacheKey = sessionId + realmOid;

                //store the smContext to the isAuth Cache
                context.getSessionDef().setCurrentServerTime( currentAgentTimeSeconds );
                cache.store( cacheKey, new SiteMinderContext( context ) );

            }
        }
        context.setAttrList( SiteMinderUtil.removeDuplicateAttributes( context.getAttrList() ));
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
            logger.log( Level.FINEST, "Session validation failed, Reason: IdleTimeOut reached" );
        } else if ( ( serverTimeSeconds - sessionStartTime ) >= maxTimeOut ){//MaxTimeOut Check
            validSession = false;
            logger.log( Level.FINEST, "Session validation failed, Reason: MaxTimeOut reached" );
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
        if (cacheManager.isUseGlobalCache()) {
            boolean useAgentCache = false;

            // Check whether the agent is configured to override the global cache
            String useAgentCacheStr = smConfig.getProperties().get(prefix + AGENT_USE_AGENT_CACHE_SUFFIX);
            if (StringUtils.isNotEmpty(useAgentCacheStr)) {
                useAgentCache = Boolean.parseBoolean(useAgentCacheStr);
            }

            // Return global cache if not overridden by the per agent configuration
            if (!useAgentCache) {
                return cacheManager.getGlobalCache();
            }
        }

        // Retrieve per agent cache
        SiteMinderAgentContextCache cache = cacheManager.getCache(smConfig.getGoid(), agentName);

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

        return cacheManager.getCache(smConfig.getGoid(), agentName, resourceSize, resourceAge, authnSize, authnAge, authzSize, authzAge);
    }
}
