package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A dialog that lets the administrator specify a list of namespaces with corresponding
 * prefix in a Map for the XPath assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 16, 2004<br/>
 * $Id$
 */
public class NamespaceMapEditor extends JDialog {

    private JPanel mainPanel;
    private JTable table1;
    private JButton removeButton;
    private JButton addButton;
    private JButton okButton;
    private JButton helpButton;
    private JButton cancelButton;

    private java.util.List prefixes = new ArrayList();
    private java.util.List namespaces = new ArrayList();
    boolean cancelled = false;

    public NamespaceMapEditor(Dialog owner, Map predefinedNamespaces) {
        super(owner, true);
        if (predefinedNamespaces != null) {
            prefixes.addAll(predefinedNamespaces.keySet());
            namespaces.addAll(predefinedNamespaces.values());
        }
        initialize();
    }

    public NamespaceMapEditor(Frame owner, Map predefinedNamespaces) {
        super(owner, true);
        if (predefinedNamespaces != null) {
            prefixes.addAll(predefinedNamespaces.keySet());
            namespaces.addAll(predefinedNamespaces.values());
        }
        initialize();
    }

    /**
     * @return null if the dialog was canceled, otherwise a map with the namespaces.
     */
    public Map newNSMap() {
        if (cancelled) return null;
        Map output = new HashMap();
        for (int i = 0; i < prefixes.size(); i++) {
            output.put(prefixes.get(i), namespaces.get(i));
        }
        return output;
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Edit namespaces and prefixes");
        setTableModel();
        setButtonListeners();
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
        table1.setModel(model);
    }

    private void setButtonListeners() {
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
    }

    private void add() {
        int selectedRow = table1.getSelectedRow();

        NamespacePrefixQueryForm grabber = new NamespacePrefixQueryForm(this);
        grabber.pack();
        grabber.show();

        if (grabber.cancelled) return;

        if (grabber.prefix == null || grabber.nsuri == null) {
            JOptionPane.showMessageDialog(addButton, "The prefix and namespace URI must both be specified");
            return;
        }

        if (prefixes.contains(grabber.prefix)) {
            JOptionPane.showMessageDialog(addButton, "The prefix " + grabber.prefix + " is already specified.");
            return;
        } else if (grabber.prefix.indexOf("=") >= 0 ||
                   grabber.prefix.indexOf("/") >= 0 ||
                   grabber.prefix.indexOf("<") >= 0 ||
                   grabber.prefix.indexOf(">") >= 0 ||
                   grabber.prefix.indexOf(":") >= 0 ||
                   grabber.prefix.indexOf("?") >= 0 ||
                   grabber.prefix.indexOf("!") >= 0 ||
                   grabber.prefix.indexOf("\"") >= 0) {
            JOptionPane.showMessageDialog(addButton, grabber.prefix + " is not a valid namespace prefix value.");
            return;
        }

        if (namespaces.contains(grabber.nsuri)) {
            JOptionPane.showMessageDialog(addButton, "The namespace " + grabber.nsuri + " is already specified.");
            return;
        } else if (grabber.nsuri.indexOf("\'") >= 0 ||
                   grabber.nsuri.indexOf("\"") >= 0) {
            JOptionPane.showMessageDialog(addButton, grabber.nsuri + " is not a valid namespace value.");
            return;
        }
        prefixes.add(grabber.prefix);
        namespaces.add(grabber.nsuri);

        setTableModel();
        if (selectedRow == -1 && table1.getModel().getRowCount() == 1) selectedRow = 0;
        if (selectedRow >= 0) {
            table1.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        }
    }

    private void remove() {
        int selectedRow = table1.getSelectedRow();
        if (selectedRow >= 0) {
            prefixes.remove(selectedRow);
            namespaces.remove(selectedRow);

            setTableModel();
        }
        if ((selectedRow - 1) >= 0) {
            --selectedRow;
        } else {
            if (table1.getModel().getRowCount() > 0) selectedRow = 0;
            else selectedRow = -1;
        }
        if (selectedRow >= 0) {
            table1.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        }
    }

    private void ok() {
        NamespaceMapEditor.this.dispose();
    }

    private void cancel() {
        cancelled = true;
        NamespaceMapEditor.this.dispose();
    }

    private void help() {
        // todo
    }

    public static void main(String[] args) {
        // test the dlg
        NamespaceMapEditor blah = new NamespaceMapEditor((Frame)null, null);
        blah.pack();
        blah.show();
        System.exit(0);
    }
}
