package com.l7tech.gateway.common.custom;

import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;

import java.io.Serializable;
import java.util.Collection;

/**
 * The interface <code>CustomAssertionsRegistrar</code> implementations
 * provide access to the custom assertions that have been registered with
 * the runtime.
 */
@Administrative
public interface CustomAssertionsRegistrar {

    /**
     * Get the class bytes for a CustomAssertion class.
     *
     * @param className The class to load
     * @return The class data or null
     */
    @Administrative(licensed=false)
    byte[] getAssertionClass(String className);

    /**
     * Get the bytes for a CustomAssertion class or resource.
     *
     * @param path  the path of the resource to load.  Required.
     * @return the resource bytes, or null if not found
     */
    @Administrative(licensed=false)
    byte[] getAssertionResourceBytes(String path);

    /**
     * Get resource data for custom / modular resources.
     *
     * @return the resource data, or null
     */
    @Administrative(licensed=false)
    AssertionResourceData getAssertionResourceData( final String name );

    /**
     * Get the known registered assertion for a given class name.
     * @param customAssertionClassName the custom assertion class name
     * @return the custom assertion holder
     */
    @Administrative(licensed=false)
    CustomAssertionHolder getAssertion(final String customAssertionClassName);

    /**
     * @return the list of all assertions known to the runtime
     */
    @Administrative(licensed=false)
    Collection getAssertions();

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     */
    @Administrative(licensed=false)
    Collection getAssertions( Category c);

    /**
     * Checks if there is a CustomAssertion registered which either implements the
     * {@link com.l7tech.policy.assertion.ext.CustomCredentialSource CustomCredentialSource} interface
     * or is placed into {@link Category#ACCESS_CONTROL ACCESS_CONTROL} category.
     *
     * @return true if there is a CustomAssertion registered which is credential source, false otherwise.
     */
    @Administrative(licensed=false)
    boolean hasCustomCredentialSource();

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or <b>null<b>.
     * Note that this method may not be invoked from management console. The descriptor
     * may not de-serialize into the ssm environment since int contains server classes.
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    @Administrative(licensed=false)
    CustomAssertionDescriptor getDescriptor(Class a);

    /**
     * Return the <code>CustomAssertionUI</code> class for a given assertion or <b>null<b>.
     *
     * @param assertionClassName the assertion class name
     * @return the custom assertion UI class or <b>null</b>
     */
    @Administrative(licensed=false)
    CustomAssertionUI getUI(String assertionClassName);

    /**
     * Return the <code>CustomTaskActionUI</code> class for a given assertion or <b>null<b>.
     *
     * @param assertionClassName the assertion class name
     * @return the task action UI class or <b>null</b>
     */
    @Administrative(licensed=false)
    CustomTaskActionUI getTaskActionUI(String assertionClassName);

    /**
     * Assertion data is used to wrap one or more resources.
     */
    static final class AssertionResourceData implements Serializable {
        private final String resourceName;
        private final boolean zip;
        private final byte[] data;

        public AssertionResourceData( final String resourceName,
                                      final boolean zip,
                                      final byte[] data ) {
            this.resourceName = resourceName;
            this.zip = zip;
            this.data = data;
        }

        /**
         * Get the resource name.
         *
         * <p>This will be the class/resource name for a single resource or the
         * package name for a ZIP.</p>
         *
         * @return The resource name.
         */
        public String getResourceName() {
            return resourceName;
        }

        /**
         * Is the data a ZIP or single resource.
         *
         * @return True if a ZIP
         */
        public boolean isZip() {
            return zip;
        }

        /**
         * Get the data.
         *
         * @return The data bytes (never null)
         */
        public byte[] getData() {
            return data;
        }
    }
}
