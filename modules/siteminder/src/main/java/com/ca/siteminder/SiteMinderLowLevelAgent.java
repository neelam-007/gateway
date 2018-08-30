package com.ca.siteminder;

import com.ca.siteminder.util.SiteMinderUtil;
import com.l7tech.util.ConfigFactory;
import com.netegrity.sdk.apiutil.SmApiConnection;
import com.netegrity.sdk.apiutil.SmApiException;
import com.netegrity.sdk.apiutil.SmApiResult;
import com.netegrity.sdk.apiutil.SmApiSession;
import com.netegrity.sdk.dmsapi.*;
import com.netegrity.sdk.policyapi.SmPolicyApi;
import com.netegrity.sdk.policyapi.SmPolicyApiImpl;
import com.netegrity.sdk.policyapi.SmUserDirectory;
import netegrity.siteminder.javaagent.*;
import org.apache.commons.lang.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/26/13
 */
public class SiteMinderLowLevelAgent {
    public static final int HTTP_HEADER_VARIABLE_ID = 224;
    public static final int HTTP_SM_ONACCEPTTEXT = 230;
    public static final int HTTP_ON_ACCEPT_REDIRECT = 229;
    public static final int HTTP_COOKIE_VALUE = 231;

    private static final Logger logger = Logger.getLogger(SiteMinderLowLevelAgent.class.getName());
    private static final Map<String, String> defaultAcoAttrMap = new HashMap<>();
    private static final String FAIL_MESSAGE = "Failed to change user password: ";

    private boolean initialized = false;

    private final AgentAPI agentApi;
    private String agentIP;
    private boolean agentCheckSessionIP;
    private boolean updateCookie;
    private final SiteMinderConfig agentConfig;

    static {
        defaultAcoAttrMap.put(SiteMinderAgentConstants.ATTR_ACO_SSOZONE_NAME,
                SiteMinderAgentConstants.ATTR_ACO_SSOZONE_NAME_DEFAULT_VALUE);
        defaultAcoAttrMap.put(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_DEFAULT_VALUE);
        defaultAcoAttrMap.put(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE_DEFAULT_VALUE);
        defaultAcoAttrMap.put(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_DEFAULT_VALUE);
        defaultAcoAttrMap.put(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE_DEFAULT_VALUE);
        defaultAcoAttrMap.put(SiteMinderAgentConstants.ATTR_ACO_USE_SECURE_COOKIES,
                SiteMinderAgentConstants.ATTR_ACO_USE_SECURE_COOKIES_DEFAULT_VALUE);
        defaultAcoAttrMap.put(SiteMinderAgentConstants.ATTR_ACO_USE_HTTP_ONLY_COOKIES,
                SiteMinderAgentConstants.ATTR_ACO_USE_HTTP_ONLY_COOKIES_DEFAULT_VALUE);
        defaultAcoAttrMap.put(SiteMinderAgentConstants.ATTR_ACO_PERSISTENT_COOKIES,
                SiteMinderAgentConstants.ATTR_ACO_PERSISTENT_COOKIES_DEFAULT_VALUE);
        defaultAcoAttrMap.put(SiteMinderAgentConstants.ATTR_ACO_COOKIE_VALIDATION_PERIOD,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_VALIDATION_PERIOD_DEFAULT_VALUE);
    }

    public SiteMinderLowLevelAgent(final SiteMinderConfig config) throws SiteMinderApiClassException {
        this(config, new AgentAPI());
    }

    public SiteMinderLowLevelAgent(final SiteMinderConfig config, final AgentAPI agentAPI) throws SiteMinderApiClassException {
        agentConfig = config;
        agentApi = agentAPI;
        initialize();
    }

    static {
        if (ConfigFactory.getBooleanProperty("com.l7tech.server.siteminder.enableJavaCompatibilityMode", true)) {
            AgentAPI.enableJavaCompatibilityMode();//fix clustering issue by enabling java compatibility mode so it can
        }
    }

    private boolean initialize() throws SiteMinderApiClassException {
        initialized = false;
        try {
            agentIP = agentConfig.getAddress();
            agentCheckSessionIP = agentConfig.isIpCheck();
            updateCookie = agentConfig.isUpdateSSOToken();

            InitDef initDef;
            Iterator iter = agentConfig.getServers().iterator();

            if (iter.hasNext()) {
                if (agentConfig.isCluster()) {
                    logger.log(Level.FINE, "Initializing agent in cluster mode...");
                    initDef = new InitDef(agentConfig.getHostname(), agentConfig.getSecret(), agentConfig.getClusterThreshold(),(ServerDef)iter.next());
                } else {
                    logger.log(Level.FINE, "Initializing agent in non-cluster mode...");
                    initDef = new InitDef(agentConfig.getHostname(), agentConfig.getSecret(), agentConfig.isNonClusterFailover(), (ServerDef)iter.next());
                }
            }
            else {
                initDef = new InitDef();//if we dont have any configuration assume it's empty
            }

            // additional servers
            while(iter.hasNext()) {
                ServerDef serverDef = (ServerDef)iter.next();
                if (0 == serverDef.clusterSeq) {
                    initDef.addServerDef(serverDef);
                } else {
                    initDef.addServerDef(serverDef.serverIpAddress,
                            serverDef.connectionMin,
                            serverDef.connectionMax,
                            serverDef.connectionStep,
                            serverDef.timeout,
                            serverDef.authorizationPort,
                            serverDef.authenticationPort,
                            serverDef.accountingPort,
                            serverDef.clusterSeq);
                }
            }

            int cryptoOpMode = agentConfig.getFipsMode();

            initDef.setCryptoOpMode(cryptoOpMode);
//            agentApi.getConfig(initDef, agentName, null); //the last parameter is used to configure ACO

            int retCode = agentApi.init(initDef);

            if (retCode == AgentAPI.SUCCESS) {
                //TODO: check if the management info is correct and if we need to put it into the cluster property
                ManagementContextDef mgtCtxDef = new ManagementContextDef(ManagementContextDef.MANAGEMENT_SET_AGENT_INFO, "Product=sdk,Platform=WinNT/Solaris,Version=12.52,Update=0,Label=160");
                AttributeList attrList = new AttributeList();
                agentApi.doManagement(mgtCtxDef, attrList);
                mgtCtxDef = new ManagementContextDef(ManagementContextDef.MANAGEMENT_GET_AGENT_COMMANDS, "");//TODO: why do we call create management context for the second time? is it really necessary?
                attrList.removeAllAttributes();//TODO: this is very suspicious code why do we need to remove all attributes?
                retCode = agentApi.doManagement(mgtCtxDef, attrList);// what the hack? do management again?

                switch(retCode) {
                    case AgentAPI.NOCONNECTION:
                    case AgentAPI.TIMEOUT:
                    case AgentAPI.FAILURE:
                        logger.log(Level.SEVERE, "Unable to connect to the CA Single Sign-On Policy Server.");
                        initialized = false;
                        break;
                    case AgentAPI.INVALID_MGMTCTXDEF:
                        logger.log(Level.SEVERE, "Management Context is invalid");
                        initialized = false;
                        break;
                    case AgentAPI.INVALID_ATTRLIST:
                        initialized = false;
                        logger.log(Level.SEVERE,"Attribute List is invalid"); //this should never happen!
                        break;
                    default:
                        initialized = true;
                }
            }
            else {
                if(retCode == AgentAPI.NOCONNECTION) {
                    logger.log(Level.SEVERE, "The CA Single Sign-On Agent " + agentConfig.getHostname() +
                            " cannot connect to the Policy Server");
                }
                else {
                    logger.log(Level.SEVERE, "The CA Single Sign-On Agent hostname and/or the secret is incorrect.");
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed initialization", e);
            throw new SiteMinderApiClassException("Unable to initialize CA Single Sign-On Agent API", e);
        }

        return initialized;
    }

    public int unInitialize() {
        return agentApi.unInit();
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Authenticate the principal against the resource
     * @param credentials the user credential to authenticate
     * @param userIp    the ip address of the client
     * @param transactionId the transaction id
     * @param context SiteMinderContext object
     * @param createToken
     */
    int authenticate(SiteMinderCredentials credentials, String userIp, String transactionId, SiteMinderContext context, boolean createToken)
            throws SiteMinderApiClassException {

        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");
        List<SiteMinderContext.Attribute> attributes = context.getAttrList();
        ResourceContextDef resCtxDef = getSiteMinderResourceDefFromContext(context);
        RealmDef realmDef = getSiteMinderRealmDefFromContext(context);

        SessionDef sessionDef = new SessionDef();
        AttributeList attrList = new AttributeList();
        String clientIP = getClientIp(userIp, context);

        UserCredentials userCreds;

        if(credentials == null) {
            userCreds = new UserCredentials();
        } else {
            userCreds = credentials.getUserCredentials();
        }

        int retCode = agentApi.loginEx(clientIP, resCtxDef, realmDef, userCreds, sessionDef, attrList, transactionId);
        //Add ATTR_USERNAME and ATTR_CLIENTIP even if they contain no value
        attrList.addAttribute(AgentAPI.ATTR_USERNAME, 0, 0, null, userCreds.name != null? userCreds.name.getBytes() : new byte[0]);
        attrList.addAttribute(AgentAPI.ATTR_CLIENTIP, 0,  0, null, clientIP != null? clientIP.getBytes() : new byte[0]);

        final String ssoZoneName = context.getSsoZoneName();
        if (StringUtils.isNotBlank(ssoZoneName)) {
            attrList.addAttribute(AgentAPI.ATTR_SSOZONE, 0, 0, null, ssoZoneName.getBytes());
        }

        storeAttributes(attributes, attrList);

        if (retCode != AgentAPI.YES) {
            logger.log(Level.FINE, "CA Single Sign-On authorization attempt: User: '" + userCreds.name + "' Resource: '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "' Access Mode: '" + SiteMinderUtil.safeNull(resCtxDef.action) + "'");
            attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.SESS_DEF_REASON, getSessionDefReasonCodeAsString(sessionDef)));

        }
        else {
            logger.log(Level.FINE, "Authenticated - principal '" + userCreds.name + "'" + " resource '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "'");
            if(createToken) {
                logger.log(Level.FINE, "Creating SSO Token");
                retCode = getSsoToken(userCreds, resCtxDef.resource, sessionDef, attrList, context);
            }
        }

        //finally, set SessionDef in the SiteMinder context. This might be useful for the authorization
        context.setSessionDef(new SiteMinderContext.SessionDef(sessionDef.reason,
                                                               sessionDef.idleTimeout,
                                                               sessionDef.maxTimeout,
                                                               sessionDef.currentServerTime,
                                                               sessionDef.sessionStartTime,
                                                               sessionDef.sessionLastTime,
                                                               sessionDef.id,
                                                               sessionDef.spec));

        return retCode;
    }

    private ResourceContextDef getSiteMinderResourceDefFromContext(SiteMinderContext context) {
        final SiteMinderContext.ResourceContextDef resSmContextDef = context.getResContextDef();
        return resSmContextDef != null? new ResourceContextDef(resSmContextDef.getAgent(), resSmContextDef.getServer(), resSmContextDef.getResource(), resSmContextDef.getAction()) : new ResourceContextDef();
    }

    private RealmDef getSiteMinderRealmDefFromContext(SiteMinderContext context) {
        RealmDef realmDef = new RealmDef();
        final SiteMinderContext.RealmDef realmSmContextDef = context.getRealmDef();
        if(realmSmContextDef != null) {
            realmDef.name = realmSmContextDef.getName();
            realmDef.oid = realmSmContextDef.getOid();
            realmDef.domOid = realmSmContextDef.getDomOid();
            realmDef.credentials = realmSmContextDef.getCredentials();
            realmDef.formLocation = realmSmContextDef.getFormLocation();
        }
        return realmDef;
    }

    /**
     * Authorize the session against the resource
     * @param ssoToken  the SSO token obtained previously by authenticate method
     * @param transactionId the transaction id
     * @param context SiteMinderContext
     * @throws java.security.AccessControlException on access control error
     */
    int authorize(String ssoToken, String userIp, String transactionId, SiteMinderContext context)
            throws SiteMinderApiClassException {

        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");
        int result;

        List<SiteMinderContext.Attribute> attributes = context.getAttrList();
        ResourceContextDef resCtxDef = getSiteMinderResourceDefFromContext(context);
        RealmDef realmDef = getSiteMinderRealmDefFromContext(context);
        SessionDef sessionDef = getSiteMinderSessionDefFromContext(context);// get SessionDef object from the context

        if( null != ssoToken && null == sessionDef ){
            AttributeList attrList = new AttributeList();

            result = decodeSsoToken(ssoToken, attrList);

            storeAttributes(attributes, attrList);

            if (result != AgentAPI.SUCCESS) {
                logger.log(Level.FINE, "CA Single Sign-On authorization attempt - CA Single Sign-On is unable to decode the token '" + SiteMinderUtil.safeNull(ssoToken) + "'");
                return result;
            }

            sessionDef = createSmSessionFromAttributes(attrList);
        }

        AttributeList attributeList = new AttributeList();
        //TODO: use authorizeEx and return a list of SM variables for processing
        //Authorize sets the lastSessionTime value to the original sessionStartTime, causing in policy session validation to fail.
        int sessionLastTime = sessionDef.sessionLastTime;
        result = agentApi.authorize(getClientIp(userIp, context), transactionId, resCtxDef, realmDef, sessionDef, attributeList);
        //might be some other context variables that needs to be set
        storeAttributes(attributes, attributeList);

        if (result == AgentAPI.YES) {
            if(!isAttributePresent(attributes, SiteMinderAgentConstants.ATTR_DEVICENAME)){
                attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_DEVICENAME, agentConfig.getHostname()));
            }
            sessionDef.sessionLastTime = sessionLastTime; //correctly set the sessionLastTime on a successful authZ
            //finally, set SessionDef in the SiteMinder context. This might be useful for the authorization
            context.setSessionDef(new SiteMinderContext.SessionDef(sessionDef.reason,
                    sessionDef.idleTimeout,
                    sessionDef.maxTimeout,
                    sessionDef.currentServerTime,
                    sessionDef.sessionStartTime,
                    sessionDef.sessionLastTime,
                    sessionDef.id,
                    sessionDef.spec));

            logger.log(Level.FINE, "Authorized - against" + " resource '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "'SSO token : " + context.getSsoToken());
        }
        else {
            logger.log(Level.FINE, "CA Single Sign-On authorization attempt - Unauthorized session = '" + ssoToken + "', resource '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "', result code '" + result + "'.");
        }
        // add sessionDef reason code
        attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.SESS_DEF_REASON, getSessionDefReasonCodeAsString( sessionDef )));

        return result;
    }

    int updateAttributes(String userIp, String transactionId, SiteMinderContext context, List<SiteMinderContext.Attribute> attributesToUpdate, List<SiteMinderContext.Attribute> updatedAttributes) throws SiteMinderApiClassException {
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");
        int result;

        ResourceContextDef resCtxDef = getSiteMinderResourceDefFromContext(context);
        RealmDef realmDef = getSiteMinderRealmDefFromContext(context);
        SessionDef sessionDef = getSiteMinderSessionDefFromContext(context);// get SessionDef object from the context

        AttributeList requestAttributeList = SiteMinderUtil.convertToAttributeList(attributesToUpdate);
        AttributeList responseAttributeList = new AttributeList();

        result = agentApi.updateAttributes(getClientIp(userIp, context), transactionId, resCtxDef, realmDef, sessionDef, requestAttributeList, responseAttributeList);

        if (result != AgentAPI.YES) {
            logger.log(Level.FINE, "SiteMinder updateAttributes attempt - SiteMinder is unable to update attributes");
        } else {
            /**
             * Iterate updated attributes list and:
             * - remove attributes 146 and 147 - they are "always returned for some historical reason and for
             * caching purpose you can ignore them", according to Canh.Cao@ca.com of the SiteMinder engineering team
             * - remove attributes for which we have received updates from the list to which we will add the updates
             *
             * The result of this is that if an update is requested for an attribute but not received, then we will
             * return the old value rather than remove it from the attribute list. The expired value will be kept,
             * and this is also the expected behaviour as per the SiteMinder engineering team.
             */
            AttributeList updatedAttributeList = new AttributeList();

            for (int i = 0; i < responseAttributeList.getAttributeCount(); i++) {
                Attribute updated = responseAttributeList.getAttributeAt(i);

                switch (updated.id) {
                    case 146:
                    case 147:
                        // just remove these - they are irrelevant
                        break;
                    default:
                        for (SiteMinderContext.Attribute original : attributesToUpdate) {
                            if (updated.oid.equals(original.getOid())) {
                                logger.log(Level.FINE, "Update found for attribute OID: " + updated.oid + ", ID: " + updated.id);
                                updatedAttributeList.addAttribute(updated);
                                break;
                            }
                        }
                        break;
                }
            }

            // convert and store the attributes for which we received updates
            if (responseAttributeList.getAttributeCount() > 0) {
                storeAttributes(updatedAttributes, updatedAttributeList);
            }
        }

        return result;
    }

    int decodeSessionToken(SiteMinderContext context, String ssoToken) throws SiteMinderApiClassException {
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");
        int result = AgentAPI.FAILURE;

        if(ssoToken != null) {
            AttributeList attrList = new AttributeList();

            List<SiteMinderContext.Attribute> attributes = context.getAttrList();
            result = decodeSsoToken(ssoToken, attrList);
            storeAttributes(attributes, attrList);

            for (int i = 0; i < attrList.getAttributeCount(); i++) {
                final Attribute attribute = attrList.getAttributeAt(i);
                if (attribute.id == AgentAPI.ATTR_SSOZONE) {
                    context.setSsoZoneName(SiteMinderUtil.chopNull(new String(attribute.value)));
                    break;
                }
            }

            if (result != AgentAPI.SUCCESS) {
                logger.log(Level.FINE, "SiteMinder authorization attempt - SiteMinder is unable to decode the token '" + SiteMinderUtil.safeNull(ssoToken) + "'");
                return result;
            }
            else {
                //finally, set SessionDef in the SiteMinder context. This might be useful for the authorization
                SessionDef sessionDef = createSmSessionFromAttributes(attrList);

                context.setSessionDef(new SiteMinderContext.SessionDef(sessionDef.reason,
                        sessionDef.idleTimeout,
                        sessionDef.maxTimeout,
                        sessionDef.currentServerTime,
                        sessionDef.sessionStartTime,
                        sessionDef.sessionLastTime,
                        sessionDef.id,
                        sessionDef.spec));
            }
        }

        return result;
    }

    private boolean isAttributePresent(List<SiteMinderContext.Attribute> attributes, String id) {
        for(SiteMinderContext.Attribute attr: attributes) {
            if(attr.getName().equals(id)){
                return true;
            }
        }
        return  false;
    }

    protected SessionDef getSiteMinderSessionDefFromContext(SiteMinderContext context) {
        SessionDef sessionDef = new SessionDef();
        SiteMinderContext.SessionDef smContextSessionDef = context.getSessionDef();
        if (smContextSessionDef != null) {
            sessionDef.id = smContextSessionDef.getId();
            sessionDef.spec = smContextSessionDef.getSpec();
            sessionDef.currentServerTime = smContextSessionDef.getCurrentServerTime();
            sessionDef.idleTimeout = smContextSessionDef.getIdleTimeout();
            sessionDef.maxTimeout = smContextSessionDef.getMaxTimeout();
            sessionDef.sessionStartTime = smContextSessionDef.getSessionStartTime();
            sessionDef.sessionLastTime = smContextSessionDef.getSessionLastTime();
            sessionDef.reason = smContextSessionDef.getReason();
        }
        return sessionDef;
    }

    private AttributeList getSiteMinderSessionDefAttrListFromContext( SiteMinderContext context ){
        AttributeList attrList = new AttributeList();
        SiteMinderContext.SessionDef sessionDef = context.getSessionDef();
        if( sessionDef != null ) {
            attrList.addAttribute(new Attribute(AgentAPI.ATTR_LASTSESSIONTIME, 0, 0, "", SiteMinderUtil.safeIntToByteArr(sessionDef.getCurrentServerTime())));
            attrList.addAttribute(new Attribute(AgentAPI.ATTR_STARTSESSIONTIME, 0, 0, "", SiteMinderUtil.safeIntToByteArr(sessionDef.getSessionStartTime())));
            //ATTR_CURRENTSERVERTIME = int 153
            Attribute currentServerTime = new Attribute(SiteMinderAgentConstants.ATTR_CURRENTAGENTTIME, 0, 0, "", SiteMinderUtil.safeIntToByteArr(sessionDef.getCurrentServerTime()));
            attrList.addAttribute(currentServerTime);
            attrList.addAttribute(new Attribute(AgentAPI.ATTR_SESSIONID, 0, 0, "", sessionDef.getId().getBytes()));
            attrList.addAttribute(new Attribute(AgentAPI.ATTR_SESSIONSPEC, 0, 0, "", sessionDef.getSpec().getBytes()));
            attrList.addAttribute(new Attribute(AgentAPI.ATTR_MAXSESSIONTIMEOUT, 0, 0, "", SiteMinderUtil.safeIntToByteArr(sessionDef.getMaxTimeout())));
            attrList.addAttribute(new Attribute(AgentAPI.ATTR_IDLESESSIONTIMEOUT, 0, 0, "", SiteMinderUtil.safeIntToByteArr(sessionDef.getIdleTimeout())));
        }
        return attrList;
    }

    private int decodeSsoToken(String ssoToken, AttributeList attrList) {
        //TODO: why version is set to 1?
        //since this is a non-3rd party cookie last parameter is set to false
        TokenDescriptor td = new TokenDescriptor(1, false);
        StringBuffer sb = new StringBuffer();

        int result = agentApi.decodeSSOToken(ssoToken, td, attrList, false, sb);//do not update SSO token at this point

        if (result ==  AgentAPI.SUCCESS) {
            logger.log(Level.FINE, "Third party token? '" + td.bThirdParty + "'; Version '" + td.ver + "'.");
        }
        else {
            logger.log(Level.FINE, "Unable to decode ssotoken + " + ssoToken);
        }

        return result;
    }

    /**
     * Tests whether the resource is protected. This version will initialize the resCtxDef & realmDef when returned.
     * @param userIp - ip address of the client
     * @param smAgentName - SiteMinder agent name
     * @param serverName - name of the server sending request (this value appears in the request attribute 217)
     * @param resource - SiteMinder resource
     * @param action - action performed (GET, POST, PUT, DELETE, ...)
     * @param context - SiteMinder context object
     * @return - true if the resource is protected by the SiteMinder Policy Server
     */
    protected boolean isProtected(String userIp, String smAgentName, String serverName, String resource, String action, SiteMinderContext context) throws  SiteMinderApiClassException {
        // In order to preserve previous behavior of setting an empty string when the serverName is undefined
        final String server = serverName != null ? serverName : "";
        // The resCtxDef holds the agent, server, resource and action
        ResourceContextDef resCtxDef = new ResourceContextDef(smAgentName, server, resource, action);
        // The realmDef object will contain the realm handle for the resource if the resource is protected.
        RealmDef realmDef = new RealmDef();
        // check the requested resource/action is actually protected by SiteMinder
        boolean isProtected = isProtected(userIp, resCtxDef, realmDef, context);
        //now set the context
        context.setResContextDef(new SiteMinderContext.ResourceContextDef(resCtxDef.agent, resCtxDef.server, resCtxDef.resource, resCtxDef.action));
        context.setRealmDef(new SiteMinderContext.RealmDef(realmDef.name, realmDef.oid, realmDef.domOid, realmDef.credentials, realmDef.formLocation));
        //determine which authentication scheme to use
        buildAuthenticationSchemes(context, realmDef.credentials);

        return isProtected;
    }

    /**
     * Tests whether the resource is protected. This version will initialize the resCtxDef & realmDef when returned.
     *
     * @param resCtxDef the SiteMinder resource context definition to be initialized
     * @param realmDef  the SiteMinder realm definition to be initialized
     */
    private boolean isProtected(String userIp, ResourceContextDef resCtxDef, RealmDef realmDef, SiteMinderContext context) throws SiteMinderApiClassException {
        int retCode = agentApi.isProtectedEx(getClientIp(userIp, context), resCtxDef, realmDef, context.getTransactionId());
        if(retCode != AgentAPI.NO && retCode != AgentAPI.YES){
            throw new SiteMinderApiClassException(getCommonErrorMessage(retCode));
        }

        return retCode == AgentAPI.YES;
    }

    /**
     * Changes the password of the specified user DN.
     *
     * @param adminUsername the SiteMinder administrator username
     * @param adminPassword the SiteMinder administrator password
     * @param domOid the object ID of the domain
     * @param username the username
     * @param oldPassword the old password
     * @param newPassword the new password
     * @return the reason code. Returns 0 if successful.
     * @throws SiteMinderApiClassException
     */
    int changePassword(
            final String adminUsername,
            final String adminPassword,
            final String domOid,
            final String username,
            final String oldPassword,
            final String newPassword) throws SiteMinderApiClassException {
        SmApiSession apiSession = null;
        try {
            apiSession = this.getAdminApiSession(adminUsername, adminPassword);

            final SmDmsUser dmsUser = this.findUserFromUserDirectory(apiSession, domOid, username);
            boolean doNotRequireOldPassword = false;
            SmApiResult result = dmsUser.changePassword(newPassword, oldPassword, doNotRequireOldPassword);
            if (!result.isSuccess()) {
                if (result.getReason() == 1003 && result.getMessage() != null && (result.getMessage().contains("data 773,") || result.getMessage().contains("data 532,"))) {
                    // SmDmsUser#changePassword() failed due to password must change or password has expired
                    // when doNotRequireOldPassword is true, even though correct old password is provided.
                    // As a workaround, call SmDmsUser#changePassword() again with doNotRequireOldPassword set to true.
                    // When doNotRequireOldPassword is true, it doesn't check old password and resets the password
                    // to new password
                    // "data 773," means ERROR_PASSWORD_MUST_CHANGE
                    // "data 532," means ERROR_PASSWORD_EXPIRED
                    //
                    logger.log(Level.FINE, "Failed to change user password. Password must change or password is expired. Attempting to change user again with doNotRequireOldPassword set to false: {0}", result);
                    doNotRequireOldPassword = true;
                    result = dmsUser.changePassword(newPassword, oldPassword, doNotRequireOldPassword);
                    if (!result.isSuccess()) {
                        logger.log(Level.WARNING, "Failed to change user password: {0}",result);
                    }
                } else {
                    logger.log(Level.WARNING, "Failed to change user password: {0}",result);
                     if (result.getReason() == 0) {
                        // It's possible that result.isSuccess() returns false to indicate failure but reason is still set to 0.
                        // In this case, throw an exception.
                        throw new SiteMinderApiClassException(FAIL_MESSAGE + result.getMessage());
                    }
                }
            }
            return result.getReason();
        } catch (SmApiException | UnknownHostException e) {
            logger.log(Level.WARNING, "An error occurred attempting to change user password", e);
            throw new SiteMinderApiClassException("An error occurred attempting to change user password", e);
        } finally {
            if (apiSession != null) {
                try {
                    final SmApiResult result = apiSession.logout();
                    if (!result.isSuccess()) {
                        logger.log(Level.WARNING, "Failed to end administrator session: {0}", result);
                    }
                } catch (SmApiException e) {
                    logger.log(Level.WARNING, "An error occurred attempting to end administrator session", e);
                }
            }
        }
    }

    /**
     * Enables the user account of the specified user DN.
     *
     * @param adminUsername the SiteMinder administrator username
     * @param adminPassword the SiteMinder administrator password
     * @param domOid the object ID of the domain
     * @param username the username
     * @return the reason code. Returns 0 if successful.
     * @throws SiteMinderApiClassException
     */
    int enableUser(
            final String adminUsername,
            final String adminPassword,
            final String domOid,
            final String username) throws SiteMinderApiClassException {
        SmApiSession apiSession = null;
        try {
            apiSession = this.getAdminApiSession(adminUsername, adminPassword);
            final SmDmsUser dmsUser = this.findUserFromUserDirectory(apiSession, domOid, username);
            final SmApiResult result = dmsUser.setEnable();
            if (!result.isSuccess()) {
                logger.log(Level.WARNING, "Failed to enable user account: {0}", result);
            }
            return result.getReason();
        } catch (SmApiException | UnknownHostException e) {
            logger.log(Level.WARNING, "An error occurred attempting to enable user account", e);
            throw new SiteMinderApiClassException("An error occurred attempting to enable user account", e);
        } finally {
            if (apiSession != null) {
                try {
                    final SmApiResult result = apiSession.logout();
                    if (!result.isSuccess()) {
                        logger.log(Level.WARNING, "Failed to end administrator session: {0}", result);
                    }
                } catch (SmApiException e) {
                    logger.log(Level.WARNING, "An error occurred attempting to end administrator session", e);
                }
            }
        }
    }

    private SmApiSession getAdminApiSession (final String adminUsername, final String adminPassword) throws UnknownHostException, SmApiException, SiteMinderApiClassException {
        final SmApiConnection apiConnection = new SmApiConnection(agentApi);
        final SmApiSession apiSession = new SmApiSession(apiConnection);

        final int challengeReason = 0;
        SmApiResult result = apiSession.login(adminUsername, adminPassword, InetAddress.getByName(agentConfig.getAddress()), challengeReason);
        if (!result.isSuccess()) {
            logger.log(Level.WARNING, "Failed to create administrator session: {0}", result);
            throw new SiteMinderApiClassException("Failed to create administrator session: " + result.getMessage());
        }
        return apiSession;
    }

    private SmDmsUser findUserFromUserDirectory (final SmApiSession apiSession, final String domOid, final String username) throws SmApiException, SiteMinderApiClassException {
        // Find User Directory using DMS Directory Context
        SmApiResult result;

        final SmPolicyApi policyApi = new SmPolicyApiImpl(apiSession);

        // Find all user directories for the specified domain.
        //
        Vector dirs = new Vector();
        result = policyApi.getUserDirSearchOrder(domOid, dirs);
        if (!result.isSuccess()) {
            logger.log(Level.WARNING, "Failed to get user directories: {0}", result);
            throw new SiteMinderApiClassException("Failed to get user directories: " + result.getMessage());
        }
        logger.log(Level.FINE, "Found {0} user directories", new Object[] { dirs.size() });

        // Search for user in each user directory until user is found.
        //
        for (Object dir : dirs) {
            String userDirectoryName = (String) dir;
            logger.log(Level.FINE, "Searching user directory ''{0}''", new Object[] { userDirectoryName });

            final SmUserDirectory userDir = new SmUserDirectory();
            result = policyApi.getUserDirectory(userDirectoryName, userDir);
            if (!result.isSuccess()) {
                logger.log(Level.FINE, "Failed to get user directory ''{0}'': {1}", new Object[] { userDirectoryName, result.getMessage() });
            }

            final String userDirNamespace = userDir.getNamespace();
            logger.log(Level.FINE, "Found user directory ''{0}'' with namespace ''{1}''", new Object[] { userDirectoryName, userDirNamespace });

            if ( !"AD:".equals(userDirNamespace) && !SmUserDirectory.LDAP_NAMESPACE.equals(userDirNamespace) ) {
                // TODO: Include queryFilter input option in the assertion UI to support other user directory namespace (ie. ODBC, Custom).
                // queryFilter can represented as a tuple of search objects to enumerate over when multiple directory exist/configured.
                logger.log(Level.FINE, "Skipping user directory ''{0}''. Namespace ''{1}'' not supported.", new Object[] { userDirectoryName, userDirNamespace });
                continue;
            }

            final SmDmsApi dmsApi = new SmDmsApiImpl(apiSession);
            final SmDmsDirectoryContext dirContext = new SmDmsDirectoryContext();
            result = dmsApi.getDirectoryContext(userDir, new SmDmsConfig(), dirContext);
            if (!result.isSuccess()) {
                logger.log(Level.FINE, "Failed to get directory context for user directory ''{0}'': {1}", new Object[] { userDirectoryName, result.getMessage() });
            }

            final String searchRoot = userDir.getSearchRoot();
            final String searchFilter = userDir.getUserLookupStart() + username + userDir.getUserLookupEnd();
            logger.log(Level.FINE, "Searching user directory ''{0}'' with search filter ''{1}'' starting at ''{2}''", new Object[] { userDirectoryName, searchFilter, searchRoot });

            final SmDmsSearch search = new SmDmsSearch(searchFilter, searchRoot);
            search.setScope(2); // Search whole tree.
            search.setNextItem(0);
            search.setPreviousItem(0);

            final SmDmsDirectory dmsDir = dirContext.getDmsDirectory();
            final SmDmsOrganization orgRoot = dmsDir.newOrganization(searchRoot);
            result = orgRoot.search(search, SmDmsSearch.Forward);
            if (!result.isSuccess()) {
                logger.log(Level.FINE, "Failed to search user directory ''{0}'': {1}", new Object[] { userDirectoryName, result.getMessage() });
            }

            final List results = search.getResults();
            // The first item is always SmDmsSearchResultParams.
            final SmDmsSearchResultParams searchParams = (SmDmsSearchResultParams) results.get(0);
            if (searchParams.m_nItemsFound == 1) {
                // The second item is the user.
                logger.log(Level.FINE, "Found user in user directory ''{0}''", new Object[]{userDirectoryName});
                return (SmDmsUser) results.get(1);
            } else if (searchParams.m_nItemsFound != 0) {
                logger.log(Level.FINE, "Unexpected number of users found in user directory ''{0}'': {1}", new Object[] { userDirectoryName, searchParams });
            }
            // else, user is not found in user directory. No need to log this.
        }

        throw new SiteMinderApiClassException("Failed to find user in user directories.");
    }

    /**
     * Validate the use session using the sessionDef extracted from the DecodeSSOToken()
     * @param credentials user credential to validate
     * @param userIp    the ip address of the client
     * @param ssoToken  ssoToken which contains sessionDef after decoded
     * @param transactionId the transaction id
     * @param context the SiteMinder context
     * @throws SiteMinderApiClassException on invalid sessionDef

     */
    public int validateSession(SiteMinderCredentials credentials, String userIp, String ssoToken, String transactionId, SiteMinderContext context)
            throws SiteMinderApiClassException {
        //check if the context is null and throw exception
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");
        List<SiteMinderContext.Attribute> attributes = context.getAttrList();
        ResourceContextDef resCtxDef = getSiteMinderResourceDefFromContext(context);
        RealmDef realmDef = getSiteMinderRealmDefFromContext(context);

        AttributeList attrList = new AttributeList();
        SessionDef sessionDef;
        int result;

        UserCredentials userCreds;

        if(credentials == null) {
            userCreds = new UserCredentials();
        } else {
            userCreds = credentials.getUserCredentials();
        }

        //Only Decode the ssoToken if it has not been done.
        if ( null == context.getSessionDef() ){
            result = decodeSsoToken(ssoToken, attrList);

            storeAttributes(attributes, attrList);

            if (result != AgentAPI.SUCCESS) {
                if (result == AgentAPI.FAILURE) {
                    logger.log(Level.WARNING, "Unable to decode the token - invalid SSO token!");
                } else {
                    logger.log(Level.FINE, "SiteMinder validate session attempt - Unable to connect to the SiteMinder Policy for decoding the SSO token." + ssoToken);
                }
                return result;
            }


            if (userCreds.name != null) {
                final String sessName = getUserIdentifier(attrList);

                if (sessName == null) {
                    logger.log(Level.WARNING, "Could not get user for session.");
                    return SiteMinderAgentConstants.SM_AGENT_API_INVALID_SESSIONID;
                }
            }

            sessionDef = createSmSessionFromAttributes( attrList );
        } else {
            sessionDef = getSiteMinderSessionDefFromContext( context );
        }

        //loginEx dose not correctly set/update the session definition
        result = agentApi.login(getClientIp(userIp, context), resCtxDef, realmDef, userCreds, sessionDef, attrList);

        storeAttributes(attributes, attrList);//Populate context variable even if apiLogin failed

        if (result != AgentAPI.YES) {
            logger.log(Level.FINE, "CA Single Sign-On authorization attempt - Unauthorized session = '" + ssoToken + "', resource '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "', result code '" + result + "'.");
            attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.SESS_DEF_REASON, getSessionDefReasonCodeAsString(sessionDef)));
            if (result == AgentAPI.NO) {//should we also check the reason code as well?
                logger.log(Level.WARNING,"Session Cookie expired!");
                //TODO: logout session?
            }
        }
        //finally, set SessionDef in the SiteMinder context. This might be useful for the authorization
        context.setSessionDef(new SiteMinderContext.SessionDef(sessionDef.reason,
                sessionDef.idleTimeout,
                sessionDef.maxTimeout,
                sessionDef.currentServerTime,
                sessionDef.sessionStartTime,
                sessionDef.sessionLastTime,
                sessionDef.id,
                sessionDef.spec));

        return result;
    }

    /**
     * creates sessionDef object from session attributes
     * @param attrList list of attributes extracted from SMSESSION cookie object
     * @return SessionDef object
     * @throws SiteMinderApiClassException
     */
    private SessionDef createSmSessionFromAttributes(final AttributeList attrList) throws SiteMinderApiClassException {
        final Enumeration en = attrList.attributes();
        final SessionDef sd = new SessionDef();//create an empty session

        while (en.hasMoreElements()) {
            Attribute att = (Attribute)en.nextElement();
            int attrId = att.id;

            if (AgentAPI.ATTR_SESSIONID == attrId) {
                //fill in the session id
                sd.id =  SiteMinderUtil.chopNull(new String(att.value));
            } else if (AgentAPI.ATTR_SESSIONSPEC == attrId) {
                //fill in the session spec
                sd.spec = SiteMinderUtil.chopNull(new String(att.value));
            }
            else if (AgentAPI.ATTR_STARTSESSIONTIME == attrId) {
                sd.sessionStartTime = SiteMinderUtil.convertAttributeValueToInt(att);
                sd.currentServerTime = sd.sessionStartTime;
            }
            else if(AgentAPI.ATTR_LASTSESSIONTIME == attrId) {
                sd.sessionLastTime = SiteMinderUtil.convertAttributeValueToInt(att);
            }
            else if(AgentAPI.ATTR_IDLESESSIONTIMEOUT == attrId) {
                sd.idleTimeout = SiteMinderUtil.convertAttributeValueToInt(att);
            }
            else if(AgentAPI.ATTR_MAXSESSIONTIMEOUT == attrId) {
                sd.maxTimeout = SiteMinderUtil.convertAttributeValueToInt(att);
            }
        }

        return sd;
    }

    private String getSessionDefReasonCodeAsString(SessionDef sd) throws SiteMinderApiClassException {
        String reasonCode = null;

        if(sd != null)
            reasonCode =  Integer.toString(sd.reason);

        return reasonCode;
    }

    private String getClientIp(String userIp, SiteMinderContext context) {
        if(agentCheckSessionIP) {
            String clientIp = userIp != null ? userIp : agentIP;
            if(context != null && userIp == null) {
                context.setSourceIpAddress(clientIp);//set source IP to client IP
            }
            return clientIp;
        }

        return null;
    }

    protected void storeAttributes(List<SiteMinderContext.Attribute> attributes, AttributeList attrs) throws SiteMinderApiClassException {
        int attrCount = attrs.getAttributeCount();

        for (int i = 0; i < attrCount; i++) {
            Attribute attr = attrs.getAttributeAt(i);
            //Cannot assume that all attribute values are character data
            logger.log(Level.FINE, "Attribute OID: " + attr.oid + " ID: " + attr.id + " Value: " + new String( attr.value ));
            switch(attr.id) {
                case HTTP_HEADER_VARIABLE_ID: // HTTP Header Variable
                    String[] info = new String( attr.value ).split("=", 2);
                    if(info[1].contains("^")) {
                        logger.log(Level.FINE, "Attribute OID: " + attr.oid + " ID: " + attr.id + " Values: " + SiteMinderUtil.hexDump(attr.value, 0, attr.value.length));
                        attributes.add(new SiteMinderContext.Attribute(info[0], info[1].split("\\^"), attr.flags, attr.id, attr.oid, attr.ttl, SiteMinderUtil.safeByteArrayCopy(attr.value)));
                    } else {
                        attributes.add(new SiteMinderContext.Attribute(info[0], info[1], attr.flags, attr.id, attr.oid, attr.ttl, SiteMinderUtil.safeByteArrayCopy(attr.value)));
                    }
                    break;
                case HTTP_SM_ONACCEPTTEXT:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_ONACCEPTTEXT));
                    break;
                case HTTP_ON_ACCEPT_REDIRECT:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_ONACCEPTREDIRECT));
                    break;
                case HTTP_COOKIE_VALUE:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_HTTP_COOKIE_VALUE));
                    break;
                case AgentAPI.ATTR_USERDN:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_USERDN));
                    break;
                case AgentAPI.ATTR_USERNAME:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_USERNAME));
                    break;
                case AgentAPI.ATTR_USERMSG:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_USERMSG));
                    break;
                case AgentAPI.ATTR_CLIENTIP:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_CLIENTIP));
                    break;
                case AgentAPI.ATTR_DEVICENAME:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_DEVICENAME));
                    break;
                case AgentAPI.ATTR_IDENTITYSPEC:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_IDENTITYSPEC));
                    break;
                case AgentAPI.ATTR_USERUNIVERSALID:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_USERUNIVERSALID));
                    break;
                case SiteMinderAgentConstants.ATTR_SESSIONAGENTDRIFT:
                    attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_SESSIONDRIFT,
                            SiteMinderUtil.safeByteArrToInt(attr.value), attr.flags, attr.id, attr.oid, attr.ttl, SiteMinderUtil.safeByteArrayCopy(attr.value)));
                    break;
                case AgentAPI.ATTR_SERVICE_DATA:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_SERVICE_DATA));
                    break;
                // Accessed via the SessionDef Object
                case AgentAPI.ATTR_SESSIONID:
                case AgentAPI.ATTR_SESSIONSPEC:
                case AgentAPI.ATTR_LASTSESSIONTIME:
                case AgentAPI.ATTR_STARTSESSIONTIME:
                case AgentAPI.ATTR_IDLESESSIONTIMEOUT:
                case AgentAPI.ATTR_MAXSESSIONTIMEOUT:
                    break;
                case AgentAPI.ATTR_STATUS_MESSAGE:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_STATUS_MESSAGE));
                    break;
                case AgentAPI.ATTR_AUTH_DIR_NAME:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_AUTH_DIR_NAME));
                    break;
                case AgentAPI.ATTR_AUTH_DIR_NAMESPACE:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_AUTH_DIR_NAMESPACE));
                    break;
                case AgentAPI.ATTR_AUTH_DIR_OID:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_AUTH_DIR_OID));
                    break;
                case AgentAPI.ATTR_AUTH_DIR_SERVER:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_AUTH_DIR_SERVER));
                    break;
                case SiteMinderAgentConstants.ATTR_DENIED_REDIRECT_CODE:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_DENIED_REDIRECT_NAME));
                    break;
                case AgentAPI.ATTR_SSOZONE:
                    attributes.add(chopNullFromValueAndCreateAttribute(attr, SiteMinderAgentConstants.ATTR_SSOZONE));
                    break;
                default:
                    attributes.add(new SiteMinderContext.Attribute("ATTR_" + Integer.toString(attr.id), attr.value, attr.flags, attr.id, attr.oid, attr.ttl, SiteMinderUtil.safeByteArrayCopy(attr.value)));
            }
        }
    }

    private SiteMinderContext.Attribute chopNullFromValueAndCreateAttribute(Attribute attr, String attributeName) {
        return new SiteMinderContext.Attribute(attributeName,
                SiteMinderUtil.chopNull(new String(attr.value)),
                attr.flags, attr.id, attr.oid, attr.ttl,
                SiteMinderUtil.safeByteArrayCopy(attr.value));
    }

    private String getUserIdentifier(final AttributeList attrList) throws SiteMinderApiClassException {
        final Enumeration en = attrList.attributes();

        String name = null;
        String dn = null;

        while (en.hasMoreElements()) {
            Attribute att = (Attribute)en.nextElement();
            int attrId = att.id;

            if (AgentAPI.ATTR_USERNAME == attrId) {
                name = new String(att.value, 0, att.value.length);
            } else if (AgentAPI.ATTR_USERDN == attrId) {
                dn = new String(att.value, 0, att.value.length);
            } // else do nothing.
        }

        if (name == null || name.trim().length() == 0) {
            name = dn;
        }

        return name;
    }

    /**
     * Calls agent API to create SSO token and checks if it was successful
     * @throws SiteMinderApiClassException
     */
    private int getSsoToken(UserCredentials userCreds, String resource, SessionDef sessionDef, AttributeList attrList, SiteMinderContext context) throws SiteMinderApiClassException {
        String token;

        StringBuffer sb = new StringBuffer();
        int retCode = agentApi.createSSOToken(sessionDef, attrList, sb);

        if(retCode == AgentAPI.SUCCESS) {
            token = sb.toString();
            logger.log(Level.FINE, "Authenticated - principal '" + userCreds.name + "'" + " resource '" + SiteMinderUtil.safeNull(resource) + "' obtained SSO token : " + token);
            context.setSsoToken(token);
            retCode = AgentAPI.YES;//set for consistency
        } else {
            logger.log(Level.FINE, "Could not obtain SSO Token - result code " + retCode);
            context.setSsoToken(null);
        }
        return retCode;
    }

    /**
     * Calls agent API to create SSO token and checks if it was successful
     * @throws SiteMinderApiClassException
     */
    protected int getSsoToken( SiteMinderContext context ) throws SiteMinderApiClassException {
        String token;

        StringBuffer sb = new StringBuffer();
        SessionDef sessionDef = getSiteMinderSessionDefFromContext(context);

        AttributeList attrList = getSiteMinderSessionDefAttrListFromContext(context);
        //When updating/reissuing a SsoToken the Attribute list must contain UserDN, UserName, ClientIP
        Attribute UserName = new Attribute( AgentAPI.ATTR_USERNAME , 0, 0, "", SiteMinderUtil.getAttrValueByName(context.getAttrList(), "ATTR_USERNAME") );
        attrList.addAttribute(UserName);
        Attribute UserDN= new Attribute( AgentAPI.ATTR_USERDN , 0, 0, "", SiteMinderUtil.getAttrValueByName(context.getAttrList(), "ATTR_USERDN") );
        attrList.addAttribute(UserDN);
        Attribute ClientIP= new Attribute( AgentAPI.ATTR_CLIENTIP , 0, 0, "", SiteMinderUtil.getAttrValueByName(context.getAttrList(), "ATTR_CLIENTIP"));
        attrList.addAttribute(ClientIP);

        final String ssoZoneName = context.getSsoZoneName();
        if (StringUtils.isNotBlank(ssoZoneName)) {
            attrList.addAttribute(new Attribute(AgentAPI.ATTR_SSOZONE, 0, 0, "", ssoZoneName.getBytes()));
        }

        logger.log(Level.FINEST, "SiteMinder Authentication - Attempt to obtain a new SSO token for " + "UserName: "+ new String( UserName.value ) +" UserDN: "+ new String( UserDN.value ));

        int retCode = agentApi.createSSOToken( sessionDef, attrList, sb );

        if(retCode == AgentAPI.SUCCESS) {
            token = sb.toString();
            logger.log(Level.FINE, "Authenticated - SessionID '" + sessionDef.id + " obtained SSO token : " + token);
            context.setSsoToken( token );
            retCode = AgentAPI.YES;//set for consistency
        } else {
            logger.log(Level.FINE, "Could not obtain SSO Token - result code " + retCode);
            context.setSsoToken( null );
        }
        return retCode;
    }


    private String getCommonErrorMessage(int errCode) {
        if(errCode == AgentAPI.NOCONNECTION) {
            return "There was no connection to the Policy Server";
        } else if(errCode == AgentAPI.INVALID_ATTRLIST) {
            return "The attribute list is invalid";
        } else if (errCode == AgentAPI.INVALID_SESSIONDEF) {
            return "The Session Definition is invalid";
        } else if(errCode == AgentAPI.INVALID_RESCTXDEF) {
            return "The Resource Context Definition is invalid.";
        } else if(errCode == AgentAPI.INVALID_REALMDEF) {
            return "The Realm Definition is invalid.";
        } else if(errCode == AgentAPI.TIMEOUT) {
            return "CA Single Sign-On Policy Server timed out.";
        } else if(errCode == AgentAPI.FAILURE) {
            return "Request to CA Single Sign-On Policy Server failed.";
        }
        else {
            return null;
        }
    }


    protected boolean getUpdateCookieStatus(){
        return this.updateCookie;
    }

    private void buildAuthenticationSchemes(SiteMinderContext context, int credentials) {
        final Set<SiteMinderContext.AuthenticationScheme> authSchemes = new HashSet<>();
        if(credentials != AgentAPI.CRED_NONE) {
            if((credentials & AgentAPI.CRED_BASIC) == AgentAPI.CRED_BASIC ){
                authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
            }
            if ((credentials & 0x40000) == 0x40000) {
                authSchemes.add(SiteMinderContext.AuthenticationScheme.JWT);
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

    public int checkUserCredentials(SiteMinderCredentials credentials, String ssoCookie) {
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

        return AgentAPI.SUCCESS;
    }

    private SiteMinderContext.Attribute fromAcoAttribute(Attribute attr) {
        String[] info = new String( attr.value ).split("=", 2);

        if (info.length == 2) {
            // Ignore commented attributes (starts with #)
            // If the commented attributes are listed in default attribute map, add them to the list with default value
            if (!info[0].startsWith("#")) {
                return new SiteMinderContext.Attribute(SiteMinderContext.Attribute.ACO_ATTRIBUTE_PREFIX + info[0], info[1],
                        attr.flags, attr.id, attr.oid, attr.ttl, SiteMinderUtil.safeByteArrayCopy(attr.value));
            } else {
                String name = SiteMinderContext.Attribute.ACO_ATTRIBUTE_PREFIX + info[0].substring(1);
                if (defaultAcoAttrMap.containsKey(name)) {
                    return new SiteMinderContext.Attribute(name, defaultAcoAttrMap.get(name),
                            attr.flags, attr.id, attr.oid, attr.ttl, SiteMinderUtil.safeByteArrayCopy(attr.value));
                }
            }
        }

        return null;
    }

    /**
     * Retrieves the ACO details from SSO policy server
     * @param smAgentName Agent name
     * @param context SSO Context
     * @return ACO details as attribute list
     */
    private AttributeList retrieveAcoDetails(String smAgentName, SiteMinderContext context) {
        AttributeList attrList = new AttributeList();
        String acoName = context.getAcoName();

        if (StringUtils.isNotEmpty(acoName)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, String.format("Retrieving %s ACO details (%s)", smAgentName, acoName));
            }

            int retCode = agentApi.getAgentConfig(acoName, attrList);
            if (retCode != AgentAPI.YES) {
                logger.log(Level.WARNING, "Failed to retrieve ACO details for " + acoName + "; error code=" + retCode);
            }
        }

        return attrList;
    }

    /**
     * Returns the ACO attributes.
     * To minimize changes (only to siteminder module alone), adding ACO attributes with ACO prefix to the existing SMCONTEXT atttrubtes list.
     * @param smAgentName Agent name
     * @param context SiteMinder Context
     * @return ACO attribute list
     */
    protected List<SiteMinderContext.Attribute> getAcoAttributes(String smAgentName, SiteMinderContext context) {
        AttributeList inAttribs = retrieveAcoDetails(smAgentName, context);
        List<SiteMinderContext.Attribute> outAttribs = new ArrayList<>();
        Enumeration<Attribute> iterator = inAttribs.attributes();

        logger.log(Level.FINE, "Available ACO attributes: " + inAttribs.getAttributeCount());

        while (iterator.hasMoreElements()) {
            Attribute attr = iterator.nextElement();
            SiteMinderContext.Attribute smAttr = fromAcoAttribute(attr);

            if (smAttr != null) {
                outAttribs.add(smAttr);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "ACO Attribute OID: " + attr.oid + " ID: " + attr.id + " Value: " + new String( attr.value ));
                }
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Ignored ACO Attribute OID: " + attr.oid + " ID: " + attr.id + " Value: " + new String( attr.value ));
                }
            }
        }

        if (inAttribs.getAttributeCount() > 0) {
            addMissingAcoAttributesWithDefaults(outAttribs);
        }

        return outAttribs;
    }

    private void addMissingAcoAttributesWithDefaults(final List<SiteMinderContext.Attribute> outAttribs) {
        final Map<String, String> missingDefaultAcoAttrMap = new HashMap<>(defaultAcoAttrMap);

        // remove the existing attributes from the missing map
        for (SiteMinderContext.Attribute attr : outAttribs) {
            if (missingDefaultAcoAttrMap.containsKey(attr.getName())) {
                missingDefaultAcoAttrMap.remove(attr.getName());
            }
        }

        // add the entries from missing map to the output attribute list
        for (Map.Entry<String, String> entry : missingDefaultAcoAttrMap.entrySet()) {
            outAttribs.add(new SiteMinderContext.Attribute(entry.getKey(), entry.getValue()));
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ACO Attribute Value: " + entry.getKey() + "=" + entry.getValue());
            }
        }
    }

}
