package com.l7tech.console.panels;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.WsdlComposer;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class WsdlServicePanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JLabel bindingLabel;
    private JTextField nameField;
    private JTextField portNameField;
    private JTextField portAddressField;
    private WsdlComposer wsdlComposer;
    private JLabel panelHeader;

    public WsdlServicePanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        panelHeader.setFont(new java.awt.Font("Dialog", 1, 16));
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "<html>" +
               "The \"service\" element defines the Web service endpoint URL and connection port." +
               "</html>";
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        WsdlComposer wsdlComposer = null;
        if (settings instanceof WsdlComposer) {
            wsdlComposer = (WsdlComposer) settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }

        Service svc = wsdlComposer.getService();
        String s = nameField.getText();
        if (s == null || "".equals(s)) {
            String name = "Service";
            if (svc != null)
                if (StringUtils.isNotEmpty(svc.getQName().getLocalPart()))
                    name = svc.getQName().getLocalPart();

            nameField.setText(name);
        }

        Port port = wsdlComposer.getSupportedSoapPort(svc);
        s = portAddressField.getText();
        if (s == null || "".equals(s)) {
            String address = "http://localhost:8080/ws/" + nameField.getText();
            if (port != null) {
                List ees = port.getExtensibilityElements();
                for (Object obj : ees) {
                    ExtensibilityElement ee = (ExtensibilityElement) obj;
                    if (StringUtils.equals(ee.getElementType().getNamespaceURI(), Wsdl.WSDL_SOAP_NAMESPACE)  && StringUtils.equals(ee.getElementType().getLocalPart(), "address")) {
                        SOAPAddress sa = (SOAPAddress) ee;
                        address = sa.getLocationURI();
                        break;
                    }
                }
            }
            portAddressField.setText(address);
        }
        s = portNameField.getText();
        if (s == null || "".equals(s)) {
            if (port != null && StringUtils.isNotEmpty(port.getName()))
                portNameField.setText(port.getName());
            else
                portNameField.setText(wsdlComposer.getQName().getLocalPart() + "Port");
        }
        bindingLabel.setText(wsdlComposer.getBinding().getQName().getLocalPart());       
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof WsdlComposer) {
            wsdlComposer = (WsdlComposer)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }

        try {
            getService();
        } catch (WSDLException e) {
            //todo: error manager
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Service";
    }

    private Service getService() throws WSDLException {
        Service sv = wsdlComposer.getService();
        if (sv == null)
            sv = wsdlComposer.createService();

        sv.setQName(new QName(nameField.getText()));
        getPort(wsdlComposer, sv);
        wsdlComposer.setService(sv);
        return sv;
    }

    private Port getPort(WsdlComposer wsdlComposer, Service service) throws WSDLException {
        Port port = wsdlComposer.getSupportedSoapPort(service);
        if (port == null){
            port = wsdlComposer.createPort();
            service.addPort(port);
        }

        port.setName(portNameField.getText());
        port.setBinding(wsdlComposer.getBinding());
        collectSoapAddress(wsdlComposer, port);
        return port;
    }

    private void collectSoapAddress(WsdlComposer wsdlComposer, Port port) throws WSDLException {
        ExtensionRegistry extensionRegistry = wsdlComposer.getExtensionRegistry();
        ExtensibilityElement ee;

        java.util.List<SOAPAddress> remove = new ArrayList<SOAPAddress>();
        java.util.List extensibilityElements = port.getExtensibilityElements();
        for (Object o : extensibilityElements) {
            if (o instanceof SOAPAddress) {
                remove.add((SOAPAddress) o);
            }
        }
        extensibilityElements.removeAll(remove);

        ee = extensionRegistry.createExtension(Port.class,
          new QName(Wsdl.WSDL_SOAP_NAMESPACE, "address"));
        if (ee instanceof SOAPAddress) {
            SOAPAddress sa = (SOAPAddress)ee;
            sa.setLocationURI(portAddressField.getText());
        } else {
            throw new RuntimeException("expected SOAPOperation, received " + ee.getClass());
        }
        port.addExtensibilityElement(ee);
    }
}
