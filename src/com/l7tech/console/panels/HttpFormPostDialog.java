package com.l7tech.console.panels;

import com.l7tech.policy.assertion.HttpFormPost;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * Used for editing the {@link com.l7tech.policy.assertion.HttpFormPost} assertion.
 */
public class HttpFormPostDialog extends JDialog {
    private JList fieldList;
    private JButton removeButton;
    private JButton modifyButton;
    private JButton addButton;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;

    private final HttpFormPost assertion;
    private final DefaultListModel fieldListModel;
    private boolean assertionModified = false;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private Frame ownerFrame;

    public HttpFormPostDialog(Frame owner, HttpFormPost ass) throws HeadlessException {
        super(owner, "HTTP Form POST Properties", true);
        this.ownerFrame = owner;
        this.assertion = ass;
        fieldListModel = new DefaultListModel();
        for (int i = 0; i < ass.getFieldInfos().length; i++) {
            HttpFormPost.FieldInfo info = ass.getFieldInfos()[i];
            fieldListModel.addElement(new FieldInfoListElement(info));
        }

        fieldList.setModel(fieldListModel);
        enableButtons();

        fieldList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    modify();
                }
            }
        });

        fieldList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assertionModified = false;
                dispose();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ArrayList infos = new ArrayList();
                for (int i = 0; i < fieldListModel.getSize(); i++) {
                    FieldInfoListElement element = (FieldInfoListElement) fieldListModel.elementAt(i);
                    infos.add(element.fieldInfo);
                }
                assertion.setFieldInfos((HttpFormPost.FieldInfo[])infos.toArray(new HttpFormPost.FieldInfo[0]));
                assertionModified = true;
                dispose();
            }
        });

        moveUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int pos = fieldList.getSelectedIndex();
                if (pos < 1) return;
                Object which = fieldListModel.elementAt(pos);
                fieldListModel.removeElementAt(pos);
                fieldListModel.insertElementAt(which, pos-1);
                fieldList.setSelectedValue(which, true);
                enableButtons();
            }
        });

        moveDownButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int pos = fieldList.getSelectedIndex();
                if (pos > fieldListModel.getSize() - 2) return;
                Object which = fieldListModel.elementAt(pos);
                fieldListModel.removeElementAt(pos);
                fieldListModel.insertElementAt(which, pos+1);
                fieldList.setSelectedValue(which, true);
                enableButtons();
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                HttpFormPost.FieldInfo fi = edit(new HttpFormPost.FieldInfo());
                if (fi != null) {
                    fieldListModel.addElement(new FieldInfoListElement(fi));
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fieldListModel.removeElement(fieldList.getSelectedValue());
            }
        });

        modifyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                modify();
            }
        });


        add(mainPanel);
    }

    private void modify() {
        FieldInfoListElement el = (FieldInfoListElement) fieldList.getSelectedValue();
        if (el == null) return;
        HttpFormPost.FieldInfo fi = el.fieldInfo;
        HttpFormPost.FieldInfo edited = edit(fi);
        if (edited != null) {
            el.fieldInfo = edited;
            fieldList.repaint(); // TODO this seems dumb but validate() doesn't cut it
        }
    }

    private HttpFormPost.FieldInfo edit(HttpFormPost.FieldInfo fi) {
        HttpPostFormFieldInfoDialog dlg = new HttpPostFormFieldInfoDialog(ownerFrame, fi);
        Utilities.centerOnScreen(dlg);
        dlg.pack();
        dlg.setVisible(true);
        if (dlg.isChanged())
            return dlg.getFieldInfo();
        else
            return null;
    }

    private void enableButtons() {
        boolean selected = fieldList.getSelectedValue() != null;
        removeButton.setEnabled(selected);
        moveUpButton.setEnabled(selected && fieldList.getSelectedIndex() > 0);
        moveDownButton.setEnabled(selected && fieldList.getSelectedIndex() <= fieldListModel.getSize() - 2);
        modifyButton.setEnabled(selected);
    }

    private class FieldInfoListElement {
        private HttpFormPost.FieldInfo fieldInfo;

        public FieldInfoListElement(HttpFormPost.FieldInfo fieldInfo) {
            this.fieldInfo = fieldInfo;
        }

        public String toString() {
            return (fieldListModel.indexOf(this) + 1) + ": " +
                   fieldInfo.getFieldname() + " (" + fieldInfo.getContentType() + ")";
        }
    }

    public boolean isAssertionModified() {
        return assertionModified;
    }

    public HttpFormPost getAssertion() {
        return assertion;
    }
}
