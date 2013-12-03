package com.l7tech.policy.assertion.ext;

/**
 * Implement this interface from your custom assertion class to receive notifications when the assertion
 * module jar is about to be load or unload by the class loader.
 */
public interface CustomLifecycleListener {

    /**
     * Called when the module associated with the assertion is about to be load.<br/>
     * This is a good place for startup initialization.
     *
     * @throws CustomLoaderException throw this exception to indicate that there is an error during assertion loading.
     * Indicates that this custom assertion version fails to load, therefore SSG will try to reload the assertion once
     * a new version is dropped in the modules folder.
     */
    void onLoad() throws CustomLoaderException;

    /**
     * Called when the module associated with the assertion is about to be unload.<br/>
     * This is a place where all resources allocated for the custom assertion can be freed.
     */
    void onUnload();
}
