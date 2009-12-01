/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.beaneditor;

import com.l7tech.gui.util.Utilities;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.InvocationTargetException;

/**
 * The generic BeanEditor that uses introspection to discover the properties
 * and creates the tabular editor.
 *
 * @author emil
 * @version Feb 17, 2004
 */
public class BeanEditor extends JPanel {
    private static final Logger logger = Logger.getLogger(BeanEditor.class.getName());

    protected Object originalBean;
    protected Object bean;
    protected JTable propertyTable;
    protected BeanInfoTableModel beanModel;
    private Class stopClass;
    private ResourceBundle resources;
    private JButton okButton;
    private PropertyChangeSupport beanListeners = new PropertyChangeSupport(this);
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
        this(frame, bean, stopClass, new Options());
    }

    public BeanEditor(final JFrame frame, Object bean, Class stopClass, Options options) {
        this(bean, stopClass, options);
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

    public BeanEditor(JDialog dialog, Object bean, Class stopClass) {
        this(dialog, bean, stopClass, new Options());
    }

    public BeanEditor(final JDialog dialog, Object bean, Class stopClass, Options options) {
        this(bean, stopClass, options);
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
        Utilities.dispose(Utilities.getRootPaneContainerAncestor(this));
    }

    public BeanEditor(Object originalBean, Class stopClass, Options options) {
        this.originalBean = originalBean;
        this.bean = copyBean(originalBean);
        this.stopClass = stopClass;
        this.options = options;
        initResources();
        setLayout(new BorderLayout());
        add(getTitlePanel(), BorderLayout.NORTH);
        beanModel = new BeanInfoTableModel(this.bean, stopClass, options.excludeProperties);
        propertyTable = new JTable(beanModel);
        propertyTable.getTableHeader().setReorderingAllowed(false);
        propertyTable.getTableHeader().setResizingAllowed(true);
        propertyTable.setRowHeight(20);
        propertyTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        TableColumn column = propertyTable.getColumnModel().getColumn(0);
        column.setCellRenderer(new ButtonRenderer());

        column = propertyTable.getColumnModel().getColumn(1);
        TableCellEditor te = column.getCellEditor();
        if (te == null) {
            final JTextField textField = new JTextField();
            final DefaultCellEditor cellEditor = new DefaultCellEditor(textField);
            cellEditor.setClickCountToStart(1);
            column.setCellEditor(cellEditor);
        }
        JScrollPane ps = new JScrollPane();
        ps.getViewport().add(propertyTable);
        add(ps, BorderLayout.CENTER);
        JPanel buttonPanel = getButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private Object copyBean(Object bean) {
        try {
            if (bean instanceof Cloneable)
                return Object.class.getMethod("clone").invoke(bean);
        } catch (InvocationTargetException e) {
            logger.log(Level.WARNING, "Unable to clone Cloneable instance of class " + bean.getClass(), e);
            /* FALLTHROUGH and try bean property copy */
        } catch (NoSuchMethodException e) {
            logger.log(Level.WARNING, "Unable to clone Cloneable instance of class " + bean.getClass(), e);
            /* FALLTHROUGH and try bean property copy */
        } catch (IllegalAccessException e) {
            logger.log(Level.WARNING, "Unable to clone Cloneable instance of class " + bean.getClass(), e);
            /* FALLTHROUGH and try bean property copy */
        }

        try {
            Object copy = bean.getClass().newInstance();
            BeanUtils.copyProperties(bean, copy);
            return copy;
        } catch (InstantiationException e) {
            logger.log(Level.WARNING, "Unable to invoke no-arg constructor for class " + bean.getClass(), e);
            return null;
        } catch (IllegalAccessException e) {
            logger.log(Level.WARNING, "Unable to invoke no-arg constructor for class " + bean.getClass(), e);
            return null;
        } catch (InvocationTargetException e) {
            logger.log(Level.WARNING, "Unable to copy properties for class " + bean.getClass(), e);
            return null;
        }
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
     * Creates the title panel of the dialog
     * <p/>
     */
    private JPanel getTitlePanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 5));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        String title = null;
        if (options.description == null) {
            title = resources.getString("dialog.description");
        } else {
            title = options.description;
        }
        panel.add(new JLabel(title));
        return panel;
    }


    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     */
    private JPanel getButtonPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());

        okButton = new JButton();
        okButton.setText(resources.getString("okButton.label"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    BeanUtils.copyProperties(bean, originalBean);
                } catch (InvocationTargetException e) {
                    logger.log(Level.WARNING, "Unable to copy properties for bean: " + ExceptionUtils.getMessage(e), e);
                } catch (IllegalAccessException e) {
                    logger.log(Level.WARNING, "Unable to copy properties for bean: " + ExceptionUtils.getMessage(e), e);
                }
                PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
                for (int i = 0; i < listeners.length; i++) {
                    PropertyChangeListener listener = listeners[i];
                    ((BeanListener)listener).onEditAccepted(BeanEditor.this, originalBean);
                }
            }
        });
        panel.add(okButton);

        cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
                for (int i = 0; i < listeners.length; i++) {
                    PropertyChangeListener listener = listeners[i];
                    ((BeanListener)listener).onEditCancelled(BeanEditor.this, originalBean);
                }
            }
        });
        panel.add(cancelButton);
        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});
        return panel;
    }

    class ButtonRenderer extends JLabel implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        public Component
          getTableCellRendererComponent(JTable table, Object value,
                                        boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
}