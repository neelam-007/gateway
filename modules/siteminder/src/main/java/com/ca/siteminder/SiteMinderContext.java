package com.ca.siteminder;

import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.Nullable;

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
    private String transactionId = null;
    private List<Attribute> attrList = new ArrayList<>();
    private List<AuthenticationScheme> authSchemes = new ArrayList<>();
    private String ssoToken;
    private String ssoZoneName;
    private SiteMinderLowLevelAgent agent;
    private String sourceIpAddress;
    private SiteMinderConfiguration config;
    private boolean resourceProtected;

    public SiteMinderContext() {

    }

    // To ensure that the SiteMinder Context can be cached as an immutable object
    // we need to obtain new instance objects (perform a private copy of all objects/members except the SiteMinderLowLevel Agent Instance).
    // NOTE: The SiteMinder Agent is the hook into the underlying SiteMinder AgentAPI
    protected SiteMinderContext( SiteMinderContext context,
                                 SiteMinderContext.SessionDef sessionDef, SiteMinderContext.RealmDef realmDef, SiteMinderContext.ResourceContextDef resContextDef ) {

        this.agent = context.getAgent();
        this.attrList = new ArrayList<>( context.getAttrList() );
        this.authSchemes = new ArrayList<>( context.getAuthSchemes() );
        //Strings are immutable, thus calling 'new' is not required
        this.sourceIpAddress = context.getSourceIpAddress();
        this.transactionId = context.getTransactionId();
        this.ssoToken = context.getSsoToken();

        this.sessionDef = new SessionDef( sessionDef.getReason(), sessionDef.getIdleTimeout() ,
                sessionDef.getMaxTimeout(), sessionDef.getCurrentServerTime(), sessionDef.getSessionStartTime(),
                sessionDef.getSessionLastTime(), sessionDef.getId(), sessionDef.getSpec() );

        this.realmDef = new RealmDef( realmDef.getName(), realmDef.getOid(), realmDef.getDomOid(),
                realmDef.getCredentials(), realmDef.getFormLocation() );

        this.resContextDef = new ResourceContextDef( resContextDef.getAgent(), resContextDef.getServer(),
                resContextDef.getResource(), resContextDef.getAction() );
    }

    protected SiteMinderContext ( SiteMinderContext siteMinderContext ) {
        this( siteMinderContext, siteMinderContext.sessionDef, siteMinderContext.realmDef, siteMinderContext.resContextDef );
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

    public void setSsoToken(@Nullable String ssoToken) {
        this.ssoToken = ssoToken;
    }

    public String getSsoZoneName() {
        return ssoZoneName;
    }

    public void setSsoZoneName(String ssoZoneName) {
        this.ssoZoneName = ssoZoneName;
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

    public List<SiteMinderContext.Attribute> getAttrList() {
        return attrList;
    }

    public void setAttrList(List<Attribute> attrMap) {
        this.attrList = attrMap;
    }

    public List<AuthenticationScheme> getAuthSchemes() {
        return authSchemes;
    }

    public void setAuthSchemes(List<AuthenticationScheme> schemes) {
        authSchemes = schemes;
    }

    public String getSourceIpAddress() {
        return sourceIpAddress;
    }

    public void setSourceIpAddress(@Nullable String sourceIpAddress) {
        this.sourceIpAddress = sourceIpAddress;
    }

    public SiteMinderConfiguration getConfig() {
        return config;
    }

    public void setConfig(SiteMinderConfiguration config) {
        this.config = config;
    }

    public boolean isResourceProtected() {
        return resourceProtected;
    }

    public void setResourceProtected(boolean resourceProtected) {
        this.resourceProtected = resourceProtected;
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

    public static class Attribute {

        private String name;
        private Object value;
        private final int ttl;//time to live in seconds
        private final int id; //raw attribute id
        private final String oid; //raw attribute oid
        private final byte[] rawValue;// raw value
        private final int flags;///raw flags

        public Attribute(String name, Object value) {
            this(name, value, 0, 0, "", 0, new byte[0]);
        }

        public Attribute(String name, Object value, int flags, int id, String oid, int ttl, byte[] rawValue) {
            this.name = name;
            this.value = value;
            this.ttl = ttl;
            this.id = id;
            this.oid = oid;
            this.flags = flags;
            this.rawValue = rawValue;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public int getTtl() {
            return ttl;
        }

        public int getId() {
            return id;
        }

        public String getOid() {
            return oid;
        }

        public byte[] getRawValue() {
            return rawValue;
        }

        public int getFlags() {
            return flags;
        }

        @Override
        public String toString() {
            return String.format("(%s,%s)", name, value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Attribute attribute = (Attribute) o;

            if (id != attribute.id) return false;
            if (flags != attribute.flags) return false;
            if (!name.equals(attribute.name)) return false;
            return !(oid != null ? !oid.equals(attribute.oid) : attribute.oid != null);

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + id;
            result = 31 * result + (oid != null ? oid.hashCode() : 0);
            result = 31 * result + flags;
            return result;
        }
    }
}
