package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.identity.IdentityProviderConfig;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class EditIdentityProviderWizard extends Wizard {
    static final Logger log = Logger.getLogger(EditIdentityProviderWizard.class.getName());
    private EntityHeader header = new EntityHeader();

    public EditIdentityProviderWizard(Frame parent, WizardStepPanel panel, IdentityProviderConfig iProvider) {
        super(parent, panel);
        setResizable(true);
        setTitle("Identity Provider Modification Wizard");
        setShowDescription(false);

        // store the current settings
        wizardInput = iProvider;

/*        Definition def = new DefinitionImpl();
        def.setExtensionRegistry(new PopulatedExtensionRegistry());
        wizardInput = def;
        this.addWizardListener(wizardListener);*/
        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(EditIdentityProviderWizard.this);
            }
        });

    }

    protected final JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EtchedBorder());
        buttonPanel.add(getButtonBack());
        buttonPanel.add(getButtonNext());
        buttonPanel.add(getButtonFinish());
        buttonPanel.add(getButtonCancel());
        buttonPanel.add(getButtonHelp());
        return buttonPanel;
    }

    private final WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard page has been changed.
         *
         * @param e the event describing the selection change
         */
        public void wizardSelectionChanged(WizardEvent e) {
            WizardStepPanel p = (WizardStepPanel) e.getSource();

            //todo: enable/disable buttons when page is changed.
            /*boolean enable =
              (!((p instanceof WsdlCreateOverviewPanel) ||
              (p instanceof WsdlDefinitionPanel)));

            getButtonPreview().setEnabled(enable);*/
        }

    };
}
