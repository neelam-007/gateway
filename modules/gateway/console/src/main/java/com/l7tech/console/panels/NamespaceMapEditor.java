package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.event.*;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * A dialog that lets the administrator specify a list of namespaces with corresponding
 * prefix in a Map for the XPath assertion. Some namespaces can be showed for viewing only
 * so that they cannot be removed.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 16, 2004<br/>
 * $Id$
 */
public class NamespaceMapEditor extends JDialog {

    private JPanel mainPanel;
    private JTable namespacesTable;
    private JButton removeButton;
    private JButton editButton;
    private JButton addButton;
    private JButton okButton;
    private JButton helpButton;
    private JButton cancelButton;

    private TreeMap<String,String> namespaceMap = new TreeMap<String,String>();
    private Map<String,String> forbiddenNamespaces;
    boolean cancelled = false;

    /**
     * Construct a dialog that lets the user edit a map of namespace/prefixes
     * @param owner the requesting dlg owner
     * @param predefinedNamespaces optional, the initial namespace map (key is prefix, value is uri)
     * @param forcedNamespaces optional, the namespace values that cannot be removed nor changed
     */
    public NamespaceMapEditor(Dialog owner, Map<String, String> predefinedNamespaces, Map<String, String> forcedNamespaces) {
        super(owner, true);
        initialize(predefinedNamespaces, forcedNamespaces);
    }

    /**
     * Construct a dialog that lets the user edit a map of namespace/prefixes
     * @param owner the requesting frame owner
     * @param predefinedNamespaces optional, the initial namespace map (key is prefix, value is uri)
     * @param forcedNamespaces optional, the namespace values that cannot be removed nor changed
     */
    public NamespaceMapEditor(Frame owner, Map<String, String> predefinedNamespaces, Map<String, String> forcedNamespaces) {
        super(owner, true);
        initialize(predefinedNamespaces, forcedNamespaces);
    }

    /**
     * @return null if the dialog was canceled, otherwise a map with the namespaces.
     */
    public Map<String, String> newNSMap() {
        if (cancelled) return null;
        return new HashMap<String, String>(namespaceMap);
    }

    private void initialize(Map<String, String> predefinedNamespaces, Map<String, String> forcedNamespaces) {
        if (predefinedNamespaces != null) {
            namespaceMap.putAll(predefinedNamespaces);
        }
        forbiddenNamespaces = forcedNamespaces;
        setContentPane(mainPanel);
        setTitle("Edit Namespaces and Prefixes");
        setTableModel();
        namespacesTable.getTableHeader().setReorderingAllowed(false);
        setListeners();
        enableRemoveBasedOnSelection();
    }

    private void setTableModel() {
        TableModel model = new AbstractTableModel() {
            @Override
            public int getColumnCount() {
                return 2;
            }
            @Override
            public int getRowCount() {
                return namespaceMap.size();
            }
            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0: return (new ArrayList<Map.Entry>(namespaceMap.entrySet()).get(rowIndex)).getKey();
                    case 1: return (new ArrayList<Map.Entry>(namespaceMap.entrySet()).get(rowIndex)).getValue();
                }
                return "";
            }
            @Override
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return "Prefix";
                    case 1: return "Namespace URI";
                }
                return "";
            }
        };
        namespacesTable.setModel(model);

        // render rows that cannot be edited with different font from ones that can be removed
        if (forbiddenNamespaces != null) {
            final TableCellRenderer normalCellRenderer = namespacesTable.getCellRenderer(0,0);
            TableCellRenderer specialCellRenderer = new TableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row,
                                                               int column) {
                    Component output = normalCellRenderer.getTableCellRendererComponent(table,
                                                                                        value,
                                                                                        isSelected,
                                                                                        hasFocus,
                                                                                        row,
                                                                                        column);
                    Font font = output.getFont();
                    Map<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>(font.getAttributes());
                    fontAttributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR);
                    fontAttributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
                    String pr = (String) namespacesTable.getModel().getValueAt(row, 0);
                    String ns = (String) namespacesTable.getModel().getValueAt(row, 1);
                    if ( !isEditableNamespace(pr,ns) ) {
                        fontAttributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
                        fontAttributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
                    }
                    Font newFont = Font.getFont(fontAttributes);
                    output.setFont(newFont);
                    return output;
                }
            };
            namespacesTable.getColumnModel().getColumn(0).setCellRenderer(specialCellRenderer);
            namespacesTable.getColumnModel().getColumn(1).setCellRenderer(specialCellRenderer);
            namespacesTable.getColumnModel().getColumn(0).setPreferredWidth(250);
            namespacesTable.getColumnModel().getColumn(1).setPreferredWidth(750);
        }
    }

    private void setListeners() {
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                edit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remove();
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        namespacesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableRemoveBasedOnSelection();
            }
        });

        // implement default behavior for esc and enter keys
        namespacesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    edit();
            }
        });
        
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void enableRemoveBasedOnSelection() {
        boolean enableContextSpecificButtons;
        // get new selection
        int selectedRow = namespacesTable.getSelectedRow();
        // decide whether or not the buttons should be enabled
        if (selectedRow < 0) {
            enableContextSpecificButtons = false;
        } else {
            String selectedPrefix= (String)namespacesTable.getModel().getValueAt(selectedRow, 0);
            String selectedNSURI = (String)namespacesTable.getModel().getValueAt(selectedRow, 1);
            enableContextSpecificButtons = isEditableNamespace(selectedPrefix,selectedNSURI);
        }
        editButton.setEnabled(enableContextSpecificButtons);
        removeButton.setEnabled(enableContextSpecificButtons);
    }

    private boolean isEditableNamespace(final String selectedPrefix, final String selectedNSURI) {
        return !(forbiddenNamespaces != null && 
                 forbiddenNamespaces.get(selectedPrefix) != null &&
                 forbiddenNamespaces.get(selectedPrefix).equals(selectedNSURI));
    }

    private void edit() {
        final int[] selectedRow = new int[] {namespacesTable.getSelectedRow()};
        final String originalPrefix = (String) namespacesTable.getModel().getValueAt(selectedRow[0], 0);
        final String originalNsURI = (String) namespacesTable.getModel().getValueAt(selectedRow[0], 1);
        if (!isEditableNamespace(originalPrefix, originalNsURI))
            return;

        final NamespacePrefixQueryForm grabber = new NamespacePrefixQueryForm(this, "Edit XML Namespace and Prefix");
        grabber.setInitialPrefix(originalPrefix);
        grabber.setInitialNsUri(originalNsURI);
        grabber.pack();
        Utilities.centerOnParentWindow(grabber);
        DialogDisplayer.display(grabber, new Runnable() {
            @Override
            public void run() {
                if (isInvalidNamespaceEntry(grabber, originalPrefix, originalNsURI)) return;

                // remove original map entry if prefix has changed
                if (namespaceMap.put(grabber.prefix, grabber.nsuri) == null)
                    namespaceMap.remove(originalPrefix) ;
                
                updateTable(selectedRow);
            }
        });
    }

    private void add() {
        final int[] selectedRow = new int[]{namespacesTable.getSelectedRow()};

        final NamespacePrefixQueryForm grabber = new NamespacePrefixQueryForm(this);
        grabber.pack();
        Utilities.centerOnParentWindow(grabber);
        DialogDisplayer.display(grabber, new Runnable() {
            @Override
            public void run() {
                if (isInvalidNamespaceEntry(grabber, null, null))
                    return;

                namespaceMap.put(grabber.prefix, grabber.nsuri);
                updateTable(selectedRow);
            }
        });

    }

    private void updateTable(int[] selectedRow) {
        ((AbstractTableModel) namespacesTable.getModel()).fireTableDataChanged();
        if (selectedRow[0] == -1 && namespacesTable.getModel().getRowCount() == 1) selectedRow[0] = 0;
        if (selectedRow[0] >= 0) {
            namespacesTable.getSelectionModel().setSelectionInterval(selectedRow[0], selectedRow[0]);
        }
        enableRemoveBasedOnSelection();
    }

    private boolean isInvalidNamespaceEntry(NamespacePrefixQueryForm grabber, String ignorePrefix, String ignoreUri) {
        if (grabber.cancelled) return true;

        if (grabber.prefix == null || grabber.nsuri == null ||
            grabber.prefix.length() < 1 || grabber.nsuri.length() < 1) {
            JOptionPane.showMessageDialog(this, "The prefix and namespace URI must both be specified");
            return true;
        }

        if (namespaceMap.keySet().contains(grabber.prefix) && !grabber.prefix.equals(ignorePrefix)) {
            JOptionPane.showMessageDialog(this, "The prefix " + grabber.prefix + " is already specified.");
            return true;
        } else if (grabber.prefix.indexOf("=") >= 0 ||
                   grabber.prefix.indexOf("/") >= 0 ||
                   grabber.prefix.indexOf("<") >= 0 ||
                   grabber.prefix.indexOf(">") >= 0 ||
                   grabber.prefix.indexOf(":") >= 0 ||
                   grabber.prefix.indexOf("?") >= 0 ||
                   grabber.prefix.indexOf("!") >= 0 ||
                   grabber.prefix.indexOf("\"") >= 0) {
            JOptionPane.showMessageDialog(this, grabber.prefix + " is not a valid namespace prefix value.");
            return true;
        }

        if (namespaceMap.values().contains(grabber.nsuri) && !grabber.nsuri.equals(ignoreUri)) {
            JOptionPane.showMessageDialog(this, "The namespace " + grabber.nsuri + " is already specified.");
            return true;
        } else if (grabber.nsuri.indexOf("\'") >= 0 ||
                   grabber.nsuri.indexOf("\"") >= 0) {
            JOptionPane.showMessageDialog(this, grabber.nsuri + " is not a valid namespace value.");
            return true;
        }
        return false;
    }

    private void remove() {
        int selectedRow = namespacesTable.getSelectedRow();
        if (selectedRow >= 0) {
            //noinspection SuspiciousMethodCalls
            namespaceMap.remove(namespacesTable.getModel().getValueAt(selectedRow, 0));
            ((AbstractTableModel) namespacesTable.getModel()).fireTableRowsDeleted(selectedRow, selectedRow);
        }
        if ((selectedRow - 1) >= 0) {
            --selectedRow;
        } else {
            if (namespacesTable.getModel().getRowCount() > 0) selectedRow = 0;
            else selectedRow = -1;
        }
        if (selectedRow >= 0) {
            namespacesTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        }
        enableRemoveBasedOnSelection();
    }

    private void ok() {
        NamespaceMapEditor.this.dispose();
    }

    private void cancel() {
        cancelled = true;
        NamespaceMapEditor.this.dispose();
    }

    private void help() {
        Actions.invokeHelp(NamespaceMapEditor.this);
    }

    public static void main(String[] args) {
        // test the dlg
        Map<String, String> initialValues = new HashMap<String, String>();
        initialValues.put("sp", "http://schemas.xmlsoap.org/soap/envelope/");
        initialValues.put("ns1", "http://warehouse.acme.com/ws");
        initialValues.put("acme", "http://ns.acme.com");
        initialValues.put("77", "http://77.acme.com");
        Map<String, String> forbiddenNamespaces = new HashMap<String, String>();
        forbiddenNamespaces.put("soap","http://schemas.xmlsoap.org/soap/envelope/");
        forbiddenNamespaces.put("targetNamespace", "http://warehouse.acme.com/ws");
        NamespaceMapEditor blah = new NamespaceMapEditor((Frame)null, initialValues, forbiddenNamespaces);
        blah.pack();
        blah.setVisible(true);
        System.exit(0);
    }
}
