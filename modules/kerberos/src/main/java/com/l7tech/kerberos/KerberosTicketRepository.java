package com.l7tech.kerberos;

import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.Subject;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores Kerberos tickets that can be re-used for subsequent calls to the same service.
 *
 * @author : vchan
 */
public class KerberosTicketRepository {

    private static final Logger logger = Logger.getLogger(KerberosTicketRepository.class.getName());

    /**
     * Singleton instance for the KerberosTicketRepository class.
     */
    private static KerberosTicketRepository instance;

    /**
     * Hashmap used to store the cached credentials.
     */
    private HashMap<String, CachedCredential> _map;

    /**
     * Maintenance thread for removing old tickets.
     */
    private ExecutorService maintenanceThread = Executors.newFixedThreadPool(1);

    /**
     * Kerberos ticket lifetime for cached entries (ms) - this parameter is configued by a the system property.
     *
     * @see com.l7tech.kerberos.KerberosClient
     */
    private Long kerberosTicketLifetime;

    /**
     * Hidden default constructor.
     */
    private KerberosTicketRepository() {
        super();
        _map = new HashMap<String, CachedCredential>();
    }

    /**
     * Returns the singleton instance of the Krb ticket repository.
     *
     * @return the single instance of this class
     */
    public static KerberosTicketRepository getInstance() {

        if (instance == null) {
            instance = new KerberosTicketRepository();
        }

        return instance;
    }

    /**
     * Returns a Subject for the given key if it exists in the cache.
     *
     * @param key the key to lookup the cache ticket
     * @return the subject for the cached principal and associated credentials
     */
    public Subject getSubject(Key key) {

        String keyVal = key.toHashkey();
        CachedCredential cred = getElement(keyVal);
        if (cred != null) {

            // need to check that
            if (cred.hasValidTgTicket()) {
                return new Subject(false,
                        Collections.singleton(new KerberosPrincipal(cred.principal)),
                        Collections.EMPTY_SET,
                        Collections.singleton(cred.tgTicket));

            } else {
                // toss cached credentials
                _map.remove(keyVal);
                cred.discard();
            }
        }

        return null;
    }

    /**
     * Adds the Kerberos credentials into the cache.
     *
     * @param key the key to use
     * @param tgTicket the Kerberos ticket to get the creds from
     * @param loginCtx the loginContext associated with the ticket
     * @param svcTicket the service ticket created based on the creds
     */
    public void add(Key key, KerberosTicket tgTicket, LoginContext loginCtx, KerberosServiceTicket svcTicket) {

        String keyVal = key.toHashkey();

        if (_map.containsKey(keyVal)) {
            // just replace the old credentials with the new
            CachedCredential oldCred = _map.get(keyVal);
            _map.remove(keyVal);
            oldCred.discard();
        }

        // create a new cache value
        _map.put( keyVal, new CachedCredential(tgTicket, loginCtx, svcTicket.getServicePrincipalName()) );

        // perform cleanup when necessary
        if (canRunCleanup())
            runCleanupTask();
    }

    /**
     * Adds the Kerberos credentials into the cache.
     *
     * @param key the key to use
     * @param subject the authenticated subject to extract the Kerberos ticket from
     * @param loginCtx the loginContext associated with the subject/ticket
     * @param svcTicket the service ticket created based on the creds
     */
    public void add(Key key, Subject subject, LoginContext loginCtx, KerberosServiceTicket svcTicket) {

        // parse out the kerberos ticket from the subject
        Iterator<?> it = subject.getPrivateCredentials().iterator();
        KerberosTicket tgTicket = null;
        for (;it.hasNext();) {
            // only extract the TGT
            Object val = it.next();
            if (val instanceof KerberosTicket &&
                    KerberosTicket.class.cast(val).getServer().getName().contains("krbtgt")) {

                tgTicket = (KerberosTicket) val;
                break;
            }
        }

        if (tgTicket != null)
            this.add(key, tgTicket, loginCtx, svcTicket);
        else
            logger.log(Level.FINE, "Could not extract KerberosTicket for caching ({0})", svcTicket.getServicePrincipalName());
    }

    /**
     * Gets the cached KerberosServiceTicket for the specified service+name combo.
     *
     * @param key the key to query the repository
     * @return the cached service ticket if found in the repository, null otherwise
     */
    public boolean contains(Key key) {

        return _map.containsKey(key.toHashkey());
    }

    /**
     * Removes the specified cached credential from the repository
     *
     * @param key the key referencing the cached creds to remove
     */
    public void remove(Key key) {
        
        // mark expired so it will be removed on next access
        if (contains(key))
            _map.get(key.toHashkey()).expires = 0;
    }

    /**
     * Generates a hashKey for the ticket cache.
     *
     * @param service the service part
     * @param name the name part
     * @return a TicketRepository key
     */
    public Key generateKey(String service, String name) {

        return new Key(service, name);
    }

    public Long getKerberosTicketLifetime() {
        return kerberosTicketLifetime;
    }

    public void setKerberosTicketLifetime(Long kerberosTicketLifetime) {
        this.kerberosTicketLifetime = kerberosTicketLifetime;
    }

    /**
     * Returns the cached credential element specified by the key updating the
     * last accessed timestamp in the process.
     *
     * @param k the key for the cache element
     * @return the CachedCredential instance specified, null if not found.
     */
    private CachedCredential getElement(String k) {

        CachedCredential cred = null;
        if ((cred = _map.get(k)) != null) {
            cred.lastAccessTime = System.currentTimeMillis();
        }
        return cred;
    }


    /**
     * Amount of time prior to a ticket's expiry
     */
    private static final long EXPIRES_BUFFER = 1000L * 30L; // 30 seconds -- a bit arbitrary

    /**
     * Check that the expireTime parameter is still sufficiently in the future for a
     * ticket to be used.  Currently, the time must be > 1 minute.
     *
     * @param expireTime the expire time to check
     * @return true if the expire time is greater than 1 minute from now, false otherwise.
     */
    private boolean checkExpiry(long expireTime) {

        return (expireTime > System.currentTimeMillis() + EXPIRES_BUFFER);
    }

    /**
     * Imposed limit on the cache such that it doesn't fill up the ssg memory
     */
    private static int CACHE_SIZE_LIMIT = 250;
    /**
     * Threshold to be reached that kicks off the cleanup task
     */
    private static float CACHE_THREASHOLD = 0.80f;

    private final Object cleanupMutex = new Object();
    private Date lastCleanupRun = new Date();

    /**
     * Runs a cleanup thread to cleanup cache entries.  Only runs if the cache has exceeded it's threashold
     * limit and at most every 5 minutes.
     */
    private void runCleanupTask() {

        synchronized (cleanupMutex) {
            if (canRunCleanup()) {

                // assign task for execution
                maintenanceThread.execute(new Runnable() {
                    static final long THREASHOLD = 300000L; // 5 mins
                    public void run() {

                        long checkTime = System.currentTimeMillis() - THREASHOLD;

                        // traverse map
                        Iterator<String> it = _map.keySet().iterator();
                        CachedCredential cred = null;
                        String key = null;
                        int counter = 0;

                        for (;it.hasNext();) {
                            key = it.next();
                            cred = _map.get(key);
                            if ( cred.isExpired() ||
                                 _map.size() > CACHE_SIZE_LIMIT && cred.getLastAccessTime() < checkTime )
                                // the 2nd check is ensure the repository doesn't consume all the gateway resources
                            {
                                _map.remove(key);
                                cred.discard();
                                counter++;
                            }
                        }
                        logger.log(Level.FINE, "Cache cleanupTask completed, removing: {0}", new String[] { Integer.toString(counter) });
                    }
                });
                
                lastCleanupRun = new Date();
            }
        }
    }

    /**
     * Checks whether the cleanup task can be executed.
     *
     * @return true if the task should be executed, false otherwise
     */
    private boolean canRunCleanup() {

        return ((_map.size() / CACHE_SIZE_LIMIT) > CACHE_THREASHOLD) &&
               ((System.currentTimeMillis() - 300000L) < lastCleanupRun.getTime());
    }

    /**
     * One credentials cache entry representing a single service/principal credential
     * consisting of a TGT (auth ticket) and a kerberos service ticket for the endpoint
     * service.
     */
    private class CachedCredential {

        String principal;
        volatile KerberosTicket tgTicket;
        volatile LoginContext loginContext;
        long lastAccessTime;
        long expires;

        private CachedCredential(KerberosTicket tgTicket, LoginContext loginCtx, String principal) {
            this.tgTicket = tgTicket;
            this.loginContext = loginCtx;
            this.lastAccessTime = System.currentTimeMillis();
            this.principal = principal;

            // set the expiry for this cache entry -- smaller value of the configured lifetime vs TGT endTime
            if (kerberosTicketLifetime != null)
                this.expires = Math.min(tgTicket.getEndTime().getTime(), this.lastAccessTime + kerberosTicketLifetime);
        }

        boolean hasValidTgTicket() {
            // check exists and not expired (both configured and tgt ticket value)
            return (tgTicket != null && checkExpiry(expires) && checkExpiry(tgTicket.getEndTime().getTime()));
        }

        boolean isExpired() {
            return (System.currentTimeMillis() > expires);
        }

        void discard() {

            tgTicket = null;
            try {
                if (loginContext != null)
                    loginContext.logout();
                loginContext = null;
            } catch (LoginException lex) {}
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }
    }


    /**
     * Represents a Key object for the credentials cache.
     */
    public class Key {

        /**
         * The string template for the hash keys
         */
        private static final String KEY_TEMPLATE = "{0}++{1}";

        private final String serviceName;
        private final String clientName;
        private String hashKey;

        Key(String service, String name) {
            this.serviceName = service;
            this.clientName = name;
        }

        public String toHashkey() {
            if (hashKey == null)
                hashKey = MessageFormat.format(KEY_TEMPLATE, serviceName, clientName);

            return hashKey;
        }
    }
}