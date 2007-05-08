package com.l7tech.server.config.ui.gui;

import javax.swing.*;
import java.awt.*;

/**
 * User: megery
 * Date: May 7, 2007
 * Time: 3:44:28 PM
 */
public class Sca6000KeystorePanel extends KeystorePanel{
    private JPanel mainPanel;


    public Sca6000KeystorePanel() {
        super();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }
}
