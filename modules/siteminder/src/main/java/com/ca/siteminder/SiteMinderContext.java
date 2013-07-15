package com.ca.siteminder;

import com.l7tech.util.Pair;
import netegrity.siteminder.javaagent.RealmDef;
import netegrity.siteminder.javaagent.ResourceContextDef;

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
    private  String transactionId = null;
    private List<Pair<String, Object>> attrList = new ArrayList<Pair<String,Object>>();
    List<AuthenticationScheme> authSchemes = new ArrayList<AuthenticationScheme>();
    private String ssoToken;
    private String agentId;

    public SiteMinderContext() {

    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String id) {
        agentId = id;
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


}
