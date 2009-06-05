package com.l7tech.external.assertions.xacmlpdp.server;

import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.message.Message;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
 * To change this template use File | Settings | File Templates.
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
        if(assertion.getSoapEncapsulation() != XacmlRequestBuilderAssertion.SoapEncapsulationType.NONE) {
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
            Element subjectElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Subject");
            root.appendChild(subjectElement);

            if(subject.getSubjectCategory().length() > 0) {
                subjectElement.setAttribute("SubjectCategory", ExpandVariables.process(subject.getSubjectCategory(), vars, auditor, true));
            }

            addAttributes(doc, subjectElement, subject.getAttributes(), vars, context);
        }

        for(XacmlRequestBuilderAssertion.Resource resource : assertion.getResources()) {
            Element resourceElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Resource");
            root.appendChild(resourceElement);

            if(resource.getResourceContent() != null) {
                Element resourceContentElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "ResourceContent");
                resourceElement.appendChild(resourceContentElement);

                for(Map.Entry<String, String> entry : resource.getResourceContent().getAttributes().entrySet()) {
                    resourceContentElement.setAttribute(entry.getKey(), entry.getValue());
                }

                resourceContentElement.appendChild(doc.createTextNode(resource.getResourceContent().getContent()));
            }

            addAttributes(doc, resourceElement, resource.getAttributes(), vars, context);
        }

        Element actionElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Action");
        root.appendChild(actionElement);
        addAttributes(doc, actionElement, assertion.getAction().getAttributes(), vars, context);

        Element environmentElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Environment");
        root.appendChild(environmentElement);

        if(assertion.getEnvironment() != null) {
            addAttributes(doc, environmentElement, assertion.getEnvironment().getAttributes(), vars, context);
        }

        switch(assertion.getOutputMessageDestination()) {
            case REQUEST_MESSAGE:
                context.getRequest().initialize(doc);
                break;
            case RESPONSE_MESSAGE:
                context.getResponse().initialize(doc);
                break;
            case MESSAGE_VARIABLE:
                Message message = new Message(doc);
                context.setVariable(assertion.getOutputMessageVariableName(), message);
                break;
        }

        return AssertionStatus.NONE;
    }

    private void addAttribute(Document doc,
                              Element parent,
                              XacmlRequestBuilderAssertion.Attribute attribute,
                              Map<String, Object> vars)
    {
        Element attributeElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Attribute");
        attributeElement.setAttribute("AttributeId", ExpandVariables.process(attribute.getId(), vars, auditor, true));
        attributeElement.setAttribute("DataType", ExpandVariables.process(attribute.getDataType(), vars, auditor, true));
        parent.appendChild(attributeElement);

        if(attribute.getIssuer().length() > 0) {
            attributeElement.setAttribute("Issuer", ExpandVariables.process(attribute.getIssuer(), vars, auditor, true));
        }

        if(assertion.getXacmlVersion() == XacmlRequestBuilderAssertion.XacmlVersionType.V1_0) {
            if(attribute.getIssueInstant().length() > 0) {
                attributeElement.setAttribute("IssueInstant", ExpandVariables.process(attribute.getIssueInstant(), vars, auditor, true));
            }
        }

        for(XacmlRequestBuilderAssertion.Value value : attribute.getValues()) {
            HashSet<String> multiValuedVars = valueContainsMultiValuedVars(value, vars);
            if(value.getRepeat() && multiValuedVars.size() > 0) {
                HashMap<String, String> variableMap = new HashMap<String, String>();
                int i = 1;
                for(String s : multiValuedVars) {
                    variableMap.put(s, "ssg.internal.xacml.request.var" + i++);
                }

                HashMap<String, String> updatedAttributes = new HashMap<String, String>(value.getAttributes().size());
                for(Map.Entry<String, String> entry : value.getAttributes().entrySet()) {
                    String k = entry.getKey();
                    String v = entry.getValue();

                    for(Map.Entry<String, String> newEntry : variableMap.entrySet()) {
                        k = k.replaceAll("\\Q${" + newEntry.getKey() + "}\\E", "\\$\\{" + newEntry.getValue() + "\\}");
                        v = v.replaceAll("\\Q${" + newEntry.getKey() + "}\\E", "\\$\\{" + newEntry.getValue() + "\\}");
                    }

                    updatedAttributes.put(k, v);
                }

                String content = value.getContent();
                for(Map.Entry<String, String> entry : variableMap.entrySet()) {
                    content = content.replaceAll("\\Q${" + entry.getKey() + "}\\E", "\\$\\{" + entry.getValue() + "\\}");
                }

                int j = 0;
                while(true) {
                    // Check if done
                    boolean done = true;
                    for(Map.Entry<String, String> entry : variableMap.entrySet()) {
                        if(vars.get(entry.getKey()) instanceof Object[]) {
                            Object[] objArray = (Object[])vars.get(entry.getKey());
                            if(objArray.length > j) {
                                done = false;
                                vars.put(entry.getValue(), objArray[j]);
                            } else {
                                vars.put(entry.getValue(), "");
                            }
                        } else if(vars.get(entry.getKey()) instanceof List) {
                            List objList = (List)vars.get(entry.getKey());
                            if(objList.size() > j) {
                                done = false;
                                vars.put(entry.getValue(), objList.get(j));
                            } else {
                                vars.put(entry.getValue(), "");
                            }
                        }
                    }

                    if(done) {
                        break;
                    } else {
                        j++;
                    }

                    // Add an AttributeValue element
                    Element valueElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
                    attributeElement.appendChild(valueElement);

                    for(Map.Entry<String, String> entry : updatedAttributes.entrySet()) {
                        String name = ExpandVariables.process(entry.getKey(), vars, auditor, true);
                        String attrValue = ExpandVariables.process(entry.getValue(), vars, auditor, true);
                        valueElement.setAttribute(name, attrValue);
                    }

                    valueElement.appendChild(doc.createTextNode(ExpandVariables.process(content, vars, auditor, true)));

                    // For XACML 1.0 and 1.1, there cannot be more than one attribute value
                    if(assertion.getXacmlVersion() != XacmlRequestBuilderAssertion.XacmlVersionType.V2_0) {
                        break;
                    }
                }

                for(String newVariableName : variableMap.values()) {
                    vars.remove(newVariableName);
                }
            } else {
                Element valueElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
                attributeElement.appendChild(valueElement);

                for(Map.Entry<String, String> entry : value.getAttributes().entrySet()) {
                    valueElement.setAttribute(entry.getKey(), entry.getValue());
                }

                valueElement.appendChild(doc.createTextNode(ExpandVariables.process(value.getContent(), vars, auditor, true)));
            }
        }
    }

    private void addAttributes(Document doc,
                               Element parent,
                               List<XacmlRequestBuilderAssertion.AttributeType> attributeTypes,
                               Map<String, Object> vars,
                               PolicyEnforcementContext context)
    {
        for(XacmlRequestBuilderAssertion.AttributeType attributeType : attributeTypes) {
            if(attributeType instanceof XacmlRequestBuilderAssertion.Attribute) {
                XacmlRequestBuilderAssertion.Attribute attribute = (XacmlRequestBuilderAssertion.Attribute)attributeType;
                addAttribute(doc, parent, attribute, vars);
            } else if(attributeType instanceof XacmlRequestBuilderAssertion.XpathMultiAttr) {
                addXpathAttributes(doc, parent, (XacmlRequestBuilderAssertion.XpathMultiAttr)attributeType, vars, context);
            }
        }
    }

    private void addXpathAttributes(Document doc, Element parent, XacmlRequestBuilderAssertion.XpathMultiAttr xpathMultiAttr,
                                    Map<String, Object> vars, PolicyEnforcementContext context)
    {
        if(!xpathMultiAttr.getIdField().getIsXpath() && !xpathMultiAttr.getDataTypeField().getIsXpath() &&
                !xpathMultiAttr.getIssuerField().getIsXpath() && !xpathMultiAttr.getIssueInstantField().getIsXpath() &&
                !xpathMultiAttr.getValueField().getIsXpath())
        {
            Element attributeElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Attribute");
            parent.appendChild(attributeElement);
            attributeElement.setAttribute("AttributeID", ExpandVariables.process(xpathMultiAttr.getIdField().getValue(), vars, auditor, true));
            attributeElement.setAttribute("DataType", ExpandVariables.process(xpathMultiAttr.getDataTypeField().getValue(), vars, auditor, true));
            if(xpathMultiAttr.getIssuerField().getValue() != null && xpathMultiAttr.getIssuerField().getValue().length() > 0) {
                attributeElement.setAttribute("Issuer", ExpandVariables.process(xpathMultiAttr.getIssuerField().getValue(), vars, auditor, true));
            }

            if(assertion.getXacmlVersion() == XacmlRequestBuilderAssertion.XacmlVersionType.V1_0) {
                if(xpathMultiAttr.getIssueInstantField().getValue() != null && xpathMultiAttr.getIssueInstantField().getValue().length() > 0) {
                    attributeElement.setAttribute("IssueInstant", ExpandVariables.process(xpathMultiAttr.getIssueInstantField().getValue(), vars, auditor, true));
                }
            }

            Element valueElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
            attributeElement.appendChild(valueElement);
            valueElement.appendChild(doc.createTextNode(ExpandVariables.process(xpathMultiAttr.getValueField().getValue(), vars, auditor, true)));

            return;
        }

        Document inputDoc = null;
        try {
            switch(xpathMultiAttr.getMessageSource()) {
                case REQUEST:
                    inputDoc = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    break;
                case RESPONSE:
                    inputDoc = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    break;
                case CONTEXT_VARIABLE:
                    inputDoc = ((Message)context.getVariable(xpathMultiAttr.getMessageSourceContextVar())).getXmlKnob().getDocumentReadOnly();
                    break;
            }
        } catch(SAXException saxe) {
            return;
        } catch(NoSuchVariableException nsve) {
            return;
        } catch(IOException ioe) {
            return;
        }

        String idValue = getXpathFieldValue(xpathMultiAttr.getIdField(), vars, inputDoc, xpathMultiAttr.getNamespaces());
        String dataTypeValue = getXpathFieldValue(xpathMultiAttr.getDataTypeField(), vars, inputDoc, xpathMultiAttr.getNamespaces());
        String issuerValue = getXpathFieldValue(xpathMultiAttr.getIssuerField(), vars, inputDoc, xpathMultiAttr.getNamespaces());
        String issueInstantValue = "";
        if(assertion.getXacmlVersion() == XacmlRequestBuilderAssertion.XacmlVersionType.V1_0) {
            getXpathFieldValue(xpathMultiAttr.getIssueInstantField(), vars, inputDoc, xpathMultiAttr.getNamespaces());
        }
        String valueValue = getXpathFieldValue(xpathMultiAttr.getValueField(), vars, inputDoc, xpathMultiAttr.getNamespaces());

        List<String> idValues = new ArrayList<String>();
        List<String> dataTypeValues = new ArrayList<String>();
        List<String> issuerValues = new ArrayList<String>();
        List<String> issueInstantValues = new ArrayList<String>();
        List<List<String>> valueValues = new ArrayList<List<String>>();

        // Above values are only null if the corresponding field is a relative XPath expression
        if(idValue == null || dataTypeValue == null || issuerValue == null || issueInstantValue == null || valueValue == null) {
            try {
                ElementCursor cursor = new DomElementCursor(inputDoc);
                cursor.moveToRoot();

                XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xpathMultiAttr.getXpathBase(), xpathMultiAttr.getNamespaces()).compile(),null, true);
                if(xpathResult.getType() != XpathResult.TYPE_NODESET) {
                    return;
                }

                for(XpathResultIterator it = xpathResult.getNodeSet().getIterator();it.hasNext();) {
                    ElementCursor resultCursor = it.nextElementAsCursor();

                    if(idValue == null) {
                        idValues.add(addXpathMatchValueForField(resultCursor, xpathMultiAttr.getIdField().getValue(), xpathMultiAttr.getNamespaces()));
                    }
                    if(dataTypeValue == null) {
                        dataTypeValues.add(addXpathMatchValueForField(resultCursor, xpathMultiAttr.getDataTypeField().getValue(), xpathMultiAttr.getNamespaces()));
                    }
                    if(issuerValue == null) {
                        issuerValues.add(addXpathMatchValueForField(resultCursor, xpathMultiAttr.getIssuerField().getValue(), xpathMultiAttr.getNamespaces()));
                    }
                    if(issueInstantValue == null) {
                        issueInstantValues.add(addXpathMatchValueForField(resultCursor, xpathMultiAttr.getIssueInstantField().getValue(), xpathMultiAttr.getNamespaces()));
                    }

                    if(valueValue == null) {
                        valueValues.add(addXpathMatchValuesForField(resultCursor, xpathMultiAttr.getValueField().getValue(), xpathMultiAttr.getNamespaces()));
                    }
                }
            } catch(XPathExpressionException xee) {
                return;
            } catch(InvalidXpathException ixe) {
                return;
            }
        } else {
            // Handle the case where all fields are single value
            Element attributeElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Attribute");
            parent.appendChild(attributeElement);
            attributeElement.setAttribute("AttributeID", idValue);
            attributeElement.setAttribute("DataType", dataTypeValue);
            if(issuerValue.length() > 0) {
                attributeElement.setAttribute("Issuer", issuerValue);
            }
            if(issueInstantValue.length() > 0) {
                attributeElement.setAttribute("IssueInstant", issueInstantValue);
            }

            Element valueElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
            attributeElement.appendChild(valueElement);
            valueElement.appendChild(inputDoc.createTextNode(valueValue));

            return;
        }

        int i = 0;
        while(idValue == null && i < idValues.size() || dataTypeValue == null && i < dataTypeValues.size() ||
                issuerValue == null && i < issuerValues.size() || issueInstantValue == null && i < issueInstantValues.size() ||
                valueValue == null && i < valueValues.size())
        {
            Element attributeElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "Attribute");
            parent.appendChild(attributeElement);

            if(idValue != null) {
                attributeElement.setAttribute("AttributeID", idValue);
            } else if(i < idValues.size()) {
                attributeElement.setAttribute("AttributeID", idValues.get(i));
            }
            if(dataTypeValue != null) {
                attributeElement.setAttribute("DataType", dataTypeValue);
            } else if(i < dataTypeValues.size()) {
                attributeElement.setAttribute("DataType", dataTypeValues.get(i));
            }
            if(issuerValue != null) {
                if(issuerValue.length() > 0) {
                    attributeElement.setAttribute("Issuer", issuerValue);
                }
            } else if(i < issuerValues.size()) {
                String v = issuerValues.get(i);
                if(v.length() > 0) {
                    attributeElement.setAttribute("Issuer", v);
                }
            }

            if(assertion.getXacmlVersion() == XacmlRequestBuilderAssertion.XacmlVersionType.V1_0) {
                if(issueInstantValue != null) {
                    if(issueInstantValue.length() > 0) {
                        attributeElement.setAttribute("IssueInstant", issueInstantValue);
                    }
                } else if(i < issueInstantValues.size()) {
                    String v = issueInstantValues.get(i);
                    if(v.length() > 0) {
                        attributeElement.setAttribute("IssueInstant", v);
                    }
                }
            }

            if(valueValue != null) {
                Element valueElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
                attributeElement.appendChild(valueElement);
                valueElement.appendChild(doc.createTextNode(valueValue));
            } else if(i < valueValues.size()) {
                for(int j = 0;j < valueValues.get(i).size();j++) {
                    Element valueElement = doc.createElementNS(assertion.getXacmlVersion().getNamespace(), "AttributeValue");
                    attributeElement.appendChild(valueElement);
                    valueElement.appendChild(doc.createTextNode(valueValues.get(i).get(j)));

                    // XACML 1.0 and 1.1 only allow one AttributeValue for each Attribute element
                    if(assertion.getXacmlVersion() != XacmlRequestBuilderAssertion.XacmlVersionType.V2_0) {
                        break;
                    }
                }
            }

            i++;
        }
    }

    private String getXpathFieldValue(XacmlRequestBuilderAssertion.XpathMultiAttrField field,
                                      Map<String, Object> vars,
                                      Document inputDoc,
                                      Map<String, String> namespaces)
    {
        if(field.getIsXpath()) {
            if(field.getIsRelative()) {
                return null;
            } else {
                try {
                    ElementCursor cursor = new DomElementCursor(inputDoc);
                    cursor.moveToRoot();
                    XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(field.getValue(), namespaces).compile(), null, true);
                    if(xpathResult.getType() == XpathResult.TYPE_NODESET) {
                        if(xpathResult.getNodeSet().size() < 1) {
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
        }

        if(field.getValue() == null) {
            return "";
        }

        return ExpandVariables.process(field.getValue(), vars, auditor, true);
    }

    private String addXpathMatchValueForField(ElementCursor cursor, String xpath, Map<String, String> namespaces) {
        try {
            XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xpath, namespaces).compile(), null, true);
            if(xpathResult.getType() == XpathResult.TYPE_NODESET) {
                if(xpathResult.getNodeSet().size() < 1) {
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

    private List<String> addXpathMatchValuesForField(ElementCursor cursor, String xpath, Map<String, String> namespaces) {
        List<String> values = new ArrayList<String>();

        try {
            XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xpath, namespaces).compile(), null, true);
            if(xpathResult.getType() == XpathResult.TYPE_NODESET) {
                for(int i = 0;i < xpathResult.getNodeSet().size();i++) {
                    if(xpathResult.getNodeSet().getType(i) == XpathResult.TYPE_NODESET) {
                        values.add("");
                    } else {
                        values.add(xpathResult.getNodeSet().getNodeValue(i));
                    }
                }
            } else if(xpathResult.getType() == XpathResult.TYPE_BOOLEAN) {
                values.add(Boolean.toString(xpathResult.getBoolean()));
            } else if(xpathResult.getType() == XpathResult.TYPE_NUMBER) {
                values.add(Double.toString(xpathResult.getNumber()));
            } else if(xpathResult.getType() == XpathResult.TYPE_STRING) {
                values.add(xpathResult.getString());
            }
        } catch(InvalidXpathException ixe) {
        } catch(XPathExpressionException xee) {
        }
        //if we can't get the list of values, just return the empty list
        return values;
    }

    private HashSet<String> valueContainsMultiValuedVars(XacmlRequestBuilderAssertion.Value value, Map<String, Object> vars) {
        HashSet<String> multiValuedVars = new HashSet<String>();

        for(Map.Entry<String, String> entry : value.getAttributes().entrySet()) {
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

        String[] v = Syntax.getReferencedNames(value.getContent());
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
