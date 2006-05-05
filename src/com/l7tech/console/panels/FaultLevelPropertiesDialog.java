package com.l7tech.console.panels;

import com.l7tech.policy.assertion.FaultLevel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.xml.SoapFaultLevel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
    private JComboBox levelBox;
    private JTextPane descriptionPane;
    private JScrollPane xmlEditorScrollPane;

    public FaultLevelPropertiesDialog(Frame owner, FaultLevel subject) {
        super(owner, TITLE, true);
        this.assertion = subject;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });

        // populate the combo box with the possible levels
        levelBox.setModel(new DefaultComboBoxModel(new LevelComboItems[] {
                            new LevelComboItems(SoapFaultLevel.DROP_CONNECTION, "Drop Connection"),
                            new LevelComboItems(SoapFaultLevel.GENERIC_FAULT, "Generic SOAP Fault"),
                            new LevelComboItems(SoapFaultLevel.MEDIUM_DETAIL_FAULT, "Medium Detail"),
                            new LevelComboItems(SoapFaultLevel.FULL_TRACE_FAULT, "Full Detail"),
                            new LevelComboItems(SoapFaultLevel.TEMPLATE_FAULT, "Template Fault")
                          }));

        levelBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onComboSelection();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        /* todo, add help button
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });
        */

        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        // todo, other controls

        setInitialData();
    }

    public void setInitialData() {
        // todo, set initial data
        onComboSelection();
    }

    private void ok() {
        // todo, some sort of validation
        cancel();
    }

    private void cancel() {
        dispose();
    }

    private class LevelComboItems {
        public int level;
        private String levelname;
        public LevelComboItems(int level, String levelname) {
            assert(levelname != null);
            this.level = level;
            this.levelname = levelname;
        }
        public String toString() {
            return levelname;
        }
    }

    private void onComboSelection() {
        LevelComboItems currentselection = (LevelComboItems)levelBox.getSelectedItem();
        String description;
        switch (currentselection.level) {
            case SoapFaultLevel.DROP_CONNECTION:
                description = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "drop the connection with the requestor without returning anything.</p></html>";
                break;
            case SoapFaultLevel.GENERIC_FAULT:
                description = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "return a generic SOAP fault without the details of the reason for " +
                              "the policy violation.</p><p>A sample of such a SOAP fault is displayed " +
                              "below:</p></html>";
                break;
            case SoapFaultLevel.MEDIUM_DETAIL_FAULT:
                description = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "return a SOAP fault which contains information regarding the reasons for " +
                              "the policy violation.</p><p>A sample of such a SOAP fault is displayed " +
                              "below:</p></html>";
                break;
            case SoapFaultLevel.FULL_TRACE_FAULT:
                description = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "return a SOAP fault which contains a full trace for each assertion " +
                              "evaluation in the policy whether the assertion was a success or " +
                              "failure.</p><p>A sample of such a SOAP fault is displayed below:</p></html>";
                break;
            case SoapFaultLevel.TEMPLATE_FAULT:
                description = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "return a SOAP fault based on a template provided by you. You can use " +
                              "context variables as part of the template.</p><p><b>You must edit the template " +
                              "below:</b></p></html>";
                break;
            default:
                // can't happen (unless bug)
                throw new RuntimeException("Unhandeled SoapFaultLevel");
        }
        descriptionPane.setText(description);
    }
}
