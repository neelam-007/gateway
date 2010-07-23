package com.l7tech.console.panels;

import com.l7tech.console.util.SsmPreferences;
import com.l7tech.gui.util.DialogDisplayer;
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

    public boolean viewToModel(){
        final String leftComment = leftCommentTextArea.getText();
        if(leftComment.length() > SsmPreferences.MAX_LEFT_COMMENT_SIZE) {
            DialogDisplayer.showMessageDialog(this, "Left comment too large",
                    "The maximum size of a left comment must be between 0 and " +
                            SsmPreferences.MAX_LEFT_COMMENT_SIZE + " characters.\n Current size is " + leftComment.length()+" characters.", null);
            return false;
        }
        assertionComment.setComment(leftComment, Assertion.Comment.LEFT_COMMENT);

        final String rightComment = rightCommentTextArea.getText();
        if(rightComment.length() > SsmPreferences.MAX_RIGHT_COMMENT_SIZE) {
            DialogDisplayer.showMessageDialog(this, "Right comment too large",
                    "The maximum size of a right comment must be between 0 and " +
                            SsmPreferences.MAX_RIGHT_COMMENT_SIZE + " characters.\n Current size is " + rightComment.length()+" characters.", null);
            return false;
        }
        assertionComment.setComment(rightComment, Assertion.Comment.RIGHT_COMMENT);

        return true;
    }

    private void onOK() {
        confirmed = true;
        if(viewToModel()){
            dispose();
        }
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
