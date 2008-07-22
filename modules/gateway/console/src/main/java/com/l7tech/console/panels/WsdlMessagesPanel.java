package com.l7tech.console.panels;


import com.l7tech.console.util.WsdlComposer;
import com.l7tech.xml.XmlSchemaConstants;
import com.l7tech.console.table.WsdlMessagePartsTableModel;
import com.l7tech.console.table.WsdlMessagesTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
//    private Definition definition;
    private WsdlComposer wsdlComposer;
    private JComboBox partTypesComboBox;
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
                  if (selectedRow == -1 || selectedRow>=messagesTable.getRowCount()-1) {
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

        messagesTable.
          getSelectionModel().addListSelectionListener(messagesTableSelectionListener);

        messagesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        messagesTable.setSurrendersFocusOnKeystroke(true);
        messagesTable.setDefaultRenderer(Object.class, new UniqueTableCellRenderer(messagesTable, "Message names must be unique."));

        //parts table
        partsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        partsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                removeMessagePartButton.setEnabled(partsTableModel.getRowCount() > 0);
            }
        });
        partsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        partsTable.setSurrendersFocusOnKeystroke(true);
        partsTable.setDefaultRenderer(Object.class, new PartsTableCellRenderer(wsdlComposer, partsTable));

        partTypesComboBox = new JComboBox(XmlSchemaConstants.QNAMES.toArray());
        partTypesComboBox.setBackground(partsTable.getBackground());
        partTypesComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list,
                                           Object value,
                                           int index,
                                           boolean isSelected,
                                           boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                QName qName = (QName) value;
                if (qName != null)
                    setText(WsdlCreateWizard.prefixedName(qName, wsdlComposer));
                else
                    setText("");

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
    @Override
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
    @Override
    public String getStepLabel() {
        return "Messages";
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
        if (!(settings instanceof WsdlComposer)) {
            throw new IllegalArgumentException("Unexpected type. " + settings.getClass() + ". Expected " + WsdlComposer.class);
        }

        wsdlComposer = (WsdlComposer) settings;
        updateMessageTable();
    }

    /**
     * update the message table model, renderer, and cell editor
     */
    private void updateMessageTable() {
        messagesTableModel = new WsdlMessagesTableModel(wsdlComposer);
        messagesTable.setModel(messagesTableModel);

        if (messagesTableModel.getRowCount()==0) {
            addMessageActionListener.actionPerformed(null);
        }
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
            wsdlComposer = (WsdlComposer)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type. " + settings.getClass() + ". Expected " + WsdlComposer.class);
        }
        ensureValid(messagesTableModel.getMessages(), wsdlComposer);
    }

    private static class PartsTableCellRenderer extends UniqueTableCellRenderer {
        private final WsdlComposer wsdlComposer;

        PartsTableCellRenderer(final WsdlComposer composer, final JTable table) {
            super(table, "Part names must be unique (within each message)");
            this.wsdlComposer = composer;
        }

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
          public Component getTableCellRendererComponent(JTable table,
                                          Object value,
                                          boolean isSelected,
                                          boolean hasFocus,
                                          int row, int column) {
              super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

              String text;
              if (value instanceof QName) {
                  QName qName = (QName)value;
                  text = WsdlCreateWizard.prefixedName(qName, wsdlComposer);
              } else {
                  text = value == null ? "" : value.toString();
              }

              setText(text);

              return this;
          }
      }

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
              java.util.List<Part> parts = messagesTableModel.getMessageParts(selectedRow);

              if (parts != null) {
                  partsTableModel = new WsdlMessagePartsTableModel(parts, wsdlComposer);
                  partsTable.setModel(partsTableModel);
                  partsTable.setDefaultRenderer(Object.class, new PartsTableCellRenderer(wsdlComposer, partsTable));
                  partsTable.getTableHeader().setReorderingAllowed(false);

                  DefaultCellEditor cellEditor = new DefaultCellEditor(new JTextField());
                  partsTable.setDefaultEditor(String.class, cellEditor);

                  cellEditor = new DefaultCellEditor(partTypesComboBox);
                  cellEditor.setClickCountToStart(1);
                  partsTable.setDefaultEditor(QName.class, cellEditor);
              }
              else {
                  partsTable.setModel(new DefaultTableModel(new String[]{"Name", "Type"}, 0));
              }
          }
      };

    private ActionListener
      addMessageActionListener = new ActionListener() {
          /**
           * Invoked when an action occurs.
           */
          public void actionPerformed(ActionEvent e) {
              String newMessageName;
              boolean found;
              int suffixAdd = 0;
              while (true) {
                  int msgSuffix = messagesTableModel.getRowCount() + suffixAdd;
                  newMessageName = "NewMessage" + msgSuffix;
                  found = true;
                  int rows = messagesTableModel.getRowCount();
                  for (int i = 0; i < rows; i++) {
                      String messageName = messagesTableModel.getValueAt(i, 0);
                      if (messageName != null && messageName.equals(newMessageName)) {
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
          public void actionPerformed(ActionEvent e) {
              int index = messagesTable.getSelectedRow();
              if (index != -1) {
                  messagesTableModel.removeMessage(index);
              }
          }
      };

    private ActionListener
      addPartActionListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              partsTableModel.addPart();
              partsTableModel.fireTableDataChanged();
          }
      };

    private ActionListener
      removePartActionListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              int index = partsTable.getSelectedRow();
              if (index != -1) {
                  partsTableModel.removePart(index);
              }
          }
      };

    private static class UniqueTableCellRenderer extends DefaultTableCellRenderer {
        private final JTable table;
        private final String warningMessage;
        private Color defaultColor = null;
        private Color defaultSelectedColor = null;

        UniqueTableCellRenderer(final JTable table, final String warningMessage) {
            this.table = table;
            this.warningMessage = warningMessage;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JComponent component = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isUnique(row, value)) {
                if (isSelected) {
                    if (defaultSelectedColor == null) defaultSelectedColor = component.getForeground();
                } else {
                    if (defaultColor == null) defaultColor = component.getForeground();
                }
                component.setForeground(Color.red);
                component.setToolTipText(warningMessage);
            } else {
                if (isSelected) {
                    if (defaultSelectedColor != null) component.setForeground(defaultSelectedColor);
                } else {
                    if (defaultColor != null) component.setForeground(defaultColor);
                }
                component.setToolTipText(null);
            }
            return component;
        }

        private boolean isUnique(int row, Object value) {
            boolean unique = true;

            if (value != null) {
                for (int i=0; i<table.getRowCount(); i++) {
                    if (i==row) continue;
                    Object rowValue = table.getValueAt(i, 0);
                    if (rowValue != null && rowValue.equals(value)) {
                        unique = false;
                        break;
                    }
                }
            }

            return unique;
        }
    }

    /**
     * Validate (and sync if needed) the existing messages with the
     * wsdl definition. The changes might have happened to the
     * WSDL definition (through different wsdl elements) and the messages
     * may not be aware of this.
     * 
     * @param messages the list of wsdl messages
     * @param composer      the wsdl composer used by this panel
     */
    private void ensureValid(java.util.List<Message> messages, WsdlComposer composer) {
        // remove old
        composer.getMessages().clear();

        // update and add new
        final String defTargetNamespace = composer.getTargetNamespace();
        for (Message message : messages) {

            if ( message.getQName() != null &&
                 !defTargetNamespace.equals(message.getQName().getNamespaceURI())) {
                message.setQName(new QName(defTargetNamespace, message.getQName().getLocalPart()));
            }
            
            composer.addMessage(message);
        }
    }
}
