package com.l7tech.console.panels;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.WsdlComposer;
import com.l7tech.console.table.WsdlBindingOperationsTableModel;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;

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
          new DefaultComboBoxModel(new String[]{"rpc", "document"});
        portTypeBindingStyle.setModel(model);
        portTypeBindingStyle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    getEditedBinding();
                } catch (WSDLException e1) {
                    throw new RuntimeException(e1);
                }
            }
        });
    }

    /**
     * @return the wizard step description
     */
    @Override
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
    @Override
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
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof WsdlComposer) {
            wsdlComposer = (WsdlComposer)settings;
        } else {
            throw new IllegalArgumentException("Unexpected type " + settings.getClass());
        }
        Binding binding;
        try {
            binding = getEditedBinding();
            if (!StringUtils.isEmpty(binding.getQName().getLocalPart())) {
                portTypeBindingNameField.setText(binding.getQName().getLocalPart());
            }
        } catch (WSDLException e) {
            throw new RuntimeException(e);
        }

        String style = wsdlComposer.getBindingStyle(binding);
        portTypeBindingStyle.setSelectedItem(StringUtils.isEmpty(style)?"RPC":style.toLowerCase());

        String transport = wsdlComposer.getBindingTransportURI(binding);
        if (transport!=null)
            portTypeBindingTransportField.setText(transport);

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
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        try {
            Binding b = getEditedBinding();
            b.setQName(new QName(wsdlComposer.getTargetNamespace(), portTypeBindingNameField.getText()));
            collectSoapBinding(b);
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
        PortType portType = wsdlComposer.getOrCreatePortType();
        Binding binding = wsdlComposer.getOrCreateBinding();

        for (Object o : portType.getOperations()) {
            Operation op = (Operation) o;

            //noinspection unchecked
            if(!isInBinding(binding.getBindingOperations(), op)) {
                BindingOperation bop = wsdlComposer.createBindingOperation();
                bop.setName(op.getName());
                bop.setOperation(op);

                // binding input
                if ( op.getInput() != null ) {
                    BindingInput bi = wsdlComposer.createBindingInput();
                    bi.setName(op.getInput().getName());
                    bi.addExtensibilityElement(getSoapBody());
                    bop.setBindingInput(bi);
                }

                // binding output
                if ( op.getOutput() != null ) {
                    BindingOutput bout = wsdlComposer.createBindingOutput();
                    bout.setName(op.getOutput().getName());
                    bout.addExtensibilityElement(getSoapBody());
                    bop.setBindingOutput(bout);
                }

                // soap action
                String action = portType.getQName().getLocalPart() + "#" + bop.getName();
                ExtensibilityElement ee;
                ExtensionRegistry extensionRegistry = wsdlComposer.getExtensionRegistry();
                ee = extensionRegistry.createExtension(BindingOperation.class, new QName(Wsdl.WSDL_SOAP_NAMESPACE, "operation"));
                if (ee instanceof SOAPOperation) {
                    SOAPOperation sop = (SOAPOperation) ee;
                    sop.setSoapActionURI(action);
                } else {
                    throw new RuntimeException("expected SOAPOperation, received " + ee.getClass());
                }
                bop.addExtensibilityElement(ee);

                binding.addBindingOperation(bop);                
            }
        }

        return binding;
    }

    /**
     * Check if a binding operation exists for the given operation
     */
    private boolean isInBinding(Collection<BindingOperation> bindingOperations, Operation operation) {
        boolean exists = false;
        for (BindingOperation bindingOperation : bindingOperations) {
            if (bindingOperation.getOperation() != null) {
                Operation toCheck = bindingOperation.getOperation();
                if (toCheck.getName() != null && toCheck.getName().equals(operation.getName())) {
                    exists = true;
                }
            }
        }
        return exists;
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
              Arrays.asList("http://schemas.xmlsoap.org/soap/encoding/");
            sob.setEncodingStyles(encodingStyles);
        } else {
            throw new RuntimeException("expected SOAPBody, received " + ee.getClass());
        }
        return ee;
    }

    private void collectSoapBinding(Binding binding) throws WSDLException {
        wsdlComposer.setBindingStyle(binding, portTypeBindingStyle.getSelectedItem().toString());
        wsdlComposer.setBindingTransportURI(binding, portTypeBindingTransportField.getText());
    }
}
