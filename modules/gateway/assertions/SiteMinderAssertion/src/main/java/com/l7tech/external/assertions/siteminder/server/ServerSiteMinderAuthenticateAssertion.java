package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderAgentConstants;
import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderCredentials;
import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.AnonymousUserReference;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Server side implementation of the SiteMinderAuthenticateAssertion.
 *
 * @see com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion
 */
public class ServerSiteMinderAuthenticateAssertion extends AbstractServerSiteMinderAssertion<SiteMinderAuthenticateAssertion> {
    private final String[] variablesUsed;


    public ServerSiteMinderAuthenticateAssertion(final SiteMinderAuthenticateAssertion assertion, ApplicationContext springContext) throws PolicyAssertionException {
        super(assertion, springContext);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                             final Message message,
                                             final String messageDescription,
                                             final AuthenticationContext authContext)  throws IOException, PolicyAssertionException {

        AssertionStatus status = AssertionStatus.FALSIFIED;

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        String varPrefix = SiteMinderAssertionUtil.extractContextVarValue(assertion.getPrefix(), variableMap, getAudit());
        String ssoToken = extractSsoToken(variableMap);

      SiteMinderContext smContext = null;
        try {
            try {
                smContext = (SiteMinderContext) context.getVariable(varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT);
            } catch (NoSuchVariableException e) {
                final String msg = "No SiteMinder context variable ${" + varPrefix + "." + SiteMinderAssertionUtil.SMCONTEXT + "} found in the Policy Enforcement Context";
                logger.log(Level.SEVERE, msg, ExceptionUtils.getDebugException(e));
                logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), msg);
                return AssertionStatus.FALSIFIED;
            }

            if(smContext.getAgent() == null) {
                logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "Agent is null!");
                return AssertionStatus.FALSIFIED;
            }

            //first check what credentials are accepted by the policy server
            SiteMinderCredentials credentials = collectCredentials(authContext, variableMap, smContext);
            int result = hla.processAuthenticationRequest(credentials, getClientIp(message, smContext), ssoToken, smContext);
            if(result == SM_YES) {
                logAndAudit(AssertionMessages.SITEMINDER_FINE, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), ssoToken != null? "Authenticated via SSO Token: " + ssoToken:"Authenticated credentials: " + credentials);
                addAuthenticatedUserToContext(authContext, smContext);
                status = AssertionStatus.NONE;
            }
            else {
                logAndAudit(AssertionMessages.SITEMINDER_WARNING, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "Unable to authenticate user using" + (ssoToken != null? " SSO Token:" + ssoToken: " credentials: " + credentials));
            }

            populateContextVariables(context, varPrefix, smContext);

        } catch (SiteMinderApiClassException e) {
            logAndAudit(AssertionMessages.SITEMINDER_ERROR, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), e.getMessage());
            return AssertionStatus.FAILED;//something really bad happened
        }

        return status;
    }

    private void addAuthenticatedUserToContext(AuthenticationContext authContext, SiteMinderContext smContext) {

        User user = null;
        //first look for the username attribute if present
        Pair<String, Object> attr = findAttributeByName(smContext, SiteMinderAgentConstants.ATTR_USERNAME);
        if(attr != null && StringUtils.isNotBlank(attr.right.toString())) {
            user = new UserBean(attr.right.toString());
        }
        else {
            //check if userdn is found in LDAP form
            attr = findAttributeByName(smContext, SiteMinderAgentConstants.ATTR_USERDN);
            if(attr != null && StringUtils.isNotBlank(attr.right.toString())) {
                user = new LdapUser(PersistentEntity.DEFAULT_GOID, attr.right.toString(), SiteMinderAssertionUtil.getCn(attr.right.toString()));
            }
        }
        //we don't have a user so create anonymous one
        if(user == null) user = new AnonymousUserReference("", PersistentEntity.DEFAULT_GOID, "<unknown>");
        authContext.addAuthenticationResult(new AuthenticationResult(user, new OpaqueSecurityToken(user.getLogin(), smContext.getSsoToken().toCharArray())));
    }

    private Pair<String, Object> findAttributeByName(SiteMinderContext siteMinderContext, String attrName) {
        for (Pair<String, Object> attr : siteMinderContext.getAttrList()){
            if(attr.left.equalsIgnoreCase(attrName)) {
                return attr;
            }
        }
        return null;//attribute not found
    }

    private String extractSsoToken(Map<String, Object> variableMap) {
        String ssoToken = null;
        if(assertion.isUseSMCookie()) {
           //get cookie from a context variable
           ssoToken = ExpandVariables.process(Syntax.SYNTAX_PREFIX + assertion.getCookieSourceVar() + Syntax.SYNTAX_SUFFIX, variableMap, getAudit());
        }
        return ssoToken;
    }


    private SiteMinderCredentials collectCredentials(AuthenticationContext context,  Map<String, Object> variableMap, SiteMinderContext smContext) throws PolicyAssertionException {

        // Get auth schemes SiteMinder is requesting:
        List<SiteMinderContext.AuthenticationScheme> requestedAuthSchemes = smContext.getAuthSchemes();

        if(requestedAuthSchemes.size() == 1 && requestedAuthSchemes.iterator().next() == SiteMinderContext.AuthenticationScheme.NONE) {
            return null; //anonymous credentials
        }

        boolean useLastCredential = assertion.isLastCredential();
        boolean sendBasic = assertion.isSendUsernamePasswordCredential();
        boolean sendCert = assertion.isSendX509CertificateCredential();

        SiteMinderCredentials siteMinderCredentials = new SiteMinderCredentials();

        if ( ! (sendBasic || sendCert ) ) {
            logAndAudit(AssertionMessages.SITEMINDER_WARNING,"Neither Username and Password; or X.509 Certificate Credentials selected to be sent to SiteMinder. No credentials sent.");
            return siteMinderCredentials;
        }

        try {

            if ( useLastCredential ) {
                if ( sendBasic ) {
                    addLastBasicCreds(context,siteMinderCredentials);
                }

                if ( sendCert ) {
                    addLastCertCreds(context,siteMinderCredentials);
                }
            } else {
                if ( sendBasic ) {
                    addSpecificBasicCreds(SiteMinderAssertionUtil.extractContextVarValue(assertion.getNamedUser(),variableMap,getAudit()), context, siteMinderCredentials);
                }

                if ( sendCert ) {
                    addSpecificCertCreds(SiteMinderAssertionUtil.extractContextVarValue(assertion.getNamedCertificate(),variableMap,getAudit()), context, siteMinderCredentials);
                }
            }

        } catch ( CertificateEncodingException e ) {
            logAndAudit(AssertionMessages.SITEMINDER_WARNING, (String)assertion.meta().get(AssertionMetadata.SHORT_NAME), "Unable to decode client certificate for login credentials.");
            logger.log(Level.WARNING, "Certificate retrieve from Policy Context improperly encoded.", ExceptionUtils.getDebugException(e));
        }

        return siteMinderCredentials;
    }

    private void addLastBasicCreds(AuthenticationContext context, SiteMinderCredentials smCreds) {
        LoginCredentials lastBasic = getLastCredOfType(context.getCredentials(),CredentialFormat.CLEARTEXT);
        if ( lastBasic != null ) {
            smCreds.addUsernamePasswordCredentials(lastBasic.getLogin(), new String(lastBasic.getCredentials()));
        }
    }

    private void addLastCertCreds(AuthenticationContext context, SiteMinderCredentials smCreds) throws CertificateEncodingException {
        LoginCredentials lastCert = getLastCredOfType(context.getCredentials(),CredentialFormat.CLIENTCERT);
        if ( lastCert != null ) {
            smCreds.addClientCertificates(lastCert.getClientCert());
        }
    }

    private void addSpecificBasicCreds(String user, AuthenticationContext context, SiteMinderCredentials smCreds) {
       LoginCredentials basicCreds = getNamedCredOfType(context.getCredentials(),CredentialFormat.CLEARTEXT,user);
       if ( basicCreds != null ) {
           smCreds.addUsernamePasswordCredentials(basicCreds.getLogin(),new String(basicCreds.getCredentials()));
       }
    }

    private void addSpecificCertCreds(String userDn, AuthenticationContext context, SiteMinderCredentials smCreds) throws CertificateEncodingException {
        LoginCredentials certificateCred = getNamedCredOfType(context.getCredentials(),CredentialFormat.CLIENTCERT,userDn);
        if (certificateCred != null) {
            smCreds.addClientCertificates(certificateCred.getClientCert());
        }
    }

    private LoginCredentials getLastCredOfType(List<LoginCredentials> creds,CredentialFormat type) {

        for ( int i = creds.size() - 1; i >= 0; i-- ) {
            if ( creds.get(i).getFormat() == type ) {
                return creds.get(i);
            }
        }

        return null;
    }

    private LoginCredentials getNamedCredOfType(List<LoginCredentials> creds, CredentialFormat type, String name) {

        for ( LoginCredentials credential : creds ) {
            if (credential.getFormat() == type) {
                if (type == CredentialFormat.CLEARTEXT) {
                    if (credential.getLogin().equals(name))
                        return credential;
                } else if (type == CredentialFormat.CLIENTCERT) {
                    X509Certificate clientCert = credential.getClientCert();
                    if (CertUtils.isEqualDNCanonical(CertUtils.getSubjectDN(clientCert), name) ||
                            CertUtils.getCn(clientCert).equalsIgnoreCase(name)) {
                        return credential;
                    }
                }
            }
        }
        return null;
    }

    /*
* Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
* that would otherwise keep our instances from getting collected.
*
* DELETEME if not required.
*/
public static void onModuleUnloaded() {
    // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
    }
}
