package com.l7tech.console.util;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */


public class BarIndicator extends JProgressBar implements TableCellRenderer {

    public BarIndicator(Color color) {
        super(JProgressBar.HORIZONTAL);
        setBorderPainted(false);
        setForeground(color);
    }

    public BarIndicator(int min, int max, Color color) {
        super(JProgressBar.HORIZONTAL, min, max);
        setBorderPainted(false);
        setForeground(color);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        int n = 0;
        if (value instanceof Number) {
            n = ((Number) value).intValue();
        } else {
            String str;
            if (value instanceof String) {
                str = (String) value;
            } else {
                str = value.toString();
            }
            try {
                n = Integer.valueOf(str).intValue();
            } catch (NumberFormatException ex) {
            }
        }

        setValue(n);
        return this;
    }

}
