package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import org.springframework.context.ApplicationContext;

/**
 * SftpPollingListenerConfig encapsulates all information necessary for an SFTP client to use a server.
 */
public class SftpPollingListenerConfig {

    /** Separator used in the listener DisplayName */
    private static final String SEPARATOR = "/";

    /** Configured listener resource attributes */
    private final SftpPollingListenerResource sftpPollingListenerResource;

    /** Spring application context */
    private final ApplicationContext appContext;

    /** String denoting the display name for an listener instance */
    private final String displayName;

    /**
     * Constructor using overrides.
     *
     * @param sftpPollingListenerResource configured SFTP polling listener connection attributes
     * @param appContext spring application context
     */
    public SftpPollingListenerConfig(final SftpPollingListenerResource sftpPollingListenerResource,
                                     final ApplicationContext appContext)
    {
        this.sftpPollingListenerResource = sftpPollingListenerResource;
        this.appContext = appContext;
        StringBuilder sb = new StringBuilder(this.sftpPollingListenerResource.getName()).append(SEPARATOR);
        sb.append(this.sftpPollingListenerResource.getHostname()).append(SEPARATOR).append(this.sftpPollingListenerResource.getPort());
        this.displayName = sb.toString();
    }

    /* Getters */
    public ApplicationContext getApplicationContext() {
        return appContext;
    }

    /**
     * Get the listener configuration
     *
     * @return The listener configuration
     */
    public SftpPollingListenerResource getSftpPollingListenerResource() {
        return sftpPollingListenerResource;
    }

    /**
     * Returns the listener display name.
     *
     * @return String for the listener display name
     */
    public String getDisplayName() {
        return displayName;
    }
}