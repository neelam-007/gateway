/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.gui.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Content of SSG property dialog tab for "Bridge Policy".
 */
class BridgePolicyPanel extends JPanel {
    private JPanel rootPanel;
    private JCheckBox cbHeaderPassthrough;
    private JCheckBox cbUseSsl;

    public BridgePolicyPanel() {
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
    }

    boolean isHeaderPassthrough() {
        return cbHeaderPassthrough.isSelected();
    }

    void setHeaderPassthrough(boolean passthrough) {
        cbHeaderPassthrough.setSelected(passthrough);
    }

    boolean isUseSslByDefault() {
        return cbUseSsl.isSelected();
    }

    void setUseSslByDefault(boolean ssl) {
        cbUseSsl.setSelected(ssl);
    }
}
