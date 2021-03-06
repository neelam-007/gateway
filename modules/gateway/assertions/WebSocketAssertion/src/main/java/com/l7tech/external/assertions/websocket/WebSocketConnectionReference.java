package com.l7tech.external.assertions.websocket;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: cirving
 * Date: 8/28/12
 * Time: 3:09 PM
 */
public class WebSocketConnectionReference extends ExternalReference {
    private static final Logger logger = Logger.getLogger(WebSocketConnectionReference.class.getName());
    private LocalizeAction localizeType;

    private static final String ELMT_NAME_REF = "WebSocketConnectionMappingReference";
    private static final String ELMT_GOID = "GOID";
    private static final String ELMT_NAME = "Name";
    private static final String ELMT_DESC = "Description";
    private static final String ELMT_NAME_OUTBOUND_ONLY = "OutboundOnly";
    private static final String ELMT_NAME_CLASSNAME = "Classname";
    private static final String ELMT_NAME_ENABLED = "Enabled";
    private static final String ELMT_NAME_XML_VALUE = "XML_Value";
    private static final String ELMT_NAME_INBOUND_MAX_IDLE_TIME = "InboundMaxIdleTime";
    private static final String ELMT_NAME_INBOUND_LISTEN_PORT = "InboundListenPort";
    private static final String ELMT_NAME_INBOUND_MAX_CONNECTIONS = "InboundMaxConnections";
    private static final String ELMT_NAME_INBOUND_POLICY_GOID = "InboundPolicyGOID";
    private static final String ELMT_NAME_CONNECTION_POLICY_GOID = "ConnectionPolicyGOID";
    private static final String ELMT_NAME_INBOUND_SSL = "InboundSSL";
    private static final String ELMT_NAME_INBOUND_PRIVATE_KEY_ID = "InboundPrivateKeyId";
    private static final String ELMT_NAME_INBOUND_PRIVATE_KEY_ALIAS = "InboundPrivateKeyAlias";
    private static final String ELMT_NAME_INBOUND_CLIENT_AUTH = "InboundClientAuth";
    private static final String ELMT_NAME_INBOUND_TLS_CIPHER_SUITES = "InboundTlsCipherSuites";
    private static final String ELMT_NAME_INBOUND_TLS_CIPHER_SUITE = "InboundTlsCipherSuite";
    private static final String ELMT_NAME_INBOUND_TLS_PROTOCOL_SUITES = "InboundTlsProtocolSuites";
    private static final String ELMT_NAME_INBOUND_TLS_PROTOCOL_SUITE = "InboundTlsProtocolSuite";


    private static final String ELMT_NAME_OUTBOUND_URL = "OutboundURL";
    private static final String ELMT_NAME_OUTBOUND_MAX_IDLE_TIME = "OutboundMaxIdleTime";
    private static final String ELMT_NAME_OUTBOUND_POLICY_GOID = "OutboundPolicyGOID";
    private static final String ELMT_NAME_OUTBOUND_CONNECTION_POLICY_ID = "OutboundConnectionPolicyId";
    private static final String ELMT_NAME_OUTBOUND_SSL = "OutboundSSL";
    private static final String ELMT_NAME_OUTBOUND_PRIVATE_KEY_ID = "OutboundPrivateKeyId";
    private static final String ELMT_NAME_OUTBOUND_PRIVATE_KEY_ALIAS = "OutboundPrivateKeyAlias";
    private static final String ELMT_NAME_OUTBOUND_CLIENT_AUTH = "OutboundClientAuth";

    private static final String ELMT_NAME_LOOPBACK = "Loopback";
    private static final String ELMT_NAME_OUTBOUND_TLS_CIPHER_SUITES = "OutboundTlsCipherSuites";
    private static final String ELMT_NAME_OUTBOUND_TLS_CIPHER_SUITE = "OutboundTlsCipherSuite";
    private static final String ELMT_NAME_OUTBOUND_TLS_PROTOCOL_SUITES = "OutboundTlsProtocolSuites";
    private static final String ELMT_NAME_OUTBOUND_TLS_PROTOCOL_SUITE = "OutboundTlsProtocolSuite";

    private Goid goid;
    private String name;
    private String description;
    private boolean outboundOnly;
    private String classname;
    private boolean enabled;
    private String xmlValue;
    private int inboundMaxIdleTime;
    private int inboundMaxConnections;
    private Goid inboundPolicyGOID;
    private Goid connectionPolicyGOID;
    private int inboundListenPort;
    private boolean inboundSsl;
    private Goid inboundPrivateKeyId;
    private String inboundPrivateKeyAlias;
    private WebSocketConnectionEntity.ClientAuthType inboundClientAuth;
    private String outboundUrl;
    private int outboundMaxIdleTime;
    private Goid outboundPolicyGOID;
    private Goid outboundConnectionPolicyId;
    private boolean outboundSsl;
    private Goid outboundPrivateKeyId;
    private String outboundPrivateKeyAlias;
    private boolean outboundClientAuthentication;
    private boolean loopback = true;
    private ExternalReferenceFinder finder;
    private String[] inboundTlsCipherSuites;
    private String[] inboundTlsProtocols;
    private String[] outboundTlsCipherSuites;
    private String[] outboundTlsProtocols;


    public WebSocketConnectionReference(ExternalReferenceFinder finder) {
        super(finder);
        this.finder = finder;
    }

    public WebSocketConnectionReference(ExternalReferenceFinder finder, WebSocketMessageInjectionAssertion assertion) {
        super(finder);
        this.finder = finder;
        try {
            WebSocketConnectionEntity entity = getEntityManager(finder).findByPrimaryKey(assertion.getServiceOid());

            //General
            goid = entity.getGoid();
            description = entity.getDescription();
            outboundOnly = entity.isOutboundOnly();
            classname = entity.getEntityClassName();
            name = entity.getName();
            xmlValue = entity.getValueXml();
            enabled = entity.isEnabled();

            //Inbound
            inboundListenPort = entity.getInboundListenPort();
            inboundClientAuth = entity.getInboundClientAuth();
            inboundMaxConnections = entity.getInboundMaxConnections();
            inboundMaxIdleTime = entity.getInboundMaxIdleTime();
            inboundPolicyGOID = entity.getInboundPolicyOID();
            connectionPolicyGOID = entity.getConnectionPolicyGOID();
            inboundPrivateKeyAlias = entity.getInboundPrivateKeyAlias();
            inboundPrivateKeyId = entity.getInboundPrivateKeyId();
            inboundSsl = entity.isInboundSsl();
            inboundTlsCipherSuites = entity.getInboundCipherSuites();
            inboundTlsProtocols = entity.getInboundTlsProtocols();

            outboundUrl = entity.getOutboundUrl();
            outboundMaxIdleTime = entity.getOutboundMaxIdleTime();
            outboundPolicyGOID = entity.getOutboundPolicyOID();
            outboundConnectionPolicyId = entity.getOutboundConnectionPolicyId();
            outboundPrivateKeyAlias = entity.getOutboundPrivateKeyAlias();
            outboundPrivateKeyId = entity.getOutboundPrivateKeyId();
            outboundSsl = entity.isOutboundSsl();
            outboundClientAuthentication = entity.isOutboundClientAuthentication();
            loopback = entity.isLoopback();
            outboundTlsCipherSuites = entity.getOutboundCipherSuites();
            outboundTlsProtocols = entity.getOutboundTlsProtocols();


        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve Entity from EntityManager");
        }
    }

    private static WebSocketConnectionEntityAdmin getEntityManager(ExternalReferenceFinder finder) throws FindException {
        if (finder.getClass().getName().contains("Console")) {
            return getEntityManager();
        } else {
            return new WebSocketConnectionEntityAdminImpl(finder.getGenericEntityManager(WebSocketConnectionEntity.class));
        }
    }

    private static WebSocketConnectionEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(WebSocketConnectionEntityAdmin.class, null);
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


        if (goid != null){
            addParameterElement(ELMT_GOID, goid.toHexString(), referenceElement);
        }

        addParameterElement(ELMT_NAME, name, referenceElement);
        addParameterElement(ELMT_DESC, description, referenceElement);
        addParameterElement(ELMT_NAME_OUTBOUND_ONLY, String.valueOf(outboundOnly), referenceElement);
        addParameterElement(ELMT_NAME_CLASSNAME, classname, referenceElement);
        addParameterElement(ELMT_NAME_ENABLED, String.valueOf(enabled), referenceElement);
        addParameterElement(ELMT_NAME_XML_VALUE, xmlValue, referenceElement);
        addParameterElement(ELMT_NAME_INBOUND_MAX_IDLE_TIME, String.valueOf(inboundMaxIdleTime), referenceElement);
        addParameterElement(ELMT_NAME_INBOUND_LISTEN_PORT, String.valueOf(inboundListenPort), referenceElement);

        addParameterElement(ELMT_NAME_INBOUND_MAX_CONNECTIONS, String.valueOf(inboundMaxConnections), referenceElement);

        if (inboundPolicyGOID != null) {
            addParameterElement(ELMT_NAME_INBOUND_POLICY_GOID, inboundPolicyGOID.toHexString(), referenceElement);
        }

        if (connectionPolicyGOID != null) {
            addParameterElement(ELMT_NAME_CONNECTION_POLICY_GOID, connectionPolicyGOID.toHexString(), referenceElement);
        }

        addParameterElement(ELMT_NAME_INBOUND_SSL, String.valueOf(inboundSsl), referenceElement);
        addParameterElement(ELMT_NAME_INBOUND_PRIVATE_KEY_ID, inboundPrivateKeyId.toHexString(), referenceElement);
        addParameterElement(ELMT_NAME_INBOUND_PRIVATE_KEY_ALIAS, inboundPrivateKeyAlias, referenceElement);
        if (inboundClientAuth != null) {
            addParameterElement(ELMT_NAME_INBOUND_CLIENT_AUTH, inboundClientAuth.toString(), referenceElement);
        } else {
            addParameterElement(ELMT_NAME_INBOUND_CLIENT_AUTH, "", referenceElement);
        }

        if (inboundTlsProtocols != null) {
            Element propertiesElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_INBOUND_TLS_PROTOCOL_SUITES);
            for (int i = 0; i< inboundTlsProtocols.length; i++ ){
                Element propertyElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_INBOUND_TLS_PROTOCOL_SUITE);
                propertyElement.setAttribute(ELMT_NAME, inboundTlsProtocols[i]);
                propertiesElement.appendChild(propertyElement);
            }
            referenceElement.appendChild(propertiesElement);
        }

        if (inboundTlsCipherSuites != null) {
            Element propertiesElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_INBOUND_TLS_CIPHER_SUITES);
            for (int i = 0; i< inboundTlsCipherSuites.length; i++ ){
                Element propertyElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_INBOUND_TLS_CIPHER_SUITE);
                propertyElement.setAttribute(ELMT_NAME, inboundTlsCipherSuites[i]);
                propertiesElement.appendChild(propertyElement);
            }
            referenceElement.appendChild(propertiesElement);
        }

        addParameterElement(ELMT_NAME_OUTBOUND_URL, outboundUrl, referenceElement);
        addParameterElement(ELMT_NAME_OUTBOUND_MAX_IDLE_TIME, String.valueOf(outboundMaxIdleTime), referenceElement);

        if (outboundPolicyGOID != null) {
            addParameterElement(ELMT_NAME_OUTBOUND_POLICY_GOID, outboundPolicyGOID.toHexString(), referenceElement);
        }

        if (outboundConnectionPolicyId != null) {
            addParameterElement(ELMT_NAME_OUTBOUND_CONNECTION_POLICY_ID, outboundConnectionPolicyId.toHexString(), referenceElement);
        }

        addParameterElement(ELMT_NAME_OUTBOUND_SSL, String.valueOf(outboundSsl), referenceElement);
        if (outboundPrivateKeyId == null){
            addParameterElement(ELMT_NAME_OUTBOUND_PRIVATE_KEY_ID, Goid.DEFAULT_GOID.toHexString(), referenceElement);
        } else {
            addParameterElement(ELMT_NAME_OUTBOUND_PRIVATE_KEY_ID, outboundPrivateKeyId.toHexString(), referenceElement);
        }

        addParameterElement(ELMT_NAME_OUTBOUND_PRIVATE_KEY_ALIAS, outboundPrivateKeyAlias, referenceElement);
        addParameterElement(ELMT_NAME_OUTBOUND_CLIENT_AUTH, String.valueOf(outboundClientAuthentication), referenceElement);
        addParameterElement(ELMT_NAME_LOOPBACK, String.valueOf(loopback), referenceElement);

        if (outboundTlsProtocols != null) {
            Element propertiesElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_OUTBOUND_TLS_PROTOCOL_SUITES);
            for (int i = 0; i< outboundTlsProtocols.length; i++ ){
                Element propertyElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_OUTBOUND_TLS_PROTOCOL_SUITE);
                propertyElement.setAttribute(ELMT_NAME, outboundTlsProtocols[i]);
                propertiesElement.appendChild(propertyElement);
            }
            referenceElement.appendChild(propertiesElement);
        }

        if (outboundTlsCipherSuites != null) {
            Element propertiesElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_OUTBOUND_TLS_CIPHER_SUITES);
            for (int i = 0; i< outboundTlsCipherSuites.length; i++ ){
                Element propertyElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_OUTBOUND_TLS_CIPHER_SUITE);
                propertyElement.setAttribute(ELMT_NAME, outboundTlsCipherSuites[i]);
                propertiesElement.appendChild(propertyElement);
            }
            referenceElement.appendChild(propertiesElement);
        }

    }

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     */
    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        //Can't think of a case where there needs to be admin interaction
        return true;
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
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof WebSocketMessageInjectionAssertion) {
                try {
                    WebSocketConnectionEntity entity = new WebSocketConnectionEntity();


                    entity.setGoid(goid);
                    entity.setName(name);
                    entity.setDescription(description);
                    entity.setOutboundOnly(outboundOnly);
                    entity.setEnabled(enabled);
                    entity.setEntityClassName(classname);
                    entity.setValueXml(xmlValue);
                    entity.setInboundClientAuth(inboundClientAuth);
                    entity.setInboundListenPort(inboundListenPort);
                    entity.setInboundMaxConnections(inboundMaxConnections);
                    entity.setInboundMaxIdleTime(inboundMaxIdleTime);
                    entity.setInboundPolicyOID(inboundPolicyGOID);
                    entity.setConnectionPolicyGOID(connectionPolicyGOID);
                    entity.setInboundSsl(inboundSsl);
                    entity.setInboundPrivateKeyAlias(inboundPrivateKeyAlias);
                    entity.setInboundPrivateKeyId(inboundPrivateKeyId);
                    entity.setInboundTlsProtocols(inboundTlsProtocols);
                    entity.setInboundCipherSuites(inboundTlsCipherSuites);

                    entity.setOutboundUrl(outboundUrl);
                    entity.setOutboundMaxIdleTime(outboundMaxIdleTime);
                    entity.setOutboundPolicyOID(outboundPolicyGOID);
                    entity.setOutboundConnectionPolicyId(outboundConnectionPolicyId);
                    entity.setOutboundSsl(outboundSsl);
                    entity.setOutboundPrivateKeyId(outboundPrivateKeyId);
                    entity.setOutboundPrivateKeyAlias(outboundPrivateKeyAlias);
                    entity.setOutboundClientAuthentication(outboundClientAuthentication);
                    entity.setOutboundTlsProtocols(outboundTlsProtocols);
                    entity.setOutboundCipherSuites(outboundTlsCipherSuites);

                    if (getEntityManager(finder).findByPrimaryKey(goid) == null) {
                        getEntityManager(finder).save(entity);
                        return true;
                    }

                } catch (InvalidRangeException e) {
                    logger.log(Level.WARNING, "Error in setting entity data");
                    return false;


                } catch (FindException e) {
                    logger.log(Level.WARNING, "Error in retrieving WebSocket Connection Entity from Manager");
                    return false;
                } catch (SaveException e) {
                    logger.log(Level.WARNING, "Error in save WebSocket Connection Entity to Manager");
                    return false;
                } catch (UpdateException e) {
                    logger.log(Level.WARNING, "Error in updating WebSocket Connection Entity to Manager");
                    return false;
                }
            }
        }

        return true;
    }

    private void addParameterElement(final String name, final String value, final Element parent) {
        if (value != null) {
            final Element parameterElement = parent.getOwnerDocument().createElement(name);
            final Text txt = DomUtils.createTextNode(parent, value);
            parameterElement.appendChild(txt);
            parent.appendChild(parameterElement);
        }
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final WebSocketConnectionReference that = (WebSocketConnectionReference) o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;

        return true;
    }


    public static Object parseFromElement(Object context, Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct connectorName
        if (!el.getNodeName().equals(ELMT_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of connectorName " + ELMT_NAME_REF);
        }

        WebSocketConnectionReference output = new WebSocketConnectionReference((ExternalReferenceFinder) context);

        String goid = getParamFromEl(el, ELMT_GOID);
        if (goid != null) {
            output.goid = Goid.parseGoid(goid);
        }
        output.name = getParamFromEl(el, ELMT_NAME);
        output.classname = getParamFromEl(el, ELMT_NAME_CLASSNAME);
        output.description = getParamFromEl(el, ELMT_DESC);
        output.outboundOnly = Boolean.parseBoolean(getParamFromEl(el, ELMT_NAME_OUTBOUND_ONLY));
        output.enabled = Boolean.parseBoolean(getParamFromEl(el, ELMT_NAME_ENABLED));
        output.xmlValue = getParamFromEl(el, ELMT_NAME_XML_VALUE);
        output.inboundClientAuth = parseClientAuthType(getParamFromEl(el, ELMT_NAME_INBOUND_CLIENT_AUTH));
        output.inboundListenPort = Integer.parseInt(getParamFromEl(el, ELMT_NAME_INBOUND_LISTEN_PORT));
        output.inboundMaxConnections = Integer.parseInt(getParamFromEl(el, ELMT_NAME_INBOUND_MAX_CONNECTIONS));
        output.inboundMaxIdleTime = Integer.parseInt(getParamFromEl(el, ELMT_NAME_INBOUND_MAX_IDLE_TIME));

        String inboundPolicyGOID = getParamFromEl(el, ELMT_NAME_INBOUND_POLICY_GOID);
        if (inboundPolicyGOID != null) {
            output.inboundPolicyGOID = Goid.parseGoid(inboundPolicyGOID);
        }

        String connectionPolicyGOID = getParamFromEl(el, ELMT_NAME_CONNECTION_POLICY_GOID);
        if (connectionPolicyGOID != null) {
            output.connectionPolicyGOID = Goid.parseGoid(connectionPolicyGOID);
        }

        output.inboundSsl = Boolean.parseBoolean(getParamFromEl(el, ELMT_NAME_INBOUND_SSL));
        output.inboundPrivateKeyId = Goid.parseGoid(getParamFromEl(el, ELMT_NAME_INBOUND_PRIVATE_KEY_ID));
        output.inboundPrivateKeyAlias = getParamFromEl(el, ELMT_NAME_INBOUND_PRIVATE_KEY_ALIAS);

        NodeList propertiesEls = el.getElementsByTagName(ELMT_NAME_INBOUND_TLS_PROTOCOL_SUITES);
        if (propertiesEls.getLength() > 0) {
            Element propertiesEl = (Element) propertiesEls.item(0);

            NodeList propertyEls = propertiesEl.getElementsByTagName(ELMT_NAME_INBOUND_TLS_PROTOCOL_SUITE);

            List<String> inboundTlsProtocolList = new ArrayList<>();
            for (int i = 0; i < propertyEls.getLength(); i++) {
                Element propertyEl = (Element) propertyEls.item(i);
                inboundTlsProtocolList.add(propertyEl.getAttribute(ELMT_NAME));
            }
            output.inboundTlsProtocols = inboundTlsProtocolList.toArray(new String[0]);
        }

        propertiesEls = el.getElementsByTagName(ELMT_NAME_INBOUND_TLS_CIPHER_SUITES);
        if (propertiesEls.getLength() > 0) {
            Element propertiesEl = (Element) propertiesEls.item(0);

            NodeList propertyEls = propertiesEl.getElementsByTagName(ELMT_NAME_INBOUND_TLS_CIPHER_SUITE);

            List<String> inboundTlsCipherSuitesList = new ArrayList<>();
            for (int i = 0; i < propertyEls.getLength(); i++) {
                Element propertyEl = (Element) propertyEls.item(i);
                inboundTlsCipherSuitesList.add(propertyEl.getAttribute(ELMT_NAME));
            }
            output.inboundTlsCipherSuites = inboundTlsCipherSuitesList.toArray(new String[0]);
        }

        output.outboundUrl = getParamFromEl(el, ELMT_NAME_OUTBOUND_URL);
        output.outboundMaxIdleTime = Integer.parseInt(getParamFromEl(el, ELMT_NAME_OUTBOUND_MAX_IDLE_TIME));

        String outboundPolicyGOID = getParamFromEl(el, ELMT_NAME_OUTBOUND_POLICY_GOID);
        if (outboundPolicyGOID != null) {
            output.outboundPolicyGOID = Goid.parseGoid(outboundPolicyGOID);
        }

        String outboundConnectionPolicyId = getParamFromEl(el, ELMT_NAME_OUTBOUND_CONNECTION_POLICY_ID);
        if (outboundConnectionPolicyId != null) {
            output.outboundConnectionPolicyId = Goid.parseGoid(outboundConnectionPolicyId);
        }

        output.outboundSsl = Boolean.parseBoolean(getParamFromEl(el, ELMT_NAME_OUTBOUND_SSL));
        output.outboundPrivateKeyId = Goid.parseGoid(getParamFromEl(el, ELMT_NAME_OUTBOUND_PRIVATE_KEY_ID));
        output.outboundPrivateKeyAlias = getParamFromEl(el, ELMT_NAME_OUTBOUND_PRIVATE_KEY_ALIAS);
        output.outboundClientAuthentication = Boolean.parseBoolean(getParamFromEl(el, ELMT_NAME_OUTBOUND_CLIENT_AUTH));
        output.loopback = Boolean.parseBoolean(getParamFromEl(el, ELMT_NAME_LOOPBACK));

        propertiesEls = el.getElementsByTagName(ELMT_NAME_OUTBOUND_TLS_PROTOCOL_SUITES);
        if (propertiesEls.getLength() > 0) {
            Element propertiesEl = (Element) propertiesEls.item(0);

            NodeList propertyEls = propertiesEl.getElementsByTagName(ELMT_NAME_OUTBOUND_TLS_PROTOCOL_SUITE);

            List<String> outboundTlsProtocolList = new ArrayList<>();
            for (int i = 0; i < propertyEls.getLength(); i++) {
                Element propertyEl = (Element) propertyEls.item(i);
                outboundTlsProtocolList.add(propertyEl.getAttribute(ELMT_NAME));
            }
            output.inboundTlsProtocols = outboundTlsProtocolList.toArray(new String[0]);
        }

        propertiesEls = el.getElementsByTagName(ELMT_NAME_OUTBOUND_TLS_CIPHER_SUITES);
        if (propertiesEls.getLength() > 0) {
            Element propertiesEl = (Element) propertiesEls.item(0);

            NodeList propertyEls = propertiesEl.getElementsByTagName(ELMT_NAME_OUTBOUND_TLS_CIPHER_SUITE);

            List<String> outboundTlsCipherSuitesList = new ArrayList<>();
            for (int i = 0; i < propertyEls.getLength(); i++) {
                Element propertyEl = (Element) propertyEls.item(i);
                outboundTlsCipherSuitesList.add(propertyEl.getAttribute(ELMT_NAME));
            }
            output.outboundTlsCipherSuites = outboundTlsCipherSuitesList.toArray(new String[0]);
        }

        return output;
    }

    private static WebSocketConnectionEntity.ClientAuthType parseClientAuthType(String s) {
        try {
            return WebSocketConnectionEntity.ClientAuthType.valueOf(s);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid Client Auth Type Value");
            return null;
        } catch (NullPointerException e) {
            logger.log(Level.INFO, "No Client Auth Type Value exists");
            return null;
        }
    }

}
