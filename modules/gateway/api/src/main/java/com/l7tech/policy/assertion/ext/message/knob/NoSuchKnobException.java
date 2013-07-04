package com.l7tech.policy.assertion.ext.message.knob;

/**
 * Thrown when the requested knob is not found in the message.
 */
public class NoSuchKnobException extends Exception {
    private static final long serialVersionUID = 2813630704259256948L;

    private final Class knobClass;

    public NoSuchKnobException(final Class knobClass) {
        this(knobClass, "Message knob for representation class [" + (knobClass == null ? "<NULL>" : knobClass.getSimpleName()) + "] doesn't exists.");
    }
    public NoSuchKnobException(final Class knobClass, final String message) {
        super(message);
        this.knobClass = knobClass;
    }

    public Class getKnobClass() {
        return knobClass;
    }
}
