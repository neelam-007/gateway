package com.l7tech.server.processcontroller.patching;

import javax.jws.WebService;
import javax.activation.DataHandler;
import javax.xml.ws.soap.MTOM;
import java.util.Collection;

/**
 * PatchService API.
 */
@WebService(name="PatchServiceApi", targetNamespace="http://ns.l7tech.com/secureSpan/5.0/component/processController/patchServiceApi")
@MTOM
public interface PatchServiceApi {

    public PatchStatus uploadPatch(DataHandler patchData) throws PatchException;

    public PatchStatus installPatch(String patchId, Collection<String> nodes) throws PatchException;

    public Collection<PatchStatus> deletePackageArchive(String option) throws PatchException;

    public Collection<PatchStatus> listPatches(boolean ignoreDeletedPatches, boolean ignoreNoneStatus);

    public PatchStatus getStatus(String patchId) throws PatchException;

    /**
     * Searches for the property with the specified {@code name} in the patcher property file (typically {@code /opt/SecureSpan/Controller/etc/conf/patcher.properties}).<br/>
     * If the {@code name} is not found, then the method returns the {@code defaultValue} argument.
     *
     * @param name            the property name.  Required and cannot be {@code null}.
     * @param defaultValue    a default value.  Optional and can be {@code null}.
     * @return A {@code String} containing the current value of the specified {@code propName}, or {@code defaultValue} if the property doesn't exist.
     * @throws PatchException if an error occurs while retrieving the property.
     */
    public String getPatcherProperty(String name, String defaultValue) throws PatchException;

    /**
     * Sets the value of the property with the specified {@code name} in the patcher property file (typically {@code /opt/SecureSpan/Controller/etc/conf/patcher.properties}).<br/>
     *
     * @param name     the property name.  Required and cannot be {@code null}.
     * @param value    the value of the property.  Required and cannot be {@code null}.
     * @return the previous value of the specified property {@code name}, or {@code null} if it did not have one.
     * @throws PatchException if an error occurs while setting the property.
     */
    public String setPatcherProperty(String name, String value) throws PatchException;

    /**
     * Retrieve whether auto deletion of *.L7P after installation is enable or not
     *
     * @throws PatchException if an error occurs while retrieving the property.
     */
    public boolean getAutoDelete() throws PatchException;

    /**
     * Enable or disable auto deletion of *.L7P after installation.
     *
     * @param value    new value.
     * @return previous value of the property, or {@code null} if there wasn't one.
     * @throws PatchException if an error occurs while setting the property.
     */
    public Boolean setAutoDelete(boolean value) throws PatchException;

    enum Action { UPLOAD, INSTALL, ROLLBACK, PACKAGE_DELETE, LIST, STATUS, PROPERTY, AUTO_DELETE }

    /**
     * System property for specifying the node IDs that a patch needs to be applied to.
     * The value is a coma-separated list of node IDs. Used by installPatch() API entry.
     */
    public static final String TARGET_NODE_IDS = "target.node.ids";
}
