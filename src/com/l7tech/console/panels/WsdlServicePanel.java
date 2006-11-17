package com.l7tech.console.panels;

import javax.swing.*;
import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

import com.l7tech.common.xml.Wsdl;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class WsdlServicePanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JPanel namePanel;
    private JLabel bindingLabel;
    private JTextField nameField;
    private JTextField portNameField;
    private JTextField portAddressField;
    private Definition definition;
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
        if (settings instanceof Definition) {
            definition = (Definition)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        String s = nameField.getText();
        if (s == null || "".equals(s)) {
            nameField.setText(definition.getQName().getLocalPart() + "Service");
        }
        s = portAddressField.getText();
        if (s == null || "".equals(s)) {
            portAddressField.setText("http://localhost:8080/ws/" + nameField.getText());
        }
        s = portNameField.getText();
        if (s == null || "".equals(s)) {
            portNameField.setText(definition.getQName().getLocalPart() + "Port");
        }
        bindingLabel.setText(getBinding(definition).getQName().getLocalPart());       
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
        if (settings instanceof Definition) {
            definition = (Definition)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        definition.getServices().clear();
        try {
            getService(definition);
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

    private Service getService(Definition def) throws WSDLException {
        Map services = def.getServices();
        Service sv;
        if (services.isEmpty()) {
            sv = def.createService();
            def.addService(sv);
        } else {
            sv = (Service)services.values().iterator().next();
        }
        sv.setQName(new QName(nameField.getText()));
        getPort(sv);
        return sv;
    }

    private Port getPort(Service service) throws WSDLException {
        Map ports = service.getPorts();
        Port port;
        if (ports.isEmpty()) {
            port = definition.createPort();
            service.addPort(port);
        } else {
            port = (Port)ports.values().iterator().next();
        }
        port.setName(portNameField.getText());
        port.setBinding(getBinding(definition));
        collectSoapAddress(port);
        return port;
    }

    private void collectSoapAddress(Port port) throws WSDLException {
        ExtensionRegistry extensionRegistry = definition.getExtensionRegistry();
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

    /**
     * @param def the service definition
     * @return the currently edited binding
     */
    private Binding getBinding(Definition def) {
        Map bindings = def.getBindings();
        if (bindings.isEmpty()) {
            throw new IllegalStateException("Should have at least one binding");
        }
        return (Binding)bindings.values().iterator().next();
    }


}
