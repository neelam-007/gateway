/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.common.gui.util.TableUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * A dialog to view/edit/add/remove cluster-wide properties
 *
 * @author flascelles@layer7-tech.com
 */
public class ClusterPropertyDialog extends JDialog {
    private JPanel mainPanel;
    private JTable propsTable;
    private JButton removeButton;
    private JButton editButton;
    private JButton addButton;
    private JButton helpButton;
    private JButton closeButton;
    private final ArrayList properties = new ArrayList();

    public ClusterPropertyDialog(Frame owner) {
        super(owner, true);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Manage Cluster-Wide Properties");

        TableModel model = new AbstractTableModel() {
            public int getColumnCount() {
                return 2;
            }
            public int getRowCount() {
                return properties.size();
            }
            public Object getValueAt(int rowIndex, int columnIndex) {
                ClusterProperty entry = (ClusterProperty)properties.get(rowIndex);
                switch (columnIndex) {
                    case 0: return entry.getKey();
                    case 1: return entry.getValue();
                    default: throw new RuntimeException("column index not supported " + columnIndex);
                }
            }
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return "Key";
                    case 1: return "Value";
                }
                return "";
            }
        };
        propsTable.setModel(model);
        setListeners();

        // support Enter and Esc keys
        Actions.setEscKeyStrokeDisposes(this);
        Actions.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();

            }
        });
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        populate();
        enableRemoveBasedOnSelection();
    }

    private void close() {
        dispose();
    }

    private void setListeners() {
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                remove();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        propsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableRemoveBasedOnSelection();
            }
        });
        propsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    edit();
            }
        });
        propsTable.addKeyListener(new KeyListener () {
            public void keyPressed(KeyEvent e) {
                edit();
            }
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
        });
    }

    private void add() {
        // todo
    }

    private void edit() {
        // todo
    }

    private void remove() {
        // todo
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void enableRemoveBasedOnSelection() {
        // get new selection
        int selectedRow = propsTable.getSelectedRow();
        // decide whether or not the remove button should be enabled
        if (selectedRow < 0) {
            removeButton.setEnabled(false);
            editButton.setEnabled(false);
        } else {
            removeButton.setEnabled(true);
            editButton.setEnabled(true);
        }
    }

    private void populate() {
        Registry reg = Registry.getDefault();
        // todo
    }

    public void show() {
        TableUtil.adjustColumnWidth(propsTable, 1);
        super.show();
    }

    public static void main(String[] args) {
        ClusterPropertyDialog me = new ClusterPropertyDialog((Frame)null);
        ClusterProperty sample = new ClusterProperty();
        sample.setKey("com.l7tech.gateway.enablePhotonSequencer");
        sample.setValue("true");
        me.properties.add(sample);
        sample = new ClusterProperty();
        sample.setKey("com.l7tech.gateway.reloadTargetOnHBXTrigger");
        sample.setValue("false");
        me.properties.add(sample);
        me.pack();
        me.show();
        System.exit(0);
    }
}
