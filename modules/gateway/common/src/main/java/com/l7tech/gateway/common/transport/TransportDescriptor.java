package com.l7tech.gateway.common.transport;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

/**
 * Holds information describing a protocol scheme offered by an available incoming message transport.
 */
public class TransportDescriptor implements Serializable {
    private static final long serialVersionUID = -8284391304992363054L;

    private String scheme = null;
    private String displayName = null;
    private String description = null;

    private boolean usesTls = false;
    private boolean httpBased = false;
    private boolean ftpBased = false;
    private boolean requiresHardwiredServiceResolutionForNonXml = false;
    private boolean requiresHardwiredServiceResolutionAlways = false;
    private boolean supportsHardwiredServiceResolution = false;
    private boolean requiresSpecifiedContentType = false;
    private boolean supportsSpecifiedContentType = false;
    private boolean supportsPrivateThreadPool = false;

    private String customPropertiesPanelClassname = null;
    private String modularAssertionClassname = null;

    private Set<SsgConnector.Endpoint> supportedEndpoints = EnumSet.of(SsgConnector.Endpoint.MESSAGE_INPUT);

    public TransportDescriptor() {
    }

    public TransportDescriptor(String scheme, boolean usesTls) {
        this.scheme = scheme;
        this.usesTls = usesTls;
    }

    /**
     * @return the protocol scheme for this custom transport protocol, ie "l7.raw.tcp", or null if this transport
     *         cannot be configured as an SsgConnector using the Manage Listen Ports dialog.
     */
    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    /**
     * @return display name of this protocol in the GUI, or null if same as scheme.
     */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return more lengthy description of protocol to show in GUI, or null if none available.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return true if the TLS Settings tab should be enabled for listen ports that use this protocol.
     */
    public boolean isUsesTls() {
        return usesTls;
    }

    public void setUsesTls(boolean usesTls) {
        this.usesTls = usesTls;
    }

    /**
     * @return true if the Hardwired Service Resolution checkbox should be enabled for listen ports that use this protocol.
     */
    public boolean isSupportsHardwiredServiceResolution() {
        return supportsHardwiredServiceResolution;
    }

    public void setSupportsHardwiredServiceResolution(boolean supportsHardwiredServiceResolution) {
        this.supportsHardwiredServiceResolution = supportsHardwiredServiceResolution;
    }

    /**
     * @return true if the Hardwired Service Resolution checkbox must always be set for listen ports that use this protocol
     *              and that have not specified an XML-based overridden content type.
     */
    public boolean isRequiresHardwiredServiceResolutionForNonXml() {
        return requiresHardwiredServiceResolutionForNonXml;
    }

    public void setRequiresHardwiredServiceResolutionForNonXml(boolean requiresHardwiredServiceResolutionForNonXml) {
        this.requiresHardwiredServiceResolutionForNonXml = requiresHardwiredServiceResolutionForNonXml;
    }

    /**
     * @return true if the Hardwired Service Resolution checkbox must always be set for listen ports that use this protocol,
     *              even if they have specified an XML-based overridden content type.
     */
    public boolean isRequiresHardwiredServiceResolutionAlways() {
        return requiresHardwiredServiceResolutionAlways;
    }

    public void setRequiresHardwiredServiceResolutionAlways(boolean requiresHardwiredServiceResolutionAlways) {
        this.requiresHardwiredServiceResolutionAlways = requiresHardwiredServiceResolutionAlways;
    }

    /**
     * @return true if the Override Content Type checkbox should be enabled for listen ports that use this protocol.
     */
    public boolean isSupportsSpecifiedContentType() {
        return supportsSpecifiedContentType;
    }

    public void setSupportsSpecifiedContentType(boolean supportsSpecifiedContentType) {
        this.supportsSpecifiedContentType = supportsSpecifiedContentType;
    }

    /**
     * @return true if the Override Content Type value must always be provided for listen ports that use this protocol.
     */
    public boolean isRequiresSpecifiedContentType() {
        return requiresSpecifiedContentType;
    }

    public void setRequiresSpecifiedContentType(boolean requiresSpecifiedContentType) {
        this.requiresSpecifiedContentType = requiresSpecifiedContentType;
    }

    /**
     * @return true if the "private thread pool" GUI controls should be enabled for listen ports that use this protocol.
     */
    public boolean isSupportsPrivateThreadPool() {
        return supportsPrivateThreadPool;
    }

    public void setSupportsPrivateThreadPool(boolean supportsPrivateThreadPool) {
        this.supportsPrivateThreadPool = supportsPrivateThreadPool;
    }

    /**
     * @return the name of a class that extends com.l7tech.console.panels.CustomTransportPropertiesPanel, or null.
     *         If a classname is returned, the class will be loaded on the SSM using the classloader of the associated
     *         modular assertion.
     */
    public String getCustomPropertiesPanelClassname() {
        return customPropertiesPanelClassname;
    }

    public void setCustomPropertiesPanelClassname(String customPropertiesPanelClassname) {
        this.customPropertiesPanelClassname = customPropertiesPanelClassname;
    }

    /**
     * Get a modular assertion classname whose classloader should be used for loading any custom GUI components
     * in the SSM, or null to assume any referenced custom classes are available in the regular SSM classloader.
     *
     * @return the name of an Assertion class that can be given to the AssertionRegistry to get back a prototype instance
     *         whose classloader shall be used to load the advanced properties custom GUI; or null to attempt to use the context classloader
     *         for loading custom GUI panels.
     */
    public String getModularAssertionClassname() {
        return modularAssertionClassname;
    }

    public void setModularAssertionClassname(String modularAssertionClassname) {
        this.modularAssertionClassname = modularAssertionClassname;
    }

    /**
     * @return a set of endpoints whose checkboxes should be enabled in the connector properties GUI for connectors that use this protocol.
     */
    public Set<SsgConnector.Endpoint> getSupportedEndpoints() {
        return supportedEndpoints;
    }

    public void setSupportedEndpoints(Set<SsgConnector.Endpoint> supportedEndpoints) {
        if (supportedEndpoints == null)
            supportedEndpoints = EnumSet.noneOf(SsgConnector.Endpoint.class);
        this.supportedEndpoints = supportedEndpoints;
    }

    /**
     * @return true if the HTTP Settings tab should be enabled in the properties dialog for connectors using this protocol.
     */
    public boolean isHttpBased() {
        return httpBased;
    }

    public void setHttpBased(boolean httpBased) {
        this.httpBased = httpBased;
    }

    /**
     * @return true if the FTP Settings tab should be enabled in the properties dialog for connectors using this protocol.
     */
    public boolean isFtpBased() {
        return ftpBased;
    }

    public void setFtpBased(boolean ftpBased) {
        this.ftpBased = ftpBased;
    }

    @Override
    public String toString() {
        String ret = getDisplayName();
        if (ret != null) return ret;
        ret = getScheme();
        if (ret != null) return ret;
        return super.toString();
    }
}
