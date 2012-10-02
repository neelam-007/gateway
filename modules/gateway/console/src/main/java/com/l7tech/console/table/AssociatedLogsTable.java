package com.l7tech.console.table;

import com.l7tech.console.util.ArrowIcon;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.audit.AssociatedLog;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditViewerPolicyNotAvailableException;
import com.l7tech.gateway.common.security.rbac.AttemptedOther;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.gui.util.JTableColumnResizeMouseListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;
import java.util.EventObject;
import java.util.logging.Logger;

/**
 * @author fpang
 */
public class AssociatedLogsTable extends JTable {

    private static int[] DEFAULT_COLUMN_WIDTHS = {175, 80, 80, 40, 400};
    private static final Logger logger = Logger.getLogger(AssociatedLogsTable.class.getName());

    private AssociatedLogsTableSorter associatedLogsTableModel = null;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);

    private DefaultTableColumnModel columnModel;
    private boolean enableAuditViewerButton;

//    int width = DEFAULT_COLUMN_WIDTHS[4] - 45;//45 ~ approx size of button

    public AssociatedLogsTable() {
        setModel(getAssociatedLogsTableModel());
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setColumnModel(getLogColumnModel());
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        addMouseListenerToHeaderInTable();
        enableAuditViewerButton = enableInvokeButton();
    }

    /**
     * Get table sorter which is the table model being used in this table.
     *
     * @return AssociatedLogsTableSorter  The table model with column sorting.
     */
    public AssociatedLogsTableSorter getTableSorter() {
        return associatedLogsTableModel;
    }

    private JButton buildButton() {
        JButton button = new JButton("...");
        button.setFont(button.getFont().deriveFont(9));
        return button;
    }

    private JComponent buildButtonComponent(JButton button) {
        JPanel detailPanel = new JPanel() {
            public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
            }

            public void firePropertyChange(String propertyName, char oldValue, char newValue) {
            }

            public void firePropertyChange(String propertyName, int oldValue, int newValue) {
            }

            public boolean isOpaque() {
                return true;
            }
        };
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.X_AXIS));
        detailPanel.add(Box.createHorizontalGlue());
        detailPanel.add(button);
        return detailPanel;
    }

    /**
     * Return LogColumnModel property value
     *
     * @return DefaultTableColumnModel
     */
    private DefaultTableColumnModel getLogColumnModel() {
        if(columnModel != null){
            return columnModel;
        }
        columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_TIMESTAMP_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[0]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_SECURITY_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[1]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_DETAIL_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[2]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_CODE_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[3]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[4]));

        //detail column
        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_DETAIL_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            JButton detailRenderButton = buildButton();
            JComponent detailRenderComponent = buildButtonComponent(detailRenderButton);

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComponent comp = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (comp instanceof JLabel) {
                    ((JLabel) comp).setText("");
                }
                String detailText = (String) value;
                if (TextUtils.trim(detailText).length() > 0) {
                    detailRenderComponent.setBackground(comp.getBackground());
                    detailRenderComponent.setBorder(comp.getBorder());
                    comp = detailRenderComponent;
                }
                return comp;
            }
        });

        CellEditorWithButton detailCellEditorWithButton = new CellEditorWithButton("Detail");
        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_DETAIL_COLUMN_INDEX).setCellEditor(detailCellEditorWithButton);
        this.getSelectionModel().addListSelectionListener(detailCellEditorWithButton);

        //message column
        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            JButton messageRenderButton = buildButton();
            JComponent messageRenderComponent = buildButtonComponent(messageRenderButton);

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final String origMessageText = (String) value;
                final String escapedMessageText = wrapInHtml(origMessageText);

                JComponent comp = (JComponent) super.getTableCellRendererComponent(table, escapedMessageText, isSelected, hasFocus, row, column);

                if (isLarge(origMessageText, true)) {
                    JLabel textLabel = new JLabel(escapedMessageText, SwingConstants.LEFT);
                    textLabel.setPreferredSize(new Dimension(columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX).getWidth() - 45, 25));

                    messageRenderComponent.setBackground(comp.getBackground());
                    messageRenderComponent.setBorder(comp.getBorder());

                    JPanel messagePane = new JPanel();
                    messagePane.setBackground(comp.getBackground());
                    messagePane.setLayout(new BorderLayout());
                    messagePane.add(textLabel, BorderLayout.WEST);
                    messagePane.add(messageRenderComponent, BorderLayout.EAST);
                    comp = messagePane;
                }

                //set tooltip
                if (TextUtils.trim(origMessageText).length() == 0) {
                    comp.setToolTipText(null);
                } else {
                    if (origMessageText.length() < 4096) {
                        //sanitize in case messageText contains html
                        comp.setToolTipText(escapedMessageText);
                    }
                }

                return comp;
            }
        });

        CellEditorWithButton messageCellEditorWithButton = new CellEditorWithButton("Message");
        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX).setCellEditor(messageCellEditorWithButton);
        this.getSelectionModel().addListSelectionListener(messageCellEditorWithButton);


        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setHeaderRenderer(iconHeaderRenderer);
            columnModel.getColumn(i).setHeaderValue(getAssociatedLogsTableModel().getColumnName(i));
        }

        return columnModel;
    }

    /**
     * create the table model with log fields
     *
     * @return AssociatedLogsTableSorter model
     */
    private AssociatedLogsTableSorter getAssociatedLogsTableModel() {
        if (associatedLogsTableModel != null) {
            return associatedLogsTableModel;
        }

        String[] cols = {"Time", "Severity", "Detail", "Code", "Message"};
        String[][] rows = new String[][]{};

        associatedLogsTableModel = new AssociatedLogsTableSorter(new DefaultTableModel(rows, cols)) {
            public boolean isCellEditable(int row, int col) {
                return col == 2 || col == 4;
            }
        };

        return associatedLogsTableModel;
    }

    /**
     * Add a mouse listener to the Table to trigger a table sort
     * when a column heading is clicked in the JTable.
     */
    private void addMouseListenerToHeaderInTable() {

        final JTable tableView = this;
        tableView.setColumnSelectionAllowed(false);
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int viewColumn = tableView.columnAtPoint(e.getPoint());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {

                    ((AssociatedLogsTableSorter) tableView.getModel()).sortData(column, true);
                    ((AssociatedLogsTableSorter) tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
        th.addMouseListener(new JTableColumnResizeMouseListener(tableView, DEFAULT_COLUMN_WIDTHS));
    }

    // This customized renderer can render objects of the type TextandIcon
    TableCellRenderer iconHeaderRenderer = new DefaultTableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            // Inherit the colors and font from the header buttonComponent
            if (table != null) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    setForeground(header.getForeground());
                    setBackground(header.getBackground());
                    setFont(header.getFont());
                    setHorizontalTextPosition(SwingConstants.LEFT);
                }
            }

            setText((String) value);

            if (getTableSorter().getSortedColumn() == column) {

                if (getTableSorter().isAscending()) {
                    setIcon(upArrowIcon);
                } else {
                    setIcon(downArrowIcon);
                }
            } else {
                setIcon(null);
            }

            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    };

    private JDialog getViewDialog(final String detail, final String title){
        final JDialog dialog = new JDialog((Frame)null, true);
        dialog.setTitle("Associated Log - " + title);
        JPanel panel = new JPanel(new BorderLayout());
        dialog.getContentPane().add(panel);

        // configure text display buttonComponent
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setText(detail);
        textArea.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(textArea);
        panel.add(sp, BorderLayout.CENTER);

        JButton aVPolicyButton = getInvokeAVPolicyButton(textArea);
        if(aVPolicyButton != null){
            final JPanel holderSouthPanel = new JPanel();
            holderSouthPanel.setLayout(new BoxLayout(holderSouthPanel, BoxLayout.X_AXIS));
            holderSouthPanel.add(aVPolicyButton);
            panel.add(holderSouthPanel, BorderLayout.SOUTH);
        }

        Utilities.setEscKeyStrokeDisposes(dialog);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
            }
        });
        return dialog;
    }

    private JButton getInvokeAVPolicyButton(final JTextArea detailTextArea){
        if(Registry.getDefault().isAdminContextPresent()){
            final JButton invokeAVButton = new JButton("Invoke Audit Viewer Policy");

            boolean canEnable = false;
            //only create an enabled button for supported audit detail message ids.
            final int row = getSelectedRow();
            if(row != -1){
                final AssociatedLogsTableSorter model = (AssociatedLogsTableSorter) getModel();
                final Object value = model.getData(row);
                if(value instanceof AssociatedLog){
                    AssociatedLog associatedLog = (AssociatedLog) value;
                    final int auditDetailId = associatedLog.getMessageId();
                    if(AuditAdmin.USER_DETAIL_MESSAGES.contains(auditDetailId)){
                        canEnable = enableAuditViewerButton;
                    }
                }
            }
            
            invokeAVButton.setEnabled(canEnable);
            invokeAVButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final int row = getSelectedRow();
                    if(row == -1) return;

                    final AssociatedLogsTableSorter model = (AssociatedLogsTableSorter) getModel();
                    final Object value = model.getData(row);
                    if(value instanceof AssociatedLog){
                        AssociatedLog associatedLog = (AssociatedLog) value;
                        String output = getAVPolicyOutput(associatedLog.getAuditRecordId(), associatedLog.getOrdinal());
                        if (output != null) {
                            detailTextArea.setText(output);
                        } else {
                            detailTextArea.setText("Error processing " + PolicyType.TAG_AUDIT_VIEWER+" policy.");
                        }
                        detailTextArea.setCaretPosition(0);
                    }
                }
            });
            return invokeAVButton;
        }

        return null;
    }

    private String getAVPolicyOutput(final long auditRecordId, long ordinal){
        final AuditAdmin auditAdmin = Registry.getDefault().getAuditAdmin();
        try {
            return auditAdmin.invokeAuditViewerPolicyForDetail(auditRecordId, ordinal);
        } catch (FindException e) {
            return ExceptionUtils.getMessage(e);
        } catch (AuditViewerPolicyNotAvailableException e) {
            enableAuditViewerButton = false;
            return ExceptionUtils.getMessage(e) + ". Reopen the audit viewer if the policy is recreated or enabled.";
        }
    }
    
    private boolean enableInvokeButton(){
        if(Registry.getDefault().isAdminContextPresent()){
            final boolean avPermGranted = Registry.getDefault().getSecurityProvider().hasPermission(
                    new AttemptedOther(EntityType.AUDIT_RECORD, OtherOperationName.AUDIT_VIEWER_POLICY.getOperationName()));

            final boolean avPolicyIsActive;
            if(avPermGranted){
                avPolicyIsActive = Registry.getDefault().getAuditAdmin().isAuditViewerPolicyAvailable();
            } else {
                avPolicyIsActive = false;
            }

            return avPermGranted && avPolicyIsActive;
        }

        return false;
    }

    private boolean isLarge(final String value, final boolean trimFirst) {
        if (trimFirst) {
            return TextUtils.trim(value).length() > 0 && (value.length() > 200 || value.contains("\n"));
        } else {
            return value != null && value.length() > 0 && (value.length() > 200 || value.contains("\n"));
        }
    }

    private String wrapInHtml(final String text) {
        return "<html>" + TextUtils.escapeHtmlSpecialCharacters(text) + "</html>";
    }

    private class CellEditorWithButton extends AbstractCellEditor implements TableCellEditor, ListSelectionListener {
        private final JButton button;
        private final JComponent buttonComponent;
        private int row;
        private String value;

        private CellEditorWithButton(final String columnTitle) {
            button = buildButton();
            buttonComponent = buildButtonComponent(button);
            buttonComponent.setBackground(getColour());

            this.button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getCellEditorValue() != null) {
                        final JDialog dialog = getViewDialog(getCellEditorValue(), columnTitle);
                        dialog.setSize(450, 175);
                        Utilities.centerOnScreen(dialog);
                        DialogDisplayer.display(dialog, new Runnable() {
                            public void run() {
                                // Make the renderer reappear.
                                fireEditingStopped();
                            }
                        });
                    }
                }
            });

            this.button.setFocusable(false);
            this.buttonComponent.setFocusable(false);
        }

        public void valueChanged(ListSelectionEvent e) {
            if (AssociatedLogsTable.this.isEditing() && row != AssociatedLogsTable.this.getSelectedRow()) {
                this.fireEditingCanceled(); // stop edit when another row is selected
            }
        }

        // When the color is exactly the same as the row bg the panel
        // does not get painted correctly, not sure why.
        private Color getColour() {
            Color selColor = AssociatedLogsTable.this.getSelectionBackground();
            return selColor.getRed() < 255 && selColor.getGreen() < 255 && selColor.getBlue() < 255 ?
                    new Color(selColor.getRed() + 1, selColor.getGreen() + 1, selColor.getBlue() + 1) :
                    new Color(selColor.getRed(), selColor.getGreen(), selColor.getBlue());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.value = (String) value;
            this.row = row;
            // Case 1: for "Message" tab in the 5th Column (index = 4)
            if (column == 4) {
                JComponent tempButtonComponent = buildButtonComponent(button);
                final String escapedValue = wrapInHtml(getCellEditorValue());
                JLabel textLabel = new JLabel(escapedValue, SwingConstants.LEFT);
                textLabel.setVerticalAlignment(SwingConstants.TOP);
                textLabel.setPreferredSize(new Dimension(getLogColumnModel().getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX).getWidth() - 45, 25));
                JPanel messagePane = new JPanel();
                messagePane.setBackground(getColour());
                messagePane.setLayout(new BorderLayout());
                messagePane.add(textLabel, BorderLayout.WEST);

                if (isLarge(getCellEditorValue(), true)) {
                    messagePane.add(tempButtonComponent, BorderLayout.EAST);
                }

                return messagePane;
            }

            // Case 2: for other tabs, for example, "Detail" tab in the 3rd Column (index = 2)
            return buttonComponent;
        }

        public String getCellEditorValue() {
            return value;
        }

        public boolean isCellEditable(EventObject anEvent) {
            int column;
            if (anEvent instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) anEvent;
                column = AssociatedLogsTable.this.columnAtPoint(me.getPoint());
                String value = getValue(anEvent);
                if (column == 4 && isLarge(value, false)) {
                    return true;
                } else if (column == 2 && TextUtils.trim(value).length() > 0) {
                    return true;
                }
            }
            return false;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                buttonComponent.dispatchEvent((MouseEvent)anEvent);
            }
            return true;
        }

        private String getValue(EventObject anEvent) {
            String value = null;

            if (anEvent instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) anEvent;
                int row = AssociatedLogsTable.this.rowAtPoint(me.getPoint());
                int column = AssociatedLogsTable.this.columnAtPoint(me.getPoint());

                value = (String) AssociatedLogsTable.this.getValueAt(row, column);
            }

            return value;
        }
    }
}
