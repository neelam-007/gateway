package com.l7tech.console.panels;

import com.ibm.wsdl.extensions.soap.SOAPConstants;
import com.l7tech.console.table.WsdlBindingOperationsTableModel;

import javax.swing.*;
import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;

/**
 *
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

    public WsdlPortTypeBindingPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JViewport viewport = bindingOperationsTableScrollPane.getViewport();
        viewport.setBackground(bindingOperationsTable.getBackground());
        bindingOperationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bindingOperationsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        add(mainPanel, BorderLayout.CENTER);
        ComboBoxModel model =
          new DefaultComboBoxModel(new String[]{"rpc", "document"});
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
        return "<html><b>Port Type Binding</b><br>" +
          "The <i>binding</i> element contains binding definitions that specify message formatting and" +
          "protocol details. " +
          "The <i>soapAction</i> attribute specifies the value of the SOAPAction header for this operation." +
          "The transport attribute indicates which SOAP transport this binding corresponds to. The value " +
          "http://schemas.xmlsoap.org/soap/http corresponds to the HTTP binding in the SOAP specification." +
          "The style attribute is set to either rpc or document." +
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
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof Definition) {
            definition = (Definition)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        PortType portType = getPortType();
        portTypeName.setText(portType.getQName().getLocalPart());

        String s = portTypeBindingNameField.getText();
        if (s == null || "".equals(s)) {
            portTypeBindingNameField.setText(portTypeName.getText() + "Binding");
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
     *
     * This is a noop version that subclasses implement.
     *
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     * @param settings the object representing wizard panel state
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
              Arrays.asList(new String[] {"http://schemas.xmlsoap.org/soap/encoding/"});
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
// do not edit this generated initializer!!! do not add your code here!!!
        $$$setupUI$$$();
    }

    /** generated code, do not edit or call this method manually !!! */
    private void $$$setupUI$$$() {
        JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(11, 4, new Insets(10, 5, 5, 5), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _3;
        _3 = new JLabel();
        _3.setText("Name");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _4;
        _4 = new JTextField();
        portTypeBindingNameField = _4;
        _4.setMargin(new Insets(0, 0, 0, 0));
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JLabel _5;
        _5 = new JLabel();
        _5.setText("Port Type Binding");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _6;
        _6 = new JLabel();
        _6.setText("Operations");
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _7;
        _7 = new JLabel();
        _7.setText("Style");
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _8;
        _8 = new JLabel();
        portTypeName = _8;
        _8.setText("<Port Type Name>");
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JComboBox _9;
        _9 = new JComboBox();
        portTypeBindingStyle = _9;
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1, 8, 0, 2, 0, new Dimension(100, -1), new Dimension(100, -1), new Dimension(-1, -1)));
        JLabel _10;
        _10 = new JLabel();
        _10.setText("Transport");
        _2.add(_10, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _11;
        _11 = new JTextField();
        portTypeBindingTransportField = _11;
        _11.setEditable(false);
        _11.setText("http://schemas.xmlsoap.org/soap/http");
        _2.add(_11, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _12;
        _12 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_12, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_13, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _14;
        _14 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_14, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _15;
        _15 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_15, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        JScrollPane _16;
        _16 = new JScrollPane();
        bindingOperationsTableScrollPane = _16;
        _2.add(_16, new com.intellij.uiDesigner.core.GridConstraints(10, 2, 1, 1, 0, 3, 7, 7, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTable _17;
        _17 = new JTable();
        bindingOperationsTable = _17;
        _16.setViewportView(_17);
    }


}
