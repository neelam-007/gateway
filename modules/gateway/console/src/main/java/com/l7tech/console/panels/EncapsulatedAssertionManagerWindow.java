package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class EncapsulatedAssertionManagerWindow extends JDialog {
    private JPanel contentPane;
    private JButton closeButton;
    private JTable eacTable;
    private JButton createButton;
    private JButton propertiesButton;
    private JButton removeButton;

    private SimpleTableModel<EncapsulatedAssertionConfig> eacTableModel;

    public EncapsulatedAssertionManagerWindow(Window parent) {
        super(parent, "Manage Encapsulated Assertion Configurations", ModalityType.APPLICATION_MODAL);
        setContentPane(contentPane);
        setModal(true);
        Utilities.setEscKeyStrokeDisposes(this);

        Utilities.enableGrayOnDisabled(removeButton, propertiesButton);

        eacTableModel = TableUtil.configureTable(eacTable,
            column("Name", 25, 130, 99999, propertyTransform(EncapsulatedAssertionConfig.class, "name")),
            column("Palette Folder", 25, 130, 99999, paletteFolderFinder()),
            column("Policy Name", 25, 130, 99999, policyNameFinder()),
            column("In", 25, 30, 50, inputsFinder()),
            column("Out", 25, 30, 50, outputsFinder()));

        closeButton.addActionListener(Utilities.createDisposeAction(this));

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doProperties(new EncapsulatedAssertionConfig());
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final EncapsulatedAssertionConfig config = getSelectedConfig();
                if (config != null)
                    doProperties(config);
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final EncapsulatedAssertionConfig config = getSelectedConfig();
                if (config == null)
                    return;

                DialogDisplayer.showSafeConfirmDialog(
                    EncapsulatedAssertionManagerWindow.this,
                    "<html>Are you sure you wish to delete the encapsulated assertion configuration " + config.getName() + "?<br>" +
                        "Any existing policies that make use of this assertion will become invalid.",
                    "Confirm Remove Encapsulated Assertion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.YES_OPTION)
                                doDeleteEnapsulatedAssertionConfig(config);
                        }
                    });
            }
        });

        eacTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisable();
            }
        });

        // TODO RBAC-awareness
        loadEncapsulatedAssertionConfigs();
        enableOrDisable();
    }

    private void doProperties(final EncapsulatedAssertionConfig config) {
        final EncapsulatedAssertionConfigPropertiesDialog dlg = new EncapsulatedAssertionConfigPropertiesDialog(this, config, false); // TODO propagate RBAC view
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    try {
                        long oid = Registry.getDefault().getEncapsulatedAssertionAdmin().saveEncapsulatedAssertionConfig(config);
                        loadEncapsulatedAssertionConfigs();
                        selectConfigByOid(oid);
                    } catch (SaveException e) {
                        showError("Unable to save encapsulated assertion configuration", e);
                    } catch (UpdateException e) {
                        showError("Unable to save encapsulated assertion configuration", e);
                    } catch (VersionException e) {
                        showError("Unable to save encapsulated assertion configuration", e);
                    }
                }
            }
        });
    }

    private void selectConfigByOid(final long oid) {
        int row = eacTableModel.findFirstRow(new Functions.Unary<Boolean, EncapsulatedAssertionConfig>() {
            @Override
            public Boolean call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                return oid == encapsulatedAssertionConfig.getOid();
            }
        });
        final ListSelectionModel sm = eacTable.getSelectionModel();
        if (row < 0) {
            sm.clearSelection();
        } else {
            sm.setSelectionInterval(row, row);
        }
    }

    private EncapsulatedAssertionConfig getSelectedConfig() {
        int row = eacTable.getSelectedRow();
        if (row < 0)
            return null;
        return eacTableModel.getRowObject(row);
    }

    private void enableOrDisable() {
        boolean haveConfig = getSelectedConfig() != null;
        removeButton.setEnabled(haveConfig);
        propertiesButton.setEnabled(haveConfig);
    }

    private Functions.Unary<String, EncapsulatedAssertionConfig> paletteFolderFinder() {
        return new Functions.Unary<String, EncapsulatedAssertionConfig>() {
            @Override
            public String call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                return encapsulatedAssertionConfig.getProperty(EncapsulatedAssertionConfig.PROP_PALETTE_FOLDER);
            }
        };
    }

    private Functions.Unary<String, EncapsulatedAssertionConfig> policyNameFinder() {
        return new Functions.Unary<String, EncapsulatedAssertionConfig>() {
            @Override
            public String call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                Policy policy = encapsulatedAssertionConfig.getPolicy();
                return policy == null ? "<No Policy>" : policy.getName();
            }
        };
    }

    private Functions.Unary<String, EncapsulatedAssertionConfig> inputsFinder() {
        return new Functions.Unary<String, EncapsulatedAssertionConfig>() {
            @Override
            public String call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                Set<EncapsulatedAssertionArgumentDescriptor> args = encapsulatedAssertionConfig.getArgumentDescriptors();
                return args == null ? "0" : String.valueOf(args.size());
            }
        };
    }

    private Functions.Unary<String, EncapsulatedAssertionConfig> outputsFinder() {
        return new Functions.Unary<String, EncapsulatedAssertionConfig>() {
            @Override
            public String call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                Set<EncapsulatedAssertionResultDescriptor> outs = encapsulatedAssertionConfig.getResultDescriptors();
                return outs == null ? "0" : String.valueOf(outs.size());
            }
        };
    }

    private void loadEncapsulatedAssertionConfigs() {
        try {
            Collection<EncapsulatedAssertionConfig> configs = Registry.getDefault().getEncapsulatedAssertionAdmin().findAllEncapsulatedAssertionConfigs();
            eacTableModel.setRows(new ArrayList<EncapsulatedAssertionConfig>(configs));
        } catch (FindException e) {
            showError("Unable to load encapsulated assertion configurations", e);
        }
    }

    private void doDeleteEnapsulatedAssertionConfig(EncapsulatedAssertionConfig config) {
        try {
            Registry.getDefault().getEncapsulatedAssertionAdmin().deleteEncapsulatedAssertionConfig(config.getOid());
            loadEncapsulatedAssertionConfigs();
        } catch (FindException e1) {
            showError("Unable to delete encapsulated assertion config", e1);
        } catch (DeleteException e1) {
            showError("Unable to delete encapsulated assertion config", e1);
        } catch (ConstraintViolationException e1) {
            showError("Unable to delete encapsulated assertion config", e1);
        }
    }

    private void showError(String message, Throwable e) {
        DialogDisplayer.showMessageDialog(this, message + ": " + ExceptionUtils.getMessage(e), "Error", JOptionPane.ERROR_MESSAGE, null);
    }
}
