package com.l7tech.console.panels;

import com.l7tech.console.util.WsdlComposer;
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
import java.util.logging.Logger;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class WsdlPortTypePanel extends WizardStepPanel {

    private static final Logger logger = Logger.getLogger(WsdlPortTypePanel.class.getName());

    private JPanel mainPanel;
    private JTextField portTypeNameField;
    private JTable operationsTable;
    private JScrollPane operationsTableScrollPane;
    private WsdlOperationsTableModel operationsModel;
    private JButton addOperationButton;
    private JButton removeOperatonButton;
    private JComboBox messagesComboBox = new JComboBox();
    private JLabel panelHeader;
    private WsdlComposer wsdlCompser;

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
    @Override
    public String getDescription() {

        return "<html>" +
        "The \"port type\" element of a Web service contains a set of abstract operations, " +
        "with each operation containing a set of abstract messages. In a RPC-type Web service, " +
        "the port type can be considered an interface definition in which each method is " +
        "defined as an operation." +
        "</html>";
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Port Type and Operations";
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     * 
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof WsdlComposer) {
            wsdlCompser = (WsdlComposer)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }

        PortType portType = wsdlCompser.getOrCreatePortType();
        portTypeNameField.setText(getLocalName(portType.getQName()));
        validate(wsdlCompser);

        operationsModel = new WsdlOperationsTableModel(wsdlCompser, portType);
        operationsTable.setModel(operationsModel);
        operationsTable.getTableHeader().setReorderingAllowed(false);

        Collection<Message> cm = new ArrayList<Message>(wsdlCompser.getMessages().values());
        cm.add(null);
        final Object[] messages = cm.toArray();
        messagesComboBox.setModel(new DefaultComboBoxModel(messages));
        messagesComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list,
                                           Object value,
                                           int index,
                                           boolean isSelected,
                                           boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value != null) {
                    QName qName = ((Message)value).getQName();
                    if ( qName != null ) {
                        setText(WsdlCreateWizard.prefixedName(qName, wsdlCompser));
                    } else {
                        setText("             ");                        
                    }
                } else {
                    setText("             ");
                }

                return this;
            }
        });

        DefaultCellEditor messageEditor = new DefaultCellEditor(messagesComboBox) {
            boolean canEdit = messages.length > 0;

            @Override
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
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof WsdlComposer) {
            wsdlCompser = (WsdlComposer)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }

        validate(wsdlCompser);

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
              }
          }
      };


    private DefaultTableCellRenderer operationsTableCellRenderer = new DefaultTableCellRenderer() {
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
          @Override
          public Component getTableCellRendererComponent(
                                JTable table,
                                Object value,
                                boolean isSelected,
                                boolean hasFocus,
                                int row, int column) {

              super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

              if (value instanceof Input) {
                  Input in = (Input)value;
                  renderMessage(in.getMessage());
              } else if (value instanceof Output) {
                  Output out = (Output)value;
                  renderMessage(out.getMessage());
              } else {
                  setText(value == null ? "" : value.toString());
              }
              return this;
          }

          private void renderMessage(Message msg) {
              String text = msg == null || msg.getQName() == null ?
                      "" :
                      WsdlCreateWizard.prefixedName(msg.getQName(), wsdlCompser);

              setText(text);
          }
      };

    /**
     * Validate (and sync if needed) the existing port type with the
     * wsdl definition. The changes might have happened to the
     * WSDL definition (through different wsdl elements) and the port
     * type may not be aware of this.
     * 
     * @param composer the wsdl definition
     */
    private void validate( WsdlComposer composer) {
        PortType portType = composer.getOrCreatePortType();

        if (needPortTypeUpdate(composer, portType)) {
            logger.fine("target namespace changed, updating port type....");
            updatePortType(portType, composer);
        }
        validateOperations(composer);
    }

    private boolean needPortTypeUpdate(WsdlComposer def, PortType p) {
        return
          !def.getTargetNamespace().equals(getNamespaceURI(p.getQName())) ||
          !portTypeNameField.getText().equals(getLocalName(p.getQName()));

    }

    private void updatePortType(PortType p, WsdlComposer composer) {
//        composer.removePortType(p);
//        PortType newPortType = composer.createPortType();
        p.setQName(new QName(composer.getTargetNamespace(), portTypeNameField.getText()));
        p.setUndefined(false);

//        List originalOperations = p.getOperations();
//        for (Object o : originalOperations) {
//            Operation op = (Operation)o;
//            p.addOperation(op);
//        }
//        composer.addPortType(newPortType, null);
    }


    private String getLocalName(QName qname) {
        String name = null;

        if ( qname != null ) {
            name = qname.getLocalPart();
        }

        return name;
    }

    private String getNamespaceURI(QName qname) {
        String name = null;

        if ( qname != null ) {
            name = qname.getNamespaceURI();
        }

        return name;
    }

    private void validateOperations(WsdlComposer composer) {
        PortType portType = composer.getOrCreatePortType();

        java.util.List operations = portType.getOperations();
        for (Object o : operations) {
            Operation op = (Operation)o;
            Input input = op.getInput();
            if (input != null) {
                Message m = input.getMessage();
                if (m != null) {
                    if (!composer.getMessages().containsKey(m.getQName())) {
                        input.setMessage(null);
                    }
                }
            }
            Output output = op.getOutput();
            if (output != null) {
                Message m = output.getMessage();
                if (m != null) {
                    if (!composer.getMessages().containsKey(m.getQName())) {
                        output.setMessage(null);
                    }
                }
            }
        }
    }
}
