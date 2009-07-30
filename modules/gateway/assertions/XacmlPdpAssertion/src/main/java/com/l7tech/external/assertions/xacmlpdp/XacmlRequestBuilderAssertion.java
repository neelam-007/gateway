package com.l7tech.external.assertions.xacmlpdp;

import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.wsp.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;
import static com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName.*;
import com.l7tech.external.assertions.xacmlpdp.console.XacmlConstants;
import com.l7tech.xml.xpath.XpathUtil;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.io.Serializable;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 31-Mar-2009
 * Time: 7:42:45 PM
 */
public class XacmlRequestBuilderAssertion extends Assertion implements UsesVariables, SetsVariables{
    /**
     * A GenericXmlElement is off the form <elementname attribute1="avlaue"...>Content, might just be a string, or
     * an xml fragment</elementname>
     *
     * Every instance of this class representing an xml Element can have a Map<String, String> of attributes and a
     * String content, which may be xml
     */
    public static class GenericXmlElement implements Cloneable, Serializable {
        private Map<String, String> attributes;
        private String content="";//I might be xml

        public GenericXmlElement() {
        }

        public Map<String, String> getAttributes() {
            if(attributes == null){
                 attributes = new HashMap<String, String>();
            }
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            if(attributes == null) throw new NullPointerException("attributes cannot be null");
            this.attributes = attributes;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            if(content == null) throw new NullPointerException("content cannot be null");
            this.content = content;
        }

        @Override
        public GenericXmlElement clone() {
            try {
                GenericXmlElement copy = (GenericXmlElement) super.clone();
                if ( attributes != null ) {
                    copy.attributes = new HashMap<String, String>(attributes);
                }

                return copy;
            } catch ( CloneNotSupportedException e ) {
                throw ExceptionUtils.wrap( e );
            }
        }
    }

    /**
     * XmlElementCanRepeatTag is implemented by any class which uses the generic AttributeValue dialog.
     * This dialog collects attributes for a single xml element, it's attributes and content, but can also allow
     * multi valued context variables to be entered, so that it can dynamically generate <AttributeValue>
     * elements. A class which is the model for this dialog can implement this interface to let the dialog know
     * it has this capability
     * <Attribute> can have more than 1 <AttributeValue> children
     * <Resource> can have 1 and only 1 <ResourceContent> children
     */
    public static interface XmlElementCanRepeatTag{
        boolean isCanElementHaveSameTypeSibilings();

        void setCanElementHaveSameTypeSibilings(boolean canElementHaveSameTypeSibilings);
    }

    /**
     * Convenience class to wrap a GenericXmlElement. Not limiting Elements like <AttributeValue>
     * or <ResourceContent> by subclassing GenericXmlElement, this class delegates to a GenericXmlElement,
     * providing subclasses the conveneince of not having to do this delegation.
     * The 'not limiting' happens as AttributeValue and ResourceContent should not share a type hierarchy, as they
     * are both goverened by different schema types and could change in the future
     */
    public static abstract class GenericXmlElementHolder implements Cloneable, Serializable {
        private GenericXmlElement xmlElement = new GenericXmlElement();

        protected GenericXmlElementHolder() {
        }

        public void setAttributes(Map<String, String> attributes){
            xmlElement.setAttributes(attributes);
        }

        public Map<String, String> getAttributes(){
            return xmlElement.getAttributes();
        }

        public void setContent(String content) {
            xmlElement.setContent(content);
        }

        public String getContent(){
            return xmlElement.getContent();
        }

        @Override
        public GenericXmlElementHolder clone() {
            try {
                GenericXmlElementHolder copy = (GenericXmlElementHolder) super.clone();
                if ( xmlElement != null ) {
                    copy.xmlElement = xmlElement.clone();
                }

                return copy;
            } catch ( CloneNotSupportedException e ) {
                throw ExceptionUtils.wrap( e );
            }
        }
    }

    /**
     * AttributeValue represents the <AttributeValue> element in a XACML request. This element can belong
     * to any Subject, Action, Resource or Environment.
     *
     * Presently AttributeValue does not care about what version of XACML the request is for. It's schema has not
     * changed yet, but it could and this can be dealt with later. It is a fundemental child element of <Attribute>
     * , which itself can be achild of any of the 4 major xacml request components
     */
    public static class AttributeValue extends GenericXmlElementHolder implements XmlElementCanRepeatTag{

        private boolean canElementHaveSameTypeSibilings;

        public AttributeValue() {
        }

        @Override
        public String toString() {
            return "AttributeValue";
        }

        /**
         * Ideally AttributeValue would not care about whether it can have siblings or not. Currently only Xacml 2.0
         * supports this. However the dialog for a single AttributeValue and multiple is shared, and so is the model
         * so we are ok with this for the moment
         * @return true if this AttributeValue has been configured to be able to have siblings
         */
        @Override
        public boolean isCanElementHaveSameTypeSibilings() {
            return canElementHaveSameTypeSibilings;
        }

        @Override
        public void setCanElementHaveSameTypeSibilings(boolean canElementHaveSameTypeSibilings) {
            this.canElementHaveSameTypeSibilings = canElementHaveSameTypeSibilings; 
        }
    }

    public static class ResourceContent extends GenericXmlElementHolder {
        public ResourceContent() {
        }

        @Override
        public String toString() {
            return "ResourceContent";
        }
    }

    /**
     * AttributeTreeNodeTag is implemented by both Attribute and MultipleAttributeConfig. The implementations are
     * listed here as the reason for this interface is to allow all nodes, which will end up as <Attribute> nodes,
     * to exist in the XACML Request Builder tree, as a direct child of either Subject, Request, Action or
     * Environment. The UI can process all these nodes generically with this interface
     */
    public static interface AttributeTreeNodeTag extends Cloneable, java.io.Serializable {
        AttributeTreeNodeTag clone();
    }

    public static class Attribute implements AttributeTreeNodeTag {
        private String id = "";
        private String dataType = "";
        private String issuer = "";
        private String issueInstant = "";
        private List<AttributeValue> attributeValues = new ArrayList<AttributeValue>();

        public Attribute() {
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

        public List<AttributeValue> getValues() {
            return attributeValues;
        }

        public void setValues(List<AttributeValue> attributeValues) {
            this.attributeValues = attributeValues;
        }

        @Override
        public String toString() {
            return "Attribute";
        }

        @Override
        public Attribute clone() {
            try {
                Attribute copy = (Attribute) super.clone();
                if ( attributeValues != null ) {
                    copy.attributeValues = new ArrayList<AttributeValue>();
                    for ( AttributeValue value : attributeValues ) {
                        copy.attributeValues.add( (AttributeValue) value.clone() );   
                    }
                }

                return copy;
            } catch ( CloneNotSupportedException e ) {
                throw ExceptionUtils.wrap( e );
            }
        }
    }

    /**
     * MultipleAttributeConfig stores the configuration which clients can use to build multiple Attribute elements
     * This configuration contains a base Xpath expression, and absolute or relative xpath expressions,  or single
     * string valued context variables for all the attributes and values an <Attribute> element can accept.
     *
     * MultipleAttributeConfig can appear as the User Object in the XACML Request Builder tree, as a sibling of
     * Attribute only. As a result it needs to be identifiable and processed differently when found in the tree
     */
    public static class MultipleAttributeConfig implements AttributeTreeNodeTag{
        public enum FieldName {
            ID("ID"),
            DATA_TYPE("Data Type"),
            ISSUER("Issuer"),
            ISSUE_INSTANT("Issue Instant"),
            VALUE("Value");

            private final String displayName;

            FieldName(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() {
                return displayName;
            }

            @Override
            public String toString() {
                return displayName;
            }
        }

        public enum FieldType {
            REGULAR("Regular", false),
            XPATH_ABSOLUTE("Absolute XPath", true),
            XPATH_RELATIVE("Relative XPath", true),
            CONTEXT_VARIABLE("Context Variable", false);

            private final String displayName;
            private boolean isXpath;

            FieldType(String displayName, boolean isXpath) {
                this.displayName = displayName;
                this.isXpath = isXpath;
            }

            @Override
            public String toString() {
                return displayName;
            }

            /**
             * @return true if the field is an XPath-type (either absolute or relative), false otherwise
             */
            public boolean isXpath() {
                return isXpath;
            }
        }

        private Map<String, Field> fields = new HashMap<String, Field>() {{
            put(ID.name(), new Field(ID));
            put(DATA_TYPE.name(), new Field(DATA_TYPE));
            put(ISSUER.name(), new Field(ISSUER));
            put(ISSUE_INSTANT.name(), new Field(ISSUE_INSTANT));
            put(VALUE.name(), new Field(VALUE));
        }};

        private XacmlAssertionEnums.MessageLocation messageSource = XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST;
        private String messageSourceContextVar = "";
        private String xpathBase = "";
        private Map<String, String> namespaces = new HashMap<String, String>();
        private boolean falsifyPolicyEnabled;  // this is corresponding to the checkbox in XPath Mulitiple Attributes panel.

        public MultipleAttributeConfig() {
        }

        /**
         * @returns a list of the field names that are marked as relative XPaths
         */
        public Set<FieldName> getRelativeXPathFieldNames() {
            Set<FieldName> result = new HashSet<FieldName>();
            for(Field field : fields.values()) {
                if (FieldType.XPATH_RELATIVE == field.getType())
                    result.add(field.getName());
            }
            return result;
        }

        /**
         * @returns a list of the field names that are marked as absolute XPaths
         */
        public Set<FieldName> getAbsoluteXPathFieldNames() {
            Set<FieldName> result = new HashSet<FieldName>();
            for(Field field : fields.values()) {
                if (FieldType.XPATH_ABSOLUTE == field.getType())
                    result.add(field.getName());
            }
            return result;
        }

        public boolean hasV1ResourceId() {
            return XacmlConstants.XACML_10_RESOURCE_ID.equals(fields.get(ID.name()).getValue());
        }

        public Field getField(FieldName name) {
            return fields.get(name.name());
        }

        public Set<Field> getNonValueFields() {
            Set<Field> result = new HashSet<Field>(fields.values());
            result.remove(fields.get(VALUE.name()));
            return result;
        }

        public Set<Field> getAllFields() {
            return new HashSet<Field>(fields.values());
        }

        public Map<String, Field> getFields() {
            return fields;
        }

        @Deprecated // persistence only
        public void setFields(Map<String, Field> fields) {
            this.fields = fields;
        }

        public XacmlAssertionEnums.MessageLocation getMessageSource() {
            return messageSource;
        }

        public void setMessageSource(XacmlAssertionEnums.MessageLocation messageSource) {
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

        public boolean isFalsifyPolicyEnabled() {
            return falsifyPolicyEnabled;
        }

        public void setFalsifyPolicyEnabled(boolean falsifyPolicyEnabled) {
            this.falsifyPolicyEnabled = falsifyPolicyEnabled;
        }

        @Override
        public String toString() {
            return "Multiple Attributes";
        }

        public static class Field implements Cloneable, Serializable {
            private FieldName name;
            private FieldType type = FieldType.REGULAR;
            private String value = "";

            @Deprecated
            public Field() {
            }

            public Field(FieldName name) {
                this.name = name;
            }

            public FieldName getName() {
                return name;
            }

            /**
             * For persistence only. 
             */
            @Deprecated
            public void setName( final FieldName name ) {
                this.name = name;
            }

            public String getValue() {
                return value;
            }

            public void setType(FieldType type) {
                this.type = type;
                setValue(this.value); // make sure ${} is added when the type is changed
            }

            public FieldType getType() {
                return type;
            }

            public void setValue(String value) {
                if ( type == FieldType.CONTEXT_VARIABLE && ! value.isEmpty() &&
                     ! value.startsWith("${") && !value.endsWith("}")) {
                    this.value = "${" + value + "}";
                } else {
                    this.value = value;
                }
            }

            @Override
            public Field clone() {
                try {
                    return (Field) super.clone();
                } catch (CloneNotSupportedException e) {
                    throw ExceptionUtils.wrap( e );
                }
            }
        }

        @Override
        public MultipleAttributeConfig clone() {
            try {
                MultipleAttributeConfig copy = (MultipleAttributeConfig) super.clone();
                if ( fields != null ) {
                    copy.fields = new HashMap<String, Field>();
                    for ( Map.Entry<String,Field> entry : fields.entrySet() ) {
                        copy.fields.put( entry.getKey(), entry.getValue().clone() );
                    }
                }
                if ( namespaces != null ) {
                    copy.namespaces =  new HashMap<String, String>( namespaces );
                }

                return copy;
            } catch ( CloneNotSupportedException e ) {
                throw ExceptionUtils.wrap( e );
            }
        }
    }

    /**
     * <p>
     * The direct children of the <Request> element are: Subject, Resource, Action and Environment.
     * </p>
     * <p>
     * Each of these elements share a common part of their schema - the ability to have an unbounded amount of
     * <Attribute> child elements. All other differences are realised in the concrete implementations of each of
     * these child nodes
     * </p>
     * <p>
     * This class deals with AttributeTreeNodeTag's, as each child of request can have an unbounded amount of
     * Attributes. This marker / tag interface allows the difference to be detected
     * </p>
     */
    public static abstract class RequestChildElement implements Cloneable, Serializable {
        private List<AttributeTreeNodeTag> attributeTreeNodes = new ArrayList<AttributeTreeNodeTag>();

        public RequestChildElement() {
        }

        public RequestChildElement(List<AttributeTreeNodeTag> attributeTreeNodes) {
            this.attributeTreeNodes = attributeTreeNodes;
        }

        public List<AttributeTreeNodeTag> getAttributes() {
            return attributeTreeNodes;
        }

        public void setAttributes(List<AttributeTreeNodeTag> attributeTreeNodes) {
            this.attributeTreeNodes = attributeTreeNodes;
        }

        @Override
        public RequestChildElement clone() {
            try {
                RequestChildElement copy = (RequestChildElement) super.clone();
                if ( attributeTreeNodes != null ) {
                    copy.attributeTreeNodes = new ArrayList<AttributeTreeNodeTag>();
                    for ( AttributeTreeNodeTag attributeTreeNodeTag : attributeTreeNodes ) {
                        copy.attributeTreeNodes.add( attributeTreeNodeTag.clone() );                       
                    }
                }

                return copy;
            } catch ( CloneNotSupportedException e ) {
                throw ExceptionUtils.wrap( e );
            }
        }
    }

    public static class Subject extends RequestChildElement {
        private String subjectCategory = "";

        public Subject() {
            super();
        }

        public Subject(String subjectCategory, List<AttributeTreeNodeTag> attributeTreeNodes) {
            super(attributeTreeNodes);
            this.subjectCategory = subjectCategory;
        }

        public String getSubjectCategory() {
            return subjectCategory;
        }

        public void setSubjectCategory(String subjectCategory) {
            this.subjectCategory = subjectCategory;
        }

        @Override
        public String toString() {
            return "Subject";
        }
    }

    public static class Resource extends RequestChildElement {
        private ResourceContent resourceContent;

        public Resource() {
            super();
        }

        public Resource(ResourceContent resourceContent, List<AttributeTreeNodeTag> attributeTreeNodes) {
            super(attributeTreeNodes);
            this.resourceContent = resourceContent;
        }

        public ResourceContent getResourceContent() {
            return resourceContent;
        }

        public void setResourceContent(ResourceContent resourceContent) {
            this.resourceContent = resourceContent;
        }

        @Override
        public String toString() {
            return "Resource";
        }

        @Override
        public Resource clone() {
            Resource copy = (Resource) super.clone();
            if ( resourceContent != null ) {
                copy.resourceContent = (ResourceContent) resourceContent.clone();
            }

            return copy;
        }
    }

    public static class Action extends RequestChildElement{
        public Action() {
            super();
        }

        public Action(List<AttributeTreeNodeTag> attributeTreeNodes) {
            super(attributeTreeNodes);
        }

        @Override
        public String toString() {
            return "Action";
        }
    }

    public static class Environment extends RequestChildElement{
        public Environment() {
            super();
        }

        public Environment(List<AttributeTreeNodeTag> attributeTreeNodes) {
            super(attributeTreeNodes);
        }

        @Override
        public String toString() {
            return "Environment";
        }
    }

    private XacmlAssertionEnums.XacmlVersionType xacmlVersion = XacmlAssertionEnums.XacmlVersionType.V2_0;
    private XacmlAssertionEnums.SoapVersion soapEncapsulation = XacmlAssertionEnums.SoapVersion.NONE;
    private XacmlAssertionEnums.MessageLocation outputMessageDestination =
            XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST;
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

    @Override
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

    private void addAttributeTypeVariables(List<AttributeTreeNodeTag> attributeTreeNodeTags, List<String[]> variables) {
        for(AttributeTreeNodeTag attributeTreeNodeTag : attributeTreeNodeTags) {
            if(attributeTreeNodeTag instanceof Attribute) {
                Attribute attribute = (Attribute) attributeTreeNodeTag;

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

                for(AttributeValue attributeValue : attribute.getValues()) {
                    for(Map.Entry<String, String> entry : attributeValue.getAttributes().entrySet()) {
                        v = Syntax.getReferencedNames(entry.getKey());
                        if(v.length > 0) {
                            variables.add(v);
                        }

                        v = Syntax.getReferencedNames(entry.getValue());
                        if(v.length > 0) {
                            variables.add(v);
                        }
                    }

                    v = Syntax.getReferencedNames(attributeValue.getContent());
                    if(v.length > 0) {
                        variables.add(v);
                    }
                }
            } else if(attributeTreeNodeTag instanceof MultipleAttributeConfig) {
                MultipleAttributeConfig multiAttr = (MultipleAttributeConfig) attributeTreeNodeTag;

                boolean baseXpathUsed = false;
                for ( MultipleAttributeConfig.Field field : multiAttr.getAllFields()) {
                    String[] vars;
                    if ( field.getType().isXpath() ) {
                        List<String> xpathVars = XpathUtil.getUnprefixedVariablesUsedInXpath(field.getValue());
                        vars = xpathVars.toArray( new String[xpathVars.size()] );
                    } else {
                        vars = Syntax.getReferencedNames(field.getValue());
                    }

                    if ( MultipleAttributeConfig.FieldType.XPATH_RELATIVE == field.getType() ) {
                        baseXpathUsed = true;
                    }

                    if( vars.length > 0 ) {
                        variables.add(vars);
                    }
                }

                if ( baseXpathUsed ) {
                    List<String> xpathVars = XpathUtil.getUnprefixedVariablesUsedInXpath(multiAttr.getXpathBase());
                    String[] vars = xpathVars.toArray( new String[xpathVars.size()] );
                    if( vars.length > 0 ) {
                        variables.add(vars);
                    }
                }
            }
        }
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if(outputMessageDestination == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
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

    public XacmlAssertionEnums.MessageLocation getOutputMessageDestination() {
        return outputMessageDestination;
    }

    public void setOutputMessageDestination(XacmlAssertionEnums.MessageLocation outputMessageDestination) {
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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "XACML Request Builder");
        meta.put(LONG_NAME, "Generate XACML Requests");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });

        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.xacmlpdp.console.XacmlRequestBuilderAssertionValidator" );

        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xacmlpdp.console.XacmlRequestBuilderDialog");

        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(WSP_EXTERNAL_NAME, "XacmlRequestBuilderAssertion");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new Java5EnumTypeMapping(XacmlAssertionEnums.XacmlVersionType.class, "xacmlVersion"));
        othermappings.add(new Java5EnumTypeMapping(XacmlAssertionEnums.MessageLocation.class, "messageTarget"));
        othermappings.add(new Java5EnumTypeMapping(XacmlAssertionEnums.SoapVersion.class, "soapEncapsulation"));
        othermappings.add(new BeanTypeMapping(Subject.class, "subject"));
        othermappings.add(new CollectionTypeMapping(List.class, Subject.class, ArrayList.class, "subjectList"));
        othermappings.add(new BeanTypeMapping(Resource.class, "resource"));
        othermappings.add(new CollectionTypeMapping(List.class, Resource.class, ArrayList.class, "resourceList"));
        othermappings.add(new BeanTypeMapping(Action.class, "action"));
        othermappings.add(new BeanTypeMapping(Environment.class, "environment"));
        othermappings.add(new BeanTypeMapping(Attribute.class, "attribute"));
        othermappings.add(new BeanTypeMapping(AttributeValue.class, "attributeValue"));
        othermappings.add(new BeanTypeMapping(MultipleAttributeConfig.Field.class, "xpathMultipleAttributesField"));
        othermappings.add(new Java5EnumTypeMapping(MultipleAttributeConfig.FieldName.class, "xpathMultipleAttributesFieldName"));
        othermappings.add(new Java5EnumTypeMapping(MultipleAttributeConfig.FieldType.class, "xpathMultipleAttributesFieldType"));
        othermappings.add(new BeanTypeMapping(MultipleAttributeConfig.class, "multipleAttributeConfig"));
        othermappings.add(new CollectionTypeMapping(List.class, AttributeTreeNodeTag.class, ArrayList.class, "attributeList"));
        othermappings.add(new CollectionTypeMapping(List.class, AttributeValue.class, ArrayList.class, "valueList"));
        othermappings.add(new BeanTypeMapping(ResourceContent.class, "resourceContent"));
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:EchoRouting" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(XacmlRequestBuilderAssertion.class.getName() + ".metadataInitialized", Boolean.TRUE);
        return meta;
    }

    @Override
    public XacmlRequestBuilderAssertion clone() {
        XacmlRequestBuilderAssertion copy = (XacmlRequestBuilderAssertion) super.clone();

        if ( action != null ) {
            copy.action = (Action) action.clone();
        }

        if ( environment != null ) {
            copy.environment = (Environment) environment.clone();
        }

        if ( subjects != null ) {
            copy.subjects = new ArrayList<Subject>();
            for ( Subject subject : subjects ) {
                copy.subjects.add( (Subject) subject.clone() );
            }
        }

        if ( resources != null ) {
            copy.resources = new ArrayList<Resource>();
            for ( Resource resource : resources ) {
                copy.resources.add( resource.clone() );               
            }
        }

        return copy;
    }
}
