package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.util.SortedSingleColumnTableModel;
import com.l7tech.console.table.MimePartsTable;
import com.l7tech.console.table.ButtonCellEditor;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;


/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionDialog extends JDialog {
    static final Logger log = Logger.getLogger(RequestSwAAssertionDialog.class.getName());
    private RequestSwAAssertion assertion;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JComboBox bindingsListComboxBox;
    private JScrollPane operationsScrollPane;
    private JScrollPane multipartScrollPane;
    private EventListenerList listenerList = new EventListenerList();
    private SortedSingleColumnTableModel bindingOperationsTableModel = null;
    private JTable bindingOperationsTable = null;
    private MimePartsTable mimePartsTable = null;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RequestSwAPropertiesDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(RequestSwAAssertionDialog.class.getName());

    public RequestSwAAssertionDialog(JFrame parent, RequestSwAAssertion assertion) {
        super(parent, resources.getString("window.title"), true);
        this.assertion = assertion;

        initialize();
        populateData();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        bindingOperationsTable = getBindingOperationsTable();
        bindingOperationsTable.setModel(getBindingOperationsTableModel());
        bindingOperationsTable.setShowHorizontalLines(false);
        bindingOperationsTable.setShowVerticalLines(false);
        bindingOperationsTable.setDefaultRenderer(Object.class, bindingOperationsTableRenderer);
        operationsScrollPane.getViewport().setBackground(bindingOperationsTable.getBackground());
        operationsScrollPane.setViewportView(bindingOperationsTable);

        bindingsListComboxBox.setRenderer(bindingListRender);
        bindingsListComboxBox.addItemListener(new ItemListener() {
            /**
             * Invoked when an item has been selected or deselected.
             * The code written for this method performs the operations
             * that need to occur when an item is selected (or deselected).
             */
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    populateBindingOperationsData((BindingInfo) e.getItem());
                }
            }
        });

        multipartScrollPane.setViewportView(getMimePartsTable());
        multipartScrollPane.getViewport().setBackground(Color.white);

        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireEventAssertionChanged(assertion);
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton});

    }

    private void populateData() {

        Map bindings = assertion.getBindings();
        // populate the binding operation table
        if(bindings == null) throw new RuntimeException("bindings map is NULL");

        boolean firstEntry = true;
        Iterator bindingsItr = bindings.keySet().iterator();
        while (bindingsItr.hasNext()) {
            String bindingName = (String) bindingsItr.next();
            BindingInfo binding = (BindingInfo) bindings.get(bindingName);

            // add the entry the the binding list
            bindingsList.add(binding);
            bindingsListComboxBox.addItem(binding);

            if(firstEntry) {
                populateBindingOperationsData(binding);
                firstEntry = false;
            }
        }

    }

    private void saveData(BindingOperationInfo bo) {
        if(bo == null) throw new RuntimeException("bindingOperation is NULL");

        Vector dataSet = getMimePartsTable().getTableSorter().getAllData();
        for (int i = 0; i < dataSet.size(); i++) {
            MimePartInfo mimePart = (MimePartInfo) dataSet.elementAt(i);
            MimePartInfo mimePartFound = (MimePartInfo) bo.getMultipart().get(mimePart.getName());
            if(mimePartFound != null) {
                mimePartFound.setMaxLength(mimePart.getMaxLength());
            }            
        }
    }

    private void populateBindingOperationsData(BindingInfo binding) {
        if(binding == null) throw new RuntimeException("binding info is NULL");

        // save the mime part data to the assertion before populating the new data
        int selectedOperation = getBindingOperationsTable().getSelectedRow();
        if(selectedOperation >= 0) {
            BindingOperationInfo bo = (BindingOperationInfo) getBindingOperationsTableModel().getValueAt(selectedOperation, 0);
            saveData(bo);
        }

        // clear the operation table
        getBindingOperationsTableModel().removeRows(getBindingOperationsTableModel().getDataSet());
        getBindingOperationsTableModel().clearDataSet();

        Iterator bindingOperationsItr = binding.getBindingOperations().keySet().iterator();
        while(bindingOperationsItr.hasNext()) {
            String bindingOperationName = (String) bindingOperationsItr.next();
            // add the entry to the binding operation table
            BindingOperationInfo bo = (BindingOperationInfo) binding.getBindingOperations().get(bindingOperationName);
            getBindingOperationsTableModel().addRow(bo);
        }
        getBindingOperationsTableModel().fireTableDataChanged();

        // show the mime parts of the first operation
        if(getBindingOperationsTableModel().getRowCount() > 0) {
            getBindingOperationsTable().setRowSelectionInterval(0,0);
            getBindingOperationsTableModel().fireTableCellUpdated(0,0);
            populateMimePartsData(((BindingOperationInfo) getBindingOperationsTableModel().getDataSet()[0]).getMultipart());
        }
    }

    private void populateMimePartsData(Map mimeParts) {
        if(mimeParts == null) throw new RuntimeException("mimeParts is NULL");

        // clear the MIME part table
        getMimePartsTable().clear();

        Iterator parts = mimeParts.keySet().iterator();
        Vector pv = new Vector();
        while (parts.hasNext()) {
            String partName = (String) parts.next();
            pv.add(mimeParts.get(partName));

        }
        getMimePartsTable().getTableSorter().setData(pv);
    }

    /**
     * add the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    /**
     * remove the the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        listenerList.remove(PolicyListener.class, listener);
    }

    /**
     * notfy the listeners
     *
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                PolicyEvent event = new
                        PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                for (int i = 0; i < listeners.length; i++) {
                    ((PolicyListener) listeners[i]).assertionsChanged(event);
                }
            }
        });
    }

    private MimePartsTable getMimePartsTable() {

        if(mimePartsTable != null) return mimePartsTable;

        mimePartsTable = new MimePartsTable();
        final ButtonCellEditor editor = ButtonCellEditor.attach(mimePartsTable, 1);
        editor.getButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // put some nicer dialog
                JOptionPane.showMessageDialog(RequestSwAAssertionDialog.this,
                                              editor.getCellEditorValue(), "Content Types",
                                              JOptionPane.PLAIN_MESSAGE);
                editor.stopCellEditing();
            }
        });
        return mimePartsTable;
    }

    private JTable getBindingOperationsTable() {
        if(bindingOperationsTable != null) {
            return bindingOperationsTable;
        }

        bindingOperationsTable = new JTable();
        bindingOperationsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bindingOperationsTable.getSelectionModel().
                addListSelectionListener(new ListSelectionListener() {
                    /**
                     * Called whenever the value of the selection changes.
                     * @param e the event that characterizes the change.
                     */
                    public void valueChanged(ListSelectionEvent e) {
                        int row = bindingOperationsTable.getSelectedRow();
                        if(row >= 0) {
                            BindingOperationInfo boInfo = (BindingOperationInfo) bindingOperationsTable.getModel().getValueAt(row, 0);
                            populateMimePartsData(boInfo.getMultipart());
                        }
                    }
                });

        return bindingOperationsTable;
    }

    /**
     * @return the table model representing the binding operations specified in WSDL
     */
    private SortedSingleColumnTableModel getBindingOperationsTableModel() {
        if (bindingOperationsTableModel != null)
            return bindingOperationsTableModel;

        bindingOperationsTableModel = new SortedSingleColumnTableModel(new Comparator() {
            public int compare(Object o1, Object o2) {
                BindingOperationInfo e1 = (BindingOperationInfo)o1;
                BindingOperationInfo e2 = (BindingOperationInfo)o2;

                return e1.getName().compareToIgnoreCase(e2.getName());
            }

            public boolean isCellEditable(int row, int col) {
                return false;
            }
        });

        // add a new column without a column title
        bindingOperationsTableModel.addColumn("");
        return bindingOperationsTableModel;
    }

    private final ListCellRenderer bindingListRender = new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            BindingInfo p = (BindingInfo)value;
            setText(p.getBindingName());
            setToolTipText(null);

            return this;
        }
    };

    private final TableCellRenderer bindingOperationsTableRenderer = new DefaultTableCellRenderer() {
        /* This is the only method defined by ListCellRenderer.  We just
        * reconfigure the Jlabel each time we're called.
        */
        public Component
                getTableCellRendererComponent(JTable table,
                                              Object value,
                                              boolean isSelected,
                                              boolean hasFocus,
                                              int row, int column) {
            if (isSelected) {
                this.setBackground(table.getSelectionBackground());
                this.setForeground(table.getSelectionForeground());
            } else {
                this.setBackground(table.getBackground());
                this.setForeground(table.getForeground());
            }

            this.setFont(new Font("Dialog", Font.PLAIN, 12));
            BindingOperationInfo p = (BindingOperationInfo)value;
            setText(p.getName());
            setToolTipText(null);
            return this;
        }
    };

    private final TableCellRenderer inputParametersTableRenderer = new DefaultTableCellRenderer() {
        /* This is the only method defined by ListCellRenderer.  We just
        * reconfigure the Jlabel each time we're called.
        */
        public Component
                getTableCellRendererComponent(JTable table,
                                              Object value,
                                              boolean iss,
                                              boolean hasFocus,
                                              int row, int column) {
            if (!table.isEnabled()) {
                this.setEnabled(false);
            } else {
                this.setEnabled(true);
                if (iss) {
                    this.setBackground(table.getSelectionBackground());
                    this.setForeground(table.getSelectionForeground());
                } else {
                    this.setBackground(table.getBackground());
                    this.setForeground(table.getForeground());
                }
            }

            this.setFont(new Font("Dialog", Font.PLAIN, 12));
            MimePartInfo p = (MimePartInfo)value;
            setText(p.getName());
            setToolTipText(null);
            return this;
        }
    };

    private Set bindingsList = new TreeSet(new Comparator() {
        public int compare(Object o1, Object o2) {
            BindingInfo p1 = (BindingInfo)o1;
            BindingInfo p2 = (BindingInfo)o2;
            return p1.getBindingName().compareToIgnoreCase(p2.getBindingName());
        }
    });

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("Operations:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        operationsScrollPane = new JScrollPane();
        panel3.add(operationsScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(150, -1), new Dimension(150, 200), new Dimension(200, -1)));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Input Parameters (only those bound to an attachment are shown):");
        panel4.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        multipartScrollPane = new JScrollPane();
        panel4.add(multipartScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(500, -1), new Dimension(500, 200), null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(10, 0, 10, 0), -1, -1));
        panel1.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("Bindings:");
        panel5.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        bindingsListComboxBox = new JComboBox();
        panel5.add(bindingsListComboxBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), new Dimension(300, -1)));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel6.add(cancelButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel6.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        okButton = new JButton();
        okButton.setText("OK");
        panel6.add(okButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
}
