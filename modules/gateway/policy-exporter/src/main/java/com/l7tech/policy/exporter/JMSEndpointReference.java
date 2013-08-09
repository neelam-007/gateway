package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.util.*;
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
                                 final Either<Long,Goid> endpointId ) {
        this( context );
        JmsConnection jmsConnection = null;
        try {
            JmsEndpoint jmsEndpoint = getFinder().findEndpointByOidOrGoid(endpointId);
            if (jmsEndpoint != null) {
                goid = jmsEndpoint.getGoid();
                name = jmsEndpoint.getName();
                type = jmsEndpoint.isQueue() ? TYPE_QUEUE : TYPE_TOPIC;
                destinationName = jmsEndpoint.getDestinationName();
                endpointTemplate = jmsEndpoint.isTemplate();
                oldOid = jmsEndpoint.getOldOid();
                jmsConnection = getFinder().findConnectionByPrimaryKey(jmsEndpoint.getConnectionGoid());
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
        String val = getParamFromEl(el,"OID");
        if (val != null) {
            output.oldOid = Long.parseLong(val);
            output.goid = JmsEndpoint.DEFAULT_GOID;
        }
        val = getParamFromEl(el, GOID_EL_NAME);
        if (val != null) {
            output.goid = new Goid(val);
        }

        val = getParamFromEl(el, OLD_OID_EL_NAME);
        if (val != null) {
            output.oldOid = Long.parseLong(val);
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

        if ( !goid.equals(JmsEndpoint.DEFAULT_GOID) ) {
            id = goid.toString();
        } else if ( oldOid > 0 ){
            id = Long.toString(oldOid);
        }

        return id;
    }

    public Goid getGoid() {
        return goid;
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

    public Long getOldOid() {
        return oldOid;
    }

    @Override
    public boolean setLocalizeReplace( final Goid newEndpointId ) {
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

        addElement( refEl, GOID_EL_NAME, goid==null?null:goid.toString() );
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
        if(oldOid!=null)
            addElement( refEl, OLD_OID_EL_NAME, Long.toString(oldOid));
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

        if (  oldOid==null  || !oldOid.equals(that.oldOid) || oldOid == that.oldOid ) return false;

        if ( goid==null  || !goid.equals(that.goid) || goid == that.goid) return false;


        return true;
    }

    @Override
    public int hashCode() {
        return goid.hashCode();
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
                         permitMapping( getGoid(), jmsTuple.getKey().getGoid() )) {
                        // WE HAVE A PERFECT MATCH!
                        logger.fine("The local JMS endpoint was resolved from oid " + getGoid() + " to " + jmsTuple.getKey().getGoid());
                        localEndpointId = jmsTuple.getKey().getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }

                // explicitly discriminate for template endpoint and template connection, if not template, make sure fields are not empty
                for (Pair<JmsEndpoint,JmsConnection> jmsTuple : tempMatches) {
                    if ( ( isTemplate(jmsTuple) ||
                          (!isTemplate(jmsTuple) && !isMissing(jndiUrl) && !isMissing(initialContextFactoryClassname) && !isMissing(queueFactoryUrl) && !isMissing(destinationName)) ) &&
                         permitMapping( getGoid(), jmsTuple.getKey().getGoid() )) {
                        logger.fine("The local JMS endpoint was resolved from oid " + getGoid() + " to " + jmsTuple.getKey().getGoid());
                        localEndpointId = jmsTuple.getKey().getGoid();
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
                if ((jmsRoutingAssertion.getEndpointOid() != null &&
                    jmsRoutingAssertion.getEndpointOid().equals(oldOid) ) ||( jmsRoutingAssertion.getEndpointGoid() != null &&
                        jmsRoutingAssertion.getEndpointGoid().equals(goid.toString()))  ) {
                    if (localizeType == LocalizeAction.REPLACE) {
                        // replace endpoint id
                        jmsRoutingAssertion.setEndpointGoid(localEndpointId.toString());
                        // replace endpoint name
                        jmsRoutingAssertion.setEndpointName(endpointNameFromGoid(localEndpointId));
                        logger.info("The endpoint id of the imported jms routing assertion has been changed " +
                                    "from " + goid + " to " + localEndpointId);
                    } else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String endpointNameFromGoid( final Goid goid ) {
        try {
            JmsEndpoint endpoint = getFinder().findEndpointByPrimaryKey(goid);
            if (endpoint != null) return endpoint.getName();
        } catch (FindException e) {
            logger.warning("could not retrieve the JMS endpoint from goid " + goid);
        }
        logger.warning("The goid " + goid + " could not be used to get an endpoint name.");
        return null;
    }

    private final Logger logger = Logger.getLogger(JMSEndpointReference.class.getName());

    private static final String TYPE_QUEUE = "Queue";
    private static final String TYPE_TOPIC = "Topic";

    private Goid goid;
    private Long oldOid;
    private Goid localEndpointId;
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
    public static final String OLD_OID_EL_NAME = "OldOid";
    public static final String GOID_EL_NAME = "Goid";
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
