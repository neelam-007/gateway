package com.l7tech.console.panels;

import com.l7tech.console.table.WsdlOperationsTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.wsdl.*;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
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
    private JLabel panelHeader;

    public WsdlPortTypePanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    private void initialize() {
        panelHeader.setFont(new java.awt.Font("Dialog", 1, 16));
        operationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        operationsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        operationsTable.setSurrendersFocusOnKeystroke(true);

        operationsTableScrollPane.getViewport().setBackground(operationsTable.getBackground());
        operationsTable.setDefaultRenderer(Object.class, operationsTableCellRenderer);
        operationsTable.setDefaultRenderer(Input.class, operationsTableCellRenderer);
        operationsTable.setDefaultRenderer(Output.class, operationsTableCellRenderer);
        operationsTable.getSelectionModel().
          addListSelectionListener(new ListSelectionListener() {
              public void valueChanged(ListSelectionEvent e) {
                  final int selectedRow = operationsTable.getSelectedRow();
                  removeOperatonButton.setEnabled(selectedRow != -1 && selectedRow<operationsTable.getRowCount()-1);
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

        return "<html>" +
        "The \"port type\" element of a Web service contains a set of abstract operations, " +
        "with each operation containing a set of abstract messages. In a RPC-type Web service, " +
        "the port type can be considered an interface definition in which each method is " +
        "defined as an operation." +
        "</html>";
        /*return "<html>" +
          "The <i>port type</i> element contains a set of abstract operations and the abstract " +
          "messages involved. " +
          "For RPC-style Web services a <i>portType</i> can be thought as an interface definition " +
          "in which each method can be defined as an operation." +
          "</html>";*/
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
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof Definition) {
            definition = (Definition)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        validate(definition);
        PortType portType = getOrCreatePortType(definition);

        operationsModel = new WsdlOperationsTableModel(definition, portType);
        operationsTable.setModel(operationsModel);
        operationsTable.getTableHeader().setReorderingAllowed(false);

        Collection cm = new ArrayList(definition.getMessages().values());
        cm.add(null);
        final Object[] messages = cm.toArray();
        messagesComboBox.setModel(new DefaultComboBoxModel(messages));
        messagesComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component
              getListCellRendererComponent(JList list,
                                           Object value,
                                           int index,
                                           boolean isSelected,
                                           boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value != null) {
                    QName qName = ((Message)value).getQName();
                    setText(WsdlCreateWizard.prefixedName(qName, definition));
                } else {
                    setText("             ");
                }

                return this;
            }
        });

        DefaultCellEditor messageEditor = new DefaultCellEditor(messagesComboBox) {
            boolean canEdit = messages.length > 0;

            public boolean stopCellEditing() {
                if (canEdit) {
                    return super.stopCellEditing();
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        cancelCellEditing();
                    }
                });
                return false;
            }
        };

        operationsTable.setDefaultEditor(Output.class, messageEditor);
        operationsTable.setDefaultEditor(Input.class, messageEditor);
        removeOperatonButton.setEnabled(operationsModel.getRowCount() > 0);
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

        validate(definition);

    }

    private ActionListener
      addOperationActionListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              operationsModel.addOperation();
          }
      };

    /**
     * remove operation listener
     */
    private ActionListener
      removeOperationActionListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              int selectedRow = operationsTable.getSelectedRow();
              if (selectedRow != -1) {
                  operationsModel.removeOperation(selectedRow);
                  return;
              }
          }
      };


    private
    DefaultTableCellRenderer operationsTableCellRenderer
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
              super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

              if (value instanceof Input) {
                  Input in = (Input)value;
                  renderMessage(table, in.getMessage(), isSelected);
              } else if (value instanceof Output) {
                  Output out = (Output)value;
                  renderMessage(table, out.getMessage(), isSelected);
              } else {
                  setText(value == null ? "" : value.toString());
              }
              return this;
          }

          private void renderMessage(JTable table, Message msg, boolean isSelected) {
              String text = msg == null ?
                      "" :
                      WsdlCreateWizard.prefixedName(msg.getQName(), definition);

              setText(text);
          }
      };

    /**
     * Retrieve the port type. Create the new port type if necessary
     * 
     * @return the port type
     */
    private PortType getOrCreatePortType(Definition def) {
        PortType portType = null;
        Map portTypes = def.getPortTypes();
        if (portTypes.isEmpty()) {
            portType = def.createPortType();
            portType.setQName(new QName(def.getTargetNamespace(), portTypeNameField.getText()));
            portType.setUndefined(false);
            def.addPortType(portType);
        } else {
            portType = (PortType)portTypes.values().iterator().next();
        }
        return portType;
    }

    /**
     * Validate (and sync if needed) the existing port type with the
     * wsdl definition. The changes might have happened to the
     * WSDL definition (through different wsdl elements) and the port
     * type may not be aware of this.
     * 
     * @param def the wsdl definition
     */
    private void validate(Definition def) {
        PortType p = getOrCreatePortType(def);

        if (needPortTypeUpdate(def, p)) {
            WsdlCreateWizard.log.fine("target namespace changed, updating port type....");
            updatePortType(p, def);
        }
        validateOperations(def);
    }

    private boolean needPortTypeUpdate(Definition def, PortType p) {
        return
          !def.getTargetNamespace().equals(p.getQName().getNamespaceURI()) ||
          !portTypeNameField.getText().equals(p.getQName().getLocalPart());

    }

    private void updatePortType(PortType p, Definition def) {
        def.removePortType(p.getQName());
        PortType portType = def.createPortType();
        portType.setQName(new QName(def.getTargetNamespace(), portTypeNameField.getText()));
        portType.setUndefined(false);
        def.addPortType(portType);
        java.util.List operations = p.getOperations();
        for (Iterator iterator = operations.iterator(); iterator.hasNext();) {
            Operation op = (Operation)iterator.next();
            portType.addOperation(op);
        }
    }

    private void validateOperations(Definition def) {
        PortType p = getOrCreatePortType(def);

        java.util.List operations = p.getOperations();
        for (Iterator iterator = operations.iterator(); iterator.hasNext();) {
            Operation op = (Operation)iterator.next();
            Input input = op.getInput();
            if (input != null) {
                Message m = input.getMessage();
                if (m != null) {
                    if (!def.getMessages().containsKey(m.getQName())) {
                        input.setMessage(null);
                    }
                }
            }
            Output output = op.getOutput();
            if (output != null) {
                Message m = output.getMessage();
                if (m != null) {
                    if (!def.getMessages().containsKey(m.getQName())) {
                        output.setMessage(null);
                    }
                }
            }
        }
    }


}
