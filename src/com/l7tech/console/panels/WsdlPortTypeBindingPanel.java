package com.l7tech.console.panels;

import com.ibm.wsdl.extensions.soap.SOAPConstants;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.console.table.WsdlBindingOperationsTableModel;

import javax.swing.*;
import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WsdlPortTypeBindingPanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JTextField portTypeBindingNameField;
    private JLabel portTypeName;
    private JTable bindingOperationsTable;
    private JScrollPane bindingOperationsTableScrollPane;

    private JTextField portTypeBindingTransportField; // http://schemas.xmlsoap.org/soap/http
    private JComboBox portTypeBindingStyle;
    private Definition definition;
    private JLabel panelHeader;

    public WsdlPortTypeBindingPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        panelHeader.setFont(new java.awt.Font("Dialog", 1, 16));
        setLayout(new BorderLayout());
        JViewport viewport = bindingOperationsTableScrollPane.getViewport();
        viewport.setBackground(bindingOperationsTable.getBackground());
        bindingOperationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bindingOperationsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        add(mainPanel, BorderLayout.CENTER);
        ComboBoxModel model =
          new DefaultComboBoxModel(new String[]{"RPC", "Document"});
        portTypeBindingStyle.setModel(model);
        portTypeBindingStyle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    collectSoapBinding(getEditedBinding());
                } catch (WSDLException e1) {
                    throw new RuntimeException(e1);
                }
            }
        });
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "<html>" +
               "The \"port type binding\" element contains binding definitions that specify Web service " +
               "message formatting and protocol details. The \"Style\" attribute defines the binding style, " +
               "the \"Transport\" attribute indicates which SOAP transport corresponds to the binding, and the " +
               "\"SOAP Action\" column in the Operations window specifies the value of the SOAPAction http header " +
               "for the operation." +
               "</html>";
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
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */

    public boolean canFinish() {
        return false;
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Bindings";
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
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
        PortType portType = getPortType();
//        portTypeName.setText(portType.getQName().getLocalPart());

        String s = portTypeBindingNameField.getText();
        if (s == null || "".equals(s)) {
            portTypeBindingNameField.setText(portType.getQName().getLocalPart() + "Binding");
        }

        Binding binding = null;
        try {
            binding = getEditedBinding();
        } catch (WSDLException e) {
            throw new RuntimeException(e);
        }
        WsdlBindingOperationsTableModel model =
          new WsdlBindingOperationsTableModel(definition, binding);

        bindingOperationsTable.setModel(model);
        bindingOperationsTable.getTableHeader().setReorderingAllowed(false);
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
        Binding binding = null;
        try {
            binding = getEditedBinding();
        } catch (WSDLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * returns the binding that is being edited (single bindng support only)
     * creats the binding first time
     *
     * @return the binding
     */
    private Binding getEditedBinding() throws WSDLException {
        PortType portType = getPortType();
        Map bindings = definition.getBindings();
        Binding binding;
        if (bindings.isEmpty()) {
            binding = definition.createBinding();
            binding.setQName(new QName(definition.getTargetNamespace(),
              portTypeBindingNameField.getText()));
            binding.setPortType(portType);
            binding.setUndefined(false);
            collectSoapBinding(binding);
            definition.addBinding(binding);
        } else {
            binding = (Binding)bindings.values().iterator().next();
            definition.removeBinding(binding.getQName());
            return getEditedBinding();
        }
        binding.getBindingOperations().clear();
        for (Iterator it = portType.getOperations().iterator(); it.hasNext();) {
            Operation op = (Operation)it.next();
            BindingOperation bop = definition.createBindingOperation();
            bop.setName(op.getName());
            bop.setOperation(op);

            // binding input
            BindingInput bi = definition.createBindingInput();
            bi.setName(op.getInput().getName());
            bi.addExtensibilityElement(getSoapBody());
            bop.setBindingInput(bi);

            // binding output
            BindingOutput bout = definition.createBindingOutput();
            bout.setName(op.getOutput().getName());
            bout.addExtensibilityElement(getSoapBody());
            bop.setBindingOutput(bout);
            binding.addBindingOperation(bop);
            // soap action
            String action =
              portType.getQName().getLocalPart() + "#" + bop.getName();
            ExtensibilityElement ee = null;
            ExtensionRegistry extensionRegistry = definition.getExtensionRegistry();
            ee = extensionRegistry.createExtension(BindingOperation.class, SOAPConstants.Q_ELEM_SOAP_OPERATION);
            if (ee instanceof SOAPOperation) {
                SOAPOperation sop = (SOAPOperation)ee;
                sop.setSoapActionURI(action);
            } else {
                throw new RuntimeException("expected SOAPOperation, received " + ee.getClass());
            }
            bop.addExtensibilityElement(ee);
        }

        return binding;
    }

    /**
     * creates the default soap body element with the soap encoded
     * style
     *
     * @return the extensibility element
     * @throws WSDLException
     */
    private ExtensibilityElement getSoapBody() throws WSDLException {
        ExtensibilityElement ee;
        ExtensionRegistry extensionRegistry = definition.getExtensionRegistry();
        ee = extensionRegistry.createExtension(BindingInput.class, SOAPConstants.Q_ELEM_SOAP_BODY);
        if (ee instanceof SOAPBody) {
            SOAPBody sob = (SOAPBody)ee;
            sob.setNamespaceURI(definition.getTargetNamespace());
            sob.setUse("encoded"); //soap encoded
            java.util.List encodingStyles =
              Arrays.asList(new String[]{"http://schemas.xmlsoap.org/soap/encoding/"});
            sob.setEncodingStyles(encodingStyles);
        } else {
            throw new RuntimeException("expected SOAPBody, received " + ee.getClass());
        }
        return ee;
    }

    private void collectSoapBinding(Binding binding) throws WSDLException {
        ExtensionRegistry extensionRegistry =
          definition.getExtensionRegistry();

        java.util.List extElements = binding.getExtensibilityElements();
        java.util.List remove = new ArrayList();
        for (Iterator iterator = extElements.iterator(); iterator.hasNext();) {
            ExtensibilityElement ee = (ExtensibilityElement)iterator.next();
            if (ee instanceof SOAPBinding) {
                remove.add(ee);
            }
        }
        extElements.removeAll(remove);

        ExtensibilityElement ee = null;
        ee = extensionRegistry.createExtension(Binding.class, SOAPConstants.Q_ELEM_SOAP_BINDING);
        if (ee instanceof SOAPBinding) {
            SOAPBinding sb = (SOAPBinding)ee;
            sb.setTransportURI(portTypeBindingTransportField.getText());
            sb.setStyle(portTypeBindingStyle.getSelectedItem().toString());
            binding.addExtensibilityElement(sb);
        } else {
            throw new RuntimeException("expected SOAPOperation, received " + ee.getClass());
        }
    }

    /**
     * Retrieve the port type. This method expects the port type, already collected
     * and throws <code>IllegalStateException</code> if port type not present.
     *
     * @return the port type
     * @throws IllegalStateException if there is no port type in the definition
     */
    private PortType getPortType() throws IllegalStateException {
        Map portTypes = definition.getPortTypes();
        if (portTypes.isEmpty()) {
            throw new IllegalStateException("Should have at least one port type");
        }
        return (PortType)portTypes.values().iterator().next();
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(11, 4, new Insets(10, 5, 5, 5), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("Name:");
        panel1.add(label1, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        portTypeBindingNameField = new JTextField();
        portTypeBindingNameField.setMargin(new Insets(0, 0, 0, 0));
        panel1.add(portTypeBindingNameField, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        panelHeader = new JLabel();
        panelHeader.setText("Port Type Binding");
        panel1.add(panelHeader, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Operations:");
        panel1.add(label2, new GridConstraints(9, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("Style:");
        panel1.add(label3, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        portTypeBindingStyle = new JComboBox();
        panel1.add(portTypeBindingStyle, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), null));
        final JLabel label4 = new JLabel();
        label4.setText("Transport:");
        panel1.add(label4, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        portTypeBindingTransportField = new JTextField();
        portTypeBindingTransportField.setEditable(false);
        portTypeBindingTransportField.setText("http://schemas.xmlsoap.org/soap/http");
        panel1.add(portTypeBindingTransportField, new GridConstraints(7, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(8, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        final Spacer spacer4 = new Spacer();
        panel1.add(spacer4, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        bindingOperationsTableScrollPane = new JScrollPane();
        panel1.add(bindingOperationsTableScrollPane, new GridConstraints(10, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        bindingOperationsTable = new JTable();
        bindingOperationsTableScrollPane.setViewportView(bindingOperationsTable);
    }
}
