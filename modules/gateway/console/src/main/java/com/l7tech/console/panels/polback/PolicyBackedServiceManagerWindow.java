package com.l7tech.console.panels.polback;

import com.l7tech.console.action.DeleteEntityNodeAction;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class PolicyBackedServiceManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(PolicyBackedServiceManagerWindow.class.getName());

    private JPanel contentPane;
    private JButton closeButton;
    private JTable pbsTable;
    private JButton createButton;
    private JButton propertiesButton;
    private JButton removeButton;

    private SimpleTableModel<PolicyBackedService> pbsTableModel;
    private PermissionFlags flags;

    public PolicyBackedServiceManagerWindow( Window parent ) {
        super(parent, "Manage Policy-Backed Services", ModalityType.APPLICATION_MODAL);
        flags = PermissionFlags.get(EntityType.POLICY_BACKED_SERVICE);
        setContentPane(contentPane);
        setModal(true);
        Utilities.setEscKeyStrokeDisposes(this);

        Utilities.enableGrayOnDisabled(removeButton, propertiesButton);

        pbsTableModel = TableUtil.configureTable( pbsTable,
            column("Name", 30, 140, 99999, propertyTransform(PolicyBackedService.class, "name")),
            column("Type", 25, 165, 99999, makeTypeFinder() ));

        closeButton.addActionListener(Utilities.createDisposeAction(this));

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doProperties(new PolicyBackedService());
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PolicyBackedService config = getSelectedConfig();
                if (config != null)
                    doProperties(config);
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PolicyBackedService config = getSelectedConfig();
                if (config == null)
                    return;

                final String msg = "Are you sure you wish to delete the policy-backed service " + config.getName() + "?" +
                        "  Any existing assertions that rely on this service instance will stop working.";
                DialogDisplayer.showSafeConfirmDialog(
                    PolicyBackedServiceManagerWindow.this,
                        WordUtils.wrap(msg, DeleteEntityNodeAction.LINE_CHAR_LIMIT, null, true),
                    "Confirm Remove Policy-Backed Service",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.YES_OPTION)
                                doDeletePolicyBackedService(config);
                        }
                    });
            }
        });

        pbsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisable();
            }
        });
        Utilities.setDoubleClickAction( pbsTable, propertiesButton);
        Utilities.setRowSorter( pbsTable, pbsTableModel );
        loadPolicyBackedServices();
        enableOrDisable();
    }

    private static Functions.Unary<String, PolicyBackedService> makeTypeFinder() {
        return new Functions.Unary<String, PolicyBackedService>() {
            @Override
            public String call( PolicyBackedService policyBackedService ) {
                return policyBackedService.getServiceInterfaceName();
            }
        };
    }

    /**
     * Display the config properties dialog.
     * @param config the config to display.
     */
    private void doProperties(@NotNull final PolicyBackedService config) {
        final boolean isNew = PersistentEntity.DEFAULT_GOID.equals(config.getGoid());

        final PolicyBackedServicePropertiesDialog dlg = new PolicyBackedServicePropertiesDialog( this, config );
        dlg.pack();
        Utilities.centerOnParentWindow( dlg );
        DialogDisplayer.display( dlg, new Runnable() {
            @Override
            public void run() {
                if (!dlg.isConfirmed())
                    return;

                dlg.getData( config );

                try {

                    final Goid savedGoid = Registry.getDefault().getPolicyBackedServiceAdmin().save( config );

                    // Replace table row with updated entity from server
                    PolicyBackedService updated = Registry.getDefault().getPolicyBackedServiceAdmin().findByPrimaryKey( savedGoid );
                    pbsTableModel.removeRow( config );
                    pbsTableModel.addRow( updated );
                    selectConfigByOid( savedGoid );

                } catch ( ObjectModelException e ) {
                    showError( "Error saving config", e );
                }
            }
        } );
    }

    private void selectConfigByOid(final Goid goid) {
        int row = pbsTableModel.findFirstRow(new Functions.Unary<Boolean, PolicyBackedService>() {
            @Override
            public Boolean call(PolicyBackedService PolicyBackedService) {
                return goid.equals(PolicyBackedService.getGoid());
            }
        });
        final ListSelectionModel sm = pbsTable.getSelectionModel();
        if (row < 0) {
            sm.clearSelection();
        } else {
            sm.setSelectionInterval(row, row);
        }
    }

    private PolicyBackedService getSelectedConfig() {
        int row = pbsTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        final int modelRow = pbsTable.convertRowIndexToModel(row);
        return pbsTableModel.getRowObject(modelRow);
    }

    private void enableOrDisable() {
        final PolicyBackedService selected = getSelectedConfig();
        boolean haveConfig = selected != null;
        removeButton.setEnabled(flags.canDeleteSome() && haveConfig);
        propertiesButton.setEnabled(haveConfig);
        createButton.setEnabled(flags.canCreateSome());
    }

    private void loadPolicyBackedServices() {
        try {
            Collection<PolicyBackedService> configs = Registry.getDefault().getPolicyBackedServiceAdmin().findAll();
            pbsTableModel.setRows( new ArrayList<PolicyBackedService>( configs ) );
        } catch (FindException e) {
            showError("Unable to load policy-backed services", e);
        }
    }

    private void doDeletePolicyBackedService(PolicyBackedService config) {
        try {
            Registry.getDefault().getPolicyBackedServiceAdmin().deletePolicyBackedService(config.getGoid());
            loadPolicyBackedServices();
        } catch (FindException e1) {
            showError("Unable to delete policy-backed service", e1);
        } catch (DeleteException e1) {
            showError("Unable to delete policy-backed service", e1);
        } catch (ConstraintViolationException e1) {
            showError("Unable to delete policy-backed service", e1);
        }
    }

    /**
     * Displays an error message to the user.
     * @param message the error message to show the user.
     * @param e the Throwable which caused the error or null if you do not want to show the exception to the user.
     */
    private void showError(@NotNull final String message, @Nullable final Throwable e) {
        String error = message;
        if (e != null) {
            error = error + ": " + ExceptionUtils.getMessage(e);
        }
        DialogDisplayer.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE, null);
    }
}
