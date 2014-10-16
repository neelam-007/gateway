package com.l7tech.external.assertions.policybundleexporter.console;

import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.policybundleexporter.ComponentInfo;
import com.l7tech.external.assertions.policybundleexporter.PolicyBundleExporterAdmin;
import com.l7tech.external.assertions.policybundleexporter.PolicyBundleExporterProperties;
import com.l7tech.external.assertions.policybundleexporter.PolicyBundleExporterAssertion;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Dialog used to create a bundle installer.
 */
public class CreateBundleInstallerDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(CreateBundleInstallerDialog.class.getName());

    private JPanel contentPanel;
    private JTextField nameTextField;
    private JTextField versionTextField;
    private JTextField schemaNamespaceTextField;
    private JTextField bundleFolderTextField;
    private JButton selectBundleFolderButton;
    private JTable componentsTable;
    private JButton addComponentButton;
    private JButton editComponentButton;
    private JButton removeComponentButton;
    private JButton createButton;
    private JButton cancelButton;
    private JTextField iconTextField;
    private JButton selectIconButton;

    private FolderHeader bundleFolder;
    private SimpleTableModel<ComponentInfo> componentsModel;

    public CreateBundleInstallerDialog(Frame owner) {
        super(owner,"Create Policy Bundle Installer", true);
        initialize();
    }

    private void initialize() {
        // todo (kpak) - remove when schema namespace is supported.
        schemaNamespaceTextField.setText("Not implemented yet");
        schemaNamespaceTextField.setEnabled(false);

        bundleFolderTextField.setEnabled(false);

        selectBundleFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSelectBundleFolder();
            }
        });

        componentsModel = TableUtil.configureTable(
                componentsTable,
                TableUtil.column("Name", 30, 200, 99999, new Functions.Unary<String, ComponentInfo>() {
                    @Override
                    public String call(ComponentInfo componentInfo) {
                        return componentInfo.getName();
                    }
                }, String.class),
                TableUtil.column("Description", 30, 400, 99999, new Functions.Unary<String, ComponentInfo>() {
                    @Override
                    public String call(ComponentInfo componentInfo) {
                        return componentInfo.getDescription();
                    }
                }, String.class),
                TableUtil.column("Version", 30, 100, 99999, new Functions.Unary<String, ComponentInfo>() {
                    @Override
                    public String call(ComponentInfo componentInfo) {
                        return componentInfo.getVersion();
                    }
                }, String.class)
        );

        componentsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Utilities.setRowSorter(componentsTable, componentsModel, new int[]{0, 1, 2}, new boolean[]{true, true, true}, null);
        ((TableRowSorter) componentsTable.getRowSorter()).setSortsOnUpdates(true);

        componentsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                editComponentButton.setEnabled(componentsTable.getSelectedRows().length == 1);
                removeComponentButton.setEnabled(componentsTable.getSelectedRows().length > 0);
            }
        });

        addComponentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAddComponent();
            }
        });

        editComponentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onEditComponent();
            }
        });
        editComponentButton.setEnabled(false);

        removeComponentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRemoveComponent();
            }
        });
        removeComponentButton.setEnabled(false);

        Utilities.setDoubleClickAction(componentsTable, editComponentButton);

        iconTextField.setEnabled(false);
        selectIconButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSelectIcon();
            }
        });

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCreate();
            }
        });
        getRootPane().setDefaultButton(createButton);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        contentPanel.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setContentPane(contentPanel);
        pack();

        // todo (kpak) - remove populate with default values.
        //
        nameTextField.setText("MyBundle");
        versionTextField.setText("1.0");
    }

    private void onSelectBundleFolder() {
        if (componentsModel.getRowCount() > 0) {
            // todo (kpak) - display warning that this will delete added components, if already added.
        }

        FolderSelectionDialog dlg = new FolderSelectionDialog(this);
        if (bundleFolder != null) {
            dlg.setFolder(bundleFolder);
        }
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);

        if (dlg.getIsConfirmed()) {
            bundleFolder = dlg.getFolder();
            bundleFolderTextField.setText(bundleFolder.getPath());
        }
    }

    private void onAddComponent() {
        if (bundleFolder == null) {
            DialogDisplayer.showMessageDialog(this, "The Bundle Folder must be selected.", "Error", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        ComponentPropertiesDialog dlg = new ComponentPropertiesDialog(this, bundleFolder.getGoid());
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);

        if (dlg.getIsConfirmed()) {
            componentsModel.addRow(dlg.getComponentInfo());
        }
    }

    private void onEditComponent() {
        int selectedRow = componentsTable.getSelectedRow();
        if (selectedRow != -1) {
            int modelIndex = componentsTable.convertRowIndexToModel(selectedRow);
            ComponentInfo componentInfo = componentsModel.getRowObject(modelIndex);
            ComponentPropertiesDialog dlg = new ComponentPropertiesDialog(this, bundleFolder.getGoid());
            dlg.setComponentInfo(componentInfo);
            Utilities.centerOnParentWindow(dlg);
            DialogDisplayer.display(dlg);

            if (dlg.getIsConfirmed()) {
                componentsModel.setRowObject(modelIndex, dlg.getComponentInfo());
            }
        }
    }

    private void onRemoveComponent() {
        int[] selectedRows = componentsTable.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int selectedRow = selectedRows[i];
            if (selectedRow != -1) {
                int modelIndex = componentsTable.convertRowIndexToModel(selectedRow);
                componentsModel.removeRowAt(modelIndex);
            }
        }
    }

    private void onSelectIcon() {
        JFileChooser fc = FileChooserUtil.createJFileChooser();
        fc.setDialogTitle("Select a File");
        fc.setMultiSelectionEnabled(false);
        fc.setFileFilter(FileUtils.getImageFileFilter());
        int ret = fc.showOpenDialog(CreateBundleInstallerDialog.this);

        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file != null) {
                iconTextField.setText(file.getAbsolutePath());
            }
        }
    }

    private boolean isFieldValid() {
        StringBuilder sb = new StringBuilder();

        String bundleName = nameTextField.getText().trim();
        if (bundleName.isEmpty()) {
            sb.append("The Bundle Name field must not be empty.\n");
        } else if (!bundleName.matches("^[a-zA-Z][a-zA-Z0-9]{0,30}$")) {
            sb.append("The Bundle Name may be up to 30 letters and numbers with no spaces or special characters.\n");
        }

        if (versionTextField.getText().trim().isEmpty()) {
            sb.append("The Bundle Version field must not be empty.\n");
        }

        // todo (kpak) - validate schema namespace
        /*
        if (schemaNamespaceTextField.getText().trim().isEmpty()) {
            sb.append("The Bundle Schema Namespace field must not be empty.\n");
        }
        */

        if (bundleFolder == null) {
            sb.append("The Bundle Folder must be selected.\n");
        }

        if (componentsModel.getRowCount() == 0) {
            sb.append("At least one Component must be added.\n");
        }

        String error = sb.toString();
        if (!error.isEmpty()) {
            DialogDisplayer.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        return true;
    }

    private void onCreate() {
        if (!isFieldValid()) {
            return;
        }

        String bundleName = nameTextField.getText().trim();
        String bundleVersion = versionTextField.getText().trim();
        List<ComponentInfo> componentInfoList = componentsModel.getRows();
        PolicyBundleExporterProperties exportProperties = new PolicyBundleExporterProperties(bundleName, bundleVersion, bundleFolder, componentInfoList);

        if (!iconTextField.getText().trim().isEmpty()) {
            exportProperties.setActionIconFilePath(iconTextField.getText().trim());
        }

        try {
            final PolicyBundleExporterAdmin admin = getExtensionInterface();

            // get installer aar bytes and server module dependency file names
            // TODO provide user option to skip finding server module dependency file names.
            // TODO Finding dependencies is noticeably slower, user may not need to download each time in the solution kit authoring process.
            Either<String, PolicyBundleExporterAdmin.InstallerAarFile> installerAarResult = AdminGuiUtils.doAsyncAdmin(admin, this, "Creating Policy Bundle Installer",
                    "Policy Bundle Installer is being created.", admin.generateInstallerAarFile(exportProperties));

            if (installerAarResult.isRight()) {
                final PolicyBundleExporterAdmin.InstallerAarFile installerAarFile = installerAarResult.right();
                final String secureSpanVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor() + "." + BuildInfo.getProductVersionSubMinor();
                final String defaultFilename = bundleName + "InstallerAssertion-" + secureSpanVersion + ".aar";
                // save installer aar locally
                boolean successful = FileChooserUtil.saveSingleFileWithOverwriteConfirmation(this,
                        "Save .AAR File", FileChooserUtil.buildFilter(".aar", "(*.aar) Modular Assertion Archive"), ".aar", defaultFilename,
                        new FileUtils.ByteSaver(installerAarFile.getBytes()));
                if (successful) {

                    // TODO introduce a screen to allow user to select server module(s)
                    Either<String, PolicyBundleExporterAdmin.ServerModuleFile> serverModuleResult;
                    for (String serverModuleFileName : installerAarFile.getServerModuleFileNames()) {

                        // get server module bytes
                        serverModuleResult = AdminGuiUtils.doAsyncAdmin(admin, this,
                                "Downloading Server Module Files", "Downloading " + serverModuleFileName + ".", admin.getServerModuleFile(serverModuleFileName));

                        // save server module locally
                        FileUtils.saveFileSafely(FileChooserUtil.getStartingDirectory().getPath() + File.separator + serverModuleFileName, new FileUtils.ByteSaver(serverModuleResult.right().getBytes()));
                    }

                    StringBuilder readme = new StringBuilder();

                    // TODO
                    readme.append("TODO: Need top level license feature set? (e.g. \"set:Profile:Gateway\")  We currently show assertion level feature set (e.g. \"assertion:JdbcQuery\") for manual cross referencing to match one or more top level license feature(s).");
                    readme.append(System.lineSeparator());
                    readme.append(" TODO: How to get installer feature set? (e.g. \"assertion:OAuthInstaller\")");
                    readme.append(System.lineSeparator());
                    readme.append(System.lineSeparator());

                    readme.append("Dependent-Gateway-Licensing: ");
                    readme.append(installerAarFile.getAssertionFeatureSetNames().toString());
                    FileUtils.saveFileSafely(FileChooserUtil.getStartingDirectory().getPath() + File.separator + "README.txt",
                            new FileUtils.ByteSaver(readme.toString().getBytes()));

                    DialogDisplayer.showMessageDialog(this, "Policy Bundle Installer created successfully.", "Create Policy Bundle Installer", JOptionPane.INFORMATION_MESSAGE, null);
                    this.dispose();
                }
            } else {
                DialogDisplayer.showMessageDialog(this, installerAarResult.left(), "Create Policy Bundle Installer", JOptionPane.WARNING_MESSAGE, null);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private PolicyBundleExporterAdmin getExtensionInterface() {
        // interfaceClass and instanceIdentifier must match registered in assertion (for example see PolicyBundleExporterAssertion)
        return Registry.getDefault().getExtensionInterface(PolicyBundleExporterAdmin.class, PolicyBundleExporterAssertion.class.getName());
    }

    private void handleException(Exception e) {
        if (e instanceof InterruptedException) {
            // do nothing, user cancelled
            logger.info("User cancelled installation.");
        } else if (e instanceof InvocationTargetException) {
            DialogDisplayer.showMessageDialog(this, "Could not invoke installation on Gateway",
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        } else if (e instanceof RuntimeException) {
            DialogDisplayer.showMessageDialog(this, "Unexpected error occurred during installation: \n" + ExceptionUtils.getMessage(e),
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        } else {
            DialogDisplayer.showMessageDialog(this, "Unexpected error occurred during installation: \n" + ExceptionUtils.getMessage(e),
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        }
    }
}