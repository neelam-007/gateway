package com.l7tech.console.table;

import com.l7tech.console.panels.HttpRuleDialog;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.awt.*;

/**
 * Common code for tables that contain customized http header/parameter rules for routing dialogs
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 5, 2007<br/>
 */
public abstract class HttpRuleTableHandler {
    private static final String MAGIC_DEF_VALUE = "<original value>";
    private String subject;
    final protected JTable table;
    final DefaultTableModel model;
    private JDialog parentDlg;
    boolean editable = true;
    private JButton editButton;

    public HttpRuleTableHandler(final String subject, final JTable table,
                                final JButton addButton, final JButton removeButton, final JButton editButton,
                                HttpPassthroughRuleSet data) {
        this.subject = subject;
        this.table = table;
        this.editButton = editButton;

        Utilities.enableGrayOnDisabled(table);

        table.setColumnSelectionAllowed(false);
        Object[][] rows = dataToRows(data);
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
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                updateeditState();
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
                HttpRuleDialog editor = new HttpRuleDialog(subject, parentDlg, null);
                editor.pack();
                Utilities.centerOnScreen(editor);
                editor.setVisible(true);
                if (editor.wasOKed()) {
                    HttpPassthroughRule dres = editor.getData();
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

    private Object[][] dataToRows(HttpPassthroughRuleSet data) {
        if (data.getRules() == null || data.getRules().length < 1) {
            return null;
        } else {
            Object[][] output = new Object[data.getRules().length][2];
            for (int i = 0; i < data.getRules().length; i++) {
                HttpPassthroughRule httpPassthroughRule = data.getRules()[i];
                output[i][0] = httpPassthroughRule.getName();
                if (httpPassthroughRule.isUsesCustomizedValue()) {
                    output[i][1] = httpPassthroughRule.getCustomizeValue();
                } else {
                    output[i][1] = MAGIC_DEF_VALUE;
                }
            }
            return output;
        }
    }

    private String[] dataToRow(HttpPassthroughRule data) {
        String val = data.getCustomizeValue();
        if (!data.isUsesCustomizedValue()) {
            val = MAGIC_DEF_VALUE;
        }
        return new String[] {data.getName(), val};
    }

    private HttpPassthroughRule rowToData(int row) {
        HttpPassthroughRule output = new HttpPassthroughRule();
        output.setName((String)model.getValueAt(row, 0));
        String val = (String)model.getValueAt(row, 1);
        if (MAGIC_DEF_VALUE.equals(val)) {
            output.setUsesCustomizedValue(false);
        } else {
            output.setUsesCustomizedValue(true);
            output.setCustomizeValue(val);
        }
        return output;
    }

    public HttpPassthroughRule[] getData() {
        HttpPassthroughRule[] output = new HttpPassthroughRule[model.getRowCount()];
        for (int i = 0; i < output.length; i++) {
            output[i] = rowToData(i);
        }
        return output;
    }

    private void removeLines() {
        int[] selectedrows = table.getSelectedRows();
        if (selectedrows != null && selectedrows.length > 0) {
            for (int i = selectedrows.length-1; i >= 0; i--) {
                model.removeRow(selectedrows[i]);
            }
        }
    }

    protected abstract boolean validateNewRule(HttpPassthroughRule in);

    private void editRuleRow() {
        if (!editable) return;
        int[] selectedrows = table.getSelectedRows();
        if (selectedrows != null && selectedrows.length > 0) {
            HttpPassthroughRule toedit = rowToData(selectedrows[0]);

            HttpRuleDialog editor = new HttpRuleDialog(subject, parentDlg, toedit);
            Utilities.centerOnScreen(editor);
            editor.pack();
            editor.setVisible(true);
            if (editor.wasOKed()) {
                HttpPassthroughRule dres = editor.getData();
                if (validateNewRule(dres)) {
                    String[] res = dataToRow(dres);
                    model.setValueAt(res[0], selectedrows[0], 0);
                    model.setValueAt(res[1], selectedrows[0], 1);
                }
            }
        }
    }
}
