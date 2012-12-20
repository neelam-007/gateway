package com.l7tech.console.panels;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyEditor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EditRowBasedAssertionPropertiesEditorTest {
    private EditRowBasedAssertionPropertiesEditor.EditRow editRow;
    @Mock
    private EditRowBasedAssertionPropertiesEditor.PropertyInfo propInfo;
    @Mock
    private PropertyEditor editor;
    @Mock
    private Component component;
    @Mock
    private JTextComponent textComponent;

    @Before
    public void setup() {
        editRow = new EditRowBasedAssertionPropertiesEditor.EditRow(propInfo, editor, component);
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
}
