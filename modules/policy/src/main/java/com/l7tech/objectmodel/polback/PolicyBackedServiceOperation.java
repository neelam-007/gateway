package com.l7tech.objectmodel.polback;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.search.Dependency;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a binding between a policy-backed service instance and a single operation offered by this service
 * (implemented by a Policy with the correct tag and subtag).
 */
@Entity
@Proxy(lazy=false)
@Table(name="policy_backed_service_operation")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@XmlRootElement(name = "PolicyBackedServiceOperation")
public class PolicyBackedServiceOperation extends NamedEntityImp {
    private PolicyBackedService policyBackedService;
    private Goid policyGoid;

    @ManyToOne(optional=false)
    @JoinColumn(name="policy_backed_service_goid", nullable=false)
    public PolicyBackedService getPolicyBackedService() {
        return policyBackedService;
    }

    public void setPolicyBackedService(PolicyBackedService policyBackedService) {
        checkLocked();
        this.policyBackedService = policyBackedService;
    }

    @Column(name="policy_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    @Dependency( type = Dependency.DependencyType.POLICY, methodReturnType = Dependency.MethodReturnType.GOID )
    public Goid getPolicyGoid() {
        return policyGoid;
    }

    public void setPolicyGoid( Goid policyGoid ) {
        this.policyGoid = policyGoid;
    }


    /**
     * Get a copy of this policy-backed service operation pointing at the specified policy-backed service bean, optionally marking it as read-only.
     * <p/>
     * The copy will have the default (non-persisted) OID and so will not compare as equals() to the original.
     *
     * @param newService config that should own the copied operation.  May be null.
     * @param readOnly true to mark the copy read-only.
     * @return a new PolicyBackedServiceOperation
     */
    public PolicyBackedServiceOperation getCopy( PolicyBackedService newService, boolean readOnly ) {
        PolicyBackedServiceOperation copy = new PolicyBackedServiceOperation();

        copy.setPolicyBackedService( newService );
        copy.setPolicyGoid( getPolicyGoid() );

        if ( readOnly )
            copy.lock();

        return copy;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof PolicyBackedServiceOperation ) ) return false;
        if ( !super.equals( o ) ) return false;

        PolicyBackedServiceOperation that = (PolicyBackedServiceOperation) o;

        if ( policyBackedService != null ? !policyBackedService.equals( that.policyBackedService ) : that.policyBackedService != null )
            return false;
        if ( !Goid.equals( policyGoid, that.policyGoid ) )
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + ( policyBackedService != null ? policyBackedService.hashCode() : 0 );
        result = 31 * result + ( policyGoid != null ? policyGoid.hashCode() : 0 );
        return result;
    }
}
