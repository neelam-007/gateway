package com.l7tech.console.panels;


import com.l7tech.common.xml.XmlSchemaConstants;
import com.l7tech.console.table.WsdlMessagesTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 
 */
public class WsdlMessagesPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JTable messagesTable;
    private WsdlMessagesTableModel messagesTableModel;

    private JScrollPane partsTableScrollPane;
    private JTable partsTable;
    private DefaultTableModel partsTableModel;
    private JButton addMessageButton;
    private JButton removeMessageButton;
    private JButton addMessagePartButton;
    private JButton removeMessagePartButton;
    private Definition definition;

    public WsdlMessagesPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    private void initialize() {
        messagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messagesTable.setShowGrid(false);
        // cellEditor.setClickCountToStart(1);

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

        partsTableModel = new DefaultTableModel(
          new String[]{
              "Name",
              "Type"
          },
          0
        );
        partsTable.setModel(partsTableModel);
        partsTable.setDefaultRenderer(Object.class, partsTableCellRenderer);

        JComboBox typesComboBox = new JComboBox(XmlSchemaConstants.QNAMES.toArray());
        DefaultCellEditor cellEditor = new DefaultCellEditor(new JTextField());
        cellEditor.setClickCountToStart(1);
        partsTable.getColumnModel().getColumn(0).setCellEditor(cellEditor);

        cellEditor = new DefaultCellEditor(typesComboBox);
        cellEditor.setClickCountToStart(1);
        partsTable.getColumnModel().getColumn(1).setCellEditor(cellEditor);

        partsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                removeMessagePartButton.setEnabled(partsTableModel.getRowCount() > 0);
            }
        });

        partsTableScrollPane.getViewport().setBackground(partsTable.getBackground());
        addMessagePartButton.addActionListener(addPartActionListener);
        removeMessagePartButton.addActionListener(removePartActionListener);
        addMessagePartButton.setEnabled(false);
        removeMessagePartButton.setEnabled(false);

        typesComboBox.setBackground(partsTable.getBackground());
        typesComboBox.setRenderer(new DefaultListCellRenderer() {
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
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "Messages";
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
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof Definition)) {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        definition = (Definition)settings;
        messagesTableModel = new WsdlMessagesTableModel(definition);
        messagesTable.setModel(messagesTableModel);
        DefaultCellEditor cellEditor = new DefaultCellEditor(new JTextField());

        messagesTable.getColumnModel().getColumn(0).setCellEditor(cellEditor);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setEnabled(false);
        renderer.setVisible(false);
        messagesTable.getColumnModel().getColumn(0).setHeaderRenderer(renderer);

    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     *
     * This is a noop version that subclasses implement.
     *
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof Definition) {
            definition = (Definition)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }

        final Map messages = definition.getMessages();
        messages.clear();
        // messages.put()
    }

    private
        DefaultTableCellRenderer messagesTableCellRenderer
        = new DefaultTableCellRenderer() {
        /**
         *
         * Returns the default table cell renderer.
         *
         * @param table  the <code>JTable</code>
         * @param value  the value to assign to the cell at
         *			<code>[row, column]</code>
         * @param isSelected true if cell is selected
         * @param hasFocus true if cell has focus
         * @param row  the row of the cell to render
         * @param column the column of the cell to render
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
                super.getTableCellRendererComponent(
                  table, value,
                  isSelected, hasFocus, row, column);
            }
            return this;
        }
    };

    private
    DefaultTableCellRenderer partsTableCellRenderer = new DefaultTableCellRenderer() {
        /**
         *
         * Returns the default table cell renderer.
         *
         * @param table  the <code>JTable</code>
         * @param value  the value to assign to the cell at
         *			<code>[row, column]</code>
         * @param isSelected true if cell is selected
         * @param hasFocus true if cell has focus
         * @param row  the row of the cell to render
         * @param column the column of the cell to render
         * @return the default table cell renderer
         */
        public Component
          getTableCellRendererComponent(JTable table,
                                        Object value,
                                        boolean isSelected,
                                        boolean hasFocus,
                                        int row, int column) {
            if (value instanceof QName) {
                QName qName = (QName)value;
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                    setForeground(table.getSelectionForeground());
                } else {
                    setBackground(table.getBackground());
                    setForeground(table.getForeground());
                }
                setText(WsdlCreateWizard.prefixedName(qName, definition));
            } else {
                super.getTableCellRendererComponent(
                  table, value,
                  isSelected, hasFocus, row, column);
            }
            return this;
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
              while (!found) {
                  newMessageName = "NewMessage" + messagesTableModel.getRowCount();
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
              }
          }
      };

    private ActionListener
      removeMessageActionListener = new ActionListener() {
          /** Invoked when an action occurs. */
          public void actionPerformed(ActionEvent e) {
              int index = messagesTable.getSelectedRow();
              if (index != -1) {
                  messagesTableModel.removeMessage(index);
              }
          }
      };

    private ActionListener
      addPartActionListener = new ActionListener() {
          /** Invoked when an action occurs.  */
          public void actionPerformed(ActionEvent e) {
              final int selectedRow = messagesTable.getSelectedRow();
              if (selectedRow == -1) {
                  throw new IllegalStateException("no selected message");
              }
              messagesTable.getValueAt(selectedRow, 0);
              partsTableModel.addRow(new
                Object[]{getNewMessagePartArgumentName(),
                         XmlSchemaConstants.QNAME_TYPE_STRING,
                         messagesTable.getValueAt(selectedRow, 0)});
          }

          private String getNewMessagePartArgumentName() {
              String newMessagePartName = null;
              boolean found = false;
              while (!found) {
                  newMessagePartName = "arg" + partsTableModel.getRowCount();
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
                  partsTableModel.removeRow(index);
              }
          }
      };

    {
// do not edit this generated initializer!!! do not add your code here!!!
        $$$setupUI$$$();
    }

    /** generated code, do not edit or call this method manually !!! */
    private void $$$setupUI$$$() {
        JPanel _1;
        _1 = new JPanel();
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        JPanel _2;
        _2 = new JPanel();
        mainPanel = _2;
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(9, 5, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _3;
        _3 = new JLabel();
        _3.setText("Messages");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _4;
        _4 = new JLabel();
        _4.setText("Parts");
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 2, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _5;
        _5 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 2, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _6;
        _6 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 2, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        JScrollPane _7;
        _7 = new JScrollPane();
        partsTableScrollPane = _7;
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 2, 0, 3, 7, 7, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTable _8;
        _8 = new JTable();
        partsTable = _8;
        _7.setViewportView(_8);
        JPanel _9;
        _9 = new JPanel();
        _9.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 2, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _10;
        _10 = new JButton();
        removeMessagePartButton = _10;
        _10.setText("Remove");
        _10.setLabel("Remove");
        _9.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _11;
        _11 = new JButton();
        addMessagePartButton = _11;
        _11.setText("Add");
        _11.setLabel("Add");
        _9.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _12;
        _12 = new com.intellij.uiDesigner.core.Spacer();
        _9.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_13, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 2, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        JPanel _14;
        _14 = new JPanel();
        _14.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_14, new com.intellij.uiDesigner.core.GridConstraints(3, 3, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _15;
        _15 = new JButton();
        removeMessageButton = _15;
        _15.setText("Remove");
        _15.setActionCommand("AddMessage");
        _15.setLabel("Remove");
        _14.add(_15, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _16;
        _16 = new JButton();
        addMessageButton = _16;
        _16.setText("Add");
        _16.setLabel("Add");
        _14.add(_16, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _17;
        _17 = new com.intellij.uiDesigner.core.Spacer();
        _14.add(_17, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTable _18;
        _18 = new JTable();
        messagesTable = _18;
        _2.add(_18, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, 0, 3, 6, 6, new Dimension(-1, -1), new Dimension(150, 50), new Dimension(-1, -1)));
    }


}
