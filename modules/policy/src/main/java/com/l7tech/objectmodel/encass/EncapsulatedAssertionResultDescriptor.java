package com.l7tech.objectmodel.encass;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Represents a declared output value from an encapsulated assertion.
 * <p/>
 * An assertion bean instance of the corresponding encapsulated assertion type will advertise these values in its
 * VariablesSet.
 */
@Entity
@Proxy(lazy=false)
@Table(name="encapsulated_assertion_result")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@XmlRootElement(name = "EncapsulatedAssertionResult")
public class EncapsulatedAssertionResultDescriptor extends PersistentEntityImp {
    private EncapsulatedAssertionConfig encapsulatedAssertionConfig;
    private String resultName;
    private String resultType;

    @ManyToOne(optional=false)
    @JoinColumn(name="encapsulated_assertion_oid", nullable=false)
    @XmlTransient
    public EncapsulatedAssertionConfig getEncapsulatedAssertionConfig() {
        return encapsulatedAssertionConfig;
    }

    public void setEncapsulatedAssertionConfig(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
        checkLocked();
        this.encapsulatedAssertionConfig = encapsulatedAssertionConfig;
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @Column(name="result_name")
    @XmlElement(name = "ResultName")
    public String getResultName() {
        return resultName;
    }

    public void setResultName(String resultName) {
        checkLocked();
        this.resultName = resultName;
    }

    /**
     * @return the data type of this result, as a name of a value of {@link com.l7tech.policy.variable.DataType}.
     */
    @Column(name="result_type")
    @XmlElement(name = "ResultType")
    public String getResultType() {
        return resultType;
    }

    /**
     * @param resultType the data type of this result, as a name of a value of {@link com.l7tech.policy.variable.DataType}.
     */
    public void setResultType(String resultType) {
        checkLocked();
        this.resultType = resultType;
    }

    /**
     * Get a copy of this result descriptor pointing at the specified config, optionally marking it as read-only.
     * <p/>
     * The copy will have the default (non-persisted) OID and so will compare as equals() to the original.
     *
     * @param newConfig config that should own the copied descriptor.  May be null.
     * @param readOnly true to mark the copy read-only.
     * @return a new EncapsulatedAssertionArgumentDescriptor
     */
    public EncapsulatedAssertionResultDescriptor getCopy(EncapsulatedAssertionConfig newConfig, boolean readOnly) {
        EncapsulatedAssertionResultDescriptor copy = new EncapsulatedAssertionResultDescriptor();
        copy.setEncapsulatedAssertionConfig(newConfig);
        copy.setResultName(getResultName());
        copy.setResultType(getResultType());
        if (readOnly)
            copy.lock();
        return copy;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncapsulatedAssertionResultDescriptor)) return false;
        if (!super.equals(o)) return false;

        EncapsulatedAssertionResultDescriptor that = (EncapsulatedAssertionResultDescriptor) o;

        if (resultName != null ? !resultName.equals(that.resultName) : that.resultName != null) return false;
        if (resultType != null ? !resultType.equals(that.resultType) : that.resultType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (resultName != null ? resultName.hashCode() : 0);
        result = 31 * result + (resultType != null ? resultType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EncapsulatedAssertionResultDescriptor{" +
            "eacOid=" + (encapsulatedAssertionConfig == null ? null : encapsulatedAssertionConfig.getOid()) +
            ", resultName='" + resultName + '\'' +
            ", resultType='" + resultType + '\'' +
            '}';
    }
}
