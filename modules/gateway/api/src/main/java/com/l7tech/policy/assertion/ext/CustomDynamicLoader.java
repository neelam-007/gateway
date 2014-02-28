package com.l7tech.policy.assertion.ext;

/**
 * Implement this interface to indicate that this custom assertion can be dynamically loaded, reloaded or unloaded,
 * while SSG is still running.
 * <p/>
 * It's very important to properly implement {@link #onUnload(ServiceFinder)} method to free all resources allocated by the custom assertion<br/>
 * It's the responsibility of the custom assertion to guarantee that all allocated resources are freed accordingly,
 * once the method is executed, otherwise there is a potential risk of memory leaks.
 */
public interface CustomDynamicLoader extends CustomLifecycleListener {
}
