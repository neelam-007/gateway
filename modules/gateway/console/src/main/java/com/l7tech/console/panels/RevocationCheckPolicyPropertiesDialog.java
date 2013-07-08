package com.l7tech.console.panels;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Collection;
import java.text.MessageFormat;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.RevocationCheckPolicyItem;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ValidationUtils;
import com.l7tech.util.Functions;

/**
 * Properties dialog for Revocation Checking Policies.
 *
 * @author Steve Jones
 */
public class RevocationCheckPolicyPropertiesDialog extends JDialog {

    //- PUBLIC

    /**
     * Create a new RevocationCheckPolicyPropertiesDialog to edit the given policy.
     *
     * @param parent The parent Dialog
     * @param readOnly True if this dialog is read-only.
     * @param revocationCheckPolicy The policy to edit
     * @param policies Collection of all RevocationCheckPolicies (may be null, used when viewing trusted certs)
     */
    public RevocationCheckPolicyPropertiesDialog(Dialog parent,
                                                 boolean readOnly,
                                                 RevocationCheckPolicy revocationCheckPolicy,
                                                 Collection<RevocationCheckPolicy> policies) {
        super(parent, resources.getString(RES_TITLE), true);
        this.readOnly = readOnly;
        this.revocationCheckPolicy = revocationCheckPolicy;
        this.policies = policies;
        init();
    }

    /**
     * Create a new RevocationCheckPolicyPropertiesDialog to edit the given policy.
     *
     * @param parent The parent Frame
     * @param readOnly True if this dialog is read-only.
     * @param revocationCheckPolicy The policy to edit
     * @param policies Collection of all RevocationCheckPolicies (may be null, used when viewing trusted certs)
     */
    public RevocationCheckPolicyPropertiesDialog(Frame parent,
                                                 boolean readOnly,
                                                 RevocationCheckPolicy revocationCheckPolicy,
                                                 Collection<RevocationCheckPolicy> policies) {
        super(parent, resources.getString(RES_TITLE), true);
        this.readOnly = readOnly;
        this.revocationCheckPolicy = revocationCheckPolicy;
        this.policies = policies;
        init();
    }

    /**
     * Did this dialog exit successfully?
     * 
     * @return True if the dialog was exited with 'OK'
     */
    public boolean wasOk() {
        return oked;
    }

    //- PRIVATE

    /**
     * Resource bundle for this dialog
     */
    private static final ResourceBundle resources =
            ResourceBundle.getBundle("com.l7tech.console.resources.RevocationCheckPolicyPropertiesDialog", Locale.getDefault());

    /**
     * Resource bundle keys
     */
    private static final String RES_TITLE = "dialog.title";
    private static final String RES_ITEM_CRLFROMCERT = "item.crlfromcert.text";
    private static final String RES_ITEM_CRLFROMURL = "item.crlfromurl.text";
    private static final String RES_ITEM_OCSPFROMCERT = "item.ocspfromcert.text";
    private static final String RES_ITEM_OCSPFROMURL = "item.ocspfromurl.text";
    private static final String RES_VALIDATE_NAME_TEXT = "error.name.text";
    private static final String RES_VALIDATE_NAME_TITLE = "error.name.title";
    private static final String RES_VALIDATE_ITEMS_TEXT = "error.empty.text";
    private static final String RES_VALIDATE_ITEMS_TITLE = "error.empty.title";

    /**
     * Characters allowed in a policy name
     */
    private static final String NAME_CHARACTERS = ValidationUtils.ALPHA_NUMERIC + " ,.'{[}]!#$%^()";

    /**
     * Maximum number of items in a policy 
     */
    private static final int MAXIMUM_ITEMS = 50;

    private final boolean readOnly;
    private final RevocationCheckPolicy revocationCheckPolicy;
    private final Collection<RevocationCheckPolicy> policies;
    private DefaultListModel policyItemModel;
    private boolean oked;

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField nameTextField;
    private JList policyItemList;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;
    private JCheckBox useAsDefaultRevocationCheckBox;
    private JButton moveDownButton;
    private JButton moveUpButton;
    private JCheckBox succeedOnUnknownCheckBox;
    private JCheckBox continueOnServerUnavailableCheckBox;
    private SecurityZoneWidget zoneControl;

    /**
     * Initialize the dialog
     */
    private void init() {
        initUI();
    }

    /**
     * Initialize the UI
     */
    private void initUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);

        // listeners
        cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        okButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                doOk();
            }
        });

        addButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                addPolicyItem();
            }
        });
        
        removeButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                removePolicyItem();
            }
        });

        editButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                editPolicyItem();
            }
        });

        moveUpButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                moveUp();
                moveUpButton.requestFocus();
            }
        });

        moveDownButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                moveDown();
                moveDownButton.requestFocus();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});

        policyItemList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableControls();
            }
        });
        policyItemList.setCellRenderer(new TextListCellRenderer(new RevocationCheckPolicyItemAccessor()));
        policyItemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utilities.setDoubleClickAction(policyItemList, editButton);
        zoneControl.configure(revocationCheckPolicy.getOid() == RevocationCheckPolicy.DEFAULT_OID ? OperationType.CREATE : readOnly ? OperationType.READ : OperationType.UPDATE, revocationCheckPolicy);

        // Set data
        nameTextField.setDocument(new FilterDocument(64, new FilterDocument.Filter(){
            public boolean accept(String s) {
                return ValidationUtils.isValidCharacters(s, NAME_CHARACTERS);
            }
        }));
        nameTextField.setText(revocationCheckPolicy.getName());
        useAsDefaultRevocationCheckBox.setSelected(revocationCheckPolicy.isDefaultPolicy());
        succeedOnUnknownCheckBox.setSelected(revocationCheckPolicy.isDefaultSuccess());
        continueOnServerUnavailableCheckBox.setSelected(revocationCheckPolicy.isContinueOnServerUnavailable());
        policyItemModel = new DefaultListModel();
        for (RevocationCheckPolicyItem item : revocationCheckPolicy.clone().getRevocationCheckItems()) {
            policyItemModel.addElement(item);
        }
        policyItemList.setModel(policyItemModel);
        enableOrDisableControls();

        add(mainPanel);
        pack();
        Utilities.centerOnParentWindow(this);
    }

    /**
     * Enable/disable controls based on permissions and selections
     */
    private void enableOrDisableControls() {
        boolean enableControls = !readOnly;

        // disable if readonly
        okButton.setEnabled(enableControls);
        nameTextField.setEnabled(enableControls);
        addButton.setEnabled(enableControls);
        removeButton.setEnabled(enableControls);
        editButton.setEnabled(enableControls);
        useAsDefaultRevocationCheckBox.setEnabled(enableControls);
        moveDownButton.setEnabled(enableControls);
        moveUpButton.setEnabled(enableControls);
        succeedOnUnknownCheckBox.setEnabled(enableControls);
        continueOnServerUnavailableCheckBox.setEnabled(enableControls);

        boolean contextualEnabled = false;
        boolean moveUpEnabled = false;
        boolean moveDownEnabled = false;

        int row = policyItemList.getSelectedIndex();
        if ( row >= 0 ) {
            contextualEnabled = true;
            moveUpEnabled = enableControls && row > 0;
            moveDownEnabled = enableControls && row < policyItemModel.size() - 1;
        }

        addButton.setEnabled(enableControls && policyItemModel.size() < MAXIMUM_ITEMS);
        removeButton.setEnabled(enableControls && contextualEnabled);
        editButton.setEnabled(contextualEnabled);
        moveUpButton.setEnabled(moveUpEnabled);
        moveDownButton.setEnabled(moveDownEnabled);
    }

    /**
     * Add a new item to the policy
     */
    private void addPolicyItem() {
        final RevocationCheckPolicyItem item = new RevocationCheckPolicyItem();
        item.setAllowIssuerSignature(true);
        final RevocationCheckPolicyItemPropertiesDialog editor =
                new RevocationCheckPolicyItemPropertiesDialog(this, readOnly, item, policies);
        DialogDisplayer.display(editor, new Runnable(){
            public void run() {
                if (editor.wasOk()) {
                    policyItemModel.addElement(item);
                    policyItemList.setSelectedIndex(policyItemModel.size()-1);
                }
            }
        });
    }

    /**
     * Remove and item from the policy
     */
    private void removePolicyItem() {
        int row = policyItemList.getSelectedIndex();
        if (row >= 0) {
            policyItemModel.remove(row);
            int selectIndex = policyItemModel.size()-1;
            if ( selectIndex >= 0 ) {
                policyItemList.setSelectedIndex(selectIndex);
            }
        }
    }

    /**
     * Edit a policy item
     */
    private void editPolicyItem() {
        final int index = policyItemList.getSelectedIndex();
        if ( index >= 0 ) {
            RevocationCheckPolicyItem item = (RevocationCheckPolicyItem) policyItemModel.getElementAt(index);
            if ( item != null ) {
                RevocationCheckPolicyItemPropertiesDialog editor =
                        new RevocationCheckPolicyItemPropertiesDialog(this, readOnly, item, policies);
                DialogDisplayer.display(editor, new Runnable() {
                    public void run() {
                        for ( ListDataListener ldl : policyItemModel.getListDataListeners() ) {
                            ldl.contentsChanged(new ListDataEvent(policyItemList, ListDataEvent.CONTENTS_CHANGED, index, index));
                        }
                        policyItemList.setSelectedIndex(index);
                    }
                });
            }
        }
    }

    /**
     * Move an item up in the policy
     */
    private void moveUp() {
        int row = policyItemList.getSelectedIndex();
        if (row >= 1) {
            Object item = policyItemModel.remove(row);
            policyItemModel.insertElementAt(item, row - 1);
            policyItemList.setSelectedIndex(row - 1);
        }
    }

    /**
     * Move an item down in the policy
     */
    private void moveDown() {
        int row = policyItemList.getSelectedIndex();
        if (row >= 0 && row < policyItemModel.getSize()-1) {
            Object item = policyItemModel.remove(row);
            policyItemModel.insertElementAt(item, row + 1);
            policyItemList.setSelectedIndex(row + 1);
        }
    }    

    /**
     * Ok action
     */
    private void doOk() {
        if ( isValidProperties() ) {
            revocationCheckPolicy.setName(nameTextField.getText().trim());
            revocationCheckPolicy.setDefaultPolicy(useAsDefaultRevocationCheckBox.isSelected());
            revocationCheckPolicy.setDefaultSuccess(succeedOnUnknownCheckBox.isSelected());
            revocationCheckPolicy.setContinueOnServerUnavailable(continueOnServerUnavailableCheckBox.isSelected());
            java.util.List<RevocationCheckPolicyItem> list = new ArrayList();
            for ( Object item : policyItemModel.toArray() ) {
                list.add((RevocationCheckPolicyItem) item);
            }
            revocationCheckPolicy.setRevocationCheckItems(list);
            revocationCheckPolicy.setSecurityZone(zoneControl.getSelectedZone());

            oked = true;
            dispose();
        }
    }

    /**
     * Validate properties 
     */
    private boolean isValidProperties() {
        boolean valid = false;

        if ( nameTextField.getText().trim().length()==0 ) {
            JOptionPane.showMessageDialog(this,
                    resources.getString(RES_VALIDATE_NAME_TEXT),
                    resources.getString(RES_VALIDATE_NAME_TITLE),
                    JOptionPane.ERROR_MESSAGE);
        } else if ( policyItemModel.isEmpty() ) {
            JOptionPane.showMessageDialog(this,
                    resources.getString(RES_VALIDATE_ITEMS_TEXT),
                    resources.getString(RES_VALIDATE_ITEMS_TITLE),
                    JOptionPane.ERROR_MESSAGE);
        } else {
            valid = true;
        }

        return valid;
    }

    /**
     * Label text accessor for RevocationCheckPolicyItems
     */
    private static final class RevocationCheckPolicyItemAccessor implements Functions.Unary<String, Object>
    {
        public String call(Object value) {        
            RevocationCheckPolicyItem revocationCheckPolicyItem = (RevocationCheckPolicyItem) value;
            RevocationCheckPolicyItem.Type type = null;
            String url = null;

            type = revocationCheckPolicyItem.getType();
            url = revocationCheckPolicyItem.getUrl();

            String labelKey = RES_ITEM_CRLFROMCERT;
            if (type != null) {
                switch(type) {
                    case CRL_FROM_CERTIFICATE:
                        labelKey = RES_ITEM_CRLFROMCERT;
                        break;
                    case CRL_FROM_URL:
                        labelKey = RES_ITEM_CRLFROMURL;
                        break;
                    case OCSP_FROM_CERTIFICATE:
                        labelKey = RES_ITEM_OCSPFROMCERT;
                        break;
                    case OCSP_FROM_URL:
                        labelKey = RES_ITEM_OCSPFROMURL;
                        break;
                }
            }

            return MessageFormat.format(resources.getString(labelKey), url);
        }
    }
}
