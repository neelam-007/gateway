package com.l7tech.console.panels;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

/**
 * This was created: 12/6/12 as 11:43 AM
 *
 * @author Victor Kazakov
 */

@Ignore("Cannot test UI components.")
public class TargetVariablePanelTest {
    TargetVariablePanel testPanel;

    @Before
    public void setUp() throws Exception {
        testPanel = new TargetVariablePanel();
    }


    @Test
    public void testNewVariable() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var.f");
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK", message);
    }

    @Test
    public void testExistingVariable() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var.c");
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK (Overwrite)", message);
    }

    @Test
    public void testExistingVariableDifferentCase() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var.D");
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK (Overwrite)", message);
    }

    @Test
    public void testExistingVariableWithSuffix() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var");
        testPanel.setSuffixes(new String[]{"d"});
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK (Overwrite)", message);
    }

    @Test
    public void testExistingVariableWithSuffixExtra() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var");
        testPanel.setSuffixes(new String[]{"d", "z"});
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK (Overwrite)", message);
    }

    @Test
    public void testExistingVariableDifferentCaseWithSuffix() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var");
        testPanel.setSuffixes(new String[]{"D"});
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK (Overwrite)", message);
    }

    @Test
    public void testEmptyAccepted() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("");
        testPanel.setAcceptEmpty(true);
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK", message);
    }

    @Test
    public void testEmptyNotAccepted() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("");
        testPanel.setAcceptEmpty(false);
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "Invalid Syntax", message);
    }

    @Test
    public void testExistingPredefinedUnsettable() {
        testPanel.setVariable("request.elapsedTime");
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "Built-in, not settable", message);
    }

    @Test
    public void testExistingPredefinedSettable() {
        testPanel.setVariable("auditLevel");
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK (Built-in, settable)", message);
    }

    @Test
    public void testNewVariableReadOnly() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var.f");
        testPanel.setValueWillBeRead(true);
        testPanel.setValueWillBeWritten(false);
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "No such variable", message);
    }

    @Test
    public void testExistingVariableReadOnly() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var.c");
        testPanel.setValueWillBeRead(true);
        testPanel.setValueWillBeWritten(false);
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK", message);
    }

    @Test
    public void testExistingVariableDifferentCaseReadOnly() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var.D");
        testPanel.setValueWillBeRead(true);
        testPanel.setValueWillBeWritten(false);
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK", message);
    }

    @Test
    public void testExistingVariableWithSuffixReadOnly() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var", "my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var");
        testPanel.setSuffixes(new String[]{"d"});
        testPanel.setValueWillBeRead(true);
        testPanel.setValueWillBeWritten(false);
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK (New Prefix)", message);
    }

    @Test
    public void testExistingVariableWithSuffixMissingReadOnly() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var", "my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var");
        testPanel.setSuffixes(new String[]{"d", "z"});
        testPanel.setValueWillBeRead(true);
        testPanel.setValueWillBeWritten(false);
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "No such variable", message);
    }

    @Test
    public void testExistingVariableDifferentCaseWithSuffixReadOnly() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var", "my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));

        testPanel.setVariable("my.var");
        testPanel.setSuffixes(new String[]{"D"});
        testPanel.setValueWillBeRead(true);
        testPanel.setValueWillBeWritten(false);
        String message = testPanel.getMessage();

        Assert.assertEquals("Unexpected Message Received", "OK (New Prefix)", message);
    }

    @Test
    public void testContextVariableNotAllowed() {
        testPanel.setPredecessorVariables(new HashSet<>(Arrays.asList(new String[]{"my.var", "my.var.a", "my.var.b", "my.var.c", "my.var.d", "my.var.e"})));
        testPanel.setAllowContextVariable(false);
        testPanel.setVariable("${abc}");
        testPanel.setValueWillBeRead(true);
        testPanel.setValueWillBeWritten(false);
        String message = testPanel.getMessage();
        Assert.assertEquals("Unexpected Message Received", "Invalid Syntax", message);
    }
}
