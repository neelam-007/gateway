/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.beaneditor;

import javax.swing.*;

/**
 * @author emil
 * @version Feb 17, 2004
 */
public class BeanEditorTerst {
    public static void main(String[] args) {
        Person person = new Person();
        JFrame frame = new JFrame();
        BeanEditor be = new BeanEditor(frame, person, Object.class);
        frame.pack();
        frame.show();
    }
}