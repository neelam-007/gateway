/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.policy;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.util.List;
import java.util.ArrayList;

/** @author alex */
public class EnumPropertyEditor<E extends Enum> extends PropertyEditorSupport implements PropertyEditor {
    private Class<E> enumClass;

    public EnumPropertyEditor(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public String[] getTags() {
        List<String> tags = new ArrayList<String>();
        for (E e : enumClass.getEnumConstants()) {
            tags.add(e.name());
        }
        return tags.toArray(new String[0]);
    }

    @Override
    public String getAsText() {
        return ((E)getValue()).name();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(Enum.valueOf(enumClass, text));
    }
}
