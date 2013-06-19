package com.l7tech.console.panels;

import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import static com.l7tech.objectmodel.EntityType.CLUSTER_PROPERTY;
import static com.l7tech.objectmodel.EntityType.REVOCATION_CHECK_POLICY;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for management of Certificate Validation settings.
 *
 * @author Steve Jones
 */
public class ManageCertificateValidationDialog extends JDialog {

    //- PUBLIC

    /**
     * Create a ManageCertificateValidationDialog with the given parent.
     *
     * @param parent The parent Dialog
     */
    public ManageCertificateValidationDialog(Dialog parent) {
        super(parent, resources.getString(RES_TITLE), true);
        init();
    }

    /**
     * Create a ManageCertificateValidationDialog with the given parent.
     *
     * @param parent The parent Frame
     */
    public ManageCertificateValidationDialog(Frame parent) {
        super(parent, resources.getString(RES_TITLE), true);
        init();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ManageCertificateValidationDialog.class.getName());

    /**
     * Resource bundle for the dialog
     */
    private static final ResourceBundle resources =
            ResourceBundle.getBundle("com.l7tech.console.resources.ManageCertificateValidationDialog", Locale.getDefault());

    /**
     * Resource bundle keys
     */
    private static final String RES_TITLE = "dialog.title";
    private static final String RES_VALIDATION_TITLE = "validation.title";
    private static final String RES_HEADER_TYPE = "validation.header.type";
    private static final String RES_HEADER_OPTION = "validation.header.option";
    private static final String RES_VAL_IDENT = "validation.identity.text";
    private static final String RES_VAL_ROUTE = "validation.routing.text";
    private static final String RES_VAL_OTHER = "validation.other.text";
    private static final String RES_VAL_DESC_IDENT = "validation.identity.description";
    private static final String RES_VAL_DESC_ROUTE = "validation.routing.description";
    private static final String RES_VAL_DESC_OTHER = "validation.other.description";
    private static final String RES_VAL_PROP_PREFIX = "validation.value.";

    /**
     * Permitted values for validation cluster properties
     */
    private static final String[] OPTIONS = {
            "validate",
            "validatepath",
            "revocation",
    };

    /**
     * Validation properties (resource bundle name key, resource bundle description key, cluster property name)
     */
    private static final String[][] CLUSTER_PROP_INFO = {
            {RES_VAL_IDENT, RES_VAL_DESC_IDENT, "pkix.validation.identityProvider"},
            {RES_VAL_ROUTE, RES_VAL_DESC_ROUTE, "pkix.validation.routing"},
            {RES_VAL_OTHER, RES_VAL_DESC_OTHER, "pkix.validation.other"},
    };


    private PermissionFlags flags;
    private PermissionFlags configFlags;

    private JPanel mainPanel;
    private JButton closeButton;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JList policyList;
    private JButton validationPropertiesButton;
    private JTable validationOptionsTable;

    /**
     * Initialize the dialog
     */
    private void init() {
        flags = PermissionFlags.get(REVOCATION_CHECK_POLICY);
        configFlags = PermissionFlags.get(CLUSTER_PROPERTY);

        initUI();
    }

    /**
     * Initialize the dialog UI
     */
    private void initUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);

        closeButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        validationPropertiesButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                editValidationOptions();
            }
        });

        addButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                addRevocationCheckPolicy();
            }
        });

        removeButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                removeRevocationCheckPolicy();
            }
        });

        propertiesButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                editRevocationCheckPolicy();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[]{validationPropertiesButton, addButton, removeButton, propertiesButton, closeButton});

        DefaultTableModel dtm = new DefaultTableModel(new String[]{
                resources.getString(RES_HEADER_TYPE),
                resources.getString(RES_HEADER_OPTION)
        }, CLUSTER_PROP_INFO.length){
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int row = 0;
        for (String[] propInfo : CLUSTER_PROP_INFO) {
            dtm.setValueAt(resources.getString(propInfo[0]), row, 0);
            dtm.setValueAt(resources.getString(RES_VAL_PROP_PREFIX + OPTIONS[0]), row, 1);
            row++;
        }
        validationOptionsTable.setModel(dtm);
        validationOptionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        validationOptionsTable.getTableHeader().setReorderingAllowed(false);
        validationOptionsTable.getTableHeader().getColumnModel().getColumn(0).setPreferredWidth(40);
        validationOptionsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });
        Utilities.setDoubleClickAction(validationOptionsTable, validationPropertiesButton);

        policyList.setCellRenderer(new Renderers.RevocationCheckPolicyRenderer());
        ListSelectionModel policyListSelectionModel = policyList.getSelectionModel();
        policyListSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        policyListSelectionModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });
        Utilities.setDoubleClickAction(policyList, propertiesButton);

        loadValidationProperties();
        loadRevocationCheckingPolicies();

        enableOrDisableButtons();

        add(mainPanel);
        pack();
        Utilities.centerOnParentWindow(this);
    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableButtons() {
        boolean optionPropsEnabled = false;
        boolean propsEnabled = false;
        boolean removeEnabled = false;

        int rowOption = validationOptionsTable.getSelectedRow();
        if ( rowOption >= 0 ) {
            optionPropsEnabled = configFlags.canUpdateAll() && configFlags.canCreateAll();
        }

        int row = policyList.getSelectedIndex();
        if ( row >= 0 ) {
            removeEnabled = true;
            propsEnabled = true;
        }

        validationPropertiesButton.setEnabled(optionPropsEnabled);
        addButton.setEnabled(flags.canCreateSome());
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
        propertiesButton.setEnabled(propsEnabled);
    }

    /**
     * Edit the selected validation cluster property.
     */
    private void editValidationOptions() {
        int row = validationOptionsTable.getSelectedRow();
        if ( row >= 0 ) {
            ClusterStatusAdmin csa = getClusterStatusAdmin();

            String description = resources.getString(CLUSTER_PROP_INFO[row][1]);
            List options = new ArrayList();
            for (String option : OPTIONS) {
                options.add(resources.getString(RES_VAL_PROP_PREFIX + option));
            }

            String option = (String) JOptionPane.showInputDialog(
                    this,
                    description,
                    resources.getString(RES_VALIDATION_TITLE),
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options.toArray(),
                    validationOptionsTable.getValueAt(row, 1)
                    );

            if (option != null) {
                int index = options.indexOf(option);
                if (index >= 0) {
                    try {
                        ClusterPropertyCrud.putClusterProperty(CLUSTER_PROP_INFO[row][2], OPTIONS[index]);
                        validationOptionsTable.setValueAt(resources.getString(RES_VAL_PROP_PREFIX + OPTIONS[index]), row, 1);
                    } catch (ObjectModelException ome) {
                        String msg = resources.getString("error.validationproperty.text");
                        logger.log(Level.WARNING, msg, ome);
                        JOptionPane.showMessageDialog(ManageCertificateValidationDialog.this, msg,
                                                      resources.getString("error.validationproperty.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    /**
     * Add a new revocation check policy
     */
    private void addRevocationCheckPolicy() {
        final RevocationCheckPolicy check = new RevocationCheckPolicy();
        final RevocationCheckPolicyPropertiesDialog editor =
                new RevocationCheckPolicyPropertiesDialog(this, false, check, getRevocationCheckPolicies());
        DialogDisplayer.display(editor, new Runnable(){
            public void run() {
                if (editor.wasOk()) {
                    try {
                        getTrustedCertAdmin().saveRevocationCheckPolicy(check);
                        loadRevocationCheckingPolicies();
                    } catch (DuplicateObjectException ome) {
                        String msg = resources.getString("error.revocationpolicyadddupe.text");
                        JOptionPane.showMessageDialog(ManageCertificateValidationDialog.this, msg,
                                                      resources.getString("error.revocationpolicyadddupe.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    } catch (ObjectModelException ome) {
                        String msg = resources.getString("error.revocationpolicyadd.text");
                        logger.log(Level.WARNING, msg, ome);
                        JOptionPane.showMessageDialog(ManageCertificateValidationDialog.this, msg,
                                                      resources.getString("error.revocationpolicyadd.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    } catch (VersionException ve) {
                        throw new RuntimeException(ve);
                    }
                }
            }
        });
    }

    /**
     * Remove the selected revocation check policy
     */
    private void removeRevocationCheckPolicy() {
        int row = policyList.getSelectedIndex();
        if ( row >= 0 ) {
            DefaultListModel model = (DefaultListModel) policyList.getModel();
            RevocationCheckPolicy revocationCheckPolicy = (RevocationCheckPolicy) model.get(row);
            try {
                getTrustedCertAdmin().deleteRevocationCheckPolicy(revocationCheckPolicy.getOid());
                model.remove(row);
            } catch (ConstraintViolationException cve) {
                String msg = resources.getString("error.revocationpolicyinuse.text");
                JOptionPane.showMessageDialog(ManageCertificateValidationDialog.this, msg,
                                              resources.getString("error.revocationpolicyinuse.title"),
                                              JOptionPane.WARNING_MESSAGE);
            } catch (ObjectModelException ome) {
                String msg = resources.getString("error.revocationpolicydelete.text");
                logger.log(Level.WARNING, msg, ome);
                JOptionPane.showMessageDialog(ManageCertificateValidationDialog.this, msg,
                                              resources.getString("error.revocationpolicydelete.title"),
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Edit the selected revocation check policy
     */
    private void editRevocationCheckPolicy() {
        final int row = policyList.getSelectedIndex();
        final RevocationCheckPolicy check = (RevocationCheckPolicy) policyList.getModel().getElementAt(row);
        if (check != null) {
            final boolean canUpdate = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.REVOCATION_CHECK_POLICY, check));
            final RevocationCheckPolicyPropertiesDialog editor =
                    new RevocationCheckPolicyPropertiesDialog(this, !canUpdate, check, getRevocationCheckPolicies());
            DialogDisplayer.display(editor, new Runnable(){
                public void run() {
                    if (editor.wasOk()) {
                        try {
                            getTrustedCertAdmin().saveRevocationCheckPolicy(check);
                            loadRevocationCheckingPolicies();
                        } catch (ObjectModelException ome) {
                            String msg = resources.getString("error.revocationpolicyedit.text");
                            logger.log(Level.WARNING, msg, ome);
                            JOptionPane.showMessageDialog(ManageCertificateValidationDialog.this, msg,
                                                          resources.getString("error.revocationpolicyedit.title"),
                                                          JOptionPane.ERROR_MESSAGE);
                        } catch (VersionException ve) {
                            throw new RuntimeException(ve);
                        }
                    }
                }
            });
        }
    }

    /**
     * Load the validation cluster properties
     */
    private void loadValidationProperties() {
        try {
            ClusterStatusAdmin csa = getClusterStatusAdmin();
            int row = 0;
            for ( String[] cpi : CLUSTER_PROP_INFO ) {
                String clusterPropertyName = cpi[2];
                ClusterProperty property = csa.findPropertyByName(clusterPropertyName);
                if ( property!=null && Arrays.asList(OPTIONS).contains(property.getValue()) ) {
                    validationOptionsTable.setValueAt(resources.getString(RES_VAL_PROP_PREFIX + property.getValue()), row, 1);
                }
                row++;
            }
        } catch (FindException e) {
            String msg = resources.getString("error.revocationpolicyfind.text");
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(this.getParent(), msg,
                                          resources.getString("error.revocationpolicyfind.title"),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Load the revocation check policies from the database
     */
    private void loadRevocationCheckingPolicies() {
        java.util.List<RevocationCheckPolicy> certList;
        try {
            certList = new ArrayList(getTrustedCertAdmin().findAllRevocationCheckPolicies());
            Collections.sort(certList, new ResolvingComparator(new Resolver<RevocationCheckPolicy, String>(){
                public String resolve(RevocationCheckPolicy rcp) {
                    String name = rcp.getName();
                    if (name == null)
                        name = "";
                    return name.toLowerCase();
                }
            }, false));

            DefaultListModel dlm = new DefaultListModel();
            for (RevocationCheckPolicy revocationCheckPolicy : certList) {
                dlm.addElement(revocationCheckPolicy);
            }
            policyList.setModel(dlm);
        } catch (FindException e) {
            String msg = resources.getString("error.revocationpolicyfind.text");
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(this.getParent(), msg,
                                          resources.getString("error.revocationpolicyfind.title"),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private Collection<RevocationCheckPolicy> getRevocationCheckPolicies() {
        List<RevocationCheckPolicy> revocationCheckPolicies = new ArrayList();

        ListModel model = policyList.getModel();
        for (int i=0; i<model.getSize(); i++) {
            revocationCheckPolicies.add((RevocationCheckPolicy) model.getElementAt(i));    
        }

        return revocationCheckPolicies;
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
     */
    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    /**
     * Retrieve the object reference of the Cluster Status Admin service
     *
     * @return ClusterStatusAdmin  - The object reference.
     * @throws RuntimeException if the object reference of the Cluster Status Admin service is not found.
     */
    private ClusterStatusAdmin getClusterStatusAdmin() throws RuntimeException {
        return Registry.getDefault().getClusterStatusAdmin();
    }
}
