package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.table.ButtonCellEditor;
import com.l7tech.console.table.MimePartsTable;
import com.l7tech.console.table.ExtraMimePartsTable;
import com.l7tech.console.table.ExtraMimePartsTableModel;
import com.l7tech.console.util.SortedSingleColumnTableModel;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;


/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RequestSwAPropertiesDialog", Locale.getDefault());

    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JComboBox bindingsListComboxBox;
    private JScrollPane operationsScrollPane;
    private JScrollPane multipartScrollPane;
    private JScrollPane extraAttachmentsScrollPane;
    private JComboBox unmatchedAttachmentComboBox;

    private RequestSwAAssertion assertion;
    private RequestSwAAssertion originalAssertion;
    private EventListenerList listenerList = new EventListenerList();
    private SortedSingleColumnTableModel bindingOperationsTableModel = null;
    private JTable bindingOperationsTable = null;
    private MimePartsTable mimePartsTable = null;
    private ExtraMimePartsTable extraMimePartsTable = null;
    private int selectedOperationForBinding = -1;

    /**
     *
     */
    public RequestSwAAssertionDialog(Frame parent, RequestSwAAssertion assertion) {
        super(parent, resources.getString("window.title"), true);
        this.originalAssertion = assertion;
        this.assertion = (RequestSwAAssertion) originalAssertion.clone();

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
                    selectedOperationForBinding = -1;
                    populateBindingOperationsData((BindingInfo) e.getItem());
                }
            }
        });

        unmatchedAttachmentComboBox.setModel(new DefaultComboBoxModel(new String[]{"Invalid request", "Drop unmatched attachments", "Pass unmatched attachments"}));
        unmatchedAttachmentComboBox.setSelectedIndex(assertion.getUnboundAttachmentPolicy());

        multipartScrollPane.setViewportView(getMimePartsTable());
        multipartScrollPane.getViewport().setBackground(Color.white);

        extraAttachmentsScrollPane.setViewportView(getExtraMimePartsTable());
        extraAttachmentsScrollPane.getViewport().setBackground(Color.white);

        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // update from controls
                if (selectedOperationForBinding >= 0) {
                    saveData((BindingOperationInfo) bindingOperationsTable.getModel().getValueAt(selectedOperationForBinding, 0));
                    saveExtraData((BindingOperationInfo) bindingOperationsTable.getModel().getValueAt(selectedOperationForBinding, 0));
                }
                assertion.setUnboundAttachmentPolicy(unmatchedAttachmentComboBox.getSelectedIndex());

                // update original assertion data
                originalAssertion.setUnboundAttachmentPolicy(assertion.getUnboundAttachmentPolicy());
                originalAssertion.setNamespaceMap(assertion.getNamespaceMap());
                originalAssertion.setBindings(assertion.getBindings());

                fireEventAssertionChanged(originalAssertion);
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

        Map<String,BindingInfo> bindings = assertion.getBindings();
        // populate the binding operation table
        if(bindings == null) throw new RuntimeException("bindings map is NULL");

        boolean firstEntry = true;
        Iterator<String> bindingsItr = bindings.keySet().iterator();
        while (bindingsItr.hasNext()) {
            String bindingName = bindingsItr.next();
            BindingInfo binding = bindings.get(bindingName);

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

        List dataSet = getMimePartsTable().getTableSorter().getData();
        for (int i = 0; i < dataSet.size(); i++) {
            MimePartInfo mimePart = (MimePartInfo) dataSet.get(i);
            MimePartInfo mimePartFound = (MimePartInfo) bo.getMultipart().get(mimePart.getName());
            if(mimePartFound != null) {
                mimePartFound.setMaxLength(mimePart.getMaxLength());
            }            
        }
    }

    private void saveExtraData(BindingOperationInfo bo) {
        if(bo == null) throw new RuntimeException("bindingOperation is NULL");

        Map extras = bo.getExtraMultipart();
        extras.clear();
        List extraDataSet = ((ExtraMimePartsTableModel)getExtraMimePartsTable().getModel()).getData();
        for (int i = 0; i < extraDataSet.size(); i++) {
            MimePartInfo mimePart = (MimePartInfo) extraDataSet.get(i);
            if (mimePart.retrieveAllContentTypes().length() > 0) {
                extras.put(mimePart.retrieveAllContentTypes(), mimePart);
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
            saveExtraData(bo);
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
            populateMimePartsData(((BindingOperationInfo) getBindingOperationsTableModel().getDataSet()[0]).getMultipart().values());
            populateExtraMimePartsData(((BindingOperationInfo) getBindingOperationsTableModel().getDataSet()[0]).getExtraMultipart().values());
        }

        if (binding.getBindingOperations().isEmpty())
            selectedOperationForBinding = -1;
        else
            selectedOperationForBinding = 0;
    }

    private void populateMimePartsData(Collection mimeParts) {
        if(mimeParts == null) throw new IllegalArgumentException("mimeParts is NULL");

        // clear the MIME part table
        getMimePartsTable().clear();
        getMimePartsTable().getTableSorter().setData(mimeParts);
    }

    private void populateExtraMimePartsData(Collection mimeParts) {
        if(mimeParts == null) throw new IllegalArgumentException("mimeParts is NULL");

        // clear the MIME part table
        getExtraMimePartsTable().clear();
        ((ExtraMimePartsTableModel)getExtraMimePartsTable().getModel()).setData(mimeParts);
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
        if(mimePartsTable == null) {
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
        }
        return mimePartsTable;
    }

    private ExtraMimePartsTable getExtraMimePartsTable() {
        if(extraMimePartsTable == null) {
            extraMimePartsTable = new ExtraMimePartsTable();
        }
        return extraMimePartsTable;
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
                            if (mimePartsTable.isEditing()) {
                                mimePartsTable.getCellEditor().stopCellEditing();
                            }
                            BindingOperationInfo boInfo = (BindingOperationInfo) bindingOperationsTable.getModel().getValueAt(row, 0);
                            populateMimePartsData(new Vector(boInfo.getMultipart().values()));

                            if (selectedOperationForBinding != row) {
                                if (selectedOperationForBinding >= 0) {
                                    saveExtraData((BindingOperationInfo) bindingOperationsTable.getModel().getValueAt(selectedOperationForBinding, 0));
                                }

                                selectedOperationForBinding = row;
                                populateExtraMimePartsData(new Vector(boInfo.getExtraMultipart().values()));
                            }
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
            if (p!=null) setText(p.getBindingName());
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

    private Set bindingsList = new TreeSet(new Comparator() {
        public int compare(Object o1, Object o2) {
            BindingInfo p1 = (BindingInfo)o1;
            BindingInfo p2 = (BindingInfo)o2;
            return p1.getBindingName().compareToIgnoreCase(p2.getBindingName());
        }
    });

}
