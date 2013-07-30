package com.ca.siteminder;

import com.ca.siteminder.util.SiteMinderUtil;
import com.l7tech.util.Pair;
import netegrity.siteminder.javaagent.*;

import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
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
    private SiteMinderConfig agentConfig;

    public SiteMinderLowLevelAgent() {
    }

    public SiteMinderLowLevelAgent(SiteMinderConfig config) throws SiteMinderApiClassException {
        agentConfig = config;
        if(!initialize()) throw new SiteMinderApiClassException("Unable to initialize SiteMinder Agent API");
    }

    private boolean initialize() throws SiteMinderApiClassException {
        initialized = false;
        try {
            agentName = agentConfig.getAgentName();
            agentIP = agentConfig.getAddress();
            agentCheckSessionIP = agentConfig.isIpcheck();
            updateCookie = agentConfig.isUpdateSSOToken();

            InitDef initDef = null;
            agentApi = new AgentAPI();
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

            int cryptoOpMode = agentConfig.getFibsmode();

            initDef.setCryptoOpMode(cryptoOpMode);
            agentApi.getConfig(initDef, agentName, null); //the last parameter is used to configure ACO

            int retcode = agentApi.init(initDef);
            //TODO: remove this code from the final version
            for(Provider prov : Security.getProviders()){
                logger.log(Level.FINE, prov.getName() + ":" + prov.getInfo());
            }

            if (retcode == AgentAPI.SUCCESS) {
                //TODO: check if the management info is correct and if we need to put it into the cluster property
                ManagementContextDef mgtCtxDef = new ManagementContextDef(ManagementContextDef.MANAGEMENT_SET_AGENT_INFO, "Product=sdk,Platform=WinNT/Solaris,Version=12.5,Update=0,Label=160");
                AttributeList attrList = new AttributeList();
                agentApi.doManagement(mgtCtxDef, attrList);
               mgtCtxDef = new ManagementContextDef(ManagementContextDef.MANAGEMENT_GET_AGENT_COMMANDS, "");//TODO: why do we call create management context for the second time? is it really necessary?
                attrList.removeAllAttributes();//TODO: this is very suspicious code why do we need to remove all attributes?
               retcode = agentApi.doManagement(mgtCtxDef, attrList);// what the hack? do management again?

                switch(retcode) {
                    case AgentAPI.NOCONNECTION:
                    case AgentAPI.TIMEOUT:
                    case AgentAPI.FAILURE:
                        logger.log(Level.SEVERE, "Unable to connect to the SiteMinder Policy Server.");
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
                if(retcode == AgentAPI.NOCONNECTION) {
                    logger.log(Level.SEVERE, "The SiteMinder Agent " + agentName + " cannot connect to the Policy Server");
                }
                else {
                    logger.log(Level.SEVERE, "The SiteMinder Agent name and/or the secret is incorrect.");
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed initialization", e);
            throw new SiteMinderApiClassException("Unable to initialize SiteMinder Agent API", e);
        }

        return initialized;
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
        ResourceContextDef resCtxDef = getSiteMinderResourceDefFromContext(context);
        RealmDef realmDef = getSiteMinderRealmDefFromContext(context);

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
     * @param transactionId
     * @param context SiteMinderContext
     * @throws java.security.AccessControlException on access control error
     */
    int authorize(String ssoToken, String userIp, String transactionId, SiteMinderContext context)
            throws SiteMinderApiClassException {

        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");
        int result = 0;

        List<Pair<String, Object>> attributes = context.getAttrList();
        ResourceContextDef resCtxDef = getSiteMinderResourceDefFromContext(context);
        RealmDef realmDef = getSiteMinderRealmDefFromContext(context);
        SessionDef sd = getSiteMinderSessionDefFromContext(context);// get SessionDef object from the context

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

    private SessionDef getSiteMinderSessionDefFromContext(SiteMinderContext context) {
        SessionDef sessionDef = new SessionDef();
        SiteMinderContext.SessionDef smContextSessionDef = context.getSessionDef();
        if(smContextSessionDef != null){
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
        ResourceContextDef resCtxDef = getSiteMinderResourceDefFromContext(context);
        RealmDef realmDef = getSiteMinderRealmDefFromContext(context);

        AttributeList attrList = new AttributeList();
        //TODO: why version is set to 0?
        //since this is a 3rd party cookie should the last parameter set to true?
        TokenDescriptor td = new TokenDescriptor(0, false);
        StringBuffer newToken = new StringBuffer();

        int result = 0; //validation does not change the token
        try {
            result = agentApi.decodeSSOToken(ssoToken, td, attrList, false, newToken);
        } catch (Exception e) {
            result =  AgentAPI.FAILURE; //this should never happen but it does
        }

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

        SessionDef sessionDef = createSmSessionFromAttributes(attrList);

        result = agentApi.loginEx(getClientIp(userIp), resCtxDef, realmDef, userCreds, sessionDef, attrList, transactionId);

        storeAttributes(attributes, attrList);//Populate context variable even if apiLogin failed

        if (result != AgentAPI.YES) {
            logger.log(Level.FINE, "SiteMinder authorization attempt - Unauthorized session = '" + ssoToken + "', resource '" + SiteMinderUtil.safeNull(resCtxDef.resource) + "', result code '" + result + "'.");
            attributes.add(new Pair<String, Object>(SiteMinderAgentConstants.SESS_DEF_REASON, getSessionDefReasonCodeAsString(sessionDef)));
            if (result == AgentAPI.NO) {//should we also check the reason code as well?
                logger.log(Level.WARNING,"Session Cookie expired!");
                //TODO: logout session?
            }
        }
        else {
            context.setSsoToken(ssoToken);
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
                sd.id =  new String(att.value);
            } else if (AgentAPI.ATTR_SESSIONSPEC == attrId) {
                //fill in the session spec
                sd.spec = new String(att.value);
            }
            else if (AgentAPI.ATTR_STARTSESSIONTIME == attrId) {
                try {
                    sd.sessionStartTime = Integer.parseInt(new String(att.value));
                    sd.currentServerTime = sd.sessionStartTime;
                } catch (NumberFormatException e) {
                    sd.sessionStartTime = -1;
                    sd.currentServerTime = -1;
                }
            }
            else if(AgentAPI.ATTR_LASTSESSIONTIME == attrId) {
                try {
                    sd.sessionLastTime = Integer.parseInt(new String(att.value));
                } catch (NumberFormatException e) {
                    sd.sessionLastTime = -1;
                }
            }
            else if(AgentAPI.ATTR_LASTSESSIONTIME == attrId) {
                try {
                    sd.sessionLastTime = Integer.parseInt(new String(att.value));
                } catch (NumberFormatException e) {
                    sd.sessionLastTime = -1;
                }
            }
            else if(AgentAPI.ATTR_IDLESESSIONTIMEOUT == attrId) {
                try {
                    sd.idleTimeout = Integer.parseInt(new String(att.value));
                } catch (NumberFormatException e) {
                    sd.idleTimeout = -1;
                }
            }
            else if(AgentAPI.ATTR_MAXSESSIONTIMEOUT == attrId) {
                try {
                    sd.maxTimeout = Integer.parseInt(new String(att.value));
                } catch (NumberFormatException e) {
                    sd.maxTimeout = -1;
                }
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
            retCode = AgentAPI.YES;//set for consistency
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
