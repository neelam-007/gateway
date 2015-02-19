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

    private static final KerberosCacheManager cacheManager = KerberosCacheManager.getInstance();

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

            KerberosPrincipal krbPrincipal = getKerberosPrincipalFromSubject(krbSubject);
            String principalName = krbPrincipal.getName();
            String realm = krbPrincipal.getRealm();


            ticket = getKerberosSelfServiceTicket(principalName, krbSubject, behalfOf, realm);
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

    private KerberosPrincipal getKerberosPrincipalFromSubject(Subject krbSubject) {
        KerberosPrincipal krbPrincipal = null;
        for (Principal principal : krbSubject.getPrincipals()) {
            if (principal instanceof KerberosPrincipal) {
                krbPrincipal = (KerberosPrincipal)principal;
                break;
            }
        }
        return krbPrincipal;
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

            KerberosPrincipal krbPrincipal = getKerberosPrincipalFromSubject(krbSubject);

            ticket = getKerberosSelfServiceTicket(keyTabPrincipal, krbSubject, behalfOf, krbPrincipal.getRealm());

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
            if (ticket != null && cacheSubject == null)
                ticketCache.add(ticketCacheKey, kerberosSubject, loginContext, ticket);

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

                //if the user realm is not the same as key
                PrincipalName servicePrincipalName = new PrincipalName(servicePrincipal);
                String targetPrincipal = KRB_SEVICE + "/" + userRealm;
                PrincipalName targetPrincipalName = new PrincipalName(targetPrincipal, servicePrincipalName.getRealmAsString());

                //Service -> TGS A
                //The service sends a request to its TGS, TGS A, for a TGT to TGS B. No S4U2self information is included in this request.
                //TGS A responds with the cross-realm TGT to TGS B. If TGS B was not the user's realm but was instead just a realm closer,
                // then the service would send a KRB_TGS_REQ message to TGS B to get a TGT to the next realm.
                KerberosTicket referralTGT = getReferralTGT(tgt, targetPrincipalName);
                Credentials referralTGTCred = Krb5Util.ticketToCreds(referralTGT);
                subject.getPrivateCredentials().clear();
                subject.getPrivateCredentials().add(referralTGT);

                int referralCount = 0;

                List<String> referralChain = new ArrayList<String>();
                while (referralCount < maxReferral) {
                    String serverRealm = referralTGTCred.getServer().getNameStrings()[1];
                    referralChain.add(serverRealm);
                    //If the TGS was not the user's realm but was instead just a realm closer.
                    if (!userRealm.equalsIgnoreCase(serverRealm)) {

                        targetPrincipalName = new PrincipalName(targetPrincipal, serverRealm);
                        referralTGT = getReferralTGT(referralTGT, targetPrincipalName );
                        referralTGTCred = Krb5Util.ticketToCreds(referralTGT);
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
                subject.getPrivateCredentials().clear();
                subject.getPrivateCredentials().add(tgt);

                return getKerberosProxyServiceTicket(servicePrincipal,getPrincipalName(behalfOf, userRealm), subject, s4uSelfServiceTicket.getDelegatedKerberosTicket());
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
     * @param kerberosTicket        The user service ticket.
     * @return The Delegated service ticket for the target service on behalf of a user.
     * @throws KerberosException
     */
    public KerberosServiceTicket getKerberosProxyServiceTicket(final String servicePrincipalName, final String keyTabPrincipal, final Object kerberosTicket, final PrincipalName userPrincipalName)
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

            ticket = getKerberosProxyServiceTicket(servicePrincipalName, userPrincipalName, krbSubject, kerberosTicket);

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
    public KerberosServiceTicket getKerberosProxyServiceTicket(final String servicePrincipalName, final String accountName, final String accountPasswd, final Object additionalTicket, PrincipalName clientPrincipalName)
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

            ticket = getKerberosProxyServiceTicket(servicePrincipalName, clientPrincipalName, krbSubject, additionalTicket);

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
    public KerberosServiceTicket getKerberosProxyServiceTicketWithCredentials(final String servicePrincipalName, final String accountName, final String accountPasswd, final String behalfOf)
            throws KerberosException {
        try {
            KerberosServiceTicket s4uSelfServiceTicket = getKerberosSelfServiceTicket(servicePrincipalName, accountName, accountPasswd, behalfOf);
            return getKerberosProxyServiceTicket(servicePrincipalName, accountName, accountPasswd, s4uSelfServiceTicket.getDelegatedKerberosTicket(), getPrincipalName(behalfOf, null));

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
    public KerberosServiceTicket getKerberosProxyServiceTicketWithKeytab(final String servicePrincipalName, final String keyTabPrincipal, final String behalfOf)
            throws KerberosException {
        try {
            KerberosServiceTicket s4uSelfServiceTicket = getKerberosSelfServiceTicket(keyTabPrincipal, behalfOf);
            return getKerberosProxyServiceTicket(servicePrincipalName, keyTabPrincipal, s4uSelfServiceTicket.getDelegatedKerberosTicket(), getPrincipalName(behalfOf, null));

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

                KerberosTicket s4u2SelfServiceTicket = getS4U2SelfTicket(tgt, ssgPrincipal, behalfOf, userRealm);

                Credentials s4u2SelfCred = Krb5Util.ticketToCreds(s4u2SelfServiceTicket);

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
     * @param clientPrincipal
     *@param subject          The logon subject, a forwardable tgt should included in the subject.
     * @param additionalTicket    The user service ticket.   @return The Delegated service ticket for the target service on behalf of a user.
     * @throws KerberosException
     */
    private KerberosServiceTicket getKerberosProxyServiceTicket(final String servicePrincipal, final PrincipalName clientPrincipal, final Subject subject, final Object additionalTicket) throws PrivilegedActionException {

        KerberosServiceTicket ticket = Subject.doAs(subject, new PrivilegedExceptionAction<KerberosServiceTicket>() {
            @Override
            public KerberosServiceTicket run() throws Exception {

                KerberosTicket tgt = getTgt(subject);

                //Get s4u2proxy service ticket
                KerberosTicket s4u2ProxyServiceTicket = getS4U2ProxyTicket(tgt, servicePrincipal, clientPrincipal, additionalTicket);

                Credentials s4u2ProxyCred = Krb5Util.ticketToCreds(s4u2ProxyServiceTicket);

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
    protected KerberosTicket getS4U2SelfTicket(KerberosTicket tgt, String servicePrincipal, String user, String userRealm) throws KrbException, IOException {
        PrincipalName clientPrincipalName = getPrincipalName(user, userRealm);

        KerberosTicket kerberosTicket = cacheManager.getKerberosTicket(clientPrincipalName, tgt);
        if (kerberosTicket == null) {
            if(userRealm == null || tgt.getClient().getRealm().equalsIgnoreCase(userRealm)) {
                //This is preferred way to acquire Delegated tickets because it does not rely on any internal implementation
                //However, JDK8 does not support referrals so it only works when the user is in the same realm and the target service
                Credentials s4u2SelfCred = Credentials.acquireS4U2selfCreds(clientPrincipalName, Krb5Util.ticketToCreds(tgt));
                kerberosTicket = Krb5Util.credsToTicket(s4u2SelfCred);
            }
            else {
                PrincipalName serverprincipalName = new PrincipalName(servicePrincipal);
                DelegateKrbTgsReq request = new DelegateKrbTgsReq(Krb5Util.ticketToCreds(tgt), serverprincipalName, clientPrincipalName);
                kerberosTicket = Krb5Util.credsToTicket(request.getCreds());
            }
            cacheManager.store(clientPrincipalName, tgt, kerberosTicket);
        }
        return kerberosTicket;
    }

    public PrincipalName getPrincipalName(String user, String userRealm) throws RealmException {
        PrincipalName clientPrincipalName;
        if (userRealm != null) {
            clientPrincipalName = new PrincipalName(user, userRealm);
        } else {
            clientPrincipalName = new PrincipalName(user);
        }
        return clientPrincipalName;
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
    protected KerberosTicket getReferralTGT(KerberosTicket tgt, PrincipalName servicePrincipal) throws KrbException, IOException {

        KerberosTicket kerberosTicket = cacheManager.getKerberosTicket(servicePrincipal, tgt);
        if (kerberosTicket == null) {
            ReferralKrbTgsReq request = new ReferralKrbTgsReq(Krb5Util.ticketToCreds(tgt),servicePrincipal);
            kerberosTicket = Krb5Util.credsToTicket(request.getCreds());
            cacheManager.store(servicePrincipal, tgt, kerberosTicket);
        }
        return kerberosTicket;

    }

    /**
     * Populate the TGS_REQ message, send to KDC, and retrieve the Service Ticket
     *
     * @param tgt The TGT
     * @param servicePrincipalName The target service principal name
     * @param o The forwardable service ticket
     * @return The service ticket as Credentials on behalf of the user
     * @throws KrbException
     * @throws IOException
     */
    protected KerberosTicket getS4U2ProxyTicket(KerberosTicket tgt, String servicePrincipalName, PrincipalName clientPrincipal, Object o) throws KrbException, IOException {
        KerberosTicket kerberosTicket = null;
        Ticket serviceTicket = null;
        PrincipalName sname = new PrincipalName(servicePrincipalName);
        //Detect ticket type and convert it accordingly
        if(o instanceof KerberosTicket) {
            serviceTicket = new Ticket(((KerberosTicket)o).getEncoded());
        }
        else if (o instanceof Ticket) {
            serviceTicket = (Ticket)o;
        }
        kerberosTicket = cacheManager.getKerberosTicket(sname, o);
        if (kerberosTicket == null) {
            Credentials credentials = Credentials.acquireS4U2proxyCreds(servicePrincipalName, serviceTicket, clientPrincipal, Krb5Util.ticketToCreds(tgt));
            kerberosTicket = Krb5Util.credsToTicket(credentials);

            if(serviceTicket == null) throw new KrbException("Invalid Kerberos ticket type");
            cacheManager.store(sname, o, kerberosTicket);
        }

        return kerberosTicket;
    }

}
