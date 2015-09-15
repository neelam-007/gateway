package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.solutionkit.BadRequestException;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.EntityHeader;
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
     * Search a solution kit object from a list of solution kits, based on a given solution kit guid.
     * @param guid: the solution kit GUID used to search
     * @return a solution kit, whose GUID is the same as the GUID of one of solution kits in solutionKitsToUpgrade
     */
    public static SolutionKit searchSolutionKitByGuidToUpgrade(@NotNull final List<SolutionKit> solutionKitsToUpgrade, @NotNull final String guid) {
        for (SolutionKit solutionKit: solutionKitsToUpgrade) {
            if (guid.equals(solutionKit.getSolutionKitGuid())) {
                return solutionKit;
            }
        }

        return null;
    }

    /**
     * Check if the instance modifier of a selected solution kit is unique or not.
     *
     * @param solutionKit: a solution kit whose instance modifier will be checked.
     * @param usedInstanceModifiersMap: a map of solution kit guid and a list of instance modifiers used by all solution kits with such guid.
     * @return true if the instance modifier is unique.  That is, the instance modifier is not used by other solution kit instances.
     */
    public static boolean checkInstanceModifierUniqueness(@NotNull final SolutionKit solutionKit, @NotNull final Map<String, List<String>> usedInstanceModifiersMap) {
        final String solutionKitGuid = solutionKit.getSolutionKitGuid();
        if (usedInstanceModifiersMap.keySet().contains(solutionKitGuid)) {
            final List<String> usedInstanceModifiers = usedInstanceModifiersMap.get(solutionKitGuid);
            final String newInstanceModifier = solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);

            if (usedInstanceModifiers != null && usedInstanceModifiers.contains(newInstanceModifier)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Find all instance modifiers used by all solution kit instances.
     *
     * @param solutionKitHeaders: solution kit headers
     * @return a map of solution kit guid and a list of instance modifiers used by all solution kits with such guid.
     */
    public static Map<String, List<String>> getInstanceModifiers(@NotNull final Collection<SolutionKitHeader> solutionKitHeaders) {
        final Map<String, List<String>> instanceModifiers = new HashMap<>();

        for (EntityHeader header: solutionKitHeaders) {
            if (! (header instanceof SolutionKitHeader)) continue;  // This line is to avoid to break the test, signedSkar() in SolutionKitManagerResourceTest.

            SolutionKitHeader solutionKitHeader = (SolutionKitHeader) header;
            String solutionKitGuid = solutionKitHeader.getSolutionKitGuid();
            java.util.List<String> usedInstanceModifiers = instanceModifiers.get(solutionKitGuid);
            if (usedInstanceModifiers == null) {
                usedInstanceModifiers = new ArrayList<>();
            }
            usedInstanceModifiers.add(solutionKitHeader.getInstanceModifier());
            instanceModifiers.put(solutionKitGuid, usedInstanceModifiers);
        }

        return instanceModifiers;
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

    public static String findTargetInstanceModifier(@NotNull final SolutionKit selectedSolutionKit, @NotNull final List<SolutionKit> solutionKitsToUpgrade) {
        for (SolutionKit solutionKit: solutionKitsToUpgrade) {
            if (selectedSolutionKit.getSolutionKitGuid().equals(solutionKit.getSolutionKitGuid())) {
                return solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
            }
        }
        throw new IllegalArgumentException("Invalid parameter of a solution kit, '" + selectedSolutionKit.getName() + "'");
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

    private SolutionKitUtils() {}
}