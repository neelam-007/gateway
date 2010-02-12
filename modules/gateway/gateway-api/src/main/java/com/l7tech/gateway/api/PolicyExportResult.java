package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * 
 */
@XmlRootElement(name="PolicyExportResult")
@XmlType(name="PolicyExportResultType", propOrder={"resource","extensions"})
public class PolicyExportResult extends ManagedObject {

    //- PUBLIC

    @XmlElement(name="Resource", required=false)
    public Resource getResource() {
        return resource;
    }

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
