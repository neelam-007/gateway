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
                            new LevelComboItems(SoapFaultLevel.DROP_CONNECTION, "Drop Connection Completly"),
                            new LevelComboItems(SoapFaultLevel.GENERIC_FAULT, "Generic SOAP Fault (no detail on failure)"),
                            new LevelComboItems(SoapFaultLevel.MEDIUM_DETAIL_FAULT, "Medium Detail (SOAP fault with failure details)"),
                            new LevelComboItems(SoapFaultLevel.FULL_TRACE_FAULT, "Full Detail (SOAP fault with complete policy evaluation details)"),
                            new LevelComboItems(SoapFaultLevel.TEMPLATE_FAULT, "Template Fault (provide your own xml to return)")
                          }));

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

        // todo, set initial data
    }

    private void ok() {
        // todo, some sort of validation
        cancel();
    }

    private void cancel() {
        dispose();
    }

    private class LevelComboItems {
        private int level;
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
}
