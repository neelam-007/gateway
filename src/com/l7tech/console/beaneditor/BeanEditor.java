/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.beaneditor;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.util.WeakPropertyChangeSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
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
    private WeakPropertyChangeSupport beanListeners = new WeakPropertyChangeSupport();
    private JButton cancelButton;
    private Options options = new Options();

    public static class Options {
        private String description;
        private String[] excludeProperties = new String[]{};

        public void setDescription(String description) {
            this.description = description;
        }

        public void setExcludeProperties(String[] excludeProperties) {
            this.excludeProperties = excludeProperties;
        }
    }

    public BeanEditor(final JFrame frame, Object bean, Class stopClass) {
        this(bean, stopClass);
        final Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        contentPane.add(this, BorderLayout.CENTER);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
    }

    public BeanEditor(final JDialog dialog, Object bean, Class stopClass) {
        this(bean, stopClass);
        final Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        contentPane.add(this, BorderLayout.CENTER);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
    }

    /**
     * Dispose the editor Invokde the dispose on the container (JDialog or JFrame)
     */
    public void dispose() {
        Window w = SwingUtilities.windowForComponent(this);
        if (w instanceof JFrame) {
            JFrame jf = (JFrame)w;
            jf.dispose();
        } else if (w instanceof JDialog) {
            JDialog jd = (JDialog)w;
            jd.dispose();
        }
    }

    public BeanEditor(Object bean, Class stopClass) {
        this.bean = bean;
        this.stopClass = stopClass;
        initResources();
        setLayout(new BorderLayout());
        beanModel = new BeanInfoTableModel(bean, stopClass);
        propertyTable = new JTable(beanModel);
        propertyTable.getTableHeader().setReorderingAllowed(false);
        propertyTable.getTableHeader().setResizingAllowed(true);
        JScrollPane ps = new JScrollPane();
        ps.getViewport().add(propertyTable);
        add(ps, BorderLayout.CENTER);
        JPanel buttonPanel = getButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Adds the bean listener to the list of bean listeners.
     *
     * @param listener the bean listener
     */
    public synchronized void addBeanListener(BeanListener listener) {
        beanListeners.addPropertyChangeListener(listener);
    }

    /**
     * Removes the bean listener from the list of
     *
     * @param listener the bean listener
     */
    public synchronized void removeBeanListener(BeanListener listener) {
        beanListeners.removePropertyChangeListener(listener);
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
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());

        // OK button (global variable)
        okButton = new JButton();
        okButton.setText(resources.getString("okButton.label"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
                for (int i = 0; i < listeners.length; i++) {
                    PropertyChangeListener listener = listeners[i];
                    ((BeanListener)listener).onEditAccepted(BeanEditor.this, bean);
                }
            }
        });
        panel.add(okButton);

        // cancel button
        cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
                for (int i = 0; i < listeners.length; i++) {
                    PropertyChangeListener listener = listeners[i];
                    ((BeanListener)listener).onEditCancelled(BeanEditor.this, bean);
                }
            }
        });
        panel.add(cancelButton);

        // equalize buttons
        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});

        return panel;
    }


}