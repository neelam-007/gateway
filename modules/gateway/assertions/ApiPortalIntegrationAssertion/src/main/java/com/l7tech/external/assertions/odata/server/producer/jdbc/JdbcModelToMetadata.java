package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcColumn;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcPrimaryKey;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcSchema;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcTable;
import org.core4j.Func1;
import org.odata4j.core.ImmutableMap;
import org.odata4j.edm.*;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class JdbcModelToMetadata implements Func1<JdbcModel, JdbcMetadataMapping> {

    private static final Logger logger = Logger.getLogger(JdbcModelToMetadata.class.getName());

    private static final Map<Integer, EdmType> SIMPLE_TYPE_MAPPING = ImmutableMap.<Integer, EdmType>of(
            Types.INTEGER, EdmSimpleType.INT32,
            Types.VARCHAR, EdmSimpleType.STRING,
            Types.BOOLEAN, EdmSimpleType.BOOLEAN,
            Types.BIGINT, EdmSimpleType.INT64,
            Types.BIT, EdmSimpleType.INT16);

    public String getModelNamespace() {
        return "JdbcModel";
    }

    public String getContainerNamespace(String entityContainerName) {
        return "JdbcEntities." + entityContainerName;
    }

    public String getEntityContainerName(String schemaName) {
        return rename(schemaName, false);
    }

    public String getEntityTypeName(String tableName) {
        return rename(tableName, false);
    }

    public String getEntitySetName(String tableName) {
        return rename(tableName, true);
    }

    public String getPropertyName(String columnName) {
        return rename(columnName, false);
    }

    private String rename(String dbName, boolean makePlural) {
        return Util.constantToPascalCase(dbName, makePlural);
    }

    private List<JdbcTable> customEntities = new ArrayList<>();

    public JdbcModelToMetadata() {

    }

    public JdbcModelToMetadata(List<JdbcTable> entities) {
        if (entities != null) {
            customEntities.addAll(entities);
        }
    }

    //NOTES: by Richard to make everything as string so it will work

    public EdmType getEdmType(int jdbcType, String columnTypeName, Integer columnSize) {
        if (!SIMPLE_TYPE_MAPPING.containsKey(jdbcType)) {
            //throw new UnsupportedOperationException("TODO implement edmtype conversion for jdbc type: " + jdbcType + "[" + columnTypeName + ":" + columnSize);
            if (Types.BLOB == jdbcType || Types.CLOB == jdbcType || Types.NCLOB == jdbcType) {
                return EdmSimpleType.BINARY;
            }
            return EdmSimpleType.STRING;
        }

        return SIMPLE_TYPE_MAPPING.get(jdbcType);
        //return EdmSimpleType.STRING;
    }

    private static final Map<Class<?>, EdmSimpleType<?>> ADDITIONAL_TYPES = new HashMap<Class<?>, EdmSimpleType<?>>();

    static {
        ADDITIONAL_TYPES.put(Object.class, EdmSimpleType.STRING);
    }

    public EdmSimpleType<?> findEdmType(Class<?> clazz) {
        EdmSimpleType<?> type = EdmSimpleType.forJavaType(clazz);
        if (type == null)
            type = ADDITIONAL_TYPES.get(clazz);

        return type;
    }

    @Override
    public JdbcMetadataMapping apply(JdbcModel jdbcModel) {
        String modelNamespace = getModelNamespace();

        List<EdmEntityType.Builder> entityTypes = new ArrayList<EdmEntityType.Builder>();
        List<EdmEntityContainer.Builder> entityContainers = new ArrayList<EdmEntityContainer.Builder>();
        List<EdmEntitySet.Builder> entitySets = new ArrayList<EdmEntitySet.Builder>();

        Map<EdmEntitySet.Builder, JdbcTable> entitySetMapping = new HashMap<EdmEntitySet.Builder, JdbcTable>();
        Map<EdmProperty.Builder, JdbcColumn> propertyMapping = new HashMap<EdmProperty.Builder, JdbcColumn>();

        boolean isView = false, isCustom = false;
        for (JdbcSchema jdbcSchema : jdbcModel.schemas) {
            List<JdbcTable> tables = new ArrayList<>();
            tables.addAll(jdbcSchema.tables);
            tables.addAll(customEntities);
            for (JdbcTable jdbcTable : tables) {
                if (jdbcTable.primaryKeys.isEmpty()) {
                    if ("VIEW".equalsIgnoreCase(jdbcTable.tableType)) {
                        isView = true;
                        logger.fine("Found view " + jdbcTable.tableName + ", allowing no keys, checking for fallback keys later");
                    } else if ("CUSTOM".equalsIgnoreCase(jdbcTable.tableType)) {
                        isCustom = true;
                        logger.fine("Found custom entity " + jdbcTable.tableName + ", allowing no keys, checking for fallback keys later");
                    } else {
                        logger.fine("Skipping JdbcTable " + jdbcTable.tableName + ", no keys");
                        continue;
                    }
                }

                String entityTypeName = getEntityTypeName(jdbcTable.tableName);
                EdmEntityType.Builder entityType = EdmEntityType.newBuilder().setName(entityTypeName).setNamespace(modelNamespace);
                entityTypes.add(entityType);

                for (JdbcPrimaryKey primaryKey : jdbcTable.primaryKeys) {
                    String propertyName = getPropertyName(primaryKey.columnName);
                    entityType.addKeys(propertyName);
                }

                boolean hasIdField = false;
                boolean hasUuidField = false;
                for (JdbcColumn jdbcColumn : jdbcTable.columns) {
                    String propertyName = getPropertyName(jdbcColumn.columnName);
                    EdmType propertyType = getEdmType(jdbcColumn.columnType, jdbcColumn.columnTypeName, jdbcColumn.columnSize);
                    EdmProperty.Builder property = EdmProperty.newBuilder(propertyName).setType(propertyType).setNullable(jdbcColumn.isNullable);
                    property.setMaxLength(jdbcColumn.columnSize);
                    entityType.addProperties(property);
                    propertyMapping.put(property, jdbcColumn);
                    if (isView || isCustom) {//if it's a view/custom, check if we have a fallback property we can use as Id
                        if (propertyName.equalsIgnoreCase("Id")) {
                            hasIdField = true;
                        } else if (propertyName.equalsIgnoreCase("Uuid")) {
                            hasUuidField = true;
                        }
                        //TODO: make this fallback key configurable???
                    }
                }
                if (isView || isCustom) {
                    if (hasIdField) {
                        entityType.addKeys("Id");
                    } else if (!hasIdField && hasUuidField) {
                        entityType.addKeys("Uuid");
                    } else {
                        logger.fine(jdbcTable.tableType + " - " + jdbcTable.tableName + ", might not have a fallback key.");
                    }
                }

                String entitySetName = getEntitySetName(jdbcTable.tableName);
                EdmEntitySet.Builder entitySet = EdmEntitySet.newBuilder().setName(entitySetName).setEntityType(entityType);
                entitySets.add(entitySet);
                entitySetMapping.put(entitySet, jdbcTable);
            }

            String entityContainerName = getEntityContainerName(jdbcSchema.schemaName);
            EdmEntityContainer.Builder entityContainer = EdmEntityContainer.newBuilder().setName(entityContainerName).setIsDefault(jdbcSchema.isDefault).addEntitySets(entitySets);
            entityContainers.add(entityContainer);
        }

        List<EdmSchema.Builder> edmSchemas = new ArrayList<EdmSchema.Builder>();
        EdmSchema.Builder modelSchema = EdmSchema.newBuilder().setNamespace(modelNamespace).addEntityTypes(entityTypes);
        edmSchemas.add(modelSchema);
        for (EdmEntityContainer.Builder entityContainer : entityContainers) {
            String containerSchemaNamespace = getContainerNamespace(entityContainer.getName());
            EdmSchema.Builder containerSchema = EdmSchema.newBuilder().setNamespace(containerSchemaNamespace).addEntityContainers(entityContainer);
            edmSchemas.add(containerSchema);
        }
        EdmDataServices metadata = EdmDataServices.newBuilder().addSchemas(edmSchemas).build();

        Map<EdmEntitySet, JdbcTable> finalEntitySetMapping = new HashMap<EdmEntitySet, JdbcTable>();
        for (Map.Entry<EdmEntitySet.Builder, JdbcTable> entry : entitySetMapping.entrySet()) {
            finalEntitySetMapping.put(entry.getKey().build(), entry.getValue());
        }
        Map<EdmProperty, JdbcColumn> finalPropertyMapping = new HashMap<EdmProperty, JdbcColumn>();
        for (Map.Entry<EdmProperty.Builder, JdbcColumn> entry : propertyMapping.entrySet()) {
            finalPropertyMapping.put(entry.getKey().build(), entry.getValue());
        }
        JdbcMetadataMapping jdbcMetadataMapping = new JdbcMetadataMapping(metadata, jdbcModel, finalEntitySetMapping, finalPropertyMapping);
        return jdbcMetadataMapping;
    }

}
