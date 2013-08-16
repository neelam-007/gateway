/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsOutboundMessageType;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.JmsEndpointHeader;
import com.l7tech.server.GoidEntityManagerStub;
import com.l7tech.server.transport.jms.JmsEndpointManager;

import java.util.Collection;

/**
 *
 * @author: vchan
 */
public class JmsEndpointManagerStub extends GoidEntityManagerStub<JmsEndpoint, JmsEndpointHeader> implements JmsEndpointManager {

    JmsConnectionManagerStub cnxMgr;

    public Collection findMessageSourceEndpoints() throws FindException {
        throw new UnsupportedOperationException();
    }

    public JmsEndpoint[] findEndpointsForConnection(Goid connectionOid) throws FindException {

        return getEndpointForConnection(connectionOid);
    }

    public JmsEndpointHeader[] findEndpointHeadersForConnection(Goid connectionOid) throws FindException {
        throw new UnsupportedOperationException();
    }

    protected JmsEndpoint[] getEndpointForConnection(Goid connectionOid) {

        JmsEndpoint[] result = { buildEndpoint((int)connectionOid.getLow()) };
        return result;
    }

    public JmsEndpoint findByPrimaryKey(Goid oid) throws FindException {
        JmsEndpoint endpt;

        if (oid.getLow() == 101L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_AMQ_IN);
        } else if (oid.getLow() == 102L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_AMQ_OUT);
        } else if (oid.getLow() == 103L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_MQS_IN);
        } else if (oid.getLow() == 104L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_MQS_OUT);
        } else if (oid.getLow() == 107L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_DYNAMIC_IN);
        } else if (oid.getLow() == 108L) {
            endpt = buildEndpoint(JmsConnectionManagerStub.TEST_CONFIG_DYNAMIC_OUT);
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
                endpt.setGoid(new Goid(0, 101));
                endpt.setVersion(0);
                endpt.setConnectionGoid(new Goid(0,which));
                endpt.setName("dynamicQueues/JMS.JUNIT.IN.Q");
                endpt.setDestinationName("dynamicQueues/JMS.JUNIT.IN.Q");
                endpt.setReplyType(JmsReplyType.AUTOMATIC);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_AMQ_OUT: {
                endpt.setGoid(new Goid(0, 102));
                endpt.setVersion(0);
                endpt.setConnectionGoid(new Goid(0,which));
                endpt.setName("dynamicQueues/JMS.JUNIT.OUT.Q");
                endpt.setDestinationName("dynamicQueues/JMS.JUNIT.OUT.Q");
                endpt.setReplyType(JmsReplyType.NO_REPLY);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_MQS_IN: {
                endpt.setGoid(new Goid(0, 103));
                endpt.setVersion(0);
                endpt.setConnectionGoid(new Goid(0,which));
                endpt.setName("cn=VCTEST.Q.IN");
                endpt.setDestinationName("cn=VCTEST.Q.IN");
                endpt.setReplyType(JmsReplyType.NO_REPLY);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_MQS_OUT: {
                endpt.setGoid(new Goid(0, 104));
                endpt.setVersion(0);
                endpt.setConnectionGoid(new Goid(0,which));
                endpt.setName("cn=VCTEST.Q.OUT");
                endpt.setDestinationName("cn=VCTEST.Q.OUT");
                endpt.setReplyType(JmsReplyType.NO_REPLY);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_DYNAMIC_IN: {
                endpt.setGoid(new Goid(0, 107));
                endpt.setVersion(0);
                endpt.setConnectionGoid(new Goid(0,which));
                endpt.setName("ilona.in1");
                endpt.setDestinationName("ilona.in1");
                endpt.setReplyType(JmsReplyType.REPLY_TO_OTHER);
                endpt.setReplyToQueueName("ilona.in3");
                endpt.setAcknowledgementType(JmsAcknowledgementType.ON_COMPLETION);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            case JmsConnectionManagerStub.TEST_CONFIG_DYNAMIC_OUT: {
                endpt.setGoid(new Goid(0, 108));
                endpt.setVersion(0);
                endpt.setConnectionGoid(new Goid(0,which));
                endpt.setName("ilona.in1");
                endpt.setDestinationName("ilona.in1");
                endpt.setReplyType(JmsReplyType.NO_REPLY);
                endpt.setOutboundMessageType(JmsOutboundMessageType.AUTOMATIC);
                endpt.setUseMessageIdForCorrelation(false);
                break;
            }
            default : {
                // use CONN_AMQ_JUNIT_IN as default
                endpt.setGoid(new Goid(0, 666));
                endpt.setVersion(0);
                endpt.setConnectionGoid(new Goid(0,which));
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
