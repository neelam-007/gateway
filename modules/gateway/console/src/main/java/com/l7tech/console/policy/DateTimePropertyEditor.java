package com.l7tech.console.policy;

import com.l7tech.console.util.jcalendar.JDateTimeChooser;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.Date;

/**
 * Property editor for Date that supports a custom UI component.
 */
public class DateTimePropertyEditor extends JDateTimeChooser implements PropertyEditor {
    public DateTimePropertyEditor() {
        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() != null)
                    firePropertyChange(null, evt.getOldValue(), evt.getNewValue());
            }
        });
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String) {
            String s = (String) value;
            setAsText(s);
        } else if (value instanceof Date) {
            Date date = (Date) value;
            setDate(date);
        }
    }

    @Override
    public Object getValue() {
        return getDate();
    }

    @Override
    public boolean isPaintable() {
        return false;
    }

    @Override
    public void paintValue(Graphics gfx, Rectangle box) {
    }

    @Override
    public String getJavaInitializationString() {
        return "???";
    }

    @Override
    public String getAsText() {
        return null;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    @Override
    public String[] getTags() {
        return null;
    }

    @Override
    public Component getCustomEditor() {
        return this;
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }
}
