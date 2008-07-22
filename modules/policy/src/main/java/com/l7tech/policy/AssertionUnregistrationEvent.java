package com.l7tech.policy;

import org.springframework.context.ApplicationEvent;
import com.l7tech.policy.assertion.Assertion;

/**
 * Event fired when an assertion is unregistered.
 */
public class AssertionUnregistrationEvent extends ApplicationEvent {
    private final Assertion prototypeInstance;

    /**
     * Create an assertion unregistration event.
     *
     * @param source the source of the event; not usually of interest.
     * @param prototypeInstance a prototype instance of the assertion that was just unregistered, or null
     *                          if no prototype is available for this no-longer-registered assertion.
     *                          If provided, assertionClassname will be ignored.
     */
    public AssertionUnregistrationEvent(Object source, Assertion prototypeInstance) {
        super(source);
        if (prototypeInstance == null)
            throw new NullPointerException("prototypeInstance must not be null");
        this.prototypeInstance = prototypeInstance;
    }

    /**
     * @return the name of the concrete Assertion subclass that was just unregistered.  Never null.
     */
    public String getAssertionClassname() {
        return prototypeInstance.getClass().getName();
    }

    /**
     * @return a prototype instance of the concrete Assertion subclass that was just unregistered, or null
     *         if no prototype instance is (any longer?) available.
     */
    public Assertion getPrototypeInstance() {
        return prototypeInstance;
    }
}
