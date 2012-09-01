/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.security.rbac;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
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
    private JLabel resultSizeExceeded;

    private final EntityType entityType;

    private EntityHeader selectedEntityHeader;

    private Action okAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            ok();
        }

        @Override
        public boolean isEnabled() {
            return okButton.isEnabled();
        }
    };

    public FindEntityDialog(Frame owner, EntityType entityType, final Assertion assertion) {
        super(owner, true);
        this.entityType = entityType;
        initialize(assertion);
    }

    public FindEntityDialog(JDialog owner, EntityType entityType, final Assertion assertion) throws HeadlessException {
        super(owner, true);
        this.entityType = entityType;
        initialize(assertion);
    }

    private void initialize(Assertion assertion) {
        if (entityType == EntityType.ID_PROVIDER_CONFIG && assertion == null)
            throw new IllegalStateException("assertion cannot be null for identity providers");
        final String name = entityType.getName();
        final String vowels = resources.getString("vowels");
        final String an = vowels.contains(name.substring(0,1).toLowerCase()) ? resources.getString("an") : resources.getString("a");

        //Identity provider specific text
        final String titleText;
        if(entityType == EntityType.ID_PROVIDER_CONFIG){
            titleText = assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString();
        }else{
            titleText = MessageFormat.format(resources.getString("titlePattern"), an, name);
        }
        setTitle(titleText);

        headerLabel.setText(MessageFormat.format(resources.getString("labelPattern"), an, name));
        headerLabel.setDisplayedMnemonic(resources.getString("labelAccelerator").charAt(0));
        headerLabel.setLabelFor(scrollPane);
        scrollPane.requestFocus();
        Utilities.setEnterAction(this, okAction);
        Utilities.setEscKeyStrokeDisposes(this);

        EntityHeaderSet<EntityHeader> headers;
        try {
            headers = Registry.getDefault().getRbacAdmin().findEntities(entityType.getEntityClass());
        } catch (Exception e) {
            throw new RuntimeException("Couldn't find " + entityType.getName(), e);
        }
        java.util.List<EntityHeader> sortedHeaders = new ArrayList<EntityHeader>(headers);
        Collections.sort( sortedHeaders, new ResolvingComparator<EntityHeader,String>( new Resolver<EntityHeader,String>(){
            @Override
            public String resolve( final EntityHeader key ) {
                return key.getName() == null ? "" : key.getName().toLowerCase();
            }
        }, false ) );
        resultSizeExceeded.setVisible(headers.isMaxExceeded());
        list.setModel(new DefaultComboBoxModel(sortedHeaders.toArray(new EntityHeader[sortedHeaders.size()])));
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisable();
            }
        });

        okButton.addActionListener(okAction);

        cancelButton.addActionListener(new ActionListener() {
            @Override
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

    public static void find(final EntityType whatKind, final Functions.UnaryVoid<EntityHeader> runWhenSelected, final Assertion assertion) {
        final FindEntityDialog fed = new FindEntityDialog(TopComponents.getInstance().getTopParent(), whatKind, assertion);
        fed.pack();
        Utilities.centerOnParentWindow(fed);
        DialogDisplayer.display(fed, new Runnable() {
            @Override
            public void run() {
                EntityHeader eh = fed.getSelectedEntityHeader();
                if (eh == null) return;
                runWhenSelected.call(eh);
            }
        });
    }
}
