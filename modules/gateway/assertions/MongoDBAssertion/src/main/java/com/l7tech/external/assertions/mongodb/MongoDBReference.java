package com.l7tech.external.assertions.mongodb;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdmin;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdminImpl;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by chaja24 on 9/2/2015.
 */
public class MongoDBReference extends ExternalReference {
    private static final Logger logger = Logger.getLogger(MongoDBReference.class.getName());

    private static final String ELMT_PROPERTY = "Property";
    private static final String ELMT_PROPERTY_ATTR_NAME = "mame";

    private static final String ELMT_NAME_REF = "MongoDBReference";
    private static final String ELMT_GOID = "GOID";
    private static final String ELMT_OID = "OID";
    private static final String ELMT_CONNECTIONNAME = "ConnectionName";
    private static final String ELMT_CLASSNAME = "Classname";
    private static final String ELMT_DATABASENAME = "Database";
    private static final String ELMT_SERVERNAME = "Servername";
    private static final String ELMT_PORT = "Port";
    private static final String ELMT_USERNAME = "Username";
    private static final String ELMT_ENCRYPTION = "Encryption";
    private static final String ELMT_USESDEFAULTKEYSTORE = "UsesDefaultKeyStore";
    private static final String ELMT_USESNOKEY = "UsesNoKey";
    private static final String ELMT_KEYALIAS = "KeyAlias";
    private static final String ELMT_NONDEFAULTKEYSTOREID = "NonDefaultKeyStoreId";
    private static final String ELMT_READ_PREFERENCE = "ReadPreference";

    private Goid goid;
    private String className;
    private String connectionName;
    private String databaseName;
    private String serverName;
    private String portNumber;
    private String userName;
    private String encryption;
    private boolean usesDefaultKeyStore;
    private boolean usesNoKey;
    private String privateKeyAlias;
    private Goid nonDefaultKeystoreId;
    private String readPreference;
    private Goid localGoid;
    private LocalizeAction localizeType;

    public Goid getGoid() {
        return goid;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    public boolean isUsesNoKey() {
        return usesNoKey;
    }

    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public Goid getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    public String getReadPreference() {
        return readPreference;
    }

    public String getServerName() {
        return serverName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getPortNumber() {
        return portNumber;
    }

    public String getUserName() {
        return userName;
    }

    public String getEncryption() {
        return encryption;
    }


    public MongoDBReference(ExternalReferenceFinder finder) {
        super(finder);
    }

    private static MongoDBConnectionEntityAdmin getEntityManager(ExternalReferenceFinder finder) throws FindException {
        if (finder.getClass().getName().contains("Console")) {
            return getEntityManager();
        } else {
            return MongoDBConnectionEntityAdminImpl.getInstance(null);
        }
    }

    private static MongoDBConnectionEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(MongoDBConnectionEntityAdmin.class, null);
    }

    public MongoDBReference(ExternalReferenceFinder finder, MongoDBAssertion assertion) {
        super(finder);
        try {

            MongoDBConnectionEntity entity = getEntityManager(finder).findByGoid(assertion.getConnectionGoid());

            if (entity != null) {

                goid = entity.getGoid();
                className = entity.getEntityClassName();
                connectionName = entity.getName();
                serverName = entity.getUri();
                databaseName = entity.getDatabaseName();
                portNumber = entity.getPort();
                userName = entity.getUsername();
                encryption = entity.getAuthType();
                usesDefaultKeyStore = entity.isUsesDefaultKeyStore();
                usesNoKey = entity.isUsesNoKey();
                privateKeyAlias = entity.getKeyAlias();
                nonDefaultKeystoreId = entity.getNonDefaultKeystoreId();
                readPreference = entity.getReadPreference();
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve Entity from EntityManager");
        }
    }

    @Override
    public String getRefId() {
        String id = null;

        if ((goid != null) && !goid.equals(GenericEntity.DEFAULT_GOID)) {
            id = goid.toString();
        }

        return id;
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

        if (goid != null) {
            addParameterElement(ELMT_GOID, goid.toString(), referenceElement);
        }
        addParameterElement(ELMT_CONNECTIONNAME, connectionName, referenceElement);
        addParameterElement(ELMT_CLASSNAME, className, referenceElement);
        addParameterElement(ELMT_DATABASENAME, databaseName, referenceElement);
        addParameterElement(ELMT_SERVERNAME, serverName, referenceElement);
        addParameterElement(ELMT_PORT, portNumber, referenceElement);
        addParameterElement(ELMT_USERNAME, userName, referenceElement);
        addParameterElement(ELMT_ENCRYPTION, encryption, referenceElement);
        addParameterElement(ELMT_USESDEFAULTKEYSTORE, String.valueOf(usesDefaultKeyStore), referenceElement);
        addParameterElement(ELMT_USESNOKEY, String.valueOf(usesNoKey), referenceElement);
        addParameterElement(ELMT_KEYALIAS,privateKeyAlias , referenceElement);
        if (nonDefaultKeystoreId != null){
            addParameterElement(ELMT_NONDEFAULTKEYSTOREID,nonDefaultKeystoreId.toString() , referenceElement);
        }

        addParameterElement(ELMT_READ_PREFERENCE, readPreference, referenceElement);


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
            final MongoDBConnectionEntity foundMongoDBConnection = getEntityManager(getFinder()).findByGoid(goid);
            if (foundMongoDBConnection != null) {
                if (isMatch(foundMongoDBConnection.getName(), connectionName) && permitMapping(goid, foundMongoDBConnection.getGoid())) {
                    // Perfect Match (OID and name are matched.)
                    logger.fine("The MongoDB was resolved by goid '" + goid.toString() + "' and name '" + foundMongoDBConnection.getName() + "'");
                    return true;
                }
            } else {

                final Collection<MongoDBConnectionEntity> foundMongoDBConnections = getEntityManager(getFinder()).findByType();
                for (MongoDBConnectionEntity mongoDBConnection : foundMongoDBConnections) {
                    if (isMatch(mongoDBConnection.getName(), connectionName) && permitMapping(goid, mongoDBConnection.getGoid())) {
                        // Connector Name matched
                        logger.fine("The MongoDB Connection was resolved from goid '" + goid.toString() + "' to '" + mongoDBConnection.getGoid() + "'");
                        localGoid = mongoDBConnection.getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }
            }
        } catch (FindException e) {
            if (goid != null){
                logger.warning("Cannot load Active Connector from goid, " + goid.toString());
            } else {
                logger.warning("Cannot load Active Connector from goid.  The Goid is null");
            }

        }

        return false;
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
            if (assertionToLocalize instanceof MongoDBAssertion) {
                final MongoDBAssertion mongoDBAssertion = (MongoDBAssertion) assertionToLocalize;
                final Goid mongoDBConnectionGoid = mongoDBAssertion.getConnectionGoid();
                if (mongoDBConnectionGoid != null && mongoDBConnectionGoid.equals(goid)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if (localizeType == LocalizeAction.REPLACE) {
                        mongoDBAssertion.setConnectionGoid(localGoid);
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MongoDBReference that = (MongoDBReference) o;

        if (connectionName != null ? !connectionName.equals(that.connectionName) : that.connectionName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return connectionName != null ? connectionName.hashCode() : 0;
    }

    public static Object parseFromElement(Object context, Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct connectorName
        if (!el.getNodeName().equals(ELMT_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of connectorName " + ELMT_NAME_REF);
        }

        MongoDBReference output = new MongoDBReference((ExternalReferenceFinder) context);

        String value = getParamFromEl(el, ELMT_GOID);
        if (value != null) {
            output.goid = Goid.parseGoid(value);
        } else {
            value = getParamFromEl(el, ELMT_OID);
            output.goid = GoidUpgradeMapper.mapOid(EntityType.GENERIC, Long.parseLong(value));
        }

        output.connectionName = getParamFromEl(el, ELMT_CONNECTIONNAME);
        output.className = getParamFromEl(el, ELMT_CLASSNAME);
        output.databaseName = getParamFromEl(el, ELMT_DATABASENAME);
        output.serverName = getParamFromEl(el, ELMT_SERVERNAME);
        output.portNumber = getParamFromEl(el, ELMT_PORT);
        output.userName = getParamFromEl(el, ELMT_USERNAME);
        output.encryption = getParamFromEl(el, ELMT_ENCRYPTION);
        output.usesDefaultKeyStore = Boolean.parseBoolean(getParamFromEl(el, ELMT_USESDEFAULTKEYSTORE));
        output.usesNoKey =  Boolean.parseBoolean(getParamFromEl(el, ELMT_USESNOKEY));
        output.privateKeyAlias = getParamFromEl(el, ELMT_KEYALIAS);

        value = getParamFromEl(el, ELMT_NONDEFAULTKEYSTOREID);
        if (value != null) {
            output.nonDefaultKeystoreId = Goid.parseGoid(value);
        }

        output.readPreference = getParamFromEl(el, ELMT_READ_PREFERENCE);

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
