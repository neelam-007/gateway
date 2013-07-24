package com.l7tech.kerberos.delegate;

import com.l7tech.kerberos.*;
import com.l7tech.kerberos.referral.ReferralKrbTgsReq;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.jaaslounge.decoding.kerberos.KerberosEncData;
import sun.security.jgss.krb5.Krb5Util;
import sun.security.krb5.*;
import sun.security.krb5.internal.Ticket;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

/**
 * Implementation of MS-SFU S4U2Self and S4U2Proxy extension.
 * OpenJDK is planning to implement this MS-SFU extensions in the future,
 * please refer to http://openjdk.java.net/jeps/113 for development status.
 */

public class KerberosDelegateClient extends KerberosClient {

    private static final String LOGIN_CONTEXT_DELEGATE_KEYTAB = "com.l7tech.common.security.kerberos.delegate.keytab";
    private static final String LOGIN_CONTEXT_DELEGATE_ACCT = "com.l7tech.common.security.kerberos.delegate.account";
    private static final String KRB_SEVICE = "krbtgt";

    public KerberosServiceTicket getKerberosSelfServiceTicket(final String servicePrincipalName, final String accountName, final String accountPasswd, final String behalfOf) throws KerberosException {
        KerberosServiceTicket ticket;
        LoginContext loginContext = null;
        KerberosTicketRepository.Key ticketCacheKey = null;
        Subject cacheSubject = null;
        try {
            final Oid kerberos5Oid = getKerberos5Oid();
            final GSSManager manager = GSSManager.getInstance();
            final GSSName clientName = manager.createName(accountName, GSSName.NT_USER_NAME, kerberos5Oid);

            // check for a cached service ticket
            ticketCacheKey = ticketCache.generateKey(servicePrincipalName, KerberosTicketRepository.KeyType.CREDENTIAL, accountName, accountPasswd);

            // check cache for TGT (ticket-granting-ticket)
            cacheSubject = ticketCache.getSubject(ticketCacheKey);

            // authenticate for real if cached subject isn't found
            final Subject krbSubject;
            if (cacheSubject == null) {
                loginContext = loginGatewayConfiguredSubject(LOGIN_CONTEXT_DELEGATE_ACCT, accountName, accountPasswd);
                krbSubject = kerberosSubject;
            } else {
                krbSubject = cacheSubject;
            }

            String principalName = null;
            for (Principal principal : krbSubject.getPrincipals()) {
                if (principal instanceof KerberosPrincipal) {
                    principalName = principal.getName();
                    break;
                }
            }

            ticket = getKerberosSelfServiceTicket(principalName, krbSubject, behalfOf, null);
            // create a new cache entry
            if (ticket != null && cacheSubject == null)
                ticketCache.add(ticketCacheKey, kerberosSubject, loginContext, ticket);
        } catch (LoginException le) {
            throw new KerberosConfigException("Unable to login using gateway configured account.", le);
        } catch (SecurityException se) {
            throw new KerberosConfigException("Kerberos configuration error.", se);
        } catch (GSSException gsse) {
            throw new KerberosConfigException("Error creating Kerberos Service Ticket.", gsse);
        } catch (PrivilegedActionException pae) {

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

    /**
     * Implementation of S4U2Self extension, which allows a service to obtain a service ticket to itself on behalf
     * of a user with keytab principal.
     *
     * @param keyTabPrincipal The service principal in the keytab, the service ticket itself.
     * @param behalfOf        The user in behalf of
     * @return The Kerberos Service Ticket to itself on behalf of the user.
     * @throws KerberosException When fail to obtain the service ticket on behalf of the user.
     */
    public KerberosServiceTicket getKerberosSelfServiceTicket(final String keyTabPrincipal, final String behalfOf)
            throws KerberosException {
        KerberosServiceTicket ticket;
        LoginContext loginContext = null;
        KerberosTicketRepository.Key ticketCacheKey = null;
        Subject cacheSubject = null;

        try {

            ticketCacheKey = ticketCache.generateKey(keyTabPrincipal, KerberosTicketRepository.KeyType.CONSTRAINED_DELEGATED, "useKeytab", null);
            cacheSubject = ticketCache.getSubject(ticketCacheKey);

            final Subject krbSubject;
            if (cacheSubject != null) {
                krbSubject = cacheSubject;
            } else {
                loginContext = new LoginContext(LOGIN_CONTEXT_DELEGATE_KEYTAB, kerberosSubject, getServerCallbackHandler(keyTabPrincipal));
                loginContext.login();
                krbSubject = kerberosSubject;
            }

            ticket = getKerberosSelfServiceTicket(keyTabPrincipal, krbSubject, behalfOf, null);

            if (ticket != null && cacheSubject == null) {
                ticketCache.add(ticketCacheKey, krbSubject, loginContext, ticket);
            }

        } catch (LoginException le) {
            throw new KerberosException("Could not login", le);
        } catch (PrivilegedActionException e) {
            if (cacheSubject != null && ticketCacheKey != null) {
                ticketCache.remove(ticketCacheKey);
            }
            throw new KerberosException("Error creating Self Kerberos Service Ticket for service '" + keyTabPrincipal + "' on behalf of " + behalfOf, e);
        }

        return ticket;
    }

    /**
     * Implementation of S4U2Self Multiple Realm extension, which allows a service to obtain a service ticket to itself on behalf
     * of a user in different realm.
     * Please refer to MS-SFU document Section 4.2 S4U2Self Multiple Realm Example for detail.
     *
     * @param servicePrincipal The service principal
     * @param ssgPrincipal The service principal in the keytab, the service ticket itself.
     * @param user The user behalf of.
     * @param userRealm The user Realm
     * @param maxReferral Maximum referral limit.
     *
     * @return The Kerberos Service Ticket to itself on behalf of the user.
     * @throws KerberosException When fail to obtain the service ticket on behalf of the user.
     */

    public KerberosServiceTicket getKerberosProxyServiceTicketWithReferral(final String servicePrincipal, final String ssgPrincipal, final String user, final String userRealm, final int maxReferral)
            throws KerberosException {
        KerberosServiceTicket ticket;
        LoginContext loginContext = null;
        KerberosTicketRepository.Key ticketCacheKey = null;
        Subject cacheSubject = null;

        try {

            ticketCacheKey = ticketCache.generateKey(ssgPrincipal, KerberosTicketRepository.KeyType.REFERRAL, "useKeytab", null);
            cacheSubject = ticketCache.getSubject(ticketCacheKey);

            //Request for TGT from
            final Subject krbSubject;
            if (cacheSubject != null) {
                krbSubject = cacheSubject;
            } else {
                loginContext = new LoginContext(LOGIN_CONTEXT_DELEGATE_KEYTAB, kerberosSubject, getServerCallbackHandler(ssgPrincipal));
                loginContext.login();
                krbSubject = kerberosSubject;
            }
            ticket = getKerberosProxyServiceTicketWithReferral(servicePrincipal, ssgPrincipal, krbSubject, user, userRealm, maxReferral);

        } catch (LoginException le) {
            throw new KerberosException("Could not login", le);
        } catch (PrivilegedActionException e) {
            // if we used cached credentials, discard it
            if (cacheSubject != null && ticketCacheKey != null) {
                ticketCache.remove(ticketCacheKey);
            }
            Throwable cause = e.getCause();
            if (cause instanceof KerberosException) { // then don't wrap, just re-throw
                throw (KerberosException) cause;
            }
            throw new KerberosException("Error creating Kerberos Service Ticket", cause);
        }

        return ticket;
    }

    /**
     * Implementation of S4U2Self Multiple Realm extension, which allows a service to obtain a service ticket to itself on behalf
     * of a user in different realm.
     * Please refer to MS-SFU document Section 4.2 S4U2Self Multiple Realm Example for detail.
     *
     * @param servicePrincipal The service Principal
     * @param accountName The ssg account name
     * @param accountPasswd The ssg account password
     * @param user The user behalf of.
     * @param userRealm The user Realm
     * @param maxReferral Maximum referral limit.
     *
     * @return The Kerberos Service Ticket to itself on behalf of the user.
     * @throws KerberosException When fail to obtain the service ticket on behalf of the user.
     */

    public KerberosServiceTicket getKerberosProxyServiceTicketWithReferral(final String servicePrincipal, final String accountName, final String accountPasswd, final String user, final String userRealm, final int maxReferral)
            throws KerberosException {
        KerberosServiceTicket ticket;
        LoginContext loginContext = null;
        KerberosTicketRepository.Key ticketCacheKey = null;
        Subject cacheSubject = null;
        try {
            final Oid kerberos5Oid = getKerberos5Oid();
            final GSSManager manager = GSSManager.getInstance();
            final GSSName clientName = manager.createName(accountName, GSSName.NT_USER_NAME, kerberos5Oid);

            // check for a cached service ticket
            ticketCacheKey = ticketCache.generateKey(servicePrincipal, KerberosTicketRepository.KeyType.CREDENTIAL, accountName, accountPasswd);

            // check cache for TGT (ticket-granting-ticket)
            cacheSubject = ticketCache.getSubject(ticketCacheKey);

            // authenticate for real if cached subject isn't found
            final Subject krbSubject;
            if (cacheSubject == null) {
                loginContext = loginGatewayConfiguredSubject(LOGIN_CONTEXT_DELEGATE_ACCT, accountName, accountPasswd);
                krbSubject = kerberosSubject;
            } else {
                krbSubject = cacheSubject;
            }

            String principalName = null;
            for (Principal principal : krbSubject.getPrincipals()) {
                if (principal instanceof KerberosPrincipal) {
                    principalName = principal.getName();
                    break;
                }
            }

            ticket = getKerberosProxyServiceTicketWithReferral(servicePrincipal, principalName, krbSubject, user, userRealm, maxReferral);
            // create a new cache entry
            if (ticket != null && cacheSubject == null)
                ticketCache.add(ticketCacheKey, kerberosSubject, loginContext, ticket);
        } catch (LoginException le) {
            throw new KerberosConfigException("Unable to login using gateway configured account.", le);
        } catch (SecurityException se) {
            throw new KerberosConfigException("Kerberos configuration error.", se);
        } catch (GSSException gsse) {
            throw new KerberosConfigException("Error creating Kerberos Service Ticket.", gsse);
        } catch (PrivilegedActionException pae) {

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
    /**
     * Implementation of S4U2Self extension, which allows a service to obtain a service ticket to itself on behalf
     * of a user with keytab principal.
     *
     * @param servicePrincipal The self service principal
     * @param subject          The logon subject, a forwardable tgt should included in the subject.
     * @param behalfOf         The user on behalf of
     * @return The Delegated service ticket for the target service on behalf of a user.
     * @throws PrivilegedActionException When unable to obtain the service ticket on behlaf of user.
     */
    private KerberosServiceTicket getKerberosProxyServiceTicketWithReferral(final String servicePrincipal, final String ssgPrincipal, final Subject subject, final String behalfOf, final String userRealm, final int maxReferral) throws PrivilegedActionException {
        KerberosServiceTicket ticket;
        ticket = Subject.doAs(subject, new PrivilegedExceptionAction<KerberosServiceTicket>() {
            @Override
            public KerberosServiceTicket run() throws Exception {

                KerberosTicket tgt = getTgt(subject);
                Credentials tgtCredentials = Krb5Util.ticketToCreds(tgt);

                //if the user realm is not the same as key
                String targetPrincipal = KRB_SEVICE + "/" + userRealm;

                //Service -> TGS A
                //The service sends a request to its TGS, TGS A, for a TGT to TGS B. No S4U2self information is included in this request.
                //TGS A responds with the cross-realm TGT to TGS B. If TGS B was not the user's realm but was instead just a realm closer,
                // then the service would send a KRB_TGS_REQ message to TGS B to get a TGT to the next realm.
                Credentials referralTGTCred = getReferralTGT(tgtCredentials, targetPrincipal);
                KerberosTicket referralTGT = Krb5Util.credsToTicket(referralTGTCred);
                subject.getPrivateCredentials().clear();
                subject.getPrivateCredentials().add(referralTGT);

                int referralCount = 0;

                List<String> referralChain = new ArrayList<String>();
                while (referralCount < maxReferral) {
                    String serverRealm = referralTGTCred.getServer().getNameStrings()[1];
                    referralChain.add(serverRealm);
                    //If the TGS was not the user's realm but was instead just a realm closer.
                    if (!userRealm.equalsIgnoreCase(serverRealm)) {
                        referralTGTCred = getReferralTGT(referralTGTCred, targetPrincipal + "@" + serverRealm );
                        referralTGT = Krb5Util.credsToTicket(referralTGTCred);
                        subject.getPrivateCredentials().clear();
                        subject.getPrivateCredentials().add(referralTGT);
                        referralCount++;
                        continue;
                    }
                    break;
                }

                if (referralCount == maxReferral) {
                    throw new KerberosException("User's realm not found. Max referral limit reached.");
                }

                //SSG -> TGS B
                //The service now uses the TGT to TGS B to make the S4U2self request in the KRB_TGS_REQ.
                //The service uses the PA-FOR-USER padata-type field in the request to indicate the user information in the S4U2self request.
                //TGS B creates a PAC with the user's authorization information (as specified in [MS-PAC] section 3) and
                //returns it in a TGT referral in the KRB_TGS_REP message. TGS B cannot create the service ticket.
                //TGS B does not possess the service's account information, because the service is part of the realm served by TGS A.
                //If there are more TGSs involved in the referral chain, repeated the chain.
                Collections.reverse(referralChain);
                for (String serverRealm: referralChain) {
                    KerberosServiceTicket nextTGT = getKerberosSelfServiceTicket(servicePrincipal + "@" + serverRealm, subject, behalfOf, userRealm);
                    referralTGT = nextTGT.getDelegatedKerberosTicket();
                    subject.getPrivateCredentials().clear();
                    subject.getPrivateCredentials().add(referralTGT);
                }


                //SSG -> TGS A
                //The server uses the TGT from the referral and uses the PA-FOR-USER padata-type to request the service ticket to itself on behalf of the user.
                KerberosServiceTicket s4uSelfServiceTicket = getKerberosSelfServiceTicket(ssgPrincipal, subject, behalfOf, userRealm);
                Ticket s4u2SelfTicket = new Ticket(s4uSelfServiceTicket.getDelegatedKerberosTicket().getEncoded());
                subject.getPrivateCredentials().clear();
                subject.getPrivateCredentials().add(tgt);
                return getKerberosProxyServiceTicket(servicePrincipal, subject, s4u2SelfTicket);
            }

        });
        return ticket;

    }
    /**
     * Implementation for Service-for-User-to-Proxy extension, which provides a service that obtains a service ticket
     * to another service on behalf of a user.
     *
     * @param servicePrincipalName The target service principal name.
     * @param keyTabPrincipal      The keytab principal name
     * @param serviceTicket        The user service ticket.
     * @return The Delegated service ticket for the target service on behalf of a user.
     * @throws KerberosException
     */
    public KerberosServiceTicket getKerberosProxyServiceTicket(final String servicePrincipalName, final String keyTabPrincipal, final Ticket serviceTicket)
            throws KerberosException {
        KerberosServiceTicket ticket;
        LoginContext loginContext = null;
        KerberosTicketRepository.Key ticketCacheKey = null;
        Subject cacheSubject = null;

        try {

            ticketCacheKey = ticketCache.generateKey(keyTabPrincipal, KerberosTicketRepository.KeyType.CONSTRAINED_DELEGATED, "useKeytab", null);
            cacheSubject = ticketCache.getSubject(ticketCacheKey);

            final Subject krbSubject;
            if (cacheSubject != null) {
                krbSubject = cacheSubject;
            } else {
                loginContext = new LoginContext(LOGIN_CONTEXT_DELEGATE_KEYTAB, kerberosSubject, getServerCallbackHandler(keyTabPrincipal));
                loginContext.login();
                krbSubject = kerberosSubject;
            }

            ticket = getKerberosProxyServiceTicket(servicePrincipalName, krbSubject, serviceTicket);

            if (ticket != null && cacheSubject == null) {
                ticketCache.add(ticketCacheKey, krbSubject, loginContext, ticket);
            }

        } catch (LoginException le) {
            throw new KerberosException("Could not login", le);
        } catch (PrivilegedActionException e) {
            if (cacheSubject != null && ticketCacheKey != null) {
                ticketCache.remove(ticketCacheKey);
            }
            throw new KerberosException("Error creating Proxy Kerberos Service Ticket for service '" + servicePrincipalName, e);
        }

        return ticket;
    }

    /**
     * Create a Kerberos service ticket for the specified host/server using a pre-configured
     * account/passwd pair.
     *
     * @param servicePrincipalName the hostname of the target service
     * @param accountName          the configured account name
     * @param accountPasswd        the configured account password
     * @return a service ticket that can be used to call the target service
     * @throws KerberosException when the kerberos authentication or service ticket provisioning fails
     */
    public KerberosServiceTicket getKerberosProxyServiceTicket(final String servicePrincipalName, final String accountName, final String accountPasswd, final Ticket serviceTicket)
            throws KerberosException {
        KerberosServiceTicket ticket;
        LoginContext loginContext = null;
        KerberosTicketRepository.Key ticketCacheKey = null;
        Subject cacheSubject = null;

        try {

            final Oid kerberos5Oid = getKerberos5Oid();
            final GSSManager manager = GSSManager.getInstance();
            final GSSName clientName = manager.createName(accountName, GSSName.NT_USER_NAME, kerberos5Oid);

            // check for a cached service ticket
            ticketCacheKey = ticketCache.generateKey(servicePrincipalName, KerberosTicketRepository.KeyType.CREDENTIAL, accountName, accountPasswd);

            // check cache for TGT (ticket-granting-ticket)
            cacheSubject = ticketCache.getSubject(ticketCacheKey);

            // authenticate for real if cached subject isn't found
            final Subject krbSubject;
            if (cacheSubject == null) {
                loginContext = loginGatewayConfiguredSubject(LOGIN_CONTEXT_DELEGATE_ACCT, accountName, accountPasswd);
                krbSubject = kerberosSubject;
            } else {
                krbSubject = cacheSubject;
            }

            ticket = getKerberosProxyServiceTicket(servicePrincipalName, krbSubject, serviceTicket);

            // create a new cache entry
            if (ticket != null && cacheSubject == null)
                ticketCache.add(ticketCacheKey, kerberosSubject, loginContext, ticket);
        } catch (LoginException le) {
            throw new KerberosConfigException("Unable to login using gateway configured account.", le);
        } catch (SecurityException se) {
            throw new KerberosConfigException("Kerberos configuration error.", se);
        } catch (GSSException gsse) {
            throw new KerberosConfigException("Error creating Kerberos Service Ticket.", gsse);
        } catch (PrivilegedActionException pae) {

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

    /**
     * Create a Kerberos service ticket for the specified host/server using a pre-configured
     * account/passwd pair.
     *
     * @param servicePrincipalName the hostname of the target service
     * @param accountName          the configured account name
     * @param accountPasswd        the configured account password
     * @return a service ticket that can be used to call the target service
     * @throws KerberosException when the kerberos authentication or service ticket provisioning fails
     */
    public KerberosServiceTicket getKerberosProxyServiceTicket(final String servicePrincipalName, final String accountName, final String accountPasswd, final String behalfOf)
            throws KerberosException {
        try {
            KerberosServiceTicket s4uSelfServiceTicket = getKerberosSelfServiceTicket(servicePrincipalName, accountName, accountPasswd, behalfOf);
            Ticket s4u2SelfTicket = new Ticket(s4uSelfServiceTicket.getDelegatedKerberosTicket().getEncoded());
            return getKerberosProxyServiceTicket(servicePrincipalName, accountName, accountPasswd, s4u2SelfTicket);

        } catch (Exception e) {
            throw new KerberosException(e);
        }
    }

    /**
     * Implementation of Constrained Delegation. first obtain a service ticket to itself on behalf of a user, then pass
     * the self ticket to S4U2Proxy to obtain a service ticket to another service on behalf of the user.
     *
     * @param servicePrincipalName The target service principal name
     * @param keyTabPrincipal      The keytab principal name
     * @param behalfOf             The user on behalf of
     * @return The Delegated service ticket for the target service ticket on behalf of a user.
     * @throws KerberosException When fail to obtain the service ticket on behalf of the user.
     */
    public KerberosServiceTicket getKerberosProxyServiceTicket(final String servicePrincipalName, final String keyTabPrincipal, final String behalfOf)
            throws KerberosException {
        try {
            KerberosServiceTicket s4uSelfServiceTicket = getKerberosSelfServiceTicket(keyTabPrincipal, behalfOf);
            Ticket s4u2SelfTicket = new Ticket(s4uSelfServiceTicket.getDelegatedKerberosTicket().getEncoded());
            return getKerberosProxyServiceTicket(servicePrincipalName, keyTabPrincipal, s4u2SelfTicket);

        } catch (Exception e) {
            throw new KerberosException(e);
        }
    }

    /**
     * Implementation of S4U2Self extension, which allows a service to obtain a service ticket to itself on behalf
     * of a user with keytab principal.
     *
     * @param ssgPrincipal The self service principal
     * @param subject          The logon subject, a forwardable tgt should included in the subject.
     * @param behalfOf         The user on behalf of
     * @return The Delegated service ticket for the target service on behalf of a user.
     * @throws PrivilegedActionException When unable to obtain the service ticket on behlaf of user.
     */
    private KerberosServiceTicket getKerberosSelfServiceTicket(final String ssgPrincipal, final Subject subject, final String behalfOf, final String userRealm) throws PrivilegedActionException {
        KerberosServiceTicket ticket;
        ticket = Subject.doAs(subject, new PrivilegedExceptionAction<KerberosServiceTicket>() {
            @Override
            public KerberosServiceTicket run() throws Exception {

                KerberosTicket tgt = getTgt(subject);
                Credentials tgtCredentials = Krb5Util.ticketToCreds(tgt);

                Credentials s4u2SelfCred = getS4U2SelfCred(tgtCredentials, ssgPrincipal, behalfOf, userRealm);

                KerberosTicket s4u2SelfServiceTicket = Krb5Util.credsToTicket(s4u2SelfCred);

                KrbApReq apReq = new KrbApReq(s4u2SelfCred, false, false, false, null);

                KerberosGSSAPReqTicket gssapReqTicket = new KerberosGSSAPReqTicket(apReq.getMessage());

                KerberosEncData krbEncData = null;
                try {
                    //get keys
                    KerberosKey[] keys = getKeys(subject.getPrivateCredentials());
                    krbEncData = getKerberosAuthorizationData(keys, gssapReqTicket);
                } catch (IllegalStateException e) {
                    //ignore if no keys present
                }

                return new KerberosServiceTicket(s4u2SelfServiceTicket.getClient().getName(),
                        ssgPrincipal,
                        s4u2SelfServiceTicket.getSessionKey().getEncoded(),
                        s4u2SelfCred.getEndTime().getTime(),
                        gssapReqTicket, s4u2SelfServiceTicket, krbEncData);

            }

        });
        return ticket;

    }

    /**
     * Implementation for Service-for-User-to-Proxy extension, which provides a service that obtains a service ticket
     * to another service on behalf of a user.
     *
     * @param servicePrincipal The target service principal name.
     * @param subject          The logon subject, a forwardable tgt should included in the subject.
     * @param serviceTicket    The user service ticket.
     * @return The Delegated service ticket for the target service on behalf of a user.
     * @throws KerberosException
     */
    private KerberosServiceTicket getKerberosProxyServiceTicket(final String servicePrincipal, final Subject subject, final Ticket serviceTicket) throws PrivilegedActionException {

        KerberosServiceTicket ticket = Subject.doAs(subject, new PrivilegedExceptionAction<KerberosServiceTicket>() {
            @Override
            public KerberosServiceTicket run() throws Exception {

                KerberosTicket tgt = getTgt(subject);
                Credentials tgtCredentials = Krb5Util.ticketToCreds(tgt);

                //Get s4u2proxy service ticket
                Credentials s4u2ProxyCred = getS4U2ProxyCred(tgtCredentials, servicePrincipal, serviceTicket);

                KerberosTicket s4u2ProxyServiceTicket = Krb5Util.credsToTicket(s4u2ProxyCred);

                KrbApReq apReq = new KrbApReq(s4u2ProxyCred, false, false, false, null);

                KerberosGSSAPReqTicket gssapReqTicket = new KerberosGSSAPReqTicket(apReq.getMessage());

                KerberosEncData krbEncData = null;
                try {
                    //get keys
                    KerberosKey[] keys = getKeys(subject.getPrivateCredentials());
                    krbEncData = getKerberosAuthorizationData(keys, gssapReqTicket);
                } catch (IllegalStateException e) {
                    //ignore if no keys present
                }
                return new KerberosServiceTicket(s4u2ProxyServiceTicket.getClient().getName(),
                        servicePrincipal,
                        s4u2ProxyServiceTicket.getSessionKey().getEncoded(),
                        s4u2ProxyCred.getEndTime().getTime(),
                        gssapReqTicket, null, krbEncData);
            }
        });
        return ticket;

    }

    /**
     * Obtain the TGT from the Subject.
     *
     * @param subject The subject for the principal and associated credentials
     * @return The TGT
     * @throws KerberosException When no TGT is found from the Subject.
     */
    private KerberosTicket getTgt(Subject subject) throws KerberosException {
        Set<Object> s = subject.getPrivateCredentials();
        for (Iterator<Object> iterator = s.iterator(); iterator.hasNext(); ) {
            Object next = iterator.next();
            if (next instanceof KerberosTicket) {
                return (KerberosTicket) next;
            }
        }
        throw new KerberosException("Error getting TGT.");
    }

    /**
     * Populate the TGS_REQ message, send to KDC, and retrieve the Service Ticket
     *
     * @param tgt The TGT
     * @param servicePrincipal The self service principal name
     * @param user the user on behalf of.
     * @return The service ticket as Credentials on behalf of the user.
     * @throws KrbException
     * @throws IOException
     */
    protected Credentials getS4U2SelfCred(Credentials tgt, String servicePrincipal, String user, String userRealm) throws KrbException, IOException {
        PrincipalName clientPrincipalName = null;
        if (userRealm != null) {
            clientPrincipalName = new PrincipalName(user, userRealm);
        } else {
            clientPrincipalName = new PrincipalName(user);
        }
        PrincipalName serverprincipalName = new PrincipalName(servicePrincipal);

        DelegateKrbTgsReq request = new DelegateKrbTgsReq(tgt, serverprincipalName, clientPrincipalName);
        return request.getCreds();
    }

    /**
     * Retrieve the Referral TGT
     *
     * @param tgt The TGT
     * @param servicePrincipal There service principal name
     * @return
     * @throws KrbException
     * @throws IOException
     */
    protected Credentials getReferralTGT(Credentials tgt, String servicePrincipal) throws KrbException, IOException {

        PrincipalName serverprincipalName = new PrincipalName(servicePrincipal);

        ReferralKrbTgsReq request = new ReferralKrbTgsReq(tgt,serverprincipalName);
        return request.getCreds();
    }

    /**
     * Populate the TGS_REQ message, send to KDC, and retrieve the Service Ticket
     *
     * @param tgt The TGT
     * @param servicePrincipalName The target service principal name
     * @param serviceTicket The forwardable service ticket
     * @return The service ticket as Credentials on behalf of the user
     * @throws KrbException
     * @throws IOException
     */
    protected Credentials getS4U2ProxyCred(Credentials tgt, String servicePrincipalName, Ticket serviceTicket) throws KrbException, IOException {
        PrincipalName sname = new PrincipalName(servicePrincipalName);
        Ticket[] tickets = new Ticket[] {serviceTicket};
        DelegateKrbTgsReq req = new DelegateKrbTgsReq(tgt, sname, tickets);
        return req.getCreds();
    }

}
