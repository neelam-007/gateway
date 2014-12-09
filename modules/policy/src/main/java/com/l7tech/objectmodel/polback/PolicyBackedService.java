package com.l7tech.objectmodel.polback;

import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.util.BeanUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an implementation of a service that provides one or more operations backed by
 * encapsulated assertions.
 * <p/>
 * Every policy backed service describes an implementation of a Java interface registered with
 * the PolicyBackedServicRegistry.  The methods of the implementation work by executing policy fragments.
 * <p/>
 * For example, a generic "key/value store" policy-backed service might include
 * two methods, one for get-value-by-key and one for put-key-and-value.  An policy backed service
 * implementing this interface would point at actual encapsulated assertion instances wired up to backing
 * policies that implement the actual GET and PUT logic, decoupled from users of the service.
 */
@Entity
@XmlRootElement(name = "PolicyBackedService")
@XmlType(propOrder = {"serviceInterfaceName", "operations"})
@Proxy(lazy=false)
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@Table(name="policy_backed_service")
public class PolicyBackedService extends ZoneableNamedEntityImp {
    private String serviceInterfaceName;
    private Set<PolicyBackedServiceOperation> operations = new HashSet<>();

    @Column( name="interface_name", length=256)
    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    public void setServiceInterfaceName( String serviceInterfaceName ) {
        this.serviceInterfaceName = serviceInterfaceName;
    }

    @Fetch( FetchMode.SUBSELECT)
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="policyBackedService", orphanRemoval=true)
    @XmlElementWrapper(name = "PolicyBackedServiceOperations")
    @XmlElement(name = "PolicyBackedServiceOperation")
    @Migration(dependency = false)
    public Set<PolicyBackedServiceOperation> getOperations() {
        return operations;
    }

    public void setOperations(Set<PolicyBackedServiceOperation> operations) {
        checkLocked();
        this.operations = operations;
    }

    @Transient
    @Migration(dependency = false)
    private PolicyBackedService getCopy(boolean readOnly) {
        //noinspection TryWithIdenticalCatches
        try {
            PolicyBackedService copy = new PolicyBackedService();
            BeanUtils.copyProperties( this, copy,
                    BeanUtils.omitProperties( BeanUtils.getProperties( getClass() ), "operations", "templateService" ) );

            final Set<PolicyBackedServiceOperation> ops = new HashSet<>();
            Set<PolicyBackedServiceOperation> ourOps = getOperations();
            if ( ourOps != null ) {
                for ( PolicyBackedServiceOperation ourOp : ourOps ) {
                    final PolicyBackedServiceOperation ourOpCopy = ourOp.getCopy( copy, readOnly );
                    ops.add( ourOpCopy );
                }
            }
            copy.setOperations( ops );

            if ( readOnly )
                copy.lock();

            return copy;
        } catch (InvocationTargetException e) {
            throw new RuntimeException( e );
        } catch (IllegalAccessException e) {
            throw new RuntimeException( e );
        }
    }

    @Transient
    @Migration(dependency = false)
    public PolicyBackedService getReadOnlyCopy() {
        PolicyBackedService copy = getCopy(true);
        copy.setReadOnly();
        return copy;
    }

    /**
     * Initialize any lazily-computed fields and mark this instance as read-only.
     */
    private void setReadOnly() {
        this.getOperations();
        this.lock();
    }
}
