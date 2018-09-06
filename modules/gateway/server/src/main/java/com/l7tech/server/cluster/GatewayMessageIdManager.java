package com.l7tech.server.cluster;

import com.ca.apim.gateway.extension.sharedstate.Configuration;
import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStore;
import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStoreProvider;
import com.l7tech.server.extension.registry.sharedstate.SharedKeyValueStoreProviderRegistry;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;

import static com.ca.apim.gateway.extension.sharedstate.Configuration.Param.PERSISTED;
import static java.lang.Boolean.FALSE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.*;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 *
 * This class manages messages. It contains a map of message ids, stored in whichever SharedKeyValueStore is provided by the SharedKeyValueStoreProvider,
 * and can validate message uniqueness. Message ids will only be stored in the map if there is some assertion which uses the GatewayMessageIdManager and will
 * only be saved until the expiry window has passed.
 */
public class GatewayMessageIdManager implements MessageIdManager {

    private static final Logger LOGGER = Logger.getLogger(GatewayMessageIdManager.class.getName());
    private static final String MESSAGE_ID_MAP_NAME = "message-replay-ids";

    private final SharedKeyValueStoreProviderRegistry sharedKeyValueStoreProviderRegistry;
    private SharedKeyValueStore<String, String> messageIdMap;
    private AtomicBoolean initialized = new AtomicBoolean(false);

    public GatewayMessageIdManager(final SharedKeyValueStoreProviderRegistry sharedKeyValueStoreProviderRegistry) {
        this.sharedKeyValueStoreProviderRegistry = sharedKeyValueStoreProviderRegistry;
    }

    @Override
    public void assertMessageIdIsUnique(MessageId prospect) throws MessageIdCheckException {
        initializeIfNecessary();
        checkMessageIdMap();

        final String key = prospect.getOpaqueIdentifier();

        try {
            /*
            We must guarantee that if a message is duplicated, a DuplicateMessageIdException is thrown no matter
            whether it is duplicated on 2+ threads on a single node or in 2+ threads on different Gateway nodes.

            Caveats of the below code:
            A blocked thread/node will have an outdated time-to-live, which is ok. The entry still expires, albeit after expiry.
             */

            final long expiry = Math.abs(prospect.getNotValidOnOrAfterDate());
            final long cacheEntryTtl = expiry - System.currentTimeMillis();

            if (cacheEntryTtl > 0) {
                boolean success = messageIdMap.putIfCondition(key, Long.toString(expiry), new Function<String, Boolean>() {
                    @Override
                    public Boolean apply(String value) {
                        return value == null || isExpired(Long.valueOf(value));
                    }
                }, cacheEntryTtl, MILLISECONDS);
                if (!success) {
                    throw new DuplicateMessageIdException();
                }
            }

        } catch (DuplicateMessageIdException e) {
            throw e;
        } catch (Exception e) {
            final String msg = "Failed to determine whether a MessageId is a replay : " + ExceptionUtils.getMessage(e);
            LOGGER.log(SEVERE, msg , ExceptionUtils.getDebugException(e));
            throw new MessageIdCheckException(msg, e);
        }
    }

    private boolean isExpired(Long expiryTime) {
        return Math.abs(expiryTime) < System.currentTimeMillis();
    }

    private void initializeIfNecessary() {
        if (initialized.get()) {
            return;
        }

        String providerName = SyspropUtil.getProperty(SharedKeyValueStoreProviderRegistry.SYSTEM_PROPERTY_KEY_VALUE_STORE_PROVIDER);
        SharedKeyValueStoreProvider sharedKeyValueStoreProvider = sharedKeyValueStoreProviderRegistry.getExtension();
        if (sharedKeyValueStoreProvider != null) {
            messageIdMap = sharedKeyValueStoreProvider.getKeyValueStore(MESSAGE_ID_MAP_NAME, new Configuration().set(PERSISTED.name(), FALSE.toString()));
            LOGGER.log(FINE,"GatewayMessageIdManager is using shared key value store provider: {0}", providerName);
        } else {
            LOGGER.log(WARNING, "No implementation of SharedKeyValueStore was provided, GatewayMessageIdManager will not work");
        }
        initialized.set(true);
    }

    private void checkMessageIdMap() throws MessageIdCheckException {
        if (this.messageIdMap == null) {
            throw new MessageIdCheckException("No implementation of SharedKeyValueStore was provided, messageIdMap is not available");
        }
    }
}
