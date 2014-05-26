package com.l7tech.external.assertions.jsondocumentstructure;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;

/**
 * Defines limits on the document structure and content of a target JSON message.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class JsonDocumentStructureAssertion extends MessageTargetableAssertion {
    private static final String baseName = "Protect Against JSON Document Structure Threats";
    private static final String META_INITIALIZED =
            JsonDocumentStructureAssertion.class.getName() + ".metadataInitialized";

    private static final long DEFAULT_MAX_CONTAINER_DEPTH = 3;
    private static final long DEFAULT_MAX_OBJECT_ENTRY_COUNT = 2048;
    private static final long DEFAULT_MAX_ARRAY_ENTRY_COUNT = 2048;
    private static final long DEFAULT_MAX_ENTRY_NAME_LENGTH = 128;
    private static final long DEFAULT_MAX_STRING_VALUE_LENGTH = 16384;

    private long maxContainerDepth = DEFAULT_MAX_CONTAINER_DEPTH;
    private long maxObjectEntryCount = DEFAULT_MAX_OBJECT_ENTRY_COUNT;
    private long maxArrayEntryCount = DEFAULT_MAX_ARRAY_ENTRY_COUNT;
    private long maxEntryNameLength = DEFAULT_MAX_ENTRY_NAME_LENGTH;
    private long maxStringValueLength = DEFAULT_MAX_STRING_VALUE_LENGTH;

    private boolean checkContainerDepth = true;
    private boolean checkObjectEntryCount = false;
    private boolean checkArrayEntryCount = false;
    private boolean checkEntryNameLength = false;
    private boolean checkStringValueLength = false;

    public long getMaxContainerDepth() {
        return maxContainerDepth;
    }

    public void setMaxContainerDepth(long maxContainerDepth) {
        this.maxContainerDepth = maxContainerDepth;
    }

    public long getMaxObjectEntryCount() {
        return maxObjectEntryCount;
    }

    public void setMaxObjectEntryCount(long maxObjectEntryCount) {
        this.maxObjectEntryCount = maxObjectEntryCount;
    }

    public long getMaxArrayEntryCount() {
        return maxArrayEntryCount;
    }

    public void setMaxArrayEntryCount(long maxArrayEntryCount) {
        this.maxArrayEntryCount = maxArrayEntryCount;
    }

    public long getMaxEntryNameLength() {
        return maxEntryNameLength;
    }

    public void setMaxEntryNameLength(long maxEntryNameLength) {
        this.maxEntryNameLength = maxEntryNameLength;
    }

    public long getMaxStringValueLength() {
        return maxStringValueLength;
    }

    public void setMaxStringValueLength(long maxStringValueLength) {
        this.maxStringValueLength = maxStringValueLength;
    }

    public boolean isCheckContainerDepth() {
        return checkContainerDepth;
    }

    public void setCheckContainerDepth(boolean checkContainerDepth) {
        this.checkContainerDepth = checkContainerDepth;
    }

    public boolean isCheckObjectEntryCount() {
        return checkObjectEntryCount;
    }

    public void setCheckObjectEntryCount(boolean checkObjectEntryCount) {
        this.checkObjectEntryCount = checkObjectEntryCount;
    }

    public boolean isCheckArrayEntryCount() {
        return checkArrayEntryCount;
    }

    public void setCheckArrayEntryCount(boolean checkArrayEntryCount) {
        this.checkArrayEntryCount = checkArrayEntryCount;
    }

    public boolean isCheckEntryNameLength() {
        return checkEntryNameLength;
    }

    public void setCheckEntryNameLength(boolean checkEntryNameLength) {
        this.checkEntryNameLength = checkEntryNameLength;
    }

    public boolean isCheckStringValueLength() {
        return checkStringValueLength;
    }

    public void setCheckStringValueLength(boolean checkStringValueLength) {
        this.checkStringValueLength = checkStringValueLength;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION,
                "Enable protection against JSON document structure threats such as oversized arrays and overdeep nesting.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "threatProtection" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON,
                "com/l7tech/external/assertions/jsondocumentstructure/console/resources/JsonDocumentStructure.png");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME,
                "com.l7tech.external.assertions.jsondocumentstructure.console.JsonDocumentStructurePropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "JSON Document Structure Threat Protection Properties");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }
}
