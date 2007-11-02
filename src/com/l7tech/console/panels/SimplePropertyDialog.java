package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Simple edit dialog for string key,value pairs.
 */
public class SimplePropertyDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField keyField;
    private JTextField valueField;

    private boolean confirmed = false;

    public SimplePropertyDialog(Window owner) {
        super(owner, "New Property");
        keyField.setEnabled(true);
        init();
    }

    public SimplePropertyDialog(Window owner, Pair<String,String> property) {
        super(owner, "Edit Property");
        keyField.setEnabled(false);
        init();
        setData(property);
    }

    private void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        Utilities.attachDefaultContextMenu(keyField);
        Utilities.attachDefaultContextMenu(valueField);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(Pair<String, String> data) {
        keyField.setText(data.left);
        valueField.setText(data.right);
    }

    public Pair<String, String> getData() {
        return new Pair<String, String>(keyField.getText(), valueField.getText());
    }
}
