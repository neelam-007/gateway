package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.TableUtil;
import com.l7tech.common.gui.util.DialogDisplayer;

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
    private JButton addButton;
    private JButton okButton;
    private JButton helpButton;
    private JButton cancelButton;

    private java.util.List<String> prefixes = new ArrayList<String>();
    private java.util.List<String> namespaces = new ArrayList<String>();
    private java.util.List<String> forbiddenNamespaces = new ArrayList<String>();
    boolean cancelled = false;

    /**
     * Construct a dialog that lets the user edit a map of namespace/prefixes
     * @param owner the requesting dlg owner
     * @param predefinedNamespaces optional, the initial namespace map (key is prefix, value is uri)
     * @param forcedNamespaces optional, the namespace values that cannot be removed nor changed
     */
    public NamespaceMapEditor(Dialog owner, Map<String, String> predefinedNamespaces, java.util.List<String> forcedNamespaces) {
        super(owner, true);
        initialize(predefinedNamespaces, forcedNamespaces);
    }

    /**
     * Construct a dialog that lets the user edit a map of namespace/prefixes
     * @param owner the requesting frame owner
     * @param predefinedNamespaces optional, the initial namespace map (key is prefix, value is uri)
     * @param forcedNamespaces optional, the namespace values that cannot be removed nor changed
     */
    public NamespaceMapEditor(Frame owner, Map<String, String> predefinedNamespaces, java.util.List<String> forcedNamespaces) {
        super(owner, true);
        initialize(predefinedNamespaces, forcedNamespaces);
    }

    /**
     * @return null if the dialog was canceled, otherwise a map with the namespaces.
     */
    public Map<String, String> newNSMap() {
        if (cancelled) return null;
        Map<String, String> output = new HashMap<String, String>();
        for (int i = 0; i < prefixes.size(); i++) {
            output.put(prefixes.get(i), namespaces.get(i));
        }
        return output;
    }

    private void initialize(Map<String, String> predefinedNamespaces, java.util.List<String> forcedNamespaces) {
        if (predefinedNamespaces != null) {
            prefixes.addAll(predefinedNamespaces.keySet());
            namespaces.addAll(predefinedNamespaces.values());
        }
        forbiddenNamespaces = forcedNamespaces;
        setContentPane(mainPanel);
        setTitle("Edit Namespaces and Prefixes");
        setTableModel();
        TableUtil.adjustColumnWidth(namespacesTable, 1);
        setListeners();
        enableRemoveBasedOnSelection();
    }

    private void setTableModel() {
        TableModel model = new AbstractTableModel() {
            public int getColumnCount() {
                return 2;
            }
            public int getRowCount() {
                return prefixes.size();
            }
            public Object getValueAt(int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0: return prefixes.get(rowIndex);
                    case 1: return namespaces.get(rowIndex);
                }
                return "";
            }
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
                    String ns = (String) namespacesTable.getModel().getValueAt(row, 1);
                    if (forbiddenNamespaces.contains(ns)) {
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
        }
    }

    private void setListeners() {
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                remove();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        namespacesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableRemoveBasedOnSelection();
            }
        });

        // implement default behavior for esc and enter keys
        KeyListener defBehaviorKeyListener = new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ok();
                }
            }
            public void keyTyped(KeyEvent e) {}
        };
        namespacesTable.addKeyListener(defBehaviorKeyListener);
        namespacesTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    edit();
            }
        });
        removeButton.addKeyListener(defBehaviorKeyListener);
        addButton.addKeyListener(defBehaviorKeyListener);
        okButton.addKeyListener(defBehaviorKeyListener);
        helpButton.addKeyListener(defBehaviorKeyListener);
        cancelButton.addKeyListener(defBehaviorKeyListener);
    }

    private void enableRemoveBasedOnSelection() {
        // get new selection
        int selectedRow = namespacesTable.getSelectedRow();
        // decide whether or not the remove button should be enabled
        if (selectedRow < 0) {
            removeButton.setEnabled(false);
            return;
        }
        String selectedNSURI = namespaces.get(selectedRow);
        removeButton.setEnabled(isEditableNamespace(selectedNSURI));
    }

    private boolean isEditableNamespace(String selectedNSURI) {
        return !(forbiddenNamespaces != null && forbiddenNamespaces.contains(selectedNSURI));
    }

    private void edit() {
        final int[] selectedRow = new int[] {namespacesTable.getSelectedRow()};
        final String originalPrefix = (String) namespacesTable.getModel().getValueAt(selectedRow[0], 0);
        final String originalNsURI = (String) namespacesTable.getModel().getValueAt(selectedRow[0], 1);
        if (!isEditableNamespace(originalNsURI))
            return;

        final NamespacePrefixQueryForm grabber = new NamespacePrefixQueryForm(this, "Edit XML Namespace and Prefix");
        grabber.setInitialPrefix(originalPrefix);
        grabber.setInitialNsUri(originalNsURI);
        grabber.pack();
        Utilities.centerOnScreen(grabber);        
        DialogDisplayer.display(grabber, new Runnable() {
            public void run() {
                if (isInvalidNamespaceEntry(grabber, false)) return;
                prefixes.remove(originalPrefix) ;
                namespaces.remove(originalNsURI);

                prefixes.add(grabber.prefix);
                namespaces.add(grabber.nsuri);
                updateTable(selectedRow);
            }
        });
    }

    private void add() {
        final int[] selectedRow = new int[]{namespacesTable.getSelectedRow()};

        final NamespacePrefixQueryForm grabber = new NamespacePrefixQueryForm(this);
        grabber.pack();
        Utilities.centerOnScreen(grabber);
        DialogDisplayer.display(grabber, new Runnable() {
            public void run() {
                if (isInvalidNamespaceEntry(grabber, true))
                    return;

                prefixes.add(grabber.prefix);
                namespaces.add(grabber.nsuri);
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

    private boolean isInvalidNamespaceEntry(NamespacePrefixQueryForm grabber, boolean isNewEntry) {
        if (grabber.cancelled) return true;

        if (grabber.prefix == null || grabber.nsuri == null ||
            grabber.prefix.length() < 1 || grabber.nsuri.length() < 1) {
            JOptionPane.showMessageDialog(addButton, "The prefix and namespace URI must both be specified");
            return true;
        }

        if (prefixes.contains(grabber.prefix) && isNewEntry) {
            JOptionPane.showMessageDialog(addButton, "The prefix " + grabber.prefix + " is already specified.");
            return true;
        } else if (grabber.prefix.indexOf("=") >= 0 ||
                   grabber.prefix.indexOf("/") >= 0 ||
                   grabber.prefix.indexOf("<") >= 0 ||
                   grabber.prefix.indexOf(">") >= 0 ||
                   grabber.prefix.indexOf(":") >= 0 ||
                   grabber.prefix.indexOf("?") >= 0 ||
                   grabber.prefix.indexOf("!") >= 0 ||
                   grabber.prefix.indexOf("\"") >= 0) {
            JOptionPane.showMessageDialog(addButton, grabber.prefix + " is not a valid namespace prefix value.");
            return true;
        }

        if (namespaces.contains(grabber.nsuri) && isNewEntry) {
            JOptionPane.showMessageDialog(addButton, "The namespace " + grabber.nsuri + " is already specified.");
            return true;
        } else if (grabber.nsuri.indexOf("\'") >= 0 ||
                   grabber.nsuri.indexOf("\"") >= 0) {
            JOptionPane.showMessageDialog(addButton, grabber.nsuri + " is not a valid namespace value.");
            return true;
        }
        return false;
    }

    private void remove() {
        int selectedRow = namespacesTable.getSelectedRow();
        if (selectedRow >= 0) {
            prefixes.remove(selectedRow);
            namespaces.remove(selectedRow);
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
        java.util.List<String> forbiddenNamespaces = new ArrayList<String>();
        forbiddenNamespaces.add("http://schemas.xmlsoap.org/soap/envelope/");
        forbiddenNamespaces.add("http://warehouse.acme.com/ws");
        NamespaceMapEditor blah = new NamespaceMapEditor((Frame)null, initialValues, forbiddenNamespaces);
        blah.pack();
        blah.setVisible(true);
        System.exit(0);
    }
}
