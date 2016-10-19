package com.ca.datasources.cassandra;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.MockDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;


public class CassandraUtilTest {


    MockDefinition mockDefinition;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testBigIntegerJavaType2CassandraDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.varint());
        assertEquals(new BigInteger("11111111111222222222233333333"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new BigInteger("11111111111222222222233333333")));
        assertEquals(new BigInteger("11111111111222222222233333333"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("11111111111222222222233333333")));
    }

    @Test
    public void testBigDecimalJavaType2CassandraVarIntDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.varint());
        assertEquals(new BigInteger("11111111111222222222233333333"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new BigDecimal("11111111111222222222233333333")));
    }

    @Test
    public void  testLongJavaType2CassandraBigIntDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.bigint());
        assertEquals(new Long(123456789098786655L), CassandraUtil.javaType2CassandraDataType(mockDefinition, new Long(123456789098786655L)));
        assertEquals(new Long(123456789098786655L), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("123456789098786655")));
    }

    @Test
    public void  testLongJavaType2CassandraCounterDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.counter());
        assertEquals(new Long(123456789098786655L), CassandraUtil.javaType2CassandraDataType(mockDefinition, new Long(123456789098786655L)));
        assertEquals(new Long(123456789098786655L), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("123456789098786655")));
    }

    @Test
    public void  testDoubleJavaType2CassandraBigIntDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.cdouble());
        assertEquals(new Double(33.33), CassandraUtil.javaType2CassandraDataType(mockDefinition, new Double(33.33)));
        assertEquals(new Double(3.333e1), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("33.33")));
    }

    @Test
    public void  testBigDecimalJavaType2CassandraDecimalDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.decimal());
        assertEquals(new BigDecimal(3399999999999999999999999999999999933.), CassandraUtil.javaType2CassandraDataType(mockDefinition, new BigDecimal(3399999999999999999999999999999999933.)));
        assertEquals(new BigDecimal(3399999999999999999999999999999999933.), CassandraUtil.javaType2CassandraDataType(mockDefinition, new BigDecimal(3399999999999999999999999999999999933.).toPlainString()));
    }

    @Test
    public void testIntegerJavaType2CassandraCintDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.cint());
        assertEquals(new Integer("1112"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new Integer("1112")));
        assertEquals(new Integer("1112"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("1112")));
    }

    @Test
    public void testStringJavaType2CassandraAsciiDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.ascii());
        assertEquals(new String("test"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("test")));
    }

    @Test
    public void testStringJavaType2CassandraTextDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.text());
        assertEquals(new String("test"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("test")));
    }

    @Test
    public void testStringJavaType2CassandraVarcharDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.varchar());
        assertEquals(new String("test"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("test")));
    }

    @Test
    public void testUUIDJavaType2CassandraUuidDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.uuid());
        assertEquals(UUID.fromString("123456789"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("123456789")));
    }

    @Test
    public void testUUIDJavaType2CassandraTimeuuidDataType() throws Exception {
        mockDefinition = new MockDefinition("test","table", "column", DataType.timeuuid());
        assertEquals(UUID.fromString("123456789"), CassandraUtil.javaType2CassandraDataType(mockDefinition, new String("123456789")));
    }

}