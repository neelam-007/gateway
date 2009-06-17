package com.l7tech.external.assertions.xacmlpdp.server;

import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResultIterator;
import com.l7tech.xml.xpath.XpathResultNode;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.util.logging.Logger;
import java.util.*;
import java.io.IOException;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 31-Mar-2009
 * Time: 9:05:35 PM
 */
public class ServerXacmlRequestBuilderAssertion extends AbstractServerAssertion<XacmlRequestBuilderAssertion> {
    public ServerXacmlRequestBuilderAssertion(XacmlRequestBuilderAssertion ea, ApplicationContext applicationContext) {
        super(ea);

        variablesUsed = ea.getVariablesUsed();

        auditor = new Auditor(this, applicationContext, logger);
        stashManagerFactory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();
        } catch(ParserConfigurationException pce) {
            return AssertionStatus.FAILED;
        }

        Document doc = builder.newDocument();

        Element rootParent = null;
        if(assertion.getSoapEncapsulation() != XacmlAssertionEnums.SoapVersion.NONE) {
            String uri = assertion.getSoapEncapsulation().getUri();
            String prefix = assertion.getSoapEncapsulation().getPrefix();

            Element trueRoot = doc.createElementNS(uri, "Envelope");
            trueRoot.setPrefix(prefix);
            trueRoot.setAttribute("xmlns:" + prefix, uri);
            doc.appendChild(trueRoot);

            Element header = doc.createElementNS(uri, "Header");
            header.setPrefix(prefix);
            trueRoot.appendChild(header);

            Element body = doc.createElementNS(uri, "Body");
            body.setPrefix(prefix);
            trueRoot.appendChild(body);

            rootParent = body;
        }

        Element root = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Request");
        root.setAttribute("xmlns", assertion.getXacmlVersion().getNamespace());
        if(rootParent == null) {
            doc.appendChild(root);
        } else {
            rootParent.appendChild(root);
        }

        for(XacmlRequestBuilderAssertion.Subject subject : assertion.getSubjects()) {
            addSubject(context, vars, doc, root, subject);
        }

        for(XacmlRequestBuilderAssertion.Resource resource : assertion.getResources()) {
            addResource(context, vars, doc, root, resource);
        }

        addAction(context, vars, doc, root);

        addEnvironment(context, vars, doc, root);

        switch(assertion.getOutputMessageDestination()) {
            case DEFAULT_REQUEST:
                context.getRequest().initialize(doc);
                break;
            case DEFAULT_RESPONSE:
                context.getResponse().initialize(doc);
                break;
            case CONTEXT_VARIABLE:
                Message message = new Message(doc);
                context.setVariable(assertion.getOutputMessageVariableName(), message);
                break;
            default:
                throw new IllegalStateException("Unsupported message output destination found");//only happen if enum changes
        }

        return AssertionStatus.NONE;
    }

    private void addEnvironment(PolicyEnforcementContext context, Map<String, Object> vars, Document doc, Element root) {
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

    private void addAction(PolicyEnforcementContext context, Map<String, Object> vars, Document doc, Element root) {
        Element actionElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Action");
        root.appendChild(actionElement);
        addAttributes(doc,
                actionElement,
                assertion.getAction().getAttributes(),
                vars,
                context,
                assertion.getXacmlVersion());
    }

    private void addResource(PolicyEnforcementContext context, Map<String, Object> vars, Document doc, Element root, XacmlRequestBuilderAssertion.Resource resource) {
        Element resourceElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Resource");
        root.appendChild(resourceElement);

        if(resource.getResourceContent() != null) {
            Element resourceContentElement = doc.createElementNS(
                    assertion.getXacmlVersion().getNamespace(), "ResourceContent");
            resourceElement.appendChild(resourceContentElement);

            for(Map.Entry<String, String> entry : resource.getResourceContent().getAttributes().entrySet()) {
                String name = ExpandVariables.process(entry.getKey(), vars, auditor, true);
                String value = ExpandVariables.process(entry.getValue(), vars, auditor, true);
                resourceContentElement.setAttribute(name, value);
            }

            addContentToElementWithMessageSupport(resourceContentElement, resource.getResourceContent().getContent(), vars);
        }

        addAttributes(doc, resourceElement, resource.getAttributes(), vars, context, assertion.getXacmlVersion());
    }

    private void addSubject(PolicyEnforcementContext context, Map<String, Object> vars, Document doc, Element root, XacmlRequestBuilderAssertion.Subject subject) {
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
        for(String s: values){
            if(!valuesAlreadyProcessed){
                String[] v = Syntax.getReferencedNames(s);
                if(v.length == 0) continue;
            }
            List multiCtxVarValues = getMultiValuedContextVariable(s, contextVariables, valuesAlreadyProcessed);

            if(multiCtxVarValues != null){
                smallestCtxVarSize = (multiCtxVarValues.size() < smallestCtxVarSize || smallestCtxVarSize == 0)
                        ? multiCtxVarValues.size() : smallestCtxVarSize;
            }
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
    private List<Object> getMultiValuedContextVariable(String variableName, Map<String, Object> contextVariables,
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
        List<Object> multiCtxVarValues = null;

        if(o instanceof Object[]){
            Object [] oArray = (Object[]) o;
            multiCtxVarValues = Arrays.asList(oArray);
        }else if(o instanceof List){
            multiCtxVarValues = (List) o;
        }

        return multiCtxVarValues;
    }

    private void addAttribute(Document doc,
                              Element parent,
                              XacmlRequestBuilderAssertion.Attribute attribute,
                              Map<String, Object> vars,
                              XacmlAssertionEnums.XacmlVersionType xacmlVersion)
    {
        Element attributeElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Attribute");
        attributeElement.setAttribute("AttributeId", ExpandVariables.process(attribute.getId(), vars, auditor, true));
        attributeElement.setAttribute("DataType", ExpandVariables.process(attribute.getDataType(), vars, auditor, true));
        parent.appendChild(attributeElement);

        if(attribute.getIssuer().length() > 0) {
            attributeElement.setAttribute("Issuer", ExpandVariables.process(attribute.getIssuer(), vars, auditor, true));
        }

        if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V1_0) {
            if(attribute.getIssueInstant().length() > 0) {
                attributeElement.setAttribute("IssueInstant",
                        ExpandVariables.process(attribute.getIssueInstant(), vars, auditor, true));
            }
        }

        for(XacmlRequestBuilderAssertion.AttributeValue attributeValue : attribute.getValues()) {
            HashSet<String> multiValuedVars = valueContainsMultiValuedVars(attributeValue, vars);

            if(attributeValue.isCanElementHaveSameTypeSibilings()
                    && multiValuedVars.size() > 0
                    && xacmlVersion == XacmlAssertionEnums.XacmlVersionType.V2_0) {
                //context var referenced -> mapped to a new context variable name which is temporary
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
                            doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
                    attributeElement.appendChild(valueElement);

                    for(Map.Entry<String, String> entry : updatedAttributes.entrySet()) {
                        String name = ExpandVariables.process(entry.getKey(), vars, auditor, true);
                        String attrValue = ExpandVariables.process(entry.getValue(), vars, auditor, true);
                        valueElement.setAttribute(name, attrValue);
                    }

                    addContentToElementWithMessageSupport(valueElement, content, vars);
                }

                for(String newVariableName : variableMap.values()) {
                    vars.remove(newVariableName);
                }
            } else {
                Element valueElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
                attributeElement.appendChild(valueElement);

                for(Map.Entry<String, String> entry : attributeValue.getAttributes().entrySet()) {
                    String name = ExpandVariables.process(entry.getKey(), vars, auditor, true);
                    String value = ExpandVariables.process(entry.getValue(), vars, auditor, true);
                    valueElement.setAttribute(name, value);
                }

                addContentToElementWithMessageSupport(valueElement, attributeValue.getContent(), vars);
            }
        }
    }

    /**
     * Check if any referenced variable is a Message. If thre is a message there can only be one
     * @param contextVariables all available contet variables
     * @param referencedVariables was obtained from a call to Syntax.getReferencedNames(content), where the value
     * of content is the same value being passed in here 
     * @return true if 1 and only 1 messgae is referenced from referencedVariables
     */
    private boolean doesContentIncludeAMessage(Map<String, Object> contextVariables,
                                               String[] referencedVariables){
        //If any Message type var is referenced, there can only be 1
        boolean isAMessage = false;
        for(String s : referencedVariables) {
            if(!contextVariables.containsKey(s)) continue;
            if(contextVariables.get(s) instanceof Message){
                isAMessage = true;
            }
        }

        //this error message is UI specific, however this is how it happens - a user types in two Message context
        //variables into the UI
        if(isAMessage && referencedVariables.length != 1)
            throw new IllegalStateException(
                    "AttributeValue content text field can only support a single message context variable");

        return isAMessage;
    }

    /**
     * Add the contents of the <AttributeValue> or <ResourceContent> elements. These schema elements can take any
     * content. We support this through allowing a context variable of type Message to be supplied.
     * If content has the name of a Message context variable , then the XML content of this variable is inserted as
     * the child of element, otherwise a text node is created.
     *
     * It is possible that nothing will be added to the attributeValue. If content does represent a Message context
     * variable, and there are any problems parsing it, then nothing will be added
     * @param element the Element representing the <AttributeValue> or <ResourceContent> element. Cannot be null
     * @param content the String representing either the string contents, with possibly string context variables or
     * the name of a Message context variable. Cannot be null. If content does contains the name of more than one
     * context variable of type Message and IllegalStateException will be thrown as this is not supported.
     * @param vars all known context variables
     */
    private void addContentToElementWithMessageSupport(Element element, String content, Map<String, Object> vars){
        String[] v = Syntax.getReferencedNames(content);
        boolean isAMessage = doesContentIncludeAMessage(vars, v);

        if(isAMessage){
            String s = v[0];
            Object o = vars.get(s);
            if(o instanceof Message) {
                Message m = (Message) vars.get(s);//wont be null based on isAMessage being true
                try {
                    addXmlMessageVariableAsAttributeValue(m, element);
                } catch (Exception e) {
                    //can log this - but action is that the attributeValue is not modified in this case
                }finally{m.close();}
            }else throw new IllegalStateException("Message expected");//will not happen based on above logic
        }else{
            //its not a Message, create a text node
            element.appendChild( element.getOwnerDocument().createTextNode(
                    ExpandVariables.process(content, vars, auditor, true)));
        }


    }

    /**
     * Caller needs to close the Message themselves
     * @param m
     * @param element
     * @throws IOException
     * @throws SAXException
     */
    private void addXmlMessageVariableAsAttributeValue(Message m, Element element) throws IOException, SAXException {
        if(m.isXml()){
            XmlKnob xmlKnob = m.getXmlKnob();
            Document doc = xmlKnob.getDocumentReadOnly();
            Document docOwner = element.getOwnerDocument();
            Node newNode = docOwner.importNode(doc.getDocumentElement(), true);
            element.appendChild(newNode);
        }
    }

    private void addAttributes(Document doc,
                               Element parent,
                               List<XacmlRequestBuilderAssertion.AttributeTreeNodeTag> attributeTreeNodeTags,
                               Map<String, Object> vars,
                               PolicyEnforcementContext context,
                               XacmlAssertionEnums.XacmlVersionType xacmlVersion)
    {
        for(XacmlRequestBuilderAssertion.AttributeTreeNodeTag attributeTreeNodeTag : attributeTreeNodeTags) {
            if(attributeTreeNodeTag instanceof XacmlRequestBuilderAssertion.Attribute) {
                XacmlRequestBuilderAssertion.Attribute attribute = (XacmlRequestBuilderAssertion.Attribute) attributeTreeNodeTag;
                addAttribute(doc, parent, attribute, vars, xacmlVersion);
            } else if(attributeTreeNodeTag instanceof XacmlRequestBuilderAssertion.MultipleAttributeConfig) {
                addXpathAttributes(doc, parent, (XacmlRequestBuilderAssertion.MultipleAttributeConfig) attributeTreeNodeTag, vars, context);
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

    private void addXpathAttributes(final Document doc,
                                       final Element parent,
                                       final XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig,
                                       final Map<String, Object> contextVariables,
                                       final PolicyEnforcementContext context){
        //Determine if any multi context var's being used outside of Value
        Set<XacmlRequestBuilderAssertion.MultipleAttributeConfigField> allFields = multipleAttributeConfig.getAllFields();
        allFields.remove(multipleAttributeConfig.getValueField());
        boolean isUsingMultiValuedCtxVariables = areAnyIterableFieldsReferencingMultiValuedVariables(contextVariables, allFields);

        int smallestCtxVarSize = 0;
        if(isUsingMultiValuedCtxVariables){
            List<String> allValues = new ArrayList<String>();
            for(XacmlRequestBuilderAssertion.MultipleAttributeConfigField configField: allFields){
                if(configField == multipleAttributeConfig.getValueField()) continue;
                allValues.add(configField.getValue());
            }
            smallestCtxVarSize =  getMinMultiValuedCtxVariableReferenced( allValues, contextVariables, false);
        }
        //Now we know if we are using a multi valued context variable

        //Will the base xpath, if defined, be part of the iteration?
        boolean iteratingOnBaseXPath = isXpathBaseUsedForIteration(multipleAttributeConfig, allFields);
        //check the base is not null
        if(iteratingOnBaseXPath &&
                (multipleAttributeConfig.getXpathBase() == null || multipleAttributeConfig.getXpathBase().isEmpty())){
            throw new IllegalStateException("If any fields are relative xpath, then xpath base must be supplied");
        }


        final Document inputDoc = getDocumentFromSource(multipleAttributeConfig, context);
        if(inputDoc == null) return;
        
        //Execute the base xpath expression, if any, to get information on iteration
        XpathResultIterator xpathResultSetIterator = null;

        if(iteratingOnBaseXPath){
            xpathResultSetIterator =
                    executeXpathExpression(
                            inputDoc,
                            multipleAttributeConfig.getXpathBase(),
                            multipleAttributeConfig.getNamespaces());
            if(xpathResultSetIterator == null){
                //log
                return;
            }
        }

        //for convenience we will create a local variable for each field being processed
        final XacmlRequestBuilderAssertion.MultipleAttributeConfigField idField = multipleAttributeConfig.getIdField();
        final XacmlRequestBuilderAssertion.MultipleAttributeConfigField dataTypeField = multipleAttributeConfig.getDataTypeField();
        final XacmlRequestBuilderAssertion.MultipleAttributeConfigField issuerField = multipleAttributeConfig.getIssuerField();
        final XacmlRequestBuilderAssertion.MultipleAttributeConfigField issueInstantField = multipleAttributeConfig.getIssueInstantField();
        final XacmlRequestBuilderAssertion.MultipleAttributeConfigField valueField = multipleAttributeConfig.getValueField();

        final Map<String, String> namespaces = multipleAttributeConfig.getNamespaces();
        //now we know everything required to define the iteration
        int iterationCount = 0;
        boolean exitIteration;
        do{
            //it's possible that all the values are single valued context variables
            //it's also possible that all the values are absolute xpaths - there is no iteration on absolute xpaths
            //it's also possible that all values are empty that is the only case in which we do nothing
            //attribute id and datatype are required, issuer and issuerinstant are optional
            //attributevalue is required

            //our minimum information at each iteration is - id, datatype and value - they must exist

            //do we have an xpath result? do we have multi valued vars? are we over the minimum value?
            //Do we have more xpath results?
            boolean xPathHasMore = false;
            if(xpathResultSetIterator != null){
                if(xpathResultSetIterator.hasNext()){
                    xPathHasMore = true;
                }
            }

            //at this point we may have xpath results or we may need to execute single absolute xpath expressions
            //or pick out static strings or single context variables

            //Implies xpathResultSetIterator is not null
            ElementCursor resultCursor = null;
            if(xPathHasMore) resultCursor = xpathResultSetIterator.nextElementAsCursor();


            String id = (xPathHasMore && idField.getIsXpath() && idField.getIsRelative())?
                    getRelativeXpathValueForField(resultCursor, idField.getValue(), namespaces)
                    : getValueForField(idField, inputDoc, namespaces, contextVariables, iterationCount);

            String dataType = (xPathHasMore && dataTypeField.getIsXpath() && dataTypeField.getIsRelative())?
                    getRelativeXpathValueForField( resultCursor, dataTypeField.getValue(), namespaces)
                    : getValueForField(dataTypeField, inputDoc, namespaces, contextVariables, iterationCount);

            String issuer = (xPathHasMore && issuerField.getIsXpath() && issuerField.getIsRelative())?
                    getRelativeXpathValueForField( resultCursor, issuerField.getValue(), namespaces)
                    : getValueForField(issuerField, inputDoc, namespaces, contextVariables, iterationCount);

            String issuerInstant = (xPathHasMore && issueInstantField.getIsXpath() && issueInstantField.getIsRelative())?
                    getRelativeXpathValueForField(resultCursor, issueInstantField.getValue(), namespaces)
                    : getValueForField(issueInstantField, inputDoc, namespaces, contextVariables, iterationCount);

            //also work out value - as it cannot change between iterations

            //check our minimum requirements
            //id, datatype and value not having a value means no more <Attribute>s can be created
            if(id == null || id.isEmpty()) return;
            if(dataType == null || dataType.isEmpty()) return;

            //our last requirement is the value field, which has the ability to be an absolute or relative
            //xpath expression, single or multi valued context variable or a string

            List<AttributeValue> attributeValues = getAttributeValues(inputDoc,
                    namespaces,
                    valueField,
                    contextVariables,
                    resultCursor);

            if(attributeValues.isEmpty()) return;

            //Create the Attribute
            Element attributeElement = addAttributeElement(doc, id, dataType, issuer, issuerInstant);

            //Add all applicable AttributeValues
            for(AttributeValue av: attributeValues){
                Element attributeValueElement = doc.createElementNS(
                        assertion.getXacmlVersion().getNamespace(), "AttributeValue");
                
                boolean successfullyAdded = av.addMeToAnElement(attributeValueElement, contextVariables);

                if(successfullyAdded){
                    //this is the only place where the parent get's an <Attribute> added to it
                    attributeElement.appendChild(attributeValueElement);
                    parent.appendChild(attributeElement);
                }

                if(assertion.getXacmlVersion() != XacmlAssertionEnums.XacmlVersionType.V2_0) break;
            }

            //look for exit condition
            //we will attempt to create an Attribute once only, after that we need to find a reason to continue
            iterationCount++;

            exitIteration = true;

            //determine state of xpath iteration
            boolean xpathMoreResults = false;
            if(xpathResultSetIterator != null){
                if(xpathResultSetIterator.hasNext()){
                    xpathMoreResults = true;
                }
            }

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
            boolean bothIterateMethodsBeingUsed = xpathResultSetIterator != null && isUsingMultiValuedCtxVariables;

            if(bothIterateMethodsBeingUsed){
                //when both methods are being used, then both must say it's ok to continue
                exitIteration = !(multiValueMoreResults && xpathMoreResults); 
            }else{
                //check xpath
                if(xpathResultSetIterator != null){
                    exitIteration = !xpathMoreResults;
                }else if(isUsingMultiValuedCtxVariables){
                    exitIteration = !multiValueMoreResults;
                }
            }
        } while(!exitIteration);

    }

    private XpathResultIterator executeXpathExpression(Document inputDoc, String xpath, Map<String, String> namespaces){
        if(xpath == null || xpath.isEmpty()) return null;
        
        XpathResultIterator xpathResultSetIterator = null;

        ElementCursor cursor = new DomElementCursor(inputDoc);
        cursor.moveToRoot();
        try {
            XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xpath, namespaces).compile(),null, true);
            if(xpathResult.getType() == XpathResult.TYPE_NODESET) {
                //our base xpath has evaluted to a result set, now our iterator it is no longer null
                xpathResultSetIterator = xpathResult.getNodeSet().getIterator();
            }
        } catch (XPathExpressionException e) {
            return null;
        } catch (InvalidXpathException e) {
            return null;
        }
        return xpathResultSetIterator;
    }

    private boolean isXpathBaseUsedForIteration(XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig, Set<XacmlRequestBuilderAssertion.MultipleAttributeConfigField> allFields) {
        boolean iteratingOnBaseXPath = false;
        for(XacmlRequestBuilderAssertion.MultipleAttributeConfigField configField: allFields){
            if(configField.getIsXpath() && configField.getIsRelative()){
                iteratingOnBaseXPath = true;
                break;
            }
        }
        return iteratingOnBaseXPath;
    }

    /**
     * Iterable fields can contain multi valued context variables. If a field contains one, the contents of the variable
     * cannot include any messages
     * @param contextVariables
     * @param allFields
     * @return
     */
    private boolean areAnyIterableFieldsReferencingMultiValuedVariables(Map<String, Object> contextVariables,
                                                                Set<XacmlRequestBuilderAssertion.MultipleAttributeConfigField> allFields) {
        boolean isUsingMultiValuedCtxVariables = false;
        for(XacmlRequestBuilderAssertion.MultipleAttributeConfigField configField: allFields){
            if(!configField.getIsXpath()){
                if(fieldValueContainMultiValuedContextVariable(configField.getValue(), contextVariables)){
                    isUsingMultiValuedCtxVariables = true;
                    //check for Message content
                    List<Object> ctxValues =
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

    private Element addAttributeElement(Document doc, String id, String dataType, String issuer, String issuerInstant) {
        Element attributeElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Attribute");
        attributeElement.setAttribute("AttributeID", id);
        attributeElement.setAttribute("DataType", dataType);
        if(issuer != null && !issuer.isEmpty()) {
            attributeElement.setAttribute("Issuer", issuer);
        }

        if(assertion.getXacmlVersion() == XacmlAssertionEnums.XacmlVersionType.V1_0) {
            if(issuerInstant != null && issuerInstant.isEmpty()) {
                attributeElement.setAttribute("IssueInstant", issuerInstant);
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
     * @param configField
     * @param multiVarIndex used as the index to extract the correct value from a multi valued context variable
     * @return the resolved String value for this field
     */
    private String getValueForField(final XacmlRequestBuilderAssertion.MultipleAttributeConfigField configField,
                                    final Document document,
                                    final Map<String, String> namespaces,
                                    final Map<String, Object> contextVariables,
                                    final int multiVarIndex){
        if(configField.getIsXpath() && configField.getIsRelative())
            throw new IllegalArgumentException("If xpath, field must not be relative");

        if(configField.getName().equalsIgnoreCase("Value"))
            throw new UnsupportedOperationException("method cannot be used for value field");

        if(configField.getIsXpath()){
            return getAbsoluteXPathResult(configField, document, namespaces);
        }else{
            //Determine if it's a multi valued context variable
            if(fieldValueContainMultiValuedContextVariable(configField.getValue(), contextVariables)){
                List<Object> multiValuedVariable =
                        getMultiValuedContextVariable(configField.getValue(), contextVariables, false);
                if(multiValuedVariable.size() < multiVarIndex){
                    //if this happens its a coding error
                    throw new IllegalStateException("" +
                            "Multi valued context variable found does not contain enough elements");                    
                }

                String value = multiValuedVariable.get(multiVarIndex).toString();
                return ExpandVariables.process(value, contextVariables, auditor, true);
            }
            //it might be a single valued context variable
            //or just a string
            return ExpandVariables.process(configField.getValue(), contextVariables, auditor, true);
        }
    }

    /**
     *
     * @param configField
     * @param document
     * @param namespaces
     * @return result of xpath expression. Can be null. Only ever a String value - expects to find a text node
     */
    private String getAbsoluteXPathResult(final XacmlRequestBuilderAssertion.MultipleAttributeConfigField configField,
                                          final Document document,
                                          final Map<String, String> namespaces) {
        ElementCursor cursor = new DomElementCursor(document);
        cursor.moveToRoot();
        XpathResult xpathResult;
        try {
            xpathResult = cursor.getXpathResult(new XpathExpression(configField.getValue(), namespaces).compile(), null, true);
        } catch (XPathExpressionException e) {
            return null;
        } catch (InvalidXpathException e) {
            return null;
        }
        if(xpathResult.getType() == XpathResult.TYPE_NODESET) {
            if(xpathResult.getNodeSet().size() < 1) {
                return null;
            } else {
                return xpathResult.getNodeSet().getNodeValue(0);
            }
        } else if(xpathResult.getType() == XpathResult.TYPE_BOOLEAN) {
            return Boolean.toString(xpathResult.getBoolean());
        } else if(xpathResult.getType() == XpathResult.TYPE_NUMBER) {
            return Double.toString(xpathResult.getNumber());
        } else if(xpathResult.getType() == XpathResult.TYPE_STRING) {
            return xpathResult.getString();
        } else {
            return null;
        }
    }

    /**
    * Evaluate a relative xpath expression which will evaluate to a text node, or if a ndoeset, the first node
    * @param cursor to evalute relative xpath against
    * @param xpath xpath expression
    * @param namespaces any required namespaces
    * @return the String value of the xpath expression. Can be the emtpy string, but not null
    */
     private String getRelativeXpathValueForField(ElementCursor cursor, String xpath, Map<String, String> namespaces) {
         try {
             XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xpath, namespaces).compile(), null, true);
             if(xpathResult.getType() == XpathResult.TYPE_NODESET) {
                 if (xpathResult.getNodeSet().size() < 1 || xpathResult.getNodeSet().getType(0) == XpathResult.TYPE_NODESET) {
                     return "";
                 } else {
                     return xpathResult.getNodeSet().getNodeValue(0);
                 }
             } else if(xpathResult.getType() == XpathResult.TYPE_BOOLEAN) {
                 return Boolean.toString(xpathResult.getBoolean());
             } else if(xpathResult.getType() == XpathResult.TYPE_NUMBER) {
                 return Double.toString(xpathResult.getNumber());
             } else if(xpathResult.getType() == XpathResult.TYPE_STRING) {
                 return xpathResult.getString();
             } else {
                 return "";
             }
         } catch(InvalidXpathException ixe) {
             return "";
         } catch(XPathExpressionException xee) {
             return "";
         }
     }

    /**
     * Evaluate an absolute xpath expression and return the list of results wrapped in XpathResultWrapper
     * @param valueField contains the xpath expression. isXpath() should return true
     * @param document used to create DomElementCursor from
     * @param namespaces any referenced namespaces from the xpath expression
     * @return result of xpath expression. Cannot be null, but can be empty
     */
    private List<XpathResultWrapper> getAbsoluteXPathResultForValue(
            final XacmlRequestBuilderAssertion.MultipleAttributeConfigField valueField,
                                          final Document document,
                                          final Map<String, String> namespaces) {
        ElementCursor cursor = new DomElementCursor(document);
        cursor.moveToRoot();
        XpathResult xpathResult;

        try {
            xpathResult = cursor.getXpathResult(
                    new XpathExpression(valueField.getValue(), namespaces).compile(), null, true);
        } catch (XPathExpressionException e) {
            return Collections.emptyList();
        } catch (InvalidXpathException e) {
            return Collections.emptyList();
        }

        return processXPathResult(xpathResult);
    }

     /**
     * Evaluate an relative xpath expression and return the list of results wrapped in XpathResultWrapper
     * @param cursor the cursor to evaluate the relative xpath expression against
     * @param xpath the string relative xpath expression
     * @param namespaces any referenced namespaces from the xpath expression
     * @return result of xpath expression. Cannot be null, but can be empty
     */
    private List<XpathResultWrapper> getRelativeXPathResult(final ElementCursor cursor,
                                                            final String xpath,
                                                            final Map<String, String> namespaces) {
        XpathResult xpathResult;
        try {
            xpathResult = cursor.getXpathResult(new XpathExpression(xpath, namespaces).compile(), null, true);
        } catch(InvalidXpathException ixe) {
            return Collections.emptyList();
        } catch(XPathExpressionException xee) {
            return Collections.emptyList();
        }

        return processXPathResult(xpathResult);
    }

    /**
     * AttributeValue knows how to add itself to a Element representing an <AttributeValue>
     */
    private class AttributeValue{
        private Message message;
        private String content;//can be a string containing content variable references
        private XpathResultWrapper wrapper;

        public AttributeValue(Message message) {
            this.message = message;
        }

        public AttributeValue(String content) {
            this.content = content;
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
        public boolean addMeToAnElement(Element element, Map<String, Object> contextVariables){

            //am I a String?
            if(content != null){
                element.appendChild(element.getOwnerDocument().createTextNode(
                        ExpandVariables.process(content, contextVariables, auditor, true)));
                return true;
            }

            //am I a Message?
            if(message != null){
                try {
                    addXmlMessageVariableAsAttributeValue(message, element);
                } catch (Exception e) {
                    //log
                    return false;
                }
//                }finally{
//                    message.close();
//                }
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
            
            return false;
        }

    }

    /**
     * From all possible inputs used to create AttributeValues, create a list of all <AttributeValue>s which
     * should be added to the <Attribute> element being created.
     * @param inputDoc - required if valueField contains an absolute xpath expression
     * @param namespaces any referenced namespaces from any xpath expression
     * @param valueField the field representing the AttributeValue
     * @param contextVariables all available context variables
     * @param resultCursor required if valueField is a relative xpath expression
     * @return list of AttributeValue, never null, but can be empty
     */
    private List<AttributeValue> getAttributeValues(
            Document inputDoc,
            Map<String, String> namespaces,
            XacmlRequestBuilderAssertion.MultipleAttributeConfigField valueField,
            Map<String, Object> contextVariables,
            ElementCursor resultCursor){
        List<AttributeValue> returnList = new ArrayList<AttributeValue>();

        boolean fieldContainsMultiValuedContextVariable =
                fieldValueContainMultiValuedContextVariable(valueField.getValue(), contextVariables);

        if(fieldContainsMultiValuedContextVariable){
            List<Object> contextVars = getMultiValuedContextVariable(valueField.getValue(), contextVariables, false);
            //contextVars contains String and or Messages
            for(Object o: contextVars){
                AttributeValue av;
                if(o instanceof Message){
                    Message m = (Message) o;
                    av = new AttributeValue(m);
                }else{
                    String value = ExpandVariables.process(o.toString(), contextVariables, auditor, true);
                    av = new AttributeValue(value);
                }
                returnList.add(av);
            }
            return returnList;
        }

        if(valueField.getIsXpath()){
            List<XpathResultWrapper> xpathResults;
            if(!valueField.getIsRelative()){
                xpathResults = getAbsoluteXPathResultForValue(valueField, inputDoc, namespaces);
            }else{
                if(resultCursor == null)
                    throw new NullPointerException("resultCursor cannot be null when AttributeValue is using a" +
                            " relative xpath expression");
                xpathResults = getRelativeXPathResult(resultCursor, valueField.getValue(), namespaces);
            }
            
            for(XpathResultWrapper wrapper: xpathResults){
                AttributeValue av = new AttributeValue(wrapper);
                returnList.add(av);
            }

            return returnList;
        }

        //is it a string referencing a message variable?
        String [] v = Syntax.getReferencedNames(valueField.getValue());
        if(v.length != 0){
            Object o = contextVariables.get(v[0]);
            if(o instanceof Message){
                AttributeValue av = new AttributeValue((Message)o);
                returnList.add(av);
                return returnList;
            }
        }
        //it's just a string or a single valued context variable of type string
        AttributeValue av = new AttributeValue(valueField.getValue());
        returnList.add(av);
        return returnList;
    }

    /**
     * Given the xpathResults, process it wrapping the results in XPathResultWrappers. This processing cares
     * about two types of results - Elements or text. This is reflected in XpathResultWrapper
     * @param xpathResult
     * @return
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

    private Document getDocumentFromSource(XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig,
                                           PolicyEnforcementContext context){
        Document document = null;
        try {
            switch(multipleAttributeConfig.getMessageSource()) {
                case DEFAULT_REQUEST:
                    document = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    break;
                case DEFAULT_RESPONSE:
                    document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    break;
                case CONTEXT_VARIABLE:
                    document = ((Message)context.getVariable(multipleAttributeConfig.getMessageSourceContextVar())).getXmlKnob().getDocumentReadOnly();
                    break;
                default:
                    throw new IllegalStateException("Unsupported message source found");//only happen if enum changes
            }
        } catch(SAXException saxe) {
            return document;
        } catch(NoSuchVariableException nsve) {
            return document;
        } catch(IOException ioe) {
            return document;
        }
        return document;
    }

   private HashSet<String> valueContainsMultiValuedVars(XacmlRequestBuilderAssertion.AttributeValue attributeValue, Map<String, Object> vars) {
        HashSet<String> multiValuedVars = new HashSet<String>();

        for(Map.Entry<String, String> entry : attributeValue.getAttributes().entrySet()) {
            String[] v = Syntax.getReferencedNames(entry.getKey());
            for(String s : v) {
                if(vars.containsKey(s) && (vars.get(s) instanceof Object[] || vars.get(s) instanceof List)) {
                    multiValuedVars.add(s);
                }
            }

            v = Syntax.getReferencedNames(entry.getValue());
            for(String s : v) {
                if(vars.containsKey(s) && (vars.get(s) instanceof Object[] || vars.get(s) instanceof List)) {
                    multiValuedVars.add(s);
                }
            }
        }

        String[] v = Syntax.getReferencedNames(attributeValue.getContent());
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
    private final StashManagerFactory stashManagerFactory;
}
