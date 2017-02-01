package com.l7tech.console.table;

import com.l7tech.console.panels.resources.HttpHeaderDialog;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.resources.HttpHeader;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Common code for tables that contain customized http header/parameter rules for routing dialogs
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: Ekta<br/>
 * Date: June 20, 2016<br/>
 */
public class HttpHeaderTableHandler {
    private static final String MAGIC_DEF_VALUE = "<original value>";
    private static final String HEADER = "Header";
    final protected JTable table;
    final DefaultTableModel model;
    private JDialog parentDlg;
    boolean editable = true;
    private JButton editButton;
    private HttpConfiguration httpConfiguration;


    public HttpHeaderTableHandler(final JTable table,
                                  final JButton addButton, final JButton removeButton, final JButton editButton,
                                  final HttpConfiguration httpConfiguration) {
        Set<HttpHeader> headers = httpConfiguration.getHeaders();
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
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                updateeditState();
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                HttpHeaderDialog editor = new HttpHeaderDialog(parentDlg, null);
                editor.pack();
                Utilities.centerOnScreen(editor);
                editor.setVisible(true);
                if (editor.wasOKed()) {
                    HttpHeader dres = editor.getData();
                    if (validateNewRule(dres)) {
                        model.addRow(dataToRow(dres));
                    }
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                removeLines();
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                editRuleRow();
            }
        });

        Container c = table.getParent();
        while (c != null) {
            if (c instanceof JDialog) {
                parentDlg = (JDialog)c;
                break;
            }
            c = c.getParent();
        }
    }

    public void updateeditState() {
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

    private Object[][] dataToRows(Set<HttpHeader> headers) {
        if (headers == null || headers.size() < 1) {
            return null;
        } else {
            Object[][] output = new Object[headers.size()][2];
            Iterator iterator = headers.iterator();
            int counter = 0;
            while (iterator.hasNext()){
                HttpHeader header = (HttpHeader)iterator.next();
                output[counter][0] = header.getName();
                output[counter][1] = header.getFullValue();
                counter++;
            }
            return output;
        }
    }

    private String[] dataToRow(HttpHeader data) {
        String val = data.getFullValue();
        return new String[] {data.getName(), val};
    }

    private HttpHeader rowToData(int row) {
        HttpHeader output = new HttpHeader();
        output.setName((String)model.getValueAt(row, 0));
        String val = (String)model.getValueAt(row, 1);
        output.setFullValue(val);
        return output;
    }

    public Set<HttpHeader> getData() {
        int rowCount = model.getRowCount();
        Set<HttpHeader> httpHeaders = new HashSet<HttpHeader>();
        for (int i = 0; i < rowCount; i++) {
            boolean added = httpHeaders.add(rowToData(i));
            System.out.println(added);
        }
        return httpHeaders;
    }

    private void removeLines() {
        int[] selectedrows = table.getSelectedRows();
        if (selectedrows != null && selectedrows.length > 0) {
            for (int i = selectedrows.length-1; i >= 0; i--) {
                model.removeRow(selectedrows[i]);
            }
        }
    }

    protected boolean validateNewRule(HttpHeader in) {
        return true;
    }

    private void editRuleRow() {
        if (!editable) return;
        int[] selectedrows = table.getSelectedRows();
        if (selectedrows != null && selectedrows.length > 0) {
            HttpHeader toedit = rowToData(selectedrows[0]);

            HttpHeaderDialog editor = new HttpHeaderDialog(parentDlg, toedit);
            Utilities.centerOnScreen(editor);
            editor.pack();
            editor.setVisible(true);
            if (editor.wasOKed()) {
                HttpHeader dres = editor.getData();
                if (validateNewRule(dres)) {
                    String[] res = dataToRow(dres);
                    model.setValueAt(res[0], selectedrows[0], 0);
                    model.setValueAt(res[1], selectedrows[0], 1);
                }
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
