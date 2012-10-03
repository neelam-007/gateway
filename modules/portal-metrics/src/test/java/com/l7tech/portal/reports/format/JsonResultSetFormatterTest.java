package com.l7tech.portal.reports.format;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JsonResultSetFormatterTest {
    @Mock
    private ResultSet resultSet;
    @Mock
    private ResultSetMetaData metaData;
    private JsonResultSetFormatter formatter;
    private ResultSetFormatOptions options;
    private Map<String, Object> defaultColumnValues;

    @Before
    public void setup() throws Exception {
        formatter = new JsonResultSetFormatter();
        when(resultSet.getMetaData()).thenReturn(metaData);
        options = new ResultSetFormatOptions();
        defaultColumnValues = new HashMap<String, Object>();
    }

    @Test
    public void singleRowSingleColumn() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn("value");

        final String result = formatter.format(resultSet, null);

        assertEquals("[{\"name\":\"value\"}]", result);
    }

    @Test
    public void singleRowMultipleColumn() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("name1");
        when(resultSet.getObject(1)).thenReturn("value1");
        when(metaData.getColumnLabel(2)).thenReturn("name2");
        when(resultSet.getObject(2)).thenReturn("value2");

        final String result = formatter.format(resultSet, null);

        assertTrue(result.contains("\"name1\":\"value1\""));
        assertTrue(result.contains("\"name2\":\"value2\""));
    }

    @Test
    public void multipleRowSingleColumn() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn("value1", "value2");

        final String result = formatter.format(resultSet, null);

        assertTrue(result.contains("{\"name\":\"value1\"}"));
        assertTrue(result.contains("{\"name\":\"value2\"}"));
    }

    @Test
    public void multipleRowMultipleColumn() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("name1");
        when(metaData.getColumnLabel(2)).thenReturn("name2");
        when(resultSet.getObject(1)).thenReturn("value1", "value3");
        when(resultSet.getObject(2)).thenReturn("value2", "value4");

        final String result = formatter.format(resultSet, null);

        assertTrue(result.contains("{\"name1\":\"value1\",\"name2\":\"value2\"}"));
        assertTrue(result.contains("{\"name1\":\"value3\",\"name2\":\"value4\"}"));
    }

    @Test
    public void noRows() throws Exception {
        when(resultSet.next()).thenReturn(false);

        final String result = formatter.format(resultSet, null);

        assertEquals("[]", result);
    }

    @Test
    public void noRowsWithGrouping() throws Exception {
        options.setGroupingColumnName("service");
        when(resultSet.next()).thenReturn(false);

        final String result = formatter.format(resultSet, options);

        assertEquals("{}", result);
    }

    @Test
    public void noRowsDoNotAddMissingData() throws Exception {
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(0L);
        options.setExpectedEndValue(1000L);
        when(resultSet.next()).thenReturn(false);

        final String result = formatter.format(resultSet, options);

        assertEquals("[]", result);
    }

    @Test
    public void returnsLowerCaseColumnName() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("NAME");
        when(resultSet.getObject(1)).thenReturn("value");

        final String result = formatter.format(resultSet, null);

        assertEquals("[{\"name\":\"value\"}]", result);
    }

    /**
     * Bug in 3rd party json library!
     */
    @Ignore
    @Test
    public void nullValue() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn(null);

        final String result = formatter.format(resultSet, null);

        assertEquals("[{\"name\":null}]", result);
    }

    @Test
    public void emptyValue() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn("");

        final String result = formatter.format(resultSet, null);

        assertEquals("[{\"name\":\"\"}]", result);
    }

    @Test(expected = FormatException.class)
    public void sqlException() throws Exception {
        when(resultSet.next()).thenThrow(new SQLException("mocking exception"));

        formatter.format(resultSet, null);
    }

    @Test
    public void groupByColumm() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("cereal");
        when(metaData.getColumnLabel(2)).thenReturn("fruit");
        when(resultSet.getObject(1)).thenReturn("Cheerios", "Fruit Loops", "Cheerios");
        when(resultSet.getObject(2)).thenReturn("banana", "blueberry", "apple");
        options.setGroupingColumnName("cereal");

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("Cheerios");
        assertEquals(2, jsonArray1.length());
        final JSONObject breakfast1 = jsonArray1.getJSONObject(0);
        assertEquals(1, breakfast1.length());
        assertEquals("banana", breakfast1.get("fruit"));
        final JSONObject breakfast2 = jsonArray1.getJSONObject(1);
        assertEquals(1, breakfast2.length());
        assertEquals("apple", breakfast2.get("fruit"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("Fruit Loops");
        assertEquals(1, jsonArray2.length());
        final JSONObject breakfast3 = jsonArray2.getJSONObject(0);
        assertEquals(1, breakfast3.length());
        assertEquals("blueberry", breakfast3.get("fruit"));
    }

    @Test
    public void groupByColummNoRows() throws Exception {
        when(resultSet.next()).thenReturn(false);
        options.setGroupingColumnName("cereal");

        final String result = formatter.format(resultSet, options);

        assertEquals("{}", result);
    }

    @Test
    public void groupByColummEmptyColumnName() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn("value");
        options.setGroupingColumnName("");

        final String result = formatter.format(resultSet, options);

        // should not be grouped
        assertEquals("[{\"name\":\"value\"}]", result);
    }

    @Test(expected = FormatException.class)
    public void groupByColummNotFound() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn("value");
        options.setGroupingColumnName("notfound");

        formatter.format(resultSet, options);
    }

    /**
     * Not ideal to have a null value translate to 'null' as a string but the org.json library does not handle null map keys.
     */
    @Test
    public void groupByColumnNullGroupingColumnValue() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("cereal");
        when(metaData.getColumnLabel(2)).thenReturn("fruit");
        when(metaData.getColumnLabel(3)).thenReturn("drink");
        when(resultSet.getObject(1)).thenReturn("Cheerios", null, null);
        when(resultSet.getObject(2)).thenReturn("banana", "blueberry", "apple");
        when(resultSet.getObject(3)).thenReturn("milk", "water", "juice");
        options.setGroupingColumnName("cereal");

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("Cheerios");
        assertEquals(1, jsonArray1.length());
        final JSONObject breakfast1 = jsonArray1.getJSONObject(0);
        assertEquals(2, breakfast1.length());
        assertEquals("banana", breakfast1.get("fruit"));
        assertEquals("milk", breakfast1.get("drink"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("null");
        assertEquals(2, jsonArray2.length());
        final JSONObject breakfast2 = jsonArray2.getJSONObject(0);
        assertEquals(2, breakfast2.length());
        assertEquals("blueberry", breakfast2.get("fruit"));
        assertEquals("water", breakfast2.get("drink"));
        final JSONObject breakfast3 = jsonArray2.getJSONObject(1);
        assertEquals(2, breakfast3.length());
        assertEquals("apple", breakfast3.get("fruit"));
        assertEquals("juice", breakfast3.get("drink"));
    }

    @Test
    public void missingRowsAdded() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2500);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(4, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(0, data2.get("numrequests"));
        assertEquals(1500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(0, data3.get("numrequests"));
        assertEquals(2000, data3.get("date"));

        final JSONObject data4 = jsonArray.getJSONObject(3);
        assertEquals(2, data4.length());
        assertEquals(8, data4.get("numrequests"));
        assertEquals(2500, data4.get("date"));
    }

    @Test
    public void missingRowsAddedDescendingOrder() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(2000, 1000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(3, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(2000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(0, data2.get("numrequests"));
        assertEquals(1500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(8, data3.get("numrequests"));
        assertEquals(1000, data3.get("date"));
    }

    @Test
    public void missingRowsAddedUnevenInterval() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        // difference between 1000 and 1900 is not a multiple of 500
        when(resultSet.getObject(2)).thenReturn(1000, 1900);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(3, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(0, data2.get("numrequests"));
        assertEquals(1500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(8, data3.get("numrequests"));
        assertEquals(1900, data3.get("date"));
    }

    /**
     * If the result set is not sorted, the results won't be ideal but we want to make sure we don't go into a continuous loop.
     */
    @Test
    public void missingRowsAddedOrderNotSorted() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8, 3);
        when(resultSet.getObject(2)).thenReturn(1000, 2500, 1500);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(6, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(0, data2.get("numrequests"));
        assertEquals(1500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(0, data3.get("numrequests"));
        assertEquals(2000, data3.get("date"));

        final JSONObject data4 = jsonArray.getJSONObject(3);
        assertEquals(2, data4.length());
        assertEquals(8, data4.get("numrequests"));
        assertEquals(2500, data4.get("date"));

        final JSONObject data5 = jsonArray.getJSONObject(4);
        assertEquals(2, data5.length());
        assertEquals(0, data5.get("numrequests"));
        assertEquals(2000, data5.get("date"));

        final JSONObject data6 = jsonArray.getJSONObject(5);
        assertEquals(2, data6.length());
        assertEquals(3, data6.get("numrequests"));
        assertEquals(1500, data6.get("date"));
    }

    @Test
    public void missingRowsAddedCanHandleZero() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        // make sure we can handle a column with zero value
        when(resultSet.getObject(2)).thenReturn(0, 1000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(3, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(0, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(0, data2.get("numrequests"));
        assertEquals(500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(8, data3.get("numrequests"));
        assertEquals(1000, data3.get("date"));
    }

    @Test
    public void missingRowsAddedCanHandleNegative() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        // make sure we can handle a column with a negative value
        when(resultSet.getObject(2)).thenReturn(-1000, 0);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(3, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(-1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(0, data2.get("numrequests"));
        assertEquals(-500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(8, data3.get("numrequests"));
        assertEquals(0, data3.get("date"));
    }

    @Test
    public void missingRowsAddedNullDefaultColumnValues() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setDefaultColumnValues(null);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(3, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(1, data2.length());
        assertEquals(1500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(8, data3.get("numrequests"));
        assertEquals(2000, data3.get("date"));
    }

    @Test
    public void missingRowsAddedEmptyDefaultColumnValues() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setDefaultColumnValues(Collections.<String, Object>emptyMap());

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(3, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(1, data2.length());
        assertEquals(1500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(8, data3.get("numrequests"));
        assertEquals(2000, data3.get("date"));
    }

    /**
     * Even if the column is not in the result set, should be inserted anyways.
     */
    @Test
    public void missingRowsAddedDefaultColumnValuesContainsColumnsNotInResultSet() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("foo", "bar");
        defaultColumnValues.put("someNumber", 5);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(3, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(3, data2.length());
        assertEquals(1500, data2.get("date"));
        assertEquals("bar", data2.get("foo"));
        assertEquals(5, data2.get("somenumber"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(8, data3.get("numrequests"));
        assertEquals(2000, data3.get("date"));
    }

    @Test
    public void missingRowsAddedDefaultColumnValuesContainsNumericColumnName() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        // if a default value is given to the column used for the missing data check, we should not use it
        defaultColumnValues.put("date", 5000);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(3, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(0, data2.get("numrequests"));
        assertEquals(1500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(8, data3.get("numrequests"));
        assertEquals(2000, data3.get("date"));
    }

    @Test
    public void missingRowsAddedToBeginning() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 1500);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(0L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(4, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(0, data1.get("numrequests"));
        assertEquals(0, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(0, data2.get("numrequests"));
        assertEquals(500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(5, data3.get("numrequests"));
        assertEquals(1000, data3.get("date"));

        final JSONObject data4 = jsonArray.getJSONObject(3);
        assertEquals(2, data4.length());
        assertEquals(8, data4.get("numrequests"));
        assertEquals(1500, data4.get("date"));
    }

    @Test
    public void missingRowsAddedToBeginningDescending() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1500, 1000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(2500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(4, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(0, data1.get("numrequests"));
        assertEquals(2500, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(0, data2.get("numrequests"));
        assertEquals(2000, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(5, data3.get("numrequests"));
        assertEquals(1500, data3.get("date"));

        final JSONObject data4 = jsonArray.getJSONObject(3);
        assertEquals(2, data4.length());
        assertEquals(8, data4.get("numrequests"));
        assertEquals(1000, data4.get("date"));
    }

    @Test
    public void missingRowsAddedToEnd() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 1500);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedEndValue(2500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(4, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals(1500, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(0, data3.get("numrequests"));
        assertEquals(2000, data3.get("date"));

        final JSONObject data4 = jsonArray.getJSONObject(3);
        assertEquals(2, data4.length());
        assertEquals(0, data4.get("numrequests"));
        assertEquals(2500, data4.get("date"));
    }

    @Test
    public void missingRowsAddedToEndDescending() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1500, 1000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedEndValue(0L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(4, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1500, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals(1000, data2.get("date"));

        final JSONObject data3 = jsonArray.getJSONObject(2);
        assertEquals(2, data3.length());
        assertEquals(0, data3.get("numrequests"));
        assertEquals(500, data3.get("date"));

        final JSONObject data4 = jsonArray.getJSONObject(3);
        assertEquals(2, data4.length());
        assertEquals(0, data4.get("numrequests"));
        assertEquals(0, data4.get("date"));
    }

    @Test
    public void doNotAddMissingRowsIfNumericColumnNameNull() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName(null);
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(2, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals(2000, data2.get("date"));
    }

    @Test
    public void doNotAddMissingRowsIfNumericColumnNameEmpty() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName("");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(2, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals(2000, data2.get("date"));
    }

    @Test
    public void doNotAddMissingRowsIfNumericColumnNameDoesNotExist() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName("doesnotexist");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(2, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals(2000, data2.get("date"));
    }

    @Test
    public void doNotAddMissingRowsIfNumericColumnNameDoesNotContainNumericValue() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn("notNumeric", "NAN");
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(0L);
        options.setExpectedEndValue(1000L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(2, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals("notNumeric", data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals("NAN", data2.get("date"));
    }

    @Test
    public void doNotAddMissingRowsIfNumericColumnNameDoesNotContainIntegerValue() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000.5, 2000.5);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(0L);
        options.setExpectedEndValue(1000L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(2, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000.5, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals(2000.5, data2.get("date"));
    }

    @Test
    public void doNotAddMissingRowsIfNumericColumnNameContainsNullValue() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(null, null);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(0L);
        options.setExpectedEndValue(1000L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(2, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(1, data1.length());
        assertEquals(5, data1.get("numrequests"));
        // json library doesn't include null values
        assertFalse(data1.has("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(1, data2.length());
        assertEquals(8, data2.get("numrequests"));
        // json library doesn't include null values
        assertFalse(data2.has("date"));
    }

    @Test
    public void doNotAddMissingRowsIfExpectedIntervalNull() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(null);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(2, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals(2000, data2.get("date"));
    }

    @Test
    public void doNotAddMissingRowsIfExpectedIntervalLessThanZero() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(-1L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(2, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals(2000, data2.get("date"));
    }

    @Test
    public void doNotAddMissingRowsIfExpectedIntervalEqualToZero() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(1000, 2000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(0L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONArray jsonArray = new JSONArray(result);
        assertEquals(2, jsonArray.length());

        final JSONObject data1 = jsonArray.getJSONObject(0);
        assertEquals(2, data1.length());
        assertEquals(5, data1.get("numrequests"));
        assertEquals(1000, data1.get("date"));

        final JSONObject data2 = jsonArray.getJSONObject(1);
        assertEquals(2, data2.length());
        assertEquals(8, data2.get("numrequests"));
        assertEquals(2000, data2.get("date"));
    }

    @Test
    public void groupByColumnAndMissingRowsAdded() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(metaData.getColumnLabel(3)).thenReturn("service");
        when(resultSet.getObject(1)).thenReturn(5, 8, 3, 7);
        when(resultSet.getObject(2)).thenReturn(1000, 2000, 1000, 2000);
        when(resultSet.getObject(3)).thenReturn("A", "A", "B", "B");
        options.setGroupingColumnName("service");
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(2, jsonObject.length());

        final JSONArray serviceA = jsonObject.getJSONArray("A");
        assertEquals(3, serviceA.length());

        final JSONObject data1a = serviceA.getJSONObject(0);
        assertEquals(2, data1a.length());
        assertEquals(5, data1a.get("numrequests"));
        assertEquals(1000, data1a.get("date"));

        final JSONObject data2a = serviceA.getJSONObject(1);
        assertEquals(2, data2a.length());
        assertEquals(0, data2a.get("numrequests"));
        assertEquals(1500, data2a.get("date"));

        final JSONObject data3a = serviceA.getJSONObject(2);
        assertEquals(2, data3a.length());
        assertEquals(8, data3a.get("numrequests"));
        assertEquals(2000, data3a.get("date"));

        final JSONArray serviceB = jsonObject.getJSONArray("B");
        assertEquals(3, serviceB.length());

        final JSONObject data1b = serviceB.getJSONObject(0);
        assertEquals(2, data1b.length());
        assertEquals(3, data1b.get("numrequests"));
        assertEquals(1000, data1b.get("date"));

        final JSONObject data2b = serviceB.getJSONObject(1);
        assertEquals(2, data2b.length());
        assertEquals(0, data2b.get("numrequests"));
        assertEquals(1500, data2b.get("date"));

        final JSONObject data3b = serviceB.getJSONObject(2);
        assertEquals(2, data3b.length());
        assertEquals(7, data3b.get("numrequests"));
        assertEquals(2000, data3b.get("date"));
    }

    @Test
    public void groupByColumnAndMissingRowsAddedToStart() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(metaData.getColumnLabel(3)).thenReturn("service");
        when(resultSet.getObject(1)).thenReturn(5, 8, 3, 7);
        when(resultSet.getObject(2)).thenReturn(1000, 1500, 1000, 1500);
        when(resultSet.getObject(3)).thenReturn("A", "A", "B", "B");
        options.setGroupingColumnName("service");
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(0L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(2, jsonObject.length());

        final JSONArray serviceA = jsonObject.getJSONArray("A");
        assertEquals(4, serviceA.length());

        final JSONObject data1a = serviceA.getJSONObject(0);
        assertEquals(2, data1a.length());
        assertEquals(0, data1a.get("numrequests"));
        assertEquals(0, data1a.get("date"));

        final JSONObject data2a = serviceA.getJSONObject(1);
        assertEquals(2, data2a.length());
        assertEquals(0, data2a.get("numrequests"));
        assertEquals(500, data2a.get("date"));

        final JSONObject data3a = serviceA.getJSONObject(2);
        assertEquals(2, data3a.length());
        assertEquals(5, data3a.get("numrequests"));
        assertEquals(1000, data3a.get("date"));

        final JSONObject data4a = serviceA.getJSONObject(3);
        assertEquals(2, data4a.length());
        assertEquals(8, data4a.get("numrequests"));
        assertEquals(1500, data4a.get("date"));

        final JSONArray serviceB = jsonObject.getJSONArray("B");
        assertEquals(4, serviceB.length());

        final JSONObject data1b = serviceB.getJSONObject(0);
        assertEquals(2, data1b.length());
        assertEquals(0, data1b.get("numrequests"));
        assertEquals(0, data1b.get("date"));

        final JSONObject data2b = serviceB.getJSONObject(1);
        assertEquals(2, data2b.length());
        assertEquals(0, data2b.get("numrequests"));
        assertEquals(500, data2b.get("date"));

        final JSONObject data3b = serviceB.getJSONObject(2);
        assertEquals(2, data3b.length());
        assertEquals(3, data3b.get("numrequests"));
        assertEquals(1000, data3b.get("date"));

        final JSONObject data4b = serviceB.getJSONObject(3);
        assertEquals(2, data4b.length());
        assertEquals(7, data4b.get("numrequests"));
        assertEquals(1500, data4b.get("date"));
    }

    @Test
    public void groupByColumnAndMissingRowsAddedToStartDescending() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(metaData.getColumnLabel(3)).thenReturn("service");
        when(resultSet.getObject(1)).thenReturn(5, 8, 3, 7);
        when(resultSet.getObject(2)).thenReturn(1500, 1000, 1500, 1000);
        when(resultSet.getObject(3)).thenReturn("A", "A", "B", "B");
        options.setGroupingColumnName("service");
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(2500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(2, jsonObject.length());

        final JSONArray serviceA = jsonObject.getJSONArray("A");
        assertEquals(4, serviceA.length());

        final JSONObject data1a = serviceA.getJSONObject(0);
        assertEquals(2, data1a.length());
        assertEquals(0, data1a.get("numrequests"));
        assertEquals(2500, data1a.get("date"));

        final JSONObject data2a = serviceA.getJSONObject(1);
        assertEquals(2, data2a.length());
        assertEquals(0, data2a.get("numrequests"));
        assertEquals(2000, data2a.get("date"));

        final JSONObject data3a = serviceA.getJSONObject(2);
        assertEquals(2, data3a.length());
        assertEquals(5, data3a.get("numrequests"));
        assertEquals(1500, data3a.get("date"));

        final JSONObject data4a = serviceA.getJSONObject(3);
        assertEquals(2, data4a.length());
        assertEquals(8, data4a.get("numrequests"));
        assertEquals(1000, data4a.get("date"));

        final JSONArray serviceB = jsonObject.getJSONArray("B");
        assertEquals(4, serviceB.length());

        final JSONObject data1b = serviceB.getJSONObject(0);
        assertEquals(2, data1b.length());
        assertEquals(0, data1b.get("numrequests"));
        assertEquals(2500, data1b.get("date"));

        final JSONObject data2b = serviceB.getJSONObject(1);
        assertEquals(2, data2b.length());
        assertEquals(0, data2b.get("numrequests"));
        assertEquals(2000, data2b.get("date"));

        final JSONObject data3b = serviceB.getJSONObject(2);
        assertEquals(2, data3b.length());
        assertEquals(3, data3b.get("numrequests"));
        assertEquals(1500, data3b.get("date"));

        final JSONObject data4b = serviceB.getJSONObject(3);
        assertEquals(2, data4b.length());
        assertEquals(7, data4b.get("numrequests"));
        assertEquals(1000, data4b.get("date"));
    }

    @Test
    public void groupByColumnAndMissingRowsAddedToEnd() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(metaData.getColumnLabel(3)).thenReturn("service");
        when(resultSet.getObject(1)).thenReturn(5, 8, 3, 7);
        when(resultSet.getObject(2)).thenReturn(1000, 1500, 1000, 1500);
        when(resultSet.getObject(3)).thenReturn("A", "A", "B", "B");
        options.setGroupingColumnName("service");
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedEndValue(2500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(2, jsonObject.length());

        final JSONArray serviceA = jsonObject.getJSONArray("A");
        assertEquals(4, serviceA.length());

        final JSONObject data1a = serviceA.getJSONObject(0);
        assertEquals(2, data1a.length());
        assertEquals(5, data1a.get("numrequests"));
        assertEquals(1000, data1a.get("date"));

        final JSONObject data2a = serviceA.getJSONObject(1);
        assertEquals(2, data2a.length());
        assertEquals(8, data2a.get("numrequests"));
        assertEquals(1500, data2a.get("date"));

        final JSONObject data3a = serviceA.getJSONObject(2);
        assertEquals(2, data3a.length());
        assertEquals(0, data3a.get("numrequests"));
        assertEquals(2000, data3a.get("date"));

        final JSONObject data4a = serviceA.getJSONObject(3);
        assertEquals(2, data4a.length());
        assertEquals(0, data4a.get("numrequests"));
        assertEquals(2500, data4a.get("date"));

        final JSONArray serviceB = jsonObject.getJSONArray("B");
        assertEquals(4, serviceB.length());

        final JSONObject data1b = serviceB.getJSONObject(0);
        assertEquals(2, data1b.length());
        assertEquals(3, data1b.get("numrequests"));
        assertEquals(1000, data1b.get("date"));

        final JSONObject data2b = serviceB.getJSONObject(1);
        assertEquals(2, data2b.length());
        assertEquals(7, data2b.get("numrequests"));
        assertEquals(1500, data2b.get("date"));

        final JSONObject data3b = serviceB.getJSONObject(2);
        assertEquals(2, data3b.length());
        assertEquals(0, data3b.get("numrequests"));
        assertEquals(2000, data3b.get("date"));

        final JSONObject data4b = serviceB.getJSONObject(3);
        assertEquals(2, data4b.length());
        assertEquals(0, data4b.get("numrequests"));
        assertEquals(2500, data4b.get("date"));
    }

    @Test
    public void groupByColumnAndMissingRowsAddedToEndDescending() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(metaData.getColumnLabel(3)).thenReturn("service");
        when(resultSet.getObject(1)).thenReturn(5, 8, 3, 7);
        when(resultSet.getObject(2)).thenReturn(1500, 1000, 1500, 1000);
        when(resultSet.getObject(3)).thenReturn("A", "A", "B", "B");
        options.setGroupingColumnName("service");
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedEndValue(0L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(2, jsonObject.length());

        final JSONArray serviceA = jsonObject.getJSONArray("A");
        assertEquals(4, serviceA.length());

        final JSONObject data1a = serviceA.getJSONObject(0);
        assertEquals(2, data1a.length());
        assertEquals(5, data1a.get("numrequests"));
        assertEquals(1500, data1a.get("date"));

        final JSONObject data2a = serviceA.getJSONObject(1);
        assertEquals(2, data2a.length());
        assertEquals(8, data2a.get("numrequests"));
        assertEquals(1000, data2a.get("date"));

        final JSONObject data3a = serviceA.getJSONObject(2);
        assertEquals(2, data3a.length());
        assertEquals(0, data3a.get("numrequests"));
        assertEquals(500, data3a.get("date"));

        final JSONObject data4a = serviceA.getJSONObject(3);
        assertEquals(2, data4a.length());
        assertEquals(0, data4a.get("numrequests"));
        assertEquals(0, data4a.get("date"));

        final JSONArray serviceB = jsonObject.getJSONArray("B");
        assertEquals(4, serviceB.length());

        final JSONObject data1b = serviceB.getJSONObject(0);
        assertEquals(2, data1b.length());
        assertEquals(3, data1b.get("numrequests"));
        assertEquals(1500, data1b.get("date"));

        final JSONObject data2b = serviceB.getJSONObject(1);
        assertEquals(2, data2b.length());
        assertEquals(7, data2b.get("numrequests"));
        assertEquals(1000, data2b.get("date"));

        final JSONObject data3b = serviceB.getJSONObject(2);
        assertEquals(2, data3b.length());
        assertEquals(0, data3b.get("numrequests"));
        assertEquals(500, data3b.get("date"));

        final JSONObject data4b = serviceB.getJSONObject(3);
        assertEquals(2, data4b.length());
        assertEquals(0, data4b.get("numrequests"));
        assertEquals(0, data4b.get("date"));
    }

    @Test
    public void groupByColummExpectedGroupings() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("cereal");
        when(metaData.getColumnLabel(2)).thenReturn("fruit");
        when(resultSet.getObject(1)).thenReturn("Cheerios", "Fruit Loops", "Cheerios");
        when(resultSet.getObject(2)).thenReturn("banana", "blueberry", "apple");
        options.setGroupingColumnName("cereal");
        options.setExpectedGroups(Arrays.asList("Cheerios", "Fruit Loops", "Special K"));

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(3, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("Cheerios");
        assertEquals(2, jsonArray1.length());
        final JSONObject breakfast1 = jsonArray1.getJSONObject(0);
        assertEquals(1, breakfast1.length());
        assertEquals("banana", breakfast1.get("fruit"));
        final JSONObject breakfast2 = jsonArray1.getJSONObject(1);
        assertEquals(1, breakfast2.length());
        assertEquals("apple", breakfast2.get("fruit"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("Fruit Loops");
        assertEquals(1, jsonArray2.length());
        final JSONObject breakfast3 = jsonArray2.getJSONObject(0);
        assertEquals(1, breakfast3.length());
        assertEquals("blueberry", breakfast3.get("fruit"));

        final JSONArray jsonArray3 = jsonObject.getJSONArray("Special K");
        assertEquals(0, jsonArray3.length());
    }

    @Test
    public void groupByColumnExpectedGroupingsAndMissingRowsAdded() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(metaData.getColumnLabel(3)).thenReturn("service");
        when(resultSet.getObject(1)).thenReturn(5, 8, 3, 7);
        when(resultSet.getObject(2)).thenReturn(1000, 1500, 1000, 1500);
        when(resultSet.getObject(3)).thenReturn("A", "A", "B", "B");
        options.setGroupingColumnName("service");
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(500L);
        options.setExpectedEndValue(2000L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);
        options.setExpectedGroups(Arrays.asList("A", "B", "C"));

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(3, jsonObject.length());

        final JSONArray serviceA = jsonObject.getJSONArray("A");
        assertEquals(4, serviceA.length());

        final JSONObject data1a = serviceA.getJSONObject(0);
        assertEquals(2, data1a.length());
        assertEquals(0, data1a.get("numrequests"));
        assertEquals(500, data1a.get("date"));

        final JSONObject data2a = serviceA.getJSONObject(1);
        assertEquals(2, data2a.length());
        assertEquals(5, data2a.get("numrequests"));
        assertEquals(1000, data2a.get("date"));

        final JSONObject data3a = serviceA.getJSONObject(2);
        assertEquals(2, data3a.length());
        assertEquals(8, data3a.get("numrequests"));
        assertEquals(1500, data3a.get("date"));

        final JSONObject data4a = serviceA.getJSONObject(3);
        assertEquals(2, data4a.length());
        assertEquals(0, data4a.get("numrequests"));
        assertEquals(2000, data4a.get("date"));

        final JSONArray serviceB = jsonObject.getJSONArray("B");
        assertEquals(4, serviceB.length());

        final JSONObject data1b = serviceB.getJSONObject(0);
        assertEquals(2, data1b.length());
        assertEquals(0, data1b.get("numrequests"));
        assertEquals(500, data1b.get("date"));

        final JSONObject data2b = serviceB.getJSONObject(1);
        assertEquals(2, data2b.length());
        assertEquals(3, data2b.get("numrequests"));
        assertEquals(1000, data2b.get("date"));

        final JSONObject data3b = serviceB.getJSONObject(2);
        assertEquals(2, data3b.length());
        assertEquals(7, data3b.get("numrequests"));
        assertEquals(1500, data3b.get("date"));

        final JSONObject data4b = serviceB.getJSONObject(3);
        assertEquals(2, data4b.length());
        assertEquals(0, data4b.get("numrequests"));
        assertEquals(2000, data4b.get("date"));

        final JSONArray serviceC = jsonObject.getJSONArray("C");
        assertEquals(4, serviceC.length());

        final JSONObject data1c = serviceC.getJSONObject(0);
        assertEquals(2, data1c.length());
        assertEquals(0, data1c.get("numrequests"));
        assertEquals(500, data1c.get("date"));

        final JSONObject data2c = serviceC.getJSONObject(1);
        assertEquals(2, data2c.length());
        assertEquals(0, data2c.get("numrequests"));
        assertEquals(1000, data2c.get("date"));

        final JSONObject data3c = serviceC.getJSONObject(2);
        assertEquals(2, data3c.length());
        assertEquals(0, data3c.get("numrequests"));
        assertEquals(1500, data3c.get("date"));

        final JSONObject data4c = serviceC.getJSONObject(3);
        assertEquals(2, data4c.length());
        assertEquals(0, data4c.get("numrequests"));
        assertEquals(2000, data4c.get("date"));
    }

    /**
     * If expected start and end are not set (but expected interval is set) we can't add missing rows for groups that are not
     * included in the result set.
     */
    @Test
    public void groupByColumnMissingRowsNotAddedForMissingGroups() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(metaData.getColumnLabel(3)).thenReturn("service");
        when(resultSet.getObject(1)).thenReturn(5, 8, 3, 7);
        when(resultSet.getObject(2)).thenReturn(1000, 2000, 1000, 2000);
        when(resultSet.getObject(3)).thenReturn("A", "A", "B", "B");
        options.setGroupingColumnName("service");
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        options.setExpectedStartValue(null);
        options.setExpectedEndValue(null);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);
        // C is not included in the result set!
        options.setExpectedGroups(Arrays.asList("A", "B", "C"));

        final String result = formatter.format(resultSet, options);

        final JSONObject jsonObject = new JSONObject(result);
        assertEquals(3, jsonObject.length());

        final JSONArray serviceA = jsonObject.getJSONArray("A");
        assertEquals(3, serviceA.length());

        final JSONObject data1a = serviceA.getJSONObject(0);
        assertEquals(2, data1a.length());
        assertEquals(5, data1a.get("numrequests"));
        assertEquals(1000, data1a.get("date"));

        final JSONObject data2a = serviceA.getJSONObject(1);
        assertEquals(2, data2a.length());
        assertEquals(0, data2a.get("numrequests"));
        assertEquals(1500, data2a.get("date"));

        final JSONObject data3a = serviceA.getJSONObject(2);
        assertEquals(2, data3a.length());
        assertEquals(8, data3a.get("numrequests"));
        assertEquals(2000, data3a.get("date"));

        final JSONArray serviceB = jsonObject.getJSONArray("B");
        assertEquals(3, serviceB.length());

        final JSONObject data1b = serviceB.getJSONObject(0);
        assertEquals(2, data1b.length());
        assertEquals(3, data1b.get("numrequests"));
        assertEquals(1000, data1b.get("date"));

        final JSONObject data2b = serviceB.getJSONObject(1);
        assertEquals(2, data2b.length());
        assertEquals(0, data2b.get("numrequests"));
        assertEquals(1500, data2b.get("date"));

        final JSONObject data3b = serviceB.getJSONObject(2);
        assertEquals(2, data3b.length());
        assertEquals(7, data3b.get("numrequests"));
        assertEquals(2000, data3b.get("date"));

        final JSONArray serviceC = jsonObject.getJSONArray("C");
        assertEquals(0, serviceC.length());
    }
}
