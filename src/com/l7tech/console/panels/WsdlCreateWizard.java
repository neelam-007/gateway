package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.WsdlComposer;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.WsdlUtils;
import com.l7tech.console.xmlviewer.Viewer;
import org.dom4j.DocumentException;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
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
    public static final String IMPORT_SERVICE_DOCUMENT_TYPE ="VIRTUALWSDL-SOURCE";
    //
    private JButton buttonPreview;
    private WsdlComposer wsdlComposer;

    public WsdlCreateWizard(Frame parent, WizardStepPanel panel, Definition defToUse, Set<WsdlComposer.WsdlHolder> originalWsdls) throws WSDLException {
        super(parent, panel);
        initialise(defToUse, originalWsdls);
    }

    public WsdlCreateWizard(Frame parent, WizardStepPanel panel) throws WSDLException {
        super(parent, panel);
        initialise(null, null);
    }

    private void initialise(Definition def, Set<WsdlComposer.WsdlHolder> originalWsdls) throws WSDLException {
        setResizable(true);
        setTitle(def == null?"Create WSDL Wizard":"Edit WSDL Wizard");

        // initialize the WSDL definition
        initModel(def, originalWsdls);
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
                        ExtensionRegistry reg =  Wsdl.disableSchemaExtensions(fac.newPopulatedExtensionRegistry());
                        WSDLWriter wsdlWriter = fac.newWSDLWriter();

                        StringWriter writer = new StringWriter();
                        Definition definition = wsdlComposer.buildOutputWsdl();
                        definition.setExtensionRegistry(reg);
                        wsdlWriter.writeWSDL(definition, writer);

                        Frame mw = TopComponents.getInstance().getTopParent();
                        DialogDisplayer.display(
                                new RawWsdlDialog(mw, writer.toString(), definition.getQName().getLocalPart()));
                    } catch (WsdlUtils.WSDLFactoryNotTrustedException wfnte) {
                        TopComponents.getInstance().showNoPrivilegesErrorMessage();    
                    } catch (WSDLException e1) {
                        throw new RuntimeException(e1);
                    } catch (DocumentException e1) {
                        throw new RuntimeException(e1);
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    } catch (SAXException e1) {
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

    private void initModel(Definition defToUse, Set<WsdlComposer.WsdlHolder> originalWsdls) throws WSDLException {

        if (defToUse == null) {
            wsdlComposer = new WsdlComposer();
//            defToUse = WsdlUtils.getWSDLFactory().newDefinition();

            wsdlComposer.setQName(new QName("NewService"));
            String tns = "http://tempuri.org/";
            wsdlComposer.setTargetNamespace(tns);
            wsdlComposer.addNamespace("tns", tns);
            wsdlComposer.addNamespace("xsd", XSD_NAME_SPACE);
            wsdlComposer.addNamespace("soap", SOAP_NAME_SPACE);
            wsdlComposer.addNamespace(null, DEFAULT_NAME_SPACE);
        } else {
            wsdlComposer = new WsdlComposer(defToUse);
        }

        if (originalWsdls == null || originalWsdls.size() == 0) {

        } else {
            for (WsdlComposer.WsdlHolder sourceWsdl : originalWsdls) {
                wsdlComposer.addSourceWsdl(sourceWsdl);
            }
        }

        wizardInput = wsdlComposer;
    }

    private class RawWsdlDialog extends JDialog {

        private RawWsdlDialog(Frame mw, String wsdlString, String title) throws DocumentException, IOException, SAXParseException {

            super(mw, true);
            setTitle(title);
            Viewer viewer = Viewer.createMessageViewer(wsdlString);
            viewer.setPreferredSize(new Dimension(580, 640));
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(viewer, BorderLayout.CENTER);

            getContentPane().add(panel, BorderLayout.CENTER);

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
            pack();
            Utilities.centerOnScreen(this);
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
    static String prefixedName(QName localName, WsdlComposer definition) {
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
