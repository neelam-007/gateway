package com.l7tech.console.util;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/*
 * This class implements the <CODE>TableCellRenderer</CODE> with the progress bar to indicate
 * the value (0 to 100%) of a table cell.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class BarIndicator extends JProgressBar implements TableCellRenderer {

    /**
     * Constructor
     *
     * @param color  The color of the progress bar.
     */
    public BarIndicator(Color color) {
        super(JProgressBar.HORIZONTAL);
        setBorderPainted(false);
        setForeground(color);
    }

    /**
     * Constructor
     * @param min  The min. of the value allowed.
     * @param max  The max. of the value allowed.
     * @param color  The color of the progress bar.
     */
    public BarIndicator(int min, int max, Color color) {
        super(JProgressBar.HORIZONTAL, min, max);
        setBorderPainted(false);
        setForeground(color);
    }

    /**
     * Implement the interface which is used to render the table cell.
     * @param table   See description in <CODE>TableCellRenderer</CODE> class.
     * @param value   See description in <CODE>TableCellRenderer</CODE> class.
     * @param isSelected   See description in <CODE>TableCellRenderer</CODE> class.
     * @param hasFocus  See description in <CODE>TableCellRenderer</CODE> class.
     * @param row   See description in <CODE>TableCellRenderer</CODE> class.
     * @param column  See description in <CODE>TableCellRenderer</CODE> class.
     * @return Component  The component to be displayed in the table cell.
     */
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
