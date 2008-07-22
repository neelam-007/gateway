package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class VirtualGroupMembershipPanel extends JPanel {

    private JPanel mainPanel;

    /**
     *  Constructor
     */
    public VirtualGroupMembershipPanel() {
            initComponents();
        }

    /**
      * This method is called from within the constructor to
      * initialize the dialog.
      */
     private void initComponents() {
         this.add(mainPanel);
     }

}
