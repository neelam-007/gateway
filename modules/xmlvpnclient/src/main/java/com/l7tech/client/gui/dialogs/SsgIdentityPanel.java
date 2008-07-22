package com.l7tech.client.gui.dialogs;

import javax.swing.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 */
abstract class SsgIdentityPanel extends JPanel {
    public abstract JButton getClientCertButton();

    public abstract JButton getSsgCertButton();

    public abstract ImageIcon getGeneralPaneImageIcon();
}
