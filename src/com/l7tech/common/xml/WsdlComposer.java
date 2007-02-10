package com.l7tech.common.xml;

import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.wsdl.factory.WSDLFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: megery
 * Date: Feb 7, 2007
 * Time: 10:28:18 AM
 */
public class WsdlComposer {
//    List<BindingOperation> changesToApply;
    private Definition outputWsdl;
    WSDLFactory wsdlFactory;
    private ExtensionRegistry extensionRegistry;

    public WsdlComposer(Definition def) {
//        changesToApply = new ArrayList<BindingOperation>();
        outputWsdl = def;
        try {
            wsdlFactory = WSDLFactory.newInstance();
            extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();
        } catch (WSDLException e) {
            e.printStackTrace();
        }
    }

    public Definition getOutputWsdl() {
        buildOutput();
        return outputWsdl;
    }

    private void buildOutput() {
//        System.out.println("The following changes will be applied");
//        for (BindingOperation bindingOperation : changesToApply) {
//            System.out.println(bindingOperation.getName());
//        }
    }

    public void addBindingOperation(BindingOperation oldBop, WsdlHolder sourceWsdl) throws WSDLException, IOException, SAXException {
        if (oldBop == null) return;

        Definition sourceDef = sourceWsdl.wsdl.getDefinition();

        Types inputTypes = sourceDef.getTypes();
        Types outputTypes = outputWsdl.getTypes();
        if (outputTypes == null) {
            outputTypes = outputWsdl.createTypes();
        }

        for (Object o : inputTypes.getExtensibilityElements()) {
            ExtensibilityElement sourceExtElement = (ExtensibilityElement) o;
            ExtensionSerializer serializer = extensionRegistry.querySerializer(sourceExtElement.getClass(), sourceExtElement.getElementType());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.marshall(sourceExtElement.getClass(), sourceExtElement.getElementType(), sourceExtElement, new PrintWriter(baos, true), sourceWsdl.wsdl.getDefinition(), extensionRegistry);

            byte[] bytes = baos.toByteArray();
            Document doc  = XmlUtil.parse(new ByteArrayInputStream(bytes));

            ExtensionDeserializer deserializer = extensionRegistry.queryDeserializer(sourceExtElement.getClass(), sourceExtElement.getElementType());
            ExtensibilityElement newElem = deserializer.unmarshall(sourceExtElement.getClass(), sourceExtElement.getElementType(), doc.getDocumentElement(), sourceDef, extensionRegistry);

            outputTypes.addExtensibilityElement(newElem);
        }

        outputWsdl.setTypes(outputTypes);

        BindingOperation newBop = outputWsdl.createBindingOperation();
        copyBindingOperation(oldBop, newBop);

        addOperationsForBindingOperation(newBop);

        Map bindings = outputWsdl.getBindings();
        for (Object key : bindings.keySet()) {
            ((Binding)bindings.get(key)).addBindingOperation(newBop);
        }
    }

    private void addOperationsForBindingOperation(BindingOperation newBop) {
        Map portTypes = outputWsdl.getPortTypes();
        for (Object portTypeKey: portTypes.keySet()) {
            PortType pt = (PortType) portTypes.get(portTypeKey);
            Operation op = newBop.getOperation();
            addMessagesForOperation(op);
            pt.addOperation(op);
        }
    }

    private void addMessagesForOperation(Operation op) {
        Input input = op.getInput();
        Output output = op.getOutput();
        Map faults = op.getFaults();

        Message m = input.getMessage();
        m.getParts();
        if (input != null)
            outputWsdl.addMessage(input.getMessage());

        if (output != null)
            outputWsdl.addMessage(output.getMessage());

        for (Object faultKey : faults.keySet()) {
            Fault f = (Fault) faults.get(faultKey);
            outputWsdl.addMessage(f.getMessage());
        }
    }

    private void copyBindingOperation(BindingOperation oldBop, BindingOperation newBop) {
        newBop.setName(oldBop.getName());
        newBop.setBindingInput(oldBop.getBindingInput());
        newBop.setBindingOutput(oldBop.getBindingOutput());
        newBop.setDocumentationElement(oldBop.getDocumentationElement());
        newBop.setOperation(oldBop.getOperation());

        Map bindingFaults = oldBop.getBindingFaults();
        Set keys = bindingFaults.keySet();
        for (Object key: keys) {
            Object value = bindingFaults.get(key);
            BindingFault bf = (BindingFault) value;
            newBop.addBindingFault(bf);
        }

        List extElements = oldBop.getExtensibilityElements();
        for (Object extElementObj : extElements) {
            ExtensibilityElement exel = (ExtensibilityElement) extElementObj;
            newBop.addExtensibilityElement(exel);
        }

    }

    public static class WsdlHolder {
        public Wsdl wsdl;
        String wsdlLocation;

        public WsdlHolder(Wsdl wsdl, String wsdlLocation) {
            this.wsdl = wsdl;
            this.wsdlLocation = wsdlLocation;
        }

        public String toString() {
            return wsdlLocation;
        }

        public int hashCode() {
            return wsdl.hashCode();
        }

        public boolean equals(Object obj) {
            return wsdl.equals(obj);
        }
    }
}
