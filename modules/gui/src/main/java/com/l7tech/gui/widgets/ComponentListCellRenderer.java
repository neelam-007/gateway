package com.l7tech.gui.widgets;

import javax.swing.*;
import java.awt.*;

/**
     * A cell renderer for lists whose list items are actually components.
 */
public class ComponentListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object valueObj, int index, boolean isSelected, boolean cellHasFocus) {
        Component value = (Component)valueObj;
        Component supe = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        value.setForeground(supe.getForeground());
        value.setBackground(supe.getBackground());
        value.setFont(supe.getFont());
        return value;
    }
}
