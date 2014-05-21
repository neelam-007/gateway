package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.entity.EntitiesResolver;
import com.l7tech.objectmodel.CustomKeyStoreEntityHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreException;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Logger;


/**
 * The custom key value store external reference element.
 * <p/>
 * Custom Assertion custom-key-value-store entity references parent element-node will look like:
 * <blockquote><pre>
 * {@code
 * <CustomKeyValueReference RefType="com.l7tech.policy.exporter.CustomKeyValueReference">
 *     <Serializer>...</Serializer>   // optional
 *     <Config>
 *         <KeyValueStoreName>...</KeyValueStoreName>
 *         <Key>...</Key>
 *         <KeyPrefix>...</KeyPrefix>
 *         <Base64Value>...</Base64Value>
 *     </Config>
 * </CustomKeyValueReference>
 * }
 * </pre><blockquote>
 * <p/>
 * Typically used during both policy import and export to reference missing custom key value store element.
 */
public class CustomKeyValueReference extends ExternalReference {
    private static final Logger logger = Logger.getLogger(CustomKeyValueReference.class.getName());

    /**
     * Root XML node for an custom-key-value-store entity
     */
    private static final String REFERENCE_ROOT_NODE = "CustomKeyValueReference";

    /**
     * Represents name of the class that implements CustomEntitySerializer.
     * <p/>
     * Serializer is optional, and will be added automatically exported when the entity class implements
     * CustomEntityDescriptor interface, and a non-null Serializer object is provided when entity is set
     * (i.e. call to {@link com.l7tech.policy.assertion.ext.entity.CustomReferenceEntitiesSupport#setKeyValueStoreReference(
     * String, String, String, com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer)
     * CustomReferenceEntitiesSupport#setReference(...)})
     */
    private static final String REFERENCE_SERIALIZER_NODE = "Serializer";

    /**
     * Represents the parent node holding entity specific config
     */
    private static final String REFERENCE_CONFIG_NODE = "Config";

    /**
     * Represents the custom-key-value-store store name.
     * <p/>
     * Currently only {@link com.l7tech.policy.assertion.ext.store.KeyValueStoreServices#INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME internalTransactional}
     * is exposed to the developer.
     */
    private static final String REFERENCE_CONFIG_KEY_VALUE_STORE_NAME_NODE = "KeyValueStoreName";

    /**
     * Represents a unique identifier for the entity in the custom-key-value-store.<br/>
     * This is a key that includes a prefix and identifier chosen by the developer.
     * For example: <i>com.l7tech.custom.salesforce.partner.v26.assertion.SalesForceConnection.sf-prod-connection</i>
     */
    private static final String REFERENCE_CONFIG_KEY_NODE = "Key";

    /**
     * Represents the prefix portion of the key in the custom-key-value-store.<br/>
     * The prefix is used by the developer to identify all entities belonging to the Assertion.
     * For example: <i>com.l7tech.custom.salesforce.partner.v26.assertion.SalesForceConnection.</i>
     */
    private static final String REFERENCE_CONFIG_PREFIX_NODE = "KeyPrefix";

    /**
     * Represents the base64 encoded entity bytes. This is the custom-key-value-store value.
     */
    private static final String REFERENCE_CONFIG_VALUE_NODE = "Base64Value";

    /**
     * Mandatory.  Represents the unique identifier of the custom-key-value-store entity key, <i>prefix+keyId</i>
     */
    @NotNull private final String entityKey;

    /**
     * Getter for {@link #entityKey}
     */
    @NotNull public String getEntityKey() { return entityKey; }

    /**
     * Mandatory.  Represents the prefix portion of custom-key-value-store entity key, <i>prefix+keyId</i>
     */
    @NotNull private final String entityKeyPrefix;

    /**
     * Getter for {@link #entityKeyPrefix}
     */
    @NotNull public String getEntityKeyPrefix() { return entityKeyPrefix; }

    /**
     * Mandatory.  Represents the base64 encoded custom-key-value-store entity bytes.
     */
    @NotNull private final String entityBase64Value;

    /**
     * Getter for {@link #entityBase64Value}
     */
    @NotNull public String getEntityBase64Value() { return entityBase64Value; }

    /**
     * Optional.  Represents the custom-key-value-store entity serializer class name.<br/>
     * If the assertion developer decides to implement custom summery and/or create dialogs, then we need the
     * serializer to get the external entity object from the bytes.
     */
    @Nullable private final String entitySerializer;

    /**
     * Getter for {@link #entitySerializer}
     */
    @Nullable public String getEntitySerializer() { return entitySerializer; }

    /**
     * Specifies the localization action the user performed for this external reference.<br/>
     * @see com.l7tech.policy.exporter.ExternalReference.LocalizeAction
     */
    @Nullable private LocalizeAction localizeType;

    /**
     * Specify which custom-key-value-store entity key the user decided to replace this external reference with.
     */
    @Nullable private String replaceRefKey;

    /**
     * During import.
     */
    private CustomKeyValueReference(
            @NotNull final ExternalReferenceFinder finder,
            @NotNull final String entityKey,
            @NotNull final String entityKeyPrefix,
            @NotNull final String entityBase64Value,
            @Nullable final String entitySerializer
    ) {
        super(finder);

        this.entityKey = entityKey;
        this.entityKeyPrefix = entityKeyPrefix;
        this.entityBase64Value = entityBase64Value;
        this.entitySerializer = entitySerializer;
    }

    /**
     * During export.
     */
    public CustomKeyValueReference(
            @NotNull final ExternalReferenceFinder finder,
            @NotNull final CustomKeyStoreEntityHeader entityHeader
    ) {
        this(
                finder,
                entityHeader.getName(), 
                entityHeader.getEntityKeyPrefix(),
                entityHeader.getEntityBase64Value(), // shouldn't be null since check is done before calling constructor
                entityHeader.getEntitySerializer()
        );
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

    public boolean setLocalizeReplace(final String replaceRefKey) {
        localizeType = LocalizeAction.REPLACE;
        this.replaceRefKey = replaceRefKey;
        return true;
    }

    @Override
    protected void serializeToRefElement(@NotNull final Element referencesParentElement) {
        // make sure references element has a Document parent.
        final Document document = DomUtils.getOwnerDocument(referencesParentElement);
        if (document == null) {
            throw new IllegalArgumentException("Failed to get node [" + referencesParentElement.getNodeName() + "] owner document");
        }

        final Element referenceElement = document.createElement(REFERENCE_ROOT_NODE);
        setTypeAttribute(referenceElement);
        referencesParentElement.appendChild(referenceElement);

        // add serializer node
        addParamEl(referenceElement, REFERENCE_SERIALIZER_NODE, getEntitySerializer(), false);

        // create external reference root node
        final Element nodeConfig = document.createElementNS(null, REFERENCE_CONFIG_NODE);

        // add ref key, prefix and value
        addParamEl(nodeConfig, REFERENCE_CONFIG_KEY_VALUE_STORE_NAME_NODE, KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME, true);
        addParamEl(nodeConfig, REFERENCE_CONFIG_KEY_NODE, getEntityKey(), true);
        addParamEl(nodeConfig, REFERENCE_CONFIG_PREFIX_NODE, getEntityKeyPrefix(), true);
        addParamEl(nodeConfig, REFERENCE_CONFIG_VALUE_NODE, getEntityBase64Value(), true);

        referenceElement.appendChild(nodeConfig);
    }

    public static CustomKeyValueReference parseFromElement(
            @NotNull final ExternalReferenceFinder context,
            @NotNull final Element element
    ) throws InvalidDocumentFormatException {
        // make sure passed element is ours
        if (!REFERENCE_ROOT_NODE.equals(element.getNodeName())) {
            throw new InvalidDocumentFormatException("Expecting element of name \"" + REFERENCE_ROOT_NODE + "\"");
        }

        // todo: once we support different values for "KeyValueStoreName", add logic to handle it accordingly
        // getExactlyOneParamFromEl(element, REFERENCE_CONFIG_KEY_VALUE_STORE_NAME_NODE);

        // get the config node
        final Element configNode = DomUtils.findExactlyOneChildElementByName(element, REFERENCE_CONFIG_NODE);
        if (configNode == null) {
            throw new InvalidDocumentFormatException("Invalid policy xml, \"" + REFERENCE_CONFIG_NODE + "\" element is missing!");
        }

        return new CustomKeyValueReference(
                context,
                getExactlyOneRequiredParamFromEl(configNode, REFERENCE_CONFIG_KEY_NODE), // mandatory
                getExactlyOneRequiredParamFromEl(configNode, REFERENCE_CONFIG_PREFIX_NODE), // mandatory
                getExactlyOneRequiredParamFromEl(configNode, REFERENCE_CONFIG_VALUE_NODE), // mandatory
                getExactlyOneParamFromEl(element, REFERENCE_SERIALIZER_NODE) // optional
        );
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        final KeyValueStore keyValueStore = getKeyValueStore();
        try {
            // return found (true) or not-found (false)
            // based whether the external reference key is in the custom-key-value-store or not
            return keyValueStore.get(getEntityKey()) != null;
        } catch (KeyValueStoreException e) {
            throw new InvalidPolicyStreamException("Failed to extract CustomKeyValue for key \"" + getEntityKey() + "\"");
        }
    }

    @Override
    protected boolean localizeAssertion(@Nullable Assertion assertionToLocalize) {
        if (localizeType == null || localizeType == LocalizeAction.IGNORE) {
            return true;
        }

        final EntitiesResolver entitiesResolver = EntitiesResolver
                .builder()
                .keyValueStore(getFinder().getCustomKeyValueStore())
                .classNameToSerializerFunction(new Functions.Unary<CustomEntitySerializer, String>() {
                    @Override
                    public CustomEntitySerializer call(final String entitySerializerClassName) {
                        return getFinder().getCustomKeyValueEntitySerializer(entitySerializerClassName);
                    }
                })
                .build();
        for (final EntityHeader entityHeader : entitiesResolver.getEntitiesUsed(assertionToLocalize)) {
            if (EntityType.CUSTOM_KEY_VALUE_STORE.equals(entityHeader.getType()) && entityHeader.getName().equals(getEntityKey()) ) {
                if (localizeType == LocalizeAction.REPLACE) {
                    if (!getEntityKey().equals(replaceRefKey)) {
                        final EntityHeader newEntityHeader = new EntityHeader();
                        newEntityHeader.setName(replaceRefKey);
                        newEntityHeader.setType(EntityType.CUSTOM_KEY_VALUE_STORE);
                        entitiesResolver.replaceEntity(assertionToLocalize, entityHeader, newEntityHeader);

                        logger.info("The server custom key value of the imported assertion has been changed " +
                                "from \"" + getEntityKey() + "\" to \"" + replaceRefKey + "\"");
                    }
                } else if (localizeType == LocalizeAction.DELETE) {
                    logger.info("Deleted this assertion from the tree.");
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CustomKeyValueReference)) return false;

        final CustomKeyValueReference ref = (CustomKeyValueReference)obj;
        return getEntityKey().equals(ref.getEntityKey());
    }

    @Override
    public int hashCode() {
        return getEntityKey().hashCode();
    }

    /**
     * Utility function for getting KeyValueStore from the console context.
     *
     * @throws IllegalStateException when the KeyValueStore cannot be extracted from the console context.
     */
    @NotNull
    public KeyValueStore getKeyValueStore() {
        final ExternalReferenceFinder finder = getFinder();
        final KeyValueStore keyValueStore = (finder != null) ? finder.getCustomKeyValueStore() : null;
        if (keyValueStore == null) {
            throw new IllegalStateException("Failed to get custom KeyValueStore");
        }
        return keyValueStore;
    }

    /**
     * Utility function for finding one and only one child {@link Element} of a parent {@link Element}
     * with the specified name that is in the default namespace.
     *
     * @param parent    the {@link Element} in which to search for children. Must be non-null.
     * @param param     the name of the element to find. Must be non-null.
     * @return The value of the first matching child {@link Element}.
     * @throws TooManyChildElementsException if multiple matching child nodes are found
     */
    private static String getExactlyOneParamFromEl (
            @NotNull final Element parent, 
            @NotNull final String param
    ) throws TooManyChildElementsException {
        // get the config node
        try {
            final Element configNode = DomUtils.findExactlyOneChildElementByName(parent, param);
            final String val = DomUtils.getTextValue(configNode);
            if (val != null && val.length() > 0) return val;
        } catch (MissingRequiredElementException ignore) { /* nothing to do */ }
        return null;
    }

    /**
     * Utility function for finding one and only one optional child {@link Element} of a parent {@link Element}
     * with the specified name that is in the default namespace.
     *
     * @param parent    the {@link Element} in which to search for children. Must be non-null.
     * @param param     the name of the element to find. Must be non-null.
     * @return The value of the first matching child {@link Element}.
     * @throws TooManyChildElementsException if multiple matching child nodes are found
     * @throws MissingRequiredElementException if no matching child node is found
     * @throws InvalidDocumentFormatException if child node value cannot be extracted
     */
    private static String getExactlyOneRequiredParamFromEl (
            @NotNull final Element parent,
            @NotNull final String param
    ) throws InvalidDocumentFormatException {
        final Element configNode = DomUtils.findExactlyOneChildElementByName(parent, param);
        final String val = DomUtils.getTextValue(configNode);
        if (val != null && val.length() > 0) return val;
        throw new InvalidDocumentFormatException(parent.getLocalName() + " missing required element " + param);
    }
}
