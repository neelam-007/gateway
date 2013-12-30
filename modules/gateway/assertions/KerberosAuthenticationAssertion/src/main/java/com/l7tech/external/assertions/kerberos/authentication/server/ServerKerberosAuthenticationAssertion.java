package com.l7tech.external.assertions.kerberos.authentication.server;

import com.l7tech.external.assertions.kerberos.authentication.KerberosAuthenticationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.identity.User;
import com.l7tech.kerberos.KerberosClient;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.kerberos.delegate.KerberosDelegateClient;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.KerberosAuthenticationSecurityToken;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.Config;
import org.apache.commons.lang.StringUtils;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.RealmException;

import javax.inject.Inject;
import javax.security.auth.kerberos.KerberosTicket;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Server side implementation of the KerberosAuthenticationAssertion.
 *
 * @see com.l7tech.external.assertions.kerberos.authentication.KerberosAuthenticationAssertion
 */
public class ServerKerberosAuthenticationAssertion extends AbstractServerAssertion<KerberosAuthenticationAssertion> {
    private static final Logger logger = Logger.getLogger(ServerKerberosAuthenticationAssertion.class.getName());

    private final String[] variablesUsed;
    @Inject
    private Config config;

    public ServerKerberosAuthenticationAssertion(final KerberosAuthenticationAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;
        KerberosDelegateClient delegateClient = new KerberosDelegateClient();
        AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        if (authContext.getCredentials().size() < 1 && authContext.getLastAuthenticatedUser() == null) {
            // No credentials have been found yet
            if (authContext.isAuthenticated()) {
                logAndAudit(AssertionMessages.KA_LOGIN_CREDENTIALS_NOT_FOUND);
                throw new IllegalStateException("Request is authenticated but request has no LoginCredentials!");
            }

            if (Assertion.isRequest(assertion)) {
                context.setAuthenticationMissing();
            }
            logAndAudit(AssertionMessages.KA_LOGIN_CREDENTIALS_NOT_FOUND);
            return AssertionStatus.AUTH_REQUIRED;
        }

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());

        return doCheckRequest(authContext, delegateClient, variableMap);
    }

    protected AssertionStatus doCheckRequest(final AuthenticationContext context, KerberosDelegateClient client, final Map<String, Object> variableMap) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;
        String authenticatedUserAccount = null;

        String realm = ExpandVariables.process(assertion.getRealm(), variableMap, getAudit()).toUpperCase();//realm should always be in upper case
        String spn = ExpandVariables.process(assertion.getServicePrincipalName(), variableMap, getAudit());
        String userRealm = null;
        if (assertion.getUserRealm() != null && assertion.getUserRealm().trim().length() > 0) {
            userRealm = ExpandVariables.process(assertion.getUserRealm(), variableMap, getAudit());
        }
        String krbServiceAccount = !assertion.isKrbUseGatewayKeytab()?ExpandVariables.process(assertion.getKrbConfiguredAccount(), variableMap, getAudit()):null;
        
        String svcPrincipal = null;
        KerberosServiceTicket kerberosServiceTicket = null;

        try {
            PrincipalName targetPrincipalName = null;

            if(containRealm(spn)) {
              targetPrincipalName = new PrincipalName(spn);
            }
            else {
                targetPrincipalName = new PrincipalName(spn, realm);
            }

            if (assertion.isS4U2Self()) { //protocol transition case
                LoginCredentials pc = null;
                if(assertion.isLastAuthenticatedUser()) {
                    AuthenticationResult lastAuthenticationResult = context.getLastAuthenticationResult();
                    if(lastAuthenticationResult == null) {
                        logAndAudit(AssertionMessages.KA_LOGIN_CREDENTIALS_NOT_FOUND);
                        return AssertionStatus.FALSIFIED;
                    }

                    User lastAuthenticatedUser = lastAuthenticationResult.getUser();
                    authenticatedUserAccount = lastAuthenticatedUser.getLogin();
                    for(LoginCredentials loginCredentials : context.getCredentials()) {
                        if(matchLoginCredentials(loginCredentials, context.getLastAuthenticationResult())) {
                            pc = loginCredentials;
                            break;
                        }
                    }
                }
                else {
                    String userName = ExpandVariables.process(assertion.getAuthenticatedUser(),variableMap, getAudit());
                    for(LoginCredentials loginCredentials : context.getCredentials()) {
                        for(AuthenticationResult result : context.getAllAuthenticationResults()) {
                            final User user = result.getUser();
                            if(result.getUser() != null && user.getName().equals(userName)) {
                                authenticatedUserAccount = user.getLogin();
                                if (matchLoginCredentials(loginCredentials, result)) {
                                    pc = loginCredentials;
                                    break;
                                }
                            }
                        }
                    }

                }

                if(pc == null) {
                    logAndAudit(AssertionMessages.KA_LOGIN_CREDENTIALS_NOT_FOUND);
                    return AssertionStatus.FALSIFIED;
                }

                if (assertion.isKrbUseGatewayKeytab()) {
                    svcPrincipal = getServicePrincipal(realm);
                    //Check for referral, if user realm != service realm, get referral service ticket.
                    if (userRealm == null || userRealm.trim().length() == 0 || userRealm.equalsIgnoreCase(realm)) {
                        kerberosServiceTicket = client.getKerberosProxyServiceTicket(targetPrincipalName.getName(), svcPrincipal, authenticatedUserAccount);
                    } else {
                        int maxReferral = config.getIntProperty(ServerConfigParams.PARAM_KERBEROS_REFERRAL_LIMIT, 5);
                        kerberosServiceTicket = client.getKerberosProxyServiceTicketWithReferral(targetPrincipalName.getName(), svcPrincipal, authenticatedUserAccount, userRealm, maxReferral);
                    }
                } else {
                    PrincipalName userPrincipal = new PrincipalName(krbServiceAccount, realm);
                    String plaintextPassword = ServerVariables.getSecurePasswordByGoid(new LoggingAudit(logger), assertion.getKrbSecurePasswordReference());
                    if (userRealm == null || userRealm.trim().length() == 0 || userRealm.equalsIgnoreCase(realm)) {
                        kerberosServiceTicket = client.getKerberosProxyServiceTicket(targetPrincipalName.getName(), userPrincipal.getName(), plaintextPassword, authenticatedUserAccount);
                    } else {
                        int maxReferral = config.getIntProperty(ServerConfigParams.PARAM_KERBEROS_REFERRAL_LIMIT, 5);
                        kerberosServiceTicket = client.getKerberosProxyServiceTicketWithReferral(targetPrincipalName.getName(), userPrincipal.getName(), plaintextPassword, authenticatedUserAccount, userRealm, maxReferral);
                    }
                }
            }
            else if(assertion.isS4U2Proxy()) {
                KerberosServiceTicket kst = null;
                //find kerberos credentials from the authentication context that are not processed by this assertion
                for(LoginCredentials pc : context.getCredentials()) {
                    if(pc.getFormat() == CredentialFormat.KERBEROSTICKET && !pc.getSecurityToken().getClass().isAssignableFrom(KerberosAuthenticationSecurityToken.class)) {
                        //extract the delegated ticket and wrap it using S4U2Proxy
                        kst = (KerberosServiceTicket) pc.getPayload();
                        if (kst == null) {
                            throw new PolicyAssertionException(assertion, "No Kerberos service ticket found");
                        }
                    }
                }

                if(kst == null && context.getCredentials().size() > 0) {
                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.WARNING, "Non-kerberos credentials cannot be used with Constrained Proxy (S4U2Proxy) option");
                    }
                    logAndAudit(AssertionMessages.KA_OPTION_NOT_SUPPORTED, "Constrained Proxy", "non-kerberos credentials");
                    return AssertionStatus.FALSIFIED;
                }

                KerberosTicket serviceTicket = kst.getDelegatedKerberosTicket();

                if (assertion.isKrbUseGatewayKeytab()) {
                    svcPrincipal = getServicePrincipal(realm);
                    // construct the ticket from the delegated ticket
                    kerberosServiceTicket = client.getKerberosProxyServiceTicket(targetPrincipalName.getName(), svcPrincipal, serviceTicket);
                }
                else {
                    PrincipalName userPrincipal = new PrincipalName(krbServiceAccount, realm);
                    String plaintextPassword = ServerVariables.getSecurePasswordByGoid(new LoggingAudit(logger), assertion.getKrbSecurePasswordReference());
                    kerberosServiceTicket = client.getKerberosProxyServiceTicket(targetPrincipalName.getName(), userPrincipal.getName(), plaintextPassword, serviceTicket);
                }
            }

        } catch (KerberosException e) {
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.SEVERE, "Unable to obtain Kerberos Service Ticket: " + e.getMessage());
            }
            logAndAudit(AssertionMessages.KA_KERBEROS_EXCEPTION, e.getMessage());
            if ( e.getCause() != null) {
                logAndAudit(AssertionMessages.KA_KERBEROS_KDC_EXCEPTION, e.getCause().getMessage());
            }
            status =  AssertionStatus.FALSIFIED;
        } catch (FindException fe) {
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.SEVERE, "Failed to find Service Account password: " + fe.getMessage());
            }
            logAndAudit(AssertionMessages.KA_UNABLE_TO_FIND_PASSWORD, fe.getMessage());
            status = AssertionStatus.FAILED;
        } catch (RealmException e) {
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.SEVERE, "Invalid Realm: " + e.getMessage());
            }
            logAndAudit(AssertionMessages.KA_LOGIN_REALM_NOT_SUPPORT, assertion.getRealm() + " " + e.getMessage());
            status = AssertionStatus.FAILED;
        }

        if (kerberosServiceTicket == null) {
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.INFO, "Kerberos service ticket cannot be obtained for service principal " + spn);
            }
            logAndAudit(AssertionMessages.KA_FAILED_TO_OBTAIN_KERBEROS_TICKET, spn);
            return AssertionStatus.FALSIFIED;
        }

        LoginCredentials lc = LoginCredentials.makeLoginCredentials(new KerberosAuthenticationSecurityToken(kerberosServiceTicket), assertion.getClass());
        context.addCredentials(lc);
        logger.log(Level.FINE, "Added kerberos credentials to authentication context. Service Principal: " + kerberosServiceTicket.getServicePrincipalName() + ", Client Principal: " + kerberosServiceTicket.getClientPrincipalName());
        logAndAudit(AssertionMessages.KA_ADDED_KERBEROS_CREDENTIALS, kerberosServiceTicket.getServicePrincipalName(), kerberosServiceTicket.getClientPrincipalName());

        return status;
    }

    protected static boolean containRealm(final String spn) {
        Matcher m = KerberosAuthenticationAssertion.spnPattern.matcher(spn);
        if(m.find()) {
            return m.groupCount() == 4 && StringUtils.isNotEmpty(m.group(4));
        }
        return false;
    }

    protected String getServicePrincipal(String realm) throws KerberosException {
        return KerberosClient.getKerberosAcceptPrincipal(realm, true);
    }


    private boolean matchLoginCredentials(LoginCredentials pc, AuthenticationResult result) {
        boolean match = true;
        for(SecurityToken securityToken : pc.getSecurityTokens())
            match &= result.matchesSecurityToken(securityToken);
        if(match)
            return true;
        return false;
    }
}
