package com.ca.datasources.cassandra;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 10/30/14
 */
public class CassandraUtil {

    public static Object javaType2CassandraDataType(ColumnDefinitions.Definition definition, Object value ) {
        //TODO: implement mapping to Cassandra data types
        Class clazz = definition.getType().asJavaClass();
        if(clazz.isAssignableFrom(Integer.class)){
            return Integer.valueOf(value.toString());
        }
        else if(clazz.isAssignableFrom(Long.class)) {
            return Long.valueOf(value.toString());
        }
        else if(clazz.isAssignableFrom(String.class)) {
            return value.toString();
        }
        else {
            return value;
        }
    }

    public static Object cassandraDataType2JavaType(ColumnDefinitions.Definition definition, Row row){
        Object o = null;
        String columnName = definition.getName();
        DataType.Name name= definition.getType().getName();
        switch (name) {
            case ASCII:
            case TEXT:
            case VARCHAR:
                o =  row.getString(columnName);
                break;
            case BIGINT:
            case COUNTER:
                o = row.getLong(columnName);
                break;
            case BLOB:
            case CUSTOM:
                o = row.getBytes(columnName);
                break;
            case BOOLEAN:
                o = row.getBool(columnName);
                break;
            case DECIMAL:
                o = row.getDecimal(columnName);
                break;
            case DOUBLE:
                o = row.getDouble(columnName);
                break;
            case FLOAT:
                o = row.getFloat(columnName);
                break;
            case INET:
                o = row.getInet(columnName);
                break;
            case INT:
                o = row.getInt(columnName);
                break;
            case LIST:
                o = row.getList(columnName, Object.class);
                break;
            case MAP:
                o =row.getMap(columnName, Object.class, Object.class);
                break;
            case SET:
                o = row.getSet(columnName, Object.class);
                break;
            case TIMESTAMP:
                o = row.getDate(columnName);
                break;
            case UUID:
            case TIMEUUID:
                o = row.getUUID(columnName);
                break;
            case VARINT:
                o = row.getVarint(columnName);
                break;
        }
        return o;
    }

    public static int getIntOrDefault(String value, int defaultVal)  {
        if(value == null) return defaultVal;
        int result;
        try{
            result = Integer.parseInt(value);
        } catch(NumberFormatException ne) {
            result = defaultVal;
        }

        return result;
    }

    public static Integer getInteger(String value) {
        if(value == null) return null;
        Integer result = null;
        try{
            result = Integer.valueOf(value);
        } catch(NumberFormatException ne) {
           return null;
        }

        return result;
    }


    private CassandraUtil(){}
}
