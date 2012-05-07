package com.l7tech.console.panels;

import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Test;

import static org.junit.Assert.*;

public class AssertionInfoDialogTest {
    private VariableMetadata[] variablesSet;

    @Test
    public void getVariableTableDataNotMultivalued() {
        variablesSet = new VariableMetadata[]{new VariableMetadata("var", false, false, "var", false, DataType.STRING)};

        final Object[][] variableTableData = AssertionInfoDialog.getVariableTableData(variablesSet);

        assertEquals(1, variableTableData.length);
        assertEquals("var", variableTableData[0][0]);
        assertEquals(DataType.STRING.getShortName(), variableTableData[0][1]);
        assertEquals("no", variableTableData[0][2]);
    }

    @Test
    public void getVariableTableDataMultivalued() {
        variablesSet = new VariableMetadata[]{new VariableMetadata("var", false, true, "var", false, DataType.STRING)};

        final Object[][] variableTableData = AssertionInfoDialog.getVariableTableData(variablesSet);

        assertEquals(1, variableTableData.length);
        assertEquals("var", variableTableData[0][0]);
        assertEquals(DataType.STRING.getShortName(), variableTableData[0][1]);
        assertEquals("yes", variableTableData[0][2]);
    }

    @Test
    public void getVariableTableDataMultivaluedUnknown() {
        variablesSet = new VariableMetadata[]{new VariableMetadata("var", false, null, "var", false, DataType.STRING)};

        final Object[][] variableTableData = AssertionInfoDialog.getVariableTableData(variablesSet);

        assertEquals(1, variableTableData.length);
        assertEquals("var", variableTableData[0][0]);
        assertEquals(DataType.STRING.getShortName(), variableTableData[0][1]);
        assertEquals("unknown", variableTableData[0][2]);
    }

    @Test
    public void getVariableTableDataMultiple() {
        variablesSet = new VariableMetadata[]{new VariableMetadata("var1", false, false, "var1", false, DataType.STRING),
                new VariableMetadata("var2", false, true, "var2", false, DataType.BOOLEAN),
                new VariableMetadata("var3", false, null, "var3", false, DataType.DECIMAL)};

        final Object[][] variableTableData = AssertionInfoDialog.getVariableTableData(variablesSet);

        assertEquals(3, variableTableData.length);
        assertEquals("var1", variableTableData[0][0]);
        assertEquals(DataType.STRING.getShortName(), variableTableData[0][1]);
        assertEquals("no", variableTableData[0][2]);
        assertEquals("var2", variableTableData[1][0]);
        assertEquals(DataType.BOOLEAN.getShortName(), variableTableData[1][1]);
        assertEquals("yes", variableTableData[1][2]);
        assertEquals("var3", variableTableData[2][0]);
        assertEquals(DataType.DECIMAL.getShortName(), variableTableData[2][1]);
        assertEquals("unknown", variableTableData[2][2]);
    }
}
