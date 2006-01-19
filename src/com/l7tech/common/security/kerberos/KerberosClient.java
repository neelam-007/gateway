package com.l7tech.common.security.kerberos;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import sun.security.krb5.EncryptionKey;
import sun.security.krb5.KrbApReq;

/**
 * Represents a client of the kerberos key distribution center.
 *
 * <p>The KDC client can be either the client (bridge) or server (gateway)</p>
 *
 * <p>The clients kerberos credentials will either be contextual (e.g. the
 * bridge uses the Windows Local Security Authority (LSA)) or explicit (e.g.
 * the bridges configured password or the gateways configured kerberos keytab)</p>
 *
 * TODO investigate ways to avoid using sun.* classes
 *
 * @author $Author$
 * @version $Revision$
 */
public class KerberosClient {

    //- PUBLIC

    /**
     *
     */
    public KerberosClient() {
        kerberosSubject = new Subject();
    }

    /**
     *
     */
    public static String getGSSServiceName() {
        return getServiceName() + "@" + getHostName();
    }

    /**
     *
     */
    public static String getServiceName() {
        return System.getProperty(KERBEROS_SERVICE_NAME_PROPERTY, KERBEROS_SERVICE_NAME_DEFAULT);
    }

    /**
     *
     */
    public static String getHostName() {
        return System.getProperty(KERBEROS_HOST_NAME_PROPERTY, KERBEROS_HOST_NAME_DEFAULT);
    }

    /**
     * If you want to supply a username / password to get the TGT then add a
     * callback handler.
     */
    public void setCallbackHandler(CallbackHandler handler) {
        this.callbackHandler = handler;
    }

    /**
     * Used to get a ticket for initialization of a session.
     *
     * @param servicePrincipalName the name of the service
     * @return the ticket
     * @throws KerberosException on error
     */
    public KerberosServiceTicket getKerberosServiceTicket(final String servicePrincipalName) throws KerberosException {
        KerberosServiceTicket ticket = null;
        try {
            LoginContext loginContext = new LoginContext(LOGIN_CONTEXT_INIT, kerberosSubject, callbackHandler);
            loginContext.login();
            ticket = (KerberosServiceTicket) Subject.doAs(kerberosSubject, new PrivilegedExceptionAction(){
                public Object run() throws Exception {
                    Oid kerberos5Oid = getKerberos5Oid();
                    GSSManager manager = GSSManager.getInstance();

                    GSSCredential credential = manager.createCredential(null, GSSCredential.DEFAULT_LIFETIME, kerb5Oid, GSSCredential.INITIATE_ONLY);
                    GSSName serviceName = manager.createName(servicePrincipalName, GSSName.NT_HOSTBASED_SERVICE, kerb5Oid);

                    GSSContext context = manager.createContext(serviceName, kerb5Oid, credential, GSSContext.DEFAULT_LIFETIME);
                    context.requestMutualAuth(false);
                    context.requestAnonymity(false);
                    context.requestConf(false);
                    context.requestInteg(true);
                    context.requestCredDeleg(false);

                    byte[] bytes = context.initSecContext(new byte[0], 0, 0);

                    KerberosTicket ticket = getTicket(kerberosSubject.getPrivateCredentials(), serviceName, manager);

                    return new KerberosServiceTicket(ticket.getClient().getName(),
                                                     servicePrincipalName,
                                                     ticket.getSessionKey().getEncoded(),
                                                     System.currentTimeMillis() + (context.getLifetime() * 1000L),
                                                     new KerberosGSSAPReqTicket(bytes));
                }
            });
            try{
                loginContext.logout();
            }
            catch(LoginException le) {
                //whatever.
            }
        }
        catch(LoginException le) {
            throw new KerberosException("Could not login", le);
        }
        catch(PrivilegedActionException pae) {
            throw new KerberosException("Error creating Kerberos Service Ticket", pae.getCause());
        }

        return ticket;
    }

    /**
     * Used to get a ticket in acceptance of a session.
     *
     * @return the ticket
     * @throws KerberosException on error
     */
    public KerberosServiceTicket getKerberosServiceTicket(final String servicePrincipalName, final KerberosGSSAPReqTicket gssAPReqTicket) throws KerberosException {
        KerberosServiceTicket ticket = null;
        try {
            LoginContext loginContext = new LoginContext(LOGIN_CONTEXT_ACCEPT, kerberosSubject, getServerCallbackHandler(servicePrincipalName));
            loginContext.login();
            ticket = (KerberosServiceTicket) Subject.doAs(kerberosSubject, new PrivilegedExceptionAction(){
                public Object run() throws Exception {
                    Oid kerberos5Oid = getKerberos5Oid();
                    GSSManager manager = GSSManager.getInstance();
                    GSSName serviceName = manager.createName(servicePrincipalName, GSSName.NT_HOSTBASED_SERVICE, kerb5Oid);
                    GSSCredential scred = manager.createCredential(serviceName, GSSCredential.INDEFINITE_LIFETIME, kerb5Oid, GSSCredential.ACCEPT_ONLY);
                    GSSContext scontext = manager.createContext(scred);

                    byte[] apReqBytes = new sun.security.util.DerValue(gssAPReqTicket.getTicketBody()).toByteArray();
                    KerberosKey key = getKey(kerberosSubject.getPrivateCredentials());
                    KrbApReq apReq = new KrbApReq(apReqBytes, new EncryptionKey[]{new EncryptionKey(key.getKeyType(),key.getEncoded())});
                    EncryptionKey sessionKey = (EncryptionKey) apReq.getCreds().getSessionKey();
                    EncryptionKey subKey = apReq.getSubKey();

                    byte[] keyBytes = (subKey==null ? sessionKey : subKey).getBytes();

                    return new KerberosServiceTicket(apReq.getCreds().getClient().getName(),
                                                     servicePrincipalName,
                                                     keyBytes,
                                                     apReq.getCreds().getEndTime().getTime(),
                                                     gssAPReqTicket);
                }
            });
            try{
                loginContext.logout();
            }
            catch(LoginException le) {
                //whatever.
            }
        }
        catch(LoginException le) {
            throw new KerberosException("Could not login", le);
        }
        catch(PrivilegedActionException pae) {
            throw new KerberosException("Error creating Kerberos Service Ticket", pae.getCause());
        }

        return ticket;
    }

    //- PACKAGE

    static Oid getKerberos5Oid() throws KerberosException {
        if(kerb5Oid==null) {
            try {
                kerb5Oid = new Oid(KERBEROS_5_OID);
            }
            catch(GSSException gsse) {
                throw new KerberosException("Could not create Oid for Kerberos v5 '"+KERBEROS_5_OID+"'", gsse);
            }
        }
        return kerb5Oid;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(KerberosClient.class.getName());

    private static final String KERBEROS_SERVICE_NAME_PROPERTY = "com.l7tech.common.security.kerberos.service";
    private static final String KERBEROS_SERVICE_NAME_DEFAULT = "http";

    private static final String KERBEROS_HOST_NAME_PROPERTY = "com.l7tech.common.security.kerberos.host";
    private static final String KERBEROS_HOST_NAME_DEFAULT = "ssg";

    private static final String KERBEROS_5_OID = "1.2.840.113554.1.2.2";
    private static final String LOGIN_CONTEXT_INIT = "com.sun.security.jgss.initiate";
    private static final String LOGIN_CONTEXT_ACCEPT = "com.sun.security.jgss.accept";

    private static Oid kerb5Oid;

    private final Subject kerberosSubject;
    private CallbackHandler callbackHandler;

    /**
     * Get a ticket from the given set of Objects that is for the given service.
     *
     * @return
     */
    private KerberosTicket getTicket(Set info, GSSName service, GSSManager manager) throws IllegalStateException {
        KerberosTicket ticket = null;

        for (Iterator iterator = info.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            if(o instanceof KerberosTicket) {
                KerberosTicket currTicket = (KerberosTicket) o;

                try {
                    GSSName ticketServicePrincialName = manager.createName(currTicket.getServer().getName(), GSSName.NT_USER_NAME);

                    if(service.equals(ticketServicePrincialName)) {
                        ticket = currTicket;
                        break;
                    }
                }
                catch(GSSException gsse) {
                    logger.log(Level.WARNING, "Error checking kerberos ticket '"+currTicket+"'", gsse);
                }
            }
        }

        if(ticket==null) throw new IllegalStateException("Ticket not found!");

        return ticket;
    }

    /**
     *
     */
    private KerberosKey getKey(Set creds) throws IllegalStateException {
        KerberosKey privateKey = null;

        for (Iterator iterator = creds.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            if(o instanceof KerberosKey) privateKey = (KerberosKey) o;
        }

        if(privateKey==null) throw new IllegalStateException("Private Kerberos key not found!");

        return privateKey;
    }

    /**
     *
     */
    private CallbackHandler getServerCallbackHandler(final String servicePrincipalName) {
        return new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (int i = 0; i < callbacks.length; i++) {
                    Callback callback = callbacks[i];
                    if(callback instanceof NameCallback) {
                        NameCallback nameCallback = (NameCallback) callback;
                        nameCallback.setName(servicePrincipalName.replace('@','/')); //conv from GSS to kerberos name
                    }
                }
            }
        };
    }

    //- TESTING

    /**
     * Example for (client) creation of kerberos token and (server) validation.
     *
     * -Djava.security.auth.login.config=file:/home/steve/HEAD/UneasyRooster/temp/loginconfig.txt -Djavax.security.auth.useSubjectCredsOnly=false -Djava.security.krb5.conf=/home/steve/HEAD/UneasyRooster/temp/krb5.conf -Dsun.security.krb5.debug=true
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        KerberosClient client = new KerberosClient();
        client.setCallbackHandler(new com.sun.security.auth.callback.TextCallbackHandler());
        KerberosServiceTicket clientKST = client.getKerberosServiceTicket("http@gateway.qawin2003.com");

        KerberosClient serverClient = new KerberosClient();
        KerberosServiceTicket serverKST = serverClient.getKerberosServiceTicket("http@gateway.qawin2003.com", clientKST.getGSSAPReqTicket());

        System.out.println("ClientKST: " + clientKST);
        System.out.println("ServerKST: " + serverKST);
    }

    /**
     * Debug function, remove later
     */
    private static void dumpCreds(String name, Set creds) {
        System.out.println(name + " credentials.");
        if(creds.isEmpty()) System.out.println("NO CREDS!");
        for (Iterator iterator = creds.iterator(); iterator.hasNext();) {
            Object o =  iterator.next();
            if(o instanceof KerberosTicket) {
                KerberosTicket ticket = (KerberosTicket) o;
                System.out.println("Kerberos Ticket:");
                System.out.println("  Client:" + ticket.getClient());
                System.out.println("  Server:" + ticket.getServer());
                System.out.println("  Session Key Type: " + ticket.getSessionKeyType());
            }
            else {
                System.out.println("Class: " + o.getClass().getName());
                System.out.println(o);
            }
        }
    }
}
