package com.l7tech.gateway.common.transport;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Named entitiy for resolution settings.
 */
@Entity
@Proxy(lazy=false)
@Table(name="resolution_configuration")
public class ResolutionConfiguration extends NamedEntityImp {

    //- PUBLIC

    public ResolutionConfiguration() {
    }

    public ResolutionConfiguration( final ResolutionConfiguration resolutionConfiguration,
                                    final boolean lock ) {
        super( resolutionConfiguration );
        setPathRequired( resolutionConfiguration.isPathRequired() );
        setPathCaseSensitive( resolutionConfiguration.isPathCaseSensitive() );
        setUseL7OriginalUrl( resolutionConfiguration.isUseL7OriginalUrl() );
        setUseServiceOid( resolutionConfiguration.isUseServiceOid() );
        setUseSoapAction( resolutionConfiguration.isUseSoapAction() );
        setUseSoapBodyChildNamespace( resolutionConfiguration .isUseSoapBodyChildNamespace());
        if ( lock ) lock();
    }

    @Column(name="path_required")
    public boolean isPathRequired() {
        return pathRequired;
    }

    public void setPathRequired( final boolean pathRequired ) {
        checkLocked();
        this.pathRequired = pathRequired;
    }

    @Column(name="path_case_sensitive")
    public boolean isPathCaseSensitive() {
        return pathCaseSensitive;
    }

    public void setPathCaseSensitive( final boolean pathCaseSensitive ) {
        checkLocked();
        this.pathCaseSensitive = pathCaseSensitive;
    }

    @Column(name="use_url_header")
    public boolean isUseL7OriginalUrl() {
        return useL7OriginalUrl;
    }

    public void setUseL7OriginalUrl( final boolean useL7OriginalUrl ) {
        checkLocked();
        this.useL7OriginalUrl = useL7OriginalUrl;
    }

    @Column(name="use_service_oid")
    public boolean isUseServiceOid() {
        return useServiceOid;
    }

    public void setUseServiceOid( final boolean useServiceOid ) {
        checkLocked();
        this.useServiceOid = useServiceOid;
    }

    @Column(name="use_soap_action")
    public boolean isUseSoapAction() {
        return useSoapAction;
    }

    public void setUseSoapAction( final boolean useSoapAction ) {
        checkLocked();
        this.useSoapAction = useSoapAction;
    }

    @Column(name="use_soap_namespace")
    public boolean isUseSoapBodyChildNamespace() {
        return useSoapBodyChildNamespace;
    }

    public void setUseSoapBodyChildNamespace( final boolean useSoapBodyChildNamespace ) {
        checkLocked();
        this.useSoapBodyChildNamespace = useSoapBodyChildNamespace;
    }

    //- PRIVATE

    private boolean pathRequired;
    private boolean pathCaseSensitive;
    private boolean useL7OriginalUrl;
    private boolean useServiceOid;
    private boolean useSoapAction;
    private boolean useSoapBodyChildNamespace;
}
