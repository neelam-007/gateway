package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms2.AbstractJmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsResourceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.ApplicationContext;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.naming.Context;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PooledJmsEndpointListenerImpl.class,AbstractJmsEndpointListener.class, JmsUtil.class})
public class PooledJmsEndpointListenerImplTest {
    static final long SHUTDOWN_TIMEOUT = 7 * 1000;

    @Mock
    JmsEndpointConfig mockJmsEndpointConfig;
    @Mock
    ApplicationContext mockApplicationContext;
    @Mock
    JmsResourceManager mockJmsResourceManager;
    @Mock
    JmsEndpoint mockJmsEndpoint;
    @Mock
    JmsConnection mockJmsConnection;
    @Mock
    JmsBag mockJmsBag;
    @Mock
    javax.jms.Connection mockConnection;


    PooledJmsEndpointListenerImpl fixture;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(JmsUtil.class);
        PowerMockito.when(mockJmsBag.getConnection()).thenReturn(mockConnection);
        PowerMockito.when(mockJmsResourceManager.borrowJmsBag(mockJmsEndpointConfig)).thenReturn(mockJmsBag);
        PowerMockito.when(mockApplicationContext.getBean("jmsResourceManager", JmsResourceManager.class)).thenReturn(mockJmsResourceManager);
        PowerMockito.when(mockJmsEndpointConfig.getApplicationContext()).thenReturn(mockApplicationContext);
        PowerMockito.when(mockJmsEndpointConfig.getDisplayName()).thenReturn("test-endpoint-config");
        PowerMockito.when(mockJmsEndpoint.getGoid()).thenReturn(Goid.DEFAULT_GOID);
        PowerMockito.when(mockJmsEndpoint.getVersion()).thenReturn(1);
        PowerMockito.when(mockJmsConnection.getVersion()).thenReturn(1);
        PowerMockito.when(mockJmsConnection.getGoid()).thenReturn(Goid.DEFAULT_GOID);
        PowerMockito.when(mockJmsEndpointConfig.getEndpoint()).thenReturn(mockJmsEndpoint);
        PowerMockito.when(mockJmsEndpoint.getDestinationName()).thenReturn("destination");
        PowerMockito.when(mockJmsEndpoint.getFailureDestinationName()).thenReturn("failureDestination");
        PowerMockito.when(mockJmsEndpointConfig.getConnection()).thenReturn(mockJmsConnection);

        fixture = PowerMockito.spy(new PooledJmsEndpointListenerImpl(mockJmsEndpointConfig));
    }

    @After
    public void tearDown() throws Exception {
        Whitebox.setInternalState(fixture, "_stop", true);
    }

    @Test
    public void stop() {
        fixture.stop();
        assertTrue(Whitebox.getInternalState(fixture, "_stop"));
    }

    @Test
    public void getJmsBag() throws Exception{
        //reflectively get the method in question
        Method protectedMethod = fixture.getClass().getDeclaredMethod("getJmsBag");
        //manually tell java that the method is accessible
        protectedMethod.setAccessible(true);

        //call the method
        JmsBag jmsBag = (JmsBag)protectedMethod.invoke(fixture, null);
        //manually change accessability of the method
        protectedMethod.setAccessible(false);

        assertEquals(mockJmsBag, jmsBag);
        verify(mockJmsResourceManager,times(1)).borrowJmsBag(mockJmsEndpointConfig);
    }

    @Test
    public void getDestination() throws Exception{
        Destination mockDestination = mock(Destination.class);
        Context mockContext = mock(Context.class);
        PowerMockito.when(mockJmsBag.getJndiContext()).thenReturn(mockContext);
        PowerMockito.when(mockContext.lookup("destination")).thenReturn(mockDestination);

        when(JmsUtil.cast(mockDestination,Destination.class)).thenReturn(mockDestination);

        //reflectively get the method in question
        Method protectedMethod = fixture.getClass().getDeclaredMethod("getDestination");
        //manually tell java that the method is accessible
        protectedMethod.setAccessible(true);
        //call the method
        Destination destination = (Destination) protectedMethod.invoke(fixture, null);
        //manually change accessability of the method
        protectedMethod.setAccessible(false);

        assertEquals(mockDestination, destination);
    }

    @Test
    public void getFailureQueue() throws Exception {
        Queue mockFailureDestination = mock(Queue.class);
        Context mockContext = mock(Context.class);
        PowerMockito.when(mockJmsBag.getJndiContext()).thenReturn(mockContext);
        PowerMockito.when(mockContext.lookup("failureDestination")).thenReturn(mockFailureDestination);
        PowerMockito.when(mockJmsEndpointConfig.isTransactional()).thenReturn(true);

        when(JmsUtil.cast(mockFailureDestination,Queue.class)).thenReturn(mockFailureDestination);

        //reflectively get the method in question
        Method protectedMethod = fixture.getClass().getDeclaredMethod("getFailureQueue");
        //manually tell java that the method is accessible
        protectedMethod.setAccessible(true);
        //call the method
        Destination destination = (Destination) protectedMethod.invoke(fixture, null);
        //manually change accessability of the method
        protectedMethod.setAccessible(false);

        assertEquals(mockFailureDestination, destination);
    }

    @Test
    public void ensureConnectionStarted() throws Exception {
        //reflectively get the method in question
        Method protectedMethod = fixture.getClass().getDeclaredMethod("ensureConnectionStarted");
        //manually tell java that the method is accessible
        protectedMethod.setAccessible(true);

        //call the method
        protectedMethod.invoke(fixture, null);
        //manually change accessability of the method
        protectedMethod.setAccessible(false);
        verify(mockJmsBag, times(1)).getConnection();
        verify(mockConnection, times(1)).start();
        PowerMockito.verifyPrivate(fixture).invoke("fireConnected");
    }

    @Test
    public void start() throws Exception{
        when(mockJmsEndpoint.isQueue()).thenReturn(true);
        fixture.start();
        Thread listenerThread = Whitebox.getInternalState(fixture, "_thread");
        fixture.stop();
        listenerThread.join(SHUTDOWN_TIMEOUT);
        verify(mockJmsResourceManager, atLeastOnce()).borrowJmsBag(mockJmsEndpointConfig);
    }

    @Test
    public void onException() throws Exception {
        Whitebox.setInternalState(fixture, "_jmsBag", mockJmsBag);

        fixture.onException(new JMSException("test exception"));

        verify(mockJmsResourceManager, atLeastOnce()).returnJmsBag(mockJmsBag);
    }
}