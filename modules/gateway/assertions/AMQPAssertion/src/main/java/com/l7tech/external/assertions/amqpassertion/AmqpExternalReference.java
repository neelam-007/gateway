package com.l7tech.external.assertions.amqpassertion;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SafeXMLDecoder;
import com.l7tech.util.SafeXMLDecoderBuilder;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.booleanProperty;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.negate;
import static java.util.Collections.emptyList;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 13/03/12
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class AmqpExternalReference extends ExternalReference {
    public static final String EL_NAME_REF = "AmqpExternalReference";
    private Goid goid = SsgActiveConnector.DEFAULT_GOID;
    private SsgActiveConnector ssgActiveConnector;
    private ExternalReferenceFinder finder;
    private LocalizeAction localizeType;
    private Goid localGoid = SsgActiveConnector.DEFAULT_GOID;
    private static final Logger logger = Logger.getLogger(AmqpExternalReference.class.getName());

    public AmqpExternalReference(final ExternalReferenceFinder _finder) {
        super(_finder);
        this.finder = _finder;
    }

    public AmqpExternalReference(final ExternalReferenceFinder _finder, final Goid _goid) {
        super(_finder);
        this.setGoid(_goid);
        this.finder = _finder;
        try {
            this.setSsgActiveConnector(finder.findConnectorByPrimaryKey(_goid));
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot retrieve active connector. partial reference will be built", e);
        }

    }

    @Override
    public boolean setLocalizeReplace(final Goid connectorGoid) {
        localizeType = LocalizeAction.REPLACE;
        localGoid = connectorGoid;
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
        if (goid != SsgActiveConnector.DEFAULT_GOID) {

            Element refEl = referencesParentElement.getOwnerDocument().createElementNS(null, EL_NAME_REF);
            setTypeAttribute(refEl);
            referencesParentElement.appendChild(refEl);
            addParamEl(refEl, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SERVICEGOID, ssgActiveConnector.getGoid().toString(), false);
            addParamEl(refEl, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_NAME, ssgActiveConnector.getName(), false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_VIRTUALHOST, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CREDENTIALS_REQUIRED, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USERNAME, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_PASSWORD_GOID, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USESSL, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CIPHERSPEC, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SSL_CLIENT_KEY_ID, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_THREADPOOLSIZE, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ACKNOWLEDGEMENT_TYPE, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_REPLY_BEHAVIOUR, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_REPLY_QUEUE, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_CORRELATION_BEHAVIOUR, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CONTENT_TYPE_VALUE, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CONTENT_TYPE_PROPERTY_NAME, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_FAILURE_QUEUE_NAME, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_RESPONSE_QUEUE, refEl, false);
            setParameter(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_CORRELATION_BEHAVIOUR, refEl, false);
        }
    }

    private void setParameter(String propName, Element refEl, boolean alwaysAdd) {
        Object o = getSsgActiveConnector().getProperty(propName);
        if (o != null) {
            addParamEl(refEl, propName, o.toString(), alwaysAdd);
        } else {
            addParamEl(refEl, propName, null, alwaysAdd);
        }
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        ByteArrayInputStream bin = null;
        ByteArrayInputStream bin2 = null;
        SafeXMLDecoder xmlDecoder = null;
        SafeXMLDecoder xmlDecoder2 = null;
        try {
            SsgActiveConnector foundConnector = finder.findConnectorByPrimaryKey(ssgActiveConnector.getGoid());
            if (foundConnector != null) {
                String str = foundConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES);
                String str2 = ssgActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES);
                bin = new ByteArrayInputStream(str.getBytes());
                bin2 = new ByteArrayInputStream(str2.getBytes());
                xmlDecoder = new SafeXMLDecoderBuilder(bin).build();
                xmlDecoder2 = new SafeXMLDecoderBuilder(bin2).build();
                Object o = xmlDecoder.readObject();
                Object o2 = xmlDecoder2.readObject();
                if (o instanceof String[] && o2 instanceof String[]) {
                    String[] add = (String[]) o;
                    String[] add2 = (String[]) o2;
                    return Arrays.equals(add, add2);
                }
            } else {
                final Collection<SsgActiveConnector> outboundQueues = findAllOutboundQueues();
                for (SsgActiveConnector connector : outboundQueues) {
                    if (isMatch(connector.getName(), ssgActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME)) && permitMapping(goid, connector.getGoid())) {
                        // Connector Name matched
                        logger.fine("The AMQP queue was resolved from oid '" + goid + "' to '" + connector.getGoid() + "'");
                        localGoid = connector.getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }

                // Check if partial matched
                for (SsgActiveConnector connector : outboundQueues) {
                    if (isMatch(connector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME), ssgActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME)) &&
                            isMatch(connector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME), ssgActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME)) &&
                            isMatch(connector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR), ssgActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR)) &&
                            permitMapping(goid, connector.getGoid())) {
                        // Partial matched
                        logger.fine("The AMQP queue was resolved from goid '" + goid + "' to '" + connector.getGoid() + "'");
                        localGoid = connector.getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }
            }
        } catch (FindException e) {
            ExceptionUtils.wrap(e);
        } finally {
            if (xmlDecoder != null) {
                xmlDecoder.close();
                xmlDecoder = null;
            }
            if (xmlDecoder2 != null) {
                xmlDecoder2.close();
                xmlDecoder2 = null;
            }
            if (bin != null) {
                try {
                    bin.close();
                    bin = null;
                } catch (IOException e) {
                    //Ignore
                }
            }
            if (bin2 != null) {
                try {
                    bin2.close();
                    bin2 = null;
                } catch (IOException e) {
                    //Ignore.
                }
            }
        }
        return false;
    }

    @Override
    protected boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType == LocalizeAction.DELETE) {
            if (assertionToLocalize instanceof RouteViaAMQPAssertion) {
                logger.info("Delete this asserton from the tree");
                return false;
            }
        } else if (localizeType == LocalizeAction.REPLACE) {
            if (assertionToLocalize instanceof RouteViaAMQPAssertion) {
                final RouteViaAMQPAssertion assertion = (RouteViaAMQPAssertion) assertionToLocalize;
                assertion.setSsgActiveConnectorGoid(localGoid);
                assertion.setSsgActiveConnectorName(getActiveConnectorNameByGoid(localGoid));
            }
        } else if (localizeType == LocalizeAction.IGNORE) {
            return true;
        }
        return true;
    }

    private String getActiveConnectorNameByGoid(final Goid goid) {
        try {
            SsgActiveConnector activeConnector = getFinder().findConnectorByPrimaryKey(goid);
            if (activeConnector != null)
                return activeConnector.getName();
        } catch (FindException e) {
            logger.warning("could not retrieve the active connector from goid " + goid);
        }
        logger.warning("The goid " + goid + " could not be used to get an active connector connectorName.");
        return null;
    }

    public static AmqpExternalReference parseFromElement(final ExternalReferenceFinder context,
                                                         final Element el) throws InvalidDocumentFormatException {

        // make sure passed element has correct connectorName
        if (!el.getNodeName().equals(EL_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of connectorName " + EL_NAME_REF);
        }

        AmqpExternalReference output = new AmqpExternalReference(context);
        SsgActiveConnector ssgActiveConnector1 = new SsgActiveConnector();
        output.setSsgActiveConnector(ssgActiveConnector1);

        ssgActiveConnector1.setType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP);
        String goid = getParamFromEl(el, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SERVICEGOID);
        if (goid != null && !goid.trim().equals("")) {
            ssgActiveConnector1.setGoid(new Goid(goid));
            output.setGoid(new Goid(goid));
        }
        String name = getParamFromEl(el, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_NAME);
        if (name != null && !name.trim().equals("")) {
            ssgActiveConnector1.setName(name);
        }
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_VIRTUALHOST, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CREDENTIALS_REQUIRED, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USERNAME, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_PASSWORD_GOID, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_USESSL, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CIPHERSPEC, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SSL_CLIENT_KEY_ID, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_THREADPOOLSIZE, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ACKNOWLEDGEMENT_TYPE, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_REPLY_BEHAVIOUR, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_REPLY_QUEUE, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_INBOUND_CORRELATION_BEHAVIOUR, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CONTENT_TYPE_VALUE, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_CONTENT_TYPE_PROPERTY_NAME, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_FAILURE_QUEUE_NAME, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_RESPONSE_QUEUE, el);
        setConnectorValues(ssgActiveConnector1, AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_CORRELATION_BEHAVIOUR, el);
        return output;
    }

    private static void setConnectorValues(final SsgActiveConnector ssgActiveConnector1, final String propertyName, final Element e) {
        String str = getParamFromEl(e, propertyName);
        if (str != null && !str.trim().equals("")) {
            ssgActiveConnector1.setProperty(propertyName, str);
        }
    }

    public Goid getGoid() {
        return goid;
    }

    public void setGoid(Goid goid) {
        this.goid = goid;
    }

    @Override
    public String getRefId() {
        String id = null;

        if (goid != null && !goid.equals(SsgActiveConnector.DEFAULT_GOID)) {
            id = goid.toString();
        }

        return id;
    }

    public SsgActiveConnector getSsgActiveConnector() {
        return ssgActiveConnector;
    }

    public void setSsgActiveConnector(SsgActiveConnector ssgActiveConnector) {
        this.ssgActiveConnector = ssgActiveConnector;
    }

    private List<SsgActiveConnector> findAllOutboundQueues() {
        try {
            return grep(getFinder().findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP),
                    negate(booleanProperty(AmqpSsgActiveConnector.PROPERTIES_KEY_IS_INBOUND)));
        } catch (IllegalStateException e) {
            // no admin context available
            logger.info("Unable to access queues from server.");
        } catch (FindException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Error loading queues");
        }
        return emptyList();
    }

    private boolean isMissing(final String value) {
        return value == null || value.isEmpty();
    }

    private boolean isMatch(final String leftValue,
                            final String rightValue) {
        return isMissing(leftValue) ? isMissing(rightValue) : leftValue.equals(rightValue);
    }
}