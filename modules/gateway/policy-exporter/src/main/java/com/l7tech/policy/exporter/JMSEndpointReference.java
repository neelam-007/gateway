package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A reference to a JMSConnection used by an assertion of type JmsRoutingAssertion
 * included in an exported policy.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 19, 2004<br/>
 */
public class JMSEndpointReference extends ExternalReference {

    public JMSEndpointReference( final ExternalReferenceFinder context,
                                 final long endpointOid ) {
        this( context );
        oid = endpointOid;
        JmsConnection jmsConnection = null;
        try {
            JmsEndpoint jmsEndpoint = getFinder().findEndpointByPrimaryKey(endpointOid);
            if (jmsEndpoint != null) {
                name = jmsEndpoint.getName();
                type = jmsEndpoint.isQueue() ? TYPE_QUEUE : TYPE_TOPIC;
                destinationName = jmsEndpoint.getDestinationName();
                endpointTemplate = jmsEndpoint.isTemplate();
                jmsConnection = getFinder().findConnectionByPrimaryKey(jmsEndpoint.getConnectionOid());
            }
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "cannot retrieve endpoint. partial reference will be built", e);
            jmsConnection = null;
        } catch (FindException e){
            logger.log(Level.SEVERE, "cannot retrieve endpoint. partial reference will be built", e);
            jmsConnection = null;
        }
        if (jmsConnection != null) {
            connectionTemplate = jmsConnection.isTemplate();
            initialContextFactoryClassname = jmsConnection.getInitialContextFactoryClassname();
            jndiUrl = jmsConnection.getJndiUrl();
            queueFactoryUrl = jmsConnection.getQueueFactoryUrl();
            topicFactoryUrl = jmsConnection.getTopicFactoryUrl();
            destinationFactoryUrl = jmsConnection.getDestinationFactoryUrl();
        }
    }

    public static JMSEndpointReference parseFromElement( final ExternalReferenceFinder context,
                                                         final Element el ) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        JMSEndpointReference output = new JMSEndpointReference(context);
        String val = getParamFromEl(el, OID_EL_NAME);
        if (val != null) {
            output.oid = Long.parseLong(val);
        }
        output.name = getParamFromEl(el, NAME_EL_NAME);
        output.type = getParamFromEl(el, TYPE_EL_NAME);
        output.connectionTemplate = Boolean.parseBoolean(getParamFromEl(el, CONNECTION_TEMPLATE_EL_NAME));
        output.endpointTemplate = Boolean.parseBoolean(getParamFromEl(el, ENDPOINT_TEMPLATE_EL_NAME));
        output.destinationName = getParamFromEl(el, DESTINATION_EL_NAME);
        if ( output.destinationName == null ) {
            output.destinationName = getParamFromEl(el, EPNAME_EL_NAME);
        }
        output.initialContextFactoryClassname = getParamFromEl(el, CONTEXT_EL_NAME);
        output.jndiUrl = getParamFromEl(el, JNDI_EL_NAME);
        output.queueFactoryUrl = getParamFromEl(el, QUEUE_EL_NAME);
        output.topicFactoryUrl = getParamFromEl(el, TOPIC_EL_NAME);
        output.destinationFactoryUrl = getParamFromEl(el, DESTINATIONURL_EL_NAME );
        return output;
    }

    private JMSEndpointReference( final ExternalReferenceFinder finder ) {
        super( finder );
    }

    @Override
    public String getRefId() { 
        String id = null;

        if ( oid > 0 ) {
            id = Long.toString( oid );
        }

        return id;
    }

    public long getOid() {
        return oid;
    }

    /**
     * Get the name of the JMS Endpoint.
     *
     * @return The endpoint name or null.
     * @since 5.3
     */
    public String getName() {
        return name;
    }

    /**
     * Is this JMS Endpoint a Queue (rather than a Topic)
     *
     * @return True for a Queue, False for a Topic
     */
    public boolean isQueue() {
        return !TYPE_TOPIC.equals( type );
    }

    public boolean isConnectionTemplate() {
        return connectionTemplate;
    }

    public boolean isEndpointTemplate() {
        return endpointTemplate;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getInitialContextFactoryClassname() {
        return initialContextFactoryClassname;
    }

    public String getJndiUrl() {
        return jndiUrl;
    }

    public String getQueueFactoryUrl() {
        return queueFactoryUrl;
    }

    public String getTopicFactoryUrl() {
        return topicFactoryUrl;
    }

    public String getDestinationFactoryUrl() {
        return destinationFactoryUrl;
    }

    @Override
    public boolean setLocalizeReplace( final long newEndpointId ) {
        localizeType = LocalizeAction.REPLACE;
        localEndpointId = newEndpointId;
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
    protected void serializeToRefElement( final Element referencesParentElement ) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        setTypeAttribute( refEl );
        referencesParentElement.appendChild(refEl);

        addElement( refEl, OID_EL_NAME, Long.toString(oid) );
        addElement( refEl, CONNECTION_TEMPLATE_EL_NAME, Boolean.toString(connectionTemplate));
        addElement( refEl, ENDPOINT_TEMPLATE_EL_NAME, Boolean.toString(endpointTemplate));
        addElement( refEl, CONTEXT_EL_NAME, initialContextFactoryClassname );
        addElement( refEl, JNDI_EL_NAME, jndiUrl );
        addElement( refEl, QUEUE_EL_NAME, queueFactoryUrl );
        addElement( refEl, TOPIC_EL_NAME, topicFactoryUrl );
        addElement( refEl, DESTINATIONURL_EL_NAME, destinationFactoryUrl );
        addElement( refEl, NAME_EL_NAME, name );
        addElement( refEl, DESTINATION_EL_NAME, destinationName );
        addElement( refEl, TYPE_EL_NAME, type );
    }

    private void addElement( final Element parent,
                             final String childElementName,
                             final String text ) {
        Element childElement = parent.getOwnerDocument().createElement( childElementName );
        parent.appendChild(childElement);

        if ( text != null ) {
            Text textNode = DomUtils.createTextNode(parent, text);
            childElement.appendChild( textNode );
        }
    }

    public String getDisplayName() {
        String displayName = getName();

        if ( displayName == null ) {
            if ( getDestinationName() != null ) {
                displayName = "queue name '" + getDestinationName() + "'";                
            } else {
                displayName = "Unknown";
            }
        }

        return displayName;
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        final JMSEndpointReference that = (JMSEndpointReference) o;

        if ( oid != that.oid ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (oid ^ (oid >>> 32));
    }

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     */
    @Override
    protected boolean verifyReference() {
        try {
            Collection<Pair<JmsEndpoint,JmsConnection>> tempMatches = new ArrayList<Pair<JmsEndpoint,JmsConnection>>(); // contains JmsAdmin.JmsTuple objects that have partial match
            List<Pair<JmsEndpoint,JmsConnection>> jmsQueues = getFinder().loadJmsQueues();
            for (Pair<JmsEndpoint,JmsConnection> jmsTuple : jmsQueues) {
                // what makes a jms queue the same?
                // let's say a combination of JndiUrl, CONTEXT_EL_NAME, QueueFactoryUrl, destination type, destination name
                if (jmsTuple.getKey().isMessageSource()) {
                    continue;
                } else if ( !isMatch(jmsTuple.getValue().getJndiUrl(), jndiUrl) ||
                            !isMatch(jmsTuple.getValue().getInitialContextFactoryClassname(), initialContextFactoryClassname) ||
                            !isMatch(jmsTuple.getValue().getQueueFactoryUrl(), queueFactoryUrl) ||
                            jmsTuple.getKey().isQueue() != isQueue() ||
                            !isMatch(jmsTuple.getKey().getDestinationName(), destinationName)) {
                    continue;
                }
                // we have a match
                tempMatches.add(jmsTuple);
            }

            if (tempMatches.isEmpty()) {
                logger.warning("The JMS endpoint cannot be resolved.");
            } else {
                // explicitly discriminate using the endpoint name
                for (Pair<JmsEndpoint,JmsConnection> jmsTuple : tempMatches) {
                    if ( jmsTuple.getKey().getName().equals(name) &&
                         permitMapping( getOid(), jmsTuple.getKey().getOid() )) {
                        // WE HAVE A PERFECT MATCH!
                        logger.fine("The local JMS endpoint was resolved from oid " + getOid() + " to " + jmsTuple.getKey().getOid());
                        localEndpointId = jmsTuple.getKey().getOid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }

                // explicitly discriminate for template endpoint and template connection, if not template, make sure fields are not empty
                for (Pair<JmsEndpoint,JmsConnection> jmsTuple : tempMatches) {
                    if ( ( isTemplate(jmsTuple) ||
                          (!isTemplate(jmsTuple) && !isMissing(jndiUrl) && !isMissing(initialContextFactoryClassname) && !isMissing(queueFactoryUrl) && !isMissing(destinationName)) ) &&
                         permitMapping( getOid(), jmsTuple.getKey().getOid() )) {
                        logger.fine("The local JMS endpoint was resolved from oid " + getOid() + " to " + jmsTuple.getKey().getOid());
                        localEndpointId = jmsTuple.getKey().getOid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }
            }
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Cannot load JMS queues '"+ ExceptionUtils.getMessage( e )+"'.", ExceptionUtils.getDebugException( e ));
        }

        return false;
    }

    private boolean isMissing( final String value ) {
        return value == null || value.isEmpty();
    }

    private boolean isMatch( final String leftValue,
                             final String rightValue) {
        return isMissing(leftValue) ? isMissing(rightValue) : leftValue.equals(rightValue);
    }

    private boolean isTemplate(final Pair<JmsEndpoint,JmsConnection> jmsTuple) {
        return jmsTuple.getKey().isTemplate() == endpointTemplate && jmsTuple.getValue().isTemplate() == connectionTemplate;
    }

    @Override
    protected boolean localizeAssertion( final @Nullable Assertion assertionToLocalize ) {
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof JmsRoutingAssertion) {
            JmsRoutingAssertion jmsRoutingAssertion = (JmsRoutingAssertion) assertionToLocalize;
                if (jmsRoutingAssertion.getEndpointOid() != null &&
                    jmsRoutingAssertion.getEndpointOid() == oid) {
                    if (localizeType == LocalizeAction.REPLACE) {
                        // replace endpoint id
                        jmsRoutingAssertion.setEndpointOid(localEndpointId);
                        // replace endpoint name
                        jmsRoutingAssertion.setEndpointName(endpointNameFromOid(localEndpointId));
                        logger.info("The endpoint id of the imported jms routing assertion has been changed " +
                                    "from " + oid + " to " + localEndpointId);
                    } else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String endpointNameFromOid( final long oid ) {
        try {
            JmsEndpoint endpoint = getFinder().findEndpointByPrimaryKey(oid);
            if (endpoint != null) return endpoint.getName();
        } catch (FindException e) {
            logger.warning("could not retrieve the JMS endpoint from oid " + oid);
        }
        logger.warning("The oid " + oid + " could not be used to get an endpoint name.");
        return null;
    }

    private final Logger logger = Logger.getLogger(JMSEndpointReference.class.getName());

    private static final String TYPE_QUEUE = "Queue";
    private static final String TYPE_TOPIC = "Topic";

    private long oid;
    private long localEndpointId;
    private String name; // Added in 5.3, will be null in earlier exports
    private String type; // Added in 5.4, will be null in earlier exports
    private boolean connectionTemplate;
    private boolean endpointTemplate;
    private String destinationName;
    private String initialContextFactoryClassname;
    private String jndiUrl;
    private String queueFactoryUrl;
    private String topicFactoryUrl;
    private String destinationFactoryUrl;
    private LocalizeAction localizeType = null;
    public static final String REF_EL_NAME = "JMSConnectionReference";
    public static final String OID_EL_NAME = "OID";
    public static final String CONNECTION_TEMPLATE_EL_NAME = "ConnectionTemplate";
    public static final String ENDPOINT_TEMPLATE_EL_NAME = "EndpointTemplate";
    public static final String NAME_EL_NAME = "Name";
    public static final String TYPE_EL_NAME = "Type";
    public static final String DESTINATION_EL_NAME = "DestinationName";
    public static final String EPNAME_EL_NAME = "EndpointName"; // used in pre 5.3 exports, value was the "Queue Name"
    public static final String CONTEXT_EL_NAME = "InitialContextFactoryClassname";
    public static final String JNDI_EL_NAME = "JndiUrl";
    public static final String QUEUE_EL_NAME = "QueueFactoryUrl";
    public static final String TOPIC_EL_NAME = "TopicFactoryUrl";
    public static final String DESTINATIONURL_EL_NAME = "DestinationFactoryUrl";
}
