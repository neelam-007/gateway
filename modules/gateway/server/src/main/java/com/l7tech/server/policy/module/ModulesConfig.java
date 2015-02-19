package com.l7tech.server.policy.module;

import java.io.File;
import java.util.List;

/**
 * Provides needed setting for the modules scanner.
 */
public interface ModulesConfig {

    /**
     * Determines whether either custom or modular assertions have license installed on the Gateway.
     *
     * @return true if the modules feature-set have been enabled on the SSG side, false otherwise.
     */
    boolean isFeatureEnabled();

    /**
     * Determines whether modules scanning is enabled on the Gateway.
     *
     * @return true if modules scanning is enabled, false otherwise.
     */
    boolean isScanningEnabled();

    /**
     * @return the modules configured folder.
     */
    File getModuleDir();

    /**
     * @return Read-only {@link List list} containing allowed modules file extension(s) (e.g. jar, aar etc.)
     */
    List<String> getModulesExt();

    /**
     * Get disabled modules extension or suffix.
     * <p/>
     * This is a workaround for mandatory file locking, which prevents modules from being unloaded on Windows platform.<br/>
     * For example, with <tt>ModulesExt</tt> of <code>".foo"</code> and <tt>DisabledSuffix</tt> of ".bar",
     * by putting a file with name "module1.foo.bar" in the modules folder, will unload "module1.foo", as if the file was deleted.
     * <p/>
     *
     * @return <code>null</code> or empty-string to disable the workaround, or to enable it, a string containing the extension to match.
     */
    String getDisabledSuffix();
}
