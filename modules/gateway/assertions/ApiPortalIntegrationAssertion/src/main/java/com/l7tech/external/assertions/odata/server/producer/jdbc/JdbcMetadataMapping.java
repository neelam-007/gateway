package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcColumn;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcTable;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmDataServicesProvider;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmProperty;

import java.util.Map;

public class JdbcMetadataMapping implements EdmDataServicesProvider {

    private final EdmDataServices metadata;
    private final JdbcModel model;
    private final Map<EdmEntitySet, JdbcTable> entitySetMapping;
    private final Map<EdmProperty, JdbcColumn> propertyMapping;

    public JdbcMetadataMapping(EdmDataServices metadata, JdbcModel model, Map<EdmEntitySet, JdbcTable> entitySetMapping, Map<EdmProperty, JdbcColumn> propertyMapping) {
        this.metadata = metadata;
        this.model = model;
        this.entitySetMapping = entitySetMapping;
        this.propertyMapping = propertyMapping;
    }

    @Override
    public EdmDataServices getMetadata() {
        return metadata;
    }

    public JdbcModel getModel() {
        return model;
    }

    public JdbcTable getMappedTable(EdmEntitySet entitySet) {
        return entitySetMapping.get(entitySet);
    }

    public JdbcColumn getMappedColumn(EdmProperty edmProperty) {
        edmProperty.getDeclaringType().getFullyQualifiedTypeName();
        return propertyMapping.get(edmProperty);
    }

}
