package com.l7tech.proxy.gui.dialogs;

import javax.swing.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
abstract class SsgIdentityPanel extends JPanel {
    public abstract JButton getClientCertButton();

    public abstract JButton getSsgCertButton();

    public abstract JCheckBox getUseClientCredentialCheckBox();

    public abstract JCheckBox getSavePasswordCheckBox();

    public abstract JTextField getUsernameTextField();

    public abstract JPasswordField getUserPasswordField();
}
