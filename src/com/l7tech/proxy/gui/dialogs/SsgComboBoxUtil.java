/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;

import javax.swing.*;
import java.awt.*;

/**
 * Cell Renderer for the Trusted SSG combo box.
 */
class SsgComboBoxUtil {
    private static final DefaultListCellRenderer LIST_CELL_RENDERER = new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (!(value instanceof Ssg))
                return c;
            Ssg g = (Ssg) value;

            if(g != null) {
                String u = g.getUsername() == null || g.getUsername().length() < 1
                        ? ""
                        : " (" + g.getUsername() + ")";
                setText(g.getLocalEndpoint() + ": " + g.getSsgAddress() + u);
            }
            return c;
        }
    };

    public static ListCellRenderer getListCellRenderer() {
        return LIST_CELL_RENDERER;
    }

    /**
     * Populate the given combo box with each trusted Ssg known to the Ssg finder, omitting those whose
     * local endpoints match that of the specified Ssg.
     *
     * @param comboBox  the combo box to populate
     * @param ssgFinder the SsgFinder from which to populate it
     * @param ssg the Ssg currently being edited
     */
    public static void populateSsgList(JComboBox comboBox, SsgFinder ssgFinder, Ssg ssg) {
        comboBox.setRenderer(getListCellRenderer());
        java.util.List sslList = ssgFinder.getSsgList();

        //clear the list first
        comboBox.removeAllItems();

        int i = 0;
        for (; i < sslList.size(); i++) {
            Ssg item = (Ssg) sslList.get(i);
            if (!item.isFederatedGateway() && !item.getLocalEndpoint().equals(ssg.getLocalEndpoint())) {
                comboBox.addItem(item);
            }
        }
    }
}
