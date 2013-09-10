package com.l7tech.policy.assertion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes an available implementation of an admin extension interface.  These are interfaces that .aar files
 * can register on the Gateway, and the corresponding SSM client .aar file code can invoke server side methods
 * of these interfaces via the SSM Registry.
 * <p/>
 * For exmple, a modular transport (that doesn't fit into
 * the SsgConnector framework) may have its GUI classes invoke methods on an extension interface in order
 * to configure the transport.
 * <p/>
 * If a single object implements more than one extension interface then more than one binding will be required
 * to describe it.
 * <p/>
 * More than one object may be registered on the Gateway at the same time for a given extension interface
 * classname as long as each binding provides a unique instance identifier string.  The instance identifier string
 * may be left null if the interface classname is specific to the .aar file and it never tries to register more than
 * one implementation.
 */
public class ExtensionInterfaceBinding<T> {
    private final @NotNull Class<T> interfaceClass;
    private final @Nullable String instanceIdentifier;
    private final @NotNull T implementationObject;

    /**
     * Create a description of an available implementation of an extension interface.
     *
     * @param interfaceClass  an extension interface this object implements (an object may implement more than one, requiring more than one binding).  Required.
     * @param instanceIdentifier an opaque instance identifier used to dispatch methods when more than one implementation is registered for some extension interface.  May be null.
     * @param implementationObject an object which implements the extension interface.  Required.
     * @throws NullPointerException if interfaceClass or implementationObject is null
     * @throws ClassCastException if implementationObject is not a subclass of interfaceClass
     */
    public ExtensionInterfaceBinding(@NotNull Class<T> interfaceClass, @Nullable String instanceIdentifier, @NotNull T implementationObject) {
        //noinspection ConstantConditions
        if (interfaceClass == null) throw new NullPointerException("interfaceClass");

        //noinspection ConstantConditions
        if (implementationObject == null) throw new NullPointerException("implementationObject");

        if (!interfaceClass.isAssignableFrom(implementationObject.getClass()))
            throw new ClassCastException("implementationObject does not implement " + interfaceClass);

        this.interfaceClass = interfaceClass;
        this.instanceIdentifier = instanceIdentifier;
        this.implementationObject = implementationObject;
    }

    /**
     * @return the interface class for this binding.  Never null.
     */
    @NotNull
    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    /**
     * @return an instance identifier to distinguish between multiple implementors of a single extension interface, or null if this will never happen.
     */
    @Nullable
    public String getInstanceIdentifier() {
        return instanceIdentifier;
    }

    @NotNull
    public T getImplementationObject() {
        return implementationObject;
    }
}
