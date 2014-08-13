package com.l7tech.console.panels.licensing;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.licensing.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.*;
import org.xml.sax.SAXException;
import sun.security.util.Resources;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;
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
    protected static final Logger logger = Logger.getLogger(ManageLicensesDialog.class.getName());

    private static final ResourceBundle RESOURCES =
            Resources.getBundle("com.l7tech.console.panels.ManageLicensesDialog");

    private static final String TITLE = RESOURCES.getString("dialog.title");
    private static final String INVALID_LICENSE = RESOURCES.getString("table.label.invalid");
    private static final String EXPIRED = RESOURCES.getString("table.label.expired");
    private static final String NA = RESOURCES.getString("table.label.na");

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

        licenseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) viewDetails();
            }
        });

        installButton.addActionListener(getInstallActionListener());

        removeButton.setEnabled(false);
        removeButton.addActionListener(getRemoveActionListener());

        viewDetailsButton.setEnabled(false);
        viewDetailsButton.addActionListener(getViewDetailsActionListener());

        closeButton.addActionListener(getCloseActionListener());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
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
        WorkSpacePanel workSpace = TopComponents.getInstance().getCurrentWorkspace();

        // if any editor tabs are open, warn the user that they are about to be closed and allow cancellation
        if (workSpace.getTabbedPane().getTabCount() > 0) {
            int confirmResult = JOptionPane.showConfirmDialog(ManageLicensesDialog.this,
                    RESOURCES.getString("dialog.install.warnCloseTabs.message"),
                    RESOURCES.getString("dialog.install.warnCloseTabs.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (JOptionPane.OK_OPTION != confirmResult) {
                return;
            }

            workSpace.clearWorkspaceUnvetoable();
        }

        String licenseXml;

        try (InputStream is = getLicenseInputStream()) {
            if (null == is) // user cancelled, or there was an error reading the file
                return;

            licenseXml = XmlUtil.nodeToString(XmlUtil.parse(is));
        } catch (IOException e) {
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                    MessageFormat.format(RESOURCES.getString("dialog.install.error.read.io.message"),
                            ExceptionUtils.getMessage(e)),
                    RESOURCES.getString("dialog.install.error.read.title"),
                    JOptionPane.ERROR_MESSAGE, null);
            return;
        } catch (SAXException e) {
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                    MessageFormat.format(RESOURCES.getString("dialog.install.error.read.parsing.message"),
                            ExceptionUtils.getMessage(e)),
                    RESOURCES.getString("dialog.install.error.read.title"),
                    JOptionPane.ERROR_MESSAGE, null);
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
                    MessageFormat.format(RESOURCES.getString("dialog.install.error.invalid.message"),
                            ExceptionUtils.getMessage(e)),
                    RESOURCES.getString("dialog.install.error.invalid.title"),
                    JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        CompositeLicense currentComposite = ConsoleLicenseManager.getInstance().getLicense();

        if (null != currentComposite && currentComposite.containsLicenseWithId(newLicense.getId())) {
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                    MessageFormat.format(RESOURCES.getString("dialog.install.error.invalid.duplicate.message"),
                            Long.toString(newLicense.getId())),
                    RESOURCES.getString("dialog.install.error.invalid.title"),
                    JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        // Prompt user to accept license agreement
        if (!getEulaAcceptance(newLicense)) {
            return;
        }

        try {
            AsyncAdminMethods.JobId<Boolean> jobId = admin.installLicense( newLicense );
            Either<String, Boolean> result =
                    AdminGuiUtils.doAsyncAdmin( admin, this, "License Installation", "License Installation", jobId );

            if ( result.isLeft() ) {
                showInstallError( result.left() );
                return;
            }
        } catch ( InterruptedException e ) {
            // Canceled by user, do nothing
            return;
        } catch ( InvocationTargetException e ) {
            showInstallError( ExceptionUtils.getMessage( e ) );
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
        selectTableRowByLicense(newLicense);

        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private void showInstallError( String err ) {
        logger.log( Level.WARNING, "Unable to install license: " + err );

        DialogDisplayer.showMessageDialog( ManageLicensesDialog.this,
                err,
                RESOURCES.getString( "dialog.install.error.failure.title" ),
                JOptionPane.ERROR_MESSAGE, null );
    }

    private void doRemoveSelectedLicense() {
        LicenseTableRow selectedRow = getSelectedTableRow();

        if (selectedRow == null) return; // shouldn't happen, as the Remove button is only enabled when a row is selected

        final LicenseDocument selected = selectedRow.getLicenseDocument();

        DialogDisplayer.showSafeConfirmDialog(
                this,
                RESOURCES.getString("dialog.remove.confirm.message"),
                RESOURCES.getString("dialog.remove.confirm.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.CANCEL_OPTION) {
                            return;
                        }

                        try {
                            AsyncAdminMethods.JobId<Boolean> jobId = admin.uninstallLicense( selected );
                            Either<String, Boolean> result = AdminGuiUtils.doAsyncAdmin( admin, ManageLicensesDialog.this, "License Removal", "License Removal", jobId );

                            if ( result.isLeft() ) {
                                showRemoveError( result.left() );
                                return;
                            }

                            disconnectManagerOnClose = true;
                        } catch ( InterruptedException e ) {
                            // Canceled by user; do nothing
                            return;
                        } catch ( InvocationTargetException e ) {
                            showRemoveError( ExceptionUtils.getMessage( e ) );
                            return;
                        }

                        updateConsoleLicenseManager();
                        populateLicenseTable();
                    }
                }
        );
    }

    private void showRemoveError( String err ) {
        DialogDisplayer.showMessageDialog( this,
                err,
                RESOURCES.getString( "dialog.remove.error.title" ),
                JOptionPane.ERROR_MESSAGE, null );
    }

    private void viewDetails() {
        FeatureLicense featureLicense = getSelectedTableRow().getFeatureLicense();

        if (featureLicense == null) {
            DialogDisplayer.showMessageDialog(ManageLicensesDialog.this,
                    RESOURCES.getString("dialog.view.error.message"),
                    RESOURCES.getString("dialog.view.error.title"),
                    JOptionPane.OK_OPTION, null);
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
                    RESOURCES.getString("dialog.disconnect.message"),
                    RESOURCES.getString("dialog.disconnect.title"),
                    JOptionPane.OK_OPTION, null);

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
                                MessageFormat.format(RESOURCES.getString("dialog.install.error.read.io.message"),
                                        ExceptionUtils.getMessage(e)),
                                RESOURCES.getString("dialog.install.error.read.title"),
                                JOptionPane.ERROR_MESSAGE, null);
                    }

                    return null;
                }
            });
        } catch (AccessControlException e) {
            return getLicenseStreamFromTextBox();
        }
    }

    private InputStream getLicenseStreamFromTextBox() {
        final JDialog textEntryDialog =
                new JDialog(this, RESOURCES.getString("licenseTextInputDialog.title"), true);

        textEntryDialog.setLayout(new BorderLayout());
        textEntryDialog.add(new JLabel(RESOURCES.getString("licenseTextInputDialog.instructions")),
                BorderLayout.NORTH);

        final TextArea licenseTextArea = new TextArea(15, 80);
        textEntryDialog.add(licenseTextArea, BorderLayout.CENTER);

        JButton okButton = new JButton(RESOURCES.getString("button.ok"));
        JButton cancelButton = new JButton(RESOURCES.getString("button.cancel"));

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

        fc.setDialogTitle(RESOURCES.getString("licenseFileChooser.title"));

        fc.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return RESOURCES.getString("licenseFileChooser.filter.description");
            }

            @Override
            public boolean accept(File f) {
                final String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(RESOURCES.getString("licenseFileChooser.filter.extension"));
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

    private void selectTableRowByLicense(FeatureLicense license) {
        long id = license.getId();

        for (LicenseTableRow row : licenseTableModel.getRows()) {
            if (id == row.getFeatureLicense().getId()) {
                int modelRowIndex = licenseTableModel.getRowIndex(row);
                int viewRowIndex = licenseTable.convertRowIndexToView(modelRowIndex);

                licenseTable.setRowSelectionInterval(viewRowIndex, viewRowIndex);

                return;
            }
        }

        // shouldn't ever reach here, and it's not a disaster if the row can't be found
    }

    private LicenseTableRow getSelectedTableRow() {
        int viewRowIndex = licenseTable.getSelectedRow();

        return viewRowIndex < 0
                ? null
                : licenseTableModel.getRowObject(licenseTable.convertRowIndexToModel(viewRowIndex));
    }

    private void configureLicenseTable() {
        licenseTable.setAutoCreateRowSorter(true);

        Col<LicenseTableRow> descriptionCol =
                column(RESOURCES.getString("table.column.description.name"), 100, 200, 1000,
                        Functions.<String, LicenseTableRow>propertyTransform(LicenseTableRow.class, "description"), String.class);
        Col<LicenseTableRow> expiryCol =
                column(RESOURCES.getString("table.column.expiry.name"), 100, 300, 1000,
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

        @SuppressWarnings("UnusedDeclaration")
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
        private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

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

                    setPlainText(new SimpleDateFormat(DATE_FORMAT_PATTERN).format(expiry) +
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
