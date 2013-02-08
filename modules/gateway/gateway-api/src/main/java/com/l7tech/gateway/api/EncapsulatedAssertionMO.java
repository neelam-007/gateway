package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.*;
import com.l7tech.util.Functions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The EncapsulatedAssertionMO object represents an encapsulated assertion configuration.
 */
@XmlRootElement(name="EncapsulatedAssertion")
@XmlType(name="EncapsulatedAssertionType", propOrder={"nameValue", "guidValue", "policyReference", "encapsulatedArgumentsValues", "encapsulatedResultsValues", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "encapsulatedAssertions")
public class EncapsulatedAssertionMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the encapsulated assertion config
     *
     * @return The name of the encapsulated assertion config (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the encapsulated assertion config.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    public String getGuid() {
        return get(guid);
    }

    public void setGuid(String guid) {
        this.guid = set(this.guid, guid);
    }

    @XmlElement(name = "PolicyReference")
    public ManagedObjectReference getPolicyReference() {
        return policyReference;
    }

    public void setPolicyReference(ManagedObjectReference policyReference) {
        this.policyReference = policyReference;
    }

    public List<EncapsulatedArgument> getEncapsulatedArguments() {
        return get(encapsulatedArguments, new ArrayList<EncapsulatedArgument>() );
    }

    public void setEncapsulatedArguments(List<EncapsulatedArgument> encapsulatedArguments) {
        this.encapsulatedArguments = set(this.encapsulatedArguments, encapsulatedArguments, AttributeExtensibleEncapsulatedArgumentList.Builder);
    }

    public List<EncapsulatedResult> getEncapsulatedResults() {
        return get(encapsulatedResults, new ArrayList<EncapsulatedResult>() );
    }

    public void setEncapsulatedResults(List<EncapsulatedResult> encapsulatedResults) {
        this.encapsulatedResults = set(this.encapsulatedResults, encapsulatedResults, AttributeExtensibleEncapsulatedResultList.Builder);
    }

    /**
     * Get the properties for this encapsulated assertion configuration.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this listen port.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, String> properties ) {
        this.properties = properties;
    }

    @XmlRootElement(name="EncapsulatedArgument")
    @XmlType(name="EncapsulatedAssertionArgumentType",propOrder={"ordinalValue","argumentNameValue","argumentTypeValue","guiLabelValue","guiPromptValue","extension","extensions"})
    public static class EncapsulatedArgument extends ElementExtensionSupport {

        //- PUBLIC

        public int getOrdinal() {
            return get(this.ordinal);
        }

        public void setOrdinal(int ordinal) {
            this.ordinal = set(this.ordinal, ordinal);
        }

        public String getArgumentName() {
            return get(argumentName);
        }

        public void setArgumentName(String argumentName) {
            this.argumentName = set(this.argumentName, argumentName);
        }

        public String getArgumentType() {
            return get(argumentType);
        }

        public void setArgumentType(String argumentType) {
            this.argumentType = set(this.argumentType, argumentType);
        }

        public String getGuiLabel() {
            return get(guiLabel);
        }

        public void setGuiLabel(String guiLabel) {
            this.guiLabel = set(this.guiLabel, guiLabel);
        }

        public boolean isGuiPrompt() {
            return get(guiPrompt);
        }

        public void setGuiPrompt(boolean guiPrompt) {
            this.guiPrompt = set(this.guiPrompt, guiPrompt);
        }

        //- PROTECTED

        @XmlElement(name = "Ordinal")
        protected AttributeExtensibleInteger getOrdinalValue() {
            return ordinal;
        }

        protected void setOrdinalValue(AttributeExtensibleInteger ordinal) {
            this.ordinal = ordinal;
        }

        @XmlElement(name = "ArgumentName")
        protected AttributeExtensibleString getArgumentNameValue() {
            return argumentName;
        }

        protected void setArgumentNameValue(AttributeExtensibleString argumentName) {
            this.argumentName = argumentName;
        }

        @XmlElement(name = "ArgumentType")
        protected AttributeExtensibleString getArgumentTypeValue() {
            return argumentType;
        }

        protected void setArgumentTypeValue(AttributeExtensibleString argumentType) {
            this.argumentType = argumentType;
        }

        @XmlElement(name = "GuiLabel")
        protected AttributeExtensibleString getGuiLabelValue() {
            return guiLabel;
        }

        protected void setGuiLabelValue(AttributeExtensibleString guiLabel) {
            this.guiLabel = guiLabel;
        }

        @XmlElement(name = "GuiPrompt")
        protected AttributeExtensibleBoolean getGuiPromptValue() {
            return guiPrompt;
        }

        protected void setGuiPromptValue(AttributeExtensibleBoolean guiPrompt) {
            this.guiPrompt = guiPrompt;
        }

        //- PRIVATE

        private AttributeExtensibleInteger ordinal;
        private AttributeExtensibleString argumentName;
        private AttributeExtensibleString argumentType;
        private AttributeExtensibleString guiLabel;
        private AttributeExtensibleBoolean guiPrompt;
    }

    @XmlType(name="EncapsulatedArgumentListPropertyType", propOrder={"value"})
    protected static class AttributeExtensibleEncapsulatedArgumentList extends AttributeExtensibleType.AttributeExtensible<List<EncapsulatedArgument>> {
        private List<EncapsulatedArgument> value;

        @Override
        @XmlElement(name="EncapsulatedAssertionArgument")
        public List<EncapsulatedArgument> getValue() {
            return value;
        }

        @Override
        public void setValue( final List<EncapsulatedArgument> value ) {
            this.value = value;
        }

        protected AttributeExtensibleEncapsulatedArgumentList() {
        }

        private static final Functions.Nullary<AttributeExtensibleEncapsulatedArgumentList> Builder =
            new Functions.Nullary<AttributeExtensibleEncapsulatedArgumentList>(){
                @Override
                public AttributeExtensibleEncapsulatedArgumentList call() {
                    return new AttributeExtensibleEncapsulatedArgumentList();
                }
            };
    }

    @XmlRootElement(name="EncapsulatedResult")
    @XmlType(name="EncapsulatedResultType",propOrder={"resultNameValue","resultTypeValue","extension","extensions"})
    public static class EncapsulatedResult extends ElementExtensionSupport {

        //- PUBLIC

        public String getResultName() {
            return get(resultName);
        }

        public void setResultName(String resultName) {
            this.resultName = set(this.resultName, resultName);
        }

        public String getResultType() {
            return get(resultType);
        }

        public void setResultType(String resultType) {
            this.resultType = set(this.resultType, resultType);
        }

        //- PROTECTED

        @XmlElement(name = "ResultName")
        protected AttributeExtensibleString getResultNameValue() {
            return resultName;
        }

        protected void setResultNameValue(AttributeExtensibleString resultName) {
            this.resultName = resultName;
        }

        @XmlElement(name = "ResultType")
        protected AttributeExtensibleString getResultTypeValue() {
            return resultType;
        }

        protected void setResultTypeValue(AttributeExtensibleString resultType) {
            this.resultType = resultType;
        }

        //- PRIVATE

        private AttributeExtensibleString resultName;
        private AttributeExtensibleString resultType;
    }

    @XmlType(name="EncapsulatedResultListPropertyType", propOrder={"value"})
    protected static class AttributeExtensibleEncapsulatedResultList extends AttributeExtensibleType.AttributeExtensible<List<EncapsulatedResult>> {
        private List<EncapsulatedResult> value;

        @Override
        @XmlElement(name="EncapsulatedAssertionResult")
        public List<EncapsulatedResult> getValue() {
            return value;
        }

        @Override
        public void setValue( final List<EncapsulatedResult> value ) {
            this.value = value;
        }

        protected AttributeExtensibleEncapsulatedResultList() {
        }

        private static final Functions.Nullary<AttributeExtensibleEncapsulatedResultList> Builder =
            new Functions.Nullary<AttributeExtensibleEncapsulatedResultList>(){
                @Override
                public AttributeExtensibleEncapsulatedResultList call() {
                    return new AttributeExtensibleEncapsulatedResultList();
                }
            };
    }

    //- PROTECTED

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name = "Guid")
    protected AttributeExtensibleString getGuidValue() {
        return guid;
    }

    protected void setGuidValue(AttributeExtensibleString guid) {
        this.guid = guid;
    }

    @XmlElement(name = "EncapsulatedArguments")
    protected AttributeExtensibleEncapsulatedArgumentList getEncapsulatedArgumentsValues() {
        return encapsulatedArguments;
    }

    protected void setEncapsulatedArgumentsValues(AttributeExtensibleEncapsulatedArgumentList encapsulatedArguments) {
        this.encapsulatedArguments = encapsulatedArguments;
    }

    @XmlElement(name = "EncapsulatedResults")
    protected AttributeExtensibleEncapsulatedResultList getEncapsulatedResultsValues() {
        return encapsulatedResults;
    }

    protected void setEncapsulatedResultsValues(AttributeExtensibleEncapsulatedResultList encapsulatedResults) {
        this.encapsulatedResults = encapsulatedResults;
    }

        //- PACKAGE

    EncapsulatedAssertionMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleString guid;
    private ManagedObjectReference policyReference;
    private AttributeExtensibleEncapsulatedArgumentList encapsulatedArguments;
    private AttributeExtensibleEncapsulatedResultList encapsulatedResults;
    private Map<String,String> properties;
}
