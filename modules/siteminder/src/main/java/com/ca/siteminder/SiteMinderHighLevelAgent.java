package com.ca.siteminder;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ca.siteminder.util.SiteMinderUtil;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;

import com.l7tech.util.Functions;
import com.whirlycott.cache.Cache;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import static com.ca.siteminder.SiteMinderConfig.*;
import static com.ca.siteminder.SiteMinderAgentContextCache.*;

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
    private final Config config;

    public SiteMinderHighLevelAgent(final Config config, final SiteMinderAgentContextCacheManager cacheManager) {
        this.config = config;
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

        boolean isProtected = false;

        SiteMinderResourceDetails resourceDetails;
        final SiteMinderAgentContextCache agentCache = getCache(context.getConfig(), smAgentName);
        if(agentCache.getResourceCacheSize() > 0) {
            final Cache cache = agentCache.getResourceCache();
            final ResourceCacheKey resourceCacheKey = new ResourceCacheKey(resource, action, serverName != null ? serverName : "");
            //Check the cache or call isProtected to initialize the Resource and Realm Definition in the context (SMContext) in the event a cache miss occurs

            if ((resourceDetails = (SiteMinderResourceDetails) cache.retrieve(resourceCacheKey)) != null) {
                //now check if the cached entry exceeded the max cached time then remove from cache
                logger.log(Level.FINE, "Found resource cache entry: " + resourceCacheKey);
                if (System.currentTimeMillis() - resourceDetails.getTimeStamp() <= agentCache.getResourceCacheMaxAge()) {
                    final String transactionId = UUID.randomUUID().toString();//generate SiteMinder transaction id.
                    //generate a new SiteMinder TransactionID as this is a per-request value
                    context.setTransactionId(transactionId);
                    context.setAuthSchemes(resourceDetails.getAuthSchemes());
                    context.setRealmDef(resourceDetails.getRealmDef());
                    context.setResContextDef(resourceDetails.getResContextDef());
                    isProtected = resourceDetails.isResourceProtected();
                    context.setResourceProtected(isProtected);
                } else {
                    //remove from cache if the entry exceeded the cache maxAge
                    cache.remove(resourceCacheKey);
                    resourceDetails = null;//remove reference
                    logger.log(Level.FINE, "Maximum resource cache age exceeded. Removed resource cache entry: " + resourceCacheKey + " from resource cache");
                }
            } else {
                logger.log(Level.FINE, "SiteMinder Resource - cache missed");
            }

            if (resourceDetails == null) {
                // check the requested resource/action is actually protected by SiteMinder
                isProtected = agent.isProtected(userIp, smAgentName, serverName, resource, action, context);
                //Populate the isProtected Cache
                resourceDetails = new SiteMinderResourceDetails(isProtected, context.getResContextDef(),
                        context.getRealmDef(), context.getAuthSchemes());

                cache.store(resourceCacheKey, resourceDetails);
            }
        }
        //we are not caching anything
        else {
            // check the requested resource/action is actually protected by SiteMinder
            isProtected = agent.isProtected(userIp, smAgentName, serverName, resource, action, context);
        }


        if (!isProtected) {
            logger.log(Level.INFO, "The resource/action '" + resource + "/" + action + "' is not protected by CA Single Sign-On. Access cannot be authorized.");
        }

        return isProtected;
    }

    public int processAuthorizationRequest(final String userIp, final String ssoCookie,final SiteMinderContext context) throws SiteMinderApiClassException {
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");//should never happen

        SiteMinderLowLevelAgent agent = context.getAgent();
        if(agent == null) throw new SiteMinderApiClassException("Unable to find CA Single Sign-On Agent");

        final String smAgentName = context.getResContextDef().getAgent();
        final String reqResource = context.getResContextDef().getResource();
        final String action = context.getResContextDef().getAction();
        final int currentAgentTimeSeconds = SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000);

        //Obtain the AttributeList encase isAuthN was called before
        List<SiteMinderContext.Attribute> attrList = context.getAttrList();
        // Ensure that the Attribute List is empty, as the
        // context could contain cached attributes from an isAuthN call
        context.setAttrList(new ArrayList<SiteMinderContext.Attribute>());
        SiteMinderContext.SessionDef sessionDef = null != context.getSessionDef() ? context.getSessionDef() : null;

        boolean updateSsoToken;
        int result;

        if (null != ssoCookie && ssoCookie.trim().length() > 0) {
            if ( null == sessionDef ){
                //decode the ssoToken
                result = agent.decodeSessionToken( context, ssoCookie);

                if (result != SUCCESS) {
                    logger.log(Level.FINE, "CA Single Sign-On Authorization attempt - CA Single Sign-On is unable to decode the token '" + SiteMinderUtil.safeNull(ssoCookie) + "'");
                    return result;
                }
                sessionDef = context.getSessionDef();
            }
        } else if ( null == context.getSessionDef() ){ // ensure that a valid SessionDef exits
            throw new SiteMinderApiClassException("SiteMinder Session Definition object is null");
        }
        //Get Agent cache and the authorization cache keys
        String sessionId = sessionDef.getId();
        AuthorizationCacheKey cacheKey = new AuthorizationCacheKey(sessionId, reqResource, action);
        final SiteMinderAgentContextCache agentCache = getCache(context.getConfig(), smAgentName);
        final Cache cache = agentCache.getAuthorizationCache();

        //Perform Session Validation
        if ( !validateDecodedSession( context.getSessionDef(), currentAgentTimeSeconds )){
            cache.remove( cacheKey );
            logger.log(Level.WARNING, "Session validation failed for the following SsoToken: " + ssoCookie);
            return CHALLENGE;

        }

        // TODO: Implement a tokenReuse threshold check, as a full SiteMinder Agent is expected to perform this operation
        // updateSsoToken = tokenReuseThresholdExceeded( context.getSessionDef().getSessionLastTime(),
        //        context.getSessionDef().getIdleTimeout(),
        //        currentAgentTimeSeconds );
        updateSsoToken = true;

        //check if we have the agent authorization cache disabled
        if(agentCache.getAuthorizationCacheSize() > 0) {

            SiteMinderContext cachedContext;
            //lookup in cache
            SiteMinderAuthResponseDetails cachedAuthResponseDetails =
                    getAuthorizationCacheEntry(cache, cacheKey, agentCache.getAuthorizationCacheMaxAge());

            if (cachedAuthResponseDetails == null) {
                // The context will contain attributes from the decode ( UserDn, UserName, ClientIP )
                // Note: Only the ClientIP (dotted-quad notation) is needed, by the authorize API call.
                result = agent.authorize(ssoCookie, userIp, context.getTransactionId(), context);

                if (result != YES) {
                    logger.log(Level.FINE, "SiteMinder authorization attempt - SiteMinder is unable to decode the token '" + SiteMinderUtil.safeNull(ssoCookie) + "'");
                    return FAILURE;
                }

                cachedContext = context;
                sessionId = cachedContext.getSessionDef().getId();

                cacheKey = new AuthorizationCacheKey(sessionId, reqResource, action);  // recreate key because session ID should be different

                SiteMinderAuthResponseDetails authResponseDetails =
                        new SiteMinderAuthResponseDetails(context.getSessionDef(), context.getAttrList());

                cache.store(cacheKey, authResponseDetails);
                //set stored attribute list to avoid loosing the attributes
                context.getAttrList().addAll(attrList);
            } else {
                List<SiteMinderContext.Attribute> updatedAttributes = new ArrayList<>();
                //Ensure all attributes are up to date i.e. ttl value is checked and those expired attributes are updated.
                int status = updateCachedAttributes(userIp, context, cachedAuthResponseDetails.getCreatedTimeStamp(), cachedAuthResponseDetails.getAttrList(), updatedAttributes);
                if (status == YES) {
                    //recreate the cache entry with the new attribute list
                    logger.log(Level.FINE, "SiteMinder authorization - updating SiteMinder authorization cache for the key " + cacheKey);
                    SiteMinderAuthResponseDetails cacheEntry = new SiteMinderAuthResponseDetails(context.getSessionDef(), updatedAttributes);
                    cache.store(cacheKey, cacheEntry);
                } else if (status != SUCCESS) {
                    logger.log(Level.FINE, "SiteMinder authorization - unable to update attributes. Removing cache entry for the key " + cacheKey + " from the cache");
                    //remove from the cache if attribute update was unsuccessful
                    cache.remove(cacheKey);
                    return status;//no need to continue
                }
                // Ensure the cached SMContext Attributes are available, if a cache hit occurs
                // Don't clear the attributeList as it will contain the decoded
                // UserDn, UserName, ClientIP
                //check all
                attrList.addAll(updatedAttributes);
                context.getAttrList().addAll(new ArrayList<>(attrList));
            }
        }
        else {
            // The context will contain attributes from the decode ( UserDn, UserName, ClientIP )
            // Note: Only the ClientIP (dotted-quad notation) is needed, by the authorize API call.
            result = agent.authorize(ssoCookie, userIp, context.getTransactionId(), context);

            if (result != YES) {
                logger.log(Level.FINE, "SiteMinder authorization attempt - SiteMinder is unable to decode the token '" + SiteMinderUtil.safeNull(ssoCookie) + "'");
                return FAILURE;
            }

            context.getAttrList().addAll(attrList);
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
                logger.log(Level.FINE, "Authorized user using cookie: " + ssoCookie);
            }
            else {
                logger.log(Level.FINE, "Authorized user via SessionDef object from the context");
            }
            //ensure the sessionDef values are used to update the Attribute list contained within the context
            //set currentServerTime
            context.getSessionDef().setCurrentServerTime(currentAgentTimeSeconds);
            result = YES;
        }

        //Remove possible duplicate attributes
        context.setAttrList( SiteMinderUtil.removeDuplicateAttributes(context.getAttrList(), new Comparator<SiteMinderContext.Attribute>() {
            @Override
            public int compare(SiteMinderContext.Attribute o1, SiteMinderContext.Attribute o2) {
                    if(o1.getId() != SiteMinderLowLevelAgent.HTTP_HEADER_VARIABLE_ID ) {
                        return o1.getName().compareTo(o2.getName());
                    }
                    else {
                        return 1;
                    }
            }
        }));
        return result;
    }

    protected int updateCachedAttributes(String userIp, SiteMinderContext context, long cacheCreatedTimeStamp, List<SiteMinderContext.Attribute> cachedAttributes, List<SiteMinderContext.Attribute> updatedAttributes) throws SiteMinderApiClassException {
        List<SiteMinderContext.Attribute> validAttributes = new ArrayList<>();
        List<SiteMinderContext.Attribute> attributesToUpdate = new ArrayList<>();
        long elapsedTime = (System.currentTimeMillis() - cacheCreatedTimeStamp)/1000;

        for(SiteMinderContext.Attribute attr : cachedAttributes) {
            if (attr.getTtl() > 0 && elapsedTime > attr.getTtl()) {
                attributesToUpdate.add(attr);
            } else {
                validAttributes.add(attr);
            }
        }

        int result = SUCCESS;

        if (attributesToUpdate.size() > 0) {
            SiteMinderLowLevelAgent agent = context.getAgent();

            result = agent.updateAttributes(userIp, context.getTransactionId(), context, attributesToUpdate, updatedAttributes);

            if (result == YES) {
                logger.log(Level.FINE, "SiteMinder cached attributes successfully updated");
                // add attributes that were not expired to list of updated ones
            }
        }

        updatedAttributes.addAll(validAttributes);

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

        final SiteMinderAgentContextCache agentCache = getCache(context.getConfig(), context.getResContextDef().getAgent());
        final Cache cache = agentCache.getAuthenticationCache();

       //Obtain the AttributeList encase isAuthN was called before
        List<SiteMinderContext.Attribute> attrList = context.getAttrList();
        // Ensure that the Attribute List is empty, as the
        // context could contain cached attributes from an isAuthN call
        context.setAttrList(new ArrayList<SiteMinderContext.Attribute>());

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

            result = agent.decodeSessionToken(context, ssoCookie);

            if ( result != SUCCESS ) {
                    logger.log(Level.FINE, "CA Single Sign-On Authentication attempt - CA Single Sign-On is unable to decode the token '" + SiteMinderUtil.safeNull(ssoCookie) + "'");
                    return result;
            }

            final AuthenticationCacheKey cacheKey =
                    new AuthenticationCacheKey(context.getSessionDef().getId(), context.getRealmDef().getOid());

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
            if(agentCache.getAuthenticationCacheSize() > 0) {
                //lookup in cache
                SiteMinderAuthResponseDetails authResponseDetails =
                        getAuthenticationCacheEntry(cache, cacheKey, agentCache.getAuthenticationCacheMaxAge());

                if (authResponseDetails == null) {
                    // Cache miss occurred we need to Validate the decoded ssoToken/session
                    // ValidateSession calls AgentApi.login, thus we can cache this token.
                    result = agent.validateSession(credentials, userIp, ssoCookie, context.getTransactionId(), context);
                    if (result != YES) {
                        //TODO: should we logout the session?
                        logger.log(Level.FINE, "Unable to validate user session!");
                        return FAILURE;
                    }

                    context.getAttrList().addAll(attrList);//we might have some attributes from the contexts needs to be preserved
                    cache.store(cacheKey, new SiteMinderAuthResponseDetails(context.getSessionDef(), context.getAttrList()));
                } else {
                    List<SiteMinderContext.Attribute> updatedAttributes = new ArrayList<>();
                    //Ensure all attributes are updated
                    int status = updateCachedAttributes(userIp, context, authResponseDetails.getCreatedTimeStamp(), authResponseDetails.getAttrList(), updatedAttributes);
                    if (status == YES) {
                        //recreate the cache entry with the new attribute list
                        logger.log(Level.FINE, "SiteMinder authentication - updating SiteMinder authorization cache for the key " + cacheKey);
                        SiteMinderAuthResponseDetails cacheEntry = new SiteMinderAuthResponseDetails(context.getSessionDef(), updatedAttributes);
                        cache.store(cacheKey, cacheEntry);
                    } else if (status != SUCCESS) {
                        logger.log(Level.FINE, "SiteMinder authentication - unable to update attributes. Removing cache entry for the key " + cacheKey + " from the cache");
                        //remove from the cache
                        cache.remove(cacheKey);
                        return status;//no need to continue
                    }
                    // Ensure the cached SMContext Attributes are available, if a cache hit occurs
                    // Don't clear the attributeList as it will contain the decoded
                    // UserDn, UserName, ClientIP
                    //check all
                    attrList.addAll(updatedAttributes);
                    context.getAttrList().addAll(new ArrayList<>(attrList));
                }
            }
            //the caching is disabled so validate session directly
            else {
                result = agent.validateSession(credentials, userIp, ssoCookie, context.getTransactionId(), context);
                if (result != YES) {
                    //TODO: should we logout the session?
                    logger.log(Level.FINE, "Unable to validate user session!");
                    return FAILURE;
                }

                context.getAttrList().addAll(attrList);//we might have some attributes from the contexts needs to be preserved
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

                logger.log(Level.FINE, "Authentication user using cookie:" + ssoCookie);
            } else {
                if (!"".equals(ssoCookie)) {
                    context.setSsoToken(ssoCookie);
                }
                //ensure the sessionDef is updated in the context
                //set currentServerTime
                context.getSessionDef().setCurrentServerTime( currentAgentTimeSeconds );
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

                if(agentCache.getAuthenticationCacheSize() > 0) {
                    final AuthenticationCacheKey  cacheKey =
                            new AuthenticationCacheKey(context.getSessionDef().getId(), context.getRealmDef().getOid());

                    //store the smContext to the isAuth Cache
                    context.getSessionDef().setCurrentServerTime(currentAgentTimeSeconds);

                    SiteMinderAuthResponseDetails authResponseDetails =
                            new SiteMinderAuthResponseDetails(context.getSessionDef(), context.getAttrList());

                    cache.store(cacheKey, authResponseDetails);
                }
            }
        }
        //remove any duplicate attributes accumulated in the context
        context.setAttrList( SiteMinderUtil.removeDuplicateAttributes( context.getAttrList(), new Comparator<SiteMinderContext.Attribute>() {
            @Override
            public int compare(SiteMinderContext.Attribute o1, SiteMinderContext.Attribute o2) {
                if(o1.getId() != SiteMinderLowLevelAgent.HTTP_HEADER_VARIABLE_ID ) {
                    return o1.getName().compareTo(o2.getName());
                }
                else {
                    return 1;
                }
            }
        } ));
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

    private SiteMinderAuthResponseDetails getAuthorizationCacheEntry(Cache cache, Object cacheKey, long entryMaxAge) {
        SiteMinderAuthResponseDetails cachedAuthResponseDetails =
                (SiteMinderAuthResponseDetails) cache.retrieve(cacheKey);

        if (null != cachedAuthResponseDetails) {
            logger.log(Level.FINE, "Found SiteMinder Authorization cache entry: " + cacheKey);

            //now check if the cached entry exceeded the max cached time then remove from cache
            if (System.currentTimeMillis() - cachedAuthResponseDetails.getCreatedTimeStamp() > entryMaxAge) {
                cache.remove(cacheKey);
                logger.log(Level.FINE, "Maximum authorization cache age exceeded. Removed expired entry: " +
                        cacheKey + " from authorization cache");
                return null;
            }
        } else {
            logger.log(Level.FINE, "SiteMinder Authorization - cache missed");
        }

        return cachedAuthResponseDetails;
    }

    private SiteMinderAuthResponseDetails getAuthenticationCacheEntry(Cache cache, Object cacheKey, long entryMaxAge) {
        SiteMinderAuthResponseDetails cachedAuthResponseDetails =
                (SiteMinderAuthResponseDetails) cache.retrieve(cacheKey);

        if (null != cachedAuthResponseDetails) {
            logger.log(Level.FINE, "Found SiteMinder Authentication cache entry: " + cacheKey);

            //now check if the cached entry exceeded the max cached time then remove from cache
            if (System.currentTimeMillis() - cachedAuthResponseDetails.getCreatedTimeStamp() > entryMaxAge) {
                cache.remove(cacheKey);
                logger.log(Level.FINE, "Maximum authentication cache age exceeded. Removed expired entry: " +
                        cacheKey + " from authentication cache");
                return null;
            }
        } else {
            logger.log(Level.FINE, "SiteMinder Authentication - cache missed");
        }

        return cachedAuthResponseDetails;
    }

    /**
     * Get the SiteMinderAgentContextCache
     *
     * @param smConfig the SiteMinderConfiguration
     * @param agentName the agent name
     * @return the cache
     */
    private SiteMinderAgentContextCache getCache(SiteMinderConfiguration smConfig, String agentName) {

        // Retrieve agent cache
        SiteMinderAgentContextCache cache = cacheManager.getCache(smConfig.getGoid(), agentName);
        //create cache if the entry is not found
        if (cache == null) {
            cache = cacheManager.createCache(smConfig.getGoid(), agentName,
                    getAgentPropertyInteger(smConfig, SiteMinderConfig.AGENT_RESOURCE_CACHE_SIZE_PROPNAME, 10),
                    getAgentPropertyLong(smConfig, SiteMinderConfig.AGENT_RESOURCE_CACHE_MAX_AGE_PROPNAME, 300000),
                    getAgentPropertyInteger(smConfig, SiteMinderConfig.AGENT_AUTHENTICATION_CACHE_SIZE_PROPNAME, 10),
                    getAgentPropertyLong(smConfig, SiteMinderConfig.AGENT_AUTHENTICATION_CACHE_MAX_AGE_PROPNAME, 300000),
                    getAgentPropertyInteger(smConfig, SiteMinderConfig.AGENT_AUTHORIZATION_CACHE_SIZE_PROPNAME, 10),
                    getAgentPropertyLong(smConfig, SiteMinderConfig.AGENT_AUTHORIZATION_CACHE_MAX_AGE_PROPNAME, 300000)
            );
        }

        return cache;
    }

    private <T> T getAgentProperty(final SiteMinderConfiguration smConfig, final String propName, T defaultValue, Functions.Unary<T, String> transform) {
        T value = null;
        String str = smConfig.getProperties().get(propName);
        if (StringUtils.isNotEmpty(str)) {
            value = transform.call(str);
        }
        if(value == null) {
            str = config.getProperty(SYSTEM_PROP_PREFIX + propName);
            if (StringUtils.isNotEmpty(str)) {
                value = transform.call(str);
            }
            else {
                value = defaultValue;
            }
        }
        return value;
    }

    private  Integer getAgentPropertyInteger(final SiteMinderConfiguration smConfig, final String propName, int defaultValue) {
        return getAgentProperty(smConfig, propName, defaultValue, new Functions.Unary<Integer, String>() {
            @Override
            public Integer call(String str) {
                try {
                    Integer val = new Integer(str);

                    if (val > -1) {
                        return val;
                    } else {
                        logger.log(Level.WARNING, "Value of " + propName + " must 0 or greater: " + str +
                                ". Using cluster settings default.");
                    }
                } catch (NumberFormatException ne) {
                    logger.log(Level.WARNING, "Value of " + propName + " is not Integer: " + str, ExceptionUtils.getDebugException(ne));
                }
                return null;
            }
        });
    }

    private Long getAgentPropertyLong(final SiteMinderConfiguration smConfig, final String propName, long defaultValue) {
        return getAgentProperty(smConfig, propName, defaultValue, new Functions.Unary<Long, String>() {
            @Override
            public Long call(String s) {
                try {
                    Long val = new Long(s);

                    if (val > -1) {
                        return val;
                    } else {
                        logger.log(Level.WARNING, "Value of " + propName + " must 0 or greater: " + s +
                                ". Using cluster settings default.");
                    }
                } catch (NumberFormatException ne) {
                    logger.log(Level.WARNING, "Value of " + propName + " is not Long: " + s, ExceptionUtils.getDebugException(ne));
                }
                return null;
            }
        });
    }


}
