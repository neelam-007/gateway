package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import com.l7tech.util.Triple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.Hashtable;

import static com.ibm.mq.constants.CMQC.MQGMO_SYNCPOINT;
import static com.ibm.mq.constants.CMQC.MQGMO_WAIT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class MqNativeClientTest extends AbstractJUnit4SpringContextTests {

    @Mock
    private MqNativeClient.MqNativeConnectionListener listener;
    @Mock
    private Option<String> replyQueueName;
    @Mock
    private MQQueueManager mqQueueManager;
    @Mock
    private MQQueue mqQueue;
    @Mock
    private MQMessage mqMessage;
    @Mock
    private Functions.NullaryThrows<Hashtable, MqNativeConfigException>  queueManagerProperties;

    @Before
    public void initMocks() throws MqNativeConfigException {

        MockitoAnnotations.initMocks(this);

        when(queueManagerProperties.call()).thenReturn(new Hashtable());
    }

    @Test
    public void constructorTest() throws MQException, MqNativeConfigException {

        MqNativeClient mqNativeClient = new MqNativeClient("TheQueueManager",queueManagerProperties,"ReplyQueue",replyQueueName,listener);
        MqNativeClient spyMqNativeClient = spy(mqNativeClient);
        MQQueueManager mqQueueManager = mock(MQQueueManager.class);
        MQQueue targetQueue = mock(MQQueue.class);
        MQQueue specifiedReplyQueue = mock(MQQueue.class);


        //  Test normal patch execution.
        doReturn(new Triple<MQQueueManager, MQQueue, MQQueue>(mqQueueManager, targetQueue, specifiedReplyQueue))
                .when(spyMqNativeClient).getMqContext(anyString(), (Hashtable) any());

        spyMqNativeClient.doWork(
                new Functions.UnaryThrows<MQMessage, MqNativeClient.ClientBag, MQException>() {
                    @Override
                    public MQMessage call(final MqNativeClient.ClientBag bag) throws MQException {
                        final MQGetMessageOptions getOptions = new MQGetMessageOptions();
                        getOptions.options = MQGMO_WAIT | MQGMO_SYNCPOINT;
                        return mqMessage;
                    }
                }, true);

        verify(listener,times(1)).notifyConnected();

        try {
            spyMqNativeClient.doWork(
                new Functions.UnaryThrows<MQMessage,MqNativeClient.ClientBag,MQException>() {
                    @Override
                    public MQMessage call( final MqNativeClient.ClientBag bag ) throws MQException {
                        final MQGetMessageOptions getOptions = new MQGetMessageOptions();
                        getOptions.options = MQGMO_WAIT | MQGMO_SYNCPOINT;
                        return mqMessage;
                    }
                },false);
            assertFalse(true);
        } catch (MqNativeConfigException e) {
            // In this context the MqNativeConfigException is expected!
        }

        verify(listener,times(1)).notifyConnected();

        // Test two Exception paths.
        // #1
        doThrow(new MQException(1, 1, this)).when(spyMqNativeClient).getMqContext(anyString(), (Hashtable) any());

        try {
            spyMqNativeClient.doWork(
                    new Functions.UnaryThrows<MQMessage, MqNativeClient.ClientBag, MQException>() {
                        @Override
                        public MQMessage call(final MqNativeClient.ClientBag bag) throws MQException {
                            final MQGetMessageOptions getOptions = new MQGetMessageOptions();
                            getOptions.options = MQGMO_WAIT | MQGMO_SYNCPOINT;
                            return mqMessage;
                        }
                    }, true);
        } catch ( MQException e ) {
            // In this context the MQException is expected!
        }

        verify(listener,times(1)).notifyConnectionError(anyString());

        // #2
        doThrow(new MqNativeConfigException("test")).when(spyMqNativeClient).getMqContext(anyString(), (Hashtable) any());

        try {
            spyMqNativeClient.doWork(
                    new Functions.UnaryThrows<MQMessage, MqNativeClient.ClientBag, MQException>() {
                        @Override
                        public MQMessage call(final MqNativeClient.ClientBag bag) throws MQException {
                            final MQGetMessageOptions getOptions = new MQGetMessageOptions();
                            getOptions.options = MQGMO_WAIT | MQGMO_SYNCPOINT;
                            return mqMessage;
                        }
                    }, true);
        } catch ( MqNativeConfigException e ) {
            // In this context the MqNativeConfigException is expected!
        }

        verify(listener,times(2)).notifyConnectionError(anyString());
    }
}
