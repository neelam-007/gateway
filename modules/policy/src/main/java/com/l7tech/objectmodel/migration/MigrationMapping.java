package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.EntityHeaderRef;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;

/**
 * A MigrationMapping represents a dependency and mapping relationship between two entities (source and target),
 * along with its characteristics:
 * - the property name of the source entity that is
 * - the mapping type
 *
 * @see MigrationMappingType
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder = {"source", "propName", "type", "target", "mappedTarget"})
public class MigrationMapping {

    private EntityHeaderRef source;
    private EntityHeaderRef target;
    private String propName;
    private MigrationMappingType type;

    private EntityHeaderRef mappedTarget;  // name-mapping

    protected MigrationMapping() {}

    public MigrationMapping(EntityHeaderRef source, EntityHeaderRef target, String propName, MigrationMappingType type) {
        this.source = EntityHeaderRef.fromOther(source);
        this.target = EntityHeaderRef.fromOther(target);
        this.propName = propName;
        this.type = type;
    }

    @XmlElement(name = "source")
    public EntityHeaderRef getSource() {
        return source;
    }

    public void setSource(EntityHeaderRef source) {
        this.source = EntityHeaderRef.fromOther(source); // discard subclass info, for clean serialization output
    }

    @XmlElement(name = "target")
    public EntityHeaderRef getTarget() {
        return target;
    }

    public void setTarget(EntityHeaderRef target) {
        this.target = EntityHeaderRef.fromOther(target); // discard subclass info, for clean serialization output
    }

    public String getPropName() {
        return propName;
    }

    public void setPropName(String propName) {
        this.propName = propName;
    }

    public MigrationMappingType getType() {
        return type;
    }

    public void setType(MigrationMappingType type) {
        this.type = type;
    }

    @XmlElement(name = "mappedTarget")
    public EntityHeaderRef getMappedTarget() {
        return mappedTarget;
    }

    public void setMappedTarget(EntityHeaderRef mappedTarget) {
        if (type.getNameMapping() == MigrationMappingSelection.NONE)
            throw new IllegalStateException("Mapping selection set to NONE, cannot set new mapping target for: " + this.toString() );
        this.mappedTarget = EntityHeaderRef.fromOther(mappedTarget);
    }

    public String toString() {
        return "source: " + source.toString() + "\npropName: " + propName + "\nmapping type: " + type.toString() + "\ntarget: " + target.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MigrationMapping that = (MigrationMapping) o;

        if (propName != null ? !propName.equals(that.propName) : that.propName != null) return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (target != null ? !target.equals(that.target) : that.target != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (source != null ? source.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (propName != null ? propName.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
