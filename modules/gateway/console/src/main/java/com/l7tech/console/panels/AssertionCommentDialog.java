package com.l7tech.console.panels;

import com.l7tech.console.util.SsmPreferences;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.awt.event.*;

public class AssertionCommentDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField leftCommentTextArea;
    private JTextArea rightCommentTextArea;
    private boolean confirmed;
    private final Assertion.Comment assertionComment;

    public AssertionCommentDialog(Assertion.Comment assertionComment) {
        this.assertionComment = assertionComment;
        setTitle("Enter Comment");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        leftCommentTextArea.setDocument(new MaxLengthDocument(SsmPreferences.MAX_LEFT_COMMENT_SIZE));
        rightCommentTextArea.setDocument(new MaxLengthDocument(SsmPreferences.MAX_RIGHT_COMMENT_SIZE));

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        modelToView();
    }

    private void modelToView(){
        leftCommentTextArea.setText(assertionComment.getAssertionComment(Assertion.Comment.LEFT_COMMENT));
        leftCommentTextArea.setCaretPosition(0);

        rightCommentTextArea.setText(assertionComment.getAssertionComment(Assertion.Comment.RIGHT_COMMENT));
        rightCommentTextArea.setCaretPosition(0);
    }
    public boolean isConfirmed() {
        return confirmed;
    }

    public void viewToModel(){
        assertionComment.setComment(leftCommentTextArea.getText(), Assertion.Comment.LEFT_COMMENT);
        assertionComment.setComment(rightCommentTextArea.getText(), Assertion.Comment.RIGHT_COMMENT);
    }

    private void onOK() {
        confirmed = true;
        viewToModel();
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static void main(String[] args) {
        AssertionCommentDialog dialog = new AssertionCommentDialog(null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
