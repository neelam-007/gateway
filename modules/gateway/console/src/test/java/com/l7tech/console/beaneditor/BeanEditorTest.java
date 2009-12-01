/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.beaneditor;

import org.junit.Ignore;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;

/**
 * @author emil
 * @version Feb 17, 2004
 */
@Ignore
public class BeanEditorTest {
    public static void main(String[] args) {
        final Person person = new Person();
        final JFrame frame = new JFrame();
        BeanEditor.Options bopts = new BeanEditor.Options();
        bopts.setExcludeProperties(new String[]{"age"});

        BeanEditor be = new BeanEditor(frame, person, Object.class, bopts);
        be.addBeanListener(new BeanListener() {
            @Override
            public void onEditAccepted(Object source, Object bean) {
                frame.dispose();
            }

            @Override
            public void onEditCancelled(Object source, Object bean) {
                frame.dispose();
            }

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
            }
        });
        frame.pack();
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.out.println("age: " + person.age);
                System.out.println("name: " + person.name);
                System.out.println("address: " + person.address);
            }
        });

    }
}