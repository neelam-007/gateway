package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * Wizard panel that allows the publication of a non-soap xml service.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 14, 2004<br/>
 * $Id$<br/>
 */
public class NonSoapServicePanel extends WizardStepPanel {
    /**
     * Creates new form WizardPanel
     */
    public NonSoapServicePanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
            setLayout(new BorderLayout());
            add(mainPanel);
    }

    private JPanel mainPanel;
}
