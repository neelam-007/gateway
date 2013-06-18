package com.l7tech.policy.assertion.ext.cei;

import com.l7tech.policy.assertion.ext.ServiceFinder;

/**
 * Describes an available implementation of an custom extension interface.  These are interfaces that the custom assertion can register
 * on the Gateway, and the corresponding Policy Manager code can invoke server side methods of these interfaces via the Console Context.
 * <p/>
 * For example, a Custom Assertion connector may have its GUI classes invoke methods on an extension interface in order to test the
 * configuration settings to an external system.
 * <p/>
 * To avoid serialization, class loader, security manager problems with third-party classes, we limit custom assertion extension interfaces
 * for now allow only passing and returning of primitives and String data types (arrays are okay).
 */
public class CustomExtensionInterfaceBinding<T> {
    private static ServiceFinder serviceFinder;

    private final Class<T> interfaceClass;
    private final T implementationObject;

    /**
     * Gets the service finder.
     *
     * @return the service finder
     */
    public static ServiceFinder getServiceFinder() {
        return serviceFinder;
    }

    /**
     * Sets the service finder.
     *
     * @param serviceFinder the service finder
     */
    public static void setServiceFinder(ServiceFinder serviceFinder) {
        CustomExtensionInterfaceBinding.serviceFinder = serviceFinder;
    }

    /**
     * Create a description of an available implementation of an extension interface.
     *
     * @param interfaceClass  a custom extension interface this object implements.  Required.
     * @param implementationObject an object which implements the extension interface.  Required.
     * @throws NullPointerException if interfaceClass or implementationObject is null
     * @throws ClassCastException if implementationObject is not a subclass of interfaceClass
     */
    public CustomExtensionInterfaceBinding(Class<T> interfaceClass, T implementationObject) {
        //noinspection ConstantConditions
        if (interfaceClass == null) throw new NullPointerException("interfaceClass");

        //noinspection ConstantConditions
        if (implementationObject == null) throw new NullPointerException("implementationObject");

        if (!interfaceClass.isAssignableFrom(implementationObject.getClass()))
            throw new ClassCastException("implementationObject does not implement " + interfaceClass);

        this.interfaceClass = interfaceClass;
        this.implementationObject = implementationObject;
    }

    /**
     * @return the interface class for this binding.  Never null.
     */
    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    /**
     * @return the implementation object for this binding.  Never null.
     */
    public T getImplementationObject() {
        return implementationObject;
    }
}
