package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.EntityHeaderRef;

import javax.xml.bind.annotation.*;

/**
 * A MigrationMapping captures a dependency and mapping relationship between two entities (source and target);
 * the following attributes describe a dependency mapping:
 * <ul>
 * <li>source entity header reference</li>
 * <li>
 *  property name of the source entity; extra semantic can be added to it, as needed by specific property resolvers;
 *  the delimiter between these and the actual property name is in this case ":"
 * </li>
 * <li>mapping type</li>
 * <li>target entity header reference</li>
 * <li>
 *  flag that specifies whether the dependency is uploaded on the target cluster by its parent (true),
 *  or by its own entity manager (false)</li>
 * </ul>
 *
 * @see Migration, MigrationMappingType
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder = {"dependant", "propName", "type", "sourceDependency", "mappedDependency"})
public class MigrationMapping {

    private EntityHeaderRef dependant;
    private EntityHeaderRef sourceDependency;
    private EntityHeaderRef mappedDependency;

    private String propName;
    private MigrationMappingType type;
    private boolean uploadedByParent;
    private boolean export;

    protected MigrationMapping() {}

    public MigrationMapping(EntityHeaderRef dependant, EntityHeaderRef sourceDependency, String propName, MigrationMappingType type, boolean uploadByParent, boolean export) {
        this.dependant = EntityHeaderRef.fromOther(dependant);
        this.sourceDependency= EntityHeaderRef.fromOther(sourceDependency);
        this.propName = propName;
        this.type = type;
        this.uploadedByParent = uploadByParent;
        this.export = export;
    }

    @XmlElement(name = "dependant")
    public EntityHeaderRef getDependant() {
        return dependant;
    }

    public void setDependant(EntityHeaderRef dependant) {
        this.dependant = EntityHeaderRef.fromOther(dependant); // discard subclass info, for clean serialization output
    }

    @XmlElement(name = "sourceDependency")
    public EntityHeaderRef getSourceDependency() {
        return sourceDependency;
    }

    public void setSourceDependency(EntityHeaderRef dependency) {
        this.sourceDependency = EntityHeaderRef.fromOther(dependency); // discard subclass info, for clean serialization output
    }

    @XmlElement(name = "mappedDependency")
    public EntityHeaderRef getMappedDependency() {
        return mappedDependency;
    }

    public void setMappedDependency(EntityHeaderRef dependency) {
        this.mappedDependency = EntityHeaderRef.fromOther(dependency); // discard subclass info, for clean serialization output
    }

    public EntityHeaderRef getDependency() {
        return mappedDependency != null ? mappedDependency : sourceDependency;
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

    @XmlAttribute
    public boolean isUploadedByParent() {
        return uploadedByParent;
    }

    public void setUploadedByParent(boolean uploadedByParent) {
        this.uploadedByParent = uploadedByParent;
    }

    @XmlAttribute
    public boolean isExport() {
        return export;
    }

    public void setExport(boolean export) {
        this.export = export;
    }

    public void mapDependency(EntityHeaderRef mappedDependency, boolean enforceMappingType) {
        if (enforceMappingType && type.getNameMapping() == MigrationMappingSelection.NONE)
            throw new IllegalStateException("Mapping selection set to NONE, cannot set new mapping target for: " + this.toString() );
        this.mappedDependency = EntityHeaderRef.fromOther(mappedDependency);
    }

    public void mapDependency(EntityHeaderRef mappedTarget) {
        mapDependency(mappedTarget, true);
    }

    @XmlAttribute
    public boolean isMappedDependency() {
        return mappedDependency != null;
    }

    public String toString() {
        return "dependant: " + dependant.toString() + "\npropName: " + propName + "\nmapping type: " + type.toString() + "\ndependency: " + getDependency().toString();
    }
}
