package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.awt.event.*;
import java.util.Map;

public class AssertionCommentDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea commentTextArea;
    private JComboBox alignComboBox;
    private boolean confirmed;
    private final Assertion.Comment assertionComment;

    public AssertionCommentDialog(Assertion.Comment assertionComment) {
        this.assertionComment = assertionComment;
        setTitle("Enter Comment");
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

        modelToView();
    }

    private void modelToView(){
        final Map<String, String> map = assertionComment.getProperties();
        final String style = map.get(Assertion.Comment.COMMENT_ALIGN);
        if (style != null && style.equals(Assertion.Comment.LEFT_ALIGN)) {
            alignComboBox.setSelectedItem(Assertion.Comment.LEFT_ALIGN);
        } else {
            alignComboBox.setSelectedItem(Assertion.Comment.RIGHT_ALIGN);
        }

        commentTextArea.setText(assertionComment.getComment());
        commentTextArea.setCaretPosition(0);
    }
    public boolean isConfirmed() {
        return confirmed;
    }

    public void viewToModel(){
        final String text = commentTextArea.getText();

        assertionComment.setComment(text);
        final Map<String,String> props = assertionComment.getProperties();
        final String alignment = alignComboBox.getSelectedItem().equals(Assertion.Comment.RIGHT_ALIGN)? Assertion.Comment.RIGHT_ALIGN : Assertion.Comment.LEFT_ALIGN;
        props.put(Assertion.Comment.COMMENT_ALIGN, alignment);
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

    //Called automatically before body of constructor runs, but not before the constructor
    private void createUIComponents() {
        alignComboBox = new JComboBox(new Object[]{Assertion.Comment.RIGHT_ALIGN, Assertion.Comment.LEFT_ALIGN}); 
    }
}
