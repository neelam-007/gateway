package com.l7tech.policy;

import org.springframework.context.ApplicationEvent;
import com.l7tech.policy.assertion.Assertion;

/**
 * Event fired when an assertion is registered.
 * @see AssertionUnregistrationEvent
 */
public class AssertionRegistrationEvent extends ApplicationEvent {
    private final Assertion prototypeInstance;

    /**
     * Create an AssertionRegistrationEvent.
     *
     * @param source  the source of this event; not usually of interest.
     * @param prototypeInstance  a prototype instance of the assertion that was just registered.  Must not be null.
     */
    public AssertionRegistrationEvent(Object source, Assertion prototypeInstance) {
        super(source);
        if (prototypeInstance == null) throw new NullPointerException();
        this.prototypeInstance = prototypeInstance;
    }

    /**
     * @return the name of the concrete Assertion subclass that was just registered.  Never null.
     */
    public String getAssertionClassname() {
        return prototypeInstance.getClass().getName();
    }

    /**
     * @return a prototype instance of the concrete Assertion subclass that was just registered.  Never null.
     */
    public Assertion getPrototypeInstance() {
        return prototypeInstance;
    }
}
