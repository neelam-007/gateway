package com.l7tech.server.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.util.ExceptionUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class HazelcastMessageIdManager implements MessageIdManager {
    private final Logger logger = Logger.getLogger(HazelcastMessageIdManager.class.getName());

    private static final String MESSAGE_ID_MAP_NAME = "message-ids";

    private HazelcastInstance hazelcastInstance;
    private AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void assertMessageIdIsUnique(MessageId prospect) throws MessageIdCheckException {
        if (!initialized.get()) {
            throw new IllegalStateException("MessageIDManager has not been initialized");
        }

        final IMap<String, Long> messageIdMap = hazelcastInstance.getMap(MESSAGE_ID_MAP_NAME);

        final String key = prospect.getOpaqueIdentifier();
        
        try {
            // lock access to this key, whether it is present or not, across all cluster nodes
            messageIdMap.lock(key);

            Long existingIdExpiry = messageIdMap.get(key);

            // if there's an entry for this key and it's not expired, this is a duplicate
            if (null != existingIdExpiry && !isExpired(existingIdExpiry)) {
                throw new DuplicateMessageIdException();
            }

            final long expiry = Math.abs(prospect.getNotValidOnOrAfterDate());

            // determine time until expiry
            final long cacheEntryTtl = expiry - System.currentTimeMillis();

            // if the prospect has not already expired, cache the prospect until it does
            if (cacheEntryTtl > 0) {
                messageIdMap.put(key, expiry, cacheEntryTtl, TimeUnit.MILLISECONDS);
            }
        } catch (DuplicateMessageIdException e) {
            throw e;
        } catch (Exception e) {
            final String msg = "Failed to determine whether a MessageId is a replay : " + ExceptionUtils.getMessage(e);
            logger.log(Level.SEVERE, msg , ExceptionUtils.getDebugException(e));
            throw new MessageIdCheckException(msg, e);
        } finally {
            messageIdMap.unlock(key);
        }
    }

    private boolean isExpired(Long expiryTime) {
        return Math.abs(expiryTime.longValue()) < System.currentTimeMillis();
    }

    public void initialize(HazelcastInstance hazelcastInstance) throws Exception {
        if (initialized.get()) {
            throw new IllegalStateException("MessageIDManager has already been initialized");
        }

        this.hazelcastInstance = hazelcastInstance;

        // create map (if first node in cluster)
        hazelcastInstance.getMap(MESSAGE_ID_MAP_NAME);

        initialized.set(true);
    }

    public boolean isInitialized() {
        return initialized.get();
    }
}
