package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CassandraConnectionable;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

public class CassandraConnectionReference extends ExternalReference {
    private final Logger logger = Logger.getLogger(CassandraConnectionReference.class.getName());

    private static final String REFERENCE = "CassandraConnectionReference";
    private static final String CONNECTION_NAME = "ConnectionName";
    private static final String KEYSPACE = "Keyspace";
    private static final String CONTACT_POINTS = "ContactPoints";
    private static final String PORT = "Port";
    private static final String USERNAME = "Username";
    private static final String COMPRESSION = "Compression";
    private static final String USE_SSL = "Ssl";
    private static final String ENABLED = "Enabled";
    private static final String PROPERTIES = "Properties";
    private static final String PROPERTY = "Property";
    private static final String PROPERTY_NAME = "Name";
    private static final String PROPERTY_VALUE = "Value";

    private String connectionName;
    private String keyspace;
    private String contactPoints;
    private String port;
    private String username;
    private String compression;
    private boolean ssl;
    private boolean enabled;
    private Map<String, String> properties = new TreeMap<>();

    private String localConnectionName;
    private LocalizeAction localizeType;

    public CassandraConnectionReference(final ExternalReferenceFinder finder) {
        super(finder);
    }

    public CassandraConnectionReference(final ExternalReferenceFinder context, final CassandraConnectionable connable) {
        this(context);
        if (connable == null) throw new IllegalStateException("CassandraConnectionable must not be null.");

        connectionName = connable.getConnectionName();
        try {
            CassandraConnection connection = getFinder().getCassandraConnection(connectionName);
            if (connection != null) {
                keyspace = connection.getKeyspaceName();
                contactPoints = connection.getContactPoints();
                port = connection.getPort();
                username = connection.getUsername();
                compression = connection.getCompression();
                ssl = connection.isSsl();
                enabled = connection.isEnabled();
                properties = connection.getProperties();
            }
        } catch (FindException e) {
            logger.warning("Cannot find the Cassandra connection entity (ConnectionName = " + connable.getConnectionName() + ").");
        }
        localizeType = LocalizeAction.IGNORE;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public String getContactPoints() {
        return contactPoints;
    }

    public void setContactPoints(String contactPoints) {
        this.contactPoints = contactPoints;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
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

    public void setLocalizeReplaceByName(String connectionName) {
        localizeType = LocalizeAction.REPLACE;
        localConnectionName = connectionName;
    }

    public static CassandraConnectionReference parseFromElement(final ExternalReferenceFinder context, final Element elmt) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!elmt.getNodeName().equals(REFERENCE)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REFERENCE);
        }

        CassandraConnectionReference output = new CassandraConnectionReference(context);
        output.connectionName = getParamFromEl(elmt, CONNECTION_NAME);
        output.keyspace = getParamFromEl(elmt, KEYSPACE);
        output.contactPoints = getParamFromEl(elmt, CONTACT_POINTS);
        output.port = getParamFromEl(elmt, PORT);
        output.username = getParamFromEl(elmt, USERNAME);
        output.compression = getParamFromEl(elmt, COMPRESSION);
        output.ssl = Boolean.parseBoolean(getParamFromEl(elmt, USE_SSL));
        output.enabled = Boolean.parseBoolean(getParamFromEl(elmt, ENABLED));

        final NodeList additionalPropertyNodes = elmt.getElementsByTagName(PROPERTY);
        for (int i = 0; i < additionalPropertyNodes.getLength(); i++) {
            final Node node = additionalPropertyNodes.item(i);
            if (node instanceof Element) {
                final Element element = (Element) node;
                final String name = getParamFromEl(element, PROPERTY_NAME);
                final String value = getParamFromEl(element, PROPERTY_VALUE);
                output.properties.put(name, value);
            }
        }
        return output;
    }

    @Override
    protected void serializeToRefElement(final Element referencesParentElement) {
        final Document doc = referencesParentElement.getOwnerDocument();
        Element referenceElement = doc.createElement(REFERENCE);
        setTypeAttribute(referenceElement);
        referencesParentElement.appendChild(referenceElement);

        addParameterElement(CONNECTION_NAME, connectionName, referenceElement);
        addParameterElement(KEYSPACE, keyspace, referenceElement);
        addParameterElement(CONTACT_POINTS, contactPoints, referenceElement);
        addParameterElement(PORT, port, referenceElement);
        addParameterElement(USERNAME, username, referenceElement);
        addParameterElement(COMPRESSION, compression, referenceElement);
        addParameterElement(USE_SSL, Boolean.toString(ssl), referenceElement);
        addParameterElement(ENABLED, Boolean.toString(enabled), referenceElement);

        if (properties != null) {
            final Element propertiesElement = doc.createElement(PROPERTIES);
            referenceElement.appendChild(propertiesElement);
            for (final Map.Entry<String, String> entry : properties.entrySet()) {
                final Element propertyElement = doc.createElement(PROPERTY);
                addParameterElement(PROPERTY_NAME, entry.getKey(), propertyElement);
                addParameterElement(PROPERTY_VALUE, entry.getValue() != null ? entry.getValue() : null, propertyElement);
                propertiesElement.appendChild(propertyElement);
            }
        }
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
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        if (Syntax.getReferencedNames(connectionName).length > 0) {
            return true;
        }
        try {
            CassandraConnection connection = getFinder().getCassandraConnection(connectionName);
            return connection != null &&
                    connection.getName().equalsIgnoreCase(connectionName) &&
                    (keyspace == null || connection.getKeyspaceName().equals(keyspace)) &&
                    (contactPoints == null || connection.getContactPoints().equals(contactPoints)) &&
                    (port == null || connection.getPort().equals(port)) &&
                    (username == null || connection.getUsername().equals(username)) &&
                    (compression == null || connection.getCompression().equals(compression)) &&
                    (connection.isSsl() == ssl) &&
                    (connection.isEnabled() == enabled) &&
                    (properties == null || connection.getProperties().equals(properties));
        } catch (FindException e) {
            logger.warning("Cannot find a Cassandra connection, " + connectionName);
            return false;
        }
    }

    @Override
    protected boolean localizeAssertion(final @Nullable Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof CassandraConnectionable) {
                final CassandraConnectionable connable = (CassandraConnectionable) assertionToLocalize;
                if (connable.getConnectionName().equalsIgnoreCase(connectionName)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if (localizeType == LocalizeAction.REPLACE) {
                        connable.setConnectionName(localConnectionName);
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

        final CassandraConnectionReference that = (CassandraConnectionReference) o;

        if (connectionName != null ? !connectionName.equals(that.connectionName) : that.connectionName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return connectionName != null ? connectionName.hashCode() : 0;
    }
}
