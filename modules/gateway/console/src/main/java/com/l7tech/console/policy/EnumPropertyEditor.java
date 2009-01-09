/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.policy;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;

/** @author alex */
public class EnumPropertyEditor extends PropertyEditorSupport implements PropertyEditor {
    private Class<? extends Enum> enumClass;

    public EnumPropertyEditor(Class<? extends Enum> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public String[] getTags() {
        List<String> tags = new ArrayList<String>();
        for (Enum e : enumClass.getEnumConstants()) {
            tags.add(e.name());
        }
        return tags.toArray(new String[tags.size()]);
    }

    @Override
    public String getAsText() {
        return ((Enum)getValue()).name();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(Enum.valueOf(enumClass, text));
    }
}
