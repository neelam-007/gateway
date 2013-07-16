package com.l7tech.kerberos;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
     * Amount of time prior to a ticket's expiry
     */
    private static final long EXPIRES_BUFFER = 1000L * 30L; // 30 seconds -- a bit arbitrary

    /**
     * Singleton instance for the KerberosTicketRepository class.
     */
    private static KerberosTicketRepository instance;

    /**
     * Used to number threads created for {@link #maintenanceThread}'s thread pool.
     */
    private static final AtomicInteger threadCount = new AtomicInteger(0);

    /**
     * Hashmap used to store the cached credentials.
     */
    private final Map<Key, CachedCredential> _map;

    /**
     * Maintenance thread for removing old tickets.
     */
    private ExecutorService maintenanceThread = Executors.newFixedThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "KerberosTicketRepository-Maint-" + threadCount.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * Kerberos ticket lifetime for cached entries (ms) - this parameter is configued by a the system property.
     *
     * @see com.l7tech.kerberos.KerberosClient
     */
    private Long kerberosTicketLifetime;

    /**
     * Hidden default constructor.
     */
    protected KerberosTicketRepository() {
        super();
        _map = new ConcurrentHashMap<Key, CachedCredential>();
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
        CachedCredential cred = getElement(key);
        if (cred != null) {

            // need to check that
            if (cred.hasValidTgTicket()) {
                Set<Object> privCreds = new LinkedHashSet<Object>();
                privCreds.add(cred.tgTicket);
                privCreds.addAll(cred.privateKeys);
                return new Subject(false,
                        Collections.singleton(new KerberosPrincipal(cred.principal)),
                        Collections.EMPTY_SET,
                        Collections.unmodifiableSet(privCreds));
                        //Collections.singleton(cred.tgTicket));

            } else {
                // toss cached credentials
                _map.remove(key);
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
    public void add(Key key, KerberosTicket tgTicket, List<KerberosKey> privKeys, LoginContext loginCtx, KerberosServiceTicket svcTicket) {
        // create a new cache value
        CachedCredential oldCred = _map.put( key, new CachedCredential(tgTicket, privKeys, loginCtx, svcTicket.getServicePrincipalName(), kerberosTicketLifetime) );
        if ( oldCred != null ) {
            oldCred.discard();
        }

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
        List<KerberosKey> privKeys = new ArrayList<KerberosKey>();
        for (;it.hasNext();) {
            // only extract the TGT
            Object val = it.next();
            if (val instanceof KerberosTicket &&
                    KerberosTicket.class.cast(val).getServer().getName().contains("krbtgt")) {

                tgTicket = (KerberosTicket) val;
               // break;
            }
            else if(val instanceof KerberosKey) {
                privKeys.add((KerberosKey)val);
            }
        }

        if (tgTicket != null)
            this.add(key, tgTicket, privKeys, loginCtx, svcTicket);
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
        return _map.containsKey(key);
    }

    /**
     * Removes the specified cached credential from the repository
     *
     * @param key the key referencing the cached creds to remove
     */
    public void remove(Key key) {
        // mark expired so it will be removed on next access
        CachedCredential cred = _map.get(key);
        if ( cred != null ) {
            cred.expires = 0;
        }
    }

    /**
     * Generates a hashKey for the ticket cache.
     *
     * @param service the service part
     * @param type the type of the credential in use
     * @param name the name part
     * @param cred the credential
     * @return a TicketRepository key
     */
    public Key generateKey( final String service,
                            final KeyType type,
                            final String name,
                            final String cred) {

        return new Key(service, type, name, cred);
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
    private CachedCredential getElement(Key k) {
        CachedCredential cred;
        if ((cred = _map.get(k)) != null) {
            cred.lastAccessTime = System.currentTimeMillis();
        }
        return cred;
    }

    /**
     * Imposed limit on the cache such that it doesn't fill up the ssg memory
     */
    private static final float CACHE_SIZE_LIMIT = 250;

    /**
     * Threshold to be reached that kicks off the cleanup task
     */
    private static final float CACHE_THREASHOLD = 0.80f;

    private final Object cleanupMutex = new Object();
    private volatile Date lastCleanupRun = new Date();

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
                    @Override
                    public void run() {
                        int counter = 0;
                        synchronized(_map) {
                            long checkTime = System.currentTimeMillis() - THREASHOLD;

                            // traverse map
                            Iterator<Key> it = _map.keySet().iterator();
                            CachedCredential cred;
                            Key key;

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
    private static class CachedCredential {

        private final String principal;
        private volatile KerberosTicket tgTicket;
        private volatile List<KerberosKey>    privateKeys;
        private volatile LoginContext loginContext;
        private volatile long lastAccessTime;
        private volatile long expires;

        private CachedCredential( final KerberosTicket tgTicket,
                                  final List<KerberosKey> privateKeys,
                                  final LoginContext loginCtx,
                                  final String principal,
                                  final Long kerberosTicketLifetime) {
            this.tgTicket = tgTicket;
            this.privateKeys = privateKeys;
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

        /**
         * Check that the expireTime parameter is still sufficiently in the future for a
         * ticket to be used.  Currently, the time must be > 1 minute.
         *
         * @param expireTime the expire time to check
         * @return true if the expire time is greater than 1 minute from now, false otherwise.
         */
        private boolean checkExpiry( final long expireTime ) {
            return (expireTime > System.currentTimeMillis() + EXPIRES_BUFFER);
        }        

        boolean isExpired() {
            return (System.currentTimeMillis() > expires);
        }

        void discard() {

            tgTicket = null;
            privateKeys = null;
            try {
                if (loginContext != null)
                    loginContext.logout();
                loginContext = null;
            } catch (LoginException lex) {
                if ( logger.isLoggable( Level.FINER ) ) {
                    logger.log( Level.FINER, "Error closing login context '"+ ExceptionUtils.getMessage(lex)+"'.", ExceptionUtils.getDebugException(lex));
                }
            }
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }
    }

    /**
     *
     */
    public static enum KeyType { KEYTAB, DELEGATED, CREDENTIAL, CONSTRAINED_DELEGATED, REFERRAL }

    /**
     * Represents a Key object for the credentials cache.
     */
    public static final class Key {
        private final String serviceName;
        private final KeyType type;
        private final String clientName;
        private final byte[] credhash;

        Key( final String service, final KeyType type, final String name, final String cred) {
            this.serviceName = service;
            this.type = type;
            this.clientName = name;
            this.credhash = cred==null ?  new byte[0] : HexUtils.getMd5Digest( cred.getBytes() );
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!clientName.equals(key.clientName)) return false;
            if (!Arrays.equals(credhash, key.credhash)) return false;
            if (!serviceName.equals(key.serviceName)) return false;
            if (type != key.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = serviceName.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + clientName.hashCode();
            result = 31 * result + Arrays.hashCode(credhash);
            return result;
        }
    }
}