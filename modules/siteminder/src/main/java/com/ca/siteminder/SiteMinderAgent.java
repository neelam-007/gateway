package com.ca.siteminder;

/**
 * Copyright: CA Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/17/13
 */

import com.ca.siteminder.util.SiteMinderUtil;

import javax.security.auth.Subject;
import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author ymoiseyenko
 *
 */
public class SiteMinderAgent {
    public static final String DEFAULT_SITEMINDER_COOKIE_NAME = "SMSESSION";
    public static final int HTTP_HEADER_VARIABLE_ID = 224;
    public static final String HTTP_HEADER_CONTEXT_VARIABLE_PREFIX = SiteMinderAgentConstants.VAR_PREFIX + ".";
    private static final String FIPS_MODE_COMPAT = "COMPAT";
    private static final String FIPS_MODE_MIGRATE = "MIGRATE";
    private static final String FIPS_MODE_ONLY = "ONLY";

    private final SiteMinderAgentApiClassHelper classHelper;

    private static final Logger logger = Logger.getLogger(SiteMinderAgent.class.getName());

    private boolean initialized = false;
    private Object agentApi;
    private String agentName;
    private String agentIP;
    private boolean agentCheckSessionIP;
    private String cookieName;
    private boolean updateCookie;

    public SiteMinderAgent(SiteMinderAgentConfig agentConfig, SiteMinderAgentApiClassHelper classHelper) throws SiteMinderApiClassException {
        this.classHelper = classHelper;

        initialize(agentConfig);
    }


    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    /**
     * Authenticate the principal against the resource
     *
     *
     * @param userCreds the user credential to authenticate
     * @param userIp    the ip address of the client
     * @param resource  the SiteMinder resource
     * @param resCtxDef the SiteMinder resource context definition
     * @param realmDef  the SiteMinder realm definition
     * @param transactionId
     * @param attrMap
     * @return session token
     * @throws javax.security.auth.login.FailedLoginException on failed authentication
     */
    String authenticate(Object userCreds, String userIp, String resource, Object resCtxDef, Object realmDef, String transactionId, Map<String, Object> attrMap)
            throws SiteMinderApiClassException {
        Object sessionDef = classHelper.createSessionDefClass();
        Object attrList = classHelper.createAttributeListClass();
        String clientIP = getClientIp(userIp);

        int retCode = classHelper.agentApiLoginEx(agentApi, clientIP, resCtxDef, realmDef, userCreds, sessionDef, attrList, transactionId);

        if (classHelper.getUserCredentials_name(userCreds) != null) {
            classHelper.attributeListAddAttribute(attrList,  classHelper.getAgentApi_ATTR_USERNAME(), 0,  0, null, classHelper.getUserCredentials_name(userCreds).getBytes());
        }

        if (clientIP != null) {
            classHelper.attributeListAddAttribute(attrList,  classHelper.getAgentApi_ATTR_CLIENTIP(), 0,  0, null, clientIP.getBytes());
        }

        populateContextVariables(attrMap, attrList);

        if (retCode != classHelper.getAgentApi_YES()) {
            logger.log(Level.FINE, "SiteMinder authorization attempt: User: '" + classHelper.getUserCredentials_name(userCreds) + "' Pass: '" + classHelper.getUserCredentials_password(userCreds) + "' Resource: '" + SiteMinderUtil.safeNull(resource) + "' Access Mode: '" + SiteMinderUtil.safeNull(classHelper.getResourceContextDef_action(resCtxDef)) + "'");
            attrMap.put(SiteMinderAgentConstants.VAR_SESS_DEF_REASON, getSessionDefReasonCodeAsString(sessionDef));
            return null;
        }

        logger.log(Level.FINE, "Authenticated - principal '" + classHelper.getUserCredentials_name(userCreds) + "'" + " resource '" + SiteMinderUtil.safeNull(resource) + "'");

        return getSsoToken(userCreds, resource, sessionDef, attrList);
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
    private String getSsoToken(Object userCreds, String resource, Object sessionDef, Object attrList) throws SiteMinderApiClassException {
        String token;
        StringBuffer sb = new StringBuffer();
        int retCode = classHelper.agentApiCreateSsoToken(agentApi, sessionDef, attrList, sb);

        if(retCode == classHelper.getAgentApi_SUCCESS()){
            token = sb.toString();
            logger.log(Level.FINE, "Authenticated - principal '" + classHelper.getUserCredentials_name(userCreds) + "'" + " resource '" + SiteMinderUtil.safeNull(resource) + "' obtained SSO token : " + token);
            return token;
        } else {
            logger.log(Level.FINE, "Could not obtain SSO Token - result code " + retCode);
            throw new IllegalStateException("Null SSO cookie returned for the protected resource: " + getCreateSSOTokenErrorMessage(retCode));
        }
    }

    /**
     * return agent API error message
     *
     * @param errCode
     * @return the appropriate error message
     * @throws SiteMinderApiClassException
     */
    private String getAuthorizationErrorMessage(int errCode) throws SiteMinderApiClassException {
        if(errCode == classHelper.getAgentApi_NO()) {
            return "The user is not authorized to access the resource.";
        } else if(errCode == classHelper.getAgentApi_TIMEOUT()) {
            return "The server did not respond in the specified time.";
        } else if(errCode == classHelper.getAgentApi_FAILURE()) {
            return "The operation failed.";
        }

        // if not any of the above return codes, check common failures
        return getCommonErrorMessage(errCode);
    }

    /**
     * return agent API error message
     * @param errCode
     * @return
     * @throws SiteMinderApiClassException
     */
    private String getCreateSSOTokenErrorMessage(int errCode) throws SiteMinderApiClassException {
        if(errCode == classHelper.getAgentApi_FAILURE()) {
            return "Unable to create SSO token";
        }

        // if not any of the above return codes, check common failures
        return getCommonErrorMessage(errCode);
    }

    private String getCommonErrorMessage(int errCode) throws SiteMinderApiClassException {
        if(errCode == classHelper.getAgentApi_NOCONNECTION()) {
            return "There was no connection to the Policy Server";
        } else if(errCode == classHelper.getAgentApi_INVALID_ATTRLIST()) {
            return "The attribute list is invalid";
        } else if (errCode == classHelper.getAgentApi_INVALID_SESSIONDEF()) {
            return "The Session Definition is invalid";
        } else if(errCode == classHelper.getAgentApi_INVALID_RESCTXDEF()) {
            return "The Resource Context Definition is invalid.";
        } else if(errCode == classHelper.getAgentApi_INVALID_REALMDEF()) {
            return "The Realm Definition is invalid.";
        } else {
            return null;
        }
    }

    /**
     * Authorize the session against the resource
     *
     *
     *
     * @param ssoToken  the SSO token obtained previously by
     *                  {@link SiteMinderAgent#authenticate(Object, String, String, Object, Object, String, java.util.Map)}
     * @param resource  the SiteMinder resource
     * @param resCtxDef the SiteMinder resource context definition
     * @param realmDef  the SiteMinder realm definition
     * @param transactionId
     * @throws java.security.AccessControlException on access control error
     */
    String authorize(String ssoToken, String userIp, String resource, Object resCtxDef, Object realmDef, String transactionId, Map<String, Object> attrMap)
            throws SiteMinderApiClassException {
        Object attrList = classHelper.createAttributeListClass();
        Object td = classHelper.createTokenDescriptorClass(0, false);
        StringBuffer sb = new StringBuffer();

        int result = classHelper.agentApiDecodeSsoToken(agentApi, ssoToken, td, attrList, updateCookie, sb);

        String newToken = updateCookie? sb.toString():ssoToken;

        if (result != classHelper.getAgentApi_SUCCESS()) {
            logger.log(Level.FINE, "SiteMinder authorization attempt - SiteMinder is unable to decode the token '" + SiteMinderUtil.safeNull(ssoToken) + "'");
            throw new AccessControlException("Unable to decode token " + ssoToken);
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,"Third party token? '" + classHelper.getTokenDescriptor_thirdParty(td) + "'; Version '" + classHelper.getTokenDescriptor_ver(td) + "'.");
        }

        Object sd = createSmSessionFromAttributes(attrList);
        attrList = classHelper.createAttributeListClass();
        result = classHelper.agentApiAuthorize(agentApi, getClientIp(userIp), transactionId, resCtxDef, realmDef, sd, attrList);
        //might be some other context variables that needs to be set
        populateContextVariables(attrMap, attrList);

        if (result != classHelper.getAgentApi_YES()) {
            logger.log(Level.FINE, "SiteMinder authorization attempt - Unauthorized session = '" + ssoToken + "', resource '" + SiteMinderUtil.safeNull(resource) + "', result code '" + result + "'.");
            attrMap.put(SiteMinderAgentConstants.VAR_SESS_DEF_REASON, getSessionDefReasonCodeAsString(sd));
            throw new AccessControlException("Could not authorize access: " + getAuthorizationErrorMessage(result));
        }

        logger.log(Level.FINE, "Authorized - against" + " resource '" + SiteMinderUtil.safeNull(resource) + "'new SSO token : " + newToken);

        return newToken;
    }

    private String getClientIp(String userIp) {
        return agentCheckSessionIP && userIp != null ? userIp : agentIP;
    }

    private void populateContextVariables(Map<String, Object> attrMap, Object attrs) throws SiteMinderApiClassException {
        int attrCount = classHelper.attributeListGetAttributeCount(attrs);

        for (int i = 0; i < attrCount; i++) {
            Object attr = classHelper.attributeListGetAttributeAt(attrs, i);
            String value = new String(classHelper.getAttribute_value(attr));
            logger.log(Level.FINE, "Attribute OID: " + classHelper.getAttribute_oid(attr) + " ID: " + classHelper.getAttribute_id(attr) + " Value: " + value);

            if(classHelper.getAttribute_id(attr) == HTTP_HEADER_VARIABLE_ID) { // HTTP Header Variable
                String[] info = value.split("=", 2);
                if(info[1].contains("^")) {
                    logger.log(Level.FINE, "Attribute OID: " + classHelper.getAttribute_oid(attr) + " ID: " + classHelper.getAttribute_id(attr) + " Values: " + SiteMinderUtil.hexDump(classHelper.getAttribute_value(attr), 0, classHelper.getAttribute_value(attr).length));
                    attrMap.put(HTTP_HEADER_CONTEXT_VARIABLE_PREFIX + info[0], info[1].split("\\^"));
                } else {
                    attrMap.put(HTTP_HEADER_CONTEXT_VARIABLE_PREFIX + info[0], info[1]);
                }
            }

            attrMap.put(HTTP_HEADER_CONTEXT_VARIABLE_PREFIX + classHelper.getAttribute_id(attr), value);
        }
    }

    /**
     * Validate the use session using the sessionDef extracted from the DecodeSSOToken()
     *
     *
     * @param userCreds user credential to validate
     * @param userIp    the ip address of the client
     * @param ssoToken  ssoToken which contains sessionDef after decoded
     * @param resCtxDef the SiteMinder resource context definition
     * @param realmDef  the SiteMinder realm definition
     * @param transactionId
     * @throws InvalidSessionCookieException on invalid sessionDef
     * @throws AccessControlException        on access control error due to some unknown reasons (not handled)
     */
    public int validateSession(Object userCreds, String userIp, String resource, String ssoToken, Object resCtxDef, Object realmDef, String transactionId, Map<String, Object> attrMap)
            throws SiteMinderApiClassException {
        Object attrList = classHelper.createAttributeListClass();
        Object td = classHelper.createTokenDescriptorClass(0, false);
        StringBuffer newToken = new StringBuffer();

        int result = classHelper.agentApiDecodeSsoToken(agentApi, ssoToken, td, attrList, false, newToken);

        if (result != classHelper.getAgentApi_SUCCESS()) {
            if (result == classHelper.getAgentApi_FAILURE()) {
                logger.log(Level.WARNING, "Unable to decode the token - invalid SSO token!");
            } else {
                logger.log(Level.FINE, "SiteMinder validate session attempt - Unable to connect to the SiteMinder Policy for decoding the SSO token." + ssoToken);
            }
            return result;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Third party token? '" + classHelper.getTokenDescriptor_thirdParty(td) + "'; Version '" + classHelper.getTokenDescriptor_ver(td) + "'.");
        }

        if (classHelper.getUserCredentials_name(userCreds) != null) {
            final String credName = classHelper.getUserCredentials_name(userCreds);
            final String sessName = getUserIdentifier(attrList);

            if (sessName == null) {
                logger.log(Level.WARNING, "Could not get user for session.");
                return SiteMinderAgentConstants.SM_AGENT_API_INVALID_SESSIONID;
               //TODO: do we need to check credentials of the user if sd is not null?
/*            } else if (!sameUser(credName, sessName)) {
                throw new InvalidSessionCookieException("Session user '" + sessName + "' does not match credentials in request '" + credName + "'.");
           */ }
        }

        Object sd = createSmSessionFromAttributes(attrList);

        int retCode = classHelper.agentApiLoginEx(agentApi, getClientIp(userIp), resCtxDef, realmDef, userCreds, sd, attrList, transactionId);

        populateContextVariables(attrMap, attrList);//Populate context variable even if apiLogin failed

        if (result != classHelper.getAgentApi_YES()) {
            logger.log(Level.FINE, "SiteMinder authorization attempt - Unauthorized session = '" + ssoToken + "', resource '" + SiteMinderUtil.safeNull(resource) + "', result code '" + result + "'.");
            attrMap.put(SiteMinderAgentConstants.VAR_SESS_DEF_REASON, getSessionDefReasonCodeAsString(sd));
            if (retCode == classHelper.getAgentApi_NO())
                logger.log(Level.WARNING,"Session Cookie expired, re-login performed.");

        }

        return result;
    }


    private String getSessionDefReasonCodeAsString(Object sd) throws SiteMinderApiClassException {
        String reasonCode = null;

        if(sd != null && classHelper.getSessionDef_reason(sd) != null)
            reasonCode =  Integer.toString(classHelper.getSessionDef_reason(sd));

        return reasonCode;
    }

    /**
     * Tests whether the resource is protected. This version will initialize the resCtxDef & realmDef when returned.
     *
     * @param resCtxDef the SiteMinder resource context definition to be initialized
     * @param realmDef  the SiteMinder realm definition to be initialized
     */
    boolean isProtected(String userIp, Object resCtxDef, Object realmDef)
            throws AccessControlException, SiteMinderApiClassException {
        int retCode = classHelper.agentApiIsProtected(agentApi,
                getClientIp(userIp),
                resCtxDef, realmDef);

        return retCode == classHelper.getAgentApi_YES();
    }

    /**
     * Perform authentication if required, and authorize the session against the specified resource.
     *
     * @param userIp    the client IP address
     * @param resource  the SiteMinder resource
     * @param action    the action used on the resource
     * @param ssoCookie the SiteMinder SSO Token cookie
     * @param isUseSiteMinderCookie preference of whether or not to set the SSO Token as a cookie in the response
     * @param cookieParams
     * @return the value of new (or updated) SiteMinder SSO Token cookie
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     * @throws SiteMinderApiClassException
     */
    public String authenticateAndAuthorize(Subject subject,
                                           final String userIp,
                                           final String resource,
                                           final String action,
                                           final String ssoCookie,
                                           final boolean isUseSiteMinderCookie,
                                           final Map cookieParams,
                                           final Map<String, Object> attrMap)
            throws IOException, GeneralSecurityException, SiteMinderApiClassException {
/*        if(serviceRequest == null)  {
            //should never happen!
            throw new SiteMinderApiClassException("ServiceContext object cannot be null!");
        }*/

        checkInitialized();

        // The realmDef object will contain the realm handle for the resource if the resource is protected.
        Object resCtxDef = classHelper.createResourceContextDefClass(agentName, "", resource, action);
        Object realmDef = classHelper.createRealmDefClass();

        String transactionId = UUID.randomUUID().toString();//generate SiteMinder transaction id.
        // This ID is used in all loggin/auditing and should be passed through the transactionId paramenter

        // check the requested resource/action is actually protected by SiteMinder
        boolean isProtected = isProtected(userIp, resCtxDef, realmDef);

        if (!isProtected) {
            String error = "The resource/action '" + resource + "/" + action + "' is not protected by SiteMinder. Access cannot be authorized.";
            throw new GeneralSecurityException(error);
        }

        Object userCreds = getCredentials(subject, attrMap);

        // check for some kind of credential
        if (ssoCookie == null && userCreds == null) {
            logger.log(Level.WARNING, "Credentials missing in service request.");
            return null;
        }
        else if(userCreds == null) {
            userCreds = classHelper.createUserCredentialsClass();
            logger.log(Level.FINE, "Created user credentials object");
        }

        String newSsoCookie = null;


        if (null != ssoCookie) {
            // attempt to authorize with existing cookie
            if(validateSession( userCreds, userIp, resource, ssoCookie, resCtxDef, realmDef, transactionId, attrMap) == 0);
            newSsoCookie = authorize(ssoCookie, userIp, resource, resCtxDef, realmDef, transactionId, attrMap);
        }
        else {
            // authenticate and authorize
            newSsoCookie = authenticate( userCreds, userIp, resource, resCtxDef, realmDef, transactionId, attrMap);
            if(newSsoCookie != null) {
                newSsoCookie = authorize(newSsoCookie, userIp, resource, resCtxDef, realmDef, transactionId, attrMap);
            }
            //TODO: move to SiteMinder assertion
/*                if(isUseSiteMinderCookie)
                setSessionCookie(attrMap, newSsoCookie, cookieParams);*/

        }
        //last thing, we have the cookie, now get the cookie attributes and set them in context variables.
        return newSsoCookie;
    }

    /**
     * If a certificate is present in the request, add it to the user credentials.
     *
     * @param cert
     * @param userCreds
     * @throws java.security.cert.CertificateEncodingException
     * @throws SiteMinderApiClassException
     */
    private boolean handleCertificate(X509Certificate cert, Object userCreds) throws  SiteMinderApiClassException {
        boolean success = false;
        if (cert != null) {
            sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            String base64Cert = null;
            try {
                base64Cert = encoder.encode(cert.getEncoded());
                classHelper.setUserCredentials_certBinary(userCreds, base64Cert.getBytes());
                classHelper.setUserCredentials_certIssuerDN(userCreds, cert.getIssuerDN().toString());
                classHelper.setUserCredentials_certUserDN(userCreds, cert.getSubjectDN().toString());
                success = true;
            } catch (CertificateEncodingException e) {
                logger.log(Level.WARNING, "Unable to decode a user certificate");
            }
        }
        return success;
    }

    /**
     * Return a user credentials object created from the username and password of the current AccessControlContext
     * Subject.
     *
     * @return the user credentials object, or null if no credentials exist for the AccessControlContext Subject
     * @throws SiteMinderApiClassException
     */
    private Object  getCredentials(Subject subject, Map<String, Object>attrMap) throws SiteMinderApiClassException {
        Object userCreds = null;
        boolean foundCreds = false;
        if (subject != null) {
            final Iterator principals = subject.getPrincipals().iterator();
            final Iterator credentials = subject.getPrivateCredentials(String.class).iterator();
            //TODO: figure out if we need to try multiple credentials
            if (principals.hasNext() && credentials.hasNext()) {
                Principal principal = (Principal) principals.next();
                userCreds =  classHelper.createUserCredentialsClass(principal.getName(), credentials.next().toString());
                foundCreds |= true;
            }
        }
        //if missing create one so we can use it later
        if(null == userCreds) {
            userCreds = classHelper.createUserCredentialsClass();
        }

        Object certObj = attrMap.get("javax.servlet.request.X509Certificate");
        if (certObj != null) {
            X509Certificate cert = null;

            if (certObj instanceof X509Certificate)
                cert = (X509Certificate) certObj;

            if (certObj instanceof X509Certificate[])
                cert = ((X509Certificate[]) certObj)[0];// get the first certificate?

            foundCreds = handleCertificate(cert, userCreds);
        }

        if(!foundCreds)
            return null;

        return userCreds;

    }

    private boolean initialize(SiteMinderAgentConfig agentConfig) throws SiteMinderApiClassException {
        try {
            agentConfig.validate();
            agentName = agentConfig.getAgentName();
            agentIP = agentConfig.getAgentAddress();
            agentCheckSessionIP = agentConfig.isAgentIpCheck();
            updateCookie = agentConfig.isUpdateCookie();


            Object initDef = null;
            agentApi = classHelper.createAgentApiClass();
            Iterator iter = agentConfig.getServers().iterator();

            if (iter.hasNext()) {
                if (agentConfig.isCluster()) {
                    logger.log(Level.FINE, "Initializing agent in cluster mode...");
                    initDef = classHelper.createInitDefClass(agentConfig.getHostname(), agentConfig.getAgentSecret(), agentConfig.getClusterFailOverThreshold(), iter.next());
                } else {
                    logger.log(Level.FINE, "Initializing agent in non-cluster mode...");
                    initDef = classHelper.createInitDefClass(agentConfig.getHostname(), agentConfig.getAgentSecret(), agentConfig.isNonClusterFailOver(), iter.next());
                }
            }

            // additional servers
            while(iter.hasNext()) {
                Object serverDef = iter.next();
                if (0 == classHelper.getServerDef_clusterSeq(serverDef)) {
                    classHelper.initDefAddServerDef(initDef,serverDef);
                } else {
                    // no InitDef.addServerDef(ServerDef, clusterSeq)
                    classHelper.initDefAddServerDef(initDef,
                            classHelper.getServerDef_serverIpAddress(serverDef),
                            classHelper.getServerDef_connectionMin(serverDef), classHelper.getServerDef_connectionMax(serverDef), classHelper.getServerDef_connectionStep(serverDef),
                            classHelper.getServerDef_timeout(serverDef),
                            classHelper.getServerDef_authorizationPort(serverDef), classHelper.getServerDef_authenticationPort(serverDef), classHelper.getServerDef_accountingPort(serverDef),
                            classHelper.getServerDef_clusterSeq(serverDef));
                }
            }

            String fipsMode = agentConfig.getFipsMode();
            int cryptoOpMode;

            if (FIPS_MODE_COMPAT.equals(fipsMode)) {
                cryptoOpMode = classHelper.getInitDef_CRYPTO_OP_COMPAT();
            } else if(FIPS_MODE_MIGRATE.equals(fipsMode)) {
                cryptoOpMode = classHelper.getInitDef_CRYPTO_OP_MIGRATE_F1402();
            } else if(FIPS_MODE_ONLY.equals(fipsMode)) {
                cryptoOpMode = classHelper.getInitDef_CRYPTO_OP_F1402();
            } else {
                logger.log(Level.SEVERE, "Unexpected FIPS mode: " + fipsMode);
                return false;
            }

            classHelper.initDefSetCryptoOpMode(initDef, cryptoOpMode);
            classHelper.agentApiGetConfig(agentApi, initDef, agentName, null); //the last parameter is used to configure ACO


            int retcode = classHelper.agentApiInit(agentApi, initDef);

            if (retcode != classHelper.getAgentApi_SUCCESS()) {
                logger.log(Level.SEVERE, "The SiteMinder Agent name and/or the secret is incorrect.");
                return false;
            }

            Object mgtCtxDef = classHelper.createManagementContextDefClass(classHelper.getManagementContextDef_MANAGEMENT_SET_AGENT_INFO(),
                    "Product=sdk,Platform=WinNT/Solaris,Version=4,Update=0,Label=160");
            Object attrList = classHelper.createAttributeListClass();
            classHelper.agentApiDoManagement(agentApi, mgtCtxDef, attrList);
            mgtCtxDef = classHelper.createManagementContextDefClass(classHelper.getManagementContextDef_MANAGEMENT_GET_AGENT_COMMANDS(), "");

            classHelper.attributeListRemoveAllAttributes(attrList);
            retcode = classHelper.agentApiDoManagement(agentApi,mgtCtxDef, attrList);

            if (retcode == classHelper.getAgentApi_NOCONNECTION() || retcode == classHelper.getAgentApi_TIMEOUT() || retcode == classHelper.getAgentApi_FAILURE()) {
                initialized = false;
                logger.log(Level.WARNING, "Unable to connect to the SiteMinder Policy Server.");
            } else {
                initialized = true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed initialization", e);
        }

        return initialized;
    }

    /**
     * creates sessionDef object from session attributes
     * @param attrList list of attributes extracted from SMSESSION cookie object
     * @return SessionDef object
     * @throws SiteMinderApiClassException
     */
    private Object createSmSessionFromAttributes(final Object attrList) throws SiteMinderApiClassException {
        final Enumeration en = classHelper.attributeListAttributes(attrList);
        final Object sd = classHelper.createSessionDefClass();//create an empty session

        while (en.hasMoreElements()) {
            Object att = en.nextElement();
            int attrId = classHelper.getAttribute_id(att);

            if (classHelper.getAgentApi_ATTR_SESSIONID().intValue() == attrId) {
                //fill in the session id
                classHelper.setSessionDef_id(sd, new String(classHelper.getAttribute_value(att)));
            } else if (classHelper.getAgentApi_ATTR_SESSIONSPEC().intValue() == attrId) {
                //fill in the session spec
                classHelper.setSessionDef_spec(sd, new String(classHelper.getAttribute_value(att)));
            }
            //TODO: fill in other session fields such as, idleTimeout, maxTimeout, sessionLastTime, sessionStartTime
        }

        return sd;
    }

    private String getUserIdentifier(final Object attrList) throws SiteMinderApiClassException {
        final Enumeration en = classHelper.attributeListAttributes(attrList);

        String name = null;
        String dn = null;

        while (en.hasMoreElements()) {
            Object att = en.nextElement();
            int attrId = classHelper.getAttribute_id(att);

            if (classHelper.getAgentApi_ATTR_USERNAME().intValue() == attrId) {
                name = new String(classHelper.getAttribute_value(att), 0, classHelper.getAttribute_value(att).length);
            } else if (classHelper.getAgentApi_ATTR_USERDN().intValue() == attrId) {
                dn = new String(classHelper.getAttribute_value(att), 0, classHelper.getAttribute_value(att).length);
            } // else do nothing.
        }

        if (name == null || name.trim().length() == 0) {
            name = dn;
        }

        return name;
    }

    public Map decodeCookieAttributes(String cookie) throws SiteMinderApiClassException {
        HashMap map = new HashMap();
        Object attrList = classHelper.createAttributeListClass();
        Object td = classHelper.createTokenDescriptorClass(0, false);
        StringBuffer newToken = new StringBuffer();

        int result = classHelper.agentApiDecodeSsoToken(agentApi, cookie, td, attrList, false, newToken);

        if (result != classHelper.getAgentApi_SUCCESS()) {
            logger.log(Level.WARNING, "The SiteMinder SSO cookie could not be decoded and the attributes will not be available in policy.");
        } else {
            //get the attributes put them in the map.
            Enumeration e = classHelper.attributeListAttributes(attrList);

            if(e!=null) {
                while(e.hasMoreElements()) {
                    Object a = e.nextElement();

                    if(a!=null){
                        int attrId = classHelper.getAttribute_id(a);

                        if (classHelper.getAgentApi_ATTR_USERDN().intValue() == attrId) {
                            map.put("ATTR_USERDN", new String(classHelper.getAttribute_value(a)).trim());
                        } else if (classHelper.getAgentApi_ATTR_SESSIONSPEC().intValue() == attrId) {
                            map.put("ATTR_SESSIONSPEC", new String(classHelper.getAttribute_value(a)).trim());
                        } else if (classHelper.getAgentApi_ATTR_SESSIONID().intValue() == attrId) {
                            map.put("ATTR_SESSIONID", new String(classHelper.getAttribute_value(a)).trim());
                        } else if (classHelper.getAgentApi_ATTR_USERNAME().intValue() == attrId) {
                            map.put("ATTR_USERNAME", new String(classHelper.getAttribute_value(a)).trim());
                        } else if (classHelper.getAgentApi_ATTR_CLIENTIP().intValue() == attrId) {
                            map.put("ATTR_CLIENTIP", new String(classHelper.getAttribute_value(a)).trim());
                        } else if (classHelper.getAgentApi_ATTR_DEVICENAME().intValue() == attrId) {
                            map.put("ATTR_DEVICENAME", new String(classHelper.getAttribute_value(a)).trim());
                        } else if (classHelper.getAgentApi_ATTR_IDLESESSIONTIMEOUT().intValue() == attrId) {
                            map.put("ATTR_IDLESESSIONTIMEOUT", new String(classHelper.getAttribute_value(a)).trim());
                        } else if (classHelper.getAgentApi_ATTR_MAXSESSIONTIMEOUT().intValue() == attrId) {
                            map.put("ATTR_MAXSESSIONTIMEOUT", new String(classHelper.getAttribute_value(a)).trim());
                        } else if (classHelper.getAgentApi_ATTR_STARTSESSIONTIME().intValue() == attrId) {
                            map.put("ATTR_STARTSESSIONTIME", new String(classHelper.getAttribute_value(a)).trim());
                        } else if (classHelper.getAgentApi_ATTR_LASTSESSIONTIME().intValue() == attrId) {
                            map.put("ATTR_LASTSESSIONTIME", new String(classHelper.getAttribute_value(a)).trim());
                        } else if(classHelper.getAgentApi_ATTR_USERMSG().intValue() == attrId) {
                            map.put("ATTR_USERMSG", new String(classHelper.getAttribute_value(a)).trim());
                        }// else do nothing
                    }
                }
            }
        }

        return map;
    }

    /**
     * Check that the user identity is consistent
     *
     * @param inUser      The incoming user name
     * @param sessionUser The session user name (could be a DN or full name)
     */
    private boolean sameUser(final String inUser, final String sessionUser) {
        boolean match = false;

        if (sessionUser != null) {
            if (sessionUser.contains("=") && sessionUser.contains(",")) {
                try {
                    String sessionMsav = SiteMinderUtil.getMostSpecificAttributeValue(sessionUser);
                    match = sessionMsav.equalsIgnoreCase(inUser);
                }
                catch (ParseException pe) {
                    logger.log(Level.WARNING, "Unable to parse LDAP name '" + sessionUser + "', error is '" + pe.getMessage() + "' at position" + pe.getErrorOffset() + ".");
                }
            } else {
                match = sessionUser.equals(inUser);
            }
        }

        return match;
    }

    public void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("SiteMinder Agent initialization failed");
        }
    }


}
