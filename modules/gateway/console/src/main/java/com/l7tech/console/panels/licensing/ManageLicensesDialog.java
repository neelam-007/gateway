package com.l7tech.console.panels.licensing;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.licensing.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.DateUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.*;

/**
 * Dialog for installing, removing, and viewing the details of licenses.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class ManageLicensesDialog extends JDialog {
    private static final String TITLE = "Manage Gateway Cluster Licenses";
    private static final String INVALID_LICENSE = "INVALID LICENSE";
    private static final String EXPIRED = "EXPIRED";
    private static final String NA = "N/A";

    protected static final Logger logger = Logger.getLogger(ManageLicensesDialog.class.getName());

    private JButton installButton;
    private JButton closeButton;
    private JButton removeButton;
    private JButton viewDetailsButton;
    private JTable licenseTable;
    private JPanel rootPanel;
    private SimpleTableModel<LicenseTableRow> licenseTableModel;

    private boolean disconnectManagerOnClose = false;
    private ClusterStatusAdmin admin = Registry.getDefault().getClusterStatusAdmin();

    public ManageLicensesDialog(Window owner) {
        super(owner);
        init();
    }

    private void init() {
        setTitle(TITLE);
        add(rootPanel);

        configureLicenseTable();
        populateLicenseTable();

        installButton.addActionListener(getInstallActionListener());

        removeButton.setEnabled(false);
        removeButton.addActionListener(getRemoveActionListener());

        viewDetailsButton.setEnabled(false);
        viewDetailsButton.addActionListener(getViewDetailsActionListener());

        closeButton.addActionListener(getCloseActionListener());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        pack();
    }

    private ActionListener getInstallActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                installLicense();
            }
        };
    }

    private ActionListener getRemoveActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doRemoveSelectedLicense();
            }
        };
    }

    private ActionListener getViewDetailsActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewDetails();
            }
        };
    }

    private ActionListener getCloseActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        };
    }

    private void installLicense() {
        String licenseXml;

        try (InputStream is = getLicenseInputStream()) {
            if (null == is) // user cancelled, or there was an error reading the file
                return;

            licenseXml = XmlUtil.nodeToString(XmlUtil.parse(is));
        } catch (IOException e) {
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                    "The license file could not be read:\n" + ExceptionUtils.getMessage(e),
                    "Unable to read license file", JOptionPane.ERROR_MESSAGE, null);
            return;
        } catch (SAXException e) {
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                    "The specified license file is not well-formed XML:\n" + ExceptionUtils.getMessage(e),
                    "Unable to read license file", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        FeatureLicense newLicense;

        try {
            // create the FeatureLicense from the license file xml
            newLicense = admin.createLicense(new LicenseDocument(licenseXml));

            // check if the license is valid
            admin.validateLicense(newLicense);
        } catch (InvalidLicenseException e) {
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                    "That license is invalid and cannot be installed:\n" + ExceptionUtils.getMessage(e),
                    "Invalid license", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        CompositeLicense currentComposite = ConsoleLicenseManager.getInstance().getLicense();

        if (null != currentComposite && currentComposite.containsLicenseWithId(newLicense.getId())) {
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this, "License " + newLicense.getId() + " is already installed.",
                    "Invalid license", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        // Prompt user to accept license agreement
        if (!getEulaAcceptance(newLicense)) {
            return;
        }

        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            admin.installLicense(newLicense);
        } catch (LicenseInstallationException e) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            logger.log(Level.SEVERE, "Unable to install license:\n" + ExceptionUtils.getMessage(e), e);

            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this, ExceptionUtils.getMessage(e),
                    "Unable to install license", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        // update the console assertion registry, flush cached security zones
        try {
            TopComponents.getInstance().getAssertionRegistry().updateModularAssertions();
            TopComponents.getInstance().getAssertionRegistry().updateCustomAssertions();
            TopComponents.getInstance().getEncapsulatedAssertionRegistry().updateEncapsulatedAssertions();
            SecurityZoneUtil.flushCachedSecurityZones();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Unable to update modular assertions: " + ExceptionUtils.getMessage(e) + ".",
                    ExceptionUtils.getDebugException(e));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to update encapsulated assertions: " + ExceptionUtils.getMessage(e) + ".",
                    ExceptionUtils.getDebugException(e));
        }

        updateConsoleLicenseManager();
        populateLicenseTable();
        selectTableRowByLicenseId(newLicense.getId());

        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private void doRemoveSelectedLicense() {
        final LicenseDocument selected = getSelectedRow().getLicenseDocument();

        String confirmationDialogTitle = "Confirm License Removal";
        String confirmationDialogMessage = "This will irrevocably destroy the existing gateway license and cannot\n" +
                "be undone. It will also cause the Policy Manager to disconnect when\n" +
                "you are finished managing licenses.\n" +
                "Really remove this gateway license?";

        DialogDisplayer.showSafeConfirmDialog(
                this,
                confirmationDialogMessage,
                confirmationDialogTitle,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.CANCEL_OPTION) {
                            return;
                        }

                        try {
                            admin.uninstallLicense(selected);
                            disconnectManagerOnClose = true;
                        } catch (LicenseRemovalException e) {
                            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this, ExceptionUtils.getMessage(e),
                                    "Unable to remove license", JOptionPane.ERROR_MESSAGE, null);
                            return;
                        }

                        updateConsoleLicenseManager();
                        populateLicenseTable();
                    }
                }
        );
    }

    private void viewDetails() {
        FeatureLicense featureLicense = getSelectedRow().getFeatureLicense();

        if (featureLicense == null) {
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                    "That license could not be parsed.", "Cannot view invalid license", JOptionPane.OK_OPTION, null);
        } else {
            LicenseDetailsDialog detailsDialog = new LicenseDetailsDialog(this, featureLicense);
            Utilities.centerOnParentWindow(detailsDialog);
            DialogDisplayer.display(detailsDialog);
        }
    }

    private void onClose() {
        dispose();

        if (disconnectManagerOnClose) {
            // warn/remind user
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                    "The Policy Manager will now disconnect.", "Disconnection Required", JOptionPane.OK_OPTION, null);

            // disconnect from SSG
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();
        }
    }

    /**
     * Show the EULA dialog and obtain user acceptance.
     *
     * @param license the FeatureLicense that is about to be installed.  Required.
     * @return true if the user agrees to the EULA
     */
    private boolean getEulaAcceptance(FeatureLicense license) {
        final AcceptEulaDialog eulaDialog = new AcceptEulaDialog(this, license);

        Utilities.centerOnParentWindow(eulaDialog);
        eulaDialog.setVisible(true);

        return eulaDialog.isLicenseAccepted();
    }

    /**
     * Find a new license InputStream from the user.
     * @return InputStream for the License contents
     */
    private InputStream getLicenseInputStream() {
        try {
            return AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                @Override
                public InputStream run() {
                    try {
                        return getLicenseStreamFromFile();
                    } catch (IOException e) {
                        DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                                "The license file could not be read:\n" + ExceptionUtils.getMessage(e),
                                "Unable to read license file", JOptionPane.ERROR_MESSAGE, null);
                    }

                    return null;
                }
            });
        } catch (AccessControlException e) {
            return getLicenseStreamFromTextBox();
        }
    }

    private InputStream getLicenseStreamFromTextBox() {
        final JDialog textEntryDialog = new JDialog(this, "License XML", true);

        textEntryDialog.setLayout(new BorderLayout());
        textEntryDialog.add(new JLabel("Paste the new license XML below:"), BorderLayout.NORTH);

        final TextArea licenseTextArea = new TextArea(15, 80);
        textEntryDialog.add(licenseTextArea, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(Box.createGlue());
        buttons.add(okButton);
        buttons.add(cancelButton);
        textEntryDialog.add(buttons, BorderLayout.SOUTH);

        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});

        textEntryDialog.pack();

        Utilities.centerOnParentWindow(textEntryDialog);

        final AtomicBoolean ok = new AtomicBoolean(false);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok.set(true);
                textEntryDialog.dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textEntryDialog.dispose();
            }
        });

        textEntryDialog.getRootPane().setDefaultButton(okButton);
        textEntryDialog.setVisible(true);

        return ok.get()
                ? new ByteArrayInputStream(HexUtils.encodeUtf8(licenseTextArea.getText()))
                : null;
    }

    /**
     * Try to get the license stream from a file on disk, if we have permission to do so.
     *
     * @return  the license stream, or null to cancel.
     * @throws AccessControlException  if our privileges are insufficient
     * @throws java.io.IOException if the selected file can't be opened or read
     */
    private InputStream getLicenseStreamFromFile() throws AccessControlException, IOException {
        JFileChooser fc = FileChooserUtil.createJFileChooser();

        fc.setDialogTitle("Select license file to install");

        fc.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "License files (*.xml)";
            }

            @Override
            public boolean accept(File f) {
                final String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".xml");
            }
        });

        int result = fc.showOpenDialog(ManageLicensesDialog.this);

        if (result != JFileChooser.APPROVE_OPTION)
            return null;

        File file = fc.getSelectedFile();

        if (file == null)
            return null;

        return new FileInputStream(file);
    }

    private void updateConsoleLicenseManager() {
        // set the new composite license in the Console License Manager
        Registry.getDefault().getLicenseManager().setLicense(admin.getCompositeLicense());
    }

    private void selectTableRowByLicenseId(long id) {
        for (LicenseTableRow row : licenseTableModel.getRows()) {
            if (id == row.getFeatureLicense().getId()) {
                int index = licenseTableModel.getRowIndex(row);
                licenseTable.setRowSelectionInterval(index, index);
                return;
            }
        }

        // shouldn't ever reach here, and it's not a disaster if the row can't be found
    }

    private LicenseTableRow getSelectedRow() {
        return licenseTableModel.getRowObject(licenseTable.getSelectedRow());
    }

    private void configureLicenseTable() {
        licenseTable.setAutoCreateRowSorter(true);

        Col<LicenseTableRow> descriptionCol = column("Description", 100, 200, 1000,
                Functions.<String, LicenseTableRow>propertyTransform(LicenseTableRow.class, "description"), String.class);
        Col<LicenseTableRow> expiryCol = column("Expiry", 100, 300, 1000,
                Functions.<Date, LicenseTableRow>propertyTransform(LicenseTableRow.class, "expiry"), Date.class);

        licenseTableModel = configureTable(licenseTable, descriptionCol, expiryCol);

        licenseTable.getColumnModel().getColumn(0).setCellRenderer(new DescriptionWarningRenderer());
        licenseTable.getColumnModel().getColumn(1).setCellRenderer(new ExpiryWarningRenderer());

        licenseTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel model = (ListSelectionModel) e.getSource();
                removeButton.setEnabled(!model.isSelectionEmpty());
                viewDetailsButton.setEnabled(!model.isSelectionEmpty());
            }
        });
    }

    private void populateLicenseTable() {
        CompositeLicense compositeLicense = Registry.getDefault().getLicenseManager().getLicense();

        ArrayList<LicenseTableRow> tableRows = new ArrayList<>();

        if (null != compositeLicense) {
            for (FeatureLicense license : compositeLicense.getValidFeatureLicenses().values()) {
                tableRows.add(new LicenseTableRow(license,
                        license.getLicenseDocument(), license.getDescription(), license.getExpiryDate(), false));
            }

            for (FeatureLicense license : compositeLicense.getExpiredFeatureLicenses().values()) {
                tableRows.add(new LicenseTableRow(license,
                        license.getLicenseDocument(), license.getDescription(), license.getExpiryDate(), true));
            }

            for (FeatureLicense license : compositeLicense.getInvalidFeatureLicenses().values()) {
                tableRows.add(new LicenseTableRow(license,
                        license.getLicenseDocument(), license.getDescription(), license.getExpiryDate(), false));
            }

            for (LicenseDocument document : compositeLicense.getInvalidLicenseDocuments()) {
                tableRows.add(new LicenseTableRow(null, document, INVALID_LICENSE, null, false));
            }
        }

        licenseTableModel.setRows(tableRows);
    }

    public static class LicenseTableRow {
        private final FeatureLicense featureLicense;
        private final LicenseDocument licenseDocument;
        private final String description;
        private final Date expiry;
        private final boolean expired;

        public LicenseTableRow(FeatureLicense featureLicense, LicenseDocument licenseDocument,
                               String description, Date expiry, boolean expired) {
            this.featureLicense = featureLicense;
            this.licenseDocument = licenseDocument;
            this.description = description;
            this.expiry = expiry;
            this.expired = expired;
        }

        public FeatureLicense getFeatureLicense() {
            return featureLicense;
        }

        public LicenseDocument getLicenseDocument() {
            return licenseDocument;
        }

        public String getDescription() {
            return description;
        }

        public Date getExpiry() {
            return expiry;
        }

        public boolean isExpired() {
            return expired;
        }
    }

    private class DescriptionWarningRenderer extends WarningRenderer {
        public DescriptionWarningRenderer() {
            super();
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            handleSelectionDecoration(table, isSelected, hasFocus);

            if (null == value) {
                setPlainText(NA);
            } else {
                String description = (String) value;

                if (INVALID_LICENSE.equals(description)) {
                    setWarningText(INVALID_LICENSE);
                } else {
                    setPlainText(description);
                }
            }

            return this;
        }
    }

    private class ExpiryWarningRenderer extends WarningRenderer {
        public ExpiryWarningRenderer() {
            super();
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            handleSelectionDecoration(table, isSelected, hasFocus);

            if (null == value) {
                setWarningText(NA);
            } else {
                LicenseTableRow rowObject = licenseTableModel.getRowObject(row);

                if (rowObject.isExpired()) {
                    setWarningText(EXPIRED);
                } else {
                    Date expiry = (Date) value;

                    setPlainText(new SimpleDateFormat("yyyy-MM-dd").format(expiry) +
                            " (" + DateUtils.makeRelativeDateMessage(expiry, false) + ")");
                }
            }

            return this;
        }
    }

    private abstract class WarningRenderer extends JLabel implements TableCellRenderer {
        protected final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

        public WarningRenderer() {
            setOpaque(true);
        }

        protected void handleSelectionDecoration(JTable table, boolean isSelected, boolean hasFocus) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            }
            else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            if (hasFocus) {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            } else {
                setBorder(noFocusBorder);
            }
        }

        protected void setPlainText(String text) {
            setText(text);
            setFont(new Font(getFont().getName(), Font.PLAIN, getFont().getSize()));
        }

        protected void setWarningText(String text) {
            setText(text);
            setFont(new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
        }
    }
}
