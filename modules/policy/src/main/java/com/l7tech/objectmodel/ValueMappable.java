package com.l7tech.objectmodel;

import com.l7tech.objectmodel.migration.MigrationMappingSelection;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @author jbufu
 */
public interface ValueMappable {

    void setValueMapping(MigrationMappingSelection valueMappingType, ExternalEntityHeader.ValueType dataType, Object sourceValue);
    MigrationMappingSelection getValueMapping();

    void setValueType(ExternalEntityHeader.ValueType type);
    ExternalEntityHeader.ValueType getValueType();

    void setDisplayValue(String value);
    String getDisplayValue();

    void setMappedValue(String mappedValue);
    String getMappedValue();

}
