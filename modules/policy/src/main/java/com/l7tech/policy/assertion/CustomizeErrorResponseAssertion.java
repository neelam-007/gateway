package com.l7tech.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.validator.AssertionValidatorSupport;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.ExceptionUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_CLASSNAME;

/**
 * Policy assertion for customization of error response.
 */
public class CustomizeErrorResponseAssertion extends Assertion implements UsesVariables {

    //- PUBLIC

    public enum ErrorLevel { DROP_CONNECTION, TEMPLATE_RESPONSE }

    public ErrorLevel getErrorLevel() {
        return errorLevel;
    }

    public void setErrorLevel( final ErrorLevel errorLevel ) {
        this.errorLevel = errorLevel;
    }

    /**
     * Get the HTTP status to use.
     *
     * <p>The status is not required when dropping a connection. The value may contain variables.</p>
     *
     * @return The HTTP status or null.
     */
    public String getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus( final String httpStatus ) {
        this.httpStatus = httpStatus;
    }

    /**
     * Get the content type to use.
     *
     * <p>The content type is not required when dropping a connection. The value may contain variables.</p>
     *
     * @return The HTTP status or null.
     */
    public String getContentType() {
        return contentType;
    }

    public void setContentType( final String contentType ) {
        this.contentType = contentType;
    }

    /**
     * Get the content to use.
     *
     * <p>The content is not required when dropping a connection. The value may contain variables.</p>
     *
     * @return The HTTP status or null.
     */
    public String getContent() {
        return content;
    }

    public void setContent( final String content ) {
        this.content = content;
    }

    public boolean isIncludePolicyDownloadURL() {
        return includePolicyDownloadURL;
    }

    public void setIncludePolicyDownloadURL( final boolean includePolicyDownloadURL ) {
        this.includePolicyDownloadURL = includePolicyDownloadURL;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        final Set<String> varNames = new HashSet<String>();
        collectVars( varNames, httpStatus );
        collectVars( varNames, contentType );
        collectVars( varNames, content );
        return varNames.toArray(new String[varNames.size()]);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "Customize Error Response");
        meta.put(DESCRIPTION, "Configure the response message to be used in case of policy failure.");
        meta.put(PALETTE_FOLDERS, new String[]{"audit"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/disconnect.gif");
        meta.put(PROPERTIES_ACTION_NAME, "Error Response Properties");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.CustomizeErrorResponsePropertiesDialog");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.assertion.CustomizeErrorResponseAssertion$CustomizeErrorResponseAssertionValidator");
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(ErrorLevel.class, "errorLevel")
        )));

        return meta;
    }

    public static final class CustomizeErrorResponseAssertionValidator extends AssertionValidatorSupport<CustomizeErrorResponseAssertion> {
        public CustomizeErrorResponseAssertionValidator( final CustomizeErrorResponseAssertion assertion ) {
            super( assertion );
            try {
                if ( assertion.isEnabled() &&
                     assertion.getErrorLevel()==CustomizeErrorResponseAssertion.ErrorLevel.TEMPLATE_RESPONSE &&
                     isXmlContentType( assertion.getContentType() ) &&
                     shouldBeValidXml( assertion.getContent() ) ) {
                    try {
                        validateXml( assertion.getContent() );
                    } catch ( SAXException se ) {
                        this.addWarningMessage( "Response body XML is not well-formed: " + ExceptionUtils.getMessage( se ));
                    }
                }
            } catch ( VariableNameSyntaxException e ) {
                //
            }
        }

        private boolean isXmlContentType( final String contentType ) {
            boolean isXml = false;
            if ( contentType != null && Syntax.getReferencedNames( contentType ).length == 0 ) {
                try {
                    isXml = ContentTypeHeader.parseValue( contentType ).isXml();
                } catch ( IOException e ) {
                    // treat invalid as non-xml
                }
            }
            return isXml;
        }

        private boolean shouldBeValidXml( final String content ) {
            return content != null && Syntax.getReferencedNames( content ).length == 0;
        }

        private void validateXml( final String content ) throws SAXException {
            XmlUtil.parse( content );
        }
    }

    //- PRIVATE

    private ErrorLevel errorLevel = ErrorLevel.TEMPLATE_RESPONSE;
    private String httpStatus = "500";
    private String contentType = "text/plain; charset=UTF-8";
    private String content = "Internal Server Error";
    private boolean includePolicyDownloadURL = false;

    private void collectVars( final Set<String> varNames,
                              final String text ) {
        if ( text != null && !text.isEmpty() ) {
            final String[] referencedNames = Syntax.getReferencedNames( text );
            varNames.addAll( Arrays.asList(referencedNames) );
        }
    }
}
