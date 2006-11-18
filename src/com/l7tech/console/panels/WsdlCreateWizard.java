package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.WsdlUtils;
import com.l7tech.console.xmlviewer.Viewer;
import org.dom4j.DocumentException;
import org.xml.sax.SAXParseException;

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
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

/**
 * The <code>Wizard</code> that drives the wizard step panels.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WsdlCreateWizard extends Wizard {

    //    
    private static final String XSD_NAME_SPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String SOAP_NAME_SPACE = "http://schemas.xmlsoap.org/wsdl/soap/";
    private static final String DEFAULT_NAME_SPACE = "http://schemas.xmlsoap.org/wsdl/";

    //
    private JButton buttonPreview;

    public WsdlCreateWizard(Frame parent, WizardStepPanel panel) throws WSDLException {
        super(parent, panel);
        setResizable(true);
        setTitle("Create WSDL Wizard");

        // initialize the WSDL definition
        initModel();
        collect();

        getButtonHelp().addActionListener(new ActionListener() {
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
                    // ensure model is up to date
                    getSelectedWizardPanel().storeSettings(wizardInput);
                    collect();

                    // 
                    try {
                        WSDLFactory fac = WsdlUtils.getWSDLFactory();
                        WSDLWriter wsdlWriter = fac.newWSDLWriter();
                        StringWriter writer = new StringWriter();
                        Definition definition = (Definition) wizardInput;
                        wsdlWriter.writeWSDL(definition, writer);

                        Frame mw = TopComponents.getInstance().getTopParent();
                        new RawWsdlDialog(mw, writer.toString(), definition.getQName().getLocalPart());
                    } catch (WsdlUtils.WSDLFactoryNotTrustedException wfnte) {
                        TopComponents.getInstance().showNoPrivilegesErrorMessage();    
                    } catch (WSDLException e1) {
                        throw new RuntimeException(e1);
                    } catch (DocumentException e1) {
                        throw new RuntimeException(e1);
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    } catch (SAXParseException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
        }
        return buttonPreview;
    }

    /**
     * Override finish so all panels get the chance to update their settings 
     */
    protected void finish(ActionEvent evt) {
        getSelectedWizardPanel().storeSettings(wizardInput);
        collect();
        super.finish(evt);
    }

    private void initModel() throws WSDLException {
        Definition definition = WsdlUtils.getWSDLFactory().newDefinition();

        definition.setQName(new QName("NewService"));
        definition.setTargetNamespace("http://tempuri.org/");
        definition.addNamespace("tns", definition.getTargetNamespace());                      
        definition.addNamespace("xsd", XSD_NAME_SPACE);
        definition.addNamespace("soap", SOAP_NAME_SPACE);
        definition.addNamespace(null, DEFAULT_NAME_SPACE);

        wizardInput = definition;
    }

    private class RawWsdlDialog extends JDialog {

        private RawWsdlDialog(Frame mw, String wsdlString, String title) throws DocumentException, IOException, SAXParseException {

            super(mw, true);
            setTitle(title);
            Viewer viewer = Viewer.createMessageViewer(wsdlString);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(viewer, BorderLayout.CENTER);

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
     * Find the prefix for the namespace within the current definition and
     * create the 'prefix:localName' expression.
     *
     * @param localName  the localname
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

}
