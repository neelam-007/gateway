package com.l7tech.gui.widgets;

import javax.swing.*;

/**
 * Companion ListSelectionModel for JCheckBoxListModel.  Sets the armed state when
 * a checkbox is being toggled.
 */
public class JCheckBoxListSelectionModel extends DefaultListSelectionModel {
    private final JCheckBoxListModelAware jCheckBoxListModelAware;

    public JCheckBoxListSelectionModel(JCheckBoxListModelAware jCheckBoxListModelAware) {
        this.jCheckBoxListModelAware = jCheckBoxListModelAware;
    }

    @Override
    public void setSelectionInterval(int index0, int index1) {
        super.setSelectionInterval(index0, index1);
        jCheckBoxListModelAware.arm(index0);
    }
}
