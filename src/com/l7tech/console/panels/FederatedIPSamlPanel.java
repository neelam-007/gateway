package com.l7tech.console.panels;

import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class FederatedIPSamlPanel extends IdentityProviderStepPanel {
    private JPanel mainPanel;
    private JCheckBox emailCheckbox;
    private JCheckBox x509SubjectNameCheckbox;
    private JCheckBox domainNameCheckbox;
    private JCheckBox senderVouchesCheckbox;
    private JCheckBox holderOfKeyCheckbox;
    private JTextField nameQualifierTextField;
    private JTextField domainNameTextField;

    /**
     * Constructor
     * @param next  The next step panel
     */
    public FederatedIPSamlPanel(WizardStepPanel next) {
        super(next);
        initComponents();
    }

    /**
     *  Initialize the components
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        domainNameCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                 domainNameTextField.setEnabled(domainNameCheckbox.isSelected());
            }
        });
    }

    public String getDescription() {
        return "Select or enter the settings of SAML credentital source that can be handled by this identity provider, " +
                "such as the name idenetitier format, confirmation method etc.";
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canFinish() {
        return false;
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Configure SAML Credential Source";
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings The current value of configuration items in the wizard input object.
     * @throws IllegalArgumentException if the data provided by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        emailCheckbox.setSelected(iProviderConfig.getSamlConfig().isNameIdEmail());
        x509SubjectNameCheckbox.setSelected(iProviderConfig.getSamlConfig().isNameIdX509SubjectName());
        holderOfKeyCheckbox.setSelected(iProviderConfig.getSamlConfig().isSubjConfHolderOfKey());
        domainNameCheckbox.setSelected(iProviderConfig.getSamlConfig().isNameIdWindowsDomain());
        senderVouchesCheckbox.setSelected(iProviderConfig.getSamlConfig().isSubjConfSenderVouches());

        if (iProviderConfig.getOid() != -1) {
            nameQualifierTextField.setText(iProviderConfig.getSamlConfig().getNameQualifier());
            domainNameTextField.setText(iProviderConfig.getSamlConfig().getNameIdWindowsDomainName());
        }
        
        domainNameTextField.setEnabled(domainNameCheckbox.isSelected());

    }


    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        iProviderConfig.getSamlConfig().setNameIdEmail(emailCheckbox.isSelected());
        iProviderConfig.getSamlConfig().setNameIdX509SubjectName(x509SubjectNameCheckbox.isSelected());
        iProviderConfig.getSamlConfig().setSubjConfHolderOfKey(holderOfKeyCheckbox.isSelected());
        iProviderConfig.getSamlConfig().setSubjConfSenderVouches(senderVouchesCheckbox.isSelected());
        iProviderConfig.getSamlConfig().setNameQualifier(nameQualifierTextField.getText().trim());
        iProviderConfig.getSamlConfig().setNameIdWindowsDomain(domainNameCheckbox.isSelected());
        iProviderConfig.getSamlConfig().setNameIdWindowsDomainName(domainNameTextField.getText().trim());
    }


}
