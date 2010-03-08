package com.l7tech.gateway.api;

import java.util.List;
import java.util.Map;

/**
 * Extension of the base Accessor interface with Policy support.
 *
 * @see Accessor.AccessorRuntimeException <code>AccessorRuntimeException</code> Which may be thrown by methods of this interface.
 */
public interface PolicyAccessor<AO extends AccessibleObject> extends Accessor<AO> {

    /**
     * Get the policy for the identified resource.
     *
     * @param identifier The identifier for the resource.
     * @return The policy resource
     * @throws AccessorException If an error occurs
     */
    Resource getPolicy( String identifier ) throws AccessorException;

    /**
     * Set the policy for the identified resource.
     *
     * @param identifier The identifier for the resource.
     * @param resource The policy resource
     * @throws AccessorException If an error occurs
     */
    void putPolicy( String identifier, Resource resource ) throws AccessorException;

    /**
     * Export the policy for the identified resource.
     *
     * @param identifier The identifier for the resource.
     * @return The policy export XML
     * @throws AccessorException If an error occurs
     */
    String exportPolicy( String identifier ) throws AccessorException;

    /**
     * Import a previously exported policy.
     *
     * @param identifier The identifier for the policy import target
     * @param properties The properties associated with the policy import
     * @param export The policy export XML to be imported
     * @param instructions The import instructions
     * @return The results of the policy import.
     * @throws AccessorException If an error occurs
     */
    PolicyImportResult importPolicy( String identifier,
                                     Map<String, Object> properties,
                                     String export,
                                     List<PolicyReferenceInstruction> instructions ) throws AccessorException;

    /**
     * Validate the given policy.
     *
     * @param item The managed object
     * @param resourceSets Additional resources to use when validating (optional)
     * @return The result of the policy validation
     * @throws AccessorException If an error occurs
     */
    PolicyValidationResult validatePolicy( AO item,
                                           List<ResourceSet> resourceSets ) throws AccessorException;

    /**
     * Validate the identified policy.
     *
     * @param identifier The identifier for the resource
     * @return The result of the policy validation
     * @throws AccessorException If an error occurs
     */
    PolicyValidationResult validatePolicy( String identifier ) throws AccessorException;

}
