package com.l7tech.server.cluster;

import com.ca.apim.gateway.extension.sharedstate.Configuration;
import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStore;
import com.ca.apim.gateway.extension.sharedstate.SharedKeyValueStoreProvider;
import com.l7tech.server.extension.registry.sharedstate.SharedKeyValueStoreProviderRegistry;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.util.TestTimeSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.*;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GatewayMessageIdManagerTest {

    private GatewayMessageIdManager messageIdManager;

    @Mock
    private SharedKeyValueStoreProviderRegistry sharedKeyValueStoreProviderRegistry;

    @Mock
    private SharedKeyValueStoreProvider sharedKeyValueStoreProvider;

    @Mock
    private SharedKeyValueStore<Serializable, Serializable> messageIdMap;

    @Before
    public void setUp() throws Exception {
        when(sharedKeyValueStoreProviderRegistry.getExtension(any(String.class))).thenReturn(sharedKeyValueStoreProvider);
        when(sharedKeyValueStoreProviderRegistry.getExtension()).thenReturn(sharedKeyValueStoreProvider);
        when(sharedKeyValueStoreProvider.getKeyValueStore(any(String.class), any(Configuration.class))).thenReturn(messageIdMap);
        messageIdManager = new GatewayMessageIdManager(sharedKeyValueStoreProviderRegistry);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test(expected = MessageIdManager.DuplicateMessageIdException.class)
    public void testAssertMessageIdIsUnique_IdDuplicate() throws Exception {
        TestTimeSource timeSource = new TestTimeSource();
        timeSource.advanceByMillis(100000);
        long time = timeSource.currentTimeMillis();

        when(messageIdMap.putIfCondition(eq("id"), eq(time), any(), any(Long.class), any(TimeUnit.class))).thenReturn(false);

        messageIdManager.assertMessageIdIsUnique(new MessageId("id", time));
    }

    @Test
    public void testAssertMessageIdIsUnique_IdExpired() throws Exception {
        TestTimeSource timeSource = new TestTimeSource();
        timeSource.advanceByMillis(-100000);

        when(messageIdMap.putIfCondition(eq("id"), any(), any(), any(Long.class), any(TimeUnit.class))).thenReturn(true);

        messageIdManager.assertMessageIdIsUnique(new MessageId("id", System.currentTimeMillis()));
    }

    @Test
    public void testMessageIdIsCached() throws Exception {
        long validTime = System.currentTimeMillis() + 100000;
        String key = "id";
        when(messageIdMap.putIfCondition(eq("id"), any(), any(), any(Long.class), any(TimeUnit.class))).thenReturn(true);
        messageIdManager.assertMessageIdIsUnique(new MessageId(key, validTime));

        verify(messageIdMap, times(1))
                .putIfCondition(eq(key), eq(validTime), any(), any(Long.class), any(TimeUnit.class));
    }

}