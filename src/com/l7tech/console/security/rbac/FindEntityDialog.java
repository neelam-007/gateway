/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.security.rbac;

import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author alex
 */
public class FindEntityDialog extends JDialog {
    private JButton okButton;
    private JPanel mainPanel;
    private JButton cancelButton;
    private JList list;

    private final EntityType entityType;

    private EntityHeader selectedEntityHeader;

    public FindEntityDialog(Frame owner, EntityType entityType) {
        super(owner, "Select a " + entityType.getName(), true);
        this.entityType = entityType;
        initialize();
    }

    public FindEntityDialog(JDialog owner, EntityType entityType) throws HeadlessException {
        super(owner, "Select a " + entityType.getName(), true);
        this.entityType = entityType;
        initialize();
    }

    private void initialize() {
        EntityHeader[] headers;
        try {
            headers = Registry.getDefault().getRbacAdmin().findEntities(entityType.getEntityClass());
        } catch (Exception e) {
            throw new RuntimeException("Couldn't find " + entityType.getName(), e);
        }
        list.setModel(new DefaultComboBoxModel(headers));
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableDisable();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        okButton.setDefaultCapable(true);

        enableDisable();

        add(mainPanel);
    }

    private void enableDisable() {
        okButton.setEnabled(list.getSelectedValue() != null);
    }

    void ok() {
        selectedEntityHeader = (EntityHeader)list.getSelectedValue();
        dispose();
    }

    void cancel() {
        selectedEntityHeader = null;
        dispose();
    }


    public EntityHeader getSelectedEntityHeader() {
        return selectedEntityHeader;
    }
}
