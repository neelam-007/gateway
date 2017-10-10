package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.CassandraConnectionMO;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.util.DomUtils;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.TooManyChildElementsException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;

import static com.l7tech.util.DomUtils.findExactlyOneChildElementByName;
import static com.l7tech.util.DomUtils.getTextValue;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

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
    public static final String SK_ELE_DESC = SolutionKit.SK_PROP_DESC_KEY;
    public static final String SK_ELE_TIMESTAMP = SolutionKit.SK_PROP_TIMESTAMP_KEY;
    public static final String SK_ELE_IS_COLLECTION = SolutionKit.SK_PROP_IS_COLLECTION_KEY;
    public static final String SK_ELE_FEATURE_SET = SolutionKit.SK_PROP_FEATURE_SET_KEY;
    public static final String SK_ELE_CUSTOM_UI = "CustomUI";   // note the uppercase "I"
    public static final String SK_ELE_CUSTOM_CALLBACK = SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY;
    public static final String SK_ELE_ALLOW_ADDENDUM = SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY;

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
     * Convert an EncapsulatedAssertionMO object to an EncapsulatedAssertionConfig object.
     * Note: Do not set policy in this method, since SolutionKitUtils cannot access PolicyAdmin or PolicyManager.
     *
     * @param mo: a EncapsulatedAssertionMO object containing EncapsulatedAssertionConfig information.
     * @return a EncapsulatedAssertionConfig object
     */
    public static EncapsulatedAssertionConfig fromMangedObject (@NotNull final EncapsulatedAssertionMO mo) {
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        final String guid = mo.getGuid() == null? UUID.randomUUID().toString() : mo.getGuid();

        encapsulatedAssertionConfig.setName(mo.getName());
        encapsulatedAssertionConfig.setGuid(guid);
        encapsulatedAssertionConfig.setProperties(mo.getProperties() != null ? new HashMap<>(mo.getProperties()) : new HashMap<String, String>());
        encapsulatedAssertionConfig.setArgumentDescriptors(getArgumentDescriptorSet(mo, encapsulatedAssertionConfig));
        encapsulatedAssertionConfig.setResultDescriptors(getResultDescriptorSet(mo, encapsulatedAssertionConfig));

        return encapsulatedAssertionConfig;
    }

    // Since EncapsulatedAssertionResourceFactory is not accessible from this class,
    // this method is copied from EncapsulatedAssertionResourceFactory#getArgumentDescriptorSet.
    private static Set<EncapsulatedAssertionArgumentDescriptor> getArgumentDescriptorSet(EncapsulatedAssertionMO encassResource, EncapsulatedAssertionConfig entity) {
        Set<EncapsulatedAssertionArgumentDescriptor> ret = entity.getArgumentDescriptors();
        if (ret == null)
            ret = new HashSet<>();
        ret.clear();

        List<EncapsulatedAssertionMO.EncapsulatedArgument> args = encassResource.getEncapsulatedArguments();
        if (args != null) {
            for (EncapsulatedAssertionMO.EncapsulatedArgument arg : args) {
                EncapsulatedAssertionArgumentDescriptor r = new EncapsulatedAssertionArgumentDescriptor();
                r.setArgumentName(arg.getArgumentName());
                r.setArgumentType(arg.getArgumentType());
                r.setGuiLabel(arg.getGuiLabel());
                r.setGuiPrompt(arg.isGuiPrompt());
                r.setOrdinal(arg.getOrdinal());
                r.setEncapsulatedAssertionConfig(entity);
                ret.add(r);
            }
        }

        return ret;
    }

    /**
     * Convert a CassandraConnectionMO object to a CassandraConnection object.
     *
     * @param mo: a CassandraConnectionMO object containing CassandraConnection information.
     * @return a CassandraConnection object
     */
    public static CassandraConnection fromMangedObject (@NotNull final CassandraConnectionMO mo) {
        final CassandraConnection cassandraConnection = new CassandraConnection();

        cassandraConnection.setName(mo.getName());
        cassandraConnection.setKeyspaceName(mo.getKeyspace());
        cassandraConnection.setContactPoints(mo.getContactPoint());
        cassandraConnection.setPort(mo.getPort());
        cassandraConnection.setUsername(mo.getUsername());
        cassandraConnection.setPasswordGoid(mo.getPasswordId() == null ? null : new Goid(mo.getPasswordId()));
        cassandraConnection.setCompression(mo.getCompression());
        cassandraConnection.setSsl(mo.isSsl());
        cassandraConnection.setTlsEnabledCipherSuites(mo.getTlsciphers());
        cassandraConnection.setEnabled(mo.isEnabled());
        cassandraConnection.setProperties(mo.getProperties());

        return cassandraConnection;
    }


    // Since EncapsulatedAssertionResourceFactory is not accessible from this class,
    // This method is copied from EncapsulatedAssertionResourceFactory#getResultDescriptorSet.
    private static Set<EncapsulatedAssertionResultDescriptor> getResultDescriptorSet(EncapsulatedAssertionMO encassResource, EncapsulatedAssertionConfig entity) {
        Set<EncapsulatedAssertionResultDescriptor> ret = entity.getResultDescriptors();
        if (ret == null)
            ret = new HashSet<>();
        ret.clear();

        List<EncapsulatedAssertionMO.EncapsulatedResult> results = encassResource.getEncapsulatedResults();
        if (results != null) {
            for (EncapsulatedAssertionMO.EncapsulatedResult result : results) {
                EncapsulatedAssertionResultDescriptor r = new EncapsulatedAssertionResultDescriptor();
                r.setResultName(result.getResultName());
                r.setResultType(result.getResultType());
                r.setEncapsulatedAssertionConfig(entity);
                ret.add(r);
            }
        }

        return ret;
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
            throw new BadRequestException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_ID));
        }
        solutionKit.setSolutionKitGuid(skId);

        final String skVersion = getTextValue(findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_VERSION));
        if (isEmpty(skVersion)) {
            throw new BadRequestException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_VERSION));
        }
        solutionKit.setSolutionKitVersion(skVersion);

        final String skName = getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_NAME));
        if (isEmpty(skName)) {
            throw new BadRequestException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_NAME));
        }
        solutionKit.setName(skName);

        final String skDescription = getTextValue(findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_DESC));
        if (isEmpty(skDescription)) {
            throw new BadRequestException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_DESC));
        }
        solutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, skDescription);

        final String skTimestamp = getTextValue(findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_TIMESTAMP));
        if (isEmpty(skTimestamp)) {
            throw new BadRequestException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_TIMESTAMP));
        }
        solutionKit.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, skTimestamp);

        final String skIsCollection = getTextValue(findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_IS_COLLECTION));
        if (isEmpty(skIsCollection)) {
            throw new BadRequestException(format(requiredElementMessage, SK_NS_PREFIX, SK_ELE_IS_COLLECTION));
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

        final Element allowAddendumEle = DomUtils.findFirstChildElementByName(docEle, SK_NS, SK_ELE_ALLOW_ADDENDUM);
        if (allowAddendumEle != null) {
            solutionKit.setProperty(SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY, DomUtils.getTextValue(allowAddendumEle));
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
        final String allowAddendum = solutionKit.getProperty(SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY);
        if (allowAddendum != null) {
            DomUtils.createAndAppendElement(docEle, SK_ELE_ALLOW_ADDENDUM).setTextContent(allowAddendum);
        }

        return doc;
    }

    /**
     * Check whether any two selected solution kits have same GUID and same instance modifier
     * @return a string containing error report if any two solution kits have same GUID and instance modifier; Otherwise null if no any errors exist.
     */
    @Nullable
    public static String haveDuplicateSelectedSolutionKits(@NotNull final Collection<SolutionKit> selectedSolutionKits) {
        final Map<String, Map<String, Integer>> duplicateSKs = new HashMap<>();

        for (SolutionKit solutionKit: selectedSolutionKits) {
            String guid = solutionKit.getSolutionKitGuid();
            String instanceModifier = solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
            if (StringUtils.isEmpty(instanceModifier)) instanceModifier = "";

            Map<String, Integer> instanceModifierAmountMap = duplicateSKs.get(guid);
            if (instanceModifierAmountMap == null) {
                instanceModifierAmountMap = new HashMap<>();
                instanceModifierAmountMap.put(instanceModifier, 1);
            } else {
                Integer oldValue = instanceModifierAmountMap.get(instanceModifier);
                if (oldValue == null) oldValue = 0;

                int newAmount = oldValue + 1;
                instanceModifierAmountMap.put(instanceModifier, newAmount);
            }

            duplicateSKs.put(guid, instanceModifierAmountMap);
        }

        final StringBuilder report = new StringBuilder();
        for (String guid: duplicateSKs.keySet()) {
            Map<String, Integer> instanceModifierAmountMap = duplicateSKs.get(guid);
            for (String instanceModifier: instanceModifierAmountMap.keySet()) {
                if (instanceModifierAmountMap.get(instanceModifier) > 1) {
                    report.append("GUID = ").append(guid).append("    Instance Modifier = ").append("".equals(instanceModifier)? "N/A" : instanceModifier).append("\n");
                }
            }
        }

        if (report.length() > 0) {
            return report.toString();
        }

        return null;
    }

    public static Map<String, Set<String>> getGuidAndInstanceModifierMapFromUpgrade(@NotNull final List<SolutionKit> solutionKitsToUpgrade) {
        final Map<String, Set<String>> guidInstanceModifierMapFromUpgrade = new HashMap<>();
        String guid;
        Set<String> instanceModifierSet;

        for (SolutionKit solutionKitToUpgrade: solutionKitsToUpgrade) {
            guid = solutionKitToUpgrade.getSolutionKitGuid();

            instanceModifierSet = guidInstanceModifierMapFromUpgrade.get(guid);
            if (instanceModifierSet == null) {
                instanceModifierSet = new HashSet<>();
            }
            instanceModifierSet.add(solutionKitToUpgrade.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));

            guidInstanceModifierMapFromUpgrade.put(guid, instanceModifierSet);
        }

        return guidInstanceModifierMapFromUpgrade;
    }

    public static boolean isParentSolutionKit(@NotNull final SolutionKit candidate) {
        return isCollectionOfSkars(candidate) && StringUtils.isBlank(candidate.getMappings());
    }

    public static boolean isCollectionOfSkars(@NotNull final SolutionKit candidate) {
        return Boolean.parseBoolean(candidate.getProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY));
    }

    /**
     * Copy the contents of a parent solution kit object to make another parent solution kit object.
     * @param originalSK The SolutionKit target
     * @param instanceModifier The new instance modifier
     * @return solutionKit is the copied solution kit
     */
    public static SolutionKit copyParentSolutionKit(@NotNull final SolutionKit originalSK, @Nullable final String instanceModifier) {
        final SolutionKit solutionKit = new SolutionKit();
        solutionKit.setSolutionKitGuid(originalSK.getSolutionKitGuid());
        solutionKit.setSolutionKitVersion(originalSK.getSolutionKitVersion());
        solutionKit.setName(originalSK.getName());
        solutionKit.setMappings(originalSK.getMappings());
        solutionKit.setLastUpdateTime(originalSK.getLastUpdateTime());
        solutionKit.setXmlProperties(originalSK.getXmlProperties());
        solutionKit.setInstallationXmlProperties(originalSK.getInstallationXmlProperties());
        solutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, instanceModifier);
        return solutionKit;
    }

    private SolutionKitUtils() {}
}