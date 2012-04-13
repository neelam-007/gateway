package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQC;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.*;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.io.IOException;

import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_AUTOMATIC;
import static com.l7tech.external.assertions.mqnative.server.MqNativeModule.DEFAULT_MESSAGE_MAX_BYTES;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test the MqNativeListenerThread component without dependency on a live MQ server.
 */
@RunWith(MockitoJUnitRunner.class)
public class MqNativeListenerThreadTest extends AbstractJUnit4SpringContextTests {

    /**
     * Exercise polling thread logic in MqNativeListenerThread.run()
     */
    @Test
    public void listenerThreadRun() {
        // TODO
    }
}
