package com.l7tech.kerberos;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.ietf.jgss.*;
import org.jaaslounge.decoding.DecodingException;
import org.jaaslounge.decoding.kerberos.KerberosEncData;
import org.jaaslounge.decoding.kerberos.KerberosToken;
import sun.security.krb5.*;
import sun.security.krb5.internal.APReq;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabEntry;
import sun.security.util.DerValue;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
@SuppressWarnings({ "UseOfSunClasses" })
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
        String serviceToUse = service;
        if (service != null) {
            if ("https".equals(service.toLowerCase())) {
                serviceToUse = "http";
            }
        } else {
            serviceToUse = getServiceName();
        }
        
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
        return ConfigFactory.getProperty( KERBEROS_SERVICE_NAME_PROPERTY, KERBEROS_SERVICE_NAME_DEFAULT );
    }

    /**
     *
     */
    public static String getHostName() {
        return ConfigFactory.getProperty( KERBEROS_HOST_NAME_PROPERTY, KERBEROS_HOST_NAME_DEFAULT );
    }

    /**
     * If you want to supply a username / password to get the TGT then add a
     * callback handler.
     */
    public void setCallbackHandler(CallbackHandler handler) {
        this.callbackHandler = handler;
    }

    protected GSSManager getGSSManager() {
        return GSSManager.getInstance();
    }

    public KerberosServiceTicket getKerberosServiceTicket(final String servicePrincipalName, boolean isOutboundRouting)
        throws KerberosException {
        return this.getKerberosServiceTicket(servicePrincipalName, servicePrincipalName, isOutboundRouting);
    }

    /**
     * Used to get a ticket for initialization of a session.
     *
     * @param servicePrincipalName the name of the service
     * @param keyTabPrincipal The principal from the keytab file
     * @param isOutboundRouting
     * @return the ticket
     * @throws KerberosException on error
     */
    public KerberosServiceTicket getKerberosServiceTicket(final String servicePrincipalName, final String keyTabPrincipal, boolean isOutboundRouting)
        throws KerberosException
    {
        KerberosServiceTicket ticket;
        LoginContext loginContext = null;
        KerberosTicketRepository.Key ticketCacheKey = null;
        Subject cacheSubject = null;

        try {
            // check against the ticket cache when doing outbound routing
            ticketCacheKey = ticketCache.generateKey(servicePrincipalName, KerberosTicketRepository.KeyType.KEYTAB, "useKeytab", null);
            cacheSubject = ticketCache.getSubject(ticketCacheKey);

            final Subject krbSubject;
            if (cacheSubject != null) {
                krbSubject = cacheSubject;

            } else {
                if (isOutboundRouting) {
                    loginContext  = new LoginContext(LOGIN_CONTEXT_OUT_KEYTAB, kerberosSubject, getServerCallbackHandler(keyTabPrincipal));
                    loginContext.login();
                } else {
                    loginContext  = new LoginContext(LOGIN_CONTEXT_INIT, kerberosSubject);
                    try {
                        loginContext.login();
                    }
                    catch(LoginException le) {
                        // if there is no available ticket cache try the other module
                        loginContext = new LoginContext(LOGIN_CONTEXT_INIT_CREDS, kerberosSubject, callbackHandler);
                        loginContext.login();
                    }
                }

                krbSubject = kerberosSubject;
            }

            ticket = Subject.doAs(krbSubject, new PrivilegedExceptionAction<KerberosServiceTicket>(){
                @Override
                public KerberosServiceTicket run() throws Exception {
                    Oid kerberos5Oid = getKerberos5Oid();
                    GSSManager manager = getGSSManager();

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

                        KerberosTicket ticket = getTicket(krbSubject.getPrivateCredentials(), serviceName, manager);

                        KerberosGSSAPReqTicket apReq = new KerberosGSSAPReqTicket(bytes);

                        KerberosEncData krbEncData = null;
                        try {
                            //get keys
                            KerberosKey[] keys = getKeys(krbSubject.getPrivateCredentials());
                            krbEncData = getKerberosAuthorizationData(keys, apReq);
                        } catch (IllegalStateException e) {
                          //ignore if no keys present
                        }

                        return new KerberosServiceTicket(ticket.getClient().getName(),
                                                         servicePrincipalName,
                                                         ticket.getSessionKey().getEncoded(),
                                                         System.currentTimeMillis() + (context.getLifetime() * 1000L),
                                                         apReq, null, krbEncData);
                    }
                    finally {
                        if(context!=null) context.dispose();
                        if(credential!=null) credential.dispose();
                    }
                }
            });

            // for outbound routing, cache the creds
            if (ticket != null && isOutboundRouting && cacheSubject == null) {
                ticketCache.add(ticketCacheKey, krbSubject, loginContext, ticket);
            }

            try{
                // do not logout when doing outbound routing because the creds are
                // cached for later requests
                if (loginContext != null && !isOutboundRouting)
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

            // if we used cached credentials, discard it
            if (cacheSubject != null && ticketCacheKey != null) {
                ticketCache.remove(ticketCacheKey);
            }

            throw new KerberosException("Error creating Kerberos Service Ticket for service '"+servicePrincipalName+"'", pae.getCause());
        }

        return ticket;
    }

    /**
     * Used to get a ticket in acceptance of a session. Tries to extract Kerberos authorization data if the ticket contains them.
     * Also extracts the delegated creds to build a service ticket that can later be used for delegation during HTTP routing.
     *
     * @return the ticket
     * @throws KerberosException on error
     */
    public KerberosServiceTicket getKerberosServiceTicket( final String servicePrincipalName,
                                                           final InetAddress clientAddress,
                                                           final KerberosGSSAPReqTicket gssAPReqTicket) throws KerberosException {
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
                @Override
                public KerberosServiceTicket run() throws Exception {
                    Oid kerberos5Oid = getKerberos5Oid();
                    GSSManager manager = getGSSManager();
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
                        KrbApReq apReq = buildKrbApReq( apReqBytes, keys, clientAddress );
                        validateServerPrincipal(kerberosSubject.getPrincipals(), new KerberosPrincipal(apReq.getCreds().getServer().getName()) );
                        //extract additional Kerberos Data from the ticket such as PAC Logon Info, PAC Signature, etc.  as per
                        // Utilizing the Windows 2000 Authorization Data in Kerberos Tickets for  Access Control to Resources http://msdn.microsoft.com/en-us/library/aa302203.aspx
                        KerberosEncData krbEncData = getKerberosAuthorizationData(keys, gssAPReqTicket);
                        // Extract the delegated kerberos ticket if one exists
                        EncryptionKey sessionKey = apReq.getCreds().getSessionKey();
                        KerberosTicket delegatedKerberosTicket = extractDelegatedServiceTicket(apReq.getChecksum(), sessionKey);
                        
                        // get the key bytes
 	 	                byte[] keyBytes = sessionKey.getBytes();

                        //The original service ticket
                        DerValue encoding = new DerValue(apReqBytes);
                        APReq ap = new APReq(encoding);
                        // create the service ticket
                        return new KerberosServiceTicket(apReq.getCreds().getClient().getName(),
                                                         gssPrincipal,
                                                         keyBytes,
                                                         apReq.getCreds().getEndTime().getTime(),
                                                         gssAPReqTicket,
                                                         delegatedKerberosTicket, krbEncData, ap.ticket);
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
            if (cause instanceof KerberosException) { // then don't wrap, just re-throw
                throw (KerberosException) cause;
            }

            String clockMsg = null;
            if (cause.getMessage().contains("Clock"))
                clockMsg = ": time synchronization issue";
            throw new KerberosException("Error creating Kerberos Service Ticket"+clockMsg, cause);
        }

        return ticket;
    }


    /**
     * extract additional Kerberos Data from the ticket such as PAC Logon Info, PAC Signature, etc.  as per
     * Utilizing the Windows 2000 Authorization Data in Kerberos Tickets for  Access Control to Resources http://msdn.microsoft.com/en-us/library/aa302203.aspx
     * @param keys  - kerberos keys
     * @param gssAPReqTicket - Kerberos authentication ticket
     * @return
     */
    protected KerberosEncData getKerberosAuthorizationData(KerberosKey[] keys, KerberosGSSAPReqTicket gssAPReqTicket) {
        KerberosEncData krbEncData = null;
        try {
            KerberosKey[] krbKeys = new KerberosKey[keys.length];
            System.arraycopy(keys,0,krbKeys, 0, keys.length);
            KerberosToken krbToken = new KerberosToken(GSSSpnego.removeSpnegoWrapper(gssAPReqTicket.toByteArray()), krbKeys);
            krbEncData = krbToken.getTicket().getEncData(); //extract Kerberos Encripted Data
        } catch (DecodingException e) {
            //Not all tokens include authorization information that jaaslounge-decode library can extract
            logger.log(Level.FINE, "Unable to extract kerberos authorization data from the kerberos ticket: " + e.getMessage());
        }
        return krbEncData;
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
                @Override
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
    public static KeyTab getKerberosAcceptPrincipalKeytab() throws KerberosException {
        return KerberosConfig.getKeytab(true);
    }

    /**
     * Try best to get a valid SPN, first try to retrieve the spn from the provided host and service,
     * if fail, then try to retrieve the default spn (the first entry in the keytab file), if fail,
     * retrieve the system spn.
     *
     * @param service The name of the service
     * @param host The name of the ssg gateway machine
     * @param initiator set this to true, if initiator, set this to false, if acceptor only
     * @return The Service Principal Name
     * @throws KerberosException When error to retrieve a valid kerberos Principal.
     */
    public static String getKerberosAcceptPrincipal(String service, String host, boolean initiator) throws KerberosException {
        String spn = KerberosClient.getServicePrincipalName(service, host);
        try {
            return KerberosClient.getKerberosAcceptPrincipal(spn, initiator);
        } catch(KerberosException ke) {
            // fallback to default spn
            try {
                return KerberosClient.getKerberosAcceptPrincipal(initiator);
            } catch (KerberosException e) {
                throw e;
            }
        }
    }

    /**
     * Retrieve the default principal from the keytab file. The default principal is the first keytab entry in the
     * KeyTab file.
     * @param initiator
     * @return The default principal from the keytab file.
     * @throws KerberosException Fail to retrieve the principal.
     */
    public static String getKerberosAcceptPrincipal(final boolean initiator) throws KerberosException {
        return getKerberosAcceptPrincipal(null, initiator);
    }

    /**
     * Check if the current server login configuration is valid. If the provided principal is not defined in
     * the keytab file, will use the default principal (first entry from the keytab file). If the provided
     * principal is defined in the keytab, will verify the login configuration.
     *
     * @param principal The service principal, the principal my not be a fully qualify service principal
     *                  The service may only contain the service and host name (e.g http/gateway.l7tech.com),
     *                  null for lookup the default qualified service principal name
     * @param initiator set this to true, if initiator, set this to false, if acceptor only
     * @return the fully qualified service principal name (e.g http/gateway.l7tech.com@QAWIN2003.COM)
     * @throws KerberosException on error
     */
    public static String getKerberosAcceptPrincipal(String principal, final boolean initiator) throws KerberosException {
        
        if (!KerberosConfig.hasKeytab()) throw new KerberosConfigException("Not Configured");

        principal = KerberosConfig.getKeytabPrincipal(principal);

        Boolean valid = acceptPrincipalCache.get(principal);
        if (valid != null) {
            if (valid) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log( Level.INFO, "Accept Principal is cached:" + principal );
                }
                return principal;
            } else {
                throw new KerberosException("Principal detection failed.");
            }
        }
        
        String aPrincipal = null;

        try {
            if (KerberosConfig.hasKeytab()) {

                final Subject kerberosSubject = new Subject();
                String contextName = initiator ? LOGIN_CONTEXT_ACCEPT_INIT : LOGIN_CONTEXT_ACCEPT;
                LoginContext loginContext = new LoginContext(contextName, kerberosSubject, getServerCallbackHandler(principal));
                loginContext.login();
                // disable inspection until we can use 1.6 api
                //noinspection unchecked
                aPrincipal = (String) Subject.doAs(kerberosSubject, new PrivilegedExceptionAction(){
                    @Override
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
            acceptPrincipalCache.put(principal, Boolean.TRUE); // cache success
        } else {
            acceptPrincipalCache.put(principal, Boolean.FALSE); // cache failure
            throw new KerberosException("Error getting principal.");
        }

        return aPrincipal;
    }

    /**
     * Validate all entries in the keytab file.
     * @throws KerberosException
     */
    public static void validateKerberosPrincipals() throws KerberosException {
        KeyTab keyTab = KerberosConfig.getKeytab(false);
        KeyTabEntry[] keyTabEntries = keyTab.getEntries();
        for (int i = 0; i < keyTabEntries.length; i++) {
            KeyTabEntry keyTabEntry = keyTabEntries[i];
            getKerberosAcceptPrincipal(keyTabEntry.getService().getName(), true);
        }
    }

    /**
     * Reset any cached information
     */
    public static void reset() {
        if (logger.isLoggable(Level.INFO)) {
            logger.log( Level.INFO, "Reset the accept Principal Cache" );
        }
        acceptPrincipalCache.clear();
    }

    public static Oid getKerberos5Oid() throws KerberosException {
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

    //- PROTECTED

    // used for outbound routing with kerberos
    protected static final String LOGIN_CONTEXT_OUT_KEYTAB = "com.l7tech.common.security.kerberos.outbound.keytab";
    protected static final String LOGIN_CONTEXT_OUT_CONFIG_ACCT = "com.l7tech.common.security.kerberos.outbound.account"; // "com.l7tech.common.security.kerberos.delegation";

    /**
     * Kerberos credentials cache
     */
    protected static final KerberosTicketRepository ticketCache;

    protected final Subject kerberosSubject;
    protected CallbackHandler callbackHandler;

    protected int getKerberosTicketLifetime() {
        return KERBEROS_LIFETIME;
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
    private static final Integer KERBEROS_LIFETIME_DEFAULT = 60 * 60; // in seconds, default ==> 1 hour
    protected static final Integer KERBEROS_LIFETIME = ConfigFactory.getIntProperty( KERBEROS_LIFETIME_PROPERTY, KERBEROS_LIFETIME_DEFAULT );

    private static final String PASS_INETADDR_PROPERTY = "com.l7tech.common.security.kerberos.useaddr";
    private static final boolean PASS_INETADDR = ConfigFactory.getBooleanProperty( PASS_INETADDR_PROPERTY, true );

    private static Oid kerb5Oid;
    private static Map<String, Boolean> acceptPrincipalCache = new ConcurrentHashMap<String, Boolean>();

    static {
        KerberosConfig.checkConfig( null, null, true );
        ticketCache = KerberosTicketRepository.getInstance();
        ticketCache.setKerberosTicketLifetime(KERBEROS_LIFETIME * 1000L);
    }

    /**
     * Get a ticket from the given set of Objects that is for the given service.
     */
    protected KerberosTicket getTicket(Set info, GSSName service, GSSManager manager) throws IllegalStateException {
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

    /** Extracts the delegated creds from the request kerberos ticket and builds a KerberosTicket
 	 * that can be used for delegation (through the HTTP routing assertion).
 	 *
 	 * @param checksum The AP_REQ checksum
 	 * @param key Encryption key
 	 * @return The kerberos ticket that can be used for delegation or null if one is not found
 	 * @throws sun.security.krb5.KrbException if error occurs creating the kerberos credentials for delegation
 	 * @throws java.io.IOException if error occurs creating the kerberos credentials for delegation
 	 */
 	 private KerberosTicket extractDelegatedServiceTicket(Checksum checksum, EncryptionKey key)
 	    throws KrbException, IOException
 	 {
            KerberosTicket delegatedKerberosTicket = null;
 	 	    byte[] checksumBytes = checksum.getBytes();

 	 	    // check the delegateFlag
 	 	    int flags = readInt(checksumBytes, 20);

 	 	    if (checksumBytes.length > 24 && (flags & 2) > 0) {
                // get the length of the creds
                int credLen = readShort(checksumBytes, 26);
                byte[] credBytes = new byte[credLen];
                System.arraycopy(checksumBytes, 28, credBytes, 0, credLen);

                Credentials delegatedCred;
                int etype = key.getEType();
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER,
                        "Encryption type is ''{0}'', size is {1}.",
                        new Object[]{etype, key.getBytes().length*8});
                }
                if (etype == EncryptedData.ETYPE_ARCFOUR_HMAC ||
                    etype == EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96 ||
                    etype == EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96) {
                    delegatedCred = new KrbCred(credBytes, key).getDelegatedCreds()[0];
                } else {
                    delegatedCred = new KrbCred(credBytes, EncryptionKey.NULL_KEY).getDelegatedCreds()[0];
                }
                EncryptionKey delegatedSessionKey = delegatedCred.getSessionKey();

                PrincipalName cPrinc = delegatedCred.getClient();
                KerberosPrincipal client = null;
                if (cPrinc != null) {
                    client =  new KerberosPrincipal(cPrinc.getName());
                }

                PrincipalName sPrinc = delegatedCred.getServer();
                KerberosPrincipal server = null;
                if (sPrinc != null) {
                    server = new KerberosPrincipal(sPrinc.getName());
                }

                delegatedKerberosTicket = new KerberosTicket(
                                                delegatedCred.getEncoded(),
                                                client,
                                                server,
                                                delegatedSessionKey.getBytes(),
                                                delegatedSessionKey.getEType(),
                                                delegatedCred.getFlags(),
                                                delegatedCred.getAuthTime() == null ? new java.util.Date() : delegatedCred.getAuthTime(),
                                                delegatedCred.getStartTime(),
                                                delegatedCred.getEndTime(),
                                                delegatedCred.getRenewTill(),
                                                delegatedCred.getClientAddresses());
 	 	    }
 	 	    return delegatedKerberosTicket;
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
     * Parse out the Kerberos private key(s) from the provide credentials.
	 *
     * @param creds the credentials to extract the private keys from
 	 * @return Array of one or more private keys found in the credentials
 	 * @throws IllegalStateException if the private key cannot be found
     */
    protected static KerberosKey[] getKeys(Set creds) throws IllegalStateException {
        List<KerberosKey> keys = new ArrayList<KerberosKey>();

        for( Object o : creds ) {
            if( o instanceof KerberosKey ) keys.add( (KerberosKey) o );
        }

        if (keys.isEmpty()) throw new IllegalStateException("Private Kerberos key not found!");

        return keys.toArray(new KerberosKey[keys.size()]);
    }

    /**
     * Takes the array of KerberosKeys and converts them into corresponding
     * sun.security.krb5.EncryptionKey objects.
	 *
     * @param keys the Kerberos Keys
 	 * @return Array of EncryptionKeys, one for each Kerberos key in the argument
     */
    private static EncryptionKey[] toEncryptionKey(KerberosKey[] keys) {
        EncryptionKey[] ekeys = new EncryptionKey[keys.length];

        for (int k=0; k<keys.length; k++) {
            ekeys[k] = new EncryptionKey(keys[k].getKeyType(), keys[k].getEncoded());
        }

        return ekeys;
    }

    /**
     * OpenJDK introduced the "InetAddress" argument, so we'll support either
     * the old or the new constructor.
     */
    protected KrbApReq buildKrbApReq( final byte[] apReqBytes,
                                    final KerberosKey[] keys,
                                    final InetAddress clientAddress ) throws KerberosException {
        KrbApReq apReq;

        try {
            Constructor constructor;
            Object[] args;
            try {
                // OpenJDK / new constructor
                constructor = KrbApReq.class.getConstructor( byte[].class, EncryptionKey[].class, InetAddress.class );
                args = new Object[]{ apReqBytes, toEncryptionKey(keys), PASS_INETADDR ? clientAddress : null };
            } catch ( NoSuchMethodException nsme ) {
                // original constructor
                constructor = KrbApReq.class.getConstructor( byte[].class, EncryptionKey[].class );
                args = new Object[]{ apReqBytes, toEncryptionKey(keys) };
            }

            apReq = (KrbApReq) constructor.newInstance( args );
        } catch (NoSuchMethodException e) {
            throw new KerberosException( e );
        } catch (InvocationTargetException e) {
            throw new KerberosException( e );
        } catch (IllegalAccessException e) {
            throw new KerberosException( e );
        } catch (InstantiationException e) {
            throw new KerberosException( e );
        }

        return apReq;
    }

    /**
     * Returns a callback handler used for authenticating the kerberos service principal.
	 *
     * @param servicePrincipalName service principal name to be returned by the callback handler
 	 * @return CallbackHandler instance
     */
    protected static CallbackHandler getServerCallbackHandler(final String servicePrincipalName) {
        return new CallbackHandler() {
            @Override
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

    protected LoginContext loginGatewayConfiguredSubject(final String subject, final String accountName, final String accountPasswd)
            throws LoginException
    {
        LoginContext loginCtx = new LoginContext(subject, kerberosSubject, new CallbackHandler() {

            @Override
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

    /**
 	 * Parse the byte indexed by the offset into a short.
 	 *
 	 * @param data the byte array to read
 	 * @param offset the index into the data bytes
 	 * @return a short value, -1 if the offset is invalid
 	 */
    private static int readShort(byte[] data, int offset) {
        if(data.length < (offset+2))
 	 	    return -1; // throw new IllegalArgumentException("Not enough data to read short!");

        return (data[offset  ]&0xFF)
            | ((data[offset+1]&0xFF) <<  8);
    }

    /**
 	 * Parse the byte indexed by the offset into an int.
 	 *
 	 * @param data the byte array to read
 	 * @param offset the index into the data bytes
 	 * @return an int value, -1 if the offset is invalid
 	 */
    private static int readInt(byte[] data, int offset) {
        if(data.length < (offset+4))
 	 	    return -1; // throw new IllegalArgumentException("Not enough data to read int!");

        return (data[offset  ]&0xFF)
            | ((data[offset+1]&0xFF) <<  8)
            | ((data[offset+2]&0xFF) << 16)
            | ((data[offset+3]&0xFF) << 24);
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
