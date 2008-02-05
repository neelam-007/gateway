package com.l7tech.common.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.util.WsdlUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
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

    private static final String DEFAULT_PORT_TYPE_NAME = "NewPortType";
    private static final String DEFAULT_BINDING_NAME = "NewPortTypeBinding";
    private static final String DEFAULT_PORT_ADDRESS = "http://localhost:8080/ws/Service";
    private static final String DEFAULT_SERVICE_NAME = "Service";
    private static final String DEFAULT_BINDING_STYLE = "rpc"; // default style for the wizard
    private static final String DEFAULT_BINDING_TRANSPORT_URI = "http://schemas.xmlsoap.org/soap/http";

    private Document originalWsdlDoc;
    private Definition delegateWsdl;

    private WSDLFactory wsdlFactory;
    private ExtensionRegistry extensionRegistry;

    private QName qname;
    private String targetNamespace;
    private Map<String, String> otherNamespaces;

    private Map<QName, Message> messagesToAdd;

    private PortType portType;
    private Binding binding;
    private Service service;

    private Map<WsdlHolder, Set<BindingOperation>> bindingOperationsToAdd;

    private Map<String, Operation> operationsToAdd;

    private Map<WsdlHolder, Types> typesMap;
    private Set<WsdlHolder> sourceWsdls;
    private Builder builder;


    public WsdlComposer() throws WSDLException {
        initialise(null);
    }

    public WsdlComposer(Document origWsdl) throws WSDLException {
        initialise(origWsdl);
    }

    private void initialise(Document wsdl) throws WSDLException {
        originalWsdlDoc = wsdl;
        wsdlFactory = WsdlUtils.getWSDLFactory();
        extensionRegistry = Wsdl.disableSchemaExtensions(wsdlFactory.newPopulatedExtensionRegistry());

        delegateWsdl = wsdlFactory.newDefinition();
        sourceWsdls = new HashSet<WsdlHolder>();
        otherNamespaces = new HashMap<String, String>();

        typesMap = new HashMap<WsdlHolder, Types>();

        //Messages
        messagesToAdd = new HashMap<QName, Message>();

        //PortTypes && Operations
        operationsToAdd = new HashMap<String, Operation>();

        //Bindings && BindingOperations
        bindingOperationsToAdd = new HashMap<WsdlHolder, Set<BindingOperation>>();

        builder = new Builder();

        populateFromDefinition();
    }

    private void populateFromDefinition() throws WSDLException {
        if (originalWsdlDoc == null)
            return;

        Wsdl originalWsdl = Wsdl.newInstance(originalWsdlDoc.getDocumentElement().getBaseURI(), originalWsdlDoc);
        WsdlHolder originalWsdlHolder = new WsdlHolder( originalWsdl, "Original Wsdl" );
        addSourceWsdl( originalWsdlHolder );

        Definition def = originalWsdl.getDefinition();
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

        // Use first service with supported binding
        out:
        //noinspection unchecked
        for (Service service : (Collection<Service>) def.getServices().values()) {
            //noinspection unchecked
            for (Port port : (Collection<Port>) service.getPorts().values()) {
                Binding b = port.getBinding();

                if (isSupportedSoapBinding(b)) {
                    Service composedService = getOrCreateService();
                    composedService.getPorts().clear();
                    composedService.addPort(port);

                    for (Object boObj : b.getBindingOperations()) {
                        addBindingOperation((BindingOperation) boObj, originalWsdlHolder );
                    }

                    // copy style
                    String style = getBindingStyle(b);
                    if (style == null) style = "document"; // document is the WSDL default
                    setBindingStyle(getOrCreateBinding(), style);

                    // copy names
                    if (def.getQName() != null) setQName(new QName(def.getQName().getLocalPart()));
                    composedService.setQName(new QName(service.getQName().getLocalPart()));

                    PortType portType = getOrCreatePortType();
                    portType.setQName(new QName(b.getPortType().getQName().getLocalPart()));

                    Binding binding = getOrCreateBinding();
                    binding.setQName(new QName(b.getQName().getLocalPart()));

                    break out;
                }
            }
        }
    }

    public boolean isSupportedSoapBinding(Binding binding) {
        List elements = binding.getExtensibilityElements();
        for (Object element : elements) {
            ExtensibilityElement extElem = (ExtensibilityElement) element;
            QName elementType = extElem.getElementType();
            if (elementType.getNamespaceURI().equals(Wsdl.WSDL_SOAP_NAMESPACE))
                return true;
        }
        return false;
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
            addSourceWsdl(sourceWsdlHolder);
            addWsdlElementsForBindingOperation(sourceWsdlHolder, opToAdd);
            return true;
        } else {
            return false;
        }
    }

    public boolean sourceWasUsed(WsdlHolder holderToCheck) {
        boolean wasUsed = false;
        if (sourceWsdls.contains(holderToCheck)) {
            Set<BindingOperation> bos = bindingOperationsToAdd.get(holderToCheck);
            if (bos != null && !bos.isEmpty())
                wasUsed = true;
        }
        return wasUsed;
    }

    public boolean removeBindingOperationByOperation(Operation operation) {
        boolean removed = false;
        for (Map.Entry<WsdlComposer.WsdlHolder, Set<BindingOperation>> entry : bindingOperationsToAdd.entrySet()) {
            boolean empty = entry.getValue().size() <= 1;
            for (Iterator<BindingOperation> bopIter = entry.getValue().iterator(); bopIter.hasNext(); ) {
                BindingOperation bop = bopIter.next();
                if (bop.getOperation().getName().equals(operation.getName())) {
                    bopIter.remove();
                    removeWsdlElementsForBindingOperation(entry.getKey(), bop, empty);
                    removed = true;
                }
            }
        }
        return removed;
    }

    public boolean removeBindingOperation(BindingOperation bopToRemove, WsdlHolder sourceWsdlHolder) {
        if (bopToRemove == null)
            return false;

        Set<BindingOperation> bops = bindingOperationsToAdd.get(sourceWsdlHolder);
        if (bops == null)
            return false;

        if (bops.remove(bopToRemove)) {
            removeWsdlElementsForBindingOperation(sourceWsdlHolder, bopToRemove, bops.isEmpty());
            return true;
        } else {
            return false;
        }
    }

    public Map<WsdlHolder, Set<BindingOperation>> getBindingOperatiosnMap() {
        return bindingOperationsToAdd;
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
        Operation newOperation = copyOperation(bindingOperation.getOperation());
        bindingOperation.setOperation(newOperation);
        addBindingOperation(bindingOperation);
    }

    private void removeWsdlElementsForBindingOperation(WsdlHolder sourceWsdlHolder, BindingOperation bopToRemove, boolean empty) {
        removeBindingOperation(bopToRemove);
        Operation removedOp = removeOperation(bopToRemove.getOperation());
        removeMessagesFromSource(removedOp);

        if (empty) {
            removeTypesFromSource(sourceWsdlHolder);
        }
    }

    private void removeMessagesFromSource(Operation operation) {
        if (operation == null)
            return;
        Input input = operation.getInput();
        if (input != null) {
            Message m = input.getMessage();
            if (m != null)
                messagesToAdd.remove(m.getQName());
        }

        Output output = operation.getOutput();
        if (output != null) {
            Message m = output.getMessage();
            if (m != null)
                messagesToAdd.remove(m.getQName());
        }

        for (Object o : operation.getFaults().values()) {
            Fault f = (Fault) o;
            Message m = f.getMessage();
            messagesToAdd.remove(m.getQName());
        }
    }

    private void removeTypesFromSource(WsdlHolder sourceWsdlHolder) {
        typesMap.remove(sourceWsdlHolder);
    }

    private void removeBindingOperation(BindingOperation operation) {
        getOrCreateBinding().getBindingOperations().remove(operation);
    }

    private Operation removeOperation(Operation operation) {
        getOrCreatePortType().getOperations().remove(operation);
        return operationsToAdd.remove(operation.getName());
    }

    public void addSourceWsdl(WsdlHolder sourceWsdlHolder) {
        sourceWsdls.add(sourceWsdlHolder);
    }

    private void addMessagesFromSource(Operation operation) {
        Message newInputMsg = operation.getInput().getMessage();
        newInputMsg.setQName(new QName(targetNamespace, newInputMsg.getQName().getLocalPart()));

        Output output = operation.getOutput();
        if (output != null) {
            Message newOutMessage = output.getMessage();
            newOutMessage.setQName(new QName(targetNamespace, newOutMessage.getQName().getLocalPart()));
            internalAddMessage(newOutMessage);
        }

        internalAddMessage(newInputMsg);

        Map faults = operation.getFaults();
        if (faults != null) {
            for (Object o : faults.values()) {
                Fault f = (Fault) o;
                Message newFaultMsg = f.getMessage();
                newFaultMsg.setQName(new QName(targetNamespace, newFaultMsg.getQName().getLocalPart()));
                internalAddMessage(newFaultMsg);
            }
        }
    }

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

    private void internalAddMessage(Message message) {
        if (messagesToAdd.containsKey(message.getQName())) {
            return;
        }

        messagesToAdd.put(message.getQName(), message);
    }

    private void addBindingOperation(BindingOperation bop) {
        Binding binding = getOrCreateBinding();
        //noinspection unchecked
        if (!hasBindingOperation(binding.getBindingOperations(), bop)) {
            binding.addBindingOperation(bop);
        }

        addOperation(bop.getOperation());
    }

    private void addOperation(Operation sourceOperation) {
        addMessagesFromSource(sourceOperation);
        if (operationsToAdd.containsKey(sourceOperation.getName()))
            return;

        operationsToAdd.put(sourceOperation.getName(), sourceOperation);
        PortType pt = getOrCreatePortType();
        //noinspection unchecked
        if (!hasOperation(pt.getOperations(), sourceOperation)) {
            pt.addOperation(sourceOperation);
        }
    }

    private boolean hasBindingOperation(List<BindingOperation> operations, BindingOperation operation) {
        boolean found = false;

        for (BindingOperation currentOperation : operations) {
            if (currentOperation.getName().equals(operation.getName())) {
                found = true;
            }
        }

        return found;
    }

    private boolean hasOperation(List<Operation> operations, Operation operation) {
        boolean found = false;

        for (Operation currentOperation : operations) {
            if (currentOperation.getName().equals(operation.getName())) {
                found = true;
            }
        }

        return found;
    }

    private boolean addTypesFromSource(WsdlHolder sourceWsdlHolder) {
        Types typesFromSource = typesMap.get(sourceWsdlHolder);
        if (typesFromSource != null)
            return false;

        if (typesMap.containsKey(sourceWsdlHolder))
            return false;

        addTypesFromDefinition(sourceWsdlHolder, sourceWsdlHolder.wsdl.getDefinition());

        return true;
    }

    private boolean addTypesFromDefinition(WsdlHolder sourceWsdlHolder, Definition definition) {
        boolean added = false;

        Types sourceTypes = definition.getTypes();
        if ( sourceTypes != null ) {
            added = true;
            typesMap.put(sourceWsdlHolder, sourceTypes);
        } else {
            //noinspection unchecked
            Collection<List<Import>> importsCollection = definition.getImports().values();
            outer:
            for ( List<Import> imports : importsCollection ) {
                for ( Import imp : imports ) {
                    added = addTypesFromDefinition(sourceWsdlHolder, imp.getDefinition());
                    if (added) break outer;
                }
            }
        }

        return added;
    }

    private PortType getDefaultPortType() {
        return portType;
    }

    private Operation copyOperation(Operation sourceOperation) {
        Operation newOperation = delegateWsdl.createOperation();
        newOperation.setDocumentationElement(sourceOperation.getDocumentationElement());

        newOperation.setInput(copyInput(sourceOperation.getInput()));
        newOperation.setOutput(copyOutput(sourceOperation.getOutput()));
        newOperation.setName(sourceOperation.getName());
        newOperation.setParameterOrdering(sourceOperation.getParameterOrdering());
        newOperation.setStyle(sourceOperation.getStyle());
        newOperation.setUndefined(sourceOperation.isUndefined());

        Map faults = sourceOperation.getFaults();
        if (faults != null) {
            for (Object fault : faults.values()) {
                newOperation.addFault(copyFault((Fault) fault));
            }
        }

        /*List extElem = sourceOperation.getExtensibilityElements();
        if (extElem != null) {
            for (Object o : extElem) {
                newOperation.addExtensibilityElement((ExtensibilityElement) o);
            }
        }*/

        return newOperation;
    }

    private Fault copyFault(Fault sourceFault) {
        Fault newFault = delegateWsdl.createFault();
        newFault.setName(sourceFault.getName());
        newFault.setDocumentationElement(sourceFault.getDocumentationElement());
        newFault.setMessage(copyMessage(sourceFault.getMessage()));
        /*for (Object o : newFault.getExtensionAttributes().keySet()) {
            newFault.setExtensionAttribute((QName) o, newFault.getExtensionAttributes().get(o));
        }*/
        return newFault;

    }

    private Output copyOutput(Output sourceOutput) {
        // An one-way operation might not have any output (Bug #3697)
        if (sourceOutput == null) return null;
        Output newOutput = delegateWsdl.createOutput();
        newOutput.setName(sourceOutput.getName());
        newOutput.setDocumentationElement(sourceOutput.getDocumentationElement());
        newOutput.setMessage(copyMessage(sourceOutput.getMessage()));
        /*for (Object o : newOutput.getExtensionAttributes().keySet()) {
            newOutput.setExtensionAttribute((QName) o, newOutput.getExtensionAttributes().get(o));
        }*/
        return newOutput;
    }

    private Input copyInput(Input sourceInput) {
        Input newInput = delegateWsdl.createInput();
        newInput.setName(sourceInput.getName());
        newInput.setDocumentationElement(sourceInput.getDocumentationElement());
        newInput.setMessage(copyMessage(sourceInput.getMessage()));
        /*for (Object o : newInput.getExtensionAttributes().keySet()) {
            newInput.setExtensionAttribute((QName) o, newInput.getExtensionAttributes().get(o));
        }*/
        return newInput;
    }

    public void setQName(QName qName) {
        qname = qName;
    }

    public QName getQName() {
        return qname;
    }

    public void setTargetNamespace(String ns) {
        targetNamespace = ns;
        updateElementsWithNewNamespace();

    }

    private void updateElementsWithNewNamespace() {
        if (!operationsToAdd.isEmpty()) {
            for (Operation operation : operationsToAdd.values()) {
                Input in = operation.getInput();
                in.getMessage().setQName(new QName(targetNamespace, in.getMessage().getQName().getLocalPart()));

                Output out = operation.getOutput();
                if (out != null)
                    out.getMessage().setQName(new QName(targetNamespace, out.getMessage().getQName().getLocalPart()));

                for (Object o : operation.getFaults().values()) {
                    Fault f = (Fault) o;
                    f.getMessage().setQName(new QName(targetNamespace, f.getMessage().getQName().getLocalPart()));
                }
            }
        }
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
        return binding;
    }

    public void setBinding(Binding binding) {
        this.binding = binding;
    }

    public String getBindingStyle(Binding b) {
        if (b == null)
            return null;

        //noinspection unchecked
        List<ExtensibilityElement> ees = (List<ExtensibilityElement>) b.getExtensibilityElements();
        for (ExtensibilityElement ee : ees) {
            if (ee instanceof SOAPBinding) {
                SOAPBinding sb = (SOAPBinding) ee;
                return sb.getStyle();
            }
        }
        
        return null;
    }

    public void setBindingStyle(Binding b, String style) {
        if (b != null) {
            boolean updated = false;
            //noinspection unchecked
            List<ExtensibilityElement> ees = (List<ExtensibilityElement>) b.getExtensibilityElements();
            for (ExtensibilityElement ee : ees) {
                if (ee instanceof SOAPBinding) {
                    SOAPBinding sb = (SOAPBinding) ee;
                    sb.setStyle(style);
                    updated = true;
                }
            }

            if (!updated) {
                try {
                    SOAPBinding sb = (SOAPBinding) getExtensionRegistry().createExtension(
                            Binding.class,
                            new QName(Wsdl.WSDL_SOAP_NAMESPACE, "binding"));
                    sb.setTransportURI(DEFAULT_BINDING_TRANSPORT_URI);
                    sb.setStyle(style);
                    b.addExtensibilityElement(sb);
                }
                catch(WSDLException we) {
                    throw new RuntimeException(we);
                }
            }
        }
    }

    public String getBindingTransportURI(Binding b) {
        if (b == null)
            return null;

        //noinspection unchecked
        List<ExtensibilityElement> ees = (List<ExtensibilityElement>) b.getExtensibilityElements();
        for (ExtensibilityElement ee : ees) {
            if (ee instanceof SOAPBinding) {
                SOAPBinding sb = (SOAPBinding) ee;
                return sb.getTransportURI();
            }
        }

        return null;
    }

    public void setBindingTransportURI(Binding b, String transportURI) {
        if (b != null) {
            boolean updated = false;
            //noinspection unchecked
            List<ExtensibilityElement> ees = (List<ExtensibilityElement>) b.getExtensibilityElements();
            for (ExtensibilityElement ee : ees) {
                if (ee instanceof SOAPBinding) {
                    SOAPBinding sb = (SOAPBinding) ee;
                    sb.setTransportURI(transportURI);
                    updated = true;
                }
            }

            if (!updated) {
                try {
                    SOAPBinding sb = (SOAPBinding) getExtensionRegistry().createExtension(
                            Binding.class,
                            new QName(Wsdl.WSDL_SOAP_NAMESPACE, "binding"));
                    sb.setTransportURI(transportURI);
                    sb.setStyle(DEFAULT_BINDING_STYLE);
                    b.addExtensibilityElement(sb);
                }
                catch(WSDLException we) {
                    throw new RuntimeException(we);
                }
            }
        }
    }

    public PortType getPortType() {
        return getDefaultPortType();
    }

    public Service createService() {
        return delegateWsdl.createService();
    }

    /**
     * Retrieve the port type. Create the new port type if necessary
     *
     * @return the port type
     */
    public PortType getOrCreatePortType() {
        PortType portType = getPortType();

        if (portType == null) {
            portType = createPortType();
            portType.setQName(new QName(getTargetNamespace(), DEFAULT_PORT_TYPE_NAME));
            portType.setUndefined(false);
            setPortType(portType);
        }

        return portType;
    }

    public Binding getOrCreateBinding() {
        Binding binding = getBinding();

        if (binding == null) {
            binding = createBinding();
            binding.setPortType(getOrCreatePortType());
            binding.setUndefined(false);
            binding.setQName(new QName(getTargetNamespace(), DEFAULT_BINDING_NAME)); 
            setBinding(binding);
        }

        return binding;
    }

    public Service getOrCreateService() {
        Service service = getService();
        if (service == null) {
            service = createService();

            service.setQName(new QName(DEFAULT_SERVICE_NAME));
            getPort(service);
            setService(service);
        }

        return service;
    }

    private Port getPort(Service service) {
        Port port = getSupportedSoapPort(service);

        if (port == null){
            port = createPort();
            service.addPort(port);
            port.setName(service.getQName().getLocalPart() + "Port");
            try {
                ExtensibilityElement ee = extensionRegistry.createExtension(
                        Port.class,
                        new QName(Wsdl.WSDL_SOAP_NAMESPACE, "address"));
                if (ee instanceof SOAPAddress) {
                    SOAPAddress sa = (SOAPAddress)ee;
                    sa.setLocationURI(DEFAULT_PORT_ADDRESS);
                    port.addExtensibilityElement(ee);
                }
            }
            catch(WSDLException e) {
                // no address
            }

            port.setBinding(getOrCreateBinding());
        }

        return port;
    }

    public Port createPort() {
        return delegateWsdl.createPort();
    }

    public WSDLFactory getWsdlFactory() {
        return wsdlFactory;
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
        internalAddMessage(message);
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

    public Binding createBinding() {
        return delegateWsdl.createBinding();
    }

    public BindingInput createBindingInput() {
        return delegateWsdl.createBindingInput();
    }

    public BindingOutput createBindingOutput() {
        return delegateWsdl.createBindingOutput();
    }

    public PortType createPortType() {
        return delegateWsdl.createPortType();
    }

    public void setPortType(PortType portType) {
        this.portType = portType;
    }

    public Set<WsdlHolder> getSourceWsdls(boolean fetchUnusedSources) {
        Set<WsdlHolder> usedSources;
        if (fetchUnusedSources)
            usedSources = sourceWsdls;
        else {
            usedSources = new HashSet<WsdlHolder>();
            for (WsdlHolder source : sourceWsdls) {
                if (sourceWasUsed(source)) {
                    usedSources.add(source);
                }
            }
        }
        return usedSources;
    }

    public Document getOriginalWsdlDoc() {
        return originalWsdlDoc;
    }

    public void setOriginalWsdlDoc(Document origWsdl) {
        originalWsdlDoc = origWsdl;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Port getSupportedSoapPort(Service svc) {
        if (svc == null)
            return null;

        Collection ports = svc.getPorts().values();
        for (Object o : ports) {
            Port port = (Port) o;
            if (isSupportedSoapBinding(port.getBinding())) {
                return port;
            }
        }
        return null;
    }

    public static class WsdlHolder {
        public Wsdl wsdl;
        private String wsdlLocation;

        public WsdlHolder(Wsdl wsdl, String wsdlLocation) {
            this.wsdl = wsdl;
            this.wsdlLocation = wsdlLocation;
        }

        @Override
        public String toString() {
            String serviceName = wsdl.getServiceName();
            if (serviceName == null) {
                serviceName = wsdlLocation;
            }
            return serviceName + (StringUtils.isEmpty(wsdlLocation)?"":"[" + wsdlLocation + "]");
        }

        @Override
        public int hashCode() {
            return wsdl.hashCode();
        }

        @SuppressWarnings( { "EqualsWhichDoesntCheckParameterClass" } )
        @Override
        public boolean equals(Object obj) {
            return wsdl.equals(obj);
        }

        public String getWsdlLocation() {
            return wsdlLocation;
        }
    }

    private class Builder {

        private int nsPrefixCounter = 0;

        private String getNextNsPrefix() {
            return "sourcens" + nsPrefixCounter++;
        }

        public Definition buildWsdl() throws IOException, SAXException, WSDLException {
            nsPrefixCounter = 0;
            Definition workingWsdl;
            if (originalWsdlDoc == null) {
                workingWsdl = wsdlFactory.newDefinition();
            }
            else {
                WSDLReader reader = wsdlFactory.newWSDLReader();
                reader.setExtensionRegistry(extensionRegistry);
                workingWsdl = reader.readWSDL(originalWsdlDoc.getDocumentURI(), originalWsdlDoc);
                workingWsdl.getImports().clear(); // imports are in-lined
            }
            buildDefinition(workingWsdl);
            buildNamespaces(workingWsdl);
            buildTypes(workingWsdl);

            buildMessages(workingWsdl);

            PortType portType = getPortType();
            //noinspection unchecked
            List<Operation> operations = portType == null ?
                    Collections.<Operation>emptyList() :
                    (List<Operation>) portType.getOperations();
            PortType workingPortType = buildOperations(workingWsdl, operations);
            buildBindingOperations(workingWsdl, workingPortType);
            buildServices(workingWsdl);
            return workingWsdl;
        }

        private void buildDefinition(Definition workingWsdl) {
            QName name = getQName();
            if (name != null && name.getLocalPart()!=null && name.getLocalPart().trim().length()>0)
                workingWsdl.setQName(new QName(name.getLocalPart().trim()));
        }

        private void buildServices(Definition workingWsdl) {
            workingWsdl.getServices().clear();
            if (service != null)
                workingWsdl.addService(service);
        }

        private void buildTypes(Definition workingWsdl) throws IOException, SAXException, WSDLException {
            Types workingTypes = workingWsdl.createTypes();

            boolean typeAdded = false;
            for (Map.Entry<WsdlHolder, Types> entry : typesMap.entrySet()) {
                typeAdded = true;
                Types sourceTypes = entry.getValue();
                insertTypes(sourceTypes, workingTypes, workingWsdl);
            }

            // don't add empty type element
            if ( typeAdded ) {
                workingWsdl.setTypes(workingTypes);
            }
        }

        private void insertTypes(Types sourceTypes, Types workingTypes, Definition workingWsdl) throws WSDLException, IOException, SAXException {
            Definition emptyDefinition = wsdlFactory.newDefinition();
            for (Object obj : sourceTypes.getExtensibilityElements()) {
                ExtensibilityElement sourceExtElement = (ExtensibilityElement) obj;
                ExtensionSerializer serializer = extensionRegistry.querySerializer(sourceExtElement.getClass(), sourceExtElement.getElementType());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                //pass an empty definition, as this means all namespaces will have to be declared in the fragment
                //noinspection IOResourceOpenedButNotSafelyClosed
                serializer.marshall(sourceExtElement.getClass(), sourceExtElement.getElementType(), sourceExtElement, new PrintWriter(baos, true), emptyDefinition, extensionRegistry);

                byte[] bytes = baos.toByteArray();
                Document doc  = XmlUtil.parse(new ByteArrayInputStream(bytes));

                ExtensionDeserializer deserializer = extensionRegistry.queryDeserializer(sourceExtElement.getClass(), sourceExtElement.getElementType());
                ExtensibilityElement newElem = deserializer.unmarshall(sourceExtElement.getClass(), sourceExtElement.getElementType(), doc.getDocumentElement(), workingWsdl, extensionRegistry);

                workingTypes.addExtensibilityElement(newElem);
            }
        }

        private void buildMessages(Definition workingWsdl) {
            workingWsdl.getMessages().clear();
            if (!messagesToAdd.isEmpty()) {
                for (Map.Entry<QName, Message> entry : messagesToAdd.entrySet()) {
                    workingWsdl.addMessage(entry.getValue());
                }
            }
        }

        private PortType buildOperations(Definition workingWsdl, Collection<Operation> workingOps) {
            workingWsdl.getPortTypes().clear();
            PortType destinationPt = getPortType();
            if (destinationPt == null)
                return null;

            PortType workingPortType = workingWsdl.createPortType();
            workingPortType.setQName(destinationPt.getQName());
            workingPortType.setUndefined(false);

            Map<String,Operation> opMap = new TreeMap<String,Operation>();
            for (Operation operation : workingOps) {
                opMap.put(operation.getName(), operation);
            }

            for (Operation operation : operationsToAdd.values()) {
                opMap.put(operation.getName(), operation);
            }

            for (Operation operation : opMap.values()) {
                workingPortType.addOperation(operation);
            }

            workingWsdl.addPortType(workingPortType);

            return workingPortType;
        }

        private void buildBindingOperations(Definition workingWsdl, PortType workingPortType) {
            workingWsdl.getBindings().clear();

            Binding destinationBinding = getBinding();
            if (destinationBinding == null || workingPortType==null)
                return;

            Binding workingBinding = workingWsdl.createBinding();
            workingBinding.setPortType(workingPortType);
            workingBinding.setQName(destinationBinding.getQName());
            workingBinding.setUndefined(false);

            //noinspection unchecked
            for(ExtensibilityElement ee : (List<ExtensibilityElement>) destinationBinding.getExtensibilityElements()) {
                workingBinding.addExtensibilityElement(ee);
            }

            Map<String,BindingOperation> opMap = new TreeMap<String,BindingOperation>();

            //noinspection unchecked
            for (BindingOperation bindingOperation : (List<BindingOperation>) destinationBinding.getBindingOperations()) {
                opMap.put(bindingOperation.getName(), bindingOperation);                
            }

            for (Set<BindingOperation> bops: bindingOperationsToAdd.values()) {
                for (BindingOperation currentBindingOp : bops) {
                    opMap.put(currentBindingOp.getName(), currentBindingOp);
                }
            }

            for (BindingOperation bindingOperation : opMap.values()) {
                workingBinding.addBindingOperation(bindingOperation);
            }

            workingWsdl.addBinding(workingBinding);
        }

        private void buildNamespaces(Definition workingWsdl) {
            workingWsdl.setTargetNamespace(targetNamespace);
            for (Map.Entry<String, String> entry : otherNamespaces.entrySet()) {
                workingWsdl.addNamespace(entry.getKey(), entry.getValue());
            }

            for (WsdlHolder source: sourceWsdls) {
                workingWsdl.addNamespace(getNextNsPrefix(), source.wsdl.getDefinition().getTargetNamespace());
                Definition sourceDef = source.wsdl.getDefinition();
                for (Object o : sourceDef.getNamespaces().keySet()) {
                    String key = (String) o;
                    if (workingWsdl.getNamespace(key) != null) {
                        key = getNextNsPrefix();
                    }
                    workingWsdl.addNamespace(key, sourceDef.getNamespace(key));
                }
            }
        }
    }
}
