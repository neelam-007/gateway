/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.security.rbac;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class FindEntityDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.security.rbac.FindEntityDialog");

    private JButton okButton;
    private JPanel mainPanel;
    private JButton cancelButton;
    private JList list;
    private JLabel headerLabel;
    private JScrollPane scrollPane;

    private final EntityType entityType;

    private EntityHeader selectedEntityHeader;
    private Action okAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            ok();
        }

        @Override
        public boolean isEnabled() {
            return okButton.isEnabled();
        }
    };

    public FindEntityDialog(Frame owner, EntityType entityType) {
        super(owner, true);
        this.entityType = entityType;
        initialize();
    }

    public FindEntityDialog(JDialog owner, EntityType entityType) throws HeadlessException {
        super(owner, true);
        this.entityType = entityType;
        initialize();
    }

    private void initialize() {
        final String name = entityType.getName();
        final String vowels = resources.getString("vowels");
        final String an = vowels.contains(name.substring(0,1).toLowerCase()) ? resources.getString("an") : resources.getString("a");

        setTitle(MessageFormat.format(resources.getString("titlePattern"), an, name));

        headerLabel.setText(MessageFormat.format(resources.getString("labelPattern"), an, name));
        headerLabel.setDisplayedMnemonic(resources.getString("labelAccelerator").charAt(0));
        headerLabel.setLabelFor(scrollPane);
        scrollPane.requestFocus();
        Utilities.setEnterAction(this, okAction);
        Utilities.setEscKeyStrokeDisposes(this);

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

        okButton.addActionListener(okAction);

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

    public static void find(final EntityType whatKind, final Functions.UnaryVoid<EntityHeader> runWhenSelected) {
        final FindEntityDialog fed = new FindEntityDialog(TopComponents.getInstance().getTopParent(), whatKind);
        Utilities.centerOnParent(fed);
        fed.pack();
        DialogDisplayer.display(fed, new Runnable() {
            public void run() {
                EntityHeader eh = fed.getSelectedEntityHeader();
                if (eh == null) return;
                runWhenSelected.call(eh);
            }
        });
    }
}
