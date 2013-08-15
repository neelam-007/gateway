package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.saml2attributequery.Saml2AttributeQueryAssertion;
import com.l7tech.external.assertions.saml2attributequery.SamlToLdapMap;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.Goid;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14-Jan-2009
 * Time: 9:22:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2AttributeQueryLdapServerPanel extends WizardStepPanel {
    private static class LdapServerEntry {
        private Goid goid;
        private String name;

        public LdapServerEntry(Goid goid, String name) {
            this.goid = goid;
            this.name = name;
        }

        public String toString() {
            return name;
        }

        public Goid getGoid() {
            return goid;
        }
    }

    private JPanel mainPanel;
    private JComboBox ldapServerDropdown;
    private JTextField idField;
    private JTextField idContextVariableField;
    private JComboBox mappingComboBox;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;

    public Saml2AttributeQueryLdapServerPanel(WizardStepPanel nextStep, boolean readonly) {
        super(nextStep, readonly);

        initialize();
    }

    private void initialize() {
        try {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            if(Registry.getDefault() != null && Registry.getDefault().getIdentityAdmin() != null) {
                IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
                for(EntityHeader entityHeader : identityAdmin.findAllIdentityProviderConfig()) {
                    IdentityProviderConfig cfg = identityAdmin.findIdentityProviderConfigByID(entityHeader.getGoid());
                    if (IdentityProviderType.fromVal(cfg.getTypeVal()) == IdentityProviderType.LDAP) {
                        model.addElement(new LdapServerEntry(entityHeader.getGoid(), entityHeader.getName()));
                    }
                }
            }
            ldapServerDropdown.setModel(model);
        } catch(FindException e) {
        }

        DocumentListener documentListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                Saml2AttributeQueryLdapServerPanel.this.notifyListeners();
            }

            public void insertUpdate(DocumentEvent evt) {
                Saml2AttributeQueryLdapServerPanel.this.notifyListeners();
            }

            public void removeUpdate(DocumentEvent evt) {
                Saml2AttributeQueryLdapServerPanel.this.notifyListeners();
            }
        };

        idField.getDocument().addDocumentListener(documentListener);
        idContextVariableField.getDocument().addDocumentListener(documentListener);

        final ClusterStatusAdmin clusterAdmin = Registry.getDefault().getClusterStatusAdmin();
        final DefaultComboBoxModel model = new DefaultComboBoxModel();
        mappingComboBox.setModel(model);
        try {
            for(ClusterProperty property : clusterAdmin.getAllProperties()) {
                if(property.getName().startsWith(SamlToLdapMap.PREFIX)) {
                    model.addElement(property.getName().substring(SamlToLdapMap.PREFIX.length() + 1));
                }
            }
        } catch(FindException fe) {
        }

        mappingComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                Saml2AttributeQueryLdapServerPanel.this.notifyListeners();
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SamlToLdapAttributeMapDialog dialog = new SamlToLdapAttributeMapDialog(Saml2AttributeQueryLdapServerPanel.this.getOwner(),
                        true,
                        null,
                        new SamlToLdapMap(),
                        Saml2AttributeQueryLdapServerPanel.this.isReadOnly());
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    SamlToLdapMap map = dialog.getMap();
                    String name = SamlToLdapMap.PREFIX + "." + dialog.getMappingName();
                    ClusterProperty property = new ClusterProperty(name, map.asString());

                    try {
                        clusterAdmin.saveProperty(property);
                        model.addElement(dialog.getMappingName());
                        mappingComboBox.setSelectedItem(dialog.getMappingName());

                        Saml2AttributeQueryLdapServerPanel.this.notifyListeners();
                    } catch(Exception e) {
                    }
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    ClusterProperty property = clusterAdmin.findPropertyByName(SamlToLdapMap.PREFIX + "." + mappingComboBox.getSelectedItem());
                    clusterAdmin.deleteProperty(property);
                    model.removeElement(mappingComboBox.getSelectedItem());

                    Saml2AttributeQueryLdapServerPanel.this.notifyListeners();
                } catch(Exception e) {
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    ClusterProperty property = clusterAdmin.findPropertyByName(SamlToLdapMap.PREFIX + "." + mappingComboBox.getSelectedItem());
                    SamlToLdapMap map = new SamlToLdapMap(property.getValue());

                    SamlToLdapAttributeMapDialog dialog = new SamlToLdapAttributeMapDialog(Saml2AttributeQueryLdapServerPanel.this.getOwner(),
                            true,
                            property.getName().substring(SamlToLdapMap.PREFIX.length() + 1),
                            map,
                            Saml2AttributeQueryLdapServerPanel.this.isReadOnly());
                    dialog.setVisible(true);

                    if(dialog.isConfirmed()) {
                        property.setName(SamlToLdapMap.PREFIX + "." + dialog.getMappingName());
                        property.setValue(dialog.getMap().asString());
                        clusterAdmin.saveProperty(property);

                        if(!dialog.getMappingName().equals(mappingComboBox.getSelectedItem())) {
                            mappingComboBox.removeItem(mappingComboBox.getSelectedItem());
                            mappingComboBox.addItem(dialog.getMappingName());
                            mappingComboBox.setSelectedItem(dialog.getMappingName());
                        }

                        Saml2AttributeQueryLdapServerPanel.this.notifyListeners();
                    }
                } catch(Exception e) {
                }
            }
        });

        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        if(!(settings instanceof Saml2AttributeQueryAssertion)) {
            throw new IllegalArgumentException();
        }

        Saml2AttributeQueryAssertion assertion = (Saml2AttributeQueryAssertion)settings;

        Goid ldapProviderOid = assertion.getLdapProviderOid();
        for(int i = 0;i < ldapServerDropdown.getItemCount();i++) {
            LdapServerEntry entry = (LdapServerEntry)ldapServerDropdown.getItemAt(i);
            if(entry.getGoid().equals( ldapProviderOid)) {
                ldapServerDropdown.setSelectedIndex(i);
                break;
            }
        }

        if(assertion.getIdFieldName() == null) {
            idField.setText("");
        } else {
            idField.setText(assertion.getIdFieldName());
        }

        if(assertion.getIdContextVariable() == null) {
            idContextVariableField.setText("");
        } else {
            idContextVariableField.setText(assertion.getIdContextVariable());
        }

        if(assertion.getMapClusterProperty() != null) {
            mappingComboBox.setSelectedItem(SamlToLdapMap.PREFIX + "." + assertion.getMapClusterProperty());
        }
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        if(!(settings instanceof Saml2AttributeQueryAssertion)) {
            throw new IllegalArgumentException();
        }

        Saml2AttributeQueryAssertion assertion = (Saml2AttributeQueryAssertion)settings;

        LdapServerEntry entry = (LdapServerEntry)ldapServerDropdown.getSelectedItem();
        if(entry != null) {
            assertion.setLdapProviderOid(entry.getGoid());
        }

        assertion.setIdFieldName(idField.getText());
        assertion.setIdContextVariable(idContextVariableField.getText());

        assertion.setMapClusterProperty(SamlToLdapMap.PREFIX + "." + mappingComboBox.getSelectedItem());
    }

    public String getStepLabel() {
        return "LDAP Server";
    }

    public boolean onNextButton() {
        return canAdvance();
    }

    public boolean canAdvance() {
        return ldapServerDropdown.getSelectedItem() != null &&
                idField.getText().trim().length() > 0 &&
                idContextVariableField.getText().trim().length() > 0 &&
                mappingComboBox.getSelectedItem() != null;
    }
}
