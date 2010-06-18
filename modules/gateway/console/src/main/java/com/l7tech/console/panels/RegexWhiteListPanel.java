/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.util.Functions;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.assertion.UsesResourceInfo;

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
import java.util.ResourceBundle;

/**
 * Panel that allows a user to enter in a list of URL white list regexes.
 *
 * @author alex
 * @author darmstrong - updated to be usable outside of XSL.
 */
public class RegexWhiteListPanel extends JPanel {
    private JButton removeButton;
    private JButton editButton;
    private JButton addButton;
    private JList regexList;
    private JPanel mainPanel;
    private JCheckBox noUrlFoundCheckBox;
    private JScrollPane scrollPane;
    private JLabel regexInfoLabel;
    private JLabel regexDescriptionLabel;

    private final DefaultListModel regexListModel = new DefaultListModel();
    private final JDialog parentDialog;
    private String regexTitle;
    private String regexPrompt;
    private ResourceBundle resourceBundle;

    /**
     * The following keys are required in the resource bundle:
     * regexDialog.title
     * regexDialog.prompt
     * error.noregexes
     * noUrlFoundCheckbox.text
     * fetchRegexList.label
     * fetchRegexList.description
     *
     * @param parent JDialog parent
     * @param assertion UsesResourceInfo assertion bean
     * @param resourceBundle ResourceBundle which contains the above keys
     */
    public RegexWhiteListPanel(final JDialog parent, UsesResourceInfo assertion, ResourceBundle resourceBundle) {
        this.parentDialog = parent;
        this.resourceBundle = resourceBundle;
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
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });
        regexList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    if (editButton.isEnabled()) edit();
                }
            }
        });

        noUrlFoundCheckBox.setSelected(muri != null && muri.isAllowMessagesWithoutUrl());

        regexTitle = resourceBundle.getString("regexDialog.title");
        regexPrompt = resourceBundle.getString("regexDialog.prompt");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pop(null, new Functions.UnaryVoid<Object>() {
                    @Override
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
            @Override
            public void actionPerformed(ActionEvent e) {
                edit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int which = regexList.getSelectedIndex();
                regexListModel.removeElement(regexList.getSelectedValue());
                regexList.setSelectedIndex(which);
            }
        });

        noUrlFoundCheckBox.setText(resourceBundle.getString("noUrlFoundCheckbox.text"));
        regexInfoLabel.setText(resourceBundle.getString("fetchRegexList.label"));
        regexDescriptionLabel.setText(resourceBundle.getString("fetchRegexList.description"));

        enableButtons();

        regexList.setModel(regexListModel);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void pop(String initialValue, final Functions.UnaryVoid<Object> result) {
        final OkCancelDialog regexDialog = new OkCancelDialog(parentDialog, regexTitle, true, new RegexPanel(regexPrompt, initialValue));
        regexDialog.pack();
        Utilities.centerOnScreen(regexDialog);
        DialogDisplayer.display(regexDialog, new Runnable() {
            @Override
            public void run() {
                result.call(regexDialog.getValue());
            }
        });
    }

    private void edit() {
        final String oval = (String)regexList.getSelectedValue();
        final int pos = regexList.getSelectedIndex();

        pop(oval, new Functions.UnaryVoid<Object>() {
            @Override
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

    public String check() {
        if (regexListModel.isEmpty()) {
            return resourceBundle.getString("error.noregexes");
        }
        return null;
    }

    public void updateModel(UsesResourceInfo assertion) {
        ArrayList regexes = new ArrayList();
        for (Enumeration e = regexListModel.elements(); e.hasMoreElements();) {
            String regex = (String)e.nextElement();
            regexes.add(regex);
        }
        MessageUrlResourceInfo rinfo = new MessageUrlResourceInfo();
        rinfo.setUrlRegexes((String[])regexes.toArray(new String[0]));
        rinfo.setAllowMessagesWithoutUrl(noUrlFoundCheckBox.isSelected());
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

    public JCheckBox getNoUrlFoundCheckBox() {
        return noUrlFoundCheckBox;
    }
}
