package com.l7tech.server.security.kerberos;

import com.l7tech.kerberos.*;
import org.ietf.jgss.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * User: vchan
 */
public class KerberosRoutingClient extends KerberosClient {

    private static final Logger logger = Logger.getLogger(KerberosRoutingClient.class.getName());

    /**
     * Default Constructor.
     */
    public KerberosRoutingClient() {
        super();
    }

    /**
     * Create a delegated kerberos service ticket for the service principal specified
     * using the incoming service ticket.
     *
     * @param servicePrincipalName name of the service principal
     * @param kerberosServiceTicket the request service ticket to base the delegated creds on
     * @return the ticket
     * @throws KerberosException on error
     */
    public KerberosServiceTicket getKerberosServiceTicket(final String servicePrincipalName, final KerberosServiceTicket kerberosServiceTicket) throws KerberosException {
        KerberosServiceTicket ticket = null;
        try {
            if (!KerberosConfig.hasKeytab()) {
                throw new KerberosConfigException("No Keytab (Kerberos not configured)");
            }

            // check for a cached service ticket
            final KerberosTicketRepository.Key ticketCacheKey =
                    ticketCache.generateKey(servicePrincipalName, kerberosServiceTicket.getClientPrincipalName());

            final KerberosTicket creds = kerberosServiceTicket.getDelegatedKerberosTicket();
            if (creds == null) {
                logger.info("KTKT: " + com.l7tech.util.HexUtils.encodeBase64(kerberosServiceTicket.getGSSAPReqTicket().toByteArray(), true));
                throw new KerberosException("Credentials not available.");
            }

            try {
                final Oid kerberos5Oid = getKerberos5Oid();
                final GSSManager manager = GSSManager.getInstance();
                final GSSName clientName = manager.createName(kerberosServiceTicket.getClientPrincipalName(), GSSName.NT_USER_NAME, kerberos5Oid);
                final Subject delegationSubject = new Subject(false,
                        Collections.singleton(new KerberosPrincipal(kerberosServiceTicket.getClientPrincipalName())),
                        Collections.EMPTY_SET,
                        Collections.singleton(creds));

                ticket = (KerberosServiceTicket) Subject.doAs(delegationSubject, new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        GSSContext context = null;
                        GSSCredential credential = null;
                        try {
                            credential = manager.createCredential(clientName, GSSCredential.DEFAULT_LIFETIME, kerberos5Oid, GSSCredential.INITIATE_ONLY);
                            GSSName serviceName = manager.createName(servicePrincipalName, GSSName.NT_HOSTBASED_SERVICE, kerberos5Oid);
                            context = manager.createContext(serviceName, kerberos5Oid, credential, getKerberosTicketLifetime());

                            context.requestMutualAuth(false);
                            context.requestConf(true);
                            context.requestCredDeleg(true);

                            byte[] bytes = context.initSecContext(new byte[0], 0, 0);

                            KerberosTicket ticket = getTicket(delegationSubject.getPrivateCredentials(), serviceName, manager);

                            KerberosGSSAPReqTicket apReq = new KerberosGSSAPReqTicket(bytes);
                            KerberosServiceTicket kst = new KerberosServiceTicket(ticket.getClient().getName(),
                                                             servicePrincipalName,
                                                             ticket.getSessionKey().getEncoded(),
                                                             System.currentTimeMillis() + (context.getLifetime() * 1000L),
                                                             apReq);

                            apReq.setServiceTicket(kst);

                            return kst;
                        }
                        finally {
                            if(context!=null) context.dispose();
                            if(credential!=null) credential.dispose();
                        }
                    }
                });

            }
            catch(SecurityException se) {
                throw new KerberosConfigException("Kerberos configuration error.", se);
            }

            // create a new cache entry
            if (ticket != null)
                ticketCache.add(ticketCacheKey, creds, null, ticket);

        }
        catch(GSSException gsse) {
            throw new KerberosConfigException("Error creating Kerberos Service Ticket.", gsse);
        }
        catch(PrivilegedActionException pae) {
            Throwable cause = pae.getCause();
            if (cause instanceof KerberosException) { // then don't wrap, just re-throw
                throw (KerberosException) cause;
            }
            throw new KerberosException("Error creating Kerberos Service Ticket", cause);
        }

        return ticket;
    }

    /**
     * Create a Kerberos service ticket for the specified host/server using a pre-configured
     * account/passwd pair.
     *
     * @param host the hostname of the target service
     * @param accountName the configured account name
     * @param accountPasswd the configured account password
     * @return a service ticket that can be used to call the target service
     * @throws KerberosException when the kerberos authentication or service ticket provisioning fails
     */
    public KerberosServiceTicket getKerberosServiceTicket(final URL host, final String accountName, final String accountPasswd)
        throws KerberosException
    {
        KerberosServiceTicket ticket = null;
        LoginContext loginContext = null;
        KerberosTicketRepository.Key ticketCacheKey = null;
        Subject cacheSubject = null;
        
        try {
            if (!KerberosConfig.hasKeytab()) {
                throw new KerberosConfigException("No Keytab (Kerberos not configured)");
            }

            final Oid kerberos5Oid = getKerberos5Oid();
            final GSSManager manager = GSSManager.getInstance();
            final GSSName clientName = manager.createName(accountName, GSSName.NT_USER_NAME, kerberos5Oid);
            final String gssServiceName = getGSSServiceName(host);

            // check for a cached service ticket
            ticketCacheKey = ticketCache.generateKey(gssServiceName, accountName);

            // check cache for TGT (ticket-granting-ticket)
            cacheSubject = ticketCache.getSubject(ticketCacheKey);

            // authenticate for real if cached subject isn't found
            final Subject krbSubject;
            if (cacheSubject == null) {
                loginContext = loginGatewayConfiguredSubject(accountName, accountPasswd);
                krbSubject = kerberosSubject;
            } else {
                krbSubject = cacheSubject;
            }

            // acquire service ticket
            ticket = (KerberosServiceTicket) Subject.doAs(krbSubject, new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    GSSContext context = null;
                    GSSCredential credential = null;
                    try {
                        credential = manager.createCredential(clientName, GSSCredential.DEFAULT_LIFETIME, kerberos5Oid, GSSCredential.INITIATE_ONLY);
                        GSSName serviceName = manager.createName(gssServiceName, GSSName.NT_HOSTBASED_SERVICE, kerberos5Oid);
                        context = manager.createContext(serviceName, kerberos5Oid, credential, getKerberosTicketLifetime());

                        context.requestMutualAuth(false);
                        context.requestConf(true);
                        context.requestCredDeleg(true);

                        byte[] bytes = context.initSecContext(new byte[0], 0, 0);

                        KerberosTicket ticket = getTicket(krbSubject.getPrivateCredentials(), serviceName, manager);

                        KerberosGSSAPReqTicket apReq = new KerberosGSSAPReqTicket(bytes);
                        KerberosServiceTicket kst = new KerberosServiceTicket(ticket.getClient().getName(),
                                                         accountName,
                                                         ticket.getSessionKey().getEncoded(),
                                                         System.currentTimeMillis() + (context.getLifetime() * 1000L),
                                                         apReq);

                        apReq.setServiceTicket(kst);

                        return kst;
                    }
                    finally {
                        if(context!=null) context.dispose();
                        if(credential!=null) credential.dispose();
                    }
                }
            });

            // create a new cache entry
            if (ticket != null && cacheSubject == null)
                ticketCache.add(ticketCacheKey, kerberosSubject, loginContext, ticket);
        }
        catch(LoginException le) {
            throw new KerberosConfigException("Unable to login using gateway configured account.", le);
        }
        catch(SecurityException se) {
            throw new KerberosConfigException("Kerberos configuration error.", se);
        }
        catch(GSSException gsse) {
            throw new KerberosConfigException("Error creating Kerberos Service Ticket.", gsse);
        }
        catch(PrivilegedActionException pae) {

            // if we used cached credentials, discard it
            if (cacheSubject != null && ticketCacheKey != null) {
                ticketCache.remove(ticketCacheKey);
            }

            Throwable cause = pae.getCause();
            if (cause instanceof KerberosException) { // then don't wrap, just re-throw
                throw (KerberosException) cause;
            }
            throw new KerberosException("Error creating Kerberos Service Ticket", cause);
        }

        return ticket;
    }

    private LoginContext loginGatewayConfiguredSubject(final String accountName, final String accountPasswd)
        throws LoginException
    {
        LoginContext loginCtx = new LoginContext(LOGIN_CONTEXT_OUT_CONFIG_ACCT, kerberosSubject, new CallbackHandler() {

            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

                for( Callback cb : callbacks ) {

                    if (cb instanceof NameCallback) {
                        NameCallback name = (NameCallback) cb;
                        name.setName( accountName );

                    } else if (cb instanceof PasswordCallback) {
                        PasswordCallback passwd = (PasswordCallback) cb;
                        passwd.setPassword( accountPasswd.toCharArray() );
                    }
                }

            }
        });

        loginCtx.login();
        return loginCtx;
    }

}
