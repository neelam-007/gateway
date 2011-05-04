package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.event.*;
import java.awt.*;

public class WarningBanner extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JEditorPane warningMsgEditor;

    private boolean okClicked;
    private boolean cancelClicked;
    private String warningMsg;


    public WarningBanner(Frame owner, String warningMsg) {
        super(owner, true);
        this.warningMsg = warningMsg;

        this.okClicked = false;
        this.cancelClicked = false;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okClicked = true;
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelClicked = true;
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        createUIComponents();
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public boolean isCancelClicked() {
        return cancelClicked;
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        TopComponents tc = TopComponents.getInstance();
        if ( !tc.isDisconnected() ) {
            tc.disconnectFromGateway();
        }
        dispose();
    }

    public static void main(String[] args) {
        WarningBanner dialog = new WarningBanner(null, "TEST");

        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    private void createUIComponents() {
        warningMsgEditor = new JEditorPane(){
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        warningMsgEditor.setText( warningMsg );

        this.pack();
        this.setSize(600, 350);
        this.setTitle("Warning");
        this.setResizable(true);
        warningMsgEditor.setCaretPosition(0);
    }

}
