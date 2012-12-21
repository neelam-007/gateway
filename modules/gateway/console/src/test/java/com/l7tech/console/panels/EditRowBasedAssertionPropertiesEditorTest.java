package com.l7tech.console.panels;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyEditor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EditRowBasedAssertionPropertiesEditorTest {
    private EditRowBasedAssertionPropertiesEditor.EditRow editRow;
    private EditRowBasedAssertionPropertiesEditor.ComboBoxEditRow comboRow;
    private JComboBox combo;
    @Mock
    private EditRowBasedAssertionPropertiesEditor.PropertyInfo propInfo;
    @Mock
    private PropertyEditor editor;
    @Mock
    private Component component;

    @Before
    public void setup() {
        combo = new JComboBox<String>();
        editRow = new EditRowBasedAssertionPropertiesEditor.EditRow(propInfo, editor, component);
        comboRow = new EditRowBasedAssertionPropertiesEditor.ComboBoxEditRow(propInfo, editor, combo);
    }

    @Test
    public void editRowGetViewValue() throws Exception {
        final String value = "value";
        when(editor.getValue()).thenReturn(value);
        assertEquals(value, editRow.getViewValue());
    }

    @Test(expected = EditRowBasedAssertionPropertiesEditor.BadViewValueException.class)
    public void editRowGetViewValueNull() throws Exception {
        final String name = "name";
        when(propInfo.getName()).thenReturn(name);
        try {
            editRow.getViewValue();
            fail("expected BadViewValueException ");
        } catch (final EditRowBasedAssertionPropertiesEditor.BadViewValueException e) {
            // expected
            assertEquals(name, e.getPropertyName());
            throw e;
        }
    }

    @Test(expected = EditRowBasedAssertionPropertiesEditor.BadViewValueException.class)
    public void editRowGetViewValueIllegalArgument() {
        final String name = "name";
        when(propInfo.getName()).thenReturn(name);
        when(editor.getValue()).thenThrow(new IllegalArgumentException("mocking exception"));
        try {
            editRow.getViewValue();
            fail("expected BadViewValueException ");
        } catch (final EditRowBasedAssertionPropertiesEditor.BadViewValueException e) {
            // expected
            assertEquals(name, e.getPropertyName());
            throw e;
        }
    }

    @Test
    public void comboRowGetViewValue() {
        final String value = "value";
        combo.addItem(value);
        combo.setSelectedItem(value);
        when(editor.getValue()).thenReturn(value);
        assertEquals(value, comboRow.getViewValue());
    }

    @Test(expected = EditRowBasedAssertionPropertiesEditor.BadViewValueException.class)
    public void comboRowGetViewValueNullSelected() {
        final String name = "name";
        when(propInfo.getName()).thenReturn(name);
        combo.setSelectedItem(null);
        try {
            comboRow.getViewValue();
            fail("expected BadViewValueException ");
        } catch (final EditRowBasedAssertionPropertiesEditor.BadViewValueException e) {
            // expected
            assertEquals(name, e.getPropertyName());
            throw e;
        }
    }

    @Test(expected = EditRowBasedAssertionPropertiesEditor.BadViewValueException.class)
    public void comboRowGetViewValueIllegalArgument() {
        final String value = "value";
        final String name = "name";
        combo.addItem(value);
        combo.setSelectedItem(value);
        when(propInfo.getName()).thenReturn(name);
        when(editor.getValue()).thenThrow(new IllegalArgumentException("mocking exception "));
        try {
            comboRow.getViewValue();
            fail("expected BadViewValueException ");
        } catch (final EditRowBasedAssertionPropertiesEditor.BadViewValueException e) {
            // expected
            assertEquals(name, e.getPropertyName());
            throw e;
        }
    }
}
