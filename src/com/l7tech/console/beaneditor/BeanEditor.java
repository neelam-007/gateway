/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.beaneditor;

import com.l7tech.common.gui.util.TableUtil;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author emil
 * @version Feb 17, 2004
 */
public class BeanEditor extends JPanel {
    protected Object bean;
    protected JTable propertyTable;
    protected BeanInfoTableModel beanModel;
    private Class stopClass;
    private ResourceBundle resources;
    private JButton okButton;

    public static JFrame newFrameEditor(Object bean, Class stopClass) {
        BeanEditor bp = new BeanEditor(bean, stopClass);
        JFrame jf = new JFrame();
        final Container contentPane = jf.getContentPane();
        contentPane.setLayout(new BorderLayout());
        bp.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        jf.getContentPane().add(bp, BorderLayout.CENTER);
        return jf;
    }

    public static JDialog newDialogEditor(Object bean, Class stopClass) {
        BeanEditor bp = new BeanEditor(bean, stopClass);
        JDialog jd = new JDialog();
        final Container contentPane = jd.getContentPane();
        contentPane.setLayout(new BorderLayout());
        bp.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        contentPane.add(bp, BorderLayout.CENTER);
        return jd;
    }

    public BeanEditor(Object bean, Class stopClass) {
        this.bean = bean;
        this.stopClass = stopClass;
        initResources();
        setLayout(new BorderLayout());
        beanModel = new BeanInfoTableModel(bean, stopClass);
        propertyTable = new JTable(beanModel);
        JScrollPane ps = new JScrollPane();
        ps.getViewport().add(propertyTable);
        add(ps, BorderLayout.CENTER);
        JPanel buttonPanel = getButtonPanel(); // sets global loginButton
        add(buttonPanel, BorderLayout.SOUTH);
        TableUtil.adjustColumnWidth(propertyTable, 0);
    }

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.beaneditor.BeanEditor", locale);
    }


    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     * <p/>
     * Sets the variable okButton
     */
    private JPanel getButtonPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());

        // OK button (global variable)
        okButton = new JButton();
        okButton.setText(resources.getString("okButton.label"));
        okButton.
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent event) {
              }
          });
        panel.add(okButton);

        // space

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent event) {
              }
          });
        panel.add(cancelButton);

        // equalize buttons
        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});

        return panel;
    }


}