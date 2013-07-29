package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.objectmodel.*;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog that is presented to the user so they can choose an email listener to edit or delete.
 */
public class EmailListenerManagerWindow extends JDialog {
    protected static final Logger logger = Logger.getLogger(EmailListenerManagerWindow.class.getName());

    private static final String WINDOW_TITLE = "Manage Email Listeners";

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private JButton createButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton closeButton;
    private JScrollPane mainScrollPane;
    private JPanel contentPane;
    private JButton cloneButton;
    private EmailListenerTable emailListenerTable;

    private PermissionFlags flags;

    /**
     * Creates a new instance of EmailListenerManagerWindow.
     *
     * @param owner The owner of this dialog
     */
    public EmailListenerManagerWindow(Frame owner) {
        super(owner, WINDOW_TITLE);
        initialize();
    }

    /**
     * Creates a new instance of EmailListenerManagerWindow.
     *
     * @param owner The owner of this dialog
     */
    public EmailListenerManagerWindow(Dialog owner) {
        super(owner, WINDOW_TITLE);
        initialize();
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.EmailListenerManagerWindow", locale);
    }

    private void initialize() {
        initResources();

        setTitle(resources.getString("window.title"));

        flags = PermissionFlags.get(EntityType.EMAIL_LISTENER);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        emailListenerTable = new EmailListenerTable(resources);
        mainScrollPane.setViewport(null);
        mainScrollPane.setViewportView(emailListenerTable);
        mainScrollPane.getViewport().setBackground(Color.white);

        emailListenerTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                doProperties();
            }
        });

        createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCreate();
            }
        });

        cloneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doClone();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doRemove();
            }
        });

        Utilities.setDoubleClickAction(emailListenerTable, propertiesButton);

        loadEmailListeners();
        pack();
        enableOrDisableButtons();
    }

    private void doRemove() {
        EmailListener emailListener = emailListenerTable.getSelectedEmailListener();
        if (emailListener == null)
            return;

        String message = resources.getString("confirmDelete.message.long");
        message = message.replace("$1", emailListener.getName());
        int result = JOptionPane.showConfirmDialog(this,
                                                   message,
                                                   resources.getString("confirmDelete.message.short"),
                                                   JOptionPane.YES_NO_CANCEL_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.YES_OPTION)
            return;

        EmailListenerAdmin emailListenerAdmin = getEmailListenerAdmin();
        if (emailListenerAdmin == null)
            return;
        try {
            emailListenerAdmin.deleteEmailListener(emailListener.getGoid());
            loadEmailListeners();
        } catch (DeleteException e) {
            showErrorMessage(resources.getString("errors.removalFailed.title"),
                    resources.getString("errors.removalFailed.message") + " " + ExceptionUtils.getMessage(e),
                    e);
        } catch (FindException e) {
            showErrorMessage(resources.getString("errors.removalFailed.title"),
                    resources.getString("errors.removalFailed.message") + " " + ExceptionUtils.getMessage(e),
                    e);
        }
    }

    private void doCreate() {
        final EmailListener emailListener = new EmailListener(EmailServerType.POP3);
        emailListener.setActive( true );
        editAndSave(emailListener, true);
    }

    private void doClone() {
        EmailListener emailListener = emailListenerTable.getSelectedEmailListener();
        EmailListener newEmailListener = new EmailListener(emailListener);
        EntityUtils.updateCopy( newEmailListener );
        editAndSave(newEmailListener, true);
    }

    private void doProperties() {
        EmailListener sinkConfiguration = emailListenerTable.getSelectedEmailListener();
        if (sinkConfiguration != null) {
            editAndSave(sinkConfiguration, false);
        }
    }

    private void editAndSave(final EmailListener emailListener, final boolean selectNameField) {
        boolean create = GoidEntity.DEFAULT_GOID.equals(emailListener.getGoid());
        final AttemptedOperation operation = create
                ? new AttemptedCreateSpecific(EntityType.EMAIL_LISTENER, emailListener)
                : new AttemptedUpdate(EntityType.EMAIL_LISTENER, emailListener);
        boolean readOnly = !Registry.getDefault().getSecurityProvider().hasPermission(operation);
        final EmailListenerPropertiesDialog dlg = new EmailListenerPropertiesDialog(this, emailListener, readOnly);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        if(selectNameField)
            dlg.selectNameField();
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        public void run() {
                            loadEmailListeners();
                            editAndSave(emailListener, selectNameField);
                        }
                    };

                    try {
                        Goid goid = getEmailListenerAdmin().saveEmailListener(emailListener);
                        if (goid.equals(emailListener.getGoid())) emailListener.setGoid(goid);
                        reedit = null;
                        loadEmailListeners();
                        emailListenerTable.setSelectedConnector(emailListener);
                    } catch (SaveException e) {
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(e),
                                ExceptionUtils.getDebugException(e),
                                reedit);
                    } catch (UpdateException e) {
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(e),
                                ExceptionUtils.getDebugException(e),
                                reedit);
                    }
                }
            }
        });
    }

    private void enableOrDisableButtons() {
        EmailListener emailListener = emailListenerTable.getSelectedEmailListener();
        boolean haveSel = emailListener != null;

        createButton.setEnabled(flags.canCreateSome());
        propertiesButton.setEnabled(haveSel);
        removeButton.setEnabled(haveSel && flags.canDeleteSome());
        cloneButton.setEnabled(haveSel && flags.canCreateSome());
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        showErrorMessage(title, msg, e, null);
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    /** @return the LogSinkAdmin interface, or null if not connected or it's unavailable for some other reason */
    private EmailListenerAdmin getEmailListenerAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getEmailListenerAdmin();
    }

    private void loadEmailListeners() {
        try {
            EmailListenerAdmin emailListenerAdmin = getEmailListenerAdmin();
            if (!flags.canReadSome() || emailListenerAdmin == null) {
                // Not connected to Gateway, or no permission to read connector list
                emailListenerTable.setData(Collections.<EmailListenerTableRow>emptyList());
                return;
            }
            Collection<EmailListener> sinkConfigurations = emailListenerAdmin.findAllEmailListeners();
            java.util.List<EmailListenerTableRow> rows = new ArrayList<EmailListenerTableRow>();
            for (EmailListener emailListener : sinkConfigurations)
                rows.add(new EmailListenerTableRow(emailListener, resources));
            emailListenerTable.setData(rows);

        } catch (FindException e) {
            showErrorMessage(resources.getString("errors.loadFailed.title"),
                    resources.getString("errors.loadFailed.message") + " " + ExceptionUtils.getMessage(e),
                    e);
        }
    }

    private static class EmailListenerTableRow {
        private final EmailListener emailListener;
        private ResourceBundle resources = null;

        public EmailListenerTableRow(EmailListener emailListener, ResourceBundle resources) {
            this.emailListener = emailListener;
            this.resources = resources;
        }

        public EmailListener getEmailListener() {
            return emailListener;
        }

        public Object isActive() {
            return emailListener.isActive() ? resources.getString("enabledColumn.values.yes.text") : resources.getString("enabledColumn.values.no.text");
        }

        public Object getName() {
            return emailListener.getName();
        }

        public Object getServerType() {
            return resources.getString("typeColumn.values." + emailListener.getServerType().name() + ".text");
        }
    }

    private static class EmailListenerTable extends JTable {
        private final EmailListenerTableModel model;

        EmailListenerTable(ResourceBundle resources) {
            model = new EmailListenerTableModel(resources);

            setModel(model);

            // sort by name on default
            Utilities.setRowSorter(this, model, new int[]{0}, new boolean[]{true}, null);
            getTableHeader().setReorderingAllowed(false);
            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            TableColumnModel cols = getColumnModel();
            int numCols = model.getColumnCount();
            for (int i = 0; i < numCols; ++i) {
                final TableColumn col = cols.getColumn(i);
                col.setMinWidth(model.getColumnMinWidth(i));
                col.setPreferredWidth(model.getColumnPrefWidth(i));
                col.setMaxWidth(model.getColumnMaxWidth(i));
                TableCellRenderer cr = model.getCellRenderer(i, getDefaultRenderer(String.class));
                if (cr != null) col.setCellRenderer(cr);
            }
        }

        public EmailListenerTableRow getRowAt(int row) {
            return model.getRowAt(row);
        }

        public void setData(java.util.List<EmailListenerTableRow> rows) {
            model.setData(rows);
        }

        /** @return the current selected EmailListener, or null */
        public EmailListener getSelectedEmailListener() {
            int rowNum = getSelectedRow();
            if (rowNum < 0)
                return null;

            EmailListenerTableRow row = getRowAt(Utilities.convertRowIndexToModel(this, rowNum));
            if (row == null)
                return null;
            return row.getEmailListener();
        }

        public void setSelectedConnector(EmailListener emailListener) {
            int rowNum = model.findRowBySinkConfigurationGoid(emailListener.getGoid());
            if (rowNum >= 0)
                getSelectionModel().setSelectionInterval(rowNum, rowNum);
            else
                getSelectionModel().clearSelection();
        }
    }

    private static class EmailListenerTableModel extends AbstractTableModel {
        private Map<Goid, Integer> rowMap;

        private abstract class Col {
            final String name;
            final int minWidth;
            final int prefWidth;
            final int maxWidth;

            protected Col(String name, int minWidth, int prefWidth, int maxWidth) {
                this.name = name;
                this.minWidth = minWidth;
                this.prefWidth = prefWidth;
                this.maxWidth = maxWidth;
            }

            abstract Object getValueForRow(EmailListenerTableRow row);

            public TableCellRenderer getCellRenderer(final TableCellRenderer current) {
                return new TableCellRenderer() {
                    private Color defFg;
                    private Color defSelFg;

                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        if (defFg == null) {
                            TableCellRenderer def1 = new DefaultTableCellRenderer();
                            Component c = def1.getTableCellRendererComponent(table, value, false, false, row, column);
                            defFg = c.getForeground();
                            TableCellRenderer def2 = new DefaultTableCellRenderer();
                            Component csel = def2.getTableCellRendererComponent(table, value, true, false, row, column);
                            defSelFg = csel.getForeground();
                        }

                        Component ret = current.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                        if (isSelected) {
                            ret.setForeground(defSelFg);
                        } else {
                            ret.setForeground(defFg);
                        }

                        return ret;
                    }
                };
            }
        }

        public final Col[] columns;

        private final ArrayList<EmailListenerTableRow> rows = new ArrayList<EmailListenerTableRow>();

        public EmailListenerTableModel(final ResourceBundle resources) {
            columns = new Col[] {
                    new Col(resources.getString("columns.name.title"), 60, 90, 999999) {
                        Object getValueForRow(EmailListenerTableRow row) {
                            return row.getName();
                        }
                    },

                    new Col(resources.getString("columns.type.title"), 3, 100, 150) {
                        Object getValueForRow(EmailListenerTableRow row) {
                            return row.getServerType();
                        }
                    },

                    new Col(resources.getString("columns.enabled.title"), 60, 90, 90) {
                        Object getValueForRow(EmailListenerTableRow row) {
                            return row.isActive();
                        }
                    },
            };
        }

        public int getColumnMinWidth(int column) {
            return columns[column].minWidth;
        }

        public int getColumnPrefWidth(int column) {
            return columns[column].prefWidth;
        }

        public int getColumnMaxWidth(int column) {
            return columns[column].maxWidth;
        }

        public String getColumnName(int column) {
            return columns[column].name;
        }

        public TableCellRenderer getCellRenderer(int column, final TableCellRenderer current) {
            return columns[column].getCellRenderer(current);
        }

        public void setData(java.util.List<EmailListenerTableRow> rows) {
            rowMap = null;
            this.rows.clear();
            this.rows.addAll(rows);
            fireTableDataChanged();
        }

        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return columns.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns[columnIndex].getValueForRow(rows.get(rowIndex));
        }

        public EmailListenerTableRow getRowAt(int rowIndex) {
            return rows.get(rowIndex);
        }

        /** @return a Map of Connector OID -> row number */
        private Map<Goid, Integer> getRowMap() {
            if (rowMap != null)
                return rowMap;
            Map<Goid, Integer> ret = new LinkedHashMap<Goid, Integer>();
            for (int i = 0; i < rows.size(); i++) {
                EmailListenerTableRow row = rows.get(i);
                final Goid goid = row.getEmailListener().getGoid();
                ret.put(goid, i);
            }
            return rowMap = ret;
        }

        /**
         * @param goid GOID of connector whose row to find
         * @return the row number of the connector with a matching oid, or -1 if no match found
         */
        public int findRowBySinkConfigurationGoid(Goid goid) {
            return getRowMap().containsKey(goid) ? getRowMap().get(goid) : -1;
        }
    }
}
