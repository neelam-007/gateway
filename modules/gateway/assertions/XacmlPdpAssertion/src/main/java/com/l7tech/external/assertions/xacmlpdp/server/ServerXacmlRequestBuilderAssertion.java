package com.l7tech.external.assertions.xacmlpdp.server;

import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import static com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName.*;
import static com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.XPATH_RELATIVE;
import static com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.XPATH_ABSOLUTE;
import static com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.CONTEXT_VARIABLE;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ValidationUtils;
import com.l7tech.util.DomUtils;
import com.sun.xacml.attr.DateTimeAttribute;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.jaxen.UnresolvableException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.util.logging.Logger;
import java.util.*;
import java.io.IOException;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 *
 * @author njordan
 * @author darmstrong
 * @author steve
 * @author jbufu
 */
public class ServerXacmlRequestBuilderAssertion extends AbstractServerAssertion<XacmlRequestBuilderAssertion> {
    public ServerXacmlRequestBuilderAssertion(XacmlRequestBuilderAssertion ea, ApplicationContext applicationContext) {
        super(ea);

        variablesUsed = ea.getVariablesUsed();

        auditor = new Auditor(this, applicationContext, logger);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();
        } catch(ParserConfigurationException pce) {
            auditor.logAndAudit(AssertionMessages.XACML_REQUEST_ERROR, ExceptionUtils.getMessage(pce));
            return AssertionStatus.FAILED;
        }

        final Document xacmlRequestDocument = builder.newDocument();

        Element rootParent = null;
        if(assertion.getSoapEncapsulation() != XacmlAssertionEnums.SoapVersion.NONE) {
            String uri = assertion.getSoapEncapsulation().getUri();
            String prefix = assertion.getSoapEncapsulation().getPrefix();

            Element trueRoot = xacmlRequestDocument.createElementNS(uri, "Envelope");
            trueRoot.setPrefix(prefix);
            trueRoot.setAttribute("xmlns:" + prefix, uri);
            xacmlRequestDocument.appendChild(trueRoot);

            Element header = xacmlRequestDocument.createElementNS(uri, "Header");
            header.setPrefix(prefix);
            trueRoot.appendChild(header);

            Element body = xacmlRequestDocument.createElementNS(uri, "Body");
            body.setPrefix(prefix);
            trueRoot.appendChild(body);

            rootParent = body;
        }

        final Element root = xacmlRequestDocument.createElementNS(assertion.getXacmlVersion().getNamespace(), "Request");
        root.setAttribute("xmlns", assertion.getXacmlVersion().getNamespace());
        if(rootParent == null) {
            xacmlRequestDocument.appendChild(root);
        } else {
            rootParent.appendChild(root);
        }

        try {
            for(XacmlRequestBuilderAssertion.Subject subject : assertion.getSubjects()) {
                addSubject(context, vars, xacmlRequestDocument, root, subject);
            }

            for(XacmlRequestBuilderAssertion.Resource resource : assertion.getResources()) {
                addResource(context, vars, xacmlRequestDocument, root, resource);
            }

            addAction(context, vars, xacmlRequestDocument, root);

            addEnvironment(context, vars, xacmlRequestDocument, root);
        } catch (DocumentHolderException dhe) {
            auditor.logAndAudit( AssertionMessages.XACML_REQUEST_ERROR, new String[]{ExceptionUtils.getMessage(dhe)}, dhe);
            return AssertionStatus.FAILED;
        } catch (DOMException de) {
            auditor.logAndAudit( AssertionMessages.XACML_REQUEST_ERROR, ExceptionUtils.getMessage(de));
            return AssertionStatus.FAILED;
        } catch (RequiredFieldNotFoundException ranfe) {
            auditor.logAndAudit(AssertionMessages.XACML_NOT_FOUND_OPTION_ON, ranfe.getMessage());
            return AssertionStatus.FAILED;
        } catch(VariableNameSyntaxException e){
            //if any referenced variable is not found this exception will be thrown as we require strict processing
            //see bug 7664
            //already logged by ExpandVariables.badVariable
            return AssertionStatus.FAILED;
        }

        switch(assertion.getOutputMessageDestination()) {
            case DEFAULT_REQUEST:
                context.getRequest().initialize(xacmlRequestDocument);
                break;
            case DEFAULT_RESPONSE:
                context.getResponse().initialize(xacmlRequestDocument);
                break;
            case CONTEXT_VARIABLE:
                Message message = new Message(xacmlRequestDocument);
                context.setVariable(assertion.getOutputMessageVariableName(), message);
                break;
            default:
                // todo: only happen if enum changes, move to enum
                throw new IllegalStateException("Unsupported message output destination found");
        }

        return AssertionStatus.NONE;
    }

    private void addEnvironment(PolicyEnforcementContext context, Map<String, Object> vars, Document doc, Element root) throws DocumentHolderException, RequiredFieldNotFoundException {
        Element environmentElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Environment");
        root.appendChild(environmentElement);

        if(assertion.getEnvironment() != null) {
            addAttributes(doc,
                    environmentElement,
                    assertion.getEnvironment().getAttributes(),
                    vars, context,
                    assertion.getXacmlVersion());
        }
    }

    private void addAction(PolicyEnforcementContext context, Map<String, Object> vars, Document doc, Element root) throws DocumentHolderException, RequiredFieldNotFoundException {
        Element actionElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Action");
        root.appendChild(actionElement);
        addAttributes(doc,
                actionElement,
                assertion.getAction().getAttributes(),
                vars,
                context,
                assertion.getXacmlVersion());
    }

    private void addResource(PolicyEnforcementContext context, Map<String, Object> vars, Document doc, Element root, XacmlRequestBuilderAssertion.Resource resource) throws DocumentHolderException, RequiredFieldNotFoundException {

        Element resourceElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Resource");
        root.appendChild(resourceElement);

        if (resource.getResourceContent() != null) {
            Element resourceContentElement = doc.createElementNS(
                    assertion.getXacmlVersion().getNamespace(), "ResourceContent");
            resourceElement.appendChild(resourceContentElement);

            for (Map.Entry<String, String> entry : resource.getResourceContent().getAttributes().entrySet()) {
                String name = ExpandVariables.process(entry.getKey(), vars, auditor, true);
                String value = ExpandVariables.process(entry.getValue(), vars, auditor, true);
                safelyAddXmlAttributeToElement(resourceContentElement, name, value);
            }

            addContentToElementWithMessageSupport(resourceContentElement, resource.getResourceContent().getContent(), vars);
        }

        addAttributes(doc, resourceElement, resource.getAttributes(), vars, context, assertion.getXacmlVersion());
    }

    private void addSubject(PolicyEnforcementContext context, Map<String, Object> vars, Document doc, Element root, XacmlRequestBuilderAssertion.Subject subject) throws DocumentHolderException, RequiredFieldNotFoundException {
        Element subjectElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Subject");
        root.appendChild(subjectElement);

        if(subject.getSubjectCategory().length() > 0) {
            subjectElement.setAttribute("SubjectCategory",
                    ExpandVariables.process(subject.getSubjectCategory(), vars, auditor, true));
        }

        addAttributes(doc, subjectElement, subject.getAttributes(), vars, context, assertion.getXacmlVersion());
    }

    /**
     * From the Collection values, check if each value references a multi valued context variable, and if it does
     * return the size of the smallest multi valued context variable
     * @param values cant be null
     * @param contextVariables cant be null
     * @param valuesAlreadyProcessed if true, then we won't convert the values in values to it's internal form using
     * Syntax.getReferencedNames(). false implies that variableName is in the form ${...}
     * @return size of the smallest referenced multi valued context variable. 0 if none are referenced. this is a not
     * a test to check if a multi valued context variable is referenced
     */
    private int getMinMultiValuedCtxVariableReferenced(Collection<String> values, Map<String, Object> contextVariables,
                                                       boolean valuesAlreadyProcessed){
        int smallestCtxVarSize = 0;
        int largestCtxVarSize = 0;

        for(String s: values){
            if(!valuesAlreadyProcessed){
                String[] v = Syntax.getReferencedNames(s);
                if(v.length == 0) continue;
            }
            List multiCtxVarValues = getMultiValuedContextVariable(s, contextVariables, valuesAlreadyProcessed);

            if(multiCtxVarValues != null){
                smallestCtxVarSize = (multiCtxVarValues.size() < smallestCtxVarSize || smallestCtxVarSize == 0)
                        ? multiCtxVarValues.size() : smallestCtxVarSize;

                largestCtxVarSize = (multiCtxVarValues.size() > largestCtxVarSize)
                        ? multiCtxVarValues.size(): largestCtxVarSize;
            }
        }

        if(smallestCtxVarSize != largestCtxVarSize){
            auditor.logAndAudit(AssertionMessages.XACML_NOT_ALL_CTX_VARS_USED, Integer.toString(smallestCtxVarSize),
                    Integer.toString(largestCtxVarSize));
        }

        return smallestCtxVarSize;
    }

    /**
     * This method should only be called when you know that variableName exists in contextVariables
     * @param variableName
     * @param contextVariables
     * @param valueAlreadyProcessed if true, then we won't convert variableName to it's internal form using
     * Syntax.getReferencedNames(). false implies that variableName is in the form ${...}
     * @return multi valued context variable as a list. May be null if variableName is not a multi valued context
     * variable
     */
    private List getMultiValuedContextVariable(String variableName, Map<String, Object> contextVariables,
                                                       boolean valueAlreadyProcessed){
        Object o;
        if(!valueAlreadyProcessed){
            String[] v = Syntax.getReferencedNames(variableName);
            if(v.length == 0) throw new IllegalStateException("Context variable '"+variableName+"' not found");
            o = contextVariables.get(v[0]);
        }else{
            if(!contextVariables.containsKey(variableName))
                throw new IllegalStateException("Context variable '"+variableName+"' not found");
            o  = contextVariables.get(variableName);
        }
        List multiCtxVarValues = null;

        if(o instanceof Object[]){
            Object [] oArray = (Object[]) o;
            multiCtxVarValues = Arrays.asList(oArray);
        }else if(o instanceof List){
            multiCtxVarValues = (List) o;
        }

        return multiCtxVarValues;
    }

    private void addAttribute(Document xacmlRequestDocument,
                              Element parent,
                              XacmlRequestBuilderAssertion.Attribute attribute,
                              Map<String, Object> vars,
                              XacmlAssertionEnums.XacmlVersionType xacmlVersion)
    {
        Element attributeElement = xacmlRequestDocument.createElementNS(assertion.getXacmlVersion().getNamespace(), "Attribute");
        attributeElement.setAttribute("AttributeId", ExpandVariables.process(attribute.getId(), vars, auditor, true));
        attributeElement.setAttribute("DataType", ExpandVariables.process(attribute.getDataType(), vars, auditor, true));
        parent.appendChild(attributeElement);

        if(attribute.getIssuer().length() > 0) {
            attributeElement.setAttribute("Issuer", ExpandVariables.process(attribute.getIssuer(), vars, auditor, true));
        }

        if(assertion.getXacmlVersion() != XacmlAssertionEnums.XacmlVersionType.V2_0) {
            if(attribute.getIssueInstant().length() > 0) {
                attributeElement.setAttribute("IssueInstant",
                        ExpandVariables.process(attribute.getIssueInstant(), vars, auditor, true));
            }
        }

        for(XacmlRequestBuilderAssertion.AttributeValue attributeValue : attribute.getValues()) {
            HashSet<String> multiValuedVars = getAllReferencedNonIndexedMultiValueVars(attributeValue, vars);

            if(attributeValue.isCanElementHaveSameTypeSibilings()
                    && multiValuedVars.size() > 0
                    && xacmlVersion == XacmlAssertionEnums.XacmlVersionType.V2_0) {
                //context var referenced -> mapped to a new context variable name which is temporary
                //variableMap will contain the names of all multi valued variables which are referenced. It will not
                //contain a multi valued variable if it is subscripted. All operations below on the set of referenced
                //multi valued variables will use this map only
                HashMap<String, String> variableMap = new HashMap<String, String>();

                //we need to maintain our multi valued context variables, and yet work with each value individualy
                //the variable ssg.internal.xacml.request.var allows code below to work with each value from a multi
                //valued context variable individually. There is a variable created for each context variable referenced
                //note: this mechanism allows the indiviudal values of a multi valued context variable to reference
                //other context variables
                int i = 1;
                for(String s : multiValuedVars) {
                    variableMap.put(s, "ssg.internal.xacml.request.var" + i++);
                }

                //updatedAttributes represents the original attributes, but the new single valued context variable
                //created above is used in place of the multi valued context variable entered by the user
                HashMap<String, String> updatedAttributes =
                        new HashMap<String, String>(attributeValue.getAttributes().size());

                for(Map.Entry<String, String> entry : attributeValue.getAttributes().entrySet()) {
                    String k = entry.getKey(); //name
                    String v = entry.getValue();     //value
                    //for each attribute, search variableMap seeing if it references a context var, if it does replace with new name
                    for(Map.Entry<String, String> newEntry : variableMap.entrySet()) {
                        k = k.replaceAll("\\Q${" + newEntry.getKey() + "}\\E", "\\$\\{" + newEntry.getValue() + "\\}");
                        v = v.replaceAll("\\Q${" + newEntry.getKey() + "}\\E", "\\$\\{" + newEntry.getValue() + "\\}");
                    }
                    updatedAttributes.put(k, v);
                }

                //same process for content
                String content = attributeValue.getContent();
                for(Map.Entry<String, String> entry : variableMap.entrySet()) {
                    content = content.replaceAll("\\Q${" + entry.getKey() + "}\\E", "\\$\\{" + entry.getValue() + "\\}");
                }
                //find the smallest referenced context variable
                int smallestCtxVarSize =  getMinMultiValuedCtxVariableReferenced(variableMap.keySet(), vars, true);

                for(int z = 0; z < smallestCtxVarSize; z++){
                    //for each iteration, find from variablemap what multi valued context variabels are referenced,
                    //find the single value used in it's place, and updated the single valued variable with
                    //a value for this iteration
                    for(Map.Entry<String, String> entry : variableMap.entrySet()) {

                        List multiCtxVarValues = getMultiValuedContextVariable(entry.getKey(), vars, true);
                        if(multiCtxVarValues == null) throw new IllegalStateException("Unexpected type of context variable found");
                        vars.put(entry.getValue(), multiCtxVarValues.get(z));
                    }

                    Element valueElement =
                            xacmlRequestDocument.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
                    attributeElement.appendChild(valueElement);

                    for(Map.Entry<String, String> entry : updatedAttributes.entrySet()) {
                        String name = ExpandVariables.process(entry.getKey(), vars, auditor, true);
                        String attrValue = ExpandVariables.process(entry.getValue(), vars, auditor, true);
                        safelyAddXmlAttributeToElement(valueElement, name, attrValue);
                    }

                    addContentToElementWithMessageSupport(valueElement, content, vars);
                }

                for(String newVariableName : variableMap.values()) {
                    vars.remove(newVariableName);
                }
            } else {
                Element valueElement = xacmlRequestDocument.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
                attributeElement.appendChild(valueElement);

                for(Map.Entry<String, String> entry : attributeValue.getAttributes().entrySet()) {
                    String name = ExpandVariables.process(entry.getKey(), vars, auditor, true);
                    String value = ExpandVariables.process(entry.getValue(), vars, auditor, true);
                    safelyAddXmlAttributeToElement(valueElement, name, value);
                }

                addContentToElementWithMessageSupport(valueElement, attributeValue.getContent(), vars);
            }
        }
    }

    /**
     * If its possible that the attributeName contains invalid xml due to it's value having being retrieved from
     * ExpandVariables.process, use this method to add the attribute to the element, so it can deal with the
     * DOMException and log a meaningful warning and then throw the exception as an AssertionStatusException
     * <p/>
     * See ValidationUtils.isProbablyValidXmlName() which validates the XML, this may not catch every scenario
     *
     * @param element        the Element to add the attribute to
     * @param attributeName  the name of the attribute, could likely contain invalid xml characters
     * @param attributeValue the value of the attribute
     */
    private void safelyAddXmlAttributeToElement(final Element element,
                                                final String attributeName,
                                                final String attributeValue) {

        try {
            if(!ValidationUtils.isProbablyValidXmlName(attributeName)){
                auditor.logAndAudit(AssertionMessages.XACML_INVALID_XML_ATTRIBUTE, new String[]{attributeName, attributeValue});
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
            element.setAttribute(attributeName, attributeValue);
        } catch (DOMException e) {
            auditor.logAndAudit(AssertionMessages.XACML_INVALID_XML_ATTRIBUTE, new String[]{attributeName, attributeValue}, e);
            throw new AssertionStatusException(AssertionStatus.FAILED, e);
        }
    }

    /**
     * Check if any referenced variable is a Message. If there is a message there can only be one. If there is more
     * an IllegalStateException will be thrown
     *
     * @param contextVariables    all available context variables
     * @param referencedVariables was obtained from a call to Syntax.getReferencedNames(content), where the value
     *                            of content is the same value being passed in here
     * @return true if 1 and only 1 message is referenced from referencedVariables, false otherwise
     */
    private boolean doesContentIncludeAMessage(Map<String, Object> contextVariables, String[] referencedVariables) {
        //If any Message type var is referenced, there can only be 1
        boolean isAMessage = false;
        for (String s : referencedVariables) {
            if (!contextVariables.containsKey(s)) continue;
            if (contextVariables.get(s) instanceof Message) {
                isAMessage = true;
            }
        }

        //this error message is UI specific, however this is how it happens - a user types in two Message context
        //variables into the UI
        if (isAMessage && referencedVariables.length != 1)
            throw new IllegalStateException(
                    "AttributeValue content text field can only support a single message context variable");

        return isAMessage;
    }

    /**
     * Add the contents of the <AttributeValue> or <ResourceContent> elements. These schema elements can take any
     * content. We support this through allowing a context variable of type Message to be supplied.
     * If content has the name of a Message context variable , then the XML content of this variable is inserted as
     * the child of element, otherwise a text node is created.
     * <p/>
     * If there is any problem parsing the variable if it's off type Message, then an AssertionStatusException will be
     * thrown with AssertionStatus.FAILED
     *
     * @param element the Element representing the <AttributeValue> or <ResourceContent> element. Cannot be null
     * @param content the String representing either the string contents, with possibly string context variables or
     *                the name of a Message context variable. Cannot be null. 
     * @param vars    all known context variables
     */
    private void addContentToElementWithMessageSupport(Element element, String content, Map<String, Object> vars) {
        String[] v = Syntax.getReferencedNames(content);
        boolean isAMessage = doesContentIncludeAMessage(vars, v);

        if (isAMessage) {
            String s = v[0];
            Object o = vars.get(s);
            if (o instanceof Message) {
                Message m = (Message) vars.get(s);//wont be null based on isAMessage being true
                try {
                    addXmlMessageVariableAsAttributeValue(m, element, s);
                } finally {
                    m.close();
                }
            } else throw new IllegalStateException("Message expected");//will not happen based on above logic
        } else {
            //its not a Message, create a text node
            element.appendChild(element.getOwnerDocument().createTextNode(
                    ExpandVariables.process(content, vars, auditor, true)));
        }


    }

    /**
     * Add the message to the supplied element. The element will only be added if it's xml 
     *
     * Note: Caller needs to close the Message themselves
     * @param message The Message to add. Must be xml, assertion will fail if it's not
     * @param element The element to add the xml fragment from message to
     * @param messageName The name of the message variable. Can be null when called from context where this info
     * is not available 
     * @return boolean true if the message was added, false otherwise. If the message is not xml the return will be
     * false
     * @throws AssertionStatusException if either the xml cannot be read / parsed, of the message is not xml
     */
    private void addXmlMessageVariableAsAttributeValue(final Message message,
                                                          final Element element,
                                                          String messageName) {
        if(messageName == null) messageName = "Unknown";
        
        // todo: mixed content is allowed in AttributeValue, should be able to add non-xml messages
        try {
            if(message.isXml()){
                XmlKnob xmlKnob = message.getXmlKnob();
                Document doc = xmlKnob.getDocumentReadOnly();
                Document docOwner = element.getOwnerDocument();
                Node newNode = docOwner.importNode(doc.getDocumentElement(), true);
                element.appendChild(newNode);
            }else{
                auditor.logAndAudit(AssertionMessages.MESSAGE_VARIABLE_NOT_XML, new String[]{messageName});
                throw new AssertionStatusException(AssertionStatus.FAILED, "Message does not contain xml");
            }
        } catch (IOException e) {
            auditor.logAndAudit(AssertionMessages.MESSAGE_VARIABLE_BAD_XML, new String[]{messageName}, e);
            throw new AssertionStatusException(AssertionStatus.FAILED, e);
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.MESSAGE_VARIABLE_BAD_XML, new String[]{messageName}, e);
            throw new AssertionStatusException(AssertionStatus.FAILED, e);
        }
    }

    private void addAttributes(Document xacmlRequestDocument,
                               Element parent,
                               List<XacmlRequestBuilderAssertion.AttributeTreeNodeTag> attributeTreeNodeTags,
                               Map<String, Object> vars,
                               PolicyEnforcementContext context,
                               XacmlAssertionEnums.XacmlVersionType xacmlVersion)
            throws DocumentHolderException, RequiredFieldNotFoundException {
        for(XacmlRequestBuilderAssertion.AttributeTreeNodeTag attributeTreeNodeTag : attributeTreeNodeTags) {
            if(attributeTreeNodeTag instanceof XacmlRequestBuilderAssertion.Attribute) {
                XacmlRequestBuilderAssertion.Attribute attribute = (XacmlRequestBuilderAssertion.Attribute) attributeTreeNodeTag;
                addAttribute(xacmlRequestDocument, parent, attribute, vars, xacmlVersion);
            } else if(attributeTreeNodeTag instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig) {
                addMultipleAttributes(xacmlRequestDocument, parent, (XacmlRequestBuilderAssertion.MultipleAttributeConfig) attributeTreeNodeTag, vars, context);
            }
        }
    }

    private boolean fieldValueContainMultiValuedContextVariable(String valueToCheck, Map<String, Object> vars) {
        String[] v = Syntax.getReferencedNames(valueToCheck);

        for(String s : v) {
            if(vars.containsKey(s) && (vars.get(s) instanceof Object[] || vars.get(s) instanceof List)) {
                return true;
            }
        }

        return false;
    }

    private void addMultipleAttributes(final Document xacmlRequestDocument,
                                       final Element parent,
                                       final XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig,
                                       final Map<String, Object> contextVariables,
                                       final PolicyEnforcementContext context)
            throws DocumentHolderException, RequiredFieldNotFoundException {
        //Determine if any multi context var's being used outside of Value
        Set<XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field> nonValueFields = multipleAttributeConfig.getNonValueFields();
        boolean isUsingMultiValuedCtxVariables = areAnyIterableFieldsReferencingMultiValuedVariables(contextVariables, nonValueFields);

        boolean iteratingOnBaseXPath = false;
        List<String> iterableValues = new ArrayList<String>();
        for (XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field configField : multipleAttributeConfig.getAllFields()) {
            if (XPATH_RELATIVE == configField.getType())
                iteratingOnBaseXPath = true;

            //for iteration were need to know if a relative xpath is in the attribute field, but not if it contains
            //a multi valued context variable. The attribute value field in itself cannot cause iteration for Attribute elements
            if(configField.getName() == VALUE) continue;

            if (isUsingMultiValuedCtxVariables && CONTEXT_VARIABLE == configField.getType())
                iterableValues.add(configField.getValue());
        }
        int smallestCtxVarSize = getMinMultiValuedCtxVariableReferenced(iterableValues, contextVariables, false);
        
        //if we require the xpath base it must have been supplied, error from ui screen if not
        if(iteratingOnBaseXPath &&
                (multipleAttributeConfig.getXpathBase() == null || multipleAttributeConfig.getXpathBase().isEmpty())){
            throw new IllegalStateException("If any fields are relative xpath, then xpath base must be supplied");
        }

        final DocumentHolder documentHolder = new DocumentHolder(multipleAttributeConfig, context);
        ElementCursor resultCursor = null;
        XpathResultIterator xpathResultSetIterator = null;
        final boolean isFailAssertionEnabled = multipleAttributeConfig.isFalsifyPolicyEnabled();
        final Map<String, String> namespaces = multipleAttributeConfig.getNamespaces();

        if(iteratingOnBaseXPath){
            //Set up iterator for first run through do while loop
            xpathResultSetIterator = initializeXpathBase(documentHolder.getDocument(), multipleAttributeConfig,
                    contextVariables);
            if(xpathResultSetIterator == null){
                Set<String> incorrectNamespacePrefixes = causedByIncorrectNamespaceURI(documentHolder.getDocument(), namespaces);
                if (incorrectNamespacePrefixes.isEmpty()) {
                    auditor.logAndAudit(AssertionMessages.XACML_BASE_EXPRESSION_NO_RESULTS, multipleAttributeConfig.getXpathBase());
                } else {
                    for (String  prefix: incorrectNamespacePrefixes) {
                        auditor.logAndAudit(AssertionMessages.XACML_INCORRECT_NAMESPACE_URI, prefix);
                    }
                }
            }
        }

        if(xpathResultSetIterator != null) resultCursor = xpathResultSetIterator.nextElementAsCursor();

        XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field issueInstantField = multipleAttributeConfig.getField(ISSUE_INSTANT);
        final boolean ignoreIssueInstant = (assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V2_0)
                ||  (issueInstantField.getValue() == null || issueInstantField.getValue().isEmpty());

        final String elementName = parent.getLocalName();

        //now we know everything required to define the iteration
        int iterationCount = 0;
        boolean exitIteration;
        do {
            Map<XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName,String> fieldCurrentValues =
                new HashMap<XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName, String>();
            for(XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field field : nonValueFields) {
                final String valueOfField;
                if(XPATH_RELATIVE == field.getType()){
                    if(resultCursor == null){
                        valueOfField = null;                        
                    }else{
                        final XpathResult relXpathResult = executeXpathExpression(resultCursor, false, field.getValue(), namespaces,
                                contextVariables);
                        valueOfField = processAndGetFieldValueFromXpath(relXpathResult, field.getName().getDisplayName(), elementName);
                    }
                }else{
                    valueOfField = getValueForField(field, documentHolder, namespaces, contextVariables, iterationCount, elementName);
                }
                fieldCurrentValues.put(field.getName(), valueOfField);
            }

            //check our minimum requirements: id, datatype and value not having a value means no more <Attribute>s can be created
            //validation code ignoreCreatingAttributeElement handles if valueOfField is null from above evaluation
            final boolean createElement = !ignoreCreatingAttributeElement(ID, fieldCurrentValues.get(ID), elementName, isFailAssertionEnabled, ignoreIssueInstant)
                    && !ignoreCreatingAttributeElement(DATA_TYPE, fieldCurrentValues.get(DATA_TYPE), elementName, isFailAssertionEnabled, ignoreIssueInstant)
                    && !ignoreCreatingAttributeElement(ISSUE_INSTANT, fieldCurrentValues.get(ISSUE_INSTANT), elementName, isFailAssertionEnabled, ignoreIssueInstant);

            if(createElement) {
                createXacmlAttributeElement(
                        xacmlRequestDocument,
                        parent,
                        multipleAttributeConfig,
                        contextVariables,
                        documentHolder,
                        resultCursor,
                        namespaces,
                        fieldCurrentValues);
            }

            //we will attempt to create an Attribute once only, after that we need to find a reason to continue
            iterationCount++;

            //determine state of xpath iteration
            boolean xpathMoreResults = xpathResultSetIterator != null && xpathResultSetIterator.hasNext();

            // advance XPath iteration, if used
            if (iteratingOnBaseXPath && xpathMoreResults)
                resultCursor = xpathResultSetIterator.nextElementAsCursor();

            //determine the state of min multi valued context iteration
            //check we are not already past based on smallest multi value context variable
            boolean multiValueMoreResults = false;
            if(isUsingMultiValuedCtxVariables){
                if(iterationCount < smallestCtxVarSize){
                    multiValueMoreResults = true;
                }
            }

            //when do we exit? Well xpath may or may not be part of the iteration. a multi valuex context variable
            //may not be part of the iteration
            //cannot iterate more the minumum of both
            boolean bothIterateMethodsBeingUsed = iteratingOnBaseXPath && isUsingMultiValuedCtxVariables;

            exitIteration = true;
            if(bothIterateMethodsBeingUsed){
                //when both methods are being used, then both must say it's ok to continue
                exitIteration = !(multiValueMoreResults && xpathMoreResults);
                if(exitIteration && multiValueMoreResults != xpathMoreResults){
                    //only call this if both values are not false e.g. no need to call if both methods of iteration
                    //exhaust at the same time
                    logIterationEndState(multiValueMoreResults, xpathMoreResults);
                }
            }else{
                //check xpath
                if(iteratingOnBaseXPath){
                    exitIteration = !xpathMoreResults;
                }else if(isUsingMultiValuedCtxVariables){
                    exitIteration = !multiValueMoreResults;
                }
            }
        } while(!exitIteration);
//        while (iteratingOnBaseXPath && resultCursor != null || isUsingMultiValuedCtxVariables && iterationCount < smallestCtxVarSize)

    }

    /**
     * Validate the definition of the user-defined namespaces URI.
     * @param doc: the document that contains all required namespaces.
     * @param clientNamespaces: the user-defined namespaces in Multiple Attributes.
     * @return a set of namespace prefixes with incorrect namespace URI definitions.
     */
    private Set<String> causedByIncorrectNamespaceURI(Document doc, Map<String, String> clientNamespaces) {
        NodeList list = doc.getElementsByTagName("*");
        Map<String, String> serverNamespaces = new HashMap<String, String>();

        for (int i=0; i<list.getLength(); i++) {
            Element element = (Element)list.item(i);
            Map<String, String> map = DomUtils.getNamespaceMap(element);
            if (!map.isEmpty()) serverNamespaces.putAll(map);
        }

        Set<String> prefixes = new HashSet<String>();
        for (String prefix: clientNamespaces.keySet()) {
            if (serverNamespaces.containsKey(prefix) && !clientNamespaces.get(prefix).equals(serverNamespaces.get(prefix))) {
                prefixes.add(prefix);
            }
        }

        return prefixes;
    }

    /**
     * Log and audit an INFO message explaining why iteration ended
     * Message is only logged when both context variables and xpath results are used in iteration in
     * addMultipleAttributes. Logs for the user that not all info from a source was used 
     */
    private void logIterationEndState(final boolean ctxMoreValues, final boolean xpathMoreValues){

        //coding error
        if(ctxMoreValues && xpathMoreValues) throw new IllegalStateException("Method is not required while both values are true");

        if(ctxMoreValues){
            auditor.logAndAudit(AssertionMessages.XACML_NOT_ALL_VALUES_USED, "all referenced multi valued context variables", "an xpath result set was");
        }else if (xpathMoreValues){
            auditor.logAndAudit(AssertionMessages.XACML_NOT_ALL_VALUES_USED, "the xpath result set", "multi valued context variables were");
        }

    }

    /**
     * Get the value for an attribute of the XACML <Attribute> element. The only accecptable value is a String
     *
     * @param xpathResult the result of an xpath expression evaluation
     * @param fieldDisplayName the name of the field which represents the attribute we need a value for
     * @param elementName the major element under which the <Attribute> the attribute is for, is for e.g. Subject4
     * @return a String if a value was found. null otherwise. If the value is null, and audit and log at audit level
     * will have been generated
     */
    private String processAndGetFieldValueFromXpath(final XpathResult xpathResult,
                                           final String fieldDisplayName,
                                           final String elementName){
        //Note we cannot successfully use type from xpathResult, as it can report node set, when it's actually text        
        final List<XpathResultWrapper> results = processXPathResult(xpathResult);
        if(results.isEmpty()) return null;

        if(results.size() != 1){
            auditor.logAndAudit(
                    AssertionMessages.XACML_INCORRECT_NUM_RESULTS_FOR_FIELD,
                    Integer.toString(results.size()), fieldDisplayName);
        }

        XpathResultWrapper wrapper = results.get(0);
        if(wrapper.isNodeSet()){
            auditor.logAndAudit(AssertionMessages.XACML_INCORRECT_TYPE_FOR_FIELD, "NodeSet", fieldDisplayName, elementName);
            return null;
        }

        return wrapper.getStringValue();
    }

    /**
     * This method should only be called when the xpath base has been determined to be required
     * Initialize the xpath base expression. If this method returns a non null XpathResultIterator, then the xpath
     * base expression is valid and contains results.
     * @param document document against which the xpath expression will be evaluated
     * @param multipleAttributeConfig configuration for multiple attributes
     * @param contextVariables all applicable context variables
     * @return XpathResultIterator. If null then the xpath base expression did not evaluate to any results. If not
     * null then the caller can use the xpath base expression as it contains results
     */
    private XpathResultIterator initializeXpathBase(final Document document, 
                                     final XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig,
                                     final Map<String, Object> contextVariables){
        final ElementCursor cursor = new DomElementCursor(document);

        //Execute the base xpath expression, if any, to get information on iteration
        final XpathResult xpathResult = multipleAttributeConfig.getRelativeXPathFieldNames().isEmpty() ? null :
            executeXpathExpression(cursor,
                    true,
                    multipleAttributeConfig.getXpathBase(),
                    multipleAttributeConfig.getNamespaces(),
                    contextVariables);

        if(xpathResult != null && xpathResult.getType() != XpathResult.TYPE_NODESET) {
            return null;
        }

        //no null pointer concern as the xpath result is off type nodeset due to above check and return
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();
        if (xpathResultSetIterator == null) {
            return null;
        }
        if(!xpathResultSetIterator.hasNext()){
            return null;
        }

        return xpathResultSetIterator;
    }

    /**
     * Create a complete <Attribute> element with child <AttributeValue> elements
     *
     * @param xacmlRequestDocument Document to create eleents from
     * @param parent Element to add newly created elements to
     * @param multipleAttributeConfig Multiple Attribute configuration
     * @param contextVariables all applicable context variables
     * @param documentHolder holds a reference to the input message doc for xpath expressions
     * @param resultCursor used for relative xpath expressions, cursor to current element from xpath base results
     * @param namespaces applicable namespaces from xpath expressions
     * @param fieldCurrentValues values from attribute fields
     * @throws DocumentHolderException
     */
    private void createXacmlAttributeElement(
            final Document xacmlRequestDocument,
            final Element parent,
            final XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig,
            final Map<String, Object> contextVariables,
            final DocumentHolder documentHolder,
            final ElementCursor resultCursor,
            final Map<String, String> namespaces,
            final Map<XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName, String> fieldCurrentValues
    ) throws DocumentHolderException {

        final List<AttributeValue> attributeValues = getAttributeValues(documentHolder,
                namespaces,
                multipleAttributeConfig.getField(VALUE),
                contextVariables,
                resultCursor);

        //Create the Attribute
        Element attributeElement = addAttributeElement(xacmlRequestDocument, fieldCurrentValues);

        //Add all applicable AttributeValues
        for (AttributeValue av : attributeValues) {
            Element attributeValueElement = xacmlRequestDocument.createElementNS(
                    assertion.getXacmlVersion().getNamespace(), "AttributeValue");

            boolean successfullyAdded = av.addMeToAnElement(attributeValueElement);

            if (successfullyAdded) {
                //this is the only place where the parent get's an <Attribute> added to it
                attributeElement.appendChild(attributeValueElement);
                parent.appendChild(attributeElement);
            }

            if (assertion.getXacmlVersion() != XacmlAssertionEnums.XacmlVersionType.V2_0) break;
        }
    }

    /**
     * By giving a falsify-policy option, check if ignoring to create Attribute elements in XACML
     * Multiple Attributes. If a required field not found, throw a RequiredAttributeNotFoundException to falsify policy
     * consumption. In this method, also log and audit the field not found, even Falsify Policy Option is set to off.
     *
     * @param fieldName: the field display name such as "ID", "Data Type", "Issue Instant", etc.
     * @param fieldValue: the field value.
     * @param elementName: the name of an element such as Subject, Resource, Action, or Environment.
     * @param isFalsifyPolicyEnabled: a boolean value indicates if Falsify Policy Option is set to on/off.
     * @param ignoreIssueInstant true means that no value was entered by the user. False means that a value was entered
     * When the value is false, and the value of fieldValue is null or empty, it means that the expression contained
     * for issue instant evaluated to not found
     * @return true if the attribute is not found and "Falsify Policy Option" is off.  Otherwise, false if the field is found.
     * @throws RequiredFieldNotFoundException thown if a required field is not found
     *         with the extra condition under "Falsify Policy Option" on.
     */
    private boolean ignoreCreatingAttributeElement(final XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName fieldName,
                                                   final String fieldValue,
                                                   final String elementName,
                                                   final boolean isFalsifyPolicyEnabled,
                                                   final boolean ignoreIssueInstant) throws RequiredFieldNotFoundException {
        if (fieldName == null) throw new IllegalArgumentException("Attribute display name must not be null.");

        final boolean isIssueInstant = fieldName == ISSUE_INSTANT;
        if(isIssueInstant && ignoreIssueInstant) return false;

        boolean notFound = fieldValue == null || fieldValue.trim().isEmpty();

        final boolean issueInstantIsInvalid = isIssueInstant && !isIssueInstantValid(fieldValue);

        if (notFound || (isIssueInstant && issueInstantIsInvalid)) {
            if (isFalsifyPolicyEnabled) {
                throw new RequiredFieldNotFoundException(fieldName.getDisplayName());
            } else {
                auditor.logAndAudit(AssertionMessages.XACML_NOT_FOUND_OPTION_OFF, fieldName.getDisplayName(), elementName);
                return true;
            }
        }
        return false;
    }

    private boolean isIssueInstantValid(final String issueInstant){
        if(issueInstant == null || issueInstant.trim().isEmpty()) return false;

        try {
            DateTimeAttribute.getInstance(issueInstant);
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.XACML_INVALID_ISSUE_INSTANT, new String[]{issueInstant}, e);
            return false;
        }
        return true;
    }
    /**
     * Iterable fields can contain multi valued context variables. If a field contains one, the contents of the variable
     * cannot include any messages
     * @param contextVariables
     * @param fields
     * @return
     */
    private boolean areAnyIterableFieldsReferencingMultiValuedVariables(
            final Map<String, Object> contextVariables,
            final Set<XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field> fields) {
        boolean isUsingMultiValuedCtxVariables = false;
        for(XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field configField: fields){
            if(CONTEXT_VARIABLE == configField.getType()){
                if(fieldValueContainMultiValuedContextVariable(configField.getValue(), contextVariables)){
                    isUsingMultiValuedCtxVariables = true;
                    //check for Message content
                    List ctxValues =
                            getMultiValuedContextVariable(configField.getValue(), contextVariables, false);
                    for(Object o: ctxValues){
                        if(o instanceof Message){
                            throw new IllegalArgumentException(
                                    "Multi valued context variable used for iteration cannot contain any Messges");
                        }
                    }
                    break;
                }
            }
        }
        return isUsingMultiValuedCtxVariables;
    }

    private Element addAttributeElement(Document doc, Map<XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName,String> fieldCurrentValues) {
        Element attributeElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Attribute");
        attributeElement.setAttribute("AttributeID", fieldCurrentValues.get(ID));
        attributeElement.setAttribute("DataType", fieldCurrentValues.get(DATA_TYPE));
        if(fieldCurrentValues.get(ISSUER) != null && !fieldCurrentValues.get(ISSUER).isEmpty()) {
            attributeElement.setAttribute("Issuer", fieldCurrentValues.get(ISSUER));
        }

        if(assertion.getXacmlVersion() != XacmlAssertionEnums.XacmlVersionType.V2_0) {
            if(fieldCurrentValues.get(ISSUE_INSTANT) != null && !fieldCurrentValues.get(ISSUE_INSTANT).isEmpty()) {
                attributeElement.setAttribute("IssueInstant", fieldCurrentValues.get(ISSUE_INSTANT));
            }
        }

        return attributeElement;
    }

    /**
     * Get the value for configField regardless of whether it's a context variable, multi valued context variable,
     * absolute or xpath expression.
     *
     * toString() is called on any found context variable. Message types are not supported anywhere this method
     * is called from
     * This should not be called if configField is a relative xpath expression
     * configField cannot represent the value element of the MultipleAttributeConfig dialog
     * @param configField the attribute field to get the value for
     * @param multiVarIndex used as the index to extract the correct value from a multi valued context variable
     * @param elementName String the name of the element under which this <Attribute> is being created for
     * @return the resolved String value for this field
     */
    private String getValueForField(final XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field configField,
                                    final DocumentHolder documentHolder,
                                    final Map<String, String> namespaces,
                                    final Map<String, Object> contextVariables,
                                    final int multiVarIndex,
                                    final String elementName) throws DocumentHolderException {
        if( XPATH_RELATIVE == configField.getType() )
            throw new IllegalArgumentException("If xpath, field must not be relative");

        if( VALUE == configField.getName() )
            throw new UnsupportedOperationException("method cannot be used for value field");

        if( XPATH_ABSOLUTE == configField.getType() ) {
            final ElementCursor cursor = new DomElementCursor(documentHolder.getDocument());
            final XpathResult xpathResult = executeXpathExpression(cursor,
                    true, configField.getValue(), namespaces, contextVariables);

            return processAndGetFieldValueFromXpath(xpathResult, configField.getName().getDisplayName(), elementName);
        } else if ( CONTEXT_VARIABLE == configField.getType()) {
            //Determine if it's a multi valued context variable
            if(fieldValueContainMultiValuedContextVariable(configField.getValue(), contextVariables)){
                List multiValuedVariable =
                        getMultiValuedContextVariable(configField.getValue(), contextVariables, false);
                if(multiValuedVariable.size() < multiVarIndex){
                    //if this happens its a coding error
                    throw new IllegalStateException("Multi valued context variable found does not contain enough elements");
                }

                String value = multiValuedVariable.get(multiVarIndex).toString();
                return ExpandVariables.process(value, contextVariables, auditor, true);
            } else { // single-value context variable, expand
                return ExpandVariables.process(configField.getValue(), contextVariables, auditor, true);
            }
        } else { // REGULAR, expand all variables in the field value
            return ExpandVariables.process(configField.getValue(), contextVariables, auditor, true);
        }
    }

    /**
     * Execute an xpath expression against the cursor. If the cursor should be moved to root, supply true for
     * moveCursorToRoot.
     *
     * @param cursor ElementCursor to execute xpath expression against
     * @param moveCursorToRoot if true, the cursor will be moved to root
     * @param xpath String xpath expression
     * @param namespaces Map of all namespaces used by the xpath expression
     * @param contextVariables Map of applicable context variable that the xpath expression can reference
     * @return an XpathResult, never null
     */
    private XpathResult executeXpathExpression(final ElementCursor cursor,
                                               final boolean moveCursorToRoot,
                                               final String xpath,
                                               final Map<String, String> namespaces,
                                               final Map<String, Object> contextVariables) {
        if(cursor == null) throw new NullPointerException("cursor cannot be null");
        if(xpath == null) throw new NullPointerException("xpath cannot be null");
        if(xpath.trim().isEmpty()) throw new IllegalArgumentException("xpath cannot be the emtpy string");

        if(moveCursorToRoot) cursor.moveToRoot();

        final XpathResult xpathResult;
        try {
            xpathResult = cursor.getXpathResult(new XpathExpression(xpath, namespaces).compile(),
                        buildXpathVariableFinder(contextVariables), true);
        } catch (XPathExpressionException e) {
            String errorMessage = ExceptionUtils.getMessage(e);
            if (ExceptionUtils.causedBy(e, UnresolvableException.class) && errorMessage != null && errorMessage.contains("Cannot resolve namespace prefix")) {
                // This is a case where the given namespace map includes unresolvable namespace prefix.
                String prefix = errorMessage.substring(errorMessage.indexOf('\''));
                auditor.logAndAudit(AssertionMessages.XPATH_UNRESOLVABLE_PREFIX, new String[]{prefix}, e);
                throw new AssertionStatusException(AssertionStatus.UNRESOLVABLE_NAMESPACE_PREFIX);
            } else {
                auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID_MORE_INFO, new String[]{xpath}, e);
                throw new AssertionStatusException(AssertionStatus.INVALID_XPATH);
            }
        } catch (InvalidXpathException e) {
            String errorMessage = ExceptionUtils.getMessage(e);
            if (ExceptionUtils.causedBy(e, UnresolvableException.class) && errorMessage != null && errorMessage.contains("Cannot resolve namespace prefix")) {
                // This is a case where prefix is invalid in the xpath pattern.
                auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID_MORE_INFO, new String[]{xpath}, e);
                throw new AssertionStatusException(AssertionStatus.UNRESOLVABLE_NAMESPACE_PREFIX);
            } else {
                auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID_MORE_INFO, new String[]{xpath}, e);
                throw new AssertionStatusException(AssertionStatus.INVALID_XPATH);
            }
        }

        return xpathResult;
    }

    /**
     * Given the xpathResults, process it wrapping the results in XPathResultWrappers. This processing cares
     * about two types of results - Elements or text. This is reflected in XpathResultWrapper
     * @param xpathResult the XpathResult to process
     * @return List of XpathResultWrapper, never null, can be empty only when the type of xpathResult is a nodeset
     */
    private List<XpathResultWrapper> processXPathResult(XpathResult xpathResult){
        List<XpathResultWrapper> returnList = new ArrayList<XpathResultWrapper>();
        if(xpathResult.getType() == XpathResult.TYPE_NODESET) {

            XpathResultIterator resultIterator = xpathResult.getNodeSet().getIterator();
            while(resultIterator.hasNext()){
                ElementCursor cursor = resultIterator.nextElementAsCursor();
                XpathResultWrapper wrapper;
                if(cursor == null){
                    //that means we've found text and not an Element
                    XpathResultNode resultNode = new XpathResultNode();
                    resultIterator.next(resultNode);
                    wrapper = new XpathResultWrapper(resultNode.getNodeValue());
                }else{
                    Element element = cursor.asDomElement();
                    wrapper = new XpathResultWrapper(element);
                }
                returnList.add(wrapper);
            }

        } else if(xpathResult.getType() == XpathResult.TYPE_BOOLEAN) {
            XpathResultWrapper wrapper = new XpathResultWrapper(Boolean.toString(xpathResult.getBoolean()));
            returnList.add(wrapper);
        } else if(xpathResult.getType() == XpathResult.TYPE_NUMBER) {
            XpathResultWrapper wrapper = new XpathResultWrapper(Double.toString(xpathResult.getNumber()));
            returnList.add(wrapper);
        } else if(xpathResult.getType() == XpathResult.TYPE_STRING) {
            XpathResultWrapper wrapper = new XpathResultWrapper(xpathResult.getString());
            returnList.add(wrapper);
        }else{
            throw new IllegalStateException("Unknown type of XpathResult found: " + xpathResult.getType());
        }

        return returnList;
    }
    
    private XpathVariableFinder buildXpathVariableFinder( final Map<String, Object> contextVariables ) {
        return new XpathVariableFinder(){
            @Override
            public Object getVariableValue( final String namespaceUri,
                                            final String variableName ) throws NoSuchXpathVariableException {
                if ( namespaceUri != null )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable namespace '"+namespaceUri+"'.");

                if ( !contextVariables.containsKey(variableName) )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable name '"+variableName+"'.");

                return contextVariables.get(variableName);
            }
        };
    }
    
    /**
     * AttributeValue knows how to add itself to a Element representing an <AttributeValue>
     * //todo it would be nice if the AttributeValue could remember the names of variables used in it's makeup
     */
    private class AttributeValue{
        final static String MULTI_VAL_STRING_SEPARATOR = ", "; // todo: handle this better
        private String processedContent;     // simple content or already toString()'ed
        private Message message;             // content from one (XML) message
        private List<Object> mixedContent;   // mixed content (strings and (xml) messages)
        private XpathResultWrapper wrapper;

        public AttributeValue(String processedContent) {
            this.processedContent = processedContent;
        }

        public AttributeValue(Message message) {
            this.message = message;
        }

        public AttributeValue(List<Object> mixedContent) {
            this.mixedContent = mixedContent;
        }

        public AttributeValue(XpathResultWrapper wrapper) {
            this.wrapper = wrapper;
        }

        /**
         * Correctly add this AttributeValue to the supplied element, which represents an <AttributeValue>
         * The contents of 'this' may be an string, xpath result or Message
         * @param element
         * @return true if successfully added, false otherwise
         */
        public boolean addMeToAnElement(Element element){

            //am I a String?
            if(processedContent != null){
                element.appendChild(element.getOwnerDocument().createTextNode(processedContent));
                return true;
            }

            //am I a Message?
            if(message != null){
                addXmlMessageVariableAsAttributeValue(message, element, null);
                return true;
            }

            //am I from an Xpath result?
            if(wrapper != null){
                if(!wrapper.isNodeSet()){
                    String value = wrapper.getStringValue();
                    //not going to check if a value from an xpath contains a context variable
                    element.appendChild(element.getOwnerDocument().createTextNode(value));
                    return true;
                }

                Element resultNode = wrapper.getElement();
                Node importedNode = element.getOwnerDocument().importNode(resultNode, true);
                element.appendChild(importedNode);
                return true;
            }

            if (mixedContent != null) {
                boolean isPreviousString = false;
                for (Object part : mixedContent) {
                    if (part instanceof String) {
                        element.appendChild(element.getOwnerDocument().createTextNode((isPreviousString ? MULTI_VAL_STRING_SEPARATOR : "") + part ));
                        isPreviousString = true;
                    } else if (part instanceof Message) {
                        isPreviousString = false;
                        addXmlMessageVariableAsAttributeValue((Message)part, element, null);
                    }
                }
                return true;
            }
            
            return false;
        }
    }

    /**
     * From all possible inputs used to create AttributeValues, create a list of all <AttributeValue>s which
     * should be added to the <Attribute> element being created.
     * @param documentHolder - required if valueField contains an absolute xpath expression
     * @param namespaces any referenced namespaces from any xpath expression
     * @param valueField the field representing the AttributeValue
     * @param contextVariables all available context variables
     * @param resultCursor required if valueField is a relative xpath expression
     * @return list of AttributeValue, never null, but can be empty
     */
    private List<AttributeValue> getAttributeValues(
            DocumentHolder documentHolder,
            Map<String, String> namespaces,
            XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field valueField,
            Map<String, Object> contextVariables,
            ElementCursor resultCursor) throws DocumentHolderException {

        List<AttributeValue> returnList = new ArrayList<AttributeValue>();

        if (CONTEXT_VARIABLE == valueField.getType()) { // there's only one variable, but possibly multi-value
            if (fieldValueContainMultiValuedContextVariable(valueField.getValue(), contextVariables)) {
                List varValues = getMultiValuedContextVariable(valueField.getValue(), contextVariables, false);
                //contextVars contains String and or Messages
                for(Object varValue : varValues){
                    returnList.add(varValue instanceof Message ?
                        new AttributeValue((Message)varValue) :
                        new AttributeValue(ExpandVariables.process(varValue.toString(), contextVariables, auditor, true)));
                }
            } else { // single-value context variable, may contain a Message reference
                String [] varNames = Syntax.getReferencedNames(valueField.getValue());
                if(varNames.length != 0) {
                    Object contextVarValue = contextVariables.get(varNames[0]);
                    if(contextVarValue == null){
                        auditor.logAndAudit(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE, varNames[0]);
                        throw new VariableNameSyntaxException(varNames[0]);
                    }else{
                        returnList.add(contextVarValue instanceof Message ?
                            new AttributeValue((Message)contextVarValue) :
                            new AttributeValue(ExpandVariables.process(contextVarValue.toString(), contextVariables, auditor, true)));
                    }
                }
            }
        } else if( valueField.getType().isXpath() ) { // XPATH_RELATIVE or XPATH_ABSOLUTE
            final List<XpathResultWrapper> xpathResults;

            if(XPATH_RELATIVE == valueField.getType()){
                if(resultCursor == null){
                    xpathResults = Collections.emptyList();
                }else{
                    final XpathResult relXpathResult = executeXpathExpression(resultCursor, false, valueField.getValue(),
                            namespaces, contextVariables);
                    xpathResults = processXPathResult(relXpathResult);
                }
            }else{
                final ElementCursor cursor = new DomElementCursor(documentHolder.getDocument());
                final XpathResult absXpathResult = executeXpathExpression(cursor, true, valueField.getValue(),
                        namespaces, contextVariables);

                xpathResults = processXPathResult(absXpathResult);
            }

            if (xpathResults.isEmpty()) {
                returnList.add(new AttributeValue(""));
            } else {
                for(XpathResultWrapper wrapper: xpathResults){
                    returnList.add(new AttributeValue(wrapper));
                }
            }

        } else { // REGULAR, expand all context variables, including multi-valued, into one AttributeValue element
            returnList.add(new AttributeValue(ExpandVariables.processNoFormat(valueField.getValue(), contextVariables, auditor, true)));
        }

        return returnList;
    }

    /**
     * XpathResultWrapper wrapes an xpath result which is either an Element or text
     */
    private static class XpathResultWrapper{
        private final String stringValue;
        private final Element element;

        public XpathResultWrapper(Element element) {
            this.element = element;
            this.stringValue = null;
        }

        private XpathResultWrapper(String stringValue) {
            this.stringValue = stringValue;
            this.element = null;
        }

        public boolean isNodeSet(){
            return element != null;
        }

        public Element getElement() {
            return element;
        }

        public String getStringValue() {
            return stringValue;
        }
    }

    /**
     * Get the set of all referenced multi valued variables from the AttributeValue.
     * The set will not contain any variables which have a subscript e.g. if MULTI_VAR is a multi valued variable
     * and AttributeValue contains ${MULTI_VAR[1]} as an attribute name, name or content, the returned set will
     * not contain the variable MULTI_VAR. If the variable was just referenced as ${MULTI_VAR} then it would be
     * contained in the returned set.
     * @param attributeValue the AttributeValue from which to get all referenced multi valued variables
     * @param vars the map of known context variables and values
     * @return a set containing all referenced multi valued variables
     */
   private HashSet<String> getAllReferencedNonIndexedMultiValueVars(XacmlRequestBuilderAssertion.AttributeValue attributeValue, Map<String, Object> vars) {
        HashSet<String> multiValuedVars = new HashSet<String>();

        for(Map.Entry<String, String> entry : attributeValue.getAttributes().entrySet()) {
            String[] v = Syntax.getReferencedNamesIndexedVarsOmitted(entry.getKey());
            for(String s : v) {
                if(vars.containsKey(s) && (vars.get(s) instanceof Object[] || vars.get(s) instanceof List)) {
                    multiValuedVars.add(s);
                }
            }

            v = Syntax.getReferencedNamesIndexedVarsOmitted(entry.getValue());
            for(String s : v) {
                if(vars.containsKey(s) && (vars.get(s) instanceof Object[] || vars.get(s) instanceof List)) {
                    multiValuedVars.add(s);
                }
            }
        }

        String[] v = Syntax.getReferencedNamesIndexedVarsOmitted(attributeValue.getContent());
        for(String s : v) {
            if(vars.containsKey(s) && (vars.get(s) instanceof Object[] || vars.get(s) instanceof List)) {
                multiValuedVars.add(s);
            }
        }

        return multiValuedVars;
    }

    private String[] variablesUsed;
    private static final Logger logger = Logger.getLogger(ServerXacmlPdpAssertion.class.getName());
    private final Auditor auditor;

    /**
     * Document holder for the input message source.
     */
    private static class DocumentHolder {
        private Document document;
        private Exception thrown;
        private String what;

        public DocumentHolder(XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig, PolicyEnforcementContext context) {
            what = "Unknown";
            try {
                switch(multipleAttributeConfig.getMessageSource()) {
                    case DEFAULT_REQUEST:
                        what = "Request";
                        document = context.getRequest().getXmlKnob().getDocumentReadOnly();
                        break;
                    case DEFAULT_RESPONSE:
                        what = "Response";
                        document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                        break;
                    case CONTEXT_VARIABLE:
                        what = "${" + multipleAttributeConfig.getMessageSourceContextVar() + "}";
                        document = ((Message)context.getVariable(multipleAttributeConfig.getMessageSourceContextVar())).getXmlKnob().getDocumentReadOnly();
                        break;
                    default:
                        // todo: only happen if enum changes, move to enum
                        thrown = new IllegalStateException("Unsupported message source found");
                }
            } catch(SAXException saxe) {
                thrown = saxe;
            } catch(NoSuchVariableException nsve) {
                thrown = nsve;
            } catch(IOException ioe) {
                thrown = ioe;
            }
        }

        public Document getDocument() throws DocumentHolderException {
            if (document != null)
                return document;
            else
                throw new DocumentHolderException("Error accessing " + what + " message.", thrown);
        }
    }

    private static class DocumentHolderException extends Exception {
        public DocumentHolderException(String message, Exception thrown) {
            super( message, thrown );
        }
    }
}
