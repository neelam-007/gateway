package com.l7tech.console.panels.encass;

import com.l7tech.console.action.CreateEncapsulatedAssertionAction;
import com.l7tech.console.action.EditEncapsulatedAssertionAction;
import com.l7tech.console.action.ViewEncapsulatedAssertionAction;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.policy.EncapsulatedAssertionRegistry;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.console.util.EncapsulatedAssertionConsoleUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
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
    private JButton cloneButton;

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

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final EncapsulatedAssertionConfig selected = getSelectedConfig();
                if (selected != null) {
                    final EncapsulatedAssertionConfig clone = selected.getCopy();
                    clone.setGuid(null);
                    clone.setOid(Long.valueOf(PersistentEntity.DEFAULT_OID));
                    clone.setName(selected.getName() + " Copy");
                    doProperties(clone);
                }
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
        Utilities.setRowSorter(eacTable, eacTableModel);
        loadEncapsulatedAssertionConfigs(false);
        enableOrDisable();
    }

    /**
     * Display the config properties dialog.
     */
    private void doProperties(@NotNull final EncapsulatedAssertionConfig config) {
        final SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
        final boolean isNew = Long.valueOf(PersistentEntity.DEFAULT_OID).equals(config.getOid());
        if (isNew && securityProvider.hasPermission(new AttemptedCreateSpecific(EntityType.ENCAPSULATED_ASSERTION, config))) {
            new CreateEncapsulatedAssertionAction(config, new ConfigChangeWindowUpdater(config)).actionPerformed(null);
        } else if (!isNew && securityProvider.hasPermission(new AttemptedUpdate(EntityType.ENCAPSULATED_ASSERTION, config))) {
            new EditEncapsulatedAssertionAction(Collections.singleton(config), new ConfigChangeWindowUpdater(config)).actionPerformed(null);
        } else {
            new ViewEncapsulatedAssertionAction(Collections.singleton(config), null).actionPerformed(null);
        }
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
        if (row < 0) {
            return null;
        }
        final int modelRow = eacTable.convertRowIndexToModel(row);
        return eacTableModel.getRowObject(modelRow);
    }

    private void enableOrDisable() {
        boolean haveConfig = getSelectedConfig() != null;
        removeButton.setEnabled(haveConfig);
        propertiesButton.setEnabled(haveConfig);
        cloneButton.setEnabled(haveConfig);

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
            final Collection<EncapsulatedAssertionConfig> configs = Registry.getDefault().getEncapsulatedAssertionAdmin().findAllEncapsulatedAssertionConfigs();
            iconCache.clear();
            eacTableModel.setRows(new ArrayList<EncapsulatedAssertionConfig>(configs));
            if (updateLocalRegistry) {
                final EncapsulatedAssertionRegistry encapsulatedAssertionRegistry = TopComponents.getInstance().getEncapsulatedAssertionRegistry();
                encapsulatedAssertionRegistry.replaceAllRegisteredConfigs(configs);
            }
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

    /**
     * Call when EncapsulatedAssertionConfigs have changed and the window needs updating.
     */
    private class ConfigChangeWindowUpdater implements Runnable {
        private final EncapsulatedAssertionConfig config;
        private ConfigChangeWindowUpdater (@NotNull final EncapsulatedAssertionConfig config) {
            this.config = config;
        }
        @Override
        public void run() {
            loadEncapsulatedAssertionConfigs(false);
            selectConfigByOid(config.getOid());
        }
    }
}
