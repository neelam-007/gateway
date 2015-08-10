package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.TooManyChildElementsException;
import static com.l7tech.util.DomUtils.findExactlyOneChildElementByName;
import static com.l7tech.util.DomUtils.getTextValue;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang.StringUtils.isEmpty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains utility methods for the solution kit manager.
 */
public final class SolutionKitUtils {

    public static final String SK_NS = "http://ns.l7tech.com/2010/04/gateway-management";
    public static final String SK_NS_PREFIX = "l7";
    public static final String SK_ELE_ROOT = "SolutionKit";
    public static final String SK_ELE_ID = "Id";
    public static final String SK_ELE_VERSION = "Version";
    public static final String SK_ELE_NAME = "Name";
    public static final String SK_ELE_DESC = "Description";
    public static final String SK_ELE_TIMESTAMP = "TimeStamp";
    public static final String SK_ELE_IS_COLLECTION = "IsCollection";
    public static final String SK_ELE_FEATURE_SET = "FeatureSet";
    public static final String SK_ELE_CUSTOM_UI = "CustomUI";
    public static final String SK_ELE_CUSTOM_CALLBACK = "CustomCallback";
    //TODO when Dependencies is implemented
    // public static final String SK_ELE_DEPENDENCIES = "Dependencies";


    /**
     * Converts the given managed object to an entity.
     *
     * @param mo the managed object
     * @return the entity
     */
    public static JdbcConnection fromMangedObject (JDBCConnectionMO mo) {
        // todo (kpak) : It would be good idea to reuse code from the modular GatewayManagementAssertion's
        // com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory#fromResource()
        //
        final JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName(mo.getName());
        jdbcConnection.setEnabled(mo.isEnabled());
        jdbcConnection.setDriverClass(mo.getDriverClass());
        jdbcConnection.setJdbcUrl(mo.getJdbcUrl());

        Map<String, Object> connectionProps = mo.getConnectionProperties();
        if (connectionProps != null) {
            Object value = connectionProps.get("user");
            if (value != null) {
                jdbcConnection.setUserName((String) value);
            }

            value = connectionProps.get("password");
            if (value != null) {
                jdbcConnection.setPassword((String) value);
            }

            Map<String, Object> additionProps = new HashMap<>(mo.getConnectionProperties());
            additionProps.remove("user");
            additionProps.remove("password");
            jdbcConnection.setAdditionalProperties(additionProps);
        }

        Map<String, Object> props = mo.getProperties();
        if (props != null) {
            Object value = props.get("maximumPoolSize");
            if (value != null) {
                jdbcConnection.setMaxPoolSize((int) value);
            }

            value = props.get("minimumPoolSize");
            if (value != null) {
                jdbcConnection.setMinPoolSize((int) value);
            }
        }

        return jdbcConnection;
    }

    /**
     * Converts the given managed object to an entity.
     *
     * @param mo the managed object
     * @return the entity
     */
    public static SecurePassword fromMangedObject (StoredPasswordMO mo) {
        // todo (kpak) : It would be good idea to reuse code from the modular GatewayManagementAssertion's
        // com.l7tech.external.assertions.gatewaymanagement.server.SecurePasswordResourceFactory#fromResource()
        //
        final SecurePassword securePassword = new SecurePassword();
        securePassword.setName(mo.getName());

        Map<String, Object> props = mo.getProperties();
        if (props != null) {
            Object value = props.get("description");
            if (value != null) {
                securePassword.setDescription((String) value);
            }

            value =  props.get("type");
            if (value!= null) {
                if ("Password".equals(value)) {
                    securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
                } else if ("PEM Private Key".equals(value)) {
                    securePassword.setType(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
                }
            }

            value = props.get("usageFromVariable");
            if (value != null) {
                securePassword.setUsageFromVariable((Boolean) value);
            }
        }

        return securePassword;
    }

    /**
     * Copy the contents of a Document XML object to a SolutionKit object.
     * @param doc The Solution Kit XML source
     * @param solutionKit The SolutionKit target
     * @throws TooManyChildElementsException
     * @throws MissingRequiredElementException
     */
    public static void copyDocumentToSolutionKit(final Document doc, final SolutionKit solutionKit) throws TooManyChildElementsException, MissingRequiredElementException, SolutionKitException {
        final Element docEle = doc.getDocumentElement();
        final String requiredElementMessage = "Element <{0}:{1}> value cannot be empty.";

        final String skId = getTextValue(findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_ID));
        if (isEmpty(skId)) {
            throw new SolutionKitException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_ID));
        }
        solutionKit.setSolutionKitGuid(skId);

        final String skVersion = getTextValue(findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_VERSION));
        if (isEmpty(skVersion)) {
            throw new SolutionKitException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_VERSION));
        }
        solutionKit.setSolutionKitVersion(skVersion);

        final String skName = getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_NAME));
        if (isEmpty(skName)) {
            throw new SolutionKitException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_NAME));
        }
        solutionKit.setName(skName);

        final String skDescription = getTextValue(findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_DESC));
        if (isEmpty(skDescription)) {
            throw new SolutionKitException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_DESC));
        }
        solutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, skDescription);

        final String skTimestamp = getTextValue(findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_TIMESTAMP));
        if (isEmpty(skTimestamp)) {
            throw new SolutionKitException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_TIMESTAMP));
        }
        solutionKit.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, skTimestamp);

        final String skIsCollection = getTextValue(findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_IS_COLLECTION));
        if (isEmpty(skIsCollection)) {
            throw new SolutionKitException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_IS_COLLECTION));
        }
        solutionKit.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, skIsCollection);

        final Element featureSet = DomUtils.findFirstChildElementByName(docEle, SK_NS, SK_ELE_FEATURE_SET);
        if (featureSet != null) {
            solutionKit.setProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY, DomUtils.getTextValue(featureSet));
        }

        final Element customUiEle = DomUtils.findFirstChildElementByName(docEle, SK_NS, SK_ELE_CUSTOM_UI);
        if (customUiEle != null) {
            solutionKit.setProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY, DomUtils.getTextValue(customUiEle));
        }

        final Element customCallbackEle = DomUtils.findFirstChildElementByName(docEle, SK_NS, SK_ELE_CUSTOM_CALLBACK);
        if (customCallbackEle != null) {
            solutionKit.setProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY, DomUtils.getTextValue(customCallbackEle));
        }

        //TODO - Copying over of Dependencies element and its child elements
    }

    /**
     * Create a Document object based on values of a SolutionKit object
     * @param solutionKit The SolutionKit to create from
     * @return The newly created Solution Kit XML
     * @throws TooManyChildElementsException
     * @throws MissingRequiredElementException
     */
    public static Document createDocument(final SolutionKit solutionKit) throws TooManyChildElementsException, MissingRequiredElementException {
        final Document doc = XmlUtil.createEmptyDocument(SK_ELE_ROOT, SK_NS_PREFIX, SK_NS);
        final Element docEle = doc.getDocumentElement();

        DomUtils.createAndAppendElement(docEle, SK_ELE_ID).setTextContent(solutionKit.getSolutionKitGuid());
        DomUtils.createAndAppendElement(docEle, SK_ELE_VERSION).setTextContent(solutionKit.getSolutionKitVersion());
        DomUtils.createAndAppendElement(docEle, SK_ELE_NAME).setTextContent(solutionKit.getName());
        DomUtils.createAndAppendElement(docEle, SK_ELE_DESC).setTextContent(solutionKit.getProperty(SolutionKit.SK_PROP_DESC_KEY));
        DomUtils.createAndAppendElement(docEle, SK_ELE_TIMESTAMP).setTextContent(solutionKit.getProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY));
        DomUtils.createAndAppendElement(docEle, SK_ELE_IS_COLLECTION).setTextContent(solutionKit.getProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY));

        final String featureSet = solutionKit.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
        if (featureSet != null) {
            DomUtils.createAndAppendElement(docEle, SK_ELE_FEATURE_SET).setTextContent(featureSet);
        }
        final String customCallback = solutionKit.getProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY);
        if (customCallback != null) {
            DomUtils.createAndAppendElement(docEle, SK_ELE_CUSTOM_CALLBACK).setTextContent(customCallback);
        }
        final String customUi = solutionKit.getProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY);
        if (customUi != null) {
            DomUtils.createAndAppendElement(docEle, SK_ELE_CUSTOM_UI).setTextContent(customUi);
        }

        return doc;
    }

    private SolutionKitUtils() {}
}