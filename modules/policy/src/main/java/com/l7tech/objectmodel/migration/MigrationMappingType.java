package com.l7tech.objectmodel.migration;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * @author jbufu
 */
public class MigrationMappingType {

    public final static MigrationMappingType BOTH_NONE = new MigrationMappingType(MigrationMappingSelection.NONE, MigrationMappingSelection.NONE);
    public final static MigrationMappingType BOTH_OPTIONAL = new MigrationMappingType(MigrationMappingSelection.OPTIONAL, MigrationMappingSelection.OPTIONAL);

    private MigrationMappingSelection nameMapping;
    private MigrationMappingSelection valueMapping;

    protected MigrationMappingType() {
        this.nameMapping = MigrationMappingSelection.OPTIONAL;
        this.valueMapping = MigrationMappingSelection.OPTIONAL;
    }

    public static MigrationMappingType defaultMappning() {
        //todo: which one is should be the default?
        return MigrationMappingType.BOTH_OPTIONAL;
    }

    public MigrationMappingType(MigrationMappingSelection nameMapping, MigrationMappingSelection valueMapping) {
        this.nameMapping = nameMapping;
        this.valueMapping = valueMapping;
    }

    @XmlAttribute
    public MigrationMappingSelection getNameMapping() {
        return nameMapping;
    }

    public void setNameMapping(MigrationMappingSelection name) {
        this.nameMapping = name;
    }

    @XmlAttribute
    public MigrationMappingSelection getValueMapping() {
        return valueMapping;
    }

    public void setValueMapping(MigrationMappingSelection value) {
        this.valueMapping = value;
    }

    public String toString() {
        return "mapName: " + nameMapping.toString() + " mapValue: " + valueMapping;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MigrationMappingType that = (MigrationMappingType) o;

        if (nameMapping != that.nameMapping) return false;
        if (valueMapping != that.valueMapping) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (nameMapping != null ? nameMapping.hashCode() : 0);
        result = 31 * result + (valueMapping != null ? valueMapping.hashCode() : 0);
        return result;
    }
}
