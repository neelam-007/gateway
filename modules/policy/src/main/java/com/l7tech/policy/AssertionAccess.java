package com.l7tech.policy;

import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.Functions;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Entity that represents the capability to use a given policy assertion within a policy.
 * <p/>
 * Instances of this entity are created on the fly in memory for every registered assertion.
 * Only those that have been customized with additional info (eg, a security zone) need to be
 * persisted to the DB.
 * <p/>
 * The "name" is the assertion's fully qualified class name.
 * The "id" is the OID of the AssertionAccess entity, commonly -1 for "virtual" AssertionAccess instances.
 */
@javax.persistence.Entity
@Proxy(lazy=false)
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@Table(name="assertion_access")
public class AssertionAccess extends ZoneableNamedEntityImp implements Serializable {

    public AssertionAccess() {
    }

    public AssertionAccess(String assertionClass) {
        setName(assertionClass);
    }

    @Size(min = 1, max = 255)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * @param assertion assertion for which to create a new virtual AssertionAccess with the default (unsaved) OID of -1 and no security zone.
     * @return a new virtual AssertionAccess instance with the default (unsaved) OID of -1 and no security zone.
     */
    public static AssertionAccess forAssertion(@NotNull Assertion assertion) {
        return new AssertionAccess(assertion.getClass().getName());
    }

    /**
     * @return a builder that will map an Assertion instance to a corresponding new virtual AssertionAccess instance with the default OID.
     */
    public static Functions.Unary<AssertionAccess, Assertion> builderFromAssertion() {
        return new Functions.Unary<AssertionAccess, Assertion>() {
            @Override
            public AssertionAccess call(Assertion assertion) {
                return forAssertion(assertion);
            }
        };
    }
}
