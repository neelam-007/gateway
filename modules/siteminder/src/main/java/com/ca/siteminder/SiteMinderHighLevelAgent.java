package com.ca.siteminder;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ca.siteminder.util.SiteMinderUtil;
import netegrity.siteminder.javaagent.*;
import org.jetbrains.annotations.Nullable;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/26/13
 */
public class SiteMinderHighLevelAgent {
    private static final Logger logger = Logger.getLogger(SiteMinderHighLevelAgent.class.getName());

    /**
     * @return true if the resource is protected
     * @throws SiteMinderApiClassException
     */
    public boolean checkProtected(final String userIp,
                                  String smAgentName, final String resource,
                                  final String action,
                                  SiteMinderContext context) throws SiteMinderApiClassException {
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");//should never happen

        SiteMinderLowLevelAgent agent = context.getAgent();
        if(agent == null) throw new SiteMinderApiClassException("Unable to find SiteMinder Agent");

        // The realmDef object will contain the realm handle for the resource if the resource is protected.
        ResourceContextDef resCtxDef = new ResourceContextDef(smAgentName, "", resource, action);
        RealmDef realmDef = new RealmDef();

        // check the requested resource/action is actually protected by SiteMinder
        boolean isProtected = agent.isProtected(userIp, resCtxDef, realmDef, context);

        //now set the context
        context.setResContextDef(new SiteMinderContext.ResourceContextDef(resCtxDef.agent, resCtxDef.server, resCtxDef.resource, resCtxDef.action));
        context.setRealmDef(new SiteMinderContext.RealmDef(realmDef.name, realmDef.oid, realmDef.domOid, realmDef.credentials, realmDef.formLocation));
        //determine which authethentication scheme to use

        buildAuthenticationSchemes(context, realmDef.credentials);

        if (!isProtected) {
            logger.log(Level.INFO,"The resource/action '" + resource + "/" + action + "' is not protected by SiteMinder. Access cannot be authorized.");
            return false;
        }

        return true;

    }

    private void buildAuthenticationSchemes(SiteMinderContext context, int credentials) {
        final Set<SiteMinderContext.AuthenticationScheme> authSchemes = new HashSet<>();
        if(credentials != AgentAPI.CRED_NONE) {
            if((credentials & AgentAPI.CRED_BASIC) == AgentAPI.CRED_BASIC ){
               authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
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

    public int processAuthorizationRequest(final String userIp, final String ssoCookie,final SiteMinderContext context) throws SiteMinderApiClassException {
        if(context == null) throw new SiteMinderApiClassException("SiteMinderContext object is null!");//should never happen

        SiteMinderLowLevelAgent agent = context.getAgent();
        if(agent == null) throw new SiteMinderApiClassException("Unable to find SiteMinder Agent");

        return agent.authorize(ssoCookie, userIp, context.getTransactionId(), context);


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
        if(agent == null) throw new SiteMinderApiClassException("Unable to find SiteMinder Agent");

        // check for some kind of credential
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

        int result;

        if (null != ssoCookie && ssoCookie.trim().length() > 0) {
            // attempt to authorize with existing cookie
            result = agent.validateSession( userCreds, userIp, ssoCookie,  context.getTransactionId(), context);
            if(result != AgentAPI.YES) {
                logger.log(Level.FINE, "Unable to validate user session!");
                //TODO: should we logout the session?
            }
            else {
                logger.log(Level.FINE, "Authenticated user using third-party cookie:" + ssoCookie);
            }
        } else {
            // authenticate using credentials
            result = agent.authenticate(userCreds, userIp, context.getTransactionId(), context);
            if(result != AgentAPI.YES) {
               logger.log(Level.FINE, "Unable to authenticate user: " + SiteMinderUtil.getCredentialsAsString(userCreds));
            }
            else {
                logger.log(Level.FINE, "Authenticated user via user credentials" + SiteMinderUtil.getCredentialsAsString(userCreds));
            }

        }

        return result;
    }
}
