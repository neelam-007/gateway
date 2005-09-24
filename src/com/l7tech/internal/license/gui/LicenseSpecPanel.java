/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license.gui;

import com.l7tech.internal.license.LicenseSpec;

import javax.swing.*;

/**
 * Panel for editing a LicenseSpec
 */
public class LicenseSpecPanel extends JPanel {
    private JPanel rootPanel;

    public LicenseSpecPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(rootPanel);
    }

    /** Updates the view to correspond with this license spec. */
    public void setSpec(LicenseSpec spec) {

    }
}
