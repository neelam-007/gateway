package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.ElementExtendableManagedObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Policy export result.
 *
 * <p>The policy export result encapsulates an exported policy.</p>
 */
@XmlRootElement(name="PolicyExportResult")
@XmlType(name="PolicyExportResultType", propOrder={"resource","extension","extensions"})
public class PolicyExportResult extends ElementExtendableManagedObject {

    //- PUBLIC

    /**
     * The resource containing the exported policy (required)
     *
     * <p>The resource type will be "policyexport" and the content is a
     * policy export document.</p>
     *
     * @return The resource or null.
     */
    @XmlElement(name="Resource", required=true)
    public Resource getResource() {
        return resource;
    }

    /**
     * Set the resource containing the exported policy.
     *
     * @param resource The resource to use.
     */
    public void setResource( final Resource resource ) {
        this.resource = resource;
    }

    //- PACKAGE

    PolicyExportResult(){
    }

    //- PRIVATE

    private Resource resource;
}
