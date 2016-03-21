package com.l7tech.server.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.util.TestTimeSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HazelcastMessageIdManagerTest {

    private HazelcastMessageIdManager messageIdManager;

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Mock
    private IMap<Object, Object> messageIdMap;


    @Before
    public void setUp() throws Exception {
        messageIdManager = new HazelcastMessageIdManager();

        when(hazelcastInstance.getMap(any(String.class))).thenReturn(messageIdMap);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInitialize() throws Exception {
        assertFalse(messageIdManager.isInitialized());

        messageIdManager.initialize(hazelcastInstance);

        assertTrue(messageIdManager.isInitialized());
    }

    @Test(expected = IllegalStateException.class)
    public void testAssertMessageIdIsUnique_ManagerNotInitialized() throws Exception {
        messageIdManager.assertMessageIdIsUnique(new MessageId("id", System.currentTimeMillis()));
    }

    @Test(expected = MessageIdManager.DuplicateMessageIdException.class)
    public void testAssertMessageIdIsUnique_IdDuplicate() throws Exception {
        TestTimeSource timeSource = new TestTimeSource();
        timeSource.advanceByMillis(100000);

        when(messageIdMap.get("id")).thenReturn(timeSource.currentTimeMillis());

        messageIdManager.initialize(hazelcastInstance);

        messageIdManager.assertMessageIdIsUnique(new MessageId("id", timeSource.currentTimeMillis()));
    }

    @Test
    public void testAssertMessageIdIsUnique_IdExpired() throws Exception {
        TestTimeSource timeSource = new TestTimeSource();
        timeSource.advanceByMillis(-100000);

        when(messageIdMap.get("id")).thenReturn(timeSource.currentTimeMillis());

        messageIdManager.initialize(hazelcastInstance);

        messageIdManager.assertMessageIdIsUnique(new MessageId("id", System.currentTimeMillis()));
    }
}