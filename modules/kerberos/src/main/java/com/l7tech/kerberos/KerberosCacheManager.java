package com.l7tech.kerberos;

import com.l7tech.util.ConfigFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.EvictionConfig;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jboss.cache.eviction.ExpirationAlgorithmConfig;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.Ticket;

import javax.security.auth.kerberos.KerberosTicket;
import java.util.concurrent.TimeUnit;

/**
 * Manage the kerberos Ticket cache. Once the ticket is expired, the ticket will be removed from the cache.
 * The cache size can be control by cluster property.
 */
public class KerberosCacheManager {

    private static final String KERBEROS_CACHE_SIZE = "kerberos.cache.size";
    private static final String KERBEROS_CACHE_TIMETOLIVE = "kerberos.cache.timeToLive";

    private Cache cache;
    private ExpirationAlgorithmConfig eac;


    private static KerberosCacheManager INSTANCE;
    private CacheFactory factory = new DefaultCacheFactory();
    private static final Object KERBEROS_TICKET = "KERBEROS_TICKET";

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
        eac.setMaxNodes(ConfigFactory.getIntProperty(KERBEROS_CACHE_SIZE, 2000));
        eac.setTimeToLive(ConfigFactory.getIntProperty(KERBEROS_CACHE_TIMETOLIVE, 60), TimeUnit.SECONDS);
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
        if (INSTANCE == null) {
            INSTANCE = new KerberosCacheManager();
        }
        return INSTANCE;
    }

    /**
     * Store the kerberos ticket to the cache.
     *
     * @param principalName principal name
     * @param ticket The kerberos Ticket
     * @param additionalTicket delegated ticket
     */
    public void store(PrincipalName principalName, Object ticket, KerberosTicket additionalTicket) {
        if (eac.getTimeToLive() != 0) {
            Key key = new Key(principalName, (ticket instanceof KerberosTicket? ticket : new TicketWrapper((Ticket)ticket)));
            Fqn fqn = Fqn.fromElements(key);
            if(ticket instanceof KerberosTicket) {
                KerberosTicket kerberosTicket = (KerberosTicket) ticket;
                if ((eac.getTimeToLive() < 0) || (kerberosTicket.getEndTime().getTime() - System.currentTimeMillis() < eac.getTimeToLive())) {
                    cache.getRoot().addChild(fqn).put(ExpirationAlgorithmConfig.EXPIRATION_KEY, kerberosTicket.getEndTime().getTime());
                } else {
                    cache.getRoot().addChild(fqn).put(ExpirationAlgorithmConfig.EXPIRATION_KEY, System.currentTimeMillis() + eac.getTimeToLive());
                }
                cache.put(fqn, KERBEROS_TICKET, additionalTicket);
            }
            else if(ticket instanceof Ticket){
                //there is no expiration time on the Ticket so use our values instead
                cache.getRoot().addChild(fqn).put(ExpirationAlgorithmConfig.EXPIRATION_KEY, System.currentTimeMillis() + eac.getTimeToLive());
                cache.put(fqn, KERBEROS_TICKET, additionalTicket);
            }
        }
    }

    public KerberosTicket getKerberosTicket(PrincipalName principalName, Object kerberosTicket) {
        Key key = new Key(principalName, (kerberosTicket instanceof KerberosTicket? kerberosTicket : new TicketWrapper((Ticket)kerberosTicket)));
        Fqn fqn = Fqn.fromElements(key);
        KerberosTicket ticket = (KerberosTicket) cache.get(fqn,  KERBEROS_TICKET);
        if (ticket != null && isExpired(ticket.getEndTime().getTime())) {
            return null;
        } else {
            return ticket;
        }
    }

    private boolean isExpired(long time) {
        if (time < System.currentTimeMillis()) {
            return true;
        } else {
            return false;
        }
    }

    private static class Key {
        private PrincipalName principalName;
        private Object ticket;

        private Key(PrincipalName principalName, Object ticket) {
            this.principalName = principalName;
            this.ticket = ticket;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;

            Key key = (Key) o;

            if (principalName != null ? !principalName.equals(key.principalName) : key.principalName != null)
                return false;
            if (ticket != null ? !ticket.equals(key.ticket) : key.ticket != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = principalName != null ? principalName.hashCode() : 0;
            result = 31 * result + (ticket != null ? ticket.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "principalName=" + principalName +
                    ", ticket=" + ticket +
                    '}';
        }
    }

}
