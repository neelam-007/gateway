package com.l7tech.console.panels;

import javax.swing.JPanel;
import java.awt.*;

/**
 * <code>JPanel</code> that represent a step in the wizard extend the
 * <code>WizardStepPanel</code>.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class WizardStepPanel extends JPanel {

    /** Creates new form WizardPanel */
    public WizardStepPanel() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
    }

    /**
     * @return the wizard step description
     */
    public abstract String getDescription();

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "implement " + getClass() + "getStepLabel()";
    }
}
