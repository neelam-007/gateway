package com.l7tech.external.assertions.mtom;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.annotation.ProcessesMultipart;
import com.l7tech.policy.wsp.ArrayTypeMapping;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;

import java.util.Arrays;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 *
 */
@ProcessesMultipart
public class MtomValidateAssertion extends MessageTargetableAssertion implements UsesVariables {

    //- PUBLIC

    public MtomValidateAssertion() {
        super(false);
    }

    public boolean isRequireEncoded() {
        return requireEncoded;
    }

    public void setRequireEncoded( boolean requireEncoded ) {
        this.requireEncoded = requireEncoded;
    }

    public ValidationRule[] getValidationRules() {
        return validationRules;
    }

    public void setValidationRules( final ValidationRule[] validationRules ) {
        this.validationRules = validationRules;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Validate MTOM Message");
        meta.put(PROPERTIES_ACTION_NAME, "MTOM Validate Properties");
        meta.put(DESCRIPTION, "Validate a SOAP message in Optimized MIME Multipart/Related Serialization (MTOM) format.");
        meta.put(WSP_EXTERNAL_NAME, "MTOMValidateAssertion");
        meta.put(PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(PROPERTIES_EDITOR_CLASSNAME,
                "com.l7tech.external.assertions.mtom.console.MtomValidateAssertionPropertiesDialog");
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder( Arrays.<TypeMapping>asList(
                new ArrayTypeMapping(new ValidationRule[0], "MTOMValidationRules"),
                new BeanTypeMapping(ValidationRule.class, "MTOMValidationRule") )));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static final class ValidationRule {
        private XpathExpression xpathExpression;
        private int count;
        private long size;

        public XpathExpression getXpathExpression() {
            return xpathExpression;
        }

        public void setXpathExpression( final XpathExpression xpathExpression ) {
            this.xpathExpression = xpathExpression;
        }

        public int getCount() {
            return count;
        }

        public void setCount( final int count ) {
            this.count = count;
        }

        public long getSize() {
            return size;
        }

        public void setSize( final long size ) {
            this.size = size;
        }
    }

    //- PROTECTED

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        final VariablesUsed variablesUsed = super.doGetVariablesUsed();

        if ( validationRules != null ) {
            for ( final ValidationRule validationRule : validationRules ) {
                if ( validationRule == null || validationRule.getXpathExpression() == null ) {
                    continue;
                }

                final String expression = validationRule.getXpathExpression().getExpression();
                if ( expression != null ) {
                    variablesUsed.addVariables( XpathUtil.getUnprefixedVariablesUsedInXpath( expression, validationRule.getXpathExpression().getXpathVersion() ) );
                }
            }
        }

        return variablesUsed;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = MtomValidateAssertion.class.getName() + ".metadataInitialized";

    private boolean requireEncoded = true;
    private ValidationRule[] validationRules;
}
