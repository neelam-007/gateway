package com.l7tech.console.panels;

import com.ibm.wsdl.DefinitionImpl;
import com.ibm.wsdl.extensions.PopulatedExtensionRegistry;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.MainWindow;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.io.StringWriter;

import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.tokenmarker.XMLTokenMarker;

/**
 * The <code>Wizard</code> that drives the wizard step panels.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WsdlCreateWizard extends Wizard {
    static final Logger log = Logger.getLogger(WsdlCreateWizard.class.getName());

    public WsdlCreateWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setResizable(true);
        setTitle("Create WSDL Wizard");
        Definition def = new DefinitionImpl();
        def.setExtensionRegistry(new PopulatedExtensionRegistry());
        wizardInput = def;
        this.addWizardListener(wizardListener);

        getButtonHelp().addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(WsdlCreateWizard.this);
            }
        });
    }

    protected final JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EtchedBorder());
        buttonPanel.add(getButtonBack());
        buttonPanel.add(getButtonNext());
        buttonPanel.add(getButtonPreview());
        buttonPanel.add(getButtonFinish());
        buttonPanel.add(getButtonCancel());
        buttonPanel.add(getButtonHelp());
        return buttonPanel;
    }

    protected JButton getButtonPreview() {
        if (buttonPreview == null) {
            buttonPreview = new JButton();
            buttonPreview.setText("Preview");
            buttonPreview.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        WSDLFactory fac = WSDLFactory.newInstance();
                        WSDLWriter wsdlWriter = fac.newWSDLWriter();
                        StringWriter writer = new StringWriter();
                        collect();
                        Definition definition = (Definition)wizardInput;
                        wsdlWriter.writeWSDL(definition, writer);

                        MainWindow mw = TopComponents.getInstance().getMainWindow();
                        new RawWsdlDialog(mw, writer.toString(), definition.getQName().getLocalPart());
                    } catch (WSDLException e1) {
                        e1.printStackTrace();
                    }
                }
            });
            buttonPreview.setEnabled(false);
        }
        return buttonPreview;
    }

    private class RawWsdlDialog extends JDialog {
        private JEditTextArea wsdlTextArea;

        private RawWsdlDialog(JFrame mw, String wsdlString, String title) {

            super(mw, true);
            setTitle(title);

            wsdlTextArea = new JEditTextArea();
            wsdlTextArea.setEditable(false);
            wsdlTextArea.setTokenMarker(new XMLTokenMarker());
            wsdlTextArea.setText(wsdlString);
            wsdlTextArea.setCaretPosition(0);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(wsdlTextArea, BorderLayout.CENTER);

            getContentPane().add(panel, BorderLayout.CENTER);

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
            pack();
            setSize(600, 600);
            Utilities.centerOnScreen(this);
            setVisible(true);
        }
    }


    /**
     *
     * Find the prefix for the namespace within the current definition and
     * create the 'prefix:localName' expression.
     *
     * @param localName the localname
     * @param definition the wsdl definition
     * @return the string containing the NS preefix and the localpart or
     *         the localpart only if the namespace could not be found
     */
    static String prefixedName(QName localName, Definition definition) {
        String prefix = "";
        Set entries = definition.getNamespaces().entrySet();
        for (java.util.Iterator iterator = entries.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            if (entry.getValue().equals(localName.getNamespaceURI())) {
                prefix = (String)entry.getKey() + ":";
            }
        }
        return prefix + localName.getLocalPart();
    }

    private final WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard page has been changed.
         *
         * @param e the event describing the selection change
         */
        public void wizardSelectionChanged(WizardEvent e) {
            WizardStepPanel p = (WizardStepPanel)e.getSource();
            boolean enable =
              (!((p instanceof WsdlCreateOverviewPanel) ||
              (p instanceof WsdlDefinitionPanel)));
            getButtonPreview().setEnabled(enable);
        }

    };
    private JButton buttonPreview;
}
