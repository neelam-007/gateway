package com.l7tech.external.assertions.mqnative;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.negate;
import static java.util.Collections.emptyList;

/**
 * @author ghuang
 */
public class MqNativeExternalReference extends ExternalReference {
    public static final String EL_NAME_REF = "MqNativeExternalReference";
    public static final String EL_NAME_OID = "OID";
    public static final String EL_NAME_CONN_NAME = "ConnectorName";
    public static final String EL_NAME_QUEUE_NAME = "QueueName";
    public static final String EL_NAME_CHANNEL_NAME = "ChannelName";
    public static final String EL_NAME_QUEUE_MANAGER_NAME = "QueueManagerName";
    public static final String EL_NAME_HOST = "Host";
    public static final String EL_NAME_PORT = "Port";

    private final Logger logger = Logger.getLogger(MqNativeExternalReference.class.getName());

    private long oid;
    private String connectorName;
    private String host;
    private long port;
    private String queueManagerName;
    private String channelName;
    private String queueName;

    private long localOid;
    private LocalizeAction localizeType;

    public MqNativeExternalReference(ExternalReferenceFinder finder) {
        super(finder);
    }

    public MqNativeExternalReference(ExternalReferenceFinder finder, long oid) {
        this(finder);
        this.oid = oid;

        try {
            SsgActiveConnector connector = getFinder().findConnectorByPrimaryKey(oid);
            if (connector == null)
                throw new IllegalArgumentException("The MQ Native Queue (oid = " + oid + ") does not exist");

            connectorName = connector.getName();
            host = connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME);
            port = Integer.parseInt(connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_PORT));
            queueManagerName = connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME);
            channelName = connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL);
            queueName = connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME);
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot retrieve active connector. partial reference will be built", e);
        }
    }

    public String getConnectorName() {
        return connectorName;
    }

    public String getHost() {
        return host;
    }

    public long getPort() {
        return port;
    }

    public String getQueueManagerName() {
        return queueManagerName;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getQueueName() {
        return queueName;
    }

    @Override
    public boolean setLocalizeReplace(final long connectorOid) {
        localizeType = LocalizeAction.REPLACE;
        localOid = connectorOid;
        return true;
    }

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElementNS(null,EL_NAME_REF);
        setTypeAttribute(refEl);
        referencesParentElement.appendChild(refEl);

        addParamEl(refEl, EL_NAME_OID, Long.toString(oid), false);
        addParamEl(refEl, EL_NAME_CONN_NAME, connectorName, false);
        addParamEl(refEl, EL_NAME_HOST, host, false);
        addParamEl(refEl, EL_NAME_PORT, Long.toString(port), false);
        addParamEl(refEl, EL_NAME_QUEUE_MANAGER_NAME, queueManagerName, false);
        addParamEl(refEl, EL_NAME_CHANNEL_NAME, channelName, false);
        addParamEl(refEl, EL_NAME_QUEUE_NAME, queueName, false);
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {

        try {
            final SsgActiveConnector activeConnector = getFinder().findConnectorByPrimaryKey(oid);
            if (activeConnector != null) {
                if (isMatch(activeConnector.getName(), connectorName) && permitMapping(oid, activeConnector.getOid())) {
                    // Perfect Match (OID and name are matched.)
                    logger.fine("The MQ Native queue was resolved by oid '" + oid + "' and name '" + activeConnector.getName() + "'");
                    return true;
                }
            } else {
                final Collection<SsgActiveConnector> outboundQueues = findAllOutboundQueues();
                for (SsgActiveConnector connector: outboundQueues) {
                    if (isMatch(connector.getName(), connectorName) && permitMapping(oid, connector.getOid())) {
                        // Connector Name matched
                        logger.fine("The MQ Native queue was resolved from oid '" + oid + "' to '" + connector.getOid() + "'");
                        localOid = connector.getOid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }

                // Check if partial matched
                for (SsgActiveConnector connector: outboundQueues) {
                    if (isMatch(connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME), host) &&
                        isMatch(connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_PORT), Long.toString(port)) &&
                        isMatch(connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME), queueManagerName) &&
                        isMatch(connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL), channelName) &&
                        isMatch(connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME), queueName) &&
                        permitMapping(oid, connector.getOid())) {
                        // Partial matched
                        logger.fine("The MQ Native queue was resolved from oid '" + oid + "' to '" + connector.getOid() + "'");
                        localOid = connector.getOid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }
            }
        } catch (FindException e) {
            logger.warning("Cannot load Active Connector from oid, " + oid);
        }

        return false;
    }

    @Override
    protected boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE){
            if (assertionToLocalize instanceof MqNativeRoutingAssertion) {
                final MqNativeRoutingAssertion mqNativeRoutingAssertion = (MqNativeRoutingAssertion) assertionToLocalize;
                final Long connectorId = mqNativeRoutingAssertion.getSsgActiveConnectorId();
                if (connectorId != null && connectorId == oid) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if (localizeType == LocalizeAction.REPLACE) {
                        mqNativeRoutingAssertion.setSsgActiveConnectorId(localOid);
                        mqNativeRoutingAssertion.setSsgActiveConnectorName(getActiveConnectorNameByOid(localOid));
                    }  else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static MqNativeExternalReference parseFromElement(final ExternalReferenceFinder context,
                                                               final Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct connectorName
        if (!el.getNodeName().equals(EL_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of connectorName " + EL_NAME_REF);
        }

        MqNativeExternalReference output = new MqNativeExternalReference(context);

        String oid = getParamFromEl(el, EL_NAME_OID);
        if (oid != null) {
            output.oid = Long.parseLong(oid);
        }
        output.connectorName = getParamFromEl(el, EL_NAME_CONN_NAME);
        output.host = getParamFromEl(el, EL_NAME_HOST);
        output.port = Integer.parseInt(getParamFromEl(el, EL_NAME_PORT));
        output.queueManagerName = getParamFromEl(el, EL_NAME_QUEUE_MANAGER_NAME);
        output.channelName = getParamFromEl(el, EL_NAME_CHANNEL_NAME);
        output.queueName = getParamFromEl(el, EL_NAME_QUEUE_NAME);

        return output;
    }

    private String getActiveConnectorNameByOid( final long oid ) {
        try {
            SsgActiveConnector activeConnector = getFinder().findConnectorByPrimaryKey(oid);
            if (activeConnector != null)
                return activeConnector.getName();
        } catch (FindException e) {
            logger.warning("could not retrieve the active connector from oid " + oid);
        }
        logger.warning("The oid " + oid + " could not be used to get an active connector connectorName.");
        return null;
    }

    private List<SsgActiveConnector> findAllOutboundQueues() {
        try {
            return grep( getFinder().findSsgActiveConnectorsByType(ACTIVE_CONNECTOR_TYPE_MQ_NATIVE ),
                negate( booleanProperty( PROPERTIES_KEY_IS_INBOUND ) ) );
        } catch ( IllegalStateException e ) {
            // no admin context available
            logger.info( "Unable to access queues from server." );
        } catch ( FindException e ) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error loading queues" );
        }
        return emptyList();
    }

    private boolean isMissing( final String value ) {
        return value == null || value.isEmpty();
    }

    private boolean isMatch( final String leftValue,
                             final String rightValue) {
        return isMissing(leftValue) ? isMissing(rightValue) : leftValue.equals(rightValue);
    }
}