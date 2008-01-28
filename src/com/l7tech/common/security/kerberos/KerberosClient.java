package com.l7tech.common.security.kerberos;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Principal;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KerberosPrincipal;
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
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.common.util.ExceptionUtils;

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
     * Get the default GSS service name.
     *
     * @return the name
     */
    public static String getGSSServiceName() {
        return getGSSServiceName(getServiceName(), getHostName());
    }

    /**
     * Get the GSS service name for the given service and host.
     *
     * @param service the service name, if null the default is used.
     * @param host the host name, if null the default is used.
     * @return the name
     */
    public static String getGSSServiceName(String service, String host) {
        String serviceToUse = service != null ?  service : getServiceName();
        String hostToUse = host != null ?  host : getHostName();

        return serviceToUse + "@" + hostToUse;
    }

    /**
     * Get the Kerberos service principal name for the given service and host.
     *
     * @param service the service name, if null the default is used.
     * @param host the host name, if null the default is used.
     * @return the name
     */
    public static String getServicePrincipalName(String service, String host) {
        String serviceToUse = service != null ?  service : getServiceName();
        String hostToUse = host != null ?  host : getHostName();

        return serviceToUse + "/" + hostToUse;
    }

    /**
     * Get the name of the HTTP service for the given host.
     *
     * <p>This name is suitable for use with SPNEGO (Simple and Protected
     * GSS-API NEGOtiation).</p>
     *
     * @param protectedUrl the url of the Kerberos protected service.
     * @return the name
     * @throws NullPointerException if the given url is null
     * @throws IllegalArgumentException if the given url does not have a host
     */
    public static String getGSSServiceName(URL protectedUrl) {
        if(protectedUrl==null) throw new NullPointerException("protectedUrl must not be null.");

        String host = protectedUrl.getHost();
        if(host==null) throw new IllegalArgumentException("The url must contain a host name.");

        return getGSSServiceName(SERVICE_NAME_SPNEGO, host.toLowerCase().trim());
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
        KerberosServiceTicket ticket;
        try {
            LoginContext loginContext = new LoginContext(LOGIN_CONTEXT_INIT, kerberosSubject);
            try {
                loginContext.login();
            }
            catch(LoginException le) {
                // if there is no available ticket cache try the other module
                loginContext = new LoginContext(LOGIN_CONTEXT_INIT_CREDS, kerberosSubject, callbackHandler);
                loginContext.login();
            }

            // disable inspection until we can use 1.6 api
            //noinspection unchecked
            ticket = (KerberosServiceTicket) Subject.doAs(kerberosSubject, new PrivilegedExceptionAction(){
                public KerberosServiceTicket run() throws Exception {
                    Oid kerberos5Oid = getKerberos5Oid();
                    GSSManager manager = GSSManager.getInstance();

                    GSSCredential credential = null;
                    GSSContext context = null;
                    try {
                        credential = manager.createCredential(null, GSSCredential.DEFAULT_LIFETIME, kerberos5Oid, GSSCredential.INITIATE_ONLY);
                        String gssPrincipal = KerberosUtils.toGssName(servicePrincipalName);
                        GSSName serviceName = manager.createName(gssPrincipal, GSSName.NT_HOSTBASED_SERVICE, kerberos5Oid);
                        if (logger.isLoggable(Level.FINE))
                            logger.log(Level.FINE, "GSS name is '"+gssPrincipal+"'/'"+serviceName.canonicalize(kerberos5Oid)+"'.");

                        context = manager.createContext(serviceName, kerberos5Oid, credential, KERBEROS_LIFETIME);
                        context.requestMutualAuth(false);
                        context.requestConf(true);

                        byte[] bytes = context.initSecContext(new byte[0], 0, 0);

                        KerberosTicket ticket = getTicket(kerberosSubject.getPrivateCredentials(), serviceName, manager);

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
            throw new KerberosException("Error creating Kerberos Service Ticket for service '"+servicePrincipalName+"'", pae.getCause());
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
        KerberosServiceTicket ticket;
        try {
            if (!KerberosConfig.hasKeytab()) {
                throw new KerberosConfigException("No Keytab (Kerberos not configured)");
            }

            LoginContext loginContext = new LoginContext(LOGIN_CONTEXT_ACCEPT, kerberosSubject, getServerCallbackHandler(servicePrincipalName));
            loginContext.login();
            // disable inspection until we can use 1.6 api
            //noinspection unchecked
            ticket = (KerberosServiceTicket) Subject.doAs(kerberosSubject, new PrivilegedExceptionAction(){
                public KerberosServiceTicket run() throws Exception {
                    Oid kerberos5Oid = getKerberos5Oid();
                    GSSManager manager = GSSManager.getInstance();
                    GSSCredential scred = null;
                    GSSContext scontext = null;
                    try {
                        String gssPrincipal = KerberosUtils.toGssName(servicePrincipalName);
                        GSSName serviceName = manager.createName(gssPrincipal, GSSName.NT_HOSTBASED_SERVICE, kerberos5Oid);
                        if (logger.isLoggable(Level.FINE))
                            logger.log(Level.FINE, "GSS name is '"+gssPrincipal+"'/'"+serviceName.canonicalize(kerberos5Oid)+"'.");
                        scred = manager.createCredential(serviceName, GSSCredential.INDEFINITE_LIFETIME, kerberos5Oid, GSSCredential.ACCEPT_ONLY);
                        scontext = manager.createContext(scred);

                        byte[] apReqBytes = new sun.security.util.DerValue(gssAPReqTicket.getTicketBody()).toByteArray();
                        KerberosKey[] keys = getKeys(kerberosSubject.getPrivateCredentials());
                        KrbApReq apReq = new KrbApReq(apReqBytes, toEncryptionKey(keys));
                        validateServerPrincipal(kerberosSubject.getPrincipals(), new KerberosPrincipal(apReq.getCreds().getServer().getName()) );
                        EncryptionKey sessionKey = apReq.getCreds().getSessionKey();
                        EncryptionKey subKey = apReq.getSubKey();

                        byte[] keyBytes = (subKey==null ? sessionKey : subKey).getBytes();

                        return new KerberosServiceTicket(apReq.getCreds().getClient().getName(),
                                                         gssPrincipal,
                                                         keyBytes,
                                                         apReq.getCreds().getEndTime().getTime(),
                                                         gssAPReqTicket);
                    }
                    finally {
                        if(scontext!=null) scontext.dispose();
                        if(scred!=null) scred.dispose();
                    }
                }
            });
            try{
                loginContext.logout();
            }
            catch(LoginException le) {
                //whatever.
            }
        }
        catch(SecurityException se) {
            throw new KerberosConfigException("Kerberos configuration error.", se);
        }
        catch(LoginException le) {
            throw new KerberosException("Could not login", le);
        }
        catch(PrivilegedActionException pae) {
            Throwable cause = pae.getCause();
            if (cause instanceof KerberosException) { // the don't wrap, just re-throw
                throw (KerberosException) cause;
            }
            throw new KerberosException("Error creating Kerberos Service Ticket", cause);
        }

        return ticket;
    }

    /**
     * Check if the current client login configuration is valid.
     *
     * @return the client principal name (e.g test01@QAWIN2003.COM)
     * @throws KerberosException on error
     */
    public String getKerberosInitPrincipal() throws KerberosException {
        String name;
        try {
            LoginContext loginContext = new LoginContext(LOGIN_CONTEXT_INIT, kerberosSubject);
            try {
                loginContext.login();
            }
            catch(LoginException le) {
                // if there is no available ticket cache try the other module
                loginContext = new LoginContext(LOGIN_CONTEXT_INIT_CREDS, kerberosSubject, callbackHandler);
                loginContext.login();
            }

            // disable inspection until we can use 1.6 api
            //noinspection unchecked
            name = (String) Subject.doAs(kerberosSubject, new PrivilegedExceptionAction(){
                public String run() throws Exception {
                    String name = null;
                    for( Principal principal : kerberosSubject.getPrincipals() ) {
                        if( principal instanceof KerberosPrincipal ) {
                            name = principal.getName();
                            break;
                        }
                    }
                    return name;
                }
            });
            try{
                loginContext.logout();
            }
            catch(LoginException le) {
                //whatever.
            }
        }
        catch(SecurityException se) {
            throw new KerberosConfigException("Kerberos configuration error.", se);
        }
        catch(LoginException le) {
            throw new KerberosException("Could not login", le);
        }
        catch(PrivilegedActionException pae) {
            throw new KerberosException("Error getting principal.", pae.getCause());
        }

        if (name == null) {
            throw new KerberosException("Error getting principal.");
        }

        return name;
    }

    /**
     * Get the Keytab file used on the server.
     *
     * <p>If there is no configured Keytab this will return null.</p>
     *
     * @return The Keytab or null
     * @throws KerberosException if the Keytab is invalid.
     */
    public static Keytab getKerberosAcceptPrincipalKeytab() throws KerberosException {
        return KerberosConfig.getKeytab(true);
    }

    /**
     * Check if the current server login configuration is valid.
     *
     * @return the service principal name (e.g http/gateway.l7tech.com@QAWIN2003.COM)
     * @throws KerberosException on error
     */
    public static String getKerberosAcceptPrincipal(final boolean initiator) throws KerberosException {
        String aPrincipal = acceptPrincipal;
        if (aPrincipal != null) {
            if (!KerberosConfig.hasKeytab()) throw new KerberosConfigException("Not configured");

            if(aPrincipal.length()==0) {
                throw new KerberosException("Principal detection failed.");
            }
            else {
                return aPrincipal;
            }
        }

        try {
            if (KerberosConfig.hasKeytab()) {
                String spn = KerberosConfig.getKeytabPrincipal();

                final Subject kerberosSubject = new Subject();
                String contextName = initiator ? LOGIN_CONTEXT_ACCEPT_INIT : LOGIN_CONTEXT_ACCEPT;
                LoginContext loginContext = new LoginContext(contextName, kerberosSubject, getServerCallbackHandler(spn));
                loginContext.login();
                // disable inspection until we can use 1.6 api
                //noinspection unchecked
                aPrincipal = (String) Subject.doAs(kerberosSubject, new PrivilegedExceptionAction(){
                    public String run() throws Exception {
                        String name = null;

                        try {
                            getKeys(kerberosSubject.getPrivateCredentials());
                        }
                        catch(IllegalStateException ise) {
                            throw new KerberosException("No kerberos key in private credentials.");
                        }

                        for( Principal principal : kerberosSubject.getPrincipals() ) {
                            if( principal instanceof KerberosPrincipal ) {
                                name = principal.getName();
                                break;
                            }
                        }

                        return name;
                    }
                });
                try{
                    loginContext.logout();
                }
                catch(LoginException le) {
                    //whatever.
                }
            }
            else {
                throw new KerberosConfigException("Not configured");
            }
        }
        catch(SecurityException se) {
            throw new KerberosConfigException("Kerberos configuration error '"+ ExceptionUtils.getMessage(se)+"'.", se);
        }
        catch(LoginException le) {
            throw new KerberosException("Could not login '"+ ExceptionUtils.getMessage(le)+"'.", le);
        }
        catch(PrivilegedActionException pae) {
            throw new KerberosException("Error getting principal '"+ ExceptionUtils.getMessage(pae.getCause())+"'.", pae.getCause());
        }

        if (aPrincipal != null) {
            acceptPrincipal = aPrincipal; // cache success
        }
        else {
            acceptPrincipal = ""; // cache failure
            throw new KerberosException("Error getting principal.");
        }

        return aPrincipal;
    }


    //- PACKAGE

    static Oid getKerberos5Oid() throws KerberosException {
        Oid k5oid = kerb5Oid;
        if(k5oid==null) {
            try {
                k5oid = new Oid(KERBEROS_5_OID);
            }
            catch(GSSException gsse) {
                throw new KerberosException("Could not create Oid for Kerberos v5 '"+KERBEROS_5_OID+"'", gsse);
            }

            kerb5Oid = k5oid; // stash for later ...
        }
        return k5oid;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(KerberosClient.class.getName());

    private static final String SERVICE_NAME_SPNEGO = "http";

    private static final String KERBEROS_SERVICE_NAME_PROPERTY = "com.l7tech.common.security.kerberos.service";
    private static final String KERBEROS_SERVICE_NAME_DEFAULT = SERVICE_NAME_SPNEGO;

    private static final String KERBEROS_HOST_NAME_PROPERTY = "com.l7tech.common.security.kerberos.host";
    private static final String KERBEROS_HOST_NAME_DEFAULT = "ssg";

    private static final String KERBEROS_5_OID = "1.2.840.113554.1.2.2";
    private static final String LOGIN_CONTEXT_INIT = "com.l7tech.common.security.kerberos.initiate";
    private static final String LOGIN_CONTEXT_INIT_CREDS = "com.l7tech.common.security.kerberos.initiate.callback";
    private static final String LOGIN_CONTEXT_ACCEPT = "com.l7tech.common.security.kerberos.accept";
    private static final String LOGIN_CONTEXT_ACCEPT_INIT = "com.l7tech.common.security.kerberos.acceptinit";

    private static final String KERBEROS_LIFETIME_PROPERTY = "com.l7tech.common.security.kerberos.lifetime";
    private static final Integer KERBEROS_LIFETIME_DEFAULT = 60 * 15; // seconds
    private static final Integer KERBEROS_LIFETIME = SyspropUtil.getInteger(KERBEROS_LIFETIME_PROPERTY, KERBEROS_LIFETIME_DEFAULT);

    private static Oid kerb5Oid;
    private static String acceptPrincipal;

    private final Subject kerberosSubject;
    private CallbackHandler callbackHandler;

    static {
        KerberosConfig.checkConfig();
    }

    /**
     * Get a ticket from the given set of Objects that is for the given service.
     */
    private KerberosTicket getTicket(Set info, GSSName service, GSSManager manager) throws IllegalStateException {
        KerberosTicket ticket = null;

        for( Object o : info ) {
            if( o instanceof KerberosTicket ) {
                KerberosTicket currTicket = (KerberosTicket) o;

                try {
                    GSSName ticketServicePrincialName = manager.createName( currTicket.getServer().getName(), GSSName.NT_USER_NAME );

                    if( service.equals( ticketServicePrincialName ) ) {
                        ticket = currTicket;
                        break;
                    }
                }
                catch( GSSException gsse ) {
                    logger.log( Level.WARNING, "Error checking kerberos ticket '" + currTicket + "'", gsse );
                }
            }
        }

        if(ticket==null) throw new IllegalStateException("Ticket not found! (credsize:"+info.size()+")");

        return ticket;
    }

    /**
     * Ensure that the given principal set contains the principal 
     */
    private static void validateServerPrincipal(final Set<Principal> allowedPrincipals,
                                                final KerberosPrincipal principal) throws KerberosException {
        boolean found = false;

        for ( Principal allowedPrincipal : allowedPrincipals ) {
            if ( allowedPrincipal instanceof KerberosPrincipal ) {
                KerberosPrincipal kerbAllowedPrincipal = (KerberosPrincipal) allowedPrincipal;

                // compare REALMS case sensitively but the rest of the name insensitively
                if ( kerbAllowedPrincipal.getNameType() == principal.getNameType() &&
                     kerbAllowedPrincipal.getRealm().equals( principal.getRealm() ) &&
                     kerbAllowedPrincipal.getName().toLowerCase().equals( principal.getName().toLowerCase() )) {
                    found = true;
                    break;
                }
            }
        }

        if ( !found ) {
            throw new KerberosException("Invalid server principal '"+principal+"'.");
        }
    }

    /**
     *
     */
    private static KerberosKey[] getKeys(Set creds) throws IllegalStateException {
        List<KerberosKey> keys = new ArrayList<KerberosKey>();

        for( Object o : creds ) {
            if( o instanceof KerberosKey ) keys.add( (KerberosKey) o );
        }

        if (keys.isEmpty()) throw new IllegalStateException("Private Kerberos key not found!");

        return keys.toArray(new KerberosKey[keys.size()]);
    }

    /**
     *
     */
    private static EncryptionKey[] toEncryptionKey(KerberosKey[] keys) {
        EncryptionKey[] ekeys = new EncryptionKey[keys.length];

        for (int k=0; k<keys.length; k++) {
            ekeys[k] = new EncryptionKey(keys[k].getKeyType(), keys[k].getEncoded());
        }

        return ekeys;
    }

    /**
     *
     */
    private static CallbackHandler getServerCallbackHandler(final String servicePrincipalName) {
        return new CallbackHandler() {
            public void handle(Callback[] callbacks) {
                for( Callback callback : callbacks ) {
                    if( callback instanceof NameCallback ) {
                        NameCallback nameCallback = (NameCallback) callback;
                        nameCallback.setName( servicePrincipalName );
                        if( logger.isLoggable( Level.FINE ) )
                            logger.log( Level.FINE, "Using kerberos SPN '" + nameCallback.getName() + "'." );
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
     * /
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
     * /
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

    /* */
}
