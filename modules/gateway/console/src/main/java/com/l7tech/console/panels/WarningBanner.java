package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class WarningBanner extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JEditorPane warningMsgEditor;
    private JScrollPane warningMsgScrollPane;
    private Frame owner;

    private boolean okClicked;
    private boolean cancelClicked;
    private String warningMsg;


    public WarningBanner(Frame owner, String warningMsg) {
        super(owner, true);
        this.owner = owner;
        this.warningMsg = warningMsg;

        this.okClicked = false;
        this.cancelClicked = false;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okClicked = true;
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelClicked = true;
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

        //warningMsgEditor = new JEditorPane();
        warningMsgEditor.setText(warningMsg);
        warningMsgEditor.setPreferredSize(new Dimension(300, 100));
        warningMsgEditor.setMinimumSize(new Dimension(300, 100));
        warningMsgEditor.setVisible(true);
        warningMsgEditor.setEditable(false);
        

        //Utilities.centerOnParent(this);
        //Utilities.centerOnParentWindow(this);
        this.pack();
        this.setSize(600, 350);
        this.setTitle("Warning");
        this.setResizable(true);
        warningMsgEditor.setCaretPosition(0);
    }

}
