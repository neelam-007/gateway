package com.l7tech.server.policy.module;

import com.l7tech.util.Either;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when an assertion module is registered.
 */
public class AssertionModuleRegistrationEvent extends ApplicationEvent {
    /**
     * Assertion module that was just registered.<br/>
     * Can either be {@link ModularAssertionModule} or {@link CustomAssertionModule}.
     */
    @NotNull
    private final BaseAssertionModule module;

    /**
     * Create an assertion module registration event.
     *
     * @param source  the event source; usually not relevant.
     * @param module  the module that was just registered.
     */
    public AssertionModuleRegistrationEvent(Object source, @NotNull BaseAssertionModule module) {
        super(source);
        this.module = module;
    }

    /**
     *
     * @return the assertion module that was just registered, which can either be {@link ModularAssertionModule} or {@link CustomAssertionModule}.
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
