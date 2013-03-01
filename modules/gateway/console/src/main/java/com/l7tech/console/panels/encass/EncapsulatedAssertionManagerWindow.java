package com.l7tech.console.panels.encass;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.AbstractEncapsulatedAssertionAction;
import com.l7tech.console.action.CreateEncapsulatedAssertionAction;
import com.l7tech.console.action.EditEncapsulatedAssertionAction;
import com.l7tech.console.action.ViewEncapsulatedAssertionAction;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.policy.EncapsulatedAssertionRegistry;
import com.l7tech.console.policy.exporter.ConsoleExternalReferenceFinder;
import com.l7tech.console.policy.exporter.EncapsulatedAssertionConfigExportUtil;
import com.l7tech.console.policy.exporter.PolicyExportUtils;
import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.EncapsulatedAssertionConsoleUtil;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.exporter.PolicyImportCancelledException;
import com.l7tech.policy.exporter.PolicyImporter;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class EncapsulatedAssertionManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionManagerWindow.class.getName());
    private static final FileFilter ENCASS_FILE_FILTER = FileChooserUtil.buildFilter(".xml", "Encapsulated Assertion (*.xml)");

    private JPanel contentPane;
    private JButton closeButton;
    private JTable eacTable;
    private JButton createButton;
    private JButton propertiesButton;
    private JButton removeButton;
    private JButton cloneButton;
    private JButton importButton;
    private JButton exportButton;

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
                doProperties(new EncapsulatedAssertionConfig(), true);
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
                    clone.setName(EntityUtils.getNameForCopy(selected.getName()));
                    doProperties(clone, false);
                }
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final EncapsulatedAssertionConfig config = getSelectedConfig();
                if (config != null)
                    doProperties(config, false);
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
                                doDeleteEncapsulatedAssertionConfig(config);
                        }
                    });
            }
        });

        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final EncapsulatedAssertionConfig config = getSelectedConfig();
                if (config != null)
                    doExport(config);
            }
        });

        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doImport();
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

    private void doImport() {
        FileChooserUtil.loadSingleFile(this, "Import Encapsulated Assertion", ENCASS_FILE_FILTER, null, new Functions.UnaryThrows<Boolean, FileInputStream, IOException>() {
            @Override
            public Boolean call(final FileInputStream fis) throws IOException {
                // parse xml
                final Document policyDoc;
                try {
                    policyDoc = XmlUtil.parse(fis);
                } catch (final SAXException e) {
                    // fail early if file isn't valid xml
                    showError("File contents are invalid", null);
                    return false;
                }

                // read config
                final Element encassElement = XmlUtil.findFirstChildElementByName(policyDoc.getDocumentElement(), "http://ns.l7tech.com/secureSpan/1.0/encass", "EncapsulatedAssertion");
                if (encassElement == null) {
                    throw new IOException("Export document does not contain an EncapsulatedAssertionConfig element");
                }
                final EncapsulatedAssertionConfig config = EncapsulatedAssertionConfigExportUtil.getInstance().importFromNode(encassElement, true);

                try {
                    // check guid
                    final EncapsulatedAssertionConfig sameGuid = Registry.getDefault().getEncapsulatedAssertionAdmin().findByGuid(config.getGuid());
                    // found guid conflict
                    DialogDisplayer.showOptionDialog(EncapsulatedAssertionManagerWindow.this,
                            "Found an existing Encapsulated Assertion with name " + sameGuid.getName() + ".",
                            "Import Encapsulated Assertion", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                            new Object[]{ "Overwrite", "Create New", "Cancel"}, "Update", new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == 0) {
                                try {
                                    EncapsulatedAssertionConsoleUtil.attachPolicies(Collections.singletonList(sameGuid));
                                    // update existing config
                                    config.setOid(sameGuid.getOid());
                                    config.setVersion(sameGuid.getVersion());
                                    // avoid naming conflicts by using the existing name
                                    config.setName(sameGuid.getName());
                                    // update existing policy
                                    final Policy existingPolicy = sameGuid.getPolicy();
                                    if (existingPolicy != null) {
                                        final Pair<Policy, HashMap<String, Policy>> fragmentResult = createPolicyFragment(existingPolicy.getName(), policyDoc);
                                        existingPolicy.setXml(fragmentResult.getKey().getXml());
                                        savePolicyAndConfig(existingPolicy, fragmentResult.getValue(), config);
                                    } else {
                                        // unable to attach backing policy
                                        handlePermissionDeniedForImport(null);
                                    }
                                } catch (final PolicyImportCancelledException e) {
                                    handleImportCancelledException(e);
                                } catch (final Exception e) {
                                    handleGenericException(e);
                                }
                            } else if (option == 1) {
                                // create new config and policy
                                config.setGuid(null);
                                resolveConflictsAndSave(policyDoc, config);
                            } else {
                                logger.log(Level.FINE, "Policy import cancelled.");
                            }
                        }
                    });
                } catch (final FindException e) {
                    // no guid conflict
                    // create new config and policy
                    resolveConflictsAndSave(policyDoc, config);
                }
                return true;
            }
        });
    }

    /**
     * Detects name collisions and asks the user to resolve them before saving.
     *
     * Displays an error dialog if an error occurs.
     *
     * @param policyDoc the backing policy xml Document.
     * @param config the EncapsulatedAssertionConfig to save which doesn't yet exist in the database.
     */
    private void resolveConflictsAndSave(@NotNull final Document policyDoc, @NotNull final EncapsulatedAssertionConfig config) {
        try {
            final EncapsulatedAssertionConfig conflictingConfig = Registry.getDefault().getEncapsulatedAssertionAdmin().findByUniqueName(config.getName());
            final Policy conflictingPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByUniqueName(config.getPolicy().getName());
            if (conflictingConfig != null || conflictingPolicy != null) {
                final EncapsulatedAssertionConfigConflictDialog conflictDialog =
                        new EncapsulatedAssertionConfigConflictDialog(this, config,
                                conflictingConfig, conflictingPolicy);
                conflictDialog.pack();
                Utilities.centerOnParentWindow(conflictDialog);
                DialogDisplayer.display(conflictDialog, new Runnable() {
                    @Override
                    public void run() {
                        if (conflictDialog.isConfirmed()) {
                            config.setName(conflictDialog.getEncassName());
                            try {
                                final Pair<Policy, HashMap<String, Policy>> fragmentResult = createPolicyFragment(conflictDialog.getPolicyName(), policyDoc);
                                savePolicyAndConfig(fragmentResult.getKey(), fragmentResult.getValue(), config);
                            } catch (final PolicyImportCancelledException e) {
                                handleImportCancelledException(e);
                            } catch (final PermissionDeniedException e) {
                                handlePermissionDeniedForImport(e);
                            }catch (final Exception e) {
                                handleGenericException(e);
                            }
                        }
                    }
                });
            } else {
                final Pair<Policy, HashMap<String, Policy>> fragmentResult = createPolicyFragment(config.getPolicy().getName(), policyDoc);
                savePolicyAndConfig(fragmentResult.getKey(), fragmentResult.getValue(), config);
            }
        } catch (final PolicyImportCancelledException e) {
            handleImportCancelledException(e);
        } catch (final PermissionDeniedException e) {
            handlePermissionDeniedForImport(e);
        } catch (final Exception e) {
            handleGenericException(e);
        }
    }

    /**
     * Creates a Policy fragment.
     * @param name the name of the Policy fragment to create.
     * @param policyDoc the Document containing the Policy xml.
     * @return a Pair where key = the created Policy fragment and value = map of any sub included Policy fragments (key = fragment guid).
     * @throws PolicyImportCancelledException
     * @throws IOException
     */
    private Pair<Policy, HashMap<String, Policy>> createPolicyFragment(@NotNull final String name, @NotNull final Document policyDoc)
            throws PolicyImportCancelledException, IOException {
        Pair<Policy, HashMap<String, Policy>> toReturn = null;
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, name, null, false);
        final WspReader wspReader = TopComponents.getInstance().getApplicationContext().getBean("wspReader", WspReader.class);
        final ConsoleExternalReferenceFinder finder = new ConsoleExternalReferenceFinder();
        final PolicyImporter.PolicyImporterResult result = PolicyImporter.importPolicy(policy, policyDoc,
                PolicyExportUtils.getExternalReferenceFactories(), wspReader, finder, finder, finder, finder );
        if (result.assertion != null) {
            final String newPolicyXml = WspWriter.getPolicyXml(result.assertion);
            policy.setXml(newPolicyXml);
            PolicyExportUtils.addPoliciesToPolicyReferenceAssertions(policy.getAssertion(), result.policyFragments);
            toReturn = new Pair<Policy, HashMap<String, Policy>>(policy, result.policyFragments);
        } else {
            throw new IOException("Document contains an invalid or empty policy fragment");
        }
        return toReturn;
    }

    private void savePolicyAndConfig(@NotNull final Policy policy, @Nullable HashMap<String, Policy> policyFragments, @NotNull final EncapsulatedAssertionConfig config)
            throws PolicyAssertionException, ObjectModelException, VersionException {
        final PolicyAdmin policyAdmin = Registry.getDefault().getPolicyAdmin();
        final PolicyAdmin.SavePolicyWithFragmentsResult savePolicyResult = policyAdmin.savePolicy(policy, true, policyFragments);
        final long policyOid = savePolicyResult.policyCheckpointState.getPolicyOid();
        policy.setOid(policyOid);
        policy.setGuid(savePolicyResult.policyCheckpointState.getPolicyGuid());
        final PolicyVersion version = policyAdmin.findLatestRevisionForPolicy(policyOid);
        if (version != null) {
            String artifactVersion = config.getProperty(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION);
            if (StringUtils.isBlank(artifactVersion)) {
                artifactVersion = "(unknown)";
            }
            policyAdmin.setPolicyVersionComment(policyOid, version.getOid(), "Imported Encapsulated Assertion with Artifact Version " + artifactVersion);
        } else {
            logger.log(Level.WARNING, "Unable to set policy version comment for imported encapsulated assertion");
        }
        final ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        tree.refresh();

        // update/create config
        config.setPolicy(policy);
        long oid = Registry.getDefault().getEncapsulatedAssertionAdmin().saveEncapsulatedAssertionConfig(config);
        loadEncapsulatedAssertionConfigs(true);
        selectConfigByOid(oid);
    }

    private void doExport(final EncapsulatedAssertionConfig config) {
        if (config.getPolicy() != null) {
            FileChooserUtil.saveSingleFileWithOverwriteConfirmation(this, "Export Encapsulated Assertion", ENCASS_FILE_FILTER, ".xml", new FileUtils.Saver() {
                @Override
                public void doSave(FileOutputStream fos) throws IOException {
                    try {
                        final Document exportDoc = EncapsulatedAssertionConfigExportUtil.getInstance().exportConfigAndPolicy(config);
                        XmlUtil.nodeToFormattedOutputStream(exportDoc, fos);
                    } catch (final SAXException e) {
                        throw new IOException(e);
                    }
                }
            });
        } else {
            // policy could not be attached
            showError("Export is unavailable because you do not have permission to read the underlying policy fragment or it is missing.", null);
        }
    }

    /**
     * Display the config properties dialog.
     * @param config the config to display.
     * @param promptForAutoPopulateOnNew whether the user should be asked if they want to auto-populate inputs and outputs if this is a new config.
     */
    private void doProperties(@NotNull final EncapsulatedAssertionConfig config, final boolean promptForAutoPopulateOnNew) {
        final boolean isNew = Long.valueOf(PersistentEntity.DEFAULT_OID).equals(config.getOid());
        AbstractEncapsulatedAssertionAction action = null;
        if (isNew) {
            action = new CreateEncapsulatedAssertionAction(config, new ConfigChangeWindowUpdater(config), promptForAutoPopulateOnNew);
        } else if (!isNew) {
            action = new EditEncapsulatedAssertionAction(Collections.singleton(config), new ConfigChangeWindowUpdater(config));
            if (!action.isAuthorized()) {
                action = new ViewEncapsulatedAssertionAction(Collections.singleton(config), null);
            }
        }
        if (action.isAuthorized()) {
            action.actionPerformed(null);
        } else {
            // user can't even read the encass config
            throw new PermissionDeniedException(OperationType.READ, EntityType.ENCAPSULATED_ASSERTION);
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
        final EncapsulatedAssertionConfig selected = getSelectedConfig();
        boolean haveConfig = selected != null;
        removeButton.setEnabled(flags.canDeleteSome() && haveConfig);
        propertiesButton.setEnabled(haveConfig);
        cloneButton.setEnabled(flags.canCreateSome() && haveConfig);
        // do not enable export if the backing policy could not be attached
        exportButton.setEnabled(haveConfig && selected.getPolicy() != null);
        importButton.setEnabled(flags.canCreateSome());
        createButton.setEnabled(flags.canCreateSome());
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
                return EncapsulatedAssertionConsoleUtil.getPolicyDisplayName(encapsulatedAssertionConfig);
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
            EncapsulatedAssertionConsoleUtil.attachPolicies(configs);
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

    private void doDeleteEncapsulatedAssertionConfig(EncapsulatedAssertionConfig config) {
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

    private void handlePermissionDeniedForImport(@Nullable final PermissionDeniedException e) {
        if (e != null) {
            logger.log(Level.WARNING, "Insufficient permissions for import: " + e.getMessage(), ExceptionUtils.getDebugException(e));
        }
        showError("You do not have the correct permissions to import.", null);
    }

    private void handleImportCancelledException(final PolicyImportCancelledException e) {
        logger.log(Level.FINE, "Policy import cancelled.", ExceptionUtils.getDebugException(e));
    }

    private void handleGenericException(final Exception e) {
        logger.log(Level.WARNING, "Error saving Encapsulated Assertion.", ExceptionUtils.getDebugException(e));
        showError("Error saving Encapsulated Assertion.", null);
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
