/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * The <code>FormPreparer</code> class processes the form elements and applies the
 * rules using the reflective {@link FormPreparer.ComponentPreparer} instance. The
 * preparer implementations can define overloaded {@link FormPreparer.ComponentPreparer#prepare(java.awt.Component)}
 * methods, with more specific parameter from the <code>Component</code> class hierarchy.
 *
 * @author emil
 * @version Sep 23, 2004
 */
public class FormPreparer {
    private ComponentPreparer componentPreparer;

    public FormPreparer(ComponentPreparer v) {
        componentPreparer = v;
    }

    /**
     * Prepare the <code>JFrame</code> visit each component on the frame
     * @param frame the frame to prepare
     */
    public void prepare(JFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException();
        }
        traverse(frame.getComponents());
    }

    public void prepare(JDialog dialog) {
        if (dialog == null) {
            throw new IllegalArgumentException();
        }
        traverse(dialog.getComponents());
    }


    private void traverse(Component[] components) {
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            visit(component);
            if (component instanceof Container) {
                processContainer((Container)component);
            }
        }
    }

    private void visit(Component c) {
        try {
            Method mostSpecific = getMostSpecific(c);
            if (mostSpecific == null) {
                componentPreparer.prepare(c);
            } else {
                mostSpecific.invoke(this, new Object[]{c});
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void processContainer(Container container) {
        Component[] components = container.getComponents();

        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            if (component instanceof Container) {
                processContainer((Container)component);
            }
        }
    }

    private Method getMostSpecific(Component c) {
        Class cl = c.getClass();  // the bottom-most class
        // Check through superclasses for matching method
        while (!cl.equals(Component.class)) {
            try {
                return componentPreparer.getClass().getDeclaredMethod("prepare", new Class[]{cl});
            } catch (NoSuchMethodException ex) {
                cl = cl.getSuperclass();
            }
        }
        // Check through interfaces for matching method
        Class[] interfaces = c.getClass().getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            try {
                return componentPreparer.getClass().getDeclaredMethod("prepare", new Class[]{interfaces[i]});
            } catch (NoSuchMethodException ex) {
            }
        }
        return null;
    }


    /**
     * The <code>ComponentPreparer</code> interfacde defines the method that is invoked
     * for each form <code>Component</code>. The implementations can define
     * overloaded methods, with the more speecific parameters, (must be Component
     * subclass), and the most specific method will be invoked then.
     */
    public static interface ComponentPreparer {
        void prepare(Component c);
    }

    /** the precanned <code>ComponentPreparer</code> that sets the text component read only */
    public static ComponentPreparer TEXT_COMPONENT_READONLY_PREPARER = new ComponentPreparer() {
        public void prepare(Component c) {}

        public void prepare(JTextComponent c) {
            c.setEditable(false);
        }
    };

    /**
     * <code>CompositePreparer</code> holder for one or more preparers
     */
    public static final class CompositePreparer implements ComponentPreparer {
        private ComponentPreparer[] visitors = new ComponentPreparer[] {};

        public CompositePreparer(ComponentPreparer[] visitors) {
            if (visitors != null) {
                this.visitors = visitors;
            }
        }

        public void prepare(Component c) {
            for (int i = 0; i < visitors.length; i++) {
                ComponentPreparer componentPreparer = visitors[i];
                componentPreparer.prepare(c);
            }
        }
    }
}