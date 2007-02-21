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
import javax.xml.namespace.QName;
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

    private Definition delegateWsdl;

    private WSDLFactory wsdlFactory;
    private ExtensionRegistry extensionRegistry;

    private QName qname;
    private String targetNamespace;
    private Map<String, String> otherNamespaces;

    private Map<QName, Message> messagesToAdd;

    private Map<QName, PortType> portTypes;

    private Map<QName, Binding> bindings;

    private Map<WsdlHolder, Set<BindingOperation>> bindingOperationsToAdd;

    private Map<String, Operation> operationsToAdd;

    private Map<WsdlHolder, Types> typesMap;
    private Set<WsdlHolder> sourceWsdls;
    private Builder builder;

    private Map<QName, Service> services;

    public WsdlComposer() throws WSDLException {
        initialise(null);
    }

    private void initialise(Definition def) throws WSDLException {
        wsdlFactory = WSDLFactory.newInstance();
        delegateWsdl = wsdlFactory.newDefinition();
        extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();

        sourceWsdls = new HashSet<WsdlHolder>();
        otherNamespaces = new HashMap<String, String>();

        typesMap = new HashMap<WsdlHolder, Types>();
                
        //Messages
        messagesToAdd = new HashMap<QName, Message>();

        //PortTypes && Operations
        portTypes = new HashMap<QName, PortType>();
        operationsToAdd = new HashMap<String, Operation>();

        //Bindings && BindingOperations
        bindings = new HashMap<QName, Binding>();
        bindingOperationsToAdd = new HashMap<WsdlHolder, Set<BindingOperation>>();

        services = new HashMap<QName, Service>();

        builder = new Builder();

        populateFromDefinition(def);
    }

    private void populateFromDefinition(Definition def) {
        if (def == null)
            return;

        setTargetNamespace(def.getTargetNamespace());
        for (Object o : def.getNamespaces().keySet()) {
            String key = (String) o;
            String value = (String) def.getNamespaces().get(key);
            addNamespace(key, value);
        }

        for (Object o : def.getMessages().values() ) {
            Message m = (Message) o;
            addMessage(m);
        }

        for (Object o : def.getBindings().values()) {
            addBinding((Binding) o, null);
        }
    }

    public WsdlComposer(Definition def) throws WSDLException {
        initialise(def);
    }

    public boolean addBindingOperation(BindingOperation opToAdd, WsdlHolder sourceWsdlHolder) {
        if (opToAdd == null)
            return false;
        Set<BindingOperation> opsForThisSourceWsdl = bindingOperationsToAdd.get(sourceWsdlHolder);
        if (opsForThisSourceWsdl == null) {
            opsForThisSourceWsdl = new HashSet<BindingOperation>();
            bindingOperationsToAdd.put(sourceWsdlHolder, opsForThisSourceWsdl);
        }

        if (opsForThisSourceWsdl.add(opToAdd)) {
            addWsdlElementsForBindingOperation(sourceWsdlHolder, opToAdd);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeBindingOperation(BindingOperation bopToRemove, WsdlHolder sourceWsdlHolder) {
        if (bopToRemove == null)
            return false;
        Set<BindingOperation> ops = bindingOperationsToAdd.get(sourceWsdlHolder);
        if (ops == null)
            return false;

        if (ops.remove(bopToRemove)) {
            removeWsdlElementsForBindingOperation(sourceWsdlHolder, bopToRemove);
            return true;
        } else {
            return false;
        }
    }

    public Collection<BindingOperation> getBindingOperations() {
        Set<BindingOperation> bindingOperationsList = new HashSet<BindingOperation>();
        for (Set<BindingOperation> bops : bindingOperationsToAdd.values()) {
            for (BindingOperation bop : bops) {
                bindingOperationsList.add(bop);
            }
        }
        return bindingOperationsList;
    }

    public Definition buildOutputWsdl() throws WSDLException, IOException, SAXException {
        return builder.buildWsdl();
    }

    private void addWsdlElementsForBindingOperation(WsdlHolder sourceWsdlHolder, BindingOperation bindingOperation) {
        addSourceWsdl(sourceWsdlHolder);
        addTypesFromSource(sourceWsdlHolder);
        addMessagesFromSource(sourceWsdlHolder, bindingOperation.getOperation());
        addOperation(bindingOperation.getOperation(), sourceWsdlHolder);
    }

    private void removeWsdlElementsForBindingOperation(WsdlHolder sourceWsdlHolder, BindingOperation bopToRemove) {
        removeOperation(bopToRemove.getOperation(), sourceWsdlHolder);
        removeTypesFromSource(sourceWsdlHolder);
        removeMessagesFromSource(sourceWsdlHolder, bopToRemove.getOperation());
        removeSourceWsdl(sourceWsdlHolder);
    }

    private void removeSourceWsdl(WsdlHolder sourceWsdlHolder) {
        //TODO: remove the source Wsdl from the list of wsdls to process
    }

    private void removeMessagesFromSource(WsdlHolder sourceWsdlHolder, Operation operation) {
        //TODO: remove the messages for this operation
    }

    private void removeTypesFromSource(WsdlHolder sourceWsdlHolder) {
        //TODO: remove the types for this source Wsdl if everything else is also gone
    }

    private void removeOperation(Operation operation, WsdlHolder sourceWsdlHolder) {
        operationsToAdd.remove(operation.getName());
    }

    public void addSourceWsdl(WsdlHolder sourceWsdlHolder) {
        sourceWsdls.add(sourceWsdlHolder);
    }

    private void addMessagesFromSource(WsdlHolder sourceWsdlHolder, Operation operation) {
        Message newInputMsg = copyMessage(operation.getInput().getMessage());
        newInputMsg.setQName(new QName(targetNamespace, newInputMsg.getQName().getLocalPart()));

        Message newOutMessage = copyMessage(operation.getOutput().getMessage());
        newOutMessage.setQName(new QName(targetNamespace, newOutMessage.getQName().getLocalPart()));

        internalAddMessage(newInputMsg,sourceWsdlHolder);
        internalAddMessage(newOutMessage, sourceWsdlHolder);
     
        Map faults = operation.getFaults();
        if (faults != null) {
            for (Object o : faults.values()) {
                Fault f = (Fault) o;
                Message faultMsg = copyMessage(f.getMessage());
                faultMsg.setQName(new QName(targetNamespace, faultMsg.getQName().getLocalPart()));
                internalAddMessage(faultMsg, sourceWsdlHolder);
            }
        }
    }

    //TODO: make sure this is a full deep copy
     private Message copyMessage(Message inputMsg) {
        Message newMessage = delegateWsdl.createMessage();
        newMessage.setQName(inputMsg.getQName());
        newMessage.setUndefined(inputMsg.isUndefined());
        newMessage.setDocumentationElement(newMessage.getDocumentationElement());
        for (Object o : inputMsg.getParts().values()) {
            Part oldPart = (Part) o;
            Part newPart = copyPart(oldPart);
            newMessage.addPart(newPart);
        }
        return newMessage;
    }

    //TODO: make sure this is a full deep copy
    private Part copyPart(Part oldPart) {
        Part newPart = delegateWsdl.createPart();
        newPart.setElementName(oldPart.getElementName());
        newPart.setName(oldPart.getName());
        newPart.setTypeName(oldPart.getTypeName());
        newPart.setDocumentationElement(oldPart.getDocumentationElement());
        for (Object o : oldPart.getExtensionAttributes().keySet()) {
            QName key = (QName) o;
            newPart.setExtensionAttribute(key, oldPart.getExtensionAttribute(key));
        }
        return newPart;
    }

    private void internalAddMessage(Message message, WsdlHolder sourceWsdl) {
        if (messagesToAdd.containsKey(message.getQName())) {
            return;
        }

        messagesToAdd.put(message.getQName(), message);
    }

    private void addOperation(Operation op, WsdlHolder sourceWsdlHolder) {
        internalAddOperation(op, sourceWsdlHolder);
    }

    private void removeOperation(Operation op) {
        internalRemoveOperation(op);
    }

    private void internalAddOperation(Operation op, WsdlHolder sourceWsdlHolder) {
        if (operationsToAdd.containsKey(op.getName()))
            return;

        operationsToAdd.put(op.getName(), op);

    }

    private void internalRemoveOperation(Operation op) {
        Operation removed = operationsToAdd.remove(op.getName());
    }


    private boolean addTypesFromSource(WsdlHolder sourceWsdlHolder) {
        Types typesFromSource = typesMap.get(sourceWsdlHolder);
        if (typesFromSource != null)
            return false;

        if (typesMap.containsKey(sourceWsdlHolder))
            return false;

        Types sourceTypes = sourceWsdlHolder.wsdl.getTypes();
        typesMap.put(sourceWsdlHolder, sourceTypes);
        return true;
    }

    private Types createTypes() {
        return delegateWsdl.createTypes();
    }

    private PortType getDefaultPortType() {
        if (portTypes.isEmpty())
            return null;

        return portTypes.values().iterator().next();
    }

    private Binding getDefaultBinding() {
        if (bindings.isEmpty())
            return null;

        return bindings.values().iterator().next();
    }

//    private Operation copyOperation(Operation sourceOperation) {
//        Operation newOperation = delegateWsdl.createOperation();
//        newOperation.setDocumentationElement(sourceOperation.getDocumentationElement());
//        newOperation.setInput(sourceOperation.getInput());
//        newOperation.setOutput(sourceOperation.getOutput());
//        newOperation.setName(sourceOperation.getName());
//        newOperation.setParameterOrdering(sourceOperation.getParameterOrdering());
//        newOperation.setStyle(sourceOperation.getStyle());
//        newOperation.setUndefined(sourceOperation.isUndefined());
//
//        Map faults = sourceOperation.getFaults();
//        if (faults != null) {
//            for (Object fault : faults.values()) {
//                newOperation.addFault((Fault) fault);
//            }
//        }
//
//        List extElem = sourceOperation.getExtensibilityElements();
//        if (extElem != null) {
//            for (Object o : extElem) {
//                newOperation.addExtensibilityElement((ExtensibilityElement) o);
//            }
//        }
//
//        return newOperation;
//    }

    public void setQName(QName qName) {
        qname = qName;
    }

    public QName getQName() {
        return qname;
    }
        
    public void setTargetNamespace(String ns) {
        targetNamespace = ns;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public void addNamespace(String prefix, String namespace) {
        otherNamespaces.put(prefix, namespace);
    }

    public Map getNamespaces() {
        return otherNamespaces;
    }

    public Binding getBinding() {
        return getDefaultBinding();
    }

    public PortType getPortType() {
        return getDefaultPortType();
    }
    
    public Map getServices() {
        return services;
    }

    public Service createService() {
        return delegateWsdl.createService();
    }

    public void addService(Service sv) {
        services.put(sv.getQName(), sv);
    }

    public Port createPort() {
        return delegateWsdl.createPort();
    }

    public ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }

    public BindingOperation createBindingOperation() {
        return delegateWsdl.createBindingOperation();
    }

    public Message createMessage() {
        return delegateWsdl.createMessage();
    }
    
    public Map<QName, Message> getMessages() {
        return messagesToAdd;
    }

    public void addMessage(Message message) {
        internalAddMessage(message,  null);
    }

    public Part createPart() {
        return delegateWsdl.createPart();
    }
    
    public Operation createOperation() {
        return delegateWsdl.createOperation();
    }

    public Input createInput() {
        return delegateWsdl.createInput();
    }

    public Output createOutput() {
        return delegateWsdl.createOutput();
    }

    public Map getBindings() {
        return bindings;
    }

    public Binding createBinding() {
        return delegateWsdl.createBinding();
    }

    public void addBinding(Binding binding, WsdlHolder holder) {
        internalAddBindings(binding, holder);
    }

    private void internalAddBindings(Binding binding, WsdlHolder holder) {
        if (bindings.containsKey(binding.getQName()))
            return;

        bindings.put(binding.getQName(), binding);

    }

    public void removeBinding(Binding binding) {
        internalRemoveBinding(binding);
    }

    private void internalRemoveBinding(Binding binding) {
        Binding removed = bindings.remove(binding.getQName());
    }

    public BindingInput createBindingInput() {
        return delegateWsdl.createBindingInput();
    }

    public BindingOutput createBindingOutput() {
        return delegateWsdl.createBindingOutput();
    }

    public Map getPortTypes() {
        return portTypes;
    }

    public PortType createPortType() {
        return delegateWsdl.createPortType();
    }

    public void addPortType(PortType portType, WsdlHolder holder) {
        internalAddPortType(portType, holder);
    }

    private void internalAddPortType(PortType portType, WsdlHolder holder) {
        if (portTypes.containsKey(portType.getQName()))
            return;

        portTypes.put(portType.getQName(), portType);

    }

    public void removePortType(PortType p) {
        internalRemovePortType(p);
    }

    private void internalRemovePortType(PortType p) {
        PortType removed = portTypes.remove(p.getQName());
    }

    public Set<WsdlHolder> getSourceWsdls() {
        return sourceWsdls;
    }

    public static class WsdlHolder {
        public Wsdl wsdl;
        private String wsdlLocation;

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

        public String getWsdlLocation() {
            return wsdlLocation;
        }
    }

    private class Builder {

        private int nsPrefixCounter = 0;
        private String getNextTnsPrefix() {
            return "sourcetns" + nsPrefixCounter++;
        }       

        public Definition buildWsdl() throws IOException, SAXException, WSDLException {
            nsPrefixCounter = 0;
            Definition workingWsdl = wsdlFactory.newDefinition();
            workingWsdl.setQName(qname);
            buildNamespaces(workingWsdl);
            buildTypes(workingWsdl);
            buildMessages(workingWsdl);
            buildOperations(workingWsdl);
            buildBindingOperations(workingWsdl);
            for (Map.Entry<QName,Service> serviceEntry : services.entrySet()) {
                workingWsdl.addService(serviceEntry.getValue());
            }
            return workingWsdl;
        }

        private void buildTypes(Definition workingWsdl) throws IOException, SAXException, WSDLException {
            Types workingTypes = workingWsdl.getTypes();
            if (workingTypes == null) {
                workingTypes = workingWsdl.createTypes();
            }

            if (!typesMap.isEmpty()) {
                for (Map.Entry<WsdlHolder, Types> entry : typesMap.entrySet()) {
                    Types sourceTypes = entry.getValue();
                    insertTypes(sourceTypes, workingTypes, workingWsdl);
                }
            }
            workingWsdl.setTypes(workingTypes);
        }

        private void insertTypes(Types sourceTypes, Types workingTypes, Definition workingWsdl) throws WSDLException, IOException, SAXException {
            for (Object obj : sourceTypes.getExtensibilityElements()) {
                ExtensibilityElement sourceExtElement = (ExtensibilityElement) obj;
                ExtensionSerializer serializer = extensionRegistry.querySerializer(sourceExtElement.getClass(), sourceExtElement.getElementType());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                serializer.marshall(sourceExtElement.getClass(), sourceExtElement.getElementType(), sourceExtElement, new PrintWriter(baos, true), workingWsdl, extensionRegistry);

                byte[] bytes = baos.toByteArray();
                Document doc  = XmlUtil.parse(new ByteArrayInputStream(bytes));

                ExtensionDeserializer deserializer = extensionRegistry.queryDeserializer(sourceExtElement.getClass(), sourceExtElement.getElementType());
                ExtensibilityElement newElem = deserializer.unmarshall(sourceExtElement.getClass(), sourceExtElement.getElementType(), doc.getDocumentElement(), workingWsdl, extensionRegistry);

                workingTypes.addExtensibilityElement(newElem);
            }            
        }

        private void buildMessages(Definition workingWsdl) {
            if (!messagesToAdd.isEmpty()) {
                for (Map.Entry<QName, Message> entry : messagesToAdd.entrySet()) {
                    workingWsdl.addMessage(entry.getValue());
                }
            }
        }

        private void buildOperations(Definition workingWsdl) {
            workingWsdl.getPortTypes().clear();

            PortType destinationPt = getPortType();
            if (destinationPt == null)
                return;
            workingWsdl.addPortType(destinationPt);
            if (!operationsToAdd.isEmpty()) {
                destinationPt.getOperations().clear();

                for (Operation operation : operationsToAdd.values()) {
                    destinationPt.addOperation(operation);
                }
                workingWsdl.addPortType(destinationPt);
            }
        }

        private void buildBindingOperations(Definition workingWsdl) {
            workingWsdl.getBindings().clear();

            Binding destinationBinding = getBinding();
            if (destinationBinding == null)
                return;

            workingWsdl.addBinding(destinationBinding);
            
            if (!bindingOperationsToAdd.isEmpty()) {
                destinationBinding.getBindingOperations().clear();
                for (Set<BindingOperation> bops: bindingOperationsToAdd.values()) {
                    for (BindingOperation currentBindingOp : bops) {
                        destinationBinding.addBindingOperation(currentBindingOp);
                    }
                }
                workingWsdl.addBinding(destinationBinding);
            }
        }

        private void buildNamespaces(Definition workingWsdl) {
            workingWsdl.setTargetNamespace(targetNamespace);
            for (Map.Entry<String, String> entry : otherNamespaces.entrySet()) {
                workingWsdl.addNamespace(entry.getKey(), entry.getValue());
            }

            for (WsdlHolder source: sourceWsdls) {
                workingWsdl.addNamespace(getNextTnsPrefix(), source.wsdl.getDefinition().getTargetNamespace());
                Definition sourceDef = source.wsdl.getDefinition();
                for (Object o : sourceDef.getNamespaces().keySet()) {
                    String key = (String) o;
                    workingWsdl.addNamespace(key, sourceDef.getNamespace(key));
                }
            }
        }
    }
}
