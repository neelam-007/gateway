/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jan 18, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertionBase;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.action.Actions;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Corresponding GUI for the {@link com.l7tech.console.action.EditXmlSecurityRecipientContextAction} action.
 *
 * @author flascelles@layer7-tech.com
 */
public class XmlSecurityRecipientContextEditor extends JDialog {
    private JPanel mainPanel;
    private JButton assignCertButton;
    private JTextField certSubject;
    private JComboBox actorComboBox;
    private JRadioButton specificRecipientRradio;
    private JRadioButton defaultRadio;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JLabel label2;
    private JLabel label1;
    private JPanel detailsPanel;

    private XmlSecurityAssertionBase assertion;


    public XmlSecurityRecipientContextEditor(Frame owner, XmlSecurityAssertionBase assertion) {
        super(owner, true);
        this.assertion = assertion;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("XmlSecurityRecipientContext");
        ButtonGroup bg = new ButtonGroup();
        bg.add(specificRecipientRradio);
        bg.add(defaultRadio);
        setActionListeners();
        setInitialValues();
        enableSpecificControls();
    }

    private void setActionListeners() {
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableSpecificControls();
            }
        };
        specificRecipientRradio.addActionListener(al);
        defaultRadio.addActionListener(al);

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

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        // todo, actorComboBox

        assignCertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                RecipientSecurityHeaderWizardStep panel3 = new RecipientSecurityHeaderWizardStep(null);
                CertDetailsPanel panel2 = new CertDetailsPanel(panel3);
                CertImportMethodsPanel panel1 = new CertImportMethodsPanel(panel2, true);



                JFrame f = TopComponents.getInstance().getMainWindow();
                Wizard w = new AddCertificateWizard(f, panel1);
                w.setTitle("Define new XML security recipient");
                w.pack();
                w.setSize(800, 560);
                Utilities.centerOnScreen(w);
                w.setVisible(true);

                // todo, read captured info and update combo box and txt field
            }
        });

    }

    private void setInitialValues() {
        if (assertion.getRecipientContext().localRecipient()) {
            specificRecipientRradio.setSelected(false);
            defaultRadio.setSelected(true);
        } else {
            specificRecipientRradio.setSelected(true);
            defaultRadio.setSelected(false);
        }
    }

    private void enableSpecificControls() {
        if (specificRecipientRradio.isSelected()) {
            assignCertButton.setEnabled(true);
            certSubject.setEnabled(true);
            actorComboBox.setEnabled(true);
            label2.setEnabled(true);
            label1.setEnabled(true);
            TitledBorder border = ((TitledBorder)detailsPanel.getBorder());
            border.setTitleColor(Color.BLACK);
            detailsPanel.repaint();
        } else {
            assignCertButton.setEnabled(false);
            certSubject.setEnabled(false);
            actorComboBox.setEnabled(false);
            label2.setEnabled(false);
            label1.setEnabled(false);
            TitledBorder border = ((TitledBorder)detailsPanel.getBorder());
            border.setTitleColor(Color.GRAY);
            detailsPanel.repaint();
        }
    }

    private void ok() {
        // todo, remember value
        XmlSecurityRecipientContextEditor.this.dispose();
    }

    private void help() {
        Actions.invokeHelp(XmlSecurityRecipientContextEditor.this);
    }

    private void cancel() {
        XmlSecurityRecipientContextEditor.this.dispose();
    }

    public static void main(String[] args) {
        RequestWssIntegrity assertion = new RequestWssIntegrity();
        XmlSecurityRecipientContextEditor dlg = new XmlSecurityRecipientContextEditor(null, assertion);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        System.exit(0);
    }
}
