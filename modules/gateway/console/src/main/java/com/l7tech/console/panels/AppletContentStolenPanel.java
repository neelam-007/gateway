package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * Panel the Manager Applet uses to replace its content pane when another applet instance with the same
 * codebase has stolen it.
 */
public class AppletContentStolenPanel extends JPanel {
    private JPanel mainPanel;

    public AppletContentStolenPanel() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }
}
