package com.ca.datasources.cassandra;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;
import com.l7tech.util.Functions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 10/30/14
 */
public class CassandraUtil {

    public static Object javaType2CassandraDataType(ColumnDefinitions.Definition definition, Object value ) {
        if(value == null) return null;

        if(definition.getType().equals(DataType.cint())) {
            return getInteger(value);
        }
        else if(definition.getType().equals(DataType.varint())) {
            return getBigInteger(value);
        }
        else if(definition.getType().equals(DataType.bigint()) || definition.getType().equals(DataType.counter())) {
            return getLong(value);
        }
        else if(definition.getType().equals(DataType.ascii()) || definition.getType().equals(DataType.text()) || definition.getType().equals(DataType.varchar())) {
            return value.toString();
        }
        else if(definition.getType().equals(DataType.uuid()) || definition.getType().equals(DataType.timeuuid())) {
            return UUID.fromString(value.toString());
        }
        else if(definition.getType().equals(DataType.cdouble())) {
            return getDouble(value);
        }
        else if(definition.getType().equals(DataType.decimal())) {
            return getBigDecimal(value);
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
                ByteBuffer bb = row.getBytes(columnName);
                o = bb.array();
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
            case TUPLE:
                o = row.getTupleValue(columnName);
                break;
            case UDT:
                o = row.getUDTValue(columnName);
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

    public static Long getLong(Object value) {
        if(value == null) return null;
        try {
            return getNumber(value, new Functions.Unary<Long, Object>() {
                        @Override
                        public Long call(Object o) {
                            return o instanceof Number ? (Long) o : null;
                        }
                    },
                    new Functions.Unary<Long, Object>() {
                        @Override
                        public Long call(Object o) {
                            return Long.parseLong((String) o);
                        }
                    });
        }  catch(Exception ne) {
            return null;
        }
    }

    public static Integer getInteger(Object value) {
        if(value == null) return null;
        try {
            return getNumber(value, new Functions.Unary<Integer, Object>() {
                        @Override
                        public Integer call(Object o) {
                            return o instanceof Number ? (Integer) o : null;
                        }
                    },
                    new Functions.Unary<Integer, Object>() {
                        @Override
                        public Integer call(Object o) {
                            return Integer.parseInt((String) o);
                        }
                    });
        } catch(Exception ne) {
            return null;
        }
    }

    public static BigInteger getBigInteger(Object value) {
        if(value == null) return null;
        try {
            if(value instanceof Number) {
                if(value instanceof BigDecimal) {
                    return  ((BigDecimal)value).unscaledValue();
                }
                else {
                    return (BigInteger) value;
                }
            }
            else {
                return new BigInteger((String) value);
            }
        } catch(Exception ne) {
            return null;
        }
    }

    public static BigDecimal getBigDecimal(Object value) {
        if(value == null) return null;
        try {
            return getNumber(value, new Functions.Unary<BigDecimal, Object>() {
                        @Override
                        public BigDecimal call(Object o) {
                            return o instanceof Number ? (BigDecimal) o : null;
                        }
                    },
                    new Functions.Unary<BigDecimal, Object>() {
                        @Override
                        public BigDecimal call(Object o) {
                            return new BigDecimal((String) o);
                        }
                    });
        } catch(Exception ne) {
            return null;
        }
    }

    public static Double getDouble(Object value) {
        if(value == null) return null;
        try {
            return getNumber(value, new Functions.Unary<Double, Object>() {
                        @Override
                        public Double call(Object o) {
                            return o instanceof Number ? (Double) o : null;
                        }
                    },
                    new Functions.Unary<Double, Object>() {
                        @Override
                        public Double call(Object o) {
                            return Double.parseDouble((String) o);
                        }
                    });
        } catch(Exception ne) {
            return null;
        }
    }

    private static <V extends Number> V getNumber(Object obj, Functions.Unary<V, Object> f1, Functions.Unary<V, Object> f2) throws Exception {
        V value = f1.call(obj);
        if(value != null) {
            value = (V)obj;
        }
        else {
            value = f2.call(obj);
        }
        return value;
    }


    private CassandraUtil(){}
}
