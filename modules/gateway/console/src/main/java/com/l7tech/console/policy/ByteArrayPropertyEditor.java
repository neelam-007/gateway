package com.l7tech.console.policy;

import com.l7tech.util.HexUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

/**
 * Property editor that can edit binary values as a hex string.
 */
public class ByteArrayPropertyEditor extends PropertyEditorSupport {
    @Override
    public String getAsText() {
        byte[] value = (byte[])getValue();
        return value == null ? null : HexUtils.hexDump(value);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setValue(text == null ? null : HexUtils.unHexDump(text));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
