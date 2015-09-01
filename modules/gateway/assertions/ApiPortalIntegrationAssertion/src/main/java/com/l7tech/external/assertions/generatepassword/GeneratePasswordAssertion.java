package com.l7tech.external.assertions.generatepassword;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * Assertion to generate random characters for password
 *
 * @author rraquepo, 4/9/14
 */
public class GeneratePasswordAssertion extends Assertion implements UsesVariables, SetsVariables {
    public static final String PARAM_UPPERCASE__CHARACTERS_LENGTH = "generate.password.uppercase";
    public static final String PARAM_LOWERCASE_CHARACTERS_LENGTH = "generate.password.lowercase";
    public static final String PARAM_SPECIAL_CHARACTERS = "generate.password.specialChar";
    public static final String PARAM_NUMBERS_CHARACTERS_LENGTH = "generate.password.number";
    public static final String RESPONSE_PASSWORD = "generate.password.response";
    public static final String MAX_LENGTH_PASSWORD_CONFIG = "generate.password.max.length";

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{new VariableMetadata(RESPONSE_PASSWORD)};
    }

    @Override
    public String[] getVariablesUsed() {
        return new String[]{PARAM_UPPERCASE__CHARACTERS_LENGTH, PARAM_LOWERCASE_CHARACTERS_LENGTH, PARAM_SPECIAL_CHARACTERS, PARAM_NUMBERS_CHARACTERS_LENGTH};
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(AssertionMetadata.SHORT_NAME, "Portal Generate Random Password");
        meta.put(AssertionMetadata.DESCRIPTION, "Portal Generate Random Password");
        //meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{""}); - to hide assertion
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"internalAssertions"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        // no properties need to be edited for this assertion
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private static final String META_INITIALIZED = GeneratePasswordAssertion.class.getName() + ".metadataInitialized";
}
