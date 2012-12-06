package com.l7tech.console.panels;

import com.l7tech.console.tree.AbstractPaletteFolderNode;
import com.l7tech.console.tree.AssertionsPaletteRootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class EncapsulatedAssertionConfigPropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameField;
    private JComboBox paletteFolderComboBox;
    private JButton changePolicyButton;
    private JLabel policyNameLabel;
    private JButton selectIconButton;
    private JLabel iconLabel;

    private final EncapsulatedAssertionConfig config;
    private final boolean readOnly;
    private final InputValidator inputValidator = new InputValidator(this, getTitle());
    private Policy policy;
    private boolean confirmed = false;

    /**
     * Create a new properties dialog that will show info for the specified bean,
     * and which will edit the specified bean in-place if the Ok button is successfully activated.
     *
     * @param parent the parent window.  Should be specified to avoid focus/visibility issues.
     * @param config the bean to edit.  Required.
     * @param readOnly if true, the Ok button will be disabled.
     */
    public EncapsulatedAssertionConfigPropertiesDialog(Window parent, EncapsulatedAssertionConfig config, boolean readOnly) {
        super(parent, "Encapsulated Assertion Configuration", ModalityType.APPLICATION_MODAL);
        this.config = config;
        this.readOnly = readOnly;
        init();
    }

    private void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        cancelButton.addActionListener(Utilities.createDisposeAction(this));

        inputValidator.constrainTextFieldToBeNonEmpty("name", nameField, null);
        inputValidator.addRule(new InputValidator.ComponentValidationRule(changePolicyButton) {
            @Override
            public String getValidationError() {
                return policy != null ? null
                    : "An implementation policy must be specified.";
            }
        });
        inputValidator.addRule(new InputValidator.ComponentValidationRule(paletteFolderComboBox) {
            @Override
            public String getValidationError() {
                String folderName = (String) paletteFolderComboBox.getSelectedItem();
                return folderName != null && folderName.trim().length() > 0 ? null
                    : "A palette folder must be specified.";
            }
        });

        inputValidator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!readOnly) {
                    updateBean();
                    confirmed = true;
                }
                dispose();
            }
        });

        changePolicyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doChangePolicy();
            }
        });
        final OkCancelDialog okCancelDialog = new OkCancelDialog(this, "Icon", true, new IconSelectorDialog());
        selectIconButton.addActionListener(new IconActionListener(okCancelDialog));
        iconLabel.setIcon((ImageIcon)okCancelDialog.getValue());

        okButton.setEnabled(!readOnly);
        updateView();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void updateView() {
        nameField.setText(config.getName());

        final String paletteFolderName = config.getProperty(EncapsulatedAssertionConfig.PROP_PALETTE_FOLDER);
        loadPaletteFolders(paletteFolderName);
        paletteFolderComboBox.setSelectedItem(paletteFolderName);

        setPolicyAndPolicyNameLabel(config.getPolicy());
    }

    private void setPolicyAndPolicyNameLabel(Policy policy) {
        this.policy = policy;
        policyNameLabel.setText(policy == null ? "<Not Configured>" : policy.getName());
    }

    private void updateBean() {
        config.setName(nameField.getText());
        config.putProperty(EncapsulatedAssertionConfig.PROP_PALETTE_FOLDER, (String) paletteFolderComboBox.getSelectedItem());
        config.setPolicy(policy);
    }

    private void loadPaletteFolders(String currentName) {
        List<String> folderNames = findPaletteFolderNames();

        if (currentName != null && currentName.trim().length() > 0 && !folderNames.contains(currentName))
            folderNames.add(currentName);

        paletteFolderComboBox.setModel(new DefaultComboBoxModel(folderNames.toArray(new String[0])));
    }

    private static List<String> findPaletteFolderNames() {
        List<String> folderNames = new ArrayList<String>();

        // TODO set up a registry of palette folders (ideally configurable in some way).  Fow now, punt to the existing logic, which requires creating a dummy root node.
        final AssertionsPaletteRootNode rootNode = new AssertionsPaletteRootNode("dummy root");
        rootNode.reloadChildren();
        Enumeration kids = rootNode.children();
        while (kids.hasMoreElements()) {
            Object kid = kids.nextElement();
            if (kid instanceof AbstractPaletteFolderNode) {
                AbstractPaletteFolderNode apfn = (AbstractPaletteFolderNode) kid;
                folderNames.add(apfn.getFolderId());
            }
        }

        return folderNames;
    }

    private void doChangePolicy() {
        try {
            Collection<PolicyHeader> policyHeaders = Registry.getDefault().getPolicyAdmin().findPolicyHeadersWithTypes(EnumSet.of(PolicyType.INCLUDE_FRAGMENT));

            if (policyHeaders == null || policyHeaders.size() < 1) {
                showError("No includable policy fragments are available to provide an implementation for this encapsulated assertion.\nPlese create a policy first.", null);
                return;
            }

            PolicyHeader initialValue = null;
            if (policy != null) {
                for (PolicyHeader policyHeader : policyHeaders) {
                    if (policyHeader.getOid() == policy.getOid()) {
                        initialValue = policyHeader;
                        break;
                    }
                }
            }

            final PolicyHeader[] options = policyHeaders.toArray(new PolicyHeader[policyHeaders.size()]);
            DialogDisplayer.showOptionDialog(this, "Select implementation policy:", "Set Implementation Policy", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, initialValue, new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if (JOptionPane.CLOSED_OPTION != option) {
                        PolicyHeader policyHeader = options[option];

                        // Load the selected policy
                        try {
                            Policy newPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(policyHeader.getOid());
                            if (newPolicy == null) {
                                showError("Policy not found", null);
                                return;
                            }

                            setPolicyAndPolicyNameLabel(newPolicy);

                        } catch (FindException e) {
                            showError("Unable to load policy", e);
                        }
                    }
                }
            });

        } catch (FindException e) {
            showError("Unable to load list of available policy include fragments", e);
        }
    }

    private void showError(String message, @Nullable Throwable e) {
        final String suffix = e == null ? "" : message + ": " + ExceptionUtils.getMessage(e);
        DialogDisplayer.showMessageDialog(this, suffix, "Error", JOptionPane.ERROR_MESSAGE, null);
    }

    private class IconActionListener implements ActionListener {
        private OkCancelDialog okCancelDialog;
        private IconActionListener(final OkCancelDialog okCancelDialog){
            this.okCancelDialog = okCancelDialog;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            okCancelDialog.pack();
            Utilities.centerOnParentWindow(okCancelDialog);
            DialogDisplayer.display(okCancelDialog, new Runnable() {
                @Override
                public void run() {
                    if(okCancelDialog.wasOKed()){
                        final ImageIcon icon = (ImageIcon) okCancelDialog.getValue();
                        iconLabel.setIcon(icon);
                        // TODO set icon name on Encapsulated Assertion using icon.getDescription();
                    }
                }
            });

    }
    }
}
