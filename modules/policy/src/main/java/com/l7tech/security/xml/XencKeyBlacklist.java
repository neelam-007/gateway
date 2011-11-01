package com.l7tech.security.xml;

import com.l7tech.util.Background;
import com.l7tech.util.ConfigFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A blacklist of secret keys that have been used on this node to unsuccessfully attemp to decrypt XML.
 * The intent of this feature is to make it take far, far longer for an attacker to mount an attack on a symmetric
 * cipher block chaining in CBC mode per the 2011 paper "How to Break XML Encryption" by Jager and Somorovsky (Bug #11251)
 */
public class XencKeyBlacklist {
    private static final Logger logger = Logger.getLogger(XencKeyBlacklist.class.getName());

    public static final boolean DEFAULT_ENABLEMENT = true;
    public static final boolean DEFAULT_FAIL_WHEN_FULL = false;
    public static final int DEFAULT_MAX_AGE_SEC = 7 * 86400;
    public static final int DEFAULT_FAILURE_DELAY_MILLIS = 50;
    public static final int DEFAULT_MAX_FAILURES = 5;
    public static final int DEFAULT_CAPACITY = 50000;
    public static final long DEFAULT_CLEANER_PERIOD_MILLIS = 451723L;

    public static final String PROP_XENC_KEY_BLACKLIST_ENABLED = "com.l7tech.security.xml.xenc.decryptionKeyBlacklist.enabled";
    public static final String PROP_XENC_KEY_BLACKLIST_FAIL_WHEN_FULL = "com.l7tech.security.xml.xenc.decryptionKeyBlacklist.failWhenFull";
    public static final String PROP_XENC_KEY_BLACKLIST_CAPACITY = "com.l7tech.security.xml.xenc.decryptionKeyBlacklist.capacity";
    public static final String PROP_XENC_KEY_BLACKLIST_MAX_FAILURES = "com.l7tech.security.xml.xenc.decryptionKeyBlacklist.maxFailures";
    public static final String PROP_XENC_KEY_BLACKLIST_MAX_AGE = "com.l7tech.security.xml.xenc.decryptionKeyBlacklist.maxAgeSec";
    public static final String PROP_XENC_KEY_BLACKLIST_FAILURE_DELAY = "com.l7tech.security.xml.xenc.decryptionKeyBlacklist.failureDelayMillis";
    public static final String PROP_XENC_KEY_BLACKLIST_CLEANER_PERIOD = "com.l7tech.security.xml.xenc.decryptionKeyBlacklist.cleanerPeriodMillis";

    private static final ConcurrentMap<KeyHolder, FailedKey> failedKeys = new ConcurrentHashMap<KeyHolder, FailedKey>();
    private static final AtomicBoolean blacklistFull = new AtomicBoolean(false);

    static {
        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                expireOldKeys();
            }
        }, getCleanerPeriodMillis(), getCleanerPeriodMillis());
    }

    /**
     * Check if the specified secret key bytes are blacklisted due to too many unsuccessful decryption attempts.
     *
     * @param keyBytes the secret key bytes to check.  Required.
     * @return true if the blacklist enabled, and decryption attempts with the specified key have failed on this node more than the maximum acceptable number of times.
     */
    public static boolean isKeyBlacklisted(byte[] keyBytes) {
        if (!isBlacklistEnabled())
            return false;

        if (blacklistFull.get()) {
            maybeExpireOldKeys();
            // Ouch -- have to disallow any encryption from even being attempted, lest an attacker flood us with random keys to disable this mechanism
            if (blacklistFull.get() && isFailWhenFull())
                return true;
        }

        KeyHolder keyHolder = new KeyHolder(keyBytes); // Do not copy array yet
        FailedKey failedKey = failedKeys.get(keyHolder);

        return failedKey != null && failedKey.getFailureCount() > getMaximumAcceptableFailureCount();
    }

    /**
     * Record that an attempt to decrypt XML using the specified secret key bytes has failed.
     *
     * @param keyBytes the secret key bytes that failed to decrypt.  Required.
     */
    public static void recordDecryptionFailure(byte[] keyBytes) {
        KeyHolder keyHolder = new KeyHolder(keyBytes); // Do not copy array yet
        FailedKey failedKey = failedKeys.get(keyHolder);

        if (failedKey == null) {
            keyHolder = new KeyHolder(Arrays.copyOf(keyBytes, keyBytes.length));
            failedKey = new FailedKey(keyHolder);
            FailedKey existing = failedKeys.putIfAbsent(keyHolder, failedKey);
            if (existing != null) {
                // Put failed, someone else won the race.  Make sure we record our failure.
                existing.recordFailure();
            } else {
                // New key added -- check blacklist size
                if (failedKeys.size() > getBlacklistSize()) {
                    maybeExpireOldKeys();
                }
            }
        } else {
            failedKey.recordFailure();
        }
    }

    private static void maybeExpireOldKeys() {
        synchronized (failedKeys) {

            if (failedKeys.size() > getBlacklistSize()) {
                expireOldKeys();
            }
            if (failedKeys.size() > getBlacklistSize()) {
                // We have no choice -- we have to stop permitting further decryption attempts
                // or else permit memory to fill up, or permit the attacker to bypass the blacklist
                // cache by flooding it with random keys until it becomes ineffective
                if (!blacklistFull.getAndSet(true)) {
                    logger.log(Level.SEVERE, "Decryption key blacklist is full -- too many failed decryption attempts using too many different secret keys.  Decryption attempts disabled.");
                }
            } else {
                if (blacklistFull.getAndSet(false)) {
                    logger.log(Level.WARNING, "Decryption key blacklist is no longer full.  Decryption attempts reenabled.");
                }
            }
        }
    }

    private static void expireOldKeys() {
        final long oldestToKeep = System.currentTimeMillis() - getBlacklistMaxAgeMillis();
        Iterator<FailedKey> it = failedKeys.values().iterator();
        while (it.hasNext()) {
            FailedKey fk = it.next();
            if (fk.lastUsedMillis.get() < oldestToKeep)
                it.remove();
        }
    }

    private static class KeyHolder {
        final byte[] keyBytes;

        public KeyHolder(@NotNull byte[] keyBytes) {
            this.keyBytes = keyBytes;
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KeyHolder keyHolder = (KeyHolder) o;

            if (!Arrays.equals(keyBytes, keyHolder.keyBytes)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(keyBytes);
        }
    }

    private static class FailedKey {
        final KeyHolder key;
        final AtomicLong lastUsedMillis;
        final AtomicInteger failureCount;

        private FailedKey(KeyHolder key) {
            this.key = key;
            this.lastUsedMillis = new AtomicLong(System.currentTimeMillis());
            this.failureCount = new AtomicInteger(1);
        }

        private int getFailureCount() {
            final int fc = failureCount.get();
            
            if (fc > 0) {
                // Forcibly delay/serialize all threads checking this key, to make it harder for an attacker to 
                // to bypass the count restriction by sending a huge burst of parallel decryption attempts
                serializedDelay();
            }
            
            return failureCount.get();
        }

        private synchronized void serializedDelay() {
            try {
                Thread.sleep(getFailureDelayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void recordFailure() {
            lastUsedMillis.set(System.currentTimeMillis());
            failureCount.incrementAndGet();
        }
    }
    
    private static boolean isBlacklistEnabled() {
        return ConfigFactory.getBooleanProperty(PROP_XENC_KEY_BLACKLIST_ENABLED, DEFAULT_ENABLEMENT);
    }

    private static boolean isFailWhenFull() {
        return ConfigFactory.getBooleanProperty(PROP_XENC_KEY_BLACKLIST_FAIL_WHEN_FULL, DEFAULT_FAIL_WHEN_FULL);
    }

    private static int getBlacklistSize() {
        return ConfigFactory.getIntProperty(PROP_XENC_KEY_BLACKLIST_CAPACITY, DEFAULT_CAPACITY);
    }

    private static int getMaximumAcceptableFailureCount() {
        return ConfigFactory.getIntProperty(PROP_XENC_KEY_BLACKLIST_MAX_FAILURES, DEFAULT_MAX_FAILURES);
    }
    
    private static long getBlacklistMaxAgeMillis() {
        return ConfigFactory.getTimeUnitProperty(PROP_XENC_KEY_BLACKLIST_MAX_AGE, DEFAULT_MAX_AGE_SEC);
    }

    private static long getFailureDelayMillis() {
        return ConfigFactory.getTimeUnitProperty(PROP_XENC_KEY_BLACKLIST_FAILURE_DELAY, DEFAULT_FAILURE_DELAY_MILLIS);
    }

    private static long getCleanerPeriodMillis() {
        return ConfigFactory.getTimeUnitProperty(PROP_XENC_KEY_BLACKLIST_CLEANER_PERIOD, DEFAULT_CLEANER_PERIOD_MILLIS);
    }
}
