package com.l7tech.external.assertions.demostrategy.console;

import com.l7tech.common.io.failover.FailoverStrategyEditor;
import com.l7tech.external.assertions.demostrategy.DemoStrategy;
import com.l7tech.external.assertions.demostrategy.DemoStrategyAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

public class DemoStrategyEditor extends FailoverStrategyEditor {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea propertiesTextArea;

    public DemoStrategyEditor(Frame frame, Map<String, String> prop) {
        super(frame, prop);
        setContentPane(contentPane);
        setModal(true);
        setTitle("Type \"true\" to return no route");
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

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        propertiesTextArea.setText(properties.get(DemoStrategy.RETURN_NO_ROUTE));
    }

    private void onOK() {
        properties.put(DemoStrategy.RETURN_NO_ROUTE, propertiesTextArea.getText());
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
