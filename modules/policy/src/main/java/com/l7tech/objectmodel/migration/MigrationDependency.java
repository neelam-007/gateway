package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.ExternalEntityHeader;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

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
 * </ul>
 *
 * @see Migration, MigrationMappingType
 * @author jbufu
 */
@XmlRootElement
@XmlType(propOrder = {"dependant", "propName", "mappingType", "dependency"})
public class MigrationDependency implements Serializable {

    private ExternalEntityHeader dependant;
    private ExternalEntityHeader dependency;

    private String propName;
    private MigrationMappingType mappingType;
    private boolean export;

    protected MigrationDependency() {}

    public MigrationDependency(ExternalEntityHeader dependant, ExternalEntityHeader dependency, String propName, MigrationMappingType mappingType, boolean export) {
        this.dependant = dependant;
        this.dependency = dependency;
        this.propName = propName;
        this.mappingType = mappingType;
        this.export = export;
    }

    @XmlElement(name = "dependant")
    public ExternalEntityHeader getDependant() {
        return dependant;
    }

    public void setDependant(ExternalEntityHeader dependant) {
        this.dependant = dependant;
    }

    @XmlElement(name = "dependency")
    public ExternalEntityHeader getDependency() {
        return dependency;
    }

    public void setDependency(ExternalEntityHeader dependency) {
        this.dependency = dependency;
    }

    public String getPropName() {
        return propName;
    }

    public void setPropName(String propName) {
        this.propName = propName;
    }

    public MigrationMappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType(MigrationMappingType mappingType) {
        this.mappingType = mappingType;
    }

    @XmlAttribute
    public boolean isExport() {
        return export;
    }

    public void setExport(boolean export) {
        this.export = export;
    }

    @Override
    public String toString() {
        return "dependant: " + dependant.toString() + "\npropName: " + propName + "\nmapping type: " + mappingType.toString() + "\ndependency: " + getDependency().toString();
    }
}
