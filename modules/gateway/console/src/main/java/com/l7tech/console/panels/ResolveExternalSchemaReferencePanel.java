package com.l7tech.console.panels;

import com.l7tech.console.policy.exporter.ExternalSchemaReference;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This wizard panel allows to adminsitrator to take action on an unresolved external schema
 * refered to in the imported policy.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Oct 20, 2005<br/>
 */
public class ResolveExternalSchemaReferencePanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(ResolveExternalSchemaReferencePanel.class.getName());
    private JPanel mainPanel;
    private ExternalSchemaReference foreignRef;
    private JButton addSchemaButton;
    private JRadioButton asIsRadio;
    private JRadioButton removeRadio;
    private JTextField tnsField;
    private JTextField nameField;

    public ResolveExternalSchemaReferencePanel(WizardStepPanel next, ExternalSchemaReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        nameField.setText(foreignRef.getName());
        tnsField.setText(foreignRef.getTns());
        ButtonGroup actionRadios = new ButtonGroup();
        actionRadios.add(asIsRadio);
        actionRadios.add(removeRadio);
        addSchemaButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCreateSchema();
            }
        });
        removeRadio.setSelected(true);
    }

    public boolean onNextButton() {
        if (removeRadio.isSelected()) {
            foreignRef.setRemoveRefferees(true);
        }
        return true;
    }

    private void onCreateSchema() {
        // show global schemas
        // get wizard
        Component component = this.getParent();
        while (component != null) {
            if (component instanceof Wizard) break;
            component = component.getParent();
        }
        GlobalSchemaDialog dlg = new GlobalSchemaDialog((Wizard)component);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // see if resolution is now fixed
                Registry reg = Registry.getDefault();
                if (reg == null || reg.getSchemaAdmin() == null) {
                    logger.warning("No access to registry. Cannot check fix.");
                    return;
                }
                boolean fixed = false;
                try {
                    fixed = !reg.getSchemaAdmin().findByName(foreignRef.getName()).isEmpty();
                    if (!fixed) {
                        fixed = !reg.getSchemaAdmin().findByTNS(foreignRef.getTns()).isEmpty();
                    }
                } catch (FindException e) {
                    logger.log(Level.SEVERE, "cannot check fix", e);
                    throw new RuntimeException(e);
                }
                if (fixed) {
                    asIsRadio.setEnabled(true);
                    removeRadio.setEnabled(false);
                    asIsRadio.setSelected(true);
                    removeRadio.setSelected(false);
                    addSchemaButton.setEnabled(false);
                    // todo, force next?
                }
            }
        });
    }

    public String getDescription() {
        return getStepLabel();
    }

    public String getStepLabel() {
        String ref = foreignRef.getName();
        if (ref == null) {
            ref = foreignRef.getTns();
        }
        return "Unresolved external schema " + ref;
    }

    public boolean canFinish() {
        return !hasNextPanel();
    }

}
