package com.l7tech.console.panels;

import com.l7tech.policy.assertion.FaultLevel;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog box to edit the properties of a FaultLevel assertion
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 4, 2006<br/>
 *
 * @see com.l7tech.policy.assertion.FaultLevel
 */
public class FaultLevelPropertiesDialog extends JDialog {
    private static final String TITLE = "Fault Level Properties";
    private FaultLevel assertion;
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;

    public FaultLevelPropertiesDialog(Frame owner, FaultLevel subject) {
        super(owner, TITLE, true);
        this.assertion = subject;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });
    }
}
