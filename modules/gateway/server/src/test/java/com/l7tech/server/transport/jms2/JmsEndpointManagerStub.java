/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms2;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.gateway.common.transport.jms.JmsOutboundMessageType;
import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;

import java.util.Collection;

/**
 *
 * @author: vchan
 */
public class JmsEndpointManagerStub extends EntityManagerStub<JmsEndpoint, EntityHeader> implements JmsEndpointManager {

    JmsConnectionManagerStub cnxMgr;

    public Collection findMessageSourceEndpoints() throws FindException {
        throw new UnsupportedOperationException();
    }

    public JmsEndpoint[] findEndpointsForConnection(long connectionOid) throws FindException {

        return getEndpointForConnection(connectionOid);
    }

    public EntityHeader[] findEndpointHeadersForConnection(long connectionOid) throws FindException {
        throw new UnsupportedOperationException();
    }

    protected JmsEndpoint[] getEndpointForConnection(long connectionOid) {

        JmsEndpoint[] result = { buildEndpoint(Integer.valueOf(Long.toString(connectionOid))) };
        return result;
    }

    public JmsEndpoint findByPrimaryKey(long oid) throws FindException {
        JmsEndpoint endpt;

        if (oid == 101L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_AMQ_IN);
        } else if (oid == 102L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_AMQ_OUT);
        } else if (oid == 103L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_MQS_IN);
        } else if (oid == 104L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_MQS_OUT);
        } else if (oid == 105L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_FMQ_IN);
        } else if (oid == 106L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_FMQ_OUT);
        } else {
            endpt = buildEndpoint(6666);
        }

        return endpt;
    }

    private JmsEndpoint buildEndpoint(int which) {

        JmsEndpoint endpt = new JmsEndpoint();
        // common for all
        endpt.setDisabled(false);
        endpt.setMaxConcurrentRequests(1);
        endpt.setMessageSource(true);

        switch(which) {

            case JmsConnectionManagerStub.TEST_CONFIG_AMQ_IN: {
                endpt.setOid(101);
                endpt.setVersion(0);
                endpt.setConnectionOid(which);
                endpt.setName("dynamicQueues/JMS.JUNIT.IN.Q");
                endpt.setDestinationName("dynamicQueues/JMS.JUNIT.IN.Q");
                endpt.setReplyType(JmsReplyType.AUTOMATIC);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_AMQ_OUT: {
                endpt.setOid(102);
                endpt.setVersion(0);
                endpt.setConnectionOid(which);
                endpt.setName("dynamicQueues/JMS.JUNIT.OUT.Q");
                endpt.setDestinationName("dynamicQueues/JMS.JUNIT.OUT.Q");
                endpt.setReplyType(JmsReplyType.NO_REPLY);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_MQS_IN: {
                endpt.setOid(103);
                endpt.setVersion(0);
                endpt.setConnectionOid(which);
                endpt.setName("cn=VCTEST.Q.IN");
                endpt.setDestinationName("cn=VCTEST.Q.IN");
                endpt.setReplyType(JmsReplyType.NO_REPLY);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_MQS_OUT: {
                endpt.setOid(104);
                endpt.setVersion(0);
                endpt.setConnectionOid(which);
                endpt.setName("cn=VCTEST.Q.OUT");
                endpt.setDestinationName("cn=VCTEST.Q.OUT");
                endpt.setReplyType(JmsReplyType.NO_REPLY);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_FMQ_IN: {
                endpt.setOid(105);
                endpt.setVersion(0);
                endpt.setConnectionOid(which);
                endpt.setName("vchan_in");
                endpt.setDestinationName("vchan_in");
                endpt.setReplyType(JmsReplyType.REPLY_TO_OTHER);
                endpt.setReplyToQueueName("vchan_reply");
                endpt.setAcknowledgementType(JmsAcknowledgementType.ON_COMPLETION);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_FMQ_OUT: {
                endpt.setOid(106);
                endpt.setVersion(0);
                endpt.setConnectionOid(which);
                endpt.setName("vchan_out");
                endpt.setDestinationName("vchan_out");
                endpt.setReplyType(JmsReplyType.NO_REPLY);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            default : {
                // use CONN_AMQ_JUNIT_IN as default
                endpt.setOid(666);
                endpt.setVersion(0);
                endpt.setConnectionOid(which);
                endpt.setName("dynamicQueues/JMS.JUNIT.Q");
                endpt.setDestinationName("dynamicQueues/JMS.JUNIT.Q");
                endpt.setReplyType(JmsReplyType.AUTOMATIC);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
        }
        return endpt;
    }


}