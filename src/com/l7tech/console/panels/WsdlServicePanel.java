package com.l7tech.console.panels;

import com.ibm.wsdl.extensions.soap.SOAPConstants;

import javax.swing.*;
import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

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


    public WsdlServicePanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "<html><b>Service</b><br>" +
          "The service element defines the address (URI) of an endpoint" +
          " and the port where the Web service can be reached.</html>";
    }

    /**
     * Test whether the step is finished and it is safe to proceed to the next
     * one.
     * If the step is valid, the "Next" (or "Finish") button will be enabled.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next
     * one.
     *
     * @return true if the panel is valid, false otherwis
     */

    public boolean canAdvance() {
        return false;
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */

    public boolean canFinish() {
        return true;
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
        ExtensibilityElement ee = null;

        java.util.List remove = new ArrayList();
        java.util.List extensibilityElements = port.getExtensibilityElements();
        for (Iterator iterator = extensibilityElements.iterator(); iterator.hasNext();) {
            Object o = (Object)iterator.next();
            if (ee instanceof SOAPAddress) {
                remove.add(o);
            }
        }
        extensibilityElements.removeAll(remove);

        ee = extensionRegistry.createExtension(Port.class,
          SOAPConstants.Q_ELEM_SOAP_ADDRESS);
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
        Binding binding = (Binding)bindings.values().iterator().next();
        return binding;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        namePanel = _2;
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(12, 4, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JTextField _3;
        _3 = new JTextField();
        nameField = _3;
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JLabel _4;
        _4 = new JLabel();
        _4.setText("Name");
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 8, 1, 0, 0, null, null, null));
        final JLabel _5;
        _5 = new JLabel();
        _5.setText("Port");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, 8, 1, 0, 0, null, null, null));
        final JTextField _6;
        _6 = new JTextField();
        portNameField = _6;
        _6.setText("");
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JLabel _7;
        _7 = new JLabel();
        _7.setText("Service");
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(100, -1), new Dimension(100, -1), null));
        final JLabel _8;
        _8 = new JLabel();
        _8.setText("Address");
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(9, 1, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _9;
        _9 = new JTextField();
        portAddressField = _9;
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final com.intellij.uiDesigner.core.Spacer _10;
        _10 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_10, new com.intellij.uiDesigner.core.GridConstraints(10, 2, 1, 1, 0, 2, 1, 6, null, null, null));
        final JLabel _11;
        _11 = new JLabel();
        _11.setText("Binding");
        _2.add(_11, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _12;
        _12 = new JLabel();
        bindingLabel = _12;
        _12.setText("");
        _2.add(_12, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_13, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        final com.intellij.uiDesigner.core.Spacer _14;
        _14 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_14, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        final com.intellij.uiDesigner.core.Spacer _15;
        _15 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_15, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        final com.intellij.uiDesigner.core.Spacer _16;
        _16 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_16, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), null));
    }


}
