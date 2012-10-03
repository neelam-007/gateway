package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.parameter.MethodUsageReportParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MethodUsageReportDefinitionTest {
    public static final long START = 1;
    public static final long END = 2;
    public static final int RESOLUTION = 3;
    private MethodUsageReportDefinition definition;
    private MethodUsageReportParameters parameters;
    private List<String> methods;
    @Mock
    private PreparedStatement statement;

    @Before
    public void setup() {
        methods = new ArrayList<String>();
        methods.add("method1");
        parameters = new MethodUsageReportParameters(START, END, RESOLUTION, methods);
        definition = new MethodUsageReportDefinition(parameters, true);
    }

    @Test
    public void generateSQLQuerySingleMethod() {
        final String result = definition.generateSQLQuery();

        assertTrue(result.contains("api_method in (?)"));
    }

    @Test
    public void generateSQLQueryMultipleMethods() {
        methods.add("method2");

        final String result = definition.generateSQLQuery();

        assertTrue(result.contains("api_method in (?,?)"));
    }

    @Test
    public void setParametersSingleMethod() throws Exception {
        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "method1");
    }

    @Test
    public void setParametersMultipleMethods() throws Exception {
        methods.add("method2");
        methods.add("method3");

        definition.setSQLParams(statement);

        verify(statement).setLong(1, START);
        verify(statement).setLong(2, END);
        verify(statement).setInt(3, RESOLUTION);
        verify(statement).setString(4, "method1");
        verify(statement).setString(5, "method2");
        verify(statement).setString(6, "method3");
    }

    @Test(expected = ReportGenerationException.class)
    public void setParametersThrowsSQLException() throws Exception {
        doThrow(new SQLException()).when(statement).setString(Matchers.anyInt(), Matchers.anyString());

        definition.setSQLParams(statement);
    }
}
