/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import javax.swing.*;

/**
 * @author mike
 */
public class AuditViewerFrameTest {
    public static void main(String[] args) {
        AuditViewerFrame avf = new AuditViewerFrame();
        avf.show();
        avf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
