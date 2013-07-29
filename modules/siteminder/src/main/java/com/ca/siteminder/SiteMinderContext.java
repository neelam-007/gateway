package com.ca.siteminder;

import com.l7tech.util.Pair;

import java.util.*;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/9/13
 */
public class SiteMinderContext {


    public enum AuthenticationScheme {
        NONE, ALLOWSAVE, BASIC, FORM, DIGEST, METADATA, NTCHALLENGE, SAML, SSL, X509CERT, X509CERTISSUEDN, X509CERTUSERDN, XMLDOC, XMLDSIG, XKMS, XMLWSSEC;

        public static AuthenticationScheme findValue(String s) {
           for(AuthenticationScheme val : values()) {
               if(val.toString().equalsIgnoreCase(s)) return val;
           }
           return null;
        }

    }



    private ResourceContextDef resContextDef;
    private RealmDef realmDef;
    private SessionDef sessionDef;
    private  String transactionId = null;
    private List<Pair<String, Object>> attrList = new ArrayList<Pair<String,Object>>();
    List<AuthenticationScheme> authSchemes = new ArrayList<AuthenticationScheme>();
    private String ssoToken;
    private SiteMinderLowLevelAgent agent;

    public SiteMinderContext() {

    }

    public SiteMinderLowLevelAgent getAgent() {
        return agent;
    }

    public void setAgent(SiteMinderLowLevelAgent agent) {
        this.agent = agent;
    }

    public String getSsoToken() {
        return ssoToken;
    }

    public void setSsoToken(String ssoToken) {
        this.ssoToken = ssoToken;
    }

    public ResourceContextDef getResContextDef() {
        return resContextDef;
    }

    public void setResContextDef(ResourceContextDef resContextDef) {
        this.resContextDef = resContextDef;
    }

    public RealmDef getRealmDef() {
        return realmDef;
    }

    public void setRealmDef(RealmDef realmDef) {
        this.realmDef = realmDef;
    }

    public SessionDef getSessionDef() {
        return sessionDef;
    }

    public void setSessionDef(SessionDef sessionDef) {
        this.sessionDef = sessionDef;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public List<Pair<String, Object>> getAttrList() {
        return attrList;
    }

    public void setAttrList(List<Pair<String, Object>> attrMap) {
        this.attrList = attrMap;
    }

    public List<AuthenticationScheme> getAuthSchemes() {
        return authSchemes;
    }

    public void setAuthSchemes(List<AuthenticationScheme> schemes) {
        authSchemes = schemes;
    }

    public static class SessionDef  {
        private int reason;
        private int idleTimeout;
        private int maxTimeout;
        private int currentServerTime;
        private int sessionStartTime;
        private int sessionLastTime;
        private java.lang.String id;
        private java.lang.String spec;

        public SessionDef() {
        }

        public SessionDef(int reason,
                          int idleTimeout,
                          int maxTimeout,
                          int currentServerTime,
                          int sessionStartTime,
                          int sessionLastTime,
                          String id,
                          String spec) {
            this.reason = reason;
            this.idleTimeout = idleTimeout;
            this.maxTimeout = maxTimeout;
            this.currentServerTime = currentServerTime;
            this.sessionLastTime = sessionLastTime;
            this.sessionStartTime = sessionStartTime;
            this.id = id;
            this.spec = spec;
        }
        public int getReason() {
            return reason;
        }

        public void setReason(int reason) {
            this.reason = reason;
        }

        public int getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(int idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public int getMaxTimeout() {
            return maxTimeout;
        }

        public void setMaxTimeout(int maxTimeout) {
            this.maxTimeout = maxTimeout;
        }

        public int getCurrentServerTime() {
            return currentServerTime;
        }

        public void setCurrentServerTime(int currentServerTime) {
            this.currentServerTime = currentServerTime;
        }

        public int getSessionStartTime() {
            return sessionStartTime;
        }

        public void setSessionStartTime(int sessionStartTime) {
            this.sessionStartTime = sessionStartTime;
        }

        public int getSessionLastTime() {
            return sessionLastTime;
        }

        public void setSessionLastTime(int sessionLastTime) {
            this.sessionLastTime = sessionLastTime;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSpec() {
            return spec;
        }

        public void setSpec(String spec) {
            this.spec = spec;
        }


    }

    public static class RealmDef  {
        private String name;
        private String domOid;
        private String oid;
        private int credentials;
        private String formLocation;

        public RealmDef() {

        }

        public RealmDef(String name, String oid, String domOid, int credentials, String formLocation) {
            this.name = name;
            this.oid = oid;
            this.domOid = domOid;
            this.credentials = credentials;
            this.formLocation = formLocation;
        }
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDomOid() {
            return domOid;
        }

        public void setDomOid(String domOid) {
            this.domOid = domOid;
        }

        public String getOid() {
            return oid;
        }

        public void setOid(String oid) {
            this.oid = oid;
        }

        public int getCredentials() {
            return credentials;
        }

        public void setCredentials(int credentials) {
            this.credentials = credentials;
        }

        public String getFormLocation() {
            return formLocation;
        }

        public void setFormLocation(String formLocation) {
            this.formLocation = formLocation;
        }
    }

    public static class ResourceContextDef  {
        private String agent;
        private String server;
        private String resource;
        private String action;

        public ResourceContextDef() {

        }

        public ResourceContextDef(java.lang.String p_agent, java.lang.String p_server, java.lang.String p_resource, java.lang.String p_action) {
            this.agent = p_agent;
            this.server = p_server;
            this.resource = p_resource;
            this.action = p_action;
        }

        public String getAgent() {
            return agent;
        }

        public void setAgent(String agent) {
            this.agent = agent;
        }

        public String getServer() {
            return server;
        }

        public void setServer(String server) {
            this.server = server;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
