package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * This wizard panel allows an administrator to resolve a missing identity provider
 * refered to in an exported policy during import. This is only invoked when the
 * missing identity provider cannot be resolved automatically based on the exported
 * properties.
 * 
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 23, 2004<br/>
 * $Id$
 */
public class ResolveForeignIdentityProviderPanel extends WizardStepPanel {
    public ResolveForeignIdentityProviderPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private JPanel mainPanel;
    private JTextField foreignProviderName;
    private JTextField foreignProviderType;
}
