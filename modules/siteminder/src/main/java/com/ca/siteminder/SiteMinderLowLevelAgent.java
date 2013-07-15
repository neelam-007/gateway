package com.ca.siteminder;

import com.ca.siteminder.util.SiteMinderUtil;
import com.l7tech.util.Pair;
import netegrity.siteminder.javaagent.*;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/26/13
 */
public class SiteMinderLowLevelAgent {
    public static final String DEFAULT_SITEMINDER_COOKIE_NAME = "SMSESSION";
    public static final int HTTP_HEADER_VARIABLE_ID = 224;
    public static final String HTTP_HEADER_CONTEXT_VARIABLE_PREFIX = SiteMinderAgentConstants.VAR_PREFIX + ".";
    private static final String FIPS_MODE_COMPAT = "COMPAT";
    private static final String FIPS_MODE_MIGRATE = "MIGRATE";
    private static final String FIPS_MODE_ONLY = "ONLY";


    private static final Logger logger = Logger.getLogger(SiteMinderLowLevelAgent.class.getName());

    private boolean initialized = false;

    private AgentAPI agentApi;
    private String agentName;
    private String agentIP;
    private boolean agentCheckSessionIP;
    private String cookieName;
    private boolean updateCookie;
    private final SiteMinderAgentConfig agentConfig;


    public SiteMinderLowLevelAgent(SiteMinderAgentConfig config) throws SiteMinderApiClassException {
        this.agentConfig = config;
        initialize(agentConfig);
    }

    private int initialize(SiteMinderAgentConfig agentConfig) throws SiteMinderApiClassException {
        int retcode = 0;
        try {
            agentConfig.validate();
            agentName = agentConfig.getAgentName();
            agentIP = agentConfig.getAgentAddress();
            agentCheckSessionIP = agentConfig.isAgentIpCheck();
            updateCookie = agentConfig.isUpdateCookie();



            InitDef initDef = null;
            agentApi = new AgentAPI();
            Iterator iter = agentConfig.getServers().iterator();

            if (iter.hasNext()) {
                if (agentConfig.isCluster()) {
                    logger.log(Level.FINE, "Initializing agent in cluster mode...");
                    initDef = new InitDef(agentConfig.getHostname(), agentConfig.getAgentSecret(), agentConfig.getClusterFailOverThreshold(),(ServerDef)iter.next());
                } else {
                    logger.log(Level.FINE, "Initializing agent in non-cluster mode...");
                    initDef = new InitDef(agentConfig.getHostname(), agentConfig.getAgentSecret(), agentConfig.isNonClusterFailOver(), (ServerDef)iter.next());
                }
            }

            // additional servers
            while(iter.hasNext()) {
                ServerDef serverDef = (ServerDef)iter.next();
                if (0 == serverDef.clusterSeq) {
                    initDef.addServerDef(serverDef);
                    //classHelper.initDefAddServerDef(initDef,serverDef);
                } else {
                    // no InitDef.addServerDef(ServerDef, clusterSeq)
                    initDef.addServerDef(serverDef.serverIpAddress,
                            serverDef.connectionMin,
                            serverDef.connectionMax,
                            serverDef.connectionStep,
                            serverDef.timeout,
                            serverDef.authorizationPort,
                            serverDef.authenticationPort,
                            serverDef.accountingPort,
                            serverDef.clusterSeq);

/*                    classHelper.initDefAddServerDef(initDef,
                            classHelper.getServerDef_serverIpAddress(serverDef),
                            classHelper.getServerDef_connectionMin(serverDef), classHelper.getServerDef_connectionMax(serverDef), classHelper.getServerDef_connectionStep(serverDef),
                            classHelper.getServerDef_timeout(serverDef),
                            classHelper.getServerDef_authorizationPort(serverDef), classHelper.getServerDef_authenticationPort(serverDef), classHelper.getServerDef_accountingPort(serverDef),
                            classHelper.getServerDef_clusterSeq(serverDef));*/
                }
            }

            String fipsMode = agentConfig.getFipsMode();
            int cryptoOpMode;

            if (FIPS_MODE_COMPAT.equals(fipsMode)) {
                cryptoOpMode = InitDef.CRYPTO_OP_COMPAT;
//                cryptoOpMode = classHelper.getInitDef_CRYPTO_OP_COMPAT();
            } else if(FIPS_MODE_MIGRATE.equals(fipsMode)) {
                cryptoOpMode = InitDef.CRYPTO_OP_MIGRATE_F1402;
//                cryptoOpMode = classHelper.getInitDef_CRYPTO_OP_MIGRATE_F1402();
            } else if(FIPS_MODE_ONLY.equals(fipsMode)) {
                cryptoOpMode = InitDef.CRYPTO_OP_F1402;
//                cryptoOpMode = classHelper.getInitDef_CRYPTO_OP_F1402();
            } else {
                logger.log(Level.SEVERE, "Unexpected FIPS mode: " + fipsMode);
                return AgentAPI.FAILURE;
            }
            initDef.setCryptoOpMode(cryptoOpMode);
//            classHelper.initDefSetCryptoOpMode(initDef, cryptoOpMode);
            agentApi.getConfig(initDef, agentName, null); //the last parameter is used to configure ACO
//            classHelper.agentApiGetConfig(agentApi, initDef, agentName, null); //the last parameter is used to configure ACO

            retcode = agentApi.init(initDef);
//            int retcode = classHelper.agentApiInit(agentApi, initDef);

//            if (retcode != classHelper.getAgentApi_SUCCESS()) {
            if (retcode != AgentAPI.SUCCESS) {
                if(retcode == AgentAPI.NOCONNECTION) {
                    logger.log(Level.SEVERE, "The SiteMinder Agent " + agentName + " cannot connect to the Policy Server");
                }
                else {
                    logger.log(Level.SEVERE, "The SiteMinder Agent name and/or the secret is incorrect.");
                }
                return retcode;
            }
            //TODO: check if the management info is correct and if we need to put it into the cluster property
            ManagementContextDef mgtCtxDef = new ManagementContextDef(ManagementContextDef.MANAGEMENT_SET_AGENT_INFO, "Product=sdk,Platform=WinNT/Solaris,Version=12.5,Update=0,Label=160");
//            Object mgtCtxDef = classHelper.createManagementContextDefClass(classHelper.getManagementContextDef_MANAGEMENT_SET_AGENT_INFO(),
//                    "Product=sdk,Platform=WinNT/Solaris,Version=4,Update=0,Label=160");
            AttributeList attrList = new AttributeList();
//            Object attrList = classHelper.createAttributeListClass();
            agentApi.doManagement(mgtCtxDef, attrList);
//            classHelper.agentApiDoManagement(agentApi, mgtCtxDef, attrList);
            mgtCtxDef = new ManagementContextDef(ManagementContextDef.MANAGEMENT_GET_AGENT_COMMANDS, "");//TODO: why do we call create management context for the second time? is it really necessary?
//            mgtCtxDef = classHelper.createManagementContextDefClass(classHelper.getManagementContextDef_MANAGEMENT_GET_AGENT_COMMANDS(), "");
            attrList.removeAllAttributes();//TODO: this is very suspicious code why do we need to remove all attributes?
//            classHelper.attributeListRemoveAllAttributes(attrList);
            retcode = agentApi.doManagement(mgtCtxDef, attrList);// what the hack? do management again?
//            retcode = classHelper.agentApiDoManagement(agentApi,mgtCtxDef, attrList);

            switch(retcode) {
                case AgentAPI.NOCONNECTION:
                case AgentAPI.TIMEOUT:
                case AgentAPI.FAILURE:
                    logger.log(Level.WARNING, "Unable to connect to the SiteMinder Policy Server.");
                    initialized = false;
                    break;
                case AgentAPI.INVALID_MGMTCTXDEF:
                    logger.log(Level.WARNING, "Management Context is invalid");
                    initialized = false;
                    break;
                case AgentAPI.INVALID_ATTRLIST:
                    throw new SiteMinderApiClassException("Attribute List is invalid"); //this should never happen!
                default:
                    initialized = true;

            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed initialization", e);
        }

        return retcode;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Authenticate the principal against the resource
     * @param userCreds the user credential to authenticate
     * @param userIp    the ip address of the client
     * @param transactionId
     * @param context SiteMinderContext object
     * @throws javax.security.auth.login.FailedLoginException on failed authentication
     */
    int authenticate(UserCredentials userCreds, String userIp, String transactionId, SiteMinderContext context)
            throws SiteMinderApiClassException {

        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");
        List<Pair<String, Object>> attributes = context.getAttrList();
        ResourceContextDef resCtxDef = context.getResContextDef();
        RealmDef realmDef = context.getRealmDef();
        SessionDef sessionDef = new SessionDef();
        AttributeList attrList = new AttributeList();
        String clientIP = getClientIp(userIp);

        int retCode = agentApi.loginEx(clientIP, resCtxDef, realmDef, userCreds, sessionDef, attrList, transactionId);

        if (userCreds.name != null) {
            attrList.addAttribute(AgentAPI.ATTR_USERNAME, 0, 0, null, userCreds.name.getBytes());
        }

        if (clientIP != null) {
            attrList.addAttribute(AgentAPI.ATTR_CLIENTIP, 0,  0, null, clientIP.getBytes());
        }

        storeAttributes(attributes, attrList);


        if (retCode != AgentAPI.YES) {
            logger.log(Level.FINE, "SiteMinder authorization attempt: User: '" + userCreds.name + "' Resource: '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "' Access Mode: '" + SiteMinderUtil.safeNull(resCtxDef.action) + "'");
            attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.SESS_DEF_REASON, getSessionDefReasonCodeAsString(sessionDef)));

        }
        else {
            logger.log(Level.FINE, "Authenticated - principal '" + userCreds.name + "'" + " resource '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "'");
            retCode = getSsoToken(userCreds, resCtxDef.resource, sessionDef, attrList, context);
        }

        //finally, set SessionDef in the SiteMinder context. This might be useful for the authorization
        context.setSessionDef(sessionDef);

        return retCode;
    }

    /**
     * Authorize the session against the resource
     *
     *
     *
     *
     * @param ssoToken  the SSO token obtained previously by
     *                  {@link com.ca.siteminder.SiteMinderAgent#authenticate(Object, String, String, Object, Object, String, java.util.Map)}
     * @param transactionId
     * @param context SiteMinderContext
     * @throws java.security.AccessControlException on access control error
     */
    int authorize(String ssoToken, String userIp, String transactionId, SiteMinderContext context)
            throws SiteMinderApiClassException {

        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");
        int result = 0;

        List<Pair<String, Object>> attributes = context.getAttrList();
        ResourceContextDef resCtxDef = context.getResContextDef();
        RealmDef realmDef = context.getRealmDef();
        SessionDef sd = context.getSessionDef();// get SessionDef object from the context

        if(ssoToken != null) {
            AttributeList attrList = new AttributeList();

            result = decodeSsoToken(ssoToken, context, attrList);

            if (result != AgentAPI.SUCCESS) {
                logger.log(Level.FINE, "SiteMinder authorization attempt - SiteMinder is unable to decode the token '" + SiteMinderUtil.safeNull(ssoToken) + "'");
                return result;
            }

            sd = createSmSessionFromAttributes(attrList);
        }

        AttributeList attributeList = new AttributeList();
        //TODO: use authorizeEx and return a list of SM variables for processing
        result = agentApi.authorize(getClientIp(userIp), transactionId, resCtxDef, realmDef, sd, attributeList);
        //might be some other context variables that needs to be set
        storeAttributes(attributes, attributeList);

        if (result != AgentAPI.YES) {
            logger.log(Level.FINE, "SiteMinder authorization attempt - Unauthorized session = '" + ssoToken + "', resource '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "', result code '" + result + "'.");
            attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.SESS_DEF_REASON, getSessionDefReasonCodeAsString(sd)));
            return result;
        }

        logger.log(Level.FINE, "Authorized - against" + " resource '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "'SSO token : " + context.getSsoToken());

        return result;
    }

    private int decodeSsoToken(String ssoToken, SiteMinderContext context, AttributeList attrList) {
        //TODO: why version is set to 0?
        //since this is a 3rd party cookie should the last parameter set to true?
        TokenDescriptor td = new TokenDescriptor(0, false);
        StringBuffer sb = new StringBuffer();

        int result = agentApi.decodeSSOToken(ssoToken, td, attrList, updateCookie, sb);

        if (result ==  AgentAPI.SUCCESS) {
            logger.log(Level.FINE,"Third party token? '" + td.bThirdParty + "'; Version '" + td.ver + "'.");
            if(updateCookie) {
                context.setSsoToken(sb.toString());//set only if the token is successfully decoded
            }
        }

        return result;
    }

    /**
     * Tests whether the resource is protected. This version will initialize the resCtxDef & realmDef when returned.
     *
     * @param resCtxDef the SiteMinder resource context definition to be initialized
     * @param realmDef  the SiteMinder realm definition to be initialized
     */
    boolean isProtected(String userIp, ResourceContextDef resCtxDef, RealmDef realmDef, String transactionId) throws SiteMinderApiClassException {
        int retCode = agentApi.isProtectedEx(getClientIp(userIp), resCtxDef, realmDef, transactionId);
        if(retCode != AgentAPI.NO && retCode != AgentAPI.YES){
            throw new SiteMinderApiClassException(getCommonErrorMessage(retCode));
        }

        return retCode == AgentAPI.YES;
    }

    /**
     * Validate the use session using the sessionDef extracted from the DecodeSSOToken()
     *
     *
     *
     *
     * @param userCreds user credential to validate
     * @param userIp    the ip address of the client
     * @param ssoToken  ssoToken which contains sessionDef after decoded
     * @param transactionId
     * @param context
     * @throws SiteMinderApiClassException on invalid sessionDef

     */
    public int validateSession(UserCredentials userCreds, String userIp, String ssoToken, String transactionId, SiteMinderContext context)
            throws SiteMinderApiClassException {
        //check if the context is null and throw exception
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");
        List<Pair<String, Object>> attributes = context.getAttrList();
        ResourceContextDef resCtxDef = context.getResContextDef();
        RealmDef realmDef = context.getRealmDef();

        AttributeList attrList = new AttributeList();
        //TODO: why version is set to 0?
        //since this is a 3rd party cookie should the last parameter set to true?
        TokenDescriptor td = new TokenDescriptor(0, false);
        StringBuffer newToken = new StringBuffer();

        int result = agentApi.decodeSSOToken(ssoToken, td, attrList, false, newToken); //validation does not change the token

        if (result != AgentAPI.SUCCESS) {
            if (result == AgentAPI.FAILURE) {
                logger.log(Level.WARNING, "Unable to decode the token - invalid SSO token!");
            } else {
                logger.log(Level.FINE, "SiteMinder validate session attempt - Unable to connect to the SiteMinder Policy for decoding the SSO token." + ssoToken);
            }
            return result;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Third party token? '" + td.bThirdParty + "'; Version '" + td.ver + "'.");
        }

        if (userCreds.name != null) {
            final String credName = userCreds.name;
            final String sessName = getUserIdentifier(attrList);

            if (sessName == null) {
                logger.log(Level.WARNING, "Could not get user for session.");
                return SiteMinderAgentConstants.SM_AGENT_API_INVALID_SESSIONID;
                //TODO: do we need to check credentials of the user if sd is not null?
/*            } else if (!sameUser(credName, sessName)) {
                throw new InvalidSessionCookieException("Session user '" + sessName + "' does not match credentials in request '" + credName + "'.");
           */ }
        }

        SessionDef sd = createSmSessionFromAttributes(attrList);

        result = agentApi.loginEx(getClientIp(userIp), resCtxDef, realmDef, userCreds, sd, attrList, transactionId);

        storeAttributes(attributes, attrList);//Populate context variable even if apiLogin failed

        if (result != AgentAPI.YES) {
            logger.log(Level.FINE, "SiteMinder authorization attempt - Unauthorized session = '" + ssoToken + "', resource '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "', result code '" + result + "'.");
            //TODO: change to detouch from context variables
            attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.SESS_DEF_REASON, getSessionDefReasonCodeAsString(sd)));
            if (result == AgentAPI.NO)
                logger.log(Level.WARNING,"Session Cookie expired!");

        }

        return result;
    }

    public SiteMinderAgentConfig getConfig() {
        return agentConfig;
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
                sd.id =  new String(att.value);
            } else if (AgentAPI.ATTR_SESSIONSPEC == attrId) {
                //fill in the session spec
                sd.spec = new String(att.value);
            }
            //TODO: fill in other session fields such as, idleTimeout, maxTimeout, sessionLastTime, sessionStartTime
        }

        return sd;
    }

    private String getSessionDefReasonCodeAsString(SessionDef sd) throws SiteMinderApiClassException {
        String reasonCode = null;

        if(sd != null)
            reasonCode =  Integer.toString(sd.reason);

        return reasonCode;
    }

    private String getClientIp(String userIp) {
        return agentCheckSessionIP && userIp != null ? userIp : agentIP;
    }


    private void storeAttributes(List<Pair<String, Object>> attributes, AttributeList attrs) throws SiteMinderApiClassException {
        int attrCount = attrs.getAttributeCount();

        for (int i = 0; i < attrCount; i++) {
            Attribute attr = attrs.getAttributeAt(i);
            String value = new String(attr.value);
            logger.log(Level.FINE, "Attribute OID: " + attr.oid + " ID: " + attr.id + " Value: " + value);
            switch(attr.id) {
                case HTTP_HEADER_VARIABLE_ID: // HTTP Header Variable
                    String[] info = value.split("=", 2);
                    if(info[1].contains("^")) {
                        logger.log(Level.FINE, "Attribute OID: " + attr.oid + " ID: " + attr.id + " Values: " + SiteMinderUtil.hexDump(attr.value, 0, attr.value.length));
                        attributes.add(new Pair<String, Object>(info[0], info[1].split("\\^")));
                    } else {
                        attributes.add(new Pair<String, Object>(info[0], info[1]));
                    }
                    break;
                case AgentAPI.ATTR_USERDN:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_USERDN, value));
                    break;
                case AgentAPI.ATTR_USERNAME:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_USERNAME, value));
                    break;
                case AgentAPI.ATTR_USERMSG:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_USERMSG, value));
                    break;
                case AgentAPI.ATTR_CLIENTIP:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_CLIENTIP, value));
                    break;
                case AgentAPI.ATTR_DEVICENAME:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_DEVICENAME, value));
                    break;
                case AgentAPI.ATTR_IDENTITYSPEC:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_IDENTITYSPEC, value));
                    break;
                case AgentAPI.ATTR_USERUNIVERSALID:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_USERUNIVERSALID, value));
                    break;
                case AgentAPI.ATTR_SESSIONID:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_SESSIONID, value));
                    break;
                case AgentAPI.ATTR_SESSIONSPEC:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_SESSIONSPEC, value));
                    break;
                case AgentAPI.ATTR_SERVICE_DATA:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_SERVICE_DATA, value));
                    break;
                case AgentAPI.ATTR_LASTSESSIONTIME:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_LASTSESSIONTIME, value));
                    break;
                case AgentAPI.ATTR_MAXSESSIONTIMEOUT:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_MAXSESSIONTIMEOUT, value));
                    break;
                case AgentAPI.ATTR_STATUS_MESSAGE:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_STATUS_MESSAGE, value));
                    break;
                case AgentAPI.ATTR_AUTH_DIR_NAME:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_AUTH_DIR_NAME, value));
                    break;
                case AgentAPI.ATTR_AUTH_DIR_NAMESPACE:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_AUTH_DIR_NAMESPACE, value));
                    break;
                case AgentAPI.ATTR_AUTH_DIR_OID:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_AUTH_DIR_OID, value));
                    break;
                case AgentAPI.ATTR_AUTH_DIR_SERVER:
                    attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_AUTH_DIR_SERVER, value));
                    break;
                default:
                    attributes.add(new Pair<String, Object>("ATTR_" + Integer.toString(attr.id), value));

            }

        }
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
     * @param userCreds
     * @param resource
     * @param sessionDef
     * @param attrList
     * @return
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
        } else {
            logger.log(Level.FINE, "Could not obtain SSO Token - result code " + retCode);
            context.setSsoToken(null);
        }
        return retCode;
    }

    /**
     * return agent API error message
     * @param errCode
     * @return
     * @throws SiteMinderApiClassException
     */
    private String getCreateSSOTokenErrorMessage(int errCode) throws SiteMinderApiClassException {
        if(errCode == AgentAPI.FAILURE) {
            return "Unable to create SSO token";
        }

        // if not any of the above return codes, check common failures
        return getCommonErrorMessage(errCode);
    }

    private String getCommonErrorMessage(int errCode) throws SiteMinderApiClassException {
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
            return "SiteMinder Policy Server timed out.";
        } else if(errCode == AgentAPI.FAILURE) {
            return "Request to SiteMinder Policy Server failed.";
        }
        else {
            return null;
        }
    }

    public String getName() {
        return agentName;
    }

    public String getAgentIp() {
        return agentIP;
    }
}
