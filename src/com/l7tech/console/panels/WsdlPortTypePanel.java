package com.l7tech.console.panels;

import com.l7tech.console.table.WsdlOperationsTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.wsdl.*;
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
public class WsdlPortTypePanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField portTypeNameField;
    private JTable operationsTable;
    private JScrollPane operationsTableScrollPane;
    private WsdlOperationsTableModel operationsModel;
    private JButton addOperationButton;
    private JButton removeOperatonButton;
    private Definition definition;
    private JComboBox messagesComboBox = new JComboBox();

    public WsdlPortTypePanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    private void initialize() {
        operationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        operationsTableScrollPane.getViewport().setBackground(operationsTable.getBackground());
        operationsTable.setDefaultRenderer(Object.class, operationsTableCellRenderer);
        operationsTable.setDefaultRenderer(Input.class, operationsTableCellRenderer);
        operationsTable.setDefaultRenderer(Output.class, operationsTableCellRenderer);
        operationsTable.getSelectionModel().
          addListSelectionListener(new ListSelectionListener() {
              public void valueChanged(ListSelectionEvent e) {
                  final int selectedRow = operationsTable.getSelectedRow();
                  removeOperatonButton.setEnabled(selectedRow != -1);
              }
          });

        addOperationButton.addActionListener(addOperationActionListener);
        removeOperatonButton.setEnabled(false);
        removeOperatonButton.addActionListener(removeOperationActionListener);
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "Port Type";
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Port Type and Operations";
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
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof Definition) {
            definition = (Definition)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        PortType portType = null;
        Map portTypes = definition.getPortTypes();
        if (portTypes.isEmpty()) {
            portType = definition.createPortType();
            portType.setQName(new QName(portTypeNameField.getText()));
            portType.setUndefined(false);
            definition.addPortType(portType);
        } else {
            portType = (PortType)portTypes.values().iterator().next();
        }
        operationsModel = new WsdlOperationsTableModel(definition, portType);
        operationsTable.setModel(operationsModel);
        Object[] messages = definition.getMessages().values().toArray();
        messagesComboBox.setModel(new DefaultComboBoxModel(messages));
        messagesComboBox.setRenderer(new DefaultListCellRenderer() {
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
                QName qName = ((Message)value).getQName();
                setText(WsdlCreateWizard.prefixedName(qName, definition));
                return this;
            }
        });
        operationsTable.setDefaultEditor(Output.class, new DefaultCellEditor(messagesComboBox));
        operationsTable.setDefaultEditor(Input.class, new DefaultCellEditor(messagesComboBox));
        removeOperatonButton.setEnabled(operationsModel.getRowCount() > 0);
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     *
     *
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
    }

    private ActionListener
      addOperationActionListener = new ActionListener() {
          /**
           * Invoked when an action occurs.
           */
          public void actionPerformed(ActionEvent e) {
              String newOperationName = null;
              boolean found = false;
              while (!found) {
                  newOperationName = "NewOperation" + operationsModel.getRowCount();
                  found = true;
                  int rows = operationsModel.getRowCount();
                  for (int i = 0; i < rows; i++) {
                      String name =
                        (String)operationsModel.getValueAt(i, 0);
                      if (name.equals(newOperationName)) {
                          found = false;
                          break;
                      }
                  }
                  if (found) {
                      operationsModel.addOperation(newOperationName);
                      break;
                  }
              }
          }
      };

    /** remove operation listener */
    private ActionListener
      removeOperationActionListener = new ActionListener() {
          /**
           * Invoked when an action occurs.
           */
          public void actionPerformed(ActionEvent e) {
              int selectedRow = operationsTable.getSelectedRow();
              if (selectedRow == -1) {
                  operationsModel.removeOperation(selectedRow);
                  return;
              }
          }
      };


    private
    DefaultTableCellRenderer operationsTableCellRenderer
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
              if (value instanceof Input) {
                  Input in = (Input)value;
                  renderMessage(table, in.getMessage(), isSelected);
              } else if (value instanceof Output) {
                  Output out = (Output)value;
                  renderMessage(table, out.getMessage(), isSelected);
              } else {
                  if (isSelected) {
                      setBackground(table.getSelectionBackground());
                      setForeground(table.getSelectionForeground());
                  } else {
                      setBackground(table.getBackground());
                      setForeground(table.getForeground());
                  }
                  setText(value.toString());
              }
              return this;
          }

          private void renderMessage(JTable table, Message msg, boolean isSelected) {
              String text = msg == null ? "" :
                WsdlCreateWizard.prefixedName(msg.getQName(), definition);
              if (isSelected) {
                  setBackground(table.getSelectionBackground());
                  setForeground(table.getSelectionForeground());
              } else {
                  setBackground(table.getBackground());
                  setForeground(table.getForeground());
              }
              setText(text);
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
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(9, 6, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _3;
        _3 = new JLabel();
        _3.setText("Name");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _4;
        _4 = new JTextField();
        portTypeNameField = _4;
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 2, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _5;
        _5 = new JLabel();
        _5.setText("Port Type");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JScrollPane _6;
        _6 = new JScrollPane();
        operationsTableScrollPane = _6;
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 3, 0, 3, 7, 7, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTable _7;
        _7 = new JTable();
        operationsTable = _7;
        _6.setViewportView(_7);
        JPanel _8;
        _8 = new JPanel();
        _8.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(8, 4, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _9;
        _9 = new JButton();
        removeOperatonButton = _9;
        _9.setText("Remove");
        _8.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _10;
        _10 = new JButton();
        addOperationButton = _10;
        _10.setText("Add");
        _10.setLabel("Add");
        _8.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _11;
        _11 = new com.intellij.uiDesigner.core.Spacer();
        _8.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _12;
        _12 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_12, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_13, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        JLabel _14;
        _14 = new JLabel();
        _14.setText("Operations");
        _2.add(_14, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
    }


}
