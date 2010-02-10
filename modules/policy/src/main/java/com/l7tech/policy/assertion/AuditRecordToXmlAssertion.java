package com.l7tech.policy.assertion;

/**
 *
 */
@SuppressWarnings({ "serial" })
public class AuditRecordToXmlAssertion extends MessageTargetableAssertion {
    public AuditRecordToXmlAssertion() {
        super(TargetMessageType.REQUEST);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Convert Audit Record to XML");
        meta.put(AssertionMetadata.LONG_NAME, "Convert Audit Record to XML");
        meta.put(AssertionMetadata.DESCRIPTION, "Quickly converts an entire audit record to XML, replacing the contents of the targeted Message.<p><b>NOTE:</b> This assertion only works inside an audit sink policy.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "internalAssertions" });
        return meta;
    }
}
