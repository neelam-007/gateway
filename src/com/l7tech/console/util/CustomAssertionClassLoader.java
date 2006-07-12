package com.l7tech.console.util;

import java.rmi.RemoteException;

import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;

/**
 * ClassLoader for loading CustomAssertion classes from the connected SSG.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class CustomAssertionClassLoader extends ClassLoader {

    //- PUBLIC

    /**
     * Create a new CustomAssertionClassLoader.
     */
    public CustomAssertionClassLoader() {
        super();
    }

    /**
     * Create a new CustomAssertionClassLoader.
     *
     * @param parentLoader The parent class loader
     */
    public CustomAssertionClassLoader(ClassLoader parentLoader) {
        super(parentLoader);
    }

    //- PROTECTED

    /**
     * Find the class from the SSG.
     *
     * @param name The class name
     * @return The class
     * @throws ClassNotFoundException if not available from the attached SSG
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = null;

        try {
            Registry registry = Registry.getDefault();
            if (registry != null) {
                CustomAssertionsRegistrar customAssertionsRegistrar = registry.getCustomAssertionsRegistrar();
                if (customAssertionsRegistrar != null) {
                    byte[] classData = customAssertionsRegistrar.getAssertionClass(name);
                    if (classData != null)
                        clazz = defineClass(name, classData, 0, classData.length);
                }
            }
        }
        catch(RemoteException re) {
            throw new ClassNotFoundException(name, re);
        }

        if (clazz == null) throw new ClassNotFoundException(name);

        return clazz;
    }
}
