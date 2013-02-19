package com.l7tech.policy.assertion;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Controls whether a target message should be stashed or streamed.  This can be used:
 * <ul>
 *     <li>to immediately stash a message, so policy authors can control the timing of the resulting latency hit</li>
 *     <li>to indicate that a message <em>must</em> be processed in streaming mode, and that any attempt to stash it in the future should fail</li>
 * </ul>
 */
public class MessageBufferingAssertion extends MessageTargetableAssertion {
    private boolean alwaysBuffer = false;
    private boolean neverBuffer = true;

    public MessageBufferingAssertion() {
        super(TargetMessageType.REQUEST);
    }

    /**
     * @return true if the assertion should immediately stash the target message body, and should fail if it has already been destructively read.
     */
    public boolean isAlwaysBuffer() {
        return alwaysBuffer;
    }

    public void setAlwaysBuffer(boolean alwaysBuffer) {
        this.alwaysBuffer = alwaysBuffer;
    }

    /**
     * @return true if the assertion should prevent any future attempt to stash the target message body, and should fail if it has already been stashed.
     */
    public boolean isNeverBuffer() {
        return neverBuffer;
    }

    public void setNeverBuffer(boolean neverBuffer) {
        this.neverBuffer = neverBuffer;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"misc"});
        meta.put(DESCRIPTION, "Change message streaming status.");
        meta.put(SHORT_NAME, "Configure Message Streaming");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/MessageBuffering-16x16.gif");
        meta.put(POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<MessageBufferingAssertion>() {
            @Override
            public String getAssertionName(MessageBufferingAssertion assertion, boolean decorate) {
                final String baseName = assertion.meta().get(SHORT_NAME);
                if (!decorate)
                    return baseName;
                String extra = assertion.isAlwaysBuffer() ? ": buffer immediately" : ": enable streaming";
                return AssertionUtils.decorateName(assertion, baseName + extra);
            }
        });
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.MessageBufferingAssertionPropertiesDialog");

        return meta;
    }

}
