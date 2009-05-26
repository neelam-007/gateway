package com.l7tech.console.panels;

import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.ValidatedPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.util.Set;
import java.awt.*;

/**
 *
 */
public class InterfaceTagsPanel extends ValidatedPanel<Set<InterfaceTag>> {
    private JPanel mainPanel;
    private JButton createTagButton;
    private JButton deleteTagButton;
    private JButton addAddressButton;
    private JButton removeAddressButton;
    private JList tagList;
    private JList patternList;
    private JSplitPane splitPane;

    private final Set<InterfaceTag> model;

    public InterfaceTagsPanel(Set<InterfaceTag> model) {
        this.model = model;
        init();
    }

    public InterfaceTagsPanel(String propertyName, Set<InterfaceTag> model) {
        super(propertyName);
        this.model = model;
        init();
    }

    protected Set<InterfaceTag> getModel() {
        return model;
    }

    protected void initComponents() {
        Utilities.deuglifySplitPane(splitPane);
        Utilities.equalizeButtonSizes(createTagButton, deleteTagButton, addAddressButton, removeAddressButton);

        final DefaultListCellRenderer cellRenderer = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof InterfaceTag) {
                    InterfaceTag tag = (InterfaceTag) value;
                    value = tag.getName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        };
        tagList.setCellRenderer(cellRenderer);
        tagList.setListData(model.toArray());
        tagList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                populatePatternList(getSelectedTag());
            }
        });
        populatePatternList(null);

        add(mainPanel, BorderLayout.CENTER);
    }

    private InterfaceTag getSelectedTag() {
        return (InterfaceTag)tagList.getSelectedValue();
    }

    private void populatePatternList(InterfaceTag tag) {
        if (tag != null)
            patternList.setListData(tag.getIpPatterns().toArray());
        else
            patternList.setListData(new Object[0]);
    }

    public void focusFirstComponent() {
        tagList.requestFocus();
    }

    protected void doUpdateModel() {
        // TODO
    }
}
