package com.l7tech.console.panels;


import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.common.xml.XmlSchemaConstants;
import com.l7tech.console.table.WsdlMessagePartsTableModel;
import com.l7tech.console.table.WsdlMessagesTableModel;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class WsdlMessagesPanel extends WizardStepPanel {

    static Logger log = Logger.getLogger(WsdlMessagesPanel.class.getName());

    private JPanel mainPanel;
    private JTable messagesTable;
    private WsdlMessagesTableModel messagesTableModel;
    private JScrollPane messagesTableModelTableScrollPane;

    private JScrollPane partsTableScrollPane;
    private JTable partsTable;
    private WsdlMessagePartsTableModel partsTableModel;
    private JButton addMessageButton;
    private JButton removeMessageButton;
    private JButton addMessagePartButton;
    private JButton removeMessagePartButton;
    private Definition definition;
    private JComboBox partTypesComboBox;
    private CellEditorListener cellEditorListener;
    private DefaultCellEditor messageNameCellEditor;
    private JLabel panelHeader;

    public WsdlMessagesPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    private void initialize() {
        panelHeader.setFont(new java.awt.Font("Dialog", 1, 16));
        messagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messagesTable.setShowGrid(false);
        JViewport viewport = messagesTableModelTableScrollPane.getViewport();
        viewport.setBackground(messagesTable.getBackground());
        messagesTable.getTableHeader().setReorderingAllowed(false);

        addMessageButton.addActionListener(addMessageActionListener);
        removeMessageButton.addActionListener(removeMessageActionListener);
        removeMessageButton.setEnabled(false);

        messagesTable.getSelectionModel().
          addListSelectionListener(new ListSelectionListener() {
              public void valueChanged(ListSelectionEvent e) {
                  final int selectedRow = messagesTable.getSelectedRow();
                  if (selectedRow == -1) {
                      addMessageButton.setEnabled(true);
                      removeMessageButton.setEnabled(false);
                      addMessagePartButton.setEnabled(false);
                      removeMessagePartButton.setEnabled(false);
                      return;
                  }
                  addMessageButton.setEnabled(true);
                  removeMessageButton.setEnabled(true);
                  addMessagePartButton.setEnabled(true);
                  removeMessagePartButton.setEnabled(true);
              }
          });

        messagesTable.setDefaultRenderer(Object.class, messagesTableCellRenderer);
        messagesTable.
          getSelectionModel().addListSelectionListener(messagesTableSelectionListener);

        messagesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        messageNameCellEditor =
          new DefaultCellEditor(new JTextField()) {
              WsdlMessagesTableModel.MutableMessage message;
              int editedRow;

              /**
               * Implements the <code>TableCellEditor</code> interface.
               */
              public Component
                getTableCellEditorComponent(JTable table, Object value,
                                            boolean isSelected,
                                            int row, int column) {
                  message = (WsdlMessagesTableModel.MutableMessage)value;
                  editedRow = row;
                  delegate.setValue(message.getQName().getLocalPart());
                  return editorComponent;
              }

              /**
               * Forwards the message from the <code>CellEditor</code> to
               * the <code>delegate</code>.
               * 
               * @see EditorDelegate#getCellEditorValue
               */
              public Object getCellEditorValue() {
                  QName on = message.getQName();
                  QName nn = new QName(on.getNamespaceURI(), (String)super.getCellEditorValue());
                  Message nm = new WsdlMessagesTableModel.MutableMessage();
                  nm.setUndefined(false);
                  nm.setQName(nn);

                  Map parts = message.getParts();
                  Iterator pi = message.getadditionOrderOfParts().iterator();
                  while (pi.hasNext()) {
                      Part p = (Part)parts.get(pi.next());
                      nm.addPart(p);
                  }
                  messagesTable.getSelectionModel().setSelectionInterval(editedRow, editedRow);
                  Runnable runnable = new Runnable() {
                      public void run() {
                          messagesTableSelectionListener.valueChanged(null);
                      }
                  };
                  SwingUtilities.invokeLater(runnable);
                  return nm;
              }
          };

        cellEditorListener = new CellEditorListener() {
            public void editingCanceled(ChangeEvent e) {
            }

            public void editingStopped(ChangeEvent e) {
            }
        };
        messageNameCellEditor.addCellEditorListener(cellEditorListener);
        messagesTable.setDefaultEditor(Object.class, messageNameCellEditor);

        //parts table
        partsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        partsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                removeMessagePartButton.setEnabled(partsTableModel.getRowCount() > 0);
            }
        });
        partsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        partTypesComboBox = new JComboBox(XmlSchemaConstants.QNAMES.toArray());
        partTypesComboBox.setBackground(partsTable.getBackground());
        partTypesComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component
              getListCellRendererComponent(JList list,
                                           Object value,
                                           int index,
                                           boolean isSelected,
                                           boolean cellHasFocus) {
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
                QName qName = (QName)value;
                setText(WsdlCreateWizard.prefixedName(qName, definition));
                return this;
            }
        });

        partsTableScrollPane.getViewport().setBackground(partsTable.getBackground());
        addMessagePartButton.addActionListener(addPartActionListener);
        removeMessagePartButton.addActionListener(removePartActionListener);
        addMessagePartButton.setEnabled(false);
        removeMessagePartButton.setEnabled(false);
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "<html>" +
               "The \"message\" element of a Web service provides a common abstraction for messages passed " +
               "between the client and the server. Each message consists of one or more logical parts, with " +
               "each part defined by a particular type element." +
               "</html>";
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Messages";
    }

    /**
     * Test whether the step is finished and it is safe to proceed to the next
     * one.
     * If the step is valid, the "Next" (or "Finish") button will be enabled.
     * 
     * @return true if the panel is valid, false otherwis
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     * 
     * @return true if the panel is valid, false otherwis
     */

    public boolean canFinish() {
        return false;
    }


    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     * 
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof Definition)) {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        if (settings != definition) {
            definition = (Definition)settings;
            updateMessageTable();
        }
    }

    /**
     * update the message table model, renderer, and cell editor
     */
    private void updateMessageTable() {
        messagesTableModel = new WsdlMessagesTableModel(definition);
        messagesTable.setModel(messagesTableModel);


        if (messagesTableModel.getRowCount() == 0) {
            addMessageActionListener.actionPerformed(null);
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                messagesTable.getSelectionModel().setSelectionInterval(0, 0);
            }
        });
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * 
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof Definition) {
            definition = (Definition)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        validate(messagesTableModel.getRows(), definition);
    }

    private
    DefaultTableCellRenderer messagesTableCellRenderer
      = new DefaultTableCellRenderer() {
          /**
           * Returns the default table cell renderer.
           * 
           * @param table      the <code>JTable</code>
           * @param value      the value to assign to the cell at
           *                   <code>[row, column]</code>
           * @param isSelected true if cell is selected
           * @param hasFocus   true if cell has focus
           * @param row        the row of the cell to render
           * @param column     the column of the cell to render
           * @return the default table cell renderer
           */
          public Component
            getTableCellRendererComponent(JTable table,
                                          Object value,
                                          boolean isSelected,
                                          boolean hasFocus,
                                          int row, int column) {
              if (value instanceof Message) {
                  Message message = (Message)value;
                  if (isSelected) {
                      setBackground(table.getSelectionBackground());
                      setForeground(table.getSelectionForeground());
                  } else {
                      setBackground(table.getBackground());
                      setForeground(table.getForeground());
                  }
                  setText(message.getQName().getLocalPart());
              } else {
                  super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
              }
              return this;
          }
      };

    private
    DefaultTableCellRenderer partsTableCellRenderer
      = new DefaultTableCellRenderer() {
          /**
           * Returns the default table cell renderer.
           * 
           * @param table      the <code>JTable</code>
           * @param value      the value to assign to the cell at
           *                   <code>[row, column]</code>
           * @param isSelected true if cell is selected
           * @param hasFocus   true if cell has focus
           * @param row        the row of the cell to render
           * @param column     the column of the cell to render
           * @return the default table cell renderer
           */
          public Component
            getTableCellRendererComponent(JTable table,
                                          Object value,
                                          boolean isSelected,
                                          boolean hasFocus,
                                          int row, int column) {
              String text = null;
              if (value instanceof QName) {
                  QName qName = (QName)value;
                  text = WsdlCreateWizard.prefixedName(qName, definition);
              } else {
                  text = value.toString();
              }
              if (isSelected) {
                  setBackground(table.getSelectionBackground());
                  setForeground(table.getSelectionForeground());
              } else {
                  setBackground(table.getBackground());
                  setForeground(table.getForeground());
              }
              setText(text);

              return this;
          }
      };

    private ListSelectionListener
      messagesTableSelectionListener = new ListSelectionListener() {
          /**
           * Called whenever the value of the selection changes.
           * 
           * @param e the event that characterizes the change.
           */
          public void valueChanged(ListSelectionEvent e) {
              if (messagesTable.isEditing()) {
                  messagesTable.getCellEditor().stopCellEditing();
                  return;
              }
              if (partsTable.isEditing()) {
                  partsTable.getCellEditor().stopCellEditing();
              }
              int selectedRow = messagesTable.getSelectedRow();
              if (selectedRow == -1) {
                  partsTable.setModel(new DefaultTableModel(new String[]{"Name", "Type"}, 0));
                  return;
              }
              WsdlMessagesTableModel.MutableMessage m =
                (WsdlMessagesTableModel.MutableMessage)messagesTable.getValueAt(selectedRow,
                  messagesTable.getSelectedColumn());
              partsTableModel = new WsdlMessagePartsTableModel(m, definition);
              partsTable.setModel(partsTableModel);
              partsTable.setDefaultRenderer(Object.class, partsTableCellRenderer);
              partsTable.getTableHeader().setReorderingAllowed(false);

              DefaultCellEditor cellEditor = new DefaultCellEditor(new JTextField());
              partsTable.setDefaultEditor(String.class, cellEditor);

              cellEditor = new DefaultCellEditor(partTypesComboBox);
              cellEditor.setClickCountToStart(1);
              partsTable.setDefaultEditor(QName.class, cellEditor);
          }
      };

    private ActionListener
      addMessageActionListener = new ActionListener() {
          /**
           * Invoked when an action occurs.
           */
          public void actionPerformed(ActionEvent e) {
              String newMessageName = null;
              boolean found = false;
              int suffixAdd = 0;
              while (!found) {
                  int msgSuffix = messagesTableModel.getRowCount() + suffixAdd;
                  newMessageName = "NewMessage" + msgSuffix;
                  found = true;
                  int rows = messagesTableModel.getRowCount();
                  for (int i = 0; i < rows; i++) {
                      String name =
                        ((Message)messagesTableModel.getValueAt(i, 0)).getQName().getLocalPart();
                      if (name.equals(newMessageName)) {
                          found = false;
                          break;
                      }
                  }
                  if (found) {
                      messagesTableModel.addMessage(newMessageName);
                      break;
                  }
                  suffixAdd++;
              }
          }
      };

    private ActionListener
      removeMessageActionListener = new ActionListener() {
          /**
           * Invoked when an action occurs.
           */
          public void actionPerformed(ActionEvent e) {
              int index = messagesTable.getSelectedRow();
              if (index != -1) {
                  messagesTableModel.removeMessage(index);
              }
          }
      };

    private ActionListener
      addPartActionListener = new ActionListener() {
          /**
           * Invoked when an action occurs.
           */
          public void actionPerformed(ActionEvent e) {
              Part p = partsTableModel.addPart(getNewMessagePartArgumentName());
              p.setTypeName(XmlSchemaConstants.QNAME_TYPE_STRING);
              partsTableModel.fireTableDataChanged();
          }

          private String getNewMessagePartArgumentName() {
              String newMessagePartName = null;
              boolean found = false;
              int suffixAdd = 0;
              while (!found) {
                  int partNameSuffix = partsTableModel.getRowCount() + suffixAdd;
                  newMessagePartName = "arg" + partNameSuffix;
                  found = true;
                  int rows = partsTableModel.getRowCount();
                  for (int i = 0; i < rows; i++) {
                      String name = (String)partsTableModel.getValueAt(i, 0);
                      if (name.equals(newMessagePartName)) {
                          found = false;
                          break;
                      }
                  }
                  if (found) {
                      break;
                  }
                  suffixAdd++;
              }
              return newMessagePartName;
          }
      };

    private ActionListener
      removePartActionListener = new ActionListener() {
          /**
           * Invoked when an action occurs.
           */
          public void actionPerformed(ActionEvent e) {
              int index = partsTable.getSelectedRow();
              if (index != -1) {
                  partsTableModel.removePart(index);
              }
          }
      };

    /**
     * Validate (and sync if needed) the existing messages with the
     * wsdl definition. The changes might have happened to the
     * WSDL definition (through different wsdl elements) and the messages
     * may not be aware of this.
     * 
     * @param messages the list of wsdl messages
     * @param def      the wsdl definition
     */
    private void validate(java.util.List messages, Definition def) {
        for (Iterator iterator = messages.iterator(); iterator.hasNext();) {
            Message m = (Message)iterator.next();
            final String defTargetNamespace = def.getTargetNamespace();
            if (!defTargetNamespace.equals(m.getQName().getNamespaceURI())) {
                m.setQName(new QName(defTargetNamespace, m.getQName().getLocalPart()));
            }
        }
    }


}
