package com.l7tech.portal.reports.format;

import com.l7tech.portal.reports.AbstractXmlTestUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;

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
public class XmlResultSetFormatterTest extends AbstractXmlTestUtility {
    private XmlResultSetFormatter formatter;
    private ResultSetFormatOptions options;
    private Map<String, Object> defaultColumnValues;
    @Mock
    private ResultSet resultSet;
    @Mock
    private ResultSetMetaData metaData;

    @Before
    public void setup() throws Exception {
        setupAbstractXmlTest();
        formatter = new XmlResultSetFormatter("foo", "bar");
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

        assertEquals("<foo><bar><name>value</name></bar></foo>", result);
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

        assertEquals("<foo><bar><name1>value1</name1><name2>value2</name2></bar></foo>", result);
    }

    @Test
    public void multipleRowSingleColumn() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn("value1", "value2");

        final String result = formatter.format(resultSet, null);

        assertEquals("<foo><bar><name>value1</name></bar><bar><name>value2</name></bar></foo>", result);
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

        assertEquals("<foo><bar><name1>value1</name1><name2>value2</name2></bar><bar><name1>value3</name1><name2>value4</name2></bar></foo>", result);
    }

    @Test
    public void noRows() throws Exception {
        when(resultSet.next()).thenReturn(false);

        final String result = formatter.format(resultSet, null);

        assertEquals("<foo/>", result);
    }

    @Test
    public void returnsLowerCaseColumnName() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("NAME");
        when(resultSet.getObject(1)).thenReturn("value");

        final String result = formatter.format(resultSet, null);

        assertEquals("<foo><bar><name>value</name></bar></foo>", result);
    }

    @Test
    public void nullValue() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn(null);

        final String result = formatter.format(resultSet, null);

        assertEquals("<foo><bar><name/></bar></foo>", result);
    }

    @Test
    public void emptyValue() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn("");

        final String result = formatter.format(resultSet, null);

        assertEquals("<foo><bar><name/></bar></foo>", result);
    }

    @Test(expected = FormatException.class)
    public void sqlException() throws Exception {
        when(resultSet.next()).thenThrow(new SQLException("mocking exception"));

        formatter.format(resultSet, null);
    }

    @Test
    public void groupByColumn() throws Exception {
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("cereal");
        when(metaData.getColumnLabel(2)).thenReturn("fruit");
        when(metaData.getColumnLabel(3)).thenReturn("drink");
        when(resultSet.getObject(1)).thenReturn("Cheerios", "Fruit Loops", "Cheerios");
        when(resultSet.getObject(2)).thenReturn("banana", "blueberry", "apple");
        when(resultSet.getObject(3)).thenReturn("milk", "water", "juice");
        options.setGroupingColumnName("cereal");

        final String result = formatter.format(resultSet, options);

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/cereal", 2);
        assertXPathTextContent(document, "foo/cereal[@value='Cheerios']/bar[1]/fruit", "banana");
        assertXPathTextContent(document, "foo/cereal[@value='Cheerios']/bar[1]/drink", "milk");
        assertXPathTextContent(document, "foo/cereal[@value='Cheerios']/bar[2]/fruit", "apple");
        assertXPathTextContent(document, "foo/cereal[@value='Cheerios']/bar[2]/drink", "juice");
        assertXPathTextContent(document, "foo/cereal[@value='Fruit Loops']/bar[1]/fruit", "blueberry");
        assertXPathTextContent(document, "foo/cereal[@value='Fruit Loops']/bar[1]/drink", "water");
    }

    @Test
    public void groupByColumnNoRows() throws Exception {
        when(resultSet.next()).thenReturn(false);
        options.setGroupingColumnName("cereal");

        final String result = formatter.format(resultSet, options);

        assertEquals("<foo/>", result);
    }

    @Test
    public void groupByColumnEmptyColumnName() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn("value");
        options.setGroupingColumnName("");

        final String result = formatter.format(resultSet, options);

        // should not be grouped
        assertEquals("<foo><bar><name>value</name></bar></foo>", result);
    }

    @Test(expected = FormatException.class)
    public void groupByColumnNotFound() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.getObject(1)).thenReturn("value");
        options.setGroupingColumnName("notfound");

        formatter.format(resultSet, options);
    }

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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/cereal", 2);
        assertXPathTextContent(document, "foo/cereal[@value='Cheerios']/bar[1]/fruit", "banana");
        assertXPathTextContent(document, "foo/cereal[@value='Cheerios']/bar[1]/drink", "milk");
        assertXPathTextContent(document, "foo/cereal[not(@*)]/bar[1]/fruit", "blueberry");
        assertXPathTextContent(document, "foo/cereal[not(@*)]/bar[1]/drink", "water");
        assertXPathTextContent(document, "foo/cereal[not(@*)]/bar[2]/fruit", "apple");
        assertXPathTextContent(document, "foo/cereal[not(@*)]/bar[2]/drink", "juice");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 4);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[3]/date", "2000");
        assertXPathTextContent(document, "foo/bar[4]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[4]/date", "2500");
    }

    @Test
    public void missingRowsAddedDescendingOrder() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("numRequests");
        when(metaData.getColumnLabel(2)).thenReturn("date");
        when(resultSet.getObject(1)).thenReturn(5, 8);
        when(resultSet.getObject(2)).thenReturn(2500, 1000);
        options.setNumericColumnName("date");
        options.setExpectedInterval(500L);
        defaultColumnValues.put("numRequests", 0);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 4);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "2500");
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[2]/date", "2000");
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[3]/date", "1500");
        assertXPathTextContent(document, "foo/bar[4]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[4]/date", "1000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 3);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[3]/date", "1900");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 6);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[3]/date", "2000");
        assertXPathTextContent(document, "foo/bar[4]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[4]/date", "2500");
        assertXPathTextContent(document, "foo/bar[5]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[5]/date", "2000");
        assertXPathTextContent(document, "foo/bar[6]/numrequests", "3");
        assertXPathTextContent(document, "foo/bar[6]/date", "1500");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 3);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "0");
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[2]/date", "500");
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[3]/date", "1000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 3);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "-1000");
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[2]/date", "-500");
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[3]/date", "0");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 3);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 1);
        assertXPathTextContent(document, "foo/bar[2]/date", "1500");
        assertNumberOfNodes(document, "foo/bar[3]/*", 2);
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[3]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 3);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 1);
        assertXPathTextContent(document, "foo/bar[2]/date", "1500");
        assertNumberOfNodes(document, "foo/bar[3]/*", 2);
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[3]/date", "2000");
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
        defaultColumnValues.put("someString", "hello");
        defaultColumnValues.put("someNumber", 5);
        options.setDefaultColumnValues(defaultColumnValues);

        final String result = formatter.format(resultSet, options);

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 3);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 3);
        assertXPathTextContent(document, "foo/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/bar[2]/somestring", "hello");
        assertXPathTextContent(document, "foo/bar[2]/somenumber", "5");
        assertNumberOfNodes(document, "foo/bar[3]/*", 2);
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[3]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 3);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[2]/date", "1500");
        assertNumberOfNodes(document, "foo/bar[3]/*", 2);
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[3]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 4);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[1]/date", "0");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[2]/date", "500");
        assertNumberOfNodes(document, "foo/bar[3]/*", 2);
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[3]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[4]/*", 2);
        assertXPathTextContent(document, "foo/bar[4]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[4]/date", "1500");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 4);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[1]/date", "2500");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[2]/date", "2000");
        assertNumberOfNodes(document, "foo/bar[3]/*", 2);
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[3]/date", "1500");
        assertNumberOfNodes(document, "foo/bar[4]/*", 2);
        assertXPathTextContent(document, "foo/bar[4]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[4]/date", "1000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 4);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "1500");
        assertNumberOfNodes(document, "foo/bar[3]/*", 2);
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[3]/date", "2000");
        assertNumberOfNodes(document, "foo/bar[4]/*", 2);
        assertXPathTextContent(document, "foo/bar[4]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[4]/date", "2500");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 4);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1500");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[3]/*", 2);
        assertXPathTextContent(document, "foo/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[3]/date", "500");
        assertNumberOfNodes(document, "foo/bar[4]/*", 2);
        assertXPathTextContent(document, "foo/bar[4]/numrequests", "0");
        assertXPathTextContent(document, "foo/bar[4]/date", "0");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 2);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 2);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 2);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 2);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "notNumeric");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "NAN");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 2);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000.5");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "2000.5");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 2);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 2);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 2);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/bar", 2);
        assertNumberOfNodes(document, "foo/bar[1]/*", 2);
        assertXPathTextContent(document, "foo/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/bar[1]/date", "1000");
        assertNumberOfNodes(document, "foo/bar[2]/*", 2);
        assertXPathTextContent(document, "foo/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/bar[2]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/service", 2);
        assertNumberOfNodes(document, "foo/service[@value='A']/bar", 3);
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/date", "2000");
        assertNumberOfNodes(document, "foo/service[@value='B']/bar", 3);
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/numrequests", "3");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/numrequests", "7");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/service", 2);
        assertNumberOfNodes(document, "foo/service[@value='A']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/date", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/date", "500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/numrequests", "5");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/numrequests", "8");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/date", "1500");
        assertNumberOfNodes(document, "foo/service[@value='B']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/date", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/date", "500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/numrequests", "3");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/numrequests", "7");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/date", "1500");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/service", 2);
        assertNumberOfNodes(document, "foo/service[@value='A']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/date", "2500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/date", "2000");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/numrequests", "5");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/numrequests", "8");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/date", "1000");
        assertNumberOfNodes(document, "foo/service[@value='B']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/date", "2500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/date", "2000");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/numrequests", "3");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/numrequests", "7");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/date", "1000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/service", 2);
        assertNumberOfNodes(document, "foo/service[@value='A']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/date", "2000");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/date", "2500");
        assertNumberOfNodes(document, "foo/service[@value='B']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/numrequests", "3");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/numrequests", "7");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/date", "2000");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/date", "2500");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/service", 2);
        assertNumberOfNodes(document, "foo/service[@value='A']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/numrequests", "8");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/date", "500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/date", "0");
        assertNumberOfNodes(document, "foo/service[@value='B']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/numrequests", "3");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/numrequests", "7");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/date", "500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/date", "0");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/cereal", 3);
        assertNumberOfNodes(document, "foo/cereal[@value='Cheerios']/bar", 2);
        assertXPathTextContent(document, "foo/cereal[@value='Cheerios']/bar[1]/fruit", "banana");
        assertXPathTextContent(document, "foo/cereal[@value='Cheerios']/bar[2]/fruit", "apple");
        assertNumberOfNodes(document, "foo/cereal[@value='Fruit Loops']/bar", 1);
        assertXPathTextContent(document, "foo/cereal[@value='Fruit Loops']/bar[1]/fruit", "blueberry");
        assertNumberOfNodes(document, "foo/cereal[@value='Special K']", 1);
        assertNumberOfNodes(document, "foo/cereal[@value='Special K']/bar", 0);
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/service", 3);
        assertNumberOfNodes(document, "foo/service[@value='A']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/date", "500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/numrequests", "5");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[4]/date", "2000");
        assertNumberOfNodes(document, "foo/service[@value='B']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/date", "500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/numrequests", "3");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/numrequests", "7");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[4]/date", "2000");
        assertNumberOfNodes(document, "foo/service[@value='C']/bar", 4);
        assertXPathTextContent(document, "foo/service[@value='C']/bar[1]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='C']/bar[1]/date", "500");
        assertXPathTextContent(document, "foo/service[@value='C']/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='C']/bar[2]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='C']/bar[3]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='C']/bar[3]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='C']/bar[4]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='C']/bar[4]/date", "2000");
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

        final Document document = buildDocumentFromXml(result);

        assertNumberOfNodes(document, "foo/service", 3);
        assertNumberOfNodes(document, "foo/service[@value='A']/bar", 3);
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/numrequests", "5");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[1]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/numrequests", "8");
        assertXPathTextContent(document, "foo/service[@value='A']/bar[3]/date", "2000");
        assertNumberOfNodes(document, "foo/service[@value='B']/bar", 3);
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/numrequests", "3");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[1]/date", "1000");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/numrequests", "0");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[2]/date", "1500");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/numrequests", "7");
        assertXPathTextContent(document, "foo/service[@value='B']/bar[3]/date", "2000");
        assertNumberOfNodes(document, "foo/service[@value='C']", 1);
        assertNumberOfNodes(document, "foo/service[@value='C']/bar", 0);
    }
}
