package com.l7tech.console.panels;

import com.ibm.wsdl.DefinitionImpl;
import com.l7tech.common.gui.util.Utilities;

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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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

    public WsdlCreateWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        wizardInput = new DefinitionImpl();
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
                        wsdlWriter.writeWSDL((Definition)wizardInput, writer);
                        new RawWsdlDialog(writer.toString(), "wsdl title here....");
                    } catch (WSDLException e1) {
                        //
                    }
                }
            });
        }
        return buttonPreview;
    }

    private class RawWsdlDialog extends JFrame {
        private JEditTextArea wsdlTextArea;

        private RawWsdlDialog(String wsdlString, String title) {
            super(title);

            wsdlTextArea = new JEditTextArea();
            wsdlTextArea.setEditable(false);
            wsdlTextArea.setTokenMarker(new XMLTokenMarker());
            wsdlTextArea.setText(wsdlString);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(wsdlTextArea, BorderLayout.CENTER);

            getContentPane().add(panel, BorderLayout.CENTER);

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
            pack();
            Utilities.centerOnScreen(this);
            setVisible(true);
        }
    }

    /**
     *
     * Find the prefix for the namespace within the current definiotn and
     * create the 'prefix:localName' string.
     *
     * @param localName the localname
     * @param definition the wsdl definition
     * @return the string containing the NS preefix and the localpart or
     *         the localpart only if the namespace could not be found
     */
    static String prefixedName(QName localName, Definition definition) {
        String prefix = "";
        Set entries = definition.getNamespaces().entrySet();
        for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            if (entry.getValue().equals(localName.getNamespaceURI())) {
                prefix = (String)entry.getKey() + ":";
            }
        }
        return prefix + localName.getLocalPart();
    }

    private JButton buttonPreview;
}
