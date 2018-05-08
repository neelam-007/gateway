package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.server.transport.jms.JmsBag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jms.JMSException;
import javax.naming.Context;

import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SingleSessionHolderTest {
    @Mock
    JmsEndpointConfig mockJmsEndpointConfig;
    @Mock
    JmsBag mockJmsBag;
    @Mock
    JmsConnection mockJmsConnection;
    @Mock
    JmsEndpoint mockJmsEndpoint;
    @Mock
    JmsEndpointConfig.JmsEndpointKey mockJmsEndpointKey;
    @Mock
    Context mockJndiContext;
    @Mock
    JmsBag mockSingleSession;

    SingleSessionHolder fixture;

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        when(mockJmsConnection.properties()).thenReturn(props);
        when(mockJmsConnection.getVersion()).thenReturn(1);
        when(mockJmsConnection.getName()).thenReturn("mockJmsConnection");
        when(mockJmsEndpointConfig.getConnection()).thenReturn(mockJmsConnection);
        when(mockJmsEndpointConfig.getEndpoint()).thenReturn(mockJmsEndpoint);
        when(mockJmsEndpointConfig.getJmsEndpointKey()).thenReturn(mockJmsEndpointKey);
        when(mockJmsEndpointKey.toString()).thenReturn("mockJmsEndpointKey");
        when(mockJmsBag.getJndiContext()).thenReturn(mockJndiContext);
        fixture = new SingleSessionHolder(mockJmsEndpointConfig, mockJmsBag) {
            @Override
            protected JmsBag makeJmsBag() throws Exception {
                return mockSingleSession;
            }
        };

    }

    @After
    public void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    public void testBorrowJmsBag() throws Exception {
        assertEquals(mockSingleSession, fixture.borrowJmsBag());
        //try second time
        assertEquals(mockSingleSession, fixture.borrowJmsBag());
    }

    @Test
    public void testClose() throws Exception {
        fixture.close();
        verify(mockSingleSession, times(1)).close();
    }

    @Test
    public void testDoWithJmsResources() throws Exception {
        fixture.doWithJmsResources(new JmsResourceManager.JmsResourceCallback() {
            @Override
            public void doWork(JmsBag bag, JmsResourceManager.JndiContextProvider jndiContextProvider) throws JMSException {
                assertEquals(mockSingleSession, bag);
            }
        });
    }

}