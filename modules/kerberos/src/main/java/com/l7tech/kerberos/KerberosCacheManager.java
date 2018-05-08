package com.l7tech.kerberos;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.TimeSource;
import org.jboss.cache.*;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.EvictionConfig;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jboss.cache.eviction.ExpirationAlgorithmConfig;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.Ticket;

import javax.security.auth.Destroyable;
import javax.security.auth.kerberos.KerberosTicket;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manage the kerberos Ticket cache. Once the ticket is expired, the ticket will be removed from the cache.
 * The cache size can be control by cluster property.
 */
public class KerberosCacheManager {

    private static final Logger LOGGER = Logger.getLogger(KerberosCacheManager.class.getName());
    private static final String KERBEROS_CACHE_SIZE = "kerberos.cache.size";
    private static final String KERBEROS_CACHE_TIMETOLIVE = "kerberos.cache.timeToLive";
    private static final int DEFAULT_KERBEROS_CACHE_SIZE = 0;
    private static final int DEFAULT_KERBEROS_CACHE_TIMETOLIVE = 0;

    private Cache cache;
    private ExpirationAlgorithmConfig eac;

    private static KerberosCacheManager instance;
    private CacheFactory factory = new DefaultCacheFactory();
    private static final Object KERBEROS_TICKET = "KERBEROS_TICKET";

    private TimeSource timeSource = new TimeSource();

    private KerberosCacheManager() {
        initCache();
    }

    private void initCache() {
        cache = factory.createCache(initConfig());
    }

    /**
     * Initialize the cache configuration,
     * Set the cache eviction policy to the configuration object. The cache in memory will evict once the cache is expired.
     * Initialize the cache max size.
     */
    private Configuration initConfig() {
        Configuration config = new Configuration();
        //The cache may contain sensitive information, do not expost the cache data with MBean
        config.setExposeManagementStatistics(false);
        config.setCacheMode(Configuration.CacheMode.LOCAL);
        eac = new ExpirationAlgorithmConfig();
        eac.setMaxNodes(ConfigFactory.getIntProperty(KERBEROS_CACHE_SIZE, DEFAULT_KERBEROS_CACHE_SIZE));
        eac.setTimeToLive(ConfigFactory.getIntProperty(KERBEROS_CACHE_TIMETOLIVE, DEFAULT_KERBEROS_CACHE_TIMETOLIVE), TimeUnit.SECONDS);
        EvictionRegionConfig erc = new EvictionRegionConfig(Fqn.root(), eac);
        EvictionConfig ec = new EvictionConfig(erc);
        config.setEvictionConfig(ec);
        return config;
    }

    /**
     * Refresh the cache, all cached data will be removed and the cache will be re-initialized
     */
    public void refresh() {
        cleanup();
        initCache();
    }

    /**
     * Clean up the cached data
     */
    protected void cleanup() {
        cache.stop();
        cache.destroy();
    }

    public static KerberosCacheManager getInstance() {
        if (instance == null) {
            instance = new KerberosCacheManager();
        }
        return instance;
    }

    /**
     * Store the kerberos ticket to the cache.
     * endtime is always same for TGT / self service ticket and proxy service ticket.
     *
     * @param principalName    principal name
     * @param ticket           The kerberos Ticket
     * @param additionalTicket delegated ticket
     */
    public void store(PrincipalName principalName, final Object ticket, KerberosTicket additionalTicket) {
        long expiresTime = additionalTicket.getEndTime().getTime();
        //As long as timeToLive has a positive value and additionalTicket is not destroyed and
        // additionalTicet is valid for more then buffer time until expiry, then ticket can be saved in cache.
        if (eac.getMaxNodes() > 0 && eac.getTimeToLive() > 0 && !additionalTicket.isDestroyed() && isValid(expiresTime)) {
            long expiresTimeWithBuffer = expiresTime - KerberosUtils.EXPIRES_BUFFER;
            long currentSystemTime = timeSource.currentTimeMillis();
            long cacheExpireTime;
            Calendar endTime = Calendar.getInstance();
            endTime.setTime(additionalTicket.getEndTime());

            // Store ticket in cache for minimum duration between timeToLive and ticket expiration with buffer.
            long minDuration = Math.min(expiresTimeWithBuffer - currentSystemTime, eac.getTimeToLive());
            cacheExpireTime = minDuration + currentSystemTime;

            // Cache Structure :
            // Root:
            //      FQN1 (Key - PrincipalName and ticket which is used to retrieve service ticket stored in value)
            //          EXPIRATION_KEY <-> 1233434
            //          KERBEROS_TICKET <-> self service ticket / proxy service ticket
            //      FQN2 ...
            Key key = new Key(principalName, (ticket instanceof KerberosTicket ? ticket : new TicketWrapper((Ticket) ticket)));
            Fqn fqn = Fqn.fromElements(key);
            Node node = cache.getRoot().addChild(fqn);
            node.put(ExpirationAlgorithmConfig.EXPIRATION_KEY, cacheExpireTime);
            node.put(KERBEROS_TICKET, additionalTicket);

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, new StringBuilder("Storing ticket for principal=")
                        .append(principalName)
                        .append("\nand cached ticket (Key)=")
                        .append(((ticket instanceof Destroyable && ((KerberosTicket) ticket).isDestroyed()) ? KerberosUtils.EXPIRED : ticket.toString()))
                        .append("\ncached ticket(value)=")
                        .append(additionalTicket.isDestroyed() ? KerberosUtils.EXPIRED : additionalTicket.toString())
                        .append("\ncache expiration time=")
                        .append(cacheExpireTime)
                        .append("\ncurrent system time=")
                        .append(timeSource.currentTimeMillis())
                        .toString());
            }
        }
    }

    public KerberosTicket getKerberosTicket(final PrincipalName principalName, Object kerberosTicket, final Calendar endTime) {
        if (eac.getMaxNodes() > 0 && eac.getTimeToLive() > 0) {
            Key key = new Key(principalName, (kerberosTicket instanceof KerberosTicket ? kerberosTicket : new TicketWrapper((Ticket) kerberosTicket)));
            Fqn fqn = Fqn.fromElements(key);
            KerberosTicket ticket = (KerberosTicket) cache.get(fqn, KERBEROS_TICKET);
            if (ticket == null || ticket.isDestroyed() || !isValid(ticket.getEndTime().getTime())) {
                //all Invalid conditions
                long expireTime = 0;
                if (LOGGER.isLoggable(Level.FINEST)) {
                    StringBuilder sb = new StringBuilder();
                    String initialMsg = "Invalid ticket";
                    String strTicket = "";
                    if (ticket == null) {
                        initialMsg = "Could not retrieve ticket for principal=";
                        strTicket = "null";
                    } else if (!ticket.isDestroyed()) {
                        initialMsg = "Retrieved valid ticket but close to expiry time for principal=";
                        strTicket = ticket.toString();
                        expireTime = (long) cache.get(fqn, ExpirationAlgorithmConfig.EXPIRATION_KEY);
                    } else if (ticket.isDestroyed()) {
                        initialMsg = "Retrieved destroyed ticket for pricipal=";
                        strTicket = KerberosUtils.EXPIRED;
                        expireTime = (long) cache.get(fqn, ExpirationAlgorithmConfig.EXPIRATION_KEY);
                    }
                    sb.append(initialMsg)
                            .append(principalName)
                            .append("\nand ticket(from key)=")
                            .append((kerberosTicket instanceof Destroyable && ((KerberosTicket) kerberosTicket).isDestroyed()) ? KerberosUtils.EXPIRED : kerberosTicket.toString())
                            .append("\nticket (from value)=")
                            .append(strTicket)
                            .append("\ncache expiration time=")
                            .append(expireTime)
                            .append("\ncurrent system time=")
                            .append(timeSource.currentTimeMillis());
                    LOGGER.log(Level.FINEST, sb.toString());
                }

                return null;
            } else {
                //Valid ticket
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, new StringBuilder("Retrieved valid ticket for principal=")
                            .append(principalName)
                            .append("\nand ticket (key)=")
                            .append((kerberosTicket instanceof Destroyable && ((KerberosTicket) kerberosTicket).isDestroyed()) ? KerberosUtils.EXPIRED : kerberosTicket.toString())
                            .append("\nticket (value)=")
                            .append(ticket)
                            .append("\ncache expiration time=")
                            .append(cache.get(fqn, ExpirationAlgorithmConfig.EXPIRATION_KEY))
                            .append("\ncurrent system time=")
                            .append(timeSource.currentTimeMillis())
                            .toString());
                }

                return ticket;
            }
        }
        return null;
    }

    /**
     * This method checks for validity of the time considering buffer.
     * Buffer is reduced from endTime of the ticket so that cache expiry is set before actual ticket expiry.
     * If ticket is expired before cache can evict object then cache is not able to find this object anymore.
     * @param time - time in millis which will be checked for validity.
     * */
    private boolean isValid(long time) {
        return time - KerberosUtils.EXPIRES_BUFFER > timeSource.currentTimeMillis();
    }

    /**
     * Assigning this timesource so that It will be used instead of one instantiated inside static block for unit testing.
     * */
    public void setTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    /**
     * This class is used as Key for storing service tickets in JBoss cache.
     * Currently it stores 3 types of tickets:
     * 1. If Key is TGT and principal is user, then its self service ticket for gateway on behalf of user (kerbuser_tacoma).
     * 2. If key is self service ticket (KerberosTicket), and pricnipal is actual web resource (tacoma.seattle.local),
     *      then value is proxy ticket - ticket on behalf of user to access web resource.
     * 3. If key is encoded service ticket (Ticket) received from windows integrated credentials (KerberosServiceTicket),
     *      and pricnipal is actual web resource (tacoma.seattle.local), then value is proxy ticket - ticket on behalf of user to access web resource.
     *
     * */
     protected class Key implements Comparable<Key> {

        private PrincipalName principalName;
        private Object ticket;

        Key(PrincipalName principalName, Object ticket) {
            this.principalName = principalName;
            this.ticket = ticket;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;

            Key key = (Key) o;

            if (principalName != null ? !principalName.equals(key.principalName) : key.principalName != null) {
                return false;
            }
            if (ticket != null ? !ticket.equals(key.ticket) : key.ticket != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(principalName, ticket);
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append("Key{principalName=").append(principalName).append(", ticket=");
            if ((ticket instanceof KerberosTicket && (((KerberosTicket) ticket).isDestroyed()))
                    ) {
                sb.append(KerberosUtils.EXPIRED);
            } else {
                sb.append(ticket);
            }
            sb.append("}");
            return sb.toString();
        }

        // This compareTo method compares principlename and ticket hashcode, which will be used by eviction policy to
        @Override
        public int compareTo(Key key) {
            int result = principalName.toString().compareTo(key.principalName.toString());
            if( result == 0) {
                result = Integer.compare(ticket.hashCode(), key.ticket.hashCode());
            }
            return result;
        }
    }
}
