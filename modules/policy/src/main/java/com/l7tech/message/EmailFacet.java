package com.l7tech.message;

/**
 * Message facet for messages received by an email listener.
 */
public class EmailFacet extends MessageFacet {
    private final EmailKnob emailKnob;

    /**
     * @param message  the Message that owns this aspect
     * @param delegate the delegate to chain to or null if there isn't one.  Can't be changed after creation.
     */
    public EmailFacet(Message message, MessageFacet delegate, EmailKnob emailKnob) {
        super(message, delegate);
        this.emailKnob = emailKnob;
    }

    public MessageKnob getKnob(Class c) {
        if (c == EmailKnob.class || c == HasSoapAction.class || c == HasServiceGoid.class) {
            return emailKnob;
        }
        return super.getKnob(c);
    }
}
