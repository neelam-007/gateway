package com.l7tech.external.assertions.xacmlpdp;

import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.SHORT_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.LONG_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_ICON;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_FOLDERS;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_ADVICE_CLASSNAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_EXTERNAL_NAME;
import com.l7tech.policy.wsp.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;

import java.util.*;
import java.io.Serializable;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 31-Mar-2009
 * Time: 7:42:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderAssertion extends Assertion implements UsesVariables, SetsVariables, Cloneable {

    public static class XmlTag {
        private Map<String, String> attributes = new HashMap<String, String>();
        private String content = "";
        private boolean repeat = false;

        public XmlTag() {
        }

        public XmlTag(Map<String, String> attributes, String content) {
            this.attributes = attributes;
            this.content = content;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean getRepeat() {
            return repeat;
        }

        public void setRepeat(boolean repeat) {
            this.repeat = repeat;
        }
    }

    public static interface RequestTag extends Serializable {
    }

    public static class Value extends XmlTag implements RequestTag, Cloneable {
        public Value() {
        }

        public Value(Map<String, String> attributes, String content) {
            super(attributes, content);
        }

        public String toString() {
            return "Value";
        }

        public Object clone() {
            Value retVal = new Value(new HashMap<String, String>(getAttributes()), getContent());
            retVal.setRepeat(getRepeat());

            return retVal;
        }
    }

    public static interface AttributeType extends RequestTag, Cloneable {
        public Object clone();
    }

    public static class Attribute implements AttributeType {
        private String id = "";
        private String dataType = "";
        private String issuer = "";
        private String issueInstant = "";
        private List<Value> values = new ArrayList<Value>();

        public Attribute() {
        }

        public Attribute(String id, String dataType, String issuer, List<Value> values) {
            this.id = id;
            this.dataType = dataType;
            this.issuer = issuer;
            this.values = values;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getIssueInstant() {
            return issueInstant;
        }

        public void setIssueInstant(String issueInstant) {
            this.issueInstant = issueInstant;
        }

        public List<Value> getValues() {
            return values;
        }

        public void setValues(List<Value> values) {
            this.values = values;
        }

        public String toString() {
            return "Attribute";
        }

        public Object clone() {
            List<Value> clonedValues = new ArrayList<Value>(values.size());
            for(Value value : values) {
                clonedValues.add((Value)value.clone());
            }
            
            return new Attribute(id, dataType, issuer, clonedValues);
        }
    }

    public static class XpathMultiAttrField implements Serializable, Cloneable {
        private String name;
        private String value = "";
        private boolean isXpath;
        private boolean isRelative;

        public XpathMultiAttrField() {
        }

        public XpathMultiAttrField(String name) {
            this.name = name;
        }

        public XpathMultiAttrField(String name, String value, boolean isXpath, boolean isRelative) {
            this.name = name;
            this.value = value;
            this.isXpath = isXpath;
            this.isRelative = isRelative;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean getIsXpath() {
            return isXpath;
        }

        public void setIsXpath(boolean isXpath) {
            this.isXpath = isXpath;
        }

        public boolean getIsRelative() {
            return isRelative;
        }

        public void setIsRelative(boolean isRelative) {
            this.isRelative = isRelative;
        }

        public Object clone() {
            XpathMultiAttrField clone = new XpathMultiAttrField();
            clone.setName(name);
            clone.setValue(value);
            clone.setIsXpath(isXpath);
            clone.setIsRelative(isRelative);

            return clone;
        }
    }

    public static class XpathMultiAttr implements AttributeType {
        public static enum MessageSource {
            REQUEST,
            RESPONSE,
            CONTEXT_VARIABLE
        }

        private MessageSource messageSource = MessageSource.REQUEST;
        private String messageSourceContextVar = "";
        private String xpathBase = "";
        private Map<String, String> namespaces;
        private XpathMultiAttrField idField = new XpathMultiAttrField("id");
        private XpathMultiAttrField dataTypeField = new XpathMultiAttrField("dataType");
        private XpathMultiAttrField issuerField = new XpathMultiAttrField("issuer");
        private XpathMultiAttrField issueInstantField = new XpathMultiAttrField("issueInstant");
        private XpathMultiAttrField valueField = new XpathMultiAttrField("value");

        public XpathMultiAttr() {
        }

        public XpathMultiAttr(MessageSource messageSource,
                              String messageSourceContextVar,
                              String xpathBase,
                              Map<String, String> namespaces,
                              XpathMultiAttrField idField,
                              XpathMultiAttrField dataTypeField,
                              XpathMultiAttrField issuerField,
                              XpathMultiAttrField issueInstantField,
                              XpathMultiAttrField valueField)
        {
            this.messageSource = messageSource;
            this.messageSourceContextVar = messageSourceContextVar;
            this.xpathBase = xpathBase;
            this.namespaces = namespaces;
            this.idField = idField;
            this.dataTypeField = dataTypeField;
            this.issuerField = issuerField;
            this.issueInstantField = issueInstantField;
            this.valueField = valueField;
        }

        public MessageSource getMessageSource() {
            return messageSource;
        }

        public void setMessageSource(MessageSource messageSource) {
            this.messageSource = messageSource;
        }

        public String getMessageSourceContextVar() {
            return messageSourceContextVar;
        }

        public void setMessageSourceContextVar(String messageSourceContextVar) {
            this.messageSourceContextVar = messageSourceContextVar;
        }

        public String getXpathBase() {
            return xpathBase;
        }

        public void setXpathBase(String xpathBase) {
            this.xpathBase = xpathBase;
        }

        public Map<String, String> getNamespaces() {
            return namespaces;
        }

        public void setNamespaces(Map<String, String> namespaces) {
            this.namespaces = namespaces;
        }

        public XpathMultiAttrField getIdField() {
            return idField;
        }

        public void setIdField(XpathMultiAttrField idField) {
            this.idField = idField;
        }

        public XpathMultiAttrField getDataTypeField() {
            return dataTypeField;
        }

        public void setDataTypeField(XpathMultiAttrField dataTypeField) {
            this.dataTypeField = dataTypeField;
        }

        public XpathMultiAttrField getIssuerField() {
            return issuerField;
        }

        public void setIssuerField(XpathMultiAttrField issuerField) {
            this.issuerField = issuerField;
        }

        public XpathMultiAttrField getIssueInstantField() {
            return issueInstantField;
        }

        public void setIssueInstantField(XpathMultiAttrField issueInstantField) {
            this.issueInstantField = issueInstantField;
        }

        public XpathMultiAttrField getValueField() {
            return valueField;
        }

        public void setValueField(XpathMultiAttrField valueField) {
            this.valueField = valueField;
        }

        public String toString() {
            return "XPath Multiple Attributes";
        }

        public Object clone() {
            XpathMultiAttr clone = new XpathMultiAttr();
            clone.setMessageSource(messageSource);
            clone.setMessageSourceContextVar(messageSourceContextVar);
            clone.setXpathBase(xpathBase);
            clone.setNamespaces(new HashMap<String, String>(namespaces));
            clone.setIdField((XpathMultiAttrField)idField.clone());
            clone.setDataTypeField((XpathMultiAttrField)dataTypeField.clone());
            clone.setIssuerField((XpathMultiAttrField)issuerField.clone());
            clone.setValueField((XpathMultiAttrField)valueField.clone());

            return clone;
        }
    }

    public static abstract class AttributeHolderTag implements RequestTag {
        private List<AttributeType> attributes = new ArrayList<AttributeType>();

        public AttributeHolderTag() {
        }

        public AttributeHolderTag(List<AttributeType> attributes) {
            this.attributes = attributes;
        }

        public List<AttributeType> getAttributes() {
            return attributes;
        }

        public void setAttributes(List<AttributeType> attributes) {
            this.attributes = attributes;
        }

        protected void copyFields(AttributeHolderTag receiver) {
            for(AttributeType attributeType : attributes) {
                receiver.getAttributes().add((AttributeType)attributeType.clone());
            }
        }
    }

    public static class Subject extends AttributeHolderTag implements Cloneable {
        private String subjectCategory = "";

        public Subject() {
            super();
        }

        public Subject(String subjectCategory, List<AttributeType> attributes) {
            super(attributes);
            this.subjectCategory = subjectCategory;
        }

        public String getSubjectCategory() {
            return subjectCategory;
        }

        public void setSubjectCategory(String subjectCategory) {
            this.subjectCategory = subjectCategory;
        }

        public String toString() {
            return "Subject";
        }

        public Object clone() {
            Subject clone = new Subject();
            clone.setSubjectCategory(subjectCategory);
            copyFields(clone);

            return clone;
        }
    }

    public static class ResourceContent extends XmlTag implements RequestTag {
        public ResourceContent() {
        }

        public ResourceContent(Map<String, String> attributes, String content) {
            super(attributes, content);
        }

        public String toString() {
            return "ResourceContent";
        }

        public Object clone() {
            ResourceContent retVal = new ResourceContent(new HashMap<String, String>(getAttributes()), getContent());
            retVal.setRepeat(getRepeat());

            return retVal;
        }
    }

    public static class Resource extends AttributeHolderTag implements Cloneable {
        private ResourceContent resourceContent;

        public Resource() {
            super();
        }

        public Resource(ResourceContent resourceContent, List<AttributeType> attributes) {
            super(attributes);
            this.resourceContent = resourceContent;
        }

        public ResourceContent getResourceContent() {
            return resourceContent;
        }

        public void setResourceContent(ResourceContent resourceContent) {
            this.resourceContent = resourceContent;
        }

        public String toString() {
            return "Resource";
        }

        public Object clone() {
            Resource clone = new Resource();
            clone.setResourceContent(resourceContent);
            copyFields(clone);

            return clone;
        }
    }

    public static class Action extends AttributeHolderTag implements Cloneable {
        public Action() {
            super();
        }

        public Action(List<AttributeType> attributes) {
            super(attributes);
        }

        public String toString() {
            return "Action";
        }

        public Object clone() {
            Action clone = new Action();
            copyFields(clone);

            return clone;
        }
    }

    public static class Environment extends AttributeHolderTag implements Cloneable {
        public Environment() {
            super();
        }

        public Environment(List<AttributeType> attributes) {
            super(attributes);
        }

        public String toString() {
            return "Environment";
        }

        public Object clone() {
            Environment clone = new Environment();
            copyFields(clone);

            return clone;
        }
    }

    private XacmlAssertionEnums.XacmlVersionType xacmlVersion = XacmlAssertionEnums.XacmlVersionType.V2_0;
    private XacmlAssertionEnums.SoapVersion soapEncapsulation = XacmlAssertionEnums.SoapVersion.NONE;
    private XacmlAssertionEnums.MessageTarget outputMessageDestination = XacmlAssertionEnums.MessageTarget.REQUEST_MESSAGE;
    private String outputMessageVariableName;
    private List<Subject> subjects = new ArrayList<Subject>();
    private List<Resource> resources = new ArrayList<Resource>();
    private Action action;
    private Environment environment;

    public XacmlRequestBuilderAssertion() {
        subjects.add(new Subject());
        resources.add(new Resource());
        action = new Action();
        environment = new Environment();
    }

    public XacmlRequestBuilderAssertion(XacmlRequestBuilderAssertion assertion) {
        xacmlVersion = assertion.getXacmlVersion();
        soapEncapsulation = assertion.getSoapEncapsulation();
        outputMessageDestination = assertion.getOutputMessageDestination();
        outputMessageVariableName = assertion.getOutputMessageVariableName();

        for(Subject subject : assertion.getSubjects()) {
            subjects.add((Subject)subject.clone());
        }

        for(Resource resource : assertion.getResources()) {
            resources.add((Resource)resource.clone());
        }

        action = (assertion.getAction() == null) ? null : (Action)assertion.getAction().clone();
        environment = (assertion.getEnvironment() == null) ? null : (Environment)assertion.getEnvironment().clone();
    }

    public String[] getVariablesUsed() {
        List<String[]> variables = new ArrayList<String[]>();
        for(Subject subject : subjects) {
            String[] v = Syntax.getReferencedNames(subject.getSubjectCategory());
            if(v.length > 0) {
                variables.add(v);
            }

            addAttributeTypeVariables(subject.getAttributes(), variables);
        }

        for(Resource resource : resources) {
            if(resource.getResourceContent() != null) {
                for(Map.Entry<String, String> entry : resource.getResourceContent().getAttributes().entrySet()) {
                    String[] v = Syntax.getReferencedNames(entry.getKey());
                    if(v.length > 0) {
                        variables.add(v);
                    }

                    v = Syntax.getReferencedNames(entry.getValue());
                    if(v.length > 0) {
                        variables.add(v);
                    }
                }

                String[] v = Syntax.getReferencedNames(resource.getResourceContent().getContent());
                if(v.length > 0) {
                    variables.add(v);
                }
            }

            addAttributeTypeVariables(resource.getAttributes(), variables);
        }

        addAttributeTypeVariables(action.getAttributes(), variables);

        if(environment != null) {
            addAttributeTypeVariables(environment.getAttributes(), variables);
        }

        int size = 0;
        for(String[] subset : variables) {
            size += subset.length;
        }

        String[] variablesUsed = new String[size];
        int offset = 0;
        for(String[] subset : variables) {
            System.arraycopy(subset, 0, variablesUsed, offset, subset.length);
            offset += subset.length;
        }

        return variablesUsed;
    }

    private void addAttributeTypeVariables(List<AttributeType> attributeTypes, List<String[]> variables) {
        for(AttributeType attributeType : attributeTypes) {
            if(attributeType instanceof Attribute) {
                Attribute attribute = (Attribute)attributeType;

                String[] v = Syntax.getReferencedNames(attribute.getId());
                if(v.length > 0) {
                    variables.add(v);
                }

                v = Syntax.getReferencedNames(attribute.getDataType());
                if(v.length > 0) {
                    variables.add(v);
                }

                v = Syntax.getReferencedNames(attribute.getIssuer());
                if(v.length > 0) {
                    variables.add(v);
                }

                v = Syntax.getReferencedNames(attribute.getIssueInstant());
                if(v.length > 0) {
                    variables.add(v);
                }

                for(Value value : attribute.getValues()) {
                    for(Map.Entry<String, String> entry : value.getAttributes().entrySet()) {
                        v = Syntax.getReferencedNames(entry.getKey());
                        if(v.length > 0) {
                            variables.add(v);
                        }

                        v = Syntax.getReferencedNames(entry.getValue());
                        if(v.length > 0) {
                            variables.add(v);
                        }
                    }

                    v = Syntax.getReferencedNames(value.getContent());
                    if(v.length > 0) {
                        variables.add(v);
                    }
                }
            } else if(attributeType instanceof XpathMultiAttr) {
                XpathMultiAttr multiAttr = (XpathMultiAttr)attributeType;

                String[] v = Syntax.getReferencedNames(multiAttr.getXpathBase());
                if(v.length > 0) {
                    variables.add(v);
                }

                v = Syntax.getReferencedNames(multiAttr.getIdField().getValue());
                if(v.length > 0) {
                    variables.add(v);
                }

                v = Syntax.getReferencedNames(multiAttr.getDataTypeField().getValue());
                if(v.length > 0) {
                    variables.add(v);
                }

                v = Syntax.getReferencedNames(multiAttr.getIssuerField().getValue());
                if(v.length > 0) {
                    variables.add(v);
                }

                v = Syntax.getReferencedNames(multiAttr.getValueField().getValue());
                if(v.length > 0) {
                    variables.add(v);
                }
            }
        }
    }

    public VariableMetadata[] getVariablesSet() {
        if(outputMessageDestination == XacmlAssertionEnums.MessageTarget.CONTEXT_VARIABLE) {
            return new VariableMetadata[] {new VariableMetadata(outputMessageVariableName, false, false, null, true, DataType.MESSAGE)};
        } else {
            return new VariableMetadata[0];
        }
    }

    public XacmlAssertionEnums.XacmlVersionType getXacmlVersion() {
        return xacmlVersion;
    }

    public void setXacmlVersion(XacmlAssertionEnums.XacmlVersionType xacmlVersion) {
        this.xacmlVersion = xacmlVersion;
    }

    public XacmlAssertionEnums.SoapVersion getSoapEncapsulation() {
        return soapEncapsulation;
    }

    public void setSoapEncapsulation(XacmlAssertionEnums.SoapVersion soapEncapsulation) {
        this.soapEncapsulation = soapEncapsulation;
    }

    public XacmlAssertionEnums.MessageTarget getOutputMessageDestination() {
        return outputMessageDestination;
    }

    public void setOutputMessageDestination(XacmlAssertionEnums.MessageTarget outputMessageDestination) {
        this.outputMessageDestination = outputMessageDestination;
    }

    public String getOutputMessageVariableName() {
        return outputMessageVariableName;
    }

    public void setOutputMessageVariableName(String outputMessageVariableName) {
        this.outputMessageVariableName = outputMessageVariableName;
    }
    
    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public Object clone() {
        return new XacmlRequestBuilderAssertion(this);
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "XACML Request Builder");
        meta.put(LONG_NAME, "Generate XACML Requests");

        //meta.put(PALETTE_NODE_NAME, "CentraSite Metrics Assertion");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xml" });

        meta.put(POLICY_NODE_NAME, "XACML Request Builder");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xacmlpdp.console.XacmlRequestBuilderDialog");
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.xacmlpdp.server.ServerXacmlRequestBuilderAssertion");

        meta.put(WSP_EXTERNAL_NAME, "XacmlRequestBuilderAssertion"); // keep same WSP name as pre-3.7 (Bug #3605)

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new Java5EnumTypeMapping(XacmlAssertionEnums.XacmlVersionType.class, "xacmlVersion"));
        othermappings.add(new Java5EnumTypeMapping(XacmlAssertionEnums.MessageTarget.class, "messageTarget"));
        othermappings.add(new Java5EnumTypeMapping(XacmlAssertionEnums.SoapVersion.class, "soapEncapsulation"));
        othermappings.add(new BeanTypeMapping(Subject.class, "subject"));
        othermappings.add(new CollectionTypeMapping(List.class, Subject.class, ArrayList.class, "subjectList"));
        othermappings.add(new BeanTypeMapping(Resource.class, "resource"));
        othermappings.add(new CollectionTypeMapping(List.class, Resource.class, ArrayList.class, "resourceList"));
        othermappings.add(new BeanTypeMapping(Action.class, "action"));
        othermappings.add(new BeanTypeMapping(Environment.class, "environment"));
        othermappings.add(new BeanTypeMapping(Attribute.class, "attribute"));
        othermappings.add(new BeanTypeMapping(Value.class, "attributeValue"));
        othermappings.add(new BeanTypeMapping(XpathMultiAttrField.class, "xpathMultipleAttributesField"));
        othermappings.add(new BeanTypeMapping(XpathMultiAttr.class, "xpathMultipleAttributes"));
        othermappings.add(new Java5EnumTypeMapping(XpathMultiAttr.MessageSource.class, "messageSource"));
        othermappings.add(new CollectionTypeMapping(List.class, AttributeType.class, ArrayList.class, "attributeList"));
        othermappings.add(new CollectionTypeMapping(List.class, Value.class, ArrayList.class, "valueList"));
        othermappings.add(new BeanTypeMapping(ResourceContent.class, "resourceContent"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:EchoRouting" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(XacmlRequestBuilderAssertion.class.getName() + ".metadataInitialized", Boolean.TRUE);
        return meta;
    }
}
