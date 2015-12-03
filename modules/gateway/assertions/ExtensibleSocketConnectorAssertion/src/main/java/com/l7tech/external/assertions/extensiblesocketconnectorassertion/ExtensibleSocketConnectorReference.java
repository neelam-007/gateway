package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.CodecConfiguration;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 27/03/12
 * Time: 3:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorReference extends ExternalReference {
    private static final Logger logger = Logger.getLogger(ExtensibleSocketConnectorReference.class.getName());

    private static final String ELMT_NAME_REF = "ExtensibleSocketConnectorReference";
    private static final String ELMT_OID = "OID";
    private static final String ELMT_GOID = "GOID";
    private static final String ELMT_NAME = "Name";
    private static final String ELMT_NAME_CLASSNAME = "Classname";
    private static final String ELMT_IN = "In";
    private static final String ELMT_HOSTNAME = "Hostname";
    private static final String ELMT_PORT = "Port";
    private static final String ELMT_USE_SSL = "UseSSL";
    private static final String ELMT_SSL_KEY_ID = "SslKeyID";
    private static final String ELMT_CLIENT_AUTH = "ClientAuth";
    private static final String ELMT_THREAD_POOL_MIN = "ThreadPoolMin";
    private static final String ELMT_THREAD_POOL_MAX = "ThreadPoolMax";
    private static final String ELMT_BIND_ADDRESS = "BindAddress";
    private static final String ELMT_SERVICE_OID = "ServiceOID";
    private static final String ELMT_SERVICE_GOID = "ServiceGOID";
    private static final String ELMT_CONTENT_TYPE = "ContentType";
    private static final String ELMT_MAX_MSG_SIZE = "MaxMessageSize";
    private static final String ELMT_ENABLED = "Enabled";
    private static final String ELMT_KEEP_ALIVE="KeepAlive";
    private static final String ELMT_DNS_SERVICE="dnsService";
    private static final String ELMT_DNS_DOMAIN="dnsDomainName";
    private static final String ELMT_DNS_LOOKUP="useDnsLookup";
    private static final String ELMT_LISTEN_TIMEOUT = "ListenTimeOut";
    private static final String ELMT_CODEC_CONFIG = "CodecConfiguration";
    private static final String ELMT_EXCHANGE_PATTERN = "ExchangePattern";

    private Goid goid;
    private String name;
    private String classname;
    private boolean in;
    private String hostname;
    private int port;
    private boolean useSsl;
    private String sslKeyId;
    private SSLClientAuthEnum clientAuthEnum;
    private int threadPoolMin;
    private int threadPoolMax;
    private String bindAddress;
    private Goid serviceGoid;
    private String contentType;
    private int maxMessageSize;
    private boolean enabled;
    private CodecConfiguration codecConfiguration;
    private ExchangePatternEnum exchangePattern;

    private boolean keepAlive;
    private long listenTimeOut;
    private boolean useDnsLookup;
    private String dnsDomainName;
    private String dnsService;

    private Goid localGoid;
    private LocalizeAction localizeType;

    public ExtensibleSocketConnectorReference(ExternalReferenceFinder finder) {
        super(finder);
    }

    public ExtensibleSocketConnectorReference(ExternalReferenceFinder finder, ExtensibleSocketConnectorAssertion assertion) {
        super(finder);
        try {
            ExtensibleSocketConnectorEntity entity = getEntityManager(finder).find(assertion.getSocketConnectorGoid());

            if (entity != null) {
                goid = entity.getGoid();
                name = entity.getName();
                classname = entity.getEntityClassName();
                in = entity.isIn();
                hostname = entity.getHostname();
                port = entity.getPort();
                useSsl = entity.isUseSsl();
                sslKeyId = entity.getSslKeyId();
                clientAuthEnum = entity.getClientAuthEnum();
                threadPoolMin = entity.getThreadPoolMin();
                threadPoolMax = entity.getThreadPoolMax();
                bindAddress = entity.getBindAddress();
                serviceGoid = entity.getServiceGoid();
                contentType = entity.getContentType();
                maxMessageSize = entity.getMaxMessageSize();
                enabled = entity.isEnabled();
                keepAlive = entity.isKeepAlive();
                listenTimeOut = entity.getListenTimeout();
                codecConfiguration = entity.getCodecConfiguration();
                exchangePattern = entity.getExchangePattern();
                dnsDomainName = entity.getDnsDomainName();
                dnsService = entity.getDnsService();
                useDnsLookup = entity.isUseDnsLookup();
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve Entity from EntityManager");
        }
    }

    private static ExtensibleSocketConnectorEntityAdmin getEntityManager(ExternalReferenceFinder finder) throws FindException {
        if (finder.getClass().getName().contains("Console")) {
            return getEntityManager();
        } else {
            return new ExtensibleSocketConnectorEntityAdminImpl(finder.getGenericEntityManager(ExtensibleSocketConnectorEntity.class));
        }
    }

    private static ExtensibleSocketConnectorEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(ExtensibleSocketConnectorEntityAdmin.class, null);
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

    public boolean isIn() {
        return in;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public String getSslKeyId() {
        return sslKeyId;
    }

    public SSLClientAuthEnum getClientAuthEnum() {
        return clientAuthEnum;
    }

    public int getThreadPoolMin() {
        return threadPoolMin;
    }

    public int getThreadPoolMax() {
        return threadPoolMax;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public Goid getServiceGoid() {
        return serviceGoid;
    }

    public String getContentType() {
        return contentType;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public long getListenTimeOut() {
        return listenTimeOut;
    }

    public boolean isUseDnsLookup(){
        return useDnsLookup;
    }

    public String getDnsDomainName(){
        return dnsDomainName;
    }

    public String getDnsService(){
        return dnsService;
    }

    public CodecConfiguration getCodecConfiguration() {
        return codecConfiguration;
    }

    public ExchangePatternEnum getExchangePattern() {
        return exchangePattern;
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
        addParameterElement(ELMT_IN, Boolean.toString(in), referenceElement);
        if (in) {
            addParameterElement(ELMT_PORT, Integer.toString(port), referenceElement);
            addParameterElement(ELMT_USE_SSL, Boolean.toString(useSsl), referenceElement);
            if (useSsl) {
                addParameterElement(ELMT_SSL_KEY_ID, sslKeyId, referenceElement);
                addParameterElement(ELMT_CLIENT_AUTH, clientAuthEnum.toString(), referenceElement);
            }
            addParameterElement(ELMT_THREAD_POOL_MIN, Integer.toString(threadPoolMin), referenceElement);
            addParameterElement(ELMT_THREAD_POOL_MAX, Integer.toString(threadPoolMax), referenceElement);
            addParameterElement(ELMT_BIND_ADDRESS, bindAddress, referenceElement);
            addParameterElement(ELMT_SERVICE_GOID, new Goid(serviceGoid).toString(), referenceElement);
            addParameterElement(ELMT_CONTENT_TYPE, contentType, referenceElement);
            addParameterElement(ELMT_MAX_MSG_SIZE, Integer.toString(maxMessageSize), referenceElement);
            addParameterElement(ELMT_ENABLED, Boolean.toString(enabled), referenceElement);
        } else {
            addParameterElement(ELMT_HOSTNAME, hostname, referenceElement);
            addParameterElement(ELMT_PORT, Integer.toString(port), referenceElement);
            addParameterElement(ELMT_USE_SSL, Boolean.toString(useSsl), referenceElement);
            if (useSsl) {
                addParameterElement(ELMT_SSL_KEY_ID, sslKeyId, referenceElement);
            }
            addParameterElement(ELMT_CONTENT_TYPE, contentType, referenceElement);
            addParameterElement(ELMT_EXCHANGE_PATTERN, exchangePattern.toString(), referenceElement);
            addParameterElement(ELMT_KEEP_ALIVE, Boolean.toString(keepAlive), referenceElement);
            addParameterElement(ELMT_LISTEN_TIMEOUT, Long.toString(listenTimeOut), referenceElement);
            addParameterElement(ELMT_DNS_LOOKUP, Boolean.toString(useDnsLookup), referenceElement);
            if(useDnsLookup){
                addParameterElement(ELMT_DNS_SERVICE, dnsService, referenceElement);
                addParameterElement(ELMT_DNS_DOMAIN, dnsDomainName, referenceElement);
            }
        }

        Element codecConfigElement = referencesParentElement.getOwnerDocument().createElement(ELMT_CODEC_CONFIG);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(baos);
        encoder.writeObject(codecConfiguration);
        encoder.close();
        codecConfigElement.appendChild(referencesParentElement.getOwnerDocument().createTextNode(new String(baos.toByteArray())));
        referenceElement.appendChild(codecConfigElement);
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
            final ExtensibleSocketConnectorEntity activeConnector = getEntityManager(getFinder()).find(goid);
            if (activeConnector != null) {
                if (isMatch(activeConnector.getName(), name) && permitMapping(goid, activeConnector.getGoid())) {
                    // Perfect Match (OID and name are matched.)
                    logger.fine("The extensible socket connector was resolved by goid '" + goid.toString() + "' and name '" + activeConnector.getName() + "'");
                    return true;
                }
            } else {
                final Collection<ExtensibleSocketConnectorEntity> outboundConnectors = findAllOutboundExtensibleSocketConnectors();
                for (ExtensibleSocketConnectorEntity connector : outboundConnectors) {
                    if (isMatch(connector.getName(), name) && permitMapping(goid, connector.getGoid())) {
                        // Connector Name matched
                        logger.fine("The extensible socket connector was resolved from goid '" + goid.toString() + "' to '" + connector.getGoid().toString() + "'");
                        localGoid = connector.getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }

                // Check if partial matched
                for (ExtensibleSocketConnectorEntity connector : outboundConnectors) {
                    if (isMatch(connector.getHostname(), hostname) && connector.getPort() == port &&
                            permitMapping(goid, connector.getGoid())) {
                        // Partial matched
                        logger.fine("The extensible socket connector was resolved from goid '" + goid.toString() + "' to '" + connector.getGoid().toString() + "'");
                        localGoid = connector.getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }
            }
        } catch (FindException e) {
            logger.warning("Cannot load Active Connector from goid, " + goid.toString());
        }

        return false;
    }

    private Collection<ExtensibleSocketConnectorEntity> findAllOutboundExtensibleSocketConnectors() {
        ArrayList<ExtensibleSocketConnectorEntity> outboundConnectors = new ArrayList<ExtensibleSocketConnectorEntity>();

        try {
            for (ExtensibleSocketConnectorEntity connector : getEntityManager(getFinder()).findAll()) {
                if (!connector.isIn()) {
                    outboundConnectors.add(connector);
                }
            }
        } catch (FindException e) {
            //
        }

        return outboundConnectors;
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
            if (assertionToLocalize instanceof ExtensibleSocketConnectorAssertion) {
                final ExtensibleSocketConnectorAssertion extensibleSocketConnectorAssertion = (ExtensibleSocketConnectorAssertion) assertionToLocalize;
                final Goid connectorGoid = extensibleSocketConnectorAssertion.getSocketConnectorGoid();
                if (connectorGoid.equals(goid)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if (localizeType == LocalizeAction.REPLACE) {
                        extensibleSocketConnectorAssertion.setSocketConnectorGoid(localGoid);
                    } else if (localizeType == LocalizeAction.DELETE) {
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

        final ExtensibleSocketConnectorReference that = (ExtensibleSocketConnectorReference) o;

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

        ExtensibleSocketConnectorReference output = new ExtensibleSocketConnectorReference((ExternalReferenceFinder) context);

        String value = getParamFromEl(el, ELMT_GOID);
        if (value != null) {
            output.goid = Goid.parseGoid(value);
        } else {
            value = getParamFromEl(el, ELMT_OID);
            if (value != null) {
                output.goid = GoidUpgradeMapper.mapOid(EntityType.GENERIC, Long.parseLong(value));
            }
        }

        output.name = getParamFromEl(el, ELMT_NAME);
        output.classname = getParamFromEl(el, ELMT_NAME_CLASSNAME);

        value = getParamFromEl(el, ELMT_IN);
        if (value != null) {
            output.in = Boolean.parseBoolean(value);

            if (output.in) {
                value = getParamFromEl(el, ELMT_PORT);
                if (value != null) {
                    output.port = Integer.parseInt(value);
                }

                value = getParamFromEl(el, ELMT_USE_SSL);
                if (value != null) {
                    output.useSsl = Boolean.parseBoolean(value);

                    if (output.useSsl) {
                        output.sslKeyId = getParamFromEl(el, ELMT_SSL_KEY_ID);

                        value = getParamFromEl(el, ELMT_CLIENT_AUTH);
                        if (value != null) {
                            output.clientAuthEnum = SSLClientAuthEnum.valueOf(value);
                        }
                    }
                }

                value = getParamFromEl(el, ELMT_THREAD_POOL_MIN);
                if (value != null) {
                    output.threadPoolMin = Integer.parseInt(value);
                }

                value = getParamFromEl(el, ELMT_THREAD_POOL_MAX);
                if (value != null) {
                    output.threadPoolMax = Integer.parseInt(value);
                }

                output.bindAddress = getParamFromEl(el, ELMT_BIND_ADDRESS);

                value = getParamFromEl(el, ELMT_SERVICE_GOID);
                if (value != null) {
                    output.serviceGoid = Goid.parseGoid(value);
                } else {
                    value = getParamFromEl(el, ELMT_SERVICE_OID);
                    output.serviceGoid = GoidUpgradeMapper.mapOid(EntityType.SERVICE, Long.parseLong(value));
                }

                output.contentType = getParamFromEl(el, ELMT_CONTENT_TYPE);

                value = getParamFromEl(el, ELMT_MAX_MSG_SIZE);
                if (value != null) {
                    output.maxMessageSize = Integer.parseInt(value);
                }

                value = getParamFromEl(el, ELMT_ENABLED);
                if (value != null) {
                    output.enabled = Boolean.parseBoolean(value);
                }
            } else {
                output.hostname = getParamFromEl(el, ELMT_HOSTNAME);

                value = getParamFromEl(el, ELMT_PORT);
                if (value != null) {
                    output.port = Integer.parseInt(value);
                }

                value = getParamFromEl(el, ELMT_USE_SSL);
                if (value != null) {
                    output.useSsl = Boolean.parseBoolean(value);

                    if (output.useSsl) {
                        output.sslKeyId = getParamFromEl(el, ELMT_SSL_KEY_ID);
                    }
                }

                output.contentType = getParamFromEl(el, ELMT_CONTENT_TYPE);

                value = getParamFromEl(el, ELMT_EXCHANGE_PATTERN);
                if (value != null) {
                    output.exchangePattern = ExchangePatternEnum.valueOf(value);
                }

                //If there is no keep alive or listenTimeOut tags, then it will use the default values
                value = getParamFromEl(el, ELMT_KEEP_ALIVE);
                if (value != null) {
                    output.keepAlive = Boolean.parseBoolean(value);
                } else {
                    output.keepAlive = false;
                }

                value = getParamFromEl(el, ELMT_LISTEN_TIMEOUT);
                if (value != null) {
                    output.listenTimeOut = Long.parseLong(value);
                } else {
                    output.listenTimeOut = 0L;
                }

                value = getParamFromEl(el, ELMT_DNS_LOOKUP);
                if (value != null) {
                    output.useDnsLookup = Boolean.parseBoolean(value);
                    if (output.useDnsLookup){
                        value = getParamFromEl(el, ELMT_DNS_DOMAIN);
                        output.dnsDomainName = value != null? value : "";
                        value = getParamFromEl(el, ELMT_DNS_SERVICE);
                        output.dnsService = value != null? value : "";
                    }
                } else {
                    output.useDnsLookup = false;
                }
            }
        }

        value = getParamFromEl(el, ELMT_CODEC_CONFIG);
        if (value != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(value.getBytes());
            SafeXMLDecoder decoder = new SafeXMLDecoderBuilder(bais).build();
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(ExtensibleSocketConnectorReference.class.getClassLoader());
            output.codecConfiguration = (CodecConfiguration) decoder.readObject();
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

        return output;
    }

    private boolean isMissing(final String value) {
        return value == null || value.isEmpty();
    }

    private boolean isMatch(final String leftValue,
                            final String rightValue) {
        return isMissing(leftValue) ? isMissing(rightValue) : leftValue.equals(rightValue);
    }
}
