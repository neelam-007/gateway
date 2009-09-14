package com.l7tech.console.panels;

import com.l7tech.policy.assertion.InverseHttpFormPost;

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
public class InverseHttpFormPostDialog extends LegacyAssertionPropertyDialog {
    private JList fieldList;
    private JButton removeButton;
    private JButton modifyButton;
    private JButton addButton;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;

    private final InverseHttpFormPost assertion;
    private final DefaultComboBoxModel fieldListModel;
    private boolean assertionModified = false;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private Frame ownerFrame;

    public InverseHttpFormPostDialog(Frame owner, InverseHttpFormPost ass, boolean readOnly) throws HeadlessException {
        super(owner, ass, true);
        this.ownerFrame = owner;
        this.assertion = ass;
        fieldListModel = new DefaultComboBoxModel(wrap(ass.getFieldNames()));

        fieldList.setModel(fieldListModel);
        enableButtons();

        fieldList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    modify();
                }
            }
        });

        fieldList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                assertionModified = false;
                dispose();
            }
        });

        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList names = new ArrayList();
                for (int i = 0; i < fieldListModel.getSize(); i++) {
                    FieldnameListElement name = (FieldnameListElement) fieldListModel.getElementAt(i);
                    names.add(name.fieldname);
                }
                assertion.setFieldNames((String[]) names.toArray(new String[0]));
                assertionModified = true;
                dispose();
            }
        });

        moveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int pos = fieldList.getSelectedIndex();
                if (pos < 1) return;
                Object which = fieldListModel.getElementAt(pos);
                fieldListModel.removeElementAt(pos);
                fieldListModel.insertElementAt(which, pos-1);
                fieldList.setSelectedValue(which, true);
                enableButtons();
            }
        });

        moveDownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int pos = fieldList.getSelectedIndex();
                if (pos > fieldListModel.getSize() - 2) return;
                Object which = fieldListModel.getElementAt(pos);
                fieldListModel.removeElementAt(pos);
                fieldListModel.insertElementAt(which, pos+1);
                fieldList.setSelectedValue(which, true);
                enableButtons();
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fieldname = edit("");
                if (fieldname != null && fieldname.length() > 0) {
                    fieldListModel.addElement(new FieldnameListElement(fieldname));
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fieldListModel.removeElement(fieldList.getSelectedValue());
            }
        });

        modifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modify();
            }
        });


        getContentPane().add(mainPanel);
    }

    private void modify() {
        FieldnameListElement el = (FieldnameListElement) fieldList.getSelectedValue();
        if (el == null) return;
        String edited = edit(el.fieldname);
        if (edited != null) {
            el.fieldname = edited;
            fieldList.repaint(); // TODO this seems dumb but validate() doesn't cut it
        }
    }

    private String edit(String fieldname) {
        Object result = JOptionPane.showInputDialog(this,
                                                    "Enter the HTTP form field name that will\nbe matched to an incoming MIME part:",
                                                    "Configure Field Name",
                JOptionPane.PLAIN_MESSAGE, null, null,
                                                    fieldname);
        if (result == null || result instanceof String) {
            return (String) result;
        } else {
            throw new IllegalStateException();
        }
    }

    private void enableButtons() {
        boolean selected = fieldList.getSelectedValue() != null;
        removeButton.setEnabled(selected);
        moveUpButton.setEnabled(selected && fieldList.getSelectedIndex() > 0);
        moveDownButton.setEnabled(selected && fieldList.getSelectedIndex() <= fieldListModel.getSize() - 2);
        modifyButton.setEnabled(selected);
    }

    private class FieldnameListElement {
        private String fieldname;

        private FieldnameListElement(String fieldname) {
            this.fieldname = fieldname;
        }

        @Override
        public String toString() {
            return (fieldListModel.getIndexOf(this) + 1) + ": " + fieldname;
        }

    }

    private FieldnameListElement[] wrap(String[] fieldnames) {
        ArrayList elements = new ArrayList();
        for (int i = 0; i < fieldnames.length; i++) {
            String fieldname = fieldnames[i];
            elements.add(new FieldnameListElement(fieldname));
        }
        return (FieldnameListElement[]) elements.toArray(new FieldnameListElement[0]);
    }

    private String[] unwrap(FieldnameListElement[] elements) {
        ArrayList names = new ArrayList();
        for (int i = 0; i < elements.length; i++) {
            FieldnameListElement element = elements[i];
            names.add(element.fieldname);
        }
        return (String[]) names.toArray(new String[0]);
    }

    public boolean isAssertionModified() {
        return assertionModified;
    }

    public InverseHttpFormPost getAssertion() {
        return assertion;
    }
}
