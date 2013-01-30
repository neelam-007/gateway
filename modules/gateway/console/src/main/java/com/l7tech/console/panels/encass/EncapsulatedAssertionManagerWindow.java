package com.l7tech.console.panels.encass;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.CreateEncapsulatedAssertionAction;
import com.l7tech.console.action.EditEncapsulatedAssertionAction;
import com.l7tech.console.action.ViewEncapsulatedAssertionAction;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.policy.EncapsulatedAssertionRegistry;
import com.l7tech.console.policy.exporter.ConsoleExternalReferenceFinder;
import com.l7tech.console.policy.exporter.EncapsulatedAssertionConfigExportUtil;
import com.l7tech.console.policy.exporter.PolicyExportUtils;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.EncapsulatedAssertionConsoleUtil;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
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
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.PolicyExporter;
import com.l7tech.policy.exporter.PolicyImporter;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.transform.dom.DOMResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class EncapsulatedAssertionManagerWindow extends JDialog {
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
                                doDeleteEnapsulatedAssertionConfig(config);
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
        FileChooserUtil.loadSingleFile(this, "Import Encapsulated Assertion", ENCASS_FILE_FILTER, ".xml", new Functions.UnaryThrows<Boolean, FileInputStream, IOException>() {
            @Override
            public Boolean call(final FileInputStream fis) throws IOException {
                // parse xml
                final Document doc;
                try {
                    doc = XmlUtil.parse(fis);
                } catch (final SAXException e) {
                    // fail early if file isn't valid xml
                    showError("File contents are invalid", null);
                    return false;
                }

                // read config
                final Element encassElement = XmlUtil.findFirstChildElementByName(doc.getDocumentElement(), "http://ns.l7tech.com/secureSpan/1.0/encass", "EncapsulatedAssertion");
                if (encassElement == null) {
                    throw new IOException("Export document does not contain an EncapsulatedAssertionConfig element");
                }
                final EncapsulatedAssertionConfig config = EncapsulatedAssertionConfigExportUtil.getInstance().importFromNode(encassElement);
                config.setOid(EncapsulatedAssertionConfig.DEFAULT_OID);

                // resolve conflicts
                Policy conflictingPolicy = null;
                try {
                    conflictingPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByUniqueName(config.getPolicy().getName());
                } catch (final FindException e) {
                    showError("Error importing Encapsulated Assertion", null);
                    return false;
                }

                EncapsulatedAssertionConfig conflictingConfig = null;
                try {
                    // check guid
                    conflictingConfig = Registry.getDefault().getEncapsulatedAssertionAdmin().findByGuid(config.getGuid());
                } catch (final FindException e) {
                    // no guid conflict
                    // check config and policy names
                    try {
                        conflictingConfig = Registry.getDefault().getEncapsulatedAssertionAdmin().findByUniqueName(config.getName());
                    } catch (final FindException fe) {
                        showError("Error importing Encapsulated Assertion", null);
                        return false;
                    }
                }

                if (conflictingConfig != null || conflictingPolicy != null) {
                    final EncapsulatedAssertionConfigConflictDialog conflictDialog = new EncapsulatedAssertionConfigConflictDialog(EncapsulatedAssertionManagerWindow.this, config, conflictingConfig, conflictingPolicy);
                    conflictDialog.pack();
                    Utilities.centerOnParentWindow(conflictDialog);
                    DialogDisplayer.display(conflictDialog, new ConflictCallback(config, conflictingConfig, conflictDialog, doc));
                } else {
                    savePolicyAndConfig(doc, config);
                }

                return true;
            }
        });
    }

    /**
     * Callback which handles the conflict resolution input from the user.
     */
    private class ConflictCallback implements Runnable {
        /**
         * EncapsulatedAssertionConfig that is being imported.
         */
        private final EncapsulatedAssertionConfig toImport;
        /**
         * EncapsulatedAssertionConfig which conflicts with the imported one. Can be null.
         */
        private final EncapsulatedAssertionConfig conflictingConfig;
        /**
         * EncapsulatedAssertionConfigConflictDialog which contains the results of the user input.
         */
        private final EncapsulatedAssertionConfigConflictDialog conflictDialog;
        /**
         * Backing policy xml document for the imported EncapsulatedAssertionConfig.
         */
        private final Document policyDocument;

        private ConflictCallback(@NotNull final EncapsulatedAssertionConfig toImport, @Nullable final EncapsulatedAssertionConfig conflictingConfig,
                                 @NotNull final EncapsulatedAssertionConfigConflictDialog conflictDialog, @NotNull final Document policyDocument) {
            this.toImport = toImport;
            this.conflictingConfig = conflictingConfig;
            this.conflictDialog = conflictDialog;
            this.policyDocument = policyDocument;
        }

        @Override
        public void run() {
            if (conflictDialog.isConfirmed()) {
                if (conflictingConfig != null && conflictingConfig.getGuid().equals(toImport.getGuid()) && conflictDialog.isOverwrite()) {
                    // update
                    toImport.setOid(conflictingConfig.getOid());
                    toImport.setVersion(conflictingConfig.getVersion());
                } else if (conflictingConfig != null && conflictingConfig.getGuid().equals(toImport.getGuid()) && !conflictDialog.isOverwrite()) {
                    // create new
                    toImport.setGuid(null);
                }
                toImport.setName(conflictDialog.getEncassName());
                toImport.getPolicy().setName(conflictDialog.getPolicyName());
                savePolicyAndConfig(policyDocument, toImport);
            }
        }
    }

    private void savePolicyAndConfig(@NotNull final Document doc, @NotNull final EncapsulatedAssertionConfig config) {
        try {
            // create policy
            final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, config.getPolicy().getName(), null, false);
            final WspReader wspReader = TopComponents.getInstance().getApplicationContext().getBean("wspReader", WspReader.class);
            final ConsoleExternalReferenceFinder finder = new ConsoleExternalReferenceFinder();
            final PolicyImporter.PolicyImporterResult result = PolicyImporter.importPolicy(policy, doc, PolicyExportUtils.getExternalReferenceFactories(), wspReader, finder, finder, finder, finder );
            final Assertion newRoot = (result != null) ? result.assertion : null;
            if (newRoot == null) {
                throw new IOException("Export document contains an invalid or empty policy fragment");
            }
            final String newPolicyXml = WspWriter.getPolicyXml(newRoot);
            policy.setXml(newPolicyXml);
            PolicyExportUtils.addPoliciesToPolicyReferenceAssertions(policy.getAssertion(), result.policyFragments);
            final PolicyAdmin.SavePolicyWithFragmentsResult savePolicyResult = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true, result.policyFragments);
            policy.setOid(savePolicyResult.policyCheckpointState.getPolicyOid());
            policy.setGuid(savePolicyResult.policyCheckpointState.getPolicyGuid());
            final ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            tree.refresh();

            // update/create config
            config.setPolicy(policy);
            long oid = Registry.getDefault().getEncapsulatedAssertionAdmin().saveEncapsulatedAssertionConfig(config);
            loadEncapsulatedAssertionConfigs(true);
            selectConfigByOid(oid);
        } catch (final Exception e) {
            showError("Error saving Encapsulated Assertion", e);
        }
    }

    private void doExport(final EncapsulatedAssertionConfig config) {
        FileChooserUtil.saveSingleFileWithOverwriteConfirmation(this, "Export Encapsulated Assertion", ENCASS_FILE_FILTER, ".xml", new FileUtils.Saver() {
            @Override
            public void doSave(FileOutputStream fos) throws IOException {
                try {
                    Assertion assertion = config.getPolicy().getAssertion();
                    final ConsoleExternalReferenceFinder finder = new ConsoleExternalReferenceFinder();
                    PolicyExporter exporter = new PolicyExporter(finder, finder);
                    Document doc = exporter.exportToDocument(assertion, PolicyExportUtils.getExternalReferenceFactories());
                    final DocumentFragment fragment = doc.createDocumentFragment();
                    EncapsulatedAssertionConfigExportUtil.getInstance().export(config, new DOMResult(fragment));
                    doc.getDocumentElement().appendChild(fragment);
                    XmlUtil.nodeToFormattedOutputStream(doc, fos);
                } catch (SAXException e) {
                    throw new IOException(e);
                }
            }
        });
    }

    private EncapsulatedAssertionConfig deserialize(String b64) throws IOException {
        try {
            Object got = new ObjectInputStream(new ByteArrayInputStream(HexUtils.decodeBase64(b64))).readObject();
            if (got instanceof EncapsulatedAssertionConfig) {
                return (EncapsulatedAssertionConfig) got;
            }
            throw new IOException("Export document did not contain a serialized EncapsulatedAssertionConfig");
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * Display the config properties dialog.
     * @param config the config to display.
     * @param promptForAutoPopulateOnNew whether the user should be asked if they want to auto-populate inputs and outputs if this is a new config.
     */
    private void doProperties(@NotNull final EncapsulatedAssertionConfig config, final boolean promptForAutoPopulateOnNew) {
        final SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
        final boolean isNew = Long.valueOf(PersistentEntity.DEFAULT_OID).equals(config.getOid());
        if (isNew && securityProvider.hasPermission(new AttemptedCreateSpecific(EntityType.ENCAPSULATED_ASSERTION, config))) {
            new CreateEncapsulatedAssertionAction(config, new ConfigChangeWindowUpdater(config), promptForAutoPopulateOnNew).actionPerformed(null);
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
        removeButton.setEnabled(flags.canDeleteSome() && haveConfig);
        propertiesButton.setEnabled(haveConfig);
        cloneButton.setEnabled(flags.canCreateSome() && haveConfig);
        exportButton.setEnabled(haveConfig);
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

    private void showError(@NotNull final String message, @Nullable final Throwable e) {
        String error = message;
        if (e != null) {
            error = error + ": " + ExceptionUtils.getMessage(e);
        }
        DialogDisplayer.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE, null);
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
