package com.l7tech.server.policy.module;

import com.l7tech.util.Either;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when an assertion module is unregistered.
 */
public class AssertionModuleUnregistrationEvent extends ApplicationEvent {
    /**
     * Assertion module that was just unregistered.<br/>
     * Can either be {@link ModularAssertionModule} or {@link CustomAssertionModule}.
     */
    @NotNull
    private final BaseAssertionModule module;

    /**
     * Create an assertion module un-registration event.
     *
     * @param source the source of the event; usually not relevant.
     * @param module the module that was just unregistered.  Must not be null.
     */
    public AssertionModuleUnregistrationEvent(Object source, @NotNull BaseAssertionModule module) {
        super(source);
        this.module = module;
    }

    /**
     *
     * @return the assertion module that was just unregistered, which can either be {@link ModularAssertionModule} or {@link CustomAssertionModule}.
     * @throws IllegalStateException if {@link #module} is not of type ModularAssertionModule or CustomAssertionModule.
     */
    @NotNull
    public Either<ModularAssertionModule, CustomAssertionModule> getModule() {
        if (module instanceof ModularAssertionModule) {
            return Either.left((ModularAssertionModule) module);
        } else if (module instanceof CustomAssertionModule) {
            return Either.right((CustomAssertionModule) module);
        }
        throw new IllegalStateException("Unsupported module type. Type is \"" + module.getClass().getSimpleName() +
                "\", expected type should either be ModularAssertionModule or CustomAssertionModule");
    }

    /**
     *
     * @return the {@code ClassLoader} associated with this event {@link #module}.
     */
    public ClassLoader getModuleClassLoader() {
        return module.getModuleClassLoader();
    }
}
