/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.util.Functions;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.MessageUrlResourceInfo;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Part of {@link XslTransformationPropertiesDialog}.
 * @author alex
 */
public class XslTransformationFetchPanel extends JPanel {
    private JButton removeButton;
    private JButton editButton;
    private JButton addButton;
    private JList regexList;
    private JPanel mainPanel;
    private JCheckBox allowWithoutStylesheetCheckbox;
    private JScrollPane scrollPane;

    private final DefaultListModel regexListModel = new DefaultListModel();
    private final XslTransformationPropertiesDialog xslDialog;
    private String regexTitle;
    private String regexPrompt;

    XslTransformationFetchPanel(final XslTransformationPropertiesDialog parent, XslTransformation assertion) {
        this.xslDialog = parent;

        String[] regexes = null;
        MessageUrlResourceInfo muri = null;
        if (assertion.getResourceInfo() instanceof MessageUrlResourceInfo) {
            muri = (MessageUrlResourceInfo)assertion.getResourceInfo();
            regexes = muri.getUrlRegexes();
        }

        if (regexes != null) {
            for (int i = 0; i < regexes.length; i++) {
                regexListModel.addElement(regexes[i]);
            }
        }
        regexList.setModel(regexListModel);
        regexList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });
        regexList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    if (editButton.isEnabled()) edit();
                }
            }
        });

        allowWithoutStylesheetCheckbox.setSelected(muri != null && muri.isAllowMessagesWithoutUrl());

        regexTitle = parent.getResources().getString("regexDialog.title");
        regexPrompt = parent.getResources().getString("regexDialog.prompt");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pop(null, new Functions.UnaryVoid<Object>() {
                    public void call(Object val) {
                        if (val != null) {
                            regexListModel.addElement(val);
                            regexList.setSelectedValue(val, true);
                        }
                    }
                });
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int which = regexList.getSelectedIndex();
                regexListModel.removeElement(regexList.getSelectedValue());
                regexList.setSelectedIndex(which);
            }
        });

        enableButtons();

        regexList.setModel(regexListModel);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void pop(String initialValue, final Functions.UnaryVoid<Object> result) {
        final OkCancelDialog regexDialog = new OkCancelDialog(xslDialog, regexTitle, true, new RegexPanel(regexPrompt, initialValue));
        regexDialog.pack();
        Utilities.centerOnScreen(regexDialog);
        DialogDisplayer.display(regexDialog, new Runnable() {
            public void run() {
                result.call(regexDialog.getValue());
            }
        });
    }

    private void edit() {
        final String oval = (String)regexList.getSelectedValue();
        final int pos = regexList.getSelectedIndex();

        pop(oval, new Functions.UnaryVoid<Object>() {
            public void call(Object object) {
                String nval = (String)object;
                if (nval == null || oval.equals(nval)) return;

                regexListModel.removeElementAt(pos);
                regexListModel.add(pos, nval);
                regexList.setSelectedValue(nval, true);
                regexList.revalidate();
            }
        });
    }

    private void enableButtons() {
        boolean editremove = regexList.getSelectedValue() != null;
        editButton.setEnabled(editremove);
        removeButton.setEnabled(editremove);
    }

    String check() {
        if (regexListModel.isEmpty()) {
            return xslDialog.getResources().getString("error.noregexes");
        }
        return null;
    }

    void updateModel(XslTransformation assertion) {
        ArrayList regexes = new ArrayList();
        for (Enumeration e = regexListModel.elements(); e.hasMoreElements();) {
            String regex = (String)e.nextElement();
            regexes.add(regex);
        }
        MessageUrlResourceInfo rinfo = new MessageUrlResourceInfo();
        rinfo.setUrlRegexes((String[])regexes.toArray(new String[0]));
        rinfo.setAllowMessagesWithoutUrl(allowWithoutStylesheetCheckbox.isSelected());
        assertion.setResourceInfo(rinfo);
    }

    public JButton getRemoveButton() {
        return removeButton;
    }

    public JButton getEditButton() {
        return editButton;
    }

    public JButton getAddButton() {
        return addButton;
    }

    public JCheckBox getAllowWithoutStylesheetCheckbox() {
        return allowWithoutStylesheetCheckbox;
    }
}
