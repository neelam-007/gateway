package com.l7tech.console.panels;


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
          "The <i>Message</i> element provides a common abstraction for messages passed " +
          "between the client and the server. " +
          "A message consists of one or more logical parts each of which is associated with " +
          "a type from the type system." +
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        mainPanel = _2;
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(10, 5, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _3;
        _3 = new JLabel();
        _3.setText("Parts:");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 2, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _4;
        _4 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 2, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        final com.intellij.uiDesigner.core.Spacer _5;
        _5 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 2, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        final JScrollPane _6;
        _6 = new JScrollPane();
        partsTableScrollPane = _6;
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 2, 0, 3, 7, 7, null, null, null));
        final JTable _7;
        _7 = new JTable();
        partsTable = _7;
        _6.setViewportView(_7);
        final JPanel _8;
        _8 = new JPanel();
        _8.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 2, 0, 3, 3, 3, null, null, null));
        final JButton _9;
        _9 = new JButton();
        removeMessagePartButton = _9;
        _9.setText("Remove");
        _9.setLabel("Remove");
        _8.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 4, 0, 0, 0, null, null, null));
        final JButton _10;
        _10 = new JButton();
        addMessagePartButton = _10;
        _10.setText("Add");
        _10.setLabel("Add");
        _8.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 4, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _11;
        _11 = new com.intellij.uiDesigner.core.Spacer();
        _8.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _12;
        _12 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_12, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 2, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        final JPanel _13;
        _13 = new JPanel();
        _13.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_13, new com.intellij.uiDesigner.core.GridConstraints(4, 3, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _14;
        _14 = new JButton();
        removeMessageButton = _14;
        _14.setText("Remove");
        _14.setLabel("Remove");
        _14.setActionCommand("AddMessage");
        _13.add(_14, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 4, 0, 0, 0, null, null, null));
        final JButton _15;
        _15 = new JButton();
        addMessageButton = _15;
        _15.setText("Add");
        _15.setLabel("Add");
        _13.add(_15, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 4, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _16;
        _16 = new com.intellij.uiDesigner.core.Spacer();
        _13.add(_16, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JScrollPane _17;
        _17 = new JScrollPane();
        messagesTableModelTableScrollPane = _17;
        _2.add(_17, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, 0, 3, 7, 7, null, null, null));
        final JTable _18;
        _18 = new JTable();
        messagesTable = _18;
        _17.setViewportView(_18);
        final JLabel _19;
        _19 = new JLabel();
        _19.setText("Message List:");
        _2.add(_19, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _20;
        _20 = new JLabel();
        panelHeader = _20;
        _20.setText("Messages");
        _2.add(_20, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, null, null, null));
    }


}
