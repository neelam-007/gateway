/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.beaneditor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * The <code>TableModel</code> implementation to support bean property
 * editor.
 * @author emil
 * @version Feb 17, 2004
 */
public class BeanInfoTableModel extends AbstractTableModel {
    protected List propertiesList;
    protected Object bean;
    /**
     *
     * @param bean
     * @param stopClass
     */
    public BeanInfoTableModel(Object bean, Class stopClass) {
        this(bean, stopClass, new String[] {});
    }
    /**
     *
     * @param bean
     * @param stopClass
     * @param excluded
     */
    public BeanInfoTableModel(Object bean, Class stopClass, String[] excluded) {
        this.bean = bean;
        try {
            Set sortedProperties = new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2) {
                    PropertyDescriptor p1 = (PropertyDescriptor)o1;
                    PropertyDescriptor p2 = (PropertyDescriptor)o2;
                    return p1.getDisplayName().compareToIgnoreCase(p2.getDisplayName());
                }
            });
            BeanInfo info = Introspector.getBeanInfo(bean.getClass(), stopClass);
            PropertyDescriptor[] props = info.getPropertyDescriptors();
            List excludedList = Arrays.asList(excluded);
            int numProps = props.length;
            for (int k = 0; k < numProps; k++) {
                if (excludedList.indexOf(props[k].getName()) == -1) {
                    sortedProperties.add(props[k]);
                }
            }
            propertiesList = new ArrayList(sortedProperties);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + ex.toString(),
              "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    public int getRowCount() {
        return propertiesList.size();
    }

    public int getColumnCount() {
        return 2;
    }

    public String getColumnName(int nCol) {
        return nCol == 0 ? "Property" : "Value";
    }

    public boolean isCellEditable(int nRow, int nCol) {
        return true;
    }

    public Object getValueAt(int nRow, int nCol) {
        if (nRow < 0 || nRow >= getRowCount()) return "";

        try {
            PropertyDescriptor prop = (PropertyDescriptor)propertiesList.get(nRow);
            switch (nCol) {
                case 0:
                    return prop.getDisplayName();
                case 1:
                    return readProperty(prop);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("getValueAt", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("getValueAt", e);
        }
        return "";
    }

    public void setValueAt(Object value, int nRow, int nCol) {
        if (nCol !=1) return;
        String str = value.toString();
        PropertyDescriptor prop = (PropertyDescriptor)propertiesList.get(nRow);
        Class cls = prop.getPropertyType();
        Object obj = stringToObj(str, cls);
        if (obj == null)
            return;				// can't process

        Method mWrite = prop.getWriteMethod();
        if (mWrite == null || mWrite.getParameterTypes().length != 1)
            return;
        try {
            mWrite.invoke(bean, new Object[]{obj});
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + ex.toString(),
              "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }


    private Object readProperty(PropertyDescriptor prop)
      throws IllegalAccessException, InvocationTargetException {
        Method mRead = prop.getReadMethod();
        if (mRead != null &&
          mRead.getParameterTypes().length == 0) {
            Object value = mRead.invoke(bean, null);
            return value != null ? value.toString() : "";
        } else
            return "error";
    }

    private Object stringToObj(String str, Class cls) {
        try {
            if (str == null)
                return null;
            String name = cls.getName();
            if (name.equals("java.lang.String"))
                return str;
            else if (name.equals("int"))
                return new Integer(str);
            else if (name.equals("long"))
                return new Long(str);
            else if (name.equals("float"))
                return new Float(str);
            else if (name.equals("double"))
                return new Double(str);
            else if (name.equals("boolean"))
                return new Boolean(str);
            return null;		// not supported
        } catch (Exception ex) {
            return null;
        }
    }
}