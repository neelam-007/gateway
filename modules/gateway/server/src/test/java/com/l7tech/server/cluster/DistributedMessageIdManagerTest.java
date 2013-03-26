package com.l7tech.server.cluster;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.util.FileUtils;
import com.l7tech.util.TestTimeSource;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class DistributedMessageIdManagerTest {

    private DistributedMessageIdManager distributedMessageIdManager;
    private TestTimeSource timeSource = new TestTimeSource();

    @Before
    public void setUp() throws Exception {
        ServerConfigStub serverConfig = new ServerConfigStub();
        serverConfig.putProperty(ServerConfigParams.PARAM_MULTICAST_ENABLED, "false");
        serverConfig.putProperty(ServerConfigParams.PARAM_CONFIG_DIRECTORY, FileUtils.createTempDirectory("jgroup", null, null, true).getPath());
        distributedMessageIdManager = new DistributedMessageIdManager(serverConfig, "clusterNodeId");
        distributedMessageIdManager.setHibernateTemplate(new HibernateTemplate() {
            @Override
            public <T> T execute(HibernateCallback<T> action) throws DataAccessException {
                return null;
            }
        });

        distributedMessageIdManager.initialize(null, 0, null);
    }

    @After
    public void tearDown() throws Exception {
        distributedMessageIdManager.close();
    }

    @Test(expected = MessageIdManager.DuplicateMessageIdException.class)
    public void testMessageIdDuplicate() throws MessageIdManager.MessageIdCheckException {
        String key = "key1";
        timeSource.advanceByMillis(1000);
        distributedMessageIdManager.assertMessageIdIsUnique(new MessageId(key, timeSource.getCurrentTimeMillis()));
        distributedMessageIdManager.assertMessageIdIsUnique(new MessageId(key,timeSource.getCurrentTimeMillis()));
    }

    @Test
    public void testMessageIdExpired() throws MessageIdManager.MessageIdCheckException {
        String key = "key1";
        timeSource.advanceByMillis(-1000);
        distributedMessageIdManager.assertMessageIdIsUnique(new MessageId(key, timeSource.getCurrentTimeMillis()));
        distributedMessageIdManager.assertMessageIdIsUnique(new MessageId(key,timeSource.getCurrentTimeMillis()));
    }
}
