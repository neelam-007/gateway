package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: njordan
 * Date: 20/03/12
 * Time: 10:54 AM
 */
public class XMPPConnectionReference extends ExternalReference {
    private static final Logger logger = Logger.getLogger(XMPPConnectionReference.class.getName());

    private static final String ELMT_NAME_REF = "XMPPConnectionReference";
    private static final String ELMT_OID = "OID";
    private static final String ELMT_GOID = "GOID";
    private static final String ELMT_NAME = "Name";
    private static final String ELMT_NAME_CLASSNAME = "Classname";
    private static final String ELMT_INBOUND = "Inbound";
    private static final String ELMT_THREAD_POOL_SIZE = "ThreadPoolSize";
    private static final String ELMT_BIND_ADDRESS = "BindAddress";
    private static final String ELMT_ENABLED = "Enabled";
    private static final String ELMT_HOSTNAME = "Hostname";
    private static final String ELMT_PORT = "Port";
    private static final String ELMT_MSG_RCVD_SRVC_GOID = "MessageReceivedServiceGoid";
    private static final String ELMT_SESSION_TERMINATED_SRVC_GOID = "SessionTerminatedServiceGoid";
    private static final String ELMT_CONTENT_TYPE = "ContentType";

    private Goid goid;
    private String name;
    private String classname;
    private boolean inbound;
    private int threadPoolSize;
    private String bindAddress;
    private boolean enabled;
    private String hostname;
    private int port;
    private Goid messageReceivedServiceGoid;
    private Goid sessionTerminatedServiceGoid;
    private String contentType;

    private Goid localGoid;
    private LocalizeAction localizeType;

    public XMPPConnectionReference(ExternalReferenceFinder finder) {
        super(finder);
    }

    public XMPPConnectionReference(ExternalReferenceFinder finder, XMPPOpenServerSessionAssertion assertion) {
        super(finder);
        try {
            XMPPConnectionEntity entity = getEntityManager(finder).find(assertion.getXMPPConnectionId());

            if(entity != null) {
                goid = entity.getGoid();
                name = entity.getName();
                classname = entity.getEntityClassName();
                inbound = entity.isInbound();
                threadPoolSize = entity.getThreadpoolSize();
                bindAddress = entity.getBindAddress();
                enabled = entity.isEnabled();
                hostname = entity.getHostname();
                port = entity.getPort();
                messageReceivedServiceGoid = entity.getMessageReceivedServiceOid();
                sessionTerminatedServiceGoid = entity.getSessionTerminatedServiceOid();
                contentType = entity.getContentType();
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve Entity from EntityManager");
        }
    }

    private static XMPPConnectionEntityAdmin getEntityManager(ExternalReferenceFinder finder) throws FindException {
        if ( finder.getClass().getName().contains("Console") ) {
            return getEntityManager();
        } else {
            return new XMPPConnectionEntityAdminImpl(finder.getGenericEntityManager(XMPPConnectionEntity.class));
        }
    }

    private static XMPPConnectionEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(XMPPConnectionEntityAdmin.class, null);
    }

    public Goid getGoid() {
        return goid;
    }
    
    public String getName() {
        return name;
    }
    
    public String getClassname() {
        return classname;
    }
    
    public boolean isInbound() {
        return inbound;
    }
    
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    public String getBindAddress() {
        return bindAddress;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public int getPort() {
        return port;
    }
    
    public Goid getMessageReceivedServiceGoid() {
        return messageReceivedServiceGoid;
    }
    
    public Goid getSessionTerminatedServiceGoid() {
        return sessionTerminatedServiceGoid;
    }
    
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean setLocalizeReplace(final Goid connectionGoid) {
        localizeType = LocalizeAction.REPLACE;
        localGoid = connectionGoid;
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

    /**
     * Adds a child element to the passed references element that contains the xml
     * form of this reference object. Used by the policy exporter when serializing
     * references to xml format.
     *
     * @param referencesParentElement Element containing assertion
     */
    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element referenceElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_REF);
        setTypeAttribute(referenceElement);
        referencesParentElement.appendChild(referenceElement);

        addParameterElement(ELMT_GOID, goid.toString(), referenceElement);
        addParameterElement(ELMT_NAME, name, referenceElement);
        addParameterElement(ELMT_NAME_CLASSNAME, classname, referenceElement);
        addParameterElement(ELMT_INBOUND, Boolean.toString(inbound), referenceElement);
        addParameterElement(ELMT_THREAD_POOL_SIZE, Integer.toString(threadPoolSize), referenceElement);
        addParameterElement(ELMT_BIND_ADDRESS, bindAddress, referenceElement);
        addParameterElement(ELMT_ENABLED, Boolean.toString(enabled), referenceElement);
        addParameterElement(ELMT_HOSTNAME, hostname, referenceElement);
        addParameterElement(ELMT_PORT, Integer.toString(port), referenceElement);
        if(messageReceivedServiceGoid != null) {
            addParameterElement(ELMT_MSG_RCVD_SRVC_GOID, messageReceivedServiceGoid.toHexString(), referenceElement);
        }
        if(sessionTerminatedServiceGoid != null) {
            addParameterElement(ELMT_SESSION_TERMINATED_SRVC_GOID, sessionTerminatedServiceGoid.toHexString(), referenceElement);
        }
        addParameterElement(ELMT_CONTENT_TYPE, contentType, referenceElement);
    }

    private void addParameterElement(final String name, final String value, final Element parent) {
        if (value != null) {
            final Element parameterElement = parent.getOwnerDocument().createElement(name);
            final Text txt = DomUtils.createTextNode(parent, value);
            parameterElement.appendChild(txt);
            parent.appendChild(parameterElement);
        }
    }

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     */
    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        try {
            final XMPPConnectionEntity activeConnection = getEntityManager(this.getFinder()).find(goid);
            if (activeConnection != null) {
                if (isMatch(activeConnection.getName(), name) && permitMapping(goid, activeConnection.getGoid())) {
                    // Perfect Match (OID and name are matched.)
                    logger.fine("The XMPP connection was resolved by oid '" + goid.toString() + "' and name '" + activeConnection.getName() + "'");
                    return true;
                }
            } else {
                final Collection<XMPPConnectionEntity> outboundConnections = findAllOutboundXMPPConnections();
                for (XMPPConnectionEntity connection: outboundConnections) {
                    if (isMatch(connection.getName(), name) && permitMapping(goid, connection.getGoid())) {
                        // Connector Name matched
                        logger.fine("The XMPP connection was resolved from GOID '" + goid.toString() + "' to '" + connection.getGoid().toString() + "'");
                        localGoid = connection.getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }

                // Check if partial matched
                for (XMPPConnectionEntity connection: outboundConnections) {
                    if (isMatch(connection.getHostname(), hostname) && connection.getPort() == port &&
                            permitMapping(goid, connection.getGoid())) {
                        // Partial matched
                        logger.fine("The XMPP connection was resolved from GOID '" + goid.toString() + "' to '" + connection.getGoid().toString() + "'");
                        localGoid = connection.getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }
            }
        } catch (FindException e) {
            logger.warning("Cannot load XMPP connection from GOID, " + goid.toString());
        }

        return false;
    }
    private Collection<XMPPConnectionEntity> findAllOutboundXMPPConnections() {
        ArrayList<XMPPConnectionEntity> outboundConnections = new ArrayList<XMPPConnectionEntity>();
        
        try {
            for(XMPPConnectionEntity connection : getEntityManager(getFinder()).findAll()) {
                if(!connection.isInbound()) {
                    outboundConnections.add(connection);
                }
            }
        } catch(FindException e) {
            //
        }
        
        return outboundConnections;
    }

    /**
     * Once an exported policy is loaded with it's references and the references are
     * verified, this method will apply the necessary changes to the assertion. If
     * the assertion type passed does not relate to the reference, it will be left
     * untouched.
     * Returns false if the assertion should be deleted from the tree.
     *
     * @param assertionToLocalize will be fixed once this method returns.
     */
    @Override
    protected boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE){
            if (assertionToLocalize instanceof XMPPOpenServerSessionAssertion) {
                final XMPPOpenServerSessionAssertion xmppOpenServerSessionAssertion = (XMPPOpenServerSessionAssertion) assertionToLocalize;
                final Goid connectorGoid = xmppOpenServerSessionAssertion.getXMPPConnectionId();
                if (connectorGoid == null && goid == null || connectorGoid != null && connectorGoid.equals(goid)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if (localizeType == LocalizeAction.REPLACE) {
                        xmppOpenServerSessionAssertion.setXMPPConnectionId(localGoid);
                    }  else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final XMPPConnectionReference that = (XMPPConnectionReference) o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public static Object parseFromElement(Object context, Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct connectorName
        if (!el.getNodeName().equals(ELMT_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of connectorName " + ELMT_NAME_REF);
        }

        XMPPConnectionReference output = new XMPPConnectionReference((ExternalReferenceFinder) context);

        String value = getParamFromEl(el, ELMT_GOID);
        if (value != null) {
            output.goid = Goid.parseGoid(value);
        }

        output.name = getParamFromEl(el, ELMT_NAME);
        output.classname = getParamFromEl(el, ELMT_NAME_CLASSNAME);

        value = getParamFromEl(el, ELMT_INBOUND);
        if (value != null) {
            output.inbound = Boolean.parseBoolean(value);
        }

        value = getParamFromEl(el, ELMT_THREAD_POOL_SIZE);
        if (value != null) {
            output.threadPoolSize = Integer.parseInt(value);
        }

        output.bindAddress = getParamFromEl(el, ELMT_BIND_ADDRESS);

        value = getParamFromEl(el, ELMT_ENABLED);
        if (value != null) {
            output.enabled = Boolean.parseBoolean(value);
        }

        output.hostname = getParamFromEl(el, ELMT_HOSTNAME);

        value = getParamFromEl(el, ELMT_PORT);
        if (value != null) {
            output.port = Integer.parseInt(value);
        }

        value = getParamFromEl(el, ELMT_MSG_RCVD_SRVC_GOID);
        if (value != null) {
            output.messageReceivedServiceGoid = Goid.parseGoid(value);
        } else {
            output.messageReceivedServiceGoid = null;
        }

        value = getParamFromEl(el, ELMT_SESSION_TERMINATED_SRVC_GOID);
        if (value != null) {
            output.sessionTerminatedServiceGoid = Goid.parseGoid(value);
        } else {
            output.sessionTerminatedServiceGoid = null;
        }

        output.contentType = getParamFromEl(el, ELMT_CONTENT_TYPE);

        return output;
    }

    private boolean isMissing( final String value ) {
        return value == null || value.isEmpty();
    }

    private boolean isMatch( final String leftValue,
                             final String rightValue) {
        return isMissing(leftValue) ? isMissing(rightValue) : leftValue.equals(rightValue);
    }
}
