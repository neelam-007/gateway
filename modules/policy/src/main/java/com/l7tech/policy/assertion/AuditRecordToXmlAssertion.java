package com.l7tech.policy.assertion;

/**
 *
 */
public class AuditRecordToXmlAssertion extends MessageTargetableAssertion {
    public AuditRecordToXmlAssertion() {
        super(TargetMessageType.REQUEST);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Audit Record to XML");
        meta.put(AssertionMetadata.LONG_NAME, "Convert Audit Record to XML");
        meta.put(AssertionMetadata.DESCRIPTION, "Only useful inside an audit sink policy.<p>Quickly converts an entire audit record to XML, replacing the contents of the targeted Message.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "audit" });
        return meta;
    }
}
