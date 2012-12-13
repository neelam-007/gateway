package com.l7tech.console.panels;

import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.console.util.EncapsulatedAssertionConsoleUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

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
    private Map<String, ImageIcon> iconCache = new HashMap<String, ImageIcon>();
    private PermissionFlags flags;

    public EncapsulatedAssertionManagerWindow(Window parent) {
        super(parent, "Manage Encapsulated Assertion Configurations", ModalityType.APPLICATION_MODAL);
        flags = PermissionFlags.get(EntityType.ENCAPSULATED_ASSERTION);
        setContentPane(contentPane);
        setModal(true);
        Utilities.setEscKeyStrokeDisposes(this);

        Utilities.enableGrayOnDisabled(removeButton, propertiesButton);

        eacTableModel = TableUtil.configureTable(eacTable,
            column(" ", 25, 25, 25, iconFinder(), Icon.class),
            column("Name", 30, 140, 99999, propertyTransform(EncapsulatedAssertionConfig.class, "name")),
            column("Palette Folder", 25, 165, 99999, paletteFolderFinder()),
            column("Policy Name", 25, 140, 99999, policyNameFinder()),
            column("In", 30, 30, 50, inputsFinder()),
            column("Out", 30, 30, 50, outputsFinder()));

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
        Utilities.setDoubleClickAction(eacTable, propertiesButton);

        loadEncapsulatedAssertionConfigs(false);
        enableOrDisable();
    }

    private void doProperties(@NotNull final EncapsulatedAssertionConfig config) {
        AttemptedOperation op = Long.valueOf(PersistentEntity.DEFAULT_OID).equals(config.getOid())
            ? new AttemptedCreateSpecific(EntityType.ENCAPSULATED_ASSERTION, config)
            : new AttemptedUpdate(EntityType.ENCAPSULATED_ASSERTION, config);
        SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
        boolean readOnly = !securityProvider.hasPermission(op);

        final EncapsulatedAssertionConfigPropertiesDialog dlg = new EncapsulatedAssertionConfigPropertiesDialog(this, config, readOnly);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    try {
                        long oid = Registry.getDefault().getEncapsulatedAssertionAdmin().saveEncapsulatedAssertionConfig(config);
                        loadEncapsulatedAssertionConfigs(true);
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

        if (!flags.canCreateSome()) {
            createButton.setEnabled(false);
        }

        if (!flags.canDeleteSome()) {
            removeButton.setEnabled(false);
        }
    }

    private Functions.Unary<Icon, EncapsulatedAssertionConfig> iconFinder() {
        return new Functions.Unary<Icon, EncapsulatedAssertionConfig>() {
            @Override
            public Icon call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                final String id = encapsulatedAssertionConfig.getId();
                ImageIcon icon = iconCache.get(id);
                if (icon == null) {
                    icon = EncapsulatedAssertionConsoleUtil.findIcon(encapsulatedAssertionConfig).right;
                    iconCache.put(id, icon);
                }
                return icon;
            }
        };
    }

    private Functions.Unary<String, EncapsulatedAssertionConfig> paletteFolderFinder() {
        final PaletteFolderRegistry paletteFolderRegistry = TopComponents.getInstance().getPaletteFolderRegistry();
        return new Functions.Unary<String, EncapsulatedAssertionConfig>() {
            @Override
            public String call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                String folder = encapsulatedAssertionConfig.getProperty(EncapsulatedAssertionConfig.PROP_PALETTE_FOLDER);
                return folder == null ? "" : paletteFolderRegistry.getPaletteFolderName(folder);
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

    /**
     * @param updateLocalRegistry true if the list of available encapsulated assertions may have changed, so the new list should be sent to the local EncapsulatedAssertionRegistry.
     */
    private void loadEncapsulatedAssertionConfigs(boolean updateLocalRegistry) {
        try {
            Collection<EncapsulatedAssertionConfig> configs = Registry.getDefault().getEncapsulatedAssertionAdmin().findAllEncapsulatedAssertionConfigs();
            iconCache.clear();
            eacTableModel.setRows(new ArrayList<EncapsulatedAssertionConfig>(configs));
            if (updateLocalRegistry)
                TopComponents.getInstance().getEncapsulatedAssertionRegistry().replaceAllRegisteredConfigs(configs);
        } catch (FindException e) {
            showError("Unable to load encapsulated assertion configurations", e);
        }
    }

    private void doDeleteEnapsulatedAssertionConfig(EncapsulatedAssertionConfig config) {
        try {
            Registry.getDefault().getEncapsulatedAssertionAdmin().deleteEncapsulatedAssertionConfig(config.getOid());
            loadEncapsulatedAssertionConfigs(true);
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
