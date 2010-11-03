package com.l7tech.console.panels;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.NamespaceMigrator;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.xml.soap.SOAPConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MigrateNamespacesDialog extends JDialog {
    private static final String TITLE = "Migrate Namespaces";

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox originalNamespaceComboBox;
    private JComboBox newNamespaceComboBox;

    // Some convenient URIs to make available in the combobox to save some typing
    private static final String[] DEFAULT_NAMESPACE_URIS = new String[] {
            SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE,
            SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE,
    };

    public MigrateNamespacesDialog(Window owner, final Collection<AssertionTreeNode> nodesToMigrate, String prepopulatedFromUri, String prepopulatedToUri) {
        super(owner, TITLE, ModalityType.DOCUMENT_MODAL);
        if (nodesToMigrate == null) throw new NullPointerException();
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);

        InputValidator inputValidator = new InputValidator(this, TITLE);

        originalNamespaceComboBox.setModel(new DefaultComboBoxModel(DEFAULT_NAMESPACE_URIS));
        newNamespaceComboBox.setModel(new DefaultComboBoxModel(DEFAULT_NAMESPACE_URIS));

        if (prepopulatedFromUri != null)
            originalNamespaceComboBox.setSelectedItem(prepopulatedFromUri);
        if (prepopulatedToUri != null)
            newNamespaceComboBox.setSelectedItem(prepopulatedToUri);

        inputValidator.attachToButton(buttonOK, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object origItem = originalNamespaceComboBox.getSelectedItem();
                Object newItem = newNamespaceComboBox.getSelectedItem();
                if (origItem == null || newItem == null)
                    return;
                performMigration(nodesToMigrate, origItem.toString(), newItem.toString());
                dispose();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        inputValidator.addRule(new UriComboboxValidationRule(originalNamespaceComboBox, "original namespace"));
        inputValidator.addRule(new UriComboboxValidationRule(newNamespaceComboBox, "new namespace"));        
    }

    private static class UriComboboxValidationRule extends InputValidator.ComponentValidationRule {
        private JComboBox combobox;
        private String name;

        private UriComboboxValidationRule(JComboBox component, String name) {
            super(component);
            this.combobox = component;
            this.name = name;
        }

        @Override
        public String getValidationError() {
            Object item = combobox.getSelectedItem();
            if (item == null || item.toString() == null || item.toString().length() < 1)
                return "A URI must be provided for " + name + ".";
            if (!ValidationUtils.isValidUri(item.toString()))
                return "The URI must be a valid URI for " + name + ".";
            return null;
        }
    }

    private void performMigration(Collection<AssertionTreeNode> nodesToMigrate, String oldNs, String newNs) {
        JTree policyTree = TopComponents.getInstance().getPolicyTree();
        if (policyTree == null)
            return;
        PolicyTreeModel model = (PolicyTreeModel)policyTree.getModel();

        Map<String, String> map = new HashMap<String, String>();
        map.put(oldNs, newNs);
        NamespaceMigrator migrator = new NamespaceMigrator(map);
        for (AssertionTreeNode node : nodesToMigrate) {
            Assertion assertion = node.asAssertion();
            if (assertion != null) {
                migrator.migrate(assertion);
                if (model != null)
                    model.assertionTreeNodeChanged(node);
            }
        }
    }
}
