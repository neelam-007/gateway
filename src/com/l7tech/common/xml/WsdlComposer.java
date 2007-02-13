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
import java.util.*;

/**
 * User: megery
 * Date: Feb 7, 2007
 * Time: 10:28:18 AM
 */
public class WsdlComposer {
    private Map<WsdlHolder, List<String>> operationsAdded;
    private Definition outputWsdl;
    WSDLFactory wsdlFactory;
    private ExtensionRegistry extensionRegistry;
    private List<String> targetNamespaces;

    public WsdlComposer(Definition def) {
        operationsAdded = new HashMap<WsdlHolder, List<String>>();
        targetNamespaces = new ArrayList<String>();
        outputWsdl = def;
        try {
            wsdlFactory = WSDLFactory.newInstance();
            extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();
        } catch (WSDLException e) {
            e.printStackTrace();
        }
    }

    public void removeBindingOperation(BindingOperation bo, WsdlHolder sourceWsdlHolder) {
        if (bo == null) return;
        System.out.println("removing operations and types from output wsdl for " + sourceWsdlHolder);
    }

    public void addBindingOperation(BindingOperation oldBop, WsdlHolder sourceWsdl) throws WSDLException, IOException, SAXException {
        if (oldBop == null) return;

        List<String> opsAdded = operationsAdded.get(sourceWsdl);
        if (opsAdded != null) {
            if (opsAdded.contains(oldBop.getName())) {
                return;
            }
        }

        Definition sourceDef = sourceWsdl.wsdl.getDefinition();
        BindingOperation newBop = outputWsdl.createBindingOperation();
        copyBindingOperation(oldBop, newBop);
        addOperationsForBindingOperation(newBop);
        addOperationToBindings(newBop);
        updateTargetNamespaces(sourceDef);

        if (opsAdded == null || opsAdded.size() == 0) {
            opsAdded = new ArrayList<String>();
            operationsAdded.put(sourceWsdl, opsAdded);
            addTypes(sourceDef, sourceWsdl);
        }
        opsAdded.add(oldBop.getName());
    }

    private void updateTargetNamespaces(Definition def) {
        String targetNamespace = def.getTargetNamespace();
        if (!targetNamespaces.contains(targetNamespace))
            targetNamespaces.add(targetNamespace);
    }


    public List<String> getTargetNamespaces() {
        return targetNamespaces;
    }


    public Definition getOutputWsdl() {
        return outputWsdl;
    }

    private void addOperationToBindings(BindingOperation newBop) {
        for (Object o : outputWsdl.getBindings().values()) {
            Binding binding = (Binding) o;
            binding.addBindingOperation(newBop);
        }
    }

    private void addTypes(Definition sourceDef, WsdlHolder sourceWsdl) throws WSDLException, IOException, SAXException {
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
    }

    private void addOperationsForBindingOperation(BindingOperation newBop) {
        for (Object o: outputWsdl.getPortTypes().values()) {
            PortType pt = (PortType) o;
            Operation op = newBop.getOperation();
            addMessagesForOperation(op);
            pt.addOperation(op);
        }
    }

    private void addMessagesForOperation(Operation op) {
        Input input = op.getInput();
        Output output = op.getOutput();


        Message m = input.getMessage();
        m.getParts();
        if (input != null)
            outputWsdl.addMessage(input.getMessage());

        if (output != null)
            outputWsdl.addMessage(output.getMessage());

        for (Object o : op.getFaults().values()) {
            Fault f = (Fault) o;
            outputWsdl.addMessage(f.getMessage());
        }
    }

    private void copyBindingOperation(BindingOperation oldBop, BindingOperation newBop) {
        newBop.setName(oldBop.getName());
        newBop.setBindingInput(oldBop.getBindingInput());
        newBop.setBindingOutput(oldBop.getBindingOutput());
        newBop.setDocumentationElement(oldBop.getDocumentationElement());
        newBop.setOperation(oldBop.getOperation());

        for (Object o: oldBop.getBindingFaults().values()) {
            BindingFault bf = (BindingFault) o;
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
