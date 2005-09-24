/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license.gui;

import com.l7tech.common.gui.util.Utilities;

/**
 * Standalone application that makes license files.
 */
public class LicenseGeneratorMain {
    public static void main(String[] args) {
        LicenseGeneratorTopWindow window = new LicenseGeneratorTopWindow();
        Utilities.centerOnScreen(window);
        window.setVisible(true);
        // Startup done
    }
}
