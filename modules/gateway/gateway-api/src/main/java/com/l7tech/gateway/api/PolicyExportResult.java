package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * Policy export result.
 *
 * <p>The policy export result encapsulates an exported policy.</p>
 */
@XmlRootElement(name="PolicyExportResult")
@XmlType(name="PolicyExportResultType", propOrder={"resource","extensions"})
public class PolicyExportResult extends ManagedObject {

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

    //- PROTECTED

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @Override
    protected void setExtensions( final List<Object> extensions ) {
        super.setExtensions( extensions );
    }

    //- PACKAGE

    PolicyExportResult(){
    }

    //- PRIVATE

    private Resource resource;
}
