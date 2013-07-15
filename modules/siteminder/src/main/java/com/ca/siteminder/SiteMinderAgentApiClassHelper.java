package com.ca.siteminder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/17/13
 */
public class SiteMinderAgentApiClassHelper {
    private static final Logger logger = Logger.getLogger(SiteMinderAgentApiClassHelper.class.getName());

    private boolean isInitialized = false;

    private ClassLoader classLoader;

    private Class serverDefClass;
    private Constructor serverDefConstructor;
    private Field serverDef_serverIpAddress;
    private Field serverDef_connectionMin;
    private Field serverDef_connectionMax;
    private Field serverDef_connectionStep;
    private Field serverDef_timeout;
    private Field serverDef_authorizationPort;
    private Field serverDef_authenticationPort;
    private Field serverDef_accountingPort;
    private Field serverDef_clusterSeq;

    private Class resourceContextDefClass;
    private Constructor resourceContextDefConstructor;
    private Field resourceContextDef_action;

    private Class realmDefClass;
    private Constructor realmDefConstructor;

    private Class userCredentialsClass;
    private Constructor userCredentialsDefaultConstructor;
    private Constructor userCredentialsCustomConstructor;
    private Field userCredentials_reason;
    private Field userCredentials_name;
    private Field userCredentials_password;
    private Field userCredentials_certUserDN;
    private Field userCredentials_certIssuerDN;
    private Field userCredentials_certBinary;

    private Class sessionDefClass;
    private Constructor sessionDefConstructor;
    private Field sessionDef_reason;
    private Field sessionDef_id;
    private Field sessionDef_spec;

    private Class attributeListClass;
    private Constructor attributeListConstructor;
    private Method attributeListAttributesMethod;
    private Method attributeListRemoveAllAttributesMethod;
    private Method attributeListGetAttributeAtMethod;
    private Method attributeListGetAttributeCountMethod;
    private Method attributeListAddAttributeMethod;

    private Class attributeClass;
    private Field attribute_id;
    private Field attribute_ttl;
    private Field attribute_flags;
    private Field attribute_oid;
    private Field attribute_value;

    private Class tokenDescriptorClass;
    private Constructor tokenDescriptorConstructor;
    private Field tokenDescriptor_ver;
    private Field tokenDescriptor_bThirdParty;

    private Class initDefClass;
    private Constructor initDefDefaultConstructor;
    private Constructor initDefNonClusteredConstructor;
    private Constructor initDefClusteredConstructor;
    private Integer initDef_CRYPTO_OP_UNSET ;
    private Integer initDef_CRYPTO_OP_COMPAT;
    private Integer initDef_CRYPTO_OP_MIGRATE_F1402;
    private Integer initDef_CRYPTO_OP_F1402;
    private Method initDefAddServerDefNonClusteredMethod;
    private Method initDefAddServerDefClusteredMethod;
    private Method initDefSetCryptoOpModeMethod;

    private Class managementContextDefClass;
    private Constructor managementContextDefConstructor;
    private Integer managementContextDef_MANAGEMENT_GET_AGENT_COMMANDS;
    private Integer managementContextDef_MANAGEMENT_SET_AGENT_INFO;

    private Class agentApiClass;
    private Constructor agentApiConstructor;
    private Integer agentApi_YES;
    private Integer agentApi_NO;
    private Integer agentApi_SUCCESS;
    private Integer agentApi_FAILURE;
    private Integer agentApi_NOCONNECTION;
    private Integer agentApi_INVALID_SESSIONDEF;
    private Integer agentApi_INVALID_ATTRLIST;
    private Integer agentApi_INVALID_RESCTXDEF;
    private Integer agentApi_INVALID_REALMDEF;
    private Integer agentApi_TIMEOUT;
    private Integer agentApi_ATTR_USERDN;
    private Integer agentApi_ATTR_SESSIONSPEC;
    private Integer agentApi_ATTR_SESSIONID;
    private Integer agentApi_ATTR_USERNAME;
    private Integer agentApi_ATTR_USERMSG;
    private Integer agentApi_ATTR_CLIENTIP;
    private Integer agentApi_ATTR_DEVICENAME;
    private Integer agentApi_ATTR_IDLESESSIONTIMEOUT;
    private Integer agentApi_ATTR_MAXSESSIONTIMEOUT;
    private Integer agentApi_ATTR_STARTSESSIONTIME;
    private Integer agentApi_ATTR_LASTSESSIONTIME;
    private Method agentApiLoginMethod;
    private Method agentApiLoginExMethod;
    private Method agentApiCreateSsoTokenMethod;
    private Method agentApiDecodeSsoTokenMethod;
    private Method agentApiAuthorizeMethod;
    private Method agentApiAuthorizeExMethod;
    private Method agentApiIsProtectedMethod;
    private Method agentApiGetConfigMethod;
    private Method agentApiInitMethod;
    private Method agentApiDoManagementMethod;

    public SiteMinderAgentApiClassHelper(boolean test) {
         if(test) {
             classLoader = new SiteMinderAgentApiClassLoader(SiteMinderAgentApiClassHelper.class.getClassLoader().getParent());
         }
        else {
             classLoader = new SiteMinderAgentApiClassLoader(
                     SiteMinderAgentApiClassHelper.class.getClassLoader().getParent().getParent(),
                     new String[] {
                             "com.ca.siteminder"
                     }
             );

         }

        initialize();
        logger.log(Level.FINE, "SiteMinderClassHelper initialized");
    }



    void initialize() {
        try {
            serverDefClass = Class.forName("netegrity.siteminder.javaagent.ServerDef", true, classLoader);
            serverDefConstructor = serverDefClass.getConstructor();
            serverDef_clusterSeq = serverDefClass.getField("clusterSeq");
            serverDef_serverIpAddress = serverDefClass.getField("serverIpAddress");
            serverDef_connectionMin = serverDefClass.getField("connectionMin");
            serverDef_connectionMax = serverDefClass.getField("connectionMax");
            serverDef_connectionStep = serverDefClass.getField("connectionStep");
            serverDef_timeout = serverDefClass.getField("timeout");
            serverDef_authorizationPort = serverDefClass.getField("authorizationPort");
            serverDef_authenticationPort = serverDefClass.getField("authenticationPort");
            serverDef_accountingPort = serverDefClass.getField("accountingPort");
            serverDef_clusterSeq = serverDefClass.getField("clusterSeq");

            resourceContextDefClass = Class.forName("netegrity.siteminder.javaagent.ResourceContextDef", true, classLoader);
            resourceContextDefConstructor = resourceContextDefClass.getConstructor(String.class, String.class, String.class, String.class);
            resourceContextDef_action = resourceContextDefClass.getField("action");

            realmDefClass = Class.forName("netegrity.siteminder.javaagent.RealmDef", true, classLoader);
            realmDefConstructor = realmDefClass.getConstructor();

            userCredentialsClass = Class.forName("netegrity.siteminder.javaagent.UserCredentials", true, classLoader);
            userCredentialsDefaultConstructor = userCredentialsClass.getConstructor();
            userCredentialsCustomConstructor = userCredentialsClass.getConstructor(String.class, String.class);
            userCredentials_reason = userCredentialsClass.getField("reason");
            userCredentials_name = userCredentialsClass.getField("name");
            userCredentials_password = userCredentialsClass.getField("password");
            userCredentials_certUserDN = userCredentialsClass.getField("certUserDN");
            userCredentials_certIssuerDN = userCredentialsClass.getField("certIssuerDN");
            userCredentials_certBinary = userCredentialsClass.getField("certBinary");

            sessionDefClass = Class.forName("netegrity.siteminder.javaagent.SessionDef", true, classLoader);
            sessionDefConstructor = sessionDefClass.getConstructor();
            sessionDef_reason = sessionDefClass.getField("reason");
            sessionDef_id = sessionDefClass.getField("id");
            sessionDef_spec = sessionDefClass.getField("spec");

            attributeClass = Class.forName("netegrity.siteminder.javaagent.Attribute", true, classLoader);
            attribute_id = attributeClass.getField("id");
            attribute_ttl = attributeClass.getField("ttl");
            attribute_flags = attributeClass.getField("flags");
            attribute_oid = attributeClass.getField("oid");
            attribute_value = attributeClass.getField("value");

            attributeListClass = Class.forName("netegrity.siteminder.javaagent.AttributeList", true, classLoader);
            attributeListConstructor = attributeListClass.getConstructor();
            attributeListAttributesMethod = attributeListClass.getMethod("attributes");
            attributeListRemoveAllAttributesMethod = attributeListClass.getMethod("removeAllAttributes");
            attributeListGetAttributeAtMethod = attributeListClass.getMethod("getAttributeAt", int.class);
            attributeListGetAttributeCountMethod = attributeListClass.getMethod("getAttributeCount");
            attributeListAddAttributeMethod = attributeListClass.getMethod("addAttribute", int.class, int.class, int.class, String.class, byte[].class);

            tokenDescriptorClass = Class.forName("netegrity.siteminder.javaagent.TokenDescriptor", true, classLoader);
            tokenDescriptorConstructor = tokenDescriptorClass.getConstructor(int.class, boolean.class);
            tokenDescriptor_ver = tokenDescriptorClass.getField("ver");
            tokenDescriptor_bThirdParty = tokenDescriptorClass.getField("bThirdParty");

            initDefClass = Class.forName("netegrity.siteminder.javaagent.InitDef", true, classLoader);
            initDefDefaultConstructor = initDefClass.getConstructor();
            initDefNonClusteredConstructor = initDefClass.getConstructor(String.class, String.class, boolean.class, serverDefClass);
            initDefClusteredConstructor = initDefClass.getConstructor(String.class, String.class, int.class, serverDefClass);
            initDef_CRYPTO_OP_UNSET = (Integer) initDefClass.getField("CRYPTO_OP_UNSET").get(null);
            initDef_CRYPTO_OP_COMPAT = (Integer) initDefClass.getField("CRYPTO_OP_COMPAT").get(null);
            initDef_CRYPTO_OP_MIGRATE_F1402 = (Integer) initDefClass.getField("CRYPTO_OP_MIGRATE_F1402").get(null);
            initDef_CRYPTO_OP_F1402 = (Integer) initDefClass.getField("CRYPTO_OP_F1402").get(null);

            initDefAddServerDefNonClusteredMethod = initDefClass.getMethod("addServerDef", serverDefClass);
            initDefAddServerDefClusteredMethod = initDefClass.getMethod("addServerDef", String.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            initDefSetCryptoOpModeMethod = initDefClass.getMethod("setCryptoOpMode", int.class);

            managementContextDefClass = Class.forName("netegrity.siteminder.javaagent.ManagementContextDef", true, classLoader);
            managementContextDefConstructor = managementContextDefClass.getConstructor(int.class, String.class);
            managementContextDef_MANAGEMENT_GET_AGENT_COMMANDS = (Integer) managementContextDefClass.getField("MANAGEMENT_GET_AGENT_COMMANDS").get(null);
            managementContextDef_MANAGEMENT_SET_AGENT_INFO = (Integer) managementContextDefClass.getField("MANAGEMENT_SET_AGENT_INFO").get(null);

            agentApiClass = Class.forName("netegrity.siteminder.javaagent.AgentAPI", true, classLoader);
            agentApiConstructor = agentApiClass.getConstructor();
            netegrity.siteminder.javaagent.AgentAPI test = new netegrity.siteminder.javaagent.AgentAPI();

            agentApi_YES = (Integer) agentApiClass.getField("YES").get(null);
            agentApi_NO = (Integer) agentApiClass.getField("NO").get(null);
            agentApi_SUCCESS = (Integer) agentApiClass.getField("SUCCESS").get(null);
            agentApi_FAILURE = (Integer) agentApiClass.getField("FAILURE").get(null);
            agentApi_NOCONNECTION = (Integer) agentApiClass.getField("NOCONNECTION").get(null);
            agentApi_TIMEOUT = (Integer) agentApiClass.getField("TIMEOUT").get(null);
            agentApi_ATTR_USERDN = (Integer) agentApiClass.getField("ATTR_USERDN").get(null);
            agentApi_ATTR_SESSIONSPEC = (Integer) agentApiClass.getField("ATTR_SESSIONSPEC").get(null);
            agentApi_ATTR_SESSIONID = (Integer) agentApiClass.getField("ATTR_SESSIONID").get(null);
            agentApi_ATTR_USERNAME = (Integer) agentApiClass.getField("ATTR_USERNAME").get(null);
            agentApi_ATTR_USERMSG = (Integer) agentApiClass.getField("ATTR_USERMSG").get(null);
            agentApi_ATTR_CLIENTIP = (Integer) agentApiClass.getField("ATTR_CLIENTIP").get(null);
            agentApi_ATTR_DEVICENAME = (Integer) agentApiClass.getField("ATTR_DEVICENAME").get(null);
            agentApi_ATTR_IDLESESSIONTIMEOUT = (Integer) agentApiClass.getField("ATTR_IDLESESSIONTIMEOUT").get(null);
            agentApi_ATTR_MAXSESSIONTIMEOUT = (Integer) agentApiClass.getField("ATTR_MAXSESSIONTIMEOUT").get(null);
            agentApi_ATTR_STARTSESSIONTIME = (Integer) agentApiClass.getField("ATTR_STARTSESSIONTIME").get(null);
            agentApi_ATTR_LASTSESSIONTIME = (Integer) agentApiClass.getField("ATTR_LASTSESSIONTIME").get(null);
            agentApi_INVALID_SESSIONDEF = (Integer) agentApiClass.getField("INVALID_SESSIONDEF").get(null);
            agentApi_INVALID_ATTRLIST = (Integer) agentApiClass.getField("INVALID_ATTRLIST").get(null);
            agentApi_INVALID_RESCTXDEF = (Integer) agentApiClass.getField("INVALID_RESCTXDEF").get(null);
            agentApi_INVALID_REALMDEF = (Integer) agentApiClass.getField("INVALID_REALMDEF").get(null);

            agentApiLoginMethod = agentApiClass.getMethod(
                    "login",
                    String.class,
                    resourceContextDefClass,
                    realmDefClass,
                    userCredentialsClass,
                    sessionDefClass,
                    attributeListClass);

            agentApiLoginExMethod = agentApiClass.getMethod(
                    "loginEx",
                    String.class,
                    resourceContextDefClass,
                    realmDefClass,
                    userCredentialsClass,
                    sessionDefClass,
                    attributeListClass,
                    String.class);

            agentApiCreateSsoTokenMethod = agentApiClass.getMethod(
                    "createSSOToken",
                    sessionDefClass,
                    attributeListClass,
                    StringBuffer.class);

            agentApiDecodeSsoTokenMethod = agentApiClass.getMethod(
                    "decodeSSOToken",
                    String.class,
                    tokenDescriptorClass,
                    attributeListClass,
                    boolean.class,
                    StringBuffer.class);

            agentApiAuthorizeMethod =  agentApiClass.getMethod(
                    "authorize",
                    String.class,
                    String.class,
                    resourceContextDefClass,
                    realmDefClass,
                    sessionDefClass,
                    attributeListClass);

            agentApiAuthorizeExMethod = agentApiClass.getMethod(
                    "authorizeEx",
                    String.class,
                    String.class,
                    resourceContextDefClass,
                    realmDefClass,
                    sessionDefClass,
                    attributeListClass,
                    StringBuffer.class,
                    StringBuffer.class,
                    Boolean.class);


            agentApiIsProtectedMethod = agentApiClass.getMethod(
                    "isProtected",
                    String.class,
                    resourceContextDefClass,
                    realmDefClass);

            agentApiGetConfigMethod = agentApiClass.getMethod(
                    "getConfig",
                    initDefClass,
                    String.class,
                    String.class);

            agentApiInitMethod = agentApiClass.getMethod(
                    "init",
                    initDefClass);

            agentApiDoManagementMethod = agentApiClass.getMethod(
                    "doManagement",
                    managementContextDefClass,
                    attributeListClass);

            isInitialized = true;
        } catch(ClassNotFoundException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch(NoSuchMethodException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch(NoSuchFieldException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch(IllegalAccessException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    public Object createServerDefClass() throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return serverDefConstructor.newInstance();
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setServerDef_serverIpAddress(Object serverIpAddress, String value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            serverDef_serverIpAddress.set(serverIpAddress, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public String getServerDef_serverIpAddress(Object serverIpAddress) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (String) serverDef_serverIpAddress.get(serverIpAddress);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setServerDef_connectionMin(Object connectionMin, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            serverDef_connectionMin.set(connectionMin, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getServerDef_connectionMin(Object connectionMin) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) serverDef_connectionMin.get(connectionMin);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setServerDef_connectionMax(Object connectionMax, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            serverDef_connectionMax.set(connectionMax, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getServerDef_connectionMax(Object connectionMax) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) serverDef_connectionMax.get(connectionMax);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setServerDef_connectionStep(Object connectionStep, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            serverDef_connectionStep.set(connectionStep, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getServerDef_connectionStep(Object connectionStep) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) serverDef_connectionStep.get(connectionStep);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setServerDef_timeout(Object timeout, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            serverDef_timeout.set(timeout, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getServerDef_timeout(Object timeout) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) serverDef_timeout.get(timeout);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setServerDef_authorizationPort(Object authorizationPort, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            serverDef_authorizationPort.set(authorizationPort, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getServerDef_authorizationPort(Object authorizationPort) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) serverDef_authorizationPort.get(authorizationPort);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setServerDef_authenticationPort(Object authenticationPort, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            serverDef_authenticationPort.set(authenticationPort, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getServerDef_authenticationPort(Object authenticationPort) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) serverDef_authenticationPort.get(authenticationPort);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setServerDef_accountingPort(Object accountingPort, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            serverDef_accountingPort.set(accountingPort, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getServerDef_accountingPort(Object accountingPort) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) serverDef_accountingPort.get(accountingPort);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setServerDef_clusterSeq(Object serverDef, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            serverDef_clusterSeq.set(serverDef, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getServerDef_clusterSeq(Object serverDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) serverDef_clusterSeq.get(serverDef);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createResourceContextDefClass(
            java.lang.String agent,
            java.lang.String server,
            java.lang.String resource,
            java.lang.String action) throws SiteMinderApiClassException {

        checkInitialized();
        try {
            return resourceContextDefConstructor.newInstance(agent, server, resource, action);
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setResourceContextDef_action(Object resourceContextDef, String value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            resourceContextDef_action.set(resourceContextDef, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public String getResourceContextDef_action(Object resourceContextDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (String) resourceContextDef_action.get(resourceContextDef);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createRealmDefClass() throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return realmDefConstructor.newInstance();
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createUserCredentialsClass() throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return userCredentialsDefaultConstructor.newInstance();
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createUserCredentialsClass(String name, String password) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return userCredentialsCustomConstructor.newInstance(name, password);
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setUserCredentials_reason(Object userCredentials, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            userCredentials_reason.set(userCredentials, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getUserCredentials_reason(Object userCredentials) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) userCredentials_reason.get(userCredentials);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setUserCredentials_name(Object userCredentials, String value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            userCredentials_name.set(userCredentials, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public String getUserCredentials_name(Object userCredentials) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (String) userCredentials_name.get(userCredentials);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setUserCredentials_password(Object userCredentials, String value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            userCredentials_password.set(userCredentials, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public String getUserCredentials_password(Object userCredentials) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (String) userCredentials_password.get(userCredentials);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setUserCredentials_certUserDN(Object userCredentials, String value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            userCredentials_certUserDN.set(userCredentials, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public String getUserCredentials_certUserDN(Object userCredentials) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (String) userCredentials_certUserDN.get(userCredentials);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setUserCredentials_certIssuerDN(Object userCredentials, String value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            userCredentials_certIssuerDN.set(userCredentials, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public String getUserCredentials_certIssuerDN(Object userCredentials) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (String) userCredentials_certIssuerDN.get(userCredentials);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setUserCredentials_certBinary(Object userCredentials, byte[] value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            userCredentials_certBinary.set(userCredentials, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public byte[] getUserCredentials_certBinary(Object userCredentials) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (byte[]) userCredentials_certBinary.get(userCredentials);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createSessionDefClass() throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return sessionDefConstructor.newInstance();
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setSessionDef_reason(Object sessionDef, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            sessionDef_reason.set(sessionDef, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getSessionDef_reason(Object sessionDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) sessionDef_reason.get(sessionDef);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setSessionDef_id(Object sessionDef, String value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            sessionDef_id.set(sessionDef, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public String getSessionDef_id(Object sessionDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (String) sessionDef_id.get(sessionDef);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setSessionDef_spec(Object sessionDef, String value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            sessionDef_spec.set(sessionDef, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public String getSessionDef_spec(Object sessionDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (String) sessionDef_spec.get(sessionDef);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createAttributeListClass() throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return attributeListConstructor.newInstance();
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object addAttribute() throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return attributeListConstructor.newInstance();
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Enumeration attributeListAttributes(Object attributeList) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Enumeration) attributeListAttributesMethod.invoke(attributeList);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void attributeListRemoveAllAttributes(Object attributeList) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            attributeListRemoveAllAttributesMethod.invoke(attributeList);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object attributeListGetAttributeAt(Object attributeList, int index) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return attributeListGetAttributeAtMethod.invoke(attributeList, index);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer attributeListGetAttributeCount(Object attributeList) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) attributeListGetAttributeCountMethod.invoke(attributeList);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void attributeListAddAttribute(Object attributeList, int id, int ttl, int flag, String oid, byte[] value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            attributeListAddAttributeMethod.invoke(attributeList, id, ttl, flag, oid, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setAttribute_id(Object attribute, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            attribute_id.set(attribute, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getAttribute_id(Object attribute) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) attribute_id.get(attribute);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setAttribute_ttl(Object attribute, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            attribute_ttl.set(attribute, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getAttribute_ttl(Object attribute) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) attribute_ttl.get(attribute);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setAttribute_flags(Object attribute, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            attribute_flags.set(attribute, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getAttribute_flags(Object attribute) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) attribute_flags.get(attribute);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setAttribute_oid(Object attribute, String value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            attribute_oid.set(attribute, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public String getAttribute_oid(Object attribute) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (String) attribute_oid.get(attribute);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setAttribute_value(Object attribute, byte[] value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            attribute_value.set(attribute, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public byte[] getAttribute_value(Object attribute) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (byte[]) attribute_value.get(attribute);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createTokenDescriptorClass(int ver, boolean thirdParty) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return tokenDescriptorConstructor.newInstance(ver, thirdParty);
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setTokenDescriptor_ver(Object tokenDescriptor, int value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            tokenDescriptor_ver.set(tokenDescriptor, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getTokenDescriptor_ver(Object tokenDescriptor) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) tokenDescriptor_ver.get(tokenDescriptor);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void setTokenDescriptor_thirdParty(Object tokenDescriptor, boolean value) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            tokenDescriptor_bThirdParty.set(tokenDescriptor, value);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Boolean getTokenDescriptor_thirdParty(Object tokenDescriptor) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Boolean) tokenDescriptor_bThirdParty.get(tokenDescriptor);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createInitDefClass() throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return initDefDefaultConstructor.newInstance();
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createInitDefClass(String hostName, String sharedSecret, boolean failOver, Object serverDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return initDefNonClusteredConstructor.newInstance(hostName, sharedSecret, failOver, serverDef);
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createInitDefClass(String hostName, String sharedSecret, int failOverThreshold, Object serverDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return initDefClusteredConstructor.newInstance(hostName, sharedSecret, failOverThreshold, serverDef);
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getInitDef_CRYPTO_OP_UNSET() throws SiteMinderApiClassException {
        checkInitialized();
        return initDef_CRYPTO_OP_UNSET;
    }

    public Integer getInitDef_CRYPTO_OP_COMPAT() throws SiteMinderApiClassException {
        checkInitialized();
        return initDef_CRYPTO_OP_COMPAT;
    }

    public Integer getInitDef_CRYPTO_OP_MIGRATE_F1402() throws SiteMinderApiClassException {
        checkInitialized();
        return initDef_CRYPTO_OP_MIGRATE_F1402;
    }

    public Integer getInitDef_CRYPTO_OP_F1402() throws SiteMinderApiClassException {
        checkInitialized();
        return initDef_CRYPTO_OP_F1402;
    }

    public void initDefAddServerDef(
            Object initDef,
            Object serverDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            initDefAddServerDefNonClusteredMethod.invoke(initDef, serverDef);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void initDefAddServerDef(
            Object initDef,
            String serverIpAddress,
            int connectionMin,
            int connectionMax,
            int connectionStep,
            int timeout,
            int authorizationPort,
            int authenticationPort,
            int accountingPort,
            int clusterSeq) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            initDefAddServerDefClusteredMethod.invoke(
                    initDef,
                    serverIpAddress,
                    connectionMin, connectionMax, connectionStep,
                    timeout,
                    authorizationPort, authenticationPort, accountingPort,
                    clusterSeq);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public void initDefSetCryptoOpMode(Object initDef, int cryptoMode) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            initDefSetCryptoOpModeMethod.invoke(initDef, cryptoMode);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Object createManagementContextDefClass(int command, String data) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return managementContextDefConstructor.newInstance(command, data);
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getManagementContextDef_MANAGEMENT_GET_AGENT_COMMANDS() throws SiteMinderApiClassException {
        checkInitialized();
        return managementContextDef_MANAGEMENT_GET_AGENT_COMMANDS;
    }

    public Integer getManagementContextDef_MANAGEMENT_SET_AGENT_INFO() throws SiteMinderApiClassException {
        checkInitialized();
        return managementContextDef_MANAGEMENT_SET_AGENT_INFO;
    }

    public Object createAgentApiClass() throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return agentApiConstructor.newInstance();
        } catch (InstantiationException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer getAgentApi_YES() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_YES;
    }

    public Integer getAgentApi_NO() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_NO;
    }

    public Integer getAgentApi_SUCCESS() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_SUCCESS;
    }

    public Integer getAgentApi_FAILURE() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_FAILURE;
    }

    public Integer getAgentApi_NOCONNECTION() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_NOCONNECTION;
    }

    public Integer getAgentApi_INVALID_SESSIONDEF() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_INVALID_SESSIONDEF;
    }

    public Integer getAgentApi_INVALID_ATTRLIST() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_INVALID_ATTRLIST;
    }

    public Integer getAgentApi_INVALID_RESCTXDEF() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_INVALID_RESCTXDEF;
    }

    public Integer getAgentApi_INVALID_REALMDEF() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_INVALID_REALMDEF;
    }

    public Integer getAgentApi_TIMEOUT() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_TIMEOUT;
    }

    public Integer getAgentApi_ATTR_USERDN() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_USERDN;
    }

    public Integer getAgentApi_ATTR_SESSIONSPEC() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_SESSIONSPEC;
    }

    public Integer getAgentApi_ATTR_SESSIONID() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_SESSIONID;
    }

    public Integer getAgentApi_ATTR_USERNAME() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_USERNAME;
    }

    public Integer getAgentApi_ATTR_USERMSG() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_USERMSG;
    }

    public Integer getAgentApi_ATTR_CLIENTIP() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_CLIENTIP;
    }

    public Integer getAgentApi_ATTR_DEVICENAME() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_DEVICENAME;
    }

    public Integer getAgentApi_ATTR_IDLESESSIONTIMEOUT() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_IDLESESSIONTIMEOUT;
    }

    public Integer getAgentApi_ATTR_MAXSESSIONTIMEOUT() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_MAXSESSIONTIMEOUT;
    }

    public Integer getAgentApi_ATTR_STARTSESSIONTIME() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_STARTSESSIONTIME;
    }

    public Integer getAgentApi_ATTR_LASTSESSIONTIME() throws SiteMinderApiClassException {
        checkInitialized();
        return agentApi_ATTR_LASTSESSIONTIME;
    }

    public Integer agentApiLogin(
            Object agentApi,
            String clientIpAddress,
            Object resourceContextDef,
            Object realmDef,
            Object userCredentials,
            Object sessionDef,
            Object attributeList) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiLoginMethod.invoke(
                    agentApi,
                    clientIpAddress,
                    resourceContextDef,
                    realmDef,
                    userCredentials,
                    sessionDef,
                    attributeList);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer agentApiLoginEx(
            Object agentApi,
            String clientIpAddress,
            Object resourceContextDef,
            Object realmDef,
            Object userCredentials,
            Object sessionDef,
            Object attributeList,
            Object transactionId) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiLoginExMethod.invoke(
                    agentApi,
                    clientIpAddress,
                    resourceContextDef,
                    realmDef,
                    userCredentials,
                    sessionDef,
                    attributeList,
                    transactionId);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer agentApiCreateSsoToken(
            Object agentApi,
            Object sessionDef,
            Object attributeList,
            StringBuffer ssoToken) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiCreateSsoTokenMethod.invoke(agentApi, sessionDef, attributeList, ssoToken);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer agentApiDecodeSsoToken(
            Object agentApi,
            String ssoToken,
            Object tokenDescriptor,
            Object attributeList,
            boolean updateToken,
            StringBuffer updatedSsoToken) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiDecodeSsoTokenMethod.invoke(
                    agentApi, ssoToken, tokenDescriptor, attributeList, updateToken, updatedSsoToken);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer agentApiAuthorize(
            Object agentApi,
            String clientIpAddress,
            String transactionId,
            Object resourceContextDef,
            Object realmDef,
            Object sessionDef,
            Object attributeList) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiAuthorizeMethod.invoke(
                    agentApi, clientIpAddress, transactionId, resourceContextDef, realmDef, sessionDef, attributeList);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer agentApiAuthorizeEx(
            Object agentApi,
            String clientIpAddress,
            String transactionId,
            Object resourceContextDef,
            Object realmDef,
            Object sessionDef,
            Object attributeList,
            StringBuffer unresolvedVars,
            StringBuffer resolvedVars,
            Boolean simpleAuth) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiAuthorizeExMethod.invoke(
                    agentApi, clientIpAddress, transactionId, resourceContextDef, realmDef, sessionDef, attributeList, unresolvedVars, resolvedVars, simpleAuth);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer agentApiIsProtected(
            Object agentApi,
            String clientIpAddress,
            Object resourceContextDef,
            Object realmDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiIsProtectedMethod.invoke(
                    agentApi, clientIpAddress, resourceContextDef, realmDef);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer agentApiGetConfig(
            Object agentApi,
            Object initDef,
            String agentName,
            String configPath) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiGetConfigMethod.invoke(agentApi, initDef, agentName, configPath);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer agentApiInit(
            Object agentApi,
            Object initDef) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiInitMethod.invoke(agentApi, initDef);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    public Integer agentApiDoManagement(
            Object agentApi,
            Object managementContextDef,
            Object attributeList) throws SiteMinderApiClassException {
        checkInitialized();
        try {
            return (Integer) agentApiDoManagementMethod.invoke(agentApi, managementContextDef, attributeList);
        } catch (IllegalAccessException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        } catch (InvocationTargetException e) {
            throw new SiteMinderApiClassException("Failure with CA SiteMinder components. ", e);
        }
    }

    private void checkInitialized() throws SiteMinderApiClassException {
        if (!isInitialized) {
            throw new SiteMinderApiClassException("SiteMinder custom class loader not initialized.");
        }
    }


}
