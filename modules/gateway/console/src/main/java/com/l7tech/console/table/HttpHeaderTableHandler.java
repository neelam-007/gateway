package com.l7tech.console.table;

import com.l7tech.console.panels.resources.HttpHeaderDialog;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpConfigurationProperty;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Common code for tables that contain customized http header/parameter rules for routing dialogs
 */
public class HttpHeaderTableHandler {
    private static final String HEADER = "Header";
    final protected JTable table;
    final DefaultTableModel model;
    private JDialog parentDlg;
    private boolean editable = true;
    private JButton editButton;

    public HttpHeaderTableHandler(final JTable table,
                                  final JButton addButton, final JButton removeButton, final JButton editButton,
                                  final HttpConfiguration httpConfiguration) {
        Set<HttpConfigurationProperty> headers = httpConfiguration.getHttpConfigurationProperties();
        this.table = table;
        this.editButton = editButton;

        Utilities.enableGrayOnDisabled(table);

        table.setColumnSelectionAllowed(false);
        Object[][] rows = dataToRows(headers);
        String[] columns = new String[]{HEADER + " Name", HEADER + " Value"};
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
        table.getSelectionModel().addListSelectionListener( listSelectionEvent -> updateeditState());

        addButton.addActionListener(actionEvent -> {
            HttpHeaderDialog editor = new HttpHeaderDialog(parentDlg, null);
            editor.pack();
            Utilities.centerOnScreen(editor);
            editor.setVisible(true);
            if (editor.wasOKed()) {
                HttpConfigurationProperty dres = editor.getData();
                model.addRow(dataToRow(dres));
            }
        });

        removeButton.addActionListener( actionEvent -> removeLines());

        editButton.addActionListener(actionEvent -> editRuleRow());

        Container c = table.getParent();
        while (c != null) {
            if (c instanceof JDialog) {
                parentDlg = (JDialog)c;
                break;
            }
            c = c.getParent();
        }
    }

    private void updateeditState() {
        boolean selection = false;
        if (table.getSelectedRows() != null && table.getSelectedRows().length > 0) {
            selection = true;
        }
        editButton.setEnabled(selection);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        table.setEnabled(editable);
    }

    private Object[][] dataToRows(Set<HttpConfigurationProperty> headers) {
        if (headers == null || headers.size() < 1) {
            return null;
        } else {
            Object[][] output = new Object[headers.size()][2];
            Iterator iterator = headers.iterator();
            int counter = 0;
            while (iterator.hasNext()){
                HttpConfigurationProperty header = (HttpConfigurationProperty)iterator.next();
                output[counter][0] = header.getName();
                output[counter][1] = header.getFullValue();
                counter++;
            }
            return output;
        }
    }

    private String[] dataToRow(HttpConfigurationProperty data) {
        String val = data.getFullValue();
        return new String[] {data.getName(), val};
    }

    private HttpConfigurationProperty rowToData(int row) {
        HttpConfigurationProperty output = new HttpConfigurationProperty();
        output.setName((String)model.getValueAt(row, 0));
        String val = (String)model.getValueAt(row, 1);
        output.setFullValue(val);
        return output;
    }

    public Set<HttpConfigurationProperty> getData() {
        int rowCount = model.getRowCount();
        Set<HttpConfigurationProperty> httpConfigurationProperties = new HashSet<>();
        for (int i = 0; i < rowCount; i++) {
            boolean added = httpConfigurationProperties.add(rowToData(i));
            System.out.println(added);
        }
        return httpConfigurationProperties;
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
        if (!editable) return;
        int[] selectedrows = table.getSelectedRows();
        if (selectedrows != null && selectedrows.length > 0) {
            HttpConfigurationProperty toedit = rowToData(selectedrows[0]);

            HttpHeaderDialog editor = new HttpHeaderDialog(parentDlg, toedit);
            Utilities.centerOnScreen(editor);
            editor.pack();
            editor.setVisible(true);
            if (editor.wasOKed()) {
                HttpConfigurationProperty dres = editor.getData();
                String[] res = dataToRow(dres);
                model.setValueAt(res[0], selectedrows[0], 0);
                model.setValueAt(res[1], selectedrows[0], 1);
            }
        }
    }

    public JTable getTable() {
        return table;
    }

    public DefaultTableModel getModel() {
        return model;
    }
}
