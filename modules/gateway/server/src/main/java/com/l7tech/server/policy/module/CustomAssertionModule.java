package com.l7tech.server.policy.module;

import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.ext.CustomDynamicLoader;
import com.l7tech.policy.assertion.ext.CustomLifecycleListener;
import com.l7tech.policy.assertion.ext.CustomLoaderException;
import com.l7tech.policy.assertion.ext.ServiceFinder;
import com.l7tech.util.ExceptionUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A type helper class for custom assertions modules.
 */
public class CustomAssertionModule extends BaseAssertionModule<CustomAssertionClassLoader> implements Closeable {

    private static final Logger logger = Logger.getLogger(CustomAssertionModule.class.getName());

    /**
     * I'm not sure whether there are supposed to be more than one assertions per module,
     * or whether we are going to support multiple assertions per module,
     * in any case the scanner will support it.
     */
    private final Set<CustomAssertionDescriptor> descriptors;

    /**
     * Custom Assertion {@link ServiceFinder} for locating Layer 7 API Services.<br/>
     * For available services see the Layer 7 API documentation.
     */
    private final ServiceFinder serviceFinder;

    /**
     * @return read-only collection of assertion descriptors
     */
    public Collection<CustomAssertionDescriptor> getDescriptors() {
        return Collections.unmodifiableCollection(descriptors);
    }

    /**
     * Default constructor.
     *
     * @param moduleName      the module filename.
     * @param modifiedTime    the module last modified timestamp.
     * @param jarFileSha1     the module content SHA-1 checksum.
     * @param classLoader     the module class loader.
     * @param descriptors     a set of descriptors associated with this module.
     * @param serviceFinder   the service finder for locating Layer 7 API Services available for assertions.
     */
    public CustomAssertionModule(final String moduleName,
                                 final long modifiedTime,
                                 final String jarFileSha1,
                                 final CustomAssertionClassLoader classLoader,
                                 final Set<CustomAssertionDescriptor> descriptors,
                                 final ServiceFinder serviceFinder) {
        super(moduleName, modifiedTime, jarFileSha1, classLoader);

        this.descriptors = descriptors;

        if (serviceFinder == null) {
            throw new IllegalArgumentException("serviceFinder cannot be null");
        }
        this.serviceFinder = serviceFinder;
    }

    /**
     * Determines if the this module's custom assertions implement {@link CustomDynamicLoader} interface.<br/>
     * In order for this module to be dynamically loadable, all custom assertions must implement this interface.
     *
     * @return <code>true</code> if the module's custom assertions implement {@link CustomDynamicLoader} interface, <code>false</code> otherwise.
     */
    public boolean isCustomDynamicLoader() {
        boolean found = false;

        // loop through all descriptors
        for (CustomAssertionDescriptor descriptor : descriptors) {
            // get the custom assertion class from the descriptor
            final Class assertionClass = descriptor.getAssertion();

            // if the assertion is a legacy assertion we'll disallow loading
            if (!CustomDynamicLoader.class.isAssignableFrom(assertionClass)) {
                return false;
            } else {
                found = true;
            }
        }

        return found;
    }

    /**
     * Notify all assertions that it's module is about to be loaded.<br/>
     * Will try to execute {@link com.l7tech.policy.assertion.ext.CustomLifecycleListener#onLoad(com.l7tech.policy.assertion.ext.ServiceFinder)} method.<br/>
     *
     * @throws CustomLoaderException if an error happens during loading process
     * @see CustomLifecycleListener
     */
    public void onAssertionLoad() throws CustomLoaderException {
        // loop through all descriptors
        for (CustomAssertionDescriptor descriptor : descriptors) {
            // get the custom assertion class from the descriptor
            final Class assertionClass = descriptor.getAssertion();

            // check if the assertion is implementing CustomLifecycleListener interface
            if (CustomLifecycleListener.class.isAssignableFrom(assertionClass)) {
                // try to execute onLoad method
                try {
                    final CustomLifecycleListener assertion = (CustomLifecycleListener)assertionClass.newInstance();
                    assertion.onLoad(serviceFinder);
                } catch (InstantiationException e) {
                    logger.log(Level.SEVERE, "Custom assertion module \"" + getName() + "\": exception while instantiating class " + assertionClass.getName() + " of module load: " + ExceptionUtils.getMessage(e), e);
                } catch (IllegalAccessException e) {
                    logger.log(Level.SEVERE, "Custom assertion module \"" + getName() + "\": exception while notifying class " + assertionClass.getName() + " of module load: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
    }

    /**
     * Notify all assertions that it's module is about to be unloaded.<br/>
     * Will try to execute {@link com.l7tech.policy.assertion.ext.CustomLifecycleListener#onUnload(com.l7tech.policy.assertion.ext.ServiceFinder)} method.<br/>
     *
     * @see CustomLifecycleListener
     */
    public void onAssertionUnload() {
        // loop through all descriptors
        for (CustomAssertionDescriptor descriptor : descriptors) {
            // get the custom assertion class from the descriptor
            final Class assertionClass = descriptor.getAssertion();

            // check if the assertion is implementing CustomLifecycleListener interface
            if (CustomLifecycleListener.class.isAssignableFrom(assertionClass)) {
                try {
                    final CustomLifecycleListener assertion = (CustomLifecycleListener)assertionClass.newInstance();
                    assertion.onUnload(serviceFinder);
                } catch (InstantiationException e) {
                    logger.log(Level.SEVERE, "Custom assertion module \"" + getName() + "\": exception while instantiating class " + assertionClass.getName() + " of module unload: " + ExceptionUtils.getMessage(e), e);
                } catch (IllegalAccessException e) {
                    logger.log(Level.SEVERE, "Custom assertion module \"" + getName() + "\": exception while notifying class " + assertionClass.getName() + " of module unload: " + ExceptionUtils.getMessage(e), e);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "Custom assertion module \"" + getName() + "\": unhandled exception while notifying class " + assertionClass.getName() + " of module unload: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        // unmodifiable
        //descriptors.clear();

        // unload the assertion
        onAssertionUnload();

        classLoader.close();
    }
}
