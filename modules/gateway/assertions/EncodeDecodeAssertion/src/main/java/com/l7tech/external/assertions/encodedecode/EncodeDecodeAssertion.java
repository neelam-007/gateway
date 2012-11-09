package com.l7tech.external.assertions.encodedecode;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Encode / Decode utility assertion.
 */
public class EncodeDecodeAssertion extends Assertion implements SetsVariables, UsesVariables {

    //- PUBLIC

    public EncodeDecodeAssertion(){        
    }

    public TransformType getTransformType() {
        return transformType;
    }

    public void setTransformType( final TransformType transformType ) {
        this.transformType = transformType;
    }

    public String getSourceVariableName() {
        return sourceVariableName;
    }

    public void setSourceVariableName( final String sourceVariableName ) {
        this.sourceVariableName = sourceVariableName;
    }

    public String getTargetVariableName() {
        return targetVariableName;
    }

    public void setTargetVariableName(@NotNull final String targetVariableName ) {
        this.targetVariableName = targetVariableName;
    }

    public DataType getTargetDataType() {
        return targetDataType;
    }

    public void setTargetDataType( final DataType targetDataType ) {
        this.targetDataType = targetDataType;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding( final String characterEncoding ) {
        this.characterEncoding = characterEncoding;
    }

    public String getTargetContentType() {
        return targetContentType;
    }

    public void setTargetContentType( final String targetContentType ) {
        this.targetContentType = targetContentType;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict( final boolean strict ) {
        this.strict = strict;
    }

    public int getLineBreakInterval() {
        return lineBreakInterval;
    }

    public void setLineBreakInterval( final int lineBreakInterval ) {
        this.lineBreakInterval = lineBreakInterval;
    }

    @Override
    public String[] getVariablesUsed() {
        return new String[]{ sourceVariableName };
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if(targetVariableName != null){
            return new VariableMetadata[]{
                    new VariableMetadata( targetVariableName, targetDataType!=DataType.STRING, false, null, true, targetDataType )
            };
        }
        return new VariableMetadata[]{};
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        final String baseName = "Encode / Decode Data";
        meta.put( SHORT_NAME, baseName );
        meta.put( DESCRIPTION, "Encode or decode data." );
        meta.put( PALETTE_FOLDERS, new String[] { "xml" } );
        meta.put( POLICY_ADVICE_CLASSNAME, "auto" );
        meta.put( PROPERTIES_ACTION_NAME, "Encode / Decode Data Properties" );
        meta.put( PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.encodedecode.console.EncodeDecodePropertiesDialog");
        meta.put( POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<EncodeDecodeAssertion>(){
            @Override
            public String getAssertionName( final EncodeDecodeAssertion assertion, final boolean decorate ) {
                if(!decorate) return baseName;
                StringBuilder nameBuilder = new StringBuilder(256);
                nameBuilder.append( assertion.getTransformType()==null ? baseName : assertion.getTransformType().getName() );
                nameBuilder.append( " ${" );
                nameBuilder.append( assertion.getSourceVariableName() );
                nameBuilder.append( "} into ${" );
                nameBuilder.append( assertion.getTargetVariableName() );
                nameBuilder.append( '}' );
                if ( assertion.getTargetDataType() != DataType.STRING ) {
                    nameBuilder.append( " as " );
                    nameBuilder.append( assertion.getTargetDataType().getName() );
                    if ( assertion.getTargetContentType() != null ) {
                        try {
                            String value = ContentTypeHeader.parseValue( assertion.getTargetContentType() ).getMainValue();
                            nameBuilder.append( ' ' );
                            nameBuilder.append( '\'' );
                            nameBuilder.append( value );
                            nameBuilder.append( '\'' );
                        } catch ( IOException e ) {
                            // don't include in name
                        }
                    }
                }
                return AssertionUtils.decorateName(assertion, nameBuilder);
            }
        } );
        meta.put( FEATURE_SET_NAME, "(fromClass)" );
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder( Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping( TransformType.class, "transformType" )
        ) ));
        meta.put( META_INITIALIZED, Boolean.TRUE );
        return meta;
    }

    public enum TransformType {
        BASE64_ENCODE("Base64 Encode", true, false, false, false),
        BASE64_DECODE("Base64 Decode", false, true, true, false),
        HEX_ENCODE("Base16 Encode", true, false, false, false),
        HEX_DECODE("Base16 Decode",  false, true, true, false),
        URL_ENCODE("URL Encode", false, false, false, true),
        URL_DECODE("URL Decode", false, false, false, true);

        private final String name;
        private final boolean binaryInput;
        private final boolean binaryOutput;
        private final boolean strictSupported;
        private final boolean encodingRequired; // flag for encoding required even if not binary

        private TransformType( final String name,
                               final boolean binaryInput,
                               final boolean binaryOutput,
                               final boolean strictSupported,
                               final boolean encodingRequired ) {
            this.name = name;
            this.binaryInput = binaryInput;
            this.binaryOutput = binaryOutput;
            this.strictSupported = strictSupported;
            this.encodingRequired = encodingRequired;
        }

        public String getName() {
            return name;
        }

        public boolean isBinaryInput() {
            return binaryInput;
        }

        public boolean isBinaryOutput() {
            return binaryOutput;
        }

        public boolean isStrictSupported() {
            return strictSupported;
        }

        public boolean encodingRequired() {
            return encodingRequired;
        }
    }

    //- PRIVATE

    private static final String META_INITIALIZED = EncodeDecodeAssertion.class.getName() + ".metadataInitialized";

    private TransformType transformType;
    private String sourceVariableName;
    private String targetVariableName;
    private DataType targetDataType;
    private String targetContentType;

    private String characterEncoding = "UTF-8";
    private boolean strict = false;
    private int lineBreakInterval;
}
