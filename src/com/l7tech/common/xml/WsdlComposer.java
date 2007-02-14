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
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Feb 7, 2007
 * Time: 10:28:18 AM
 */
public class WsdlComposer {
    private static final Logger logger = Logger.getLogger(WsdlComposer.class.getName());

    private Map<WsdlHolder, ContentChanges> contentsAdded;
    private Definition outputWsdl;
    WSDLFactory wsdlFactory;
    private ExtensionRegistry extensionRegistry;
    private List<String> targetNamespaces;

    public WsdlComposer(Definition def) throws WSDLException {
        contentsAdded = new HashMap<WsdlHolder, ContentChanges>();
        targetNamespaces = new ArrayList<String>();
        outputWsdl = def;

        wsdlFactory = WSDLFactory.newInstance();
        extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();
    }

    public void removeBindingOperation(BindingOperation bopToRemove, WsdlHolder sourceWsdlHolder) {
        if (bopToRemove == null) return;

        for (Object o : outputWsdl.getBindings().values()) {
            Binding b = (Binding) o;
            List bops = b.getBindingOperations();
            int i = 0;
            while(i < bops.size()) {
                Object eachObj = bops.get(i);
                BindingOperation eachBop  = (BindingOperation) eachObj;
                if (eachBop.getName().equals(bopToRemove.getName())) {
                    bops.remove(i);
                    break;
                }
                i++;
            }
        }

        updateResultDefinition(sourceWsdlHolder);
        System.out.println("removing operations and types from output wsdl for " + sourceWsdlHolder);
    }

    public void addBindingOperations(List<BindingOperation> oldBops, WsdlHolder sourceWsdlHolder) {
        if (oldBops == null || oldBops.size() == 0) return;

        for (BindingOperation oldBop : oldBops) {
            ContentChanges changes = contentsAdded.get(sourceWsdlHolder);
            if (changes != null) {
                List<BindingOperation> bindingOperationsAdded = changes.getBindingOperations();
                if (bindingOperationsAdded.contains(oldBop)) {
                    return;
                }
            }

            BindingOperation newBop = outputWsdl.createBindingOperation();
            copyBindingOperation(oldBop, newBop);
            addOperationToBindings(newBop);
        }

        if (sourceWsdlHolder != null)
            updateResultDefinition(sourceWsdlHolder);
    }

    private void updateResultDefinition(WsdlHolder sourceWsdl) {
        ContentChanges changes = contentsAdded.get(sourceWsdl);
        if (changes == null) {
            changes = new ContentChanges();
        }

        if (changes.getTypes() == null) {
            Types t = outputWsdl.getTypes();
            if (t != null)
                t.getExtensibilityElements().clear();
        }
        
        Map messages = outputWsdl.getMessages();
        if (messages != null)
            messages.clear();

        for (Object portType : outputWsdl.getPortTypes().values()) {
            PortType pt = (PortType) portType;
            pt.getOperations().clear();
        }

        for (Object b : outputWsdl.getBindings().values()) {
            Binding binding = (Binding) b;
            for (Object bo : binding.getBindingOperations()) {
                BindingOperation bindingOp = (BindingOperation) bo;
                addOperationsForBindingOperations(bindingOp);
                List<BindingOperation> bindingOpsAdded = changes.getBindingOperations();
                if (bindingOpsAdded == null)
                    bindingOpsAdded = new ArrayList<BindingOperation>();
                bindingOpsAdded.add(bindingOp);
                changes.setBindingOperations(bindingOpsAdded);
                contentsAdded.put(sourceWsdl, changes);

                try {
                    addTypes(sourceWsdl.wsdl.getDefinition(), sourceWsdl);
                } catch (WSDLException e) {
                    logger.warning("Could not add types to the resulting WSDL: " + e.getMessage());
                } catch (IOException e) {
                    logger.warning("Could not add types to the resulting WSDL: " + e.getMessage());
                } catch (SAXException e) {
                    logger.warning("Could not add types to the resulting WSDL: " + e.getMessage());
                }
                addNamespaces(sourceWsdl.wsdl.getDefinition());
            }
        }
    }

    private void addNamespaces(Definition sourceDef) {
        Map nsMap = sourceDef.getNamespaces();
        for (Object o : nsMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            outputWsdl.addNamespace((String)entry.getKey(), (String)entry.getValue());
        }
        
        String targetNamespace = sourceDef.getTargetNamespace();
        
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
        ContentChanges changes = contentsAdded.get(sourceWsdl);
        if (changes != null) {
            if (changes.getTypes() != null)
                return;
        } else {
            changes = new ContentChanges();
        }

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
        changes.setTypes(outputTypes);
        contentsAdded.put(sourceWsdl, changes);
    }

    private void addOperationsForBindingOperations(BindingOperation newBop) {
        for (Object o: outputWsdl.getPortTypes().values()) {
            PortType pt = (PortType) o;
            Operation op = newBop.getOperation();
            pt.addOperation(op);
            addMessagesForOperation(op);
        }
    }

    private void addMessagesForOperation(Operation op) {
        Input input = op.getInput();
        Output output = op.getOutput();

        if (input != null) {
            Message inMess = input.getMessage();
            if (inMess != null)
                outputWsdl.addMessage(inMess);
        }

        if (output != null) {
            Message outMess = output.getMessage();
            if (outMess != null)
                outputWsdl.addMessage(outMess);
        }

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

    public List<BindingOperation> getBindingOperations() {
        List<BindingOperation> ops = new ArrayList<BindingOperation>();
        for (Object o : outputWsdl.getBindings().values()) {
            Binding b = (Binding) o;
            ops.addAll(b.getBindingOperations());
        }
        return ops;
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

    private class ContentChanges {
        private Types types;
        private List<BindingOperation> bindingOperations;

        public List<BindingOperation> getBindingOperations() {
            return bindingOperations;
        }

        public void setBindingOperations(List<BindingOperation> bindingOperations) {
            this.bindingOperations = bindingOperations;
        }

        public Types getTypes() {
            return types;
        }

        public void setTypes(Types types) {
            this.types = types;
        }
    }
}
