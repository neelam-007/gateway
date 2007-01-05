package com.l7tech.console.table;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;

/**
 * Common code for tables that contain customized http header/parameter rules for routing dialogs
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 5, 2007<br/>
 */
public class HttpRuleTableHandler {
    private String subject;
    final private JTable table;
    private JButton adButton;
    private JButton removeButton;
    final DefaultTableModel model;

    public HttpRuleTableHandler(String subject, final JTable table, final JButton addButton, final JButton removeButton) {
        this.subject = subject;
        this.table = table;
        this.adButton = addButton;
        this.removeButton = removeButton;

        table.setColumnSelectionAllowed(false);
        Object[][] rows = new Object[][]{};
        String[] columns = new String[]{subject + " name", subject + " value"};
        model = new DefaultTableModel(rows, columns) {
            public boolean isCellEditable(int a, int b) {return false;}
        };
        table.setModel(model);

        table.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    editRuleRow();
                }
            }
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    editRuleRow();
            }
        });
        /*table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int[] selectedrows = table.getSelectedRows();
                if (selectedrows != null && selectedrows.length > 0) {
                    removeButton.setEnabled(true);
                } else {
                    removeButton.setEnabled(false);
                }
            }
        });*/

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                model.addRow(new String[]{"blah", "foo"});
                // todo, a dialog to add/edit those lines
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                removeLines();
            }
        });
    }

    public void populateDate() {
        // todo, receive data in a yet to be determined format
    }

    private void removeLines() {
        int[] selectedrows = table.getSelectedRows();
        if (selectedrows != null && selectedrows.length > 0) {
            for (int i = selectedrows.length-1; i >= 0; i--) {
                model.removeRow(selectedrows[i]);
            }
        }
    }

    private void editRuleRow() {
        int[] selectedrows = table.getSelectedRows();
        if (selectedrows != null && selectedrows.length > 0) {
            JOptionPane.showInputDialog("todo editing column " + selectedrows[0]);
            // todo invoke the dialog responsible for editing this annoyance
        }
    }
}
