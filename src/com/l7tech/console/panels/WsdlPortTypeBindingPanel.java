package com.l7tech.console.panels;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.WsdlComposer;
import com.l7tech.console.table.WsdlBindingOperationsTableModel;
import org.apache.commons.lang.StringUtils;

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

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WsdlPortTypeBindingPanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JTextField portTypeBindingNameField;
    private JTable bindingOperationsTable;
    private JScrollPane bindingOperationsTableScrollPane;

    private JTextField portTypeBindingTransportField; // http://schemas.xmlsoap.org/soap/http
    private JComboBox portTypeBindingStyle;

    private JLabel panelHeader;
    private WsdlComposer wsdlComposer;

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
        if (settings instanceof WsdlComposer) {
            wsdlComposer = (WsdlComposer)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        Binding binding = null;
        try {
            binding = getEditedBinding();
            if (!StringUtils.isEmpty(binding.getQName().getLocalPart())) {
                portTypeBindingNameField.setText(binding.getQName().getLocalPart());
            }
        } catch (WSDLException e) {
            throw new RuntimeException(e);
        }
        WsdlBindingOperationsTableModel model =
          new WsdlBindingOperationsTableModel(wsdlComposer, binding);

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
        try {
            Binding b = getEditedBinding();
            b.setQName(new QName(wsdlComposer.getTargetNamespace(), portTypeBindingNameField.getText()));
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
        Binding binding = wsdlComposer.getBinding();
        if (binding == null) {
            binding = wsdlComposer.createBinding();
            wsdlComposer.addBinding(binding);
            binding.setPortType(portType);
            binding.setUndefined(false);
            binding.setQName(new QName(wsdlComposer.getTargetNamespace(), portTypeBindingNameField.getText()));            
        }
        collectSoapBinding(binding);

        if (binding.getBindingOperations().isEmpty()) {
            for (Object o : portType.getOperations()) {
                Operation op = (Operation)o;
                BindingOperation bop = wsdlComposer.createBindingOperation();
                bop.setName(op.getName());
                bop.setOperation(op);

                // binding input
                BindingInput bi = wsdlComposer.createBindingInput();
                bi.setName(op.getInput().getName());
                bi.addExtensibilityElement(getSoapBody());
                bop.setBindingInput(bi);

                // binding output
                BindingOutput bout = wsdlComposer.createBindingOutput();
                bout.setName(op.getOutput().getName());
                bout.addExtensibilityElement(getSoapBody());
                bop.setBindingOutput(bout);
                binding.addBindingOperation(bop);
                // soap action
                String action =
                  portType.getQName().getLocalPart() + "#" + bop.getName();
                ExtensibilityElement ee = null;
                ExtensionRegistry extensionRegistry = wsdlComposer.getExtensionRegistry();
                ee = extensionRegistry.createExtension(BindingOperation.class, new QName(Wsdl.WSDL_SOAP_NAMESPACE, "operation"));
                if (ee instanceof SOAPOperation) {
                    SOAPOperation sop = (SOAPOperation)ee;
                    sop.setSoapActionURI(action);
                } else {
                    throw new RuntimeException("expected SOAPOperation, received " + ee.getClass());
                }
                bop.addExtensibilityElement(ee);
            }
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
        ExtensionRegistry extensionRegistry = wsdlComposer.getExtensionRegistry();
        ee = extensionRegistry.createExtension(BindingInput.class, new QName(Wsdl.WSDL_SOAP_NAMESPACE, "body"));
        if (ee instanceof SOAPBody) {
            SOAPBody sob = (SOAPBody)ee;
            sob.setNamespaceURI(wsdlComposer.getTargetNamespace());
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
          wsdlComposer.getExtensionRegistry();

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
        ee = extensionRegistry.createExtension(Binding.class, new QName(Wsdl.WSDL_SOAP_NAMESPACE, "binding"));
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
        PortType pt = wsdlComposer.getPortType();
        if (pt == null) {
            throw new IllegalStateException("Should have at least one port type");
        }
        return pt;
    }


}
