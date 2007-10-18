package com.l7tech.external.assertions.script;

import com.l7tech.common.util.EnumTranslator;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspEnumTypeMapping;

import java.util.Arrays;
import java.util.logging.Logger;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
 * An assertion that holds some script that is invoked on every request. 
 */
public class ScriptAssertion extends Assertion {
    protected static final Logger logger = Logger.getLogger(ScriptAssertion.class.getName());

    public enum Language {
        JAVASCRIPT("javascript", "ECMAScript",
                   "var request = policyContext.getRequest();\n" +
                   "var response = policyContext.getResponse();\n" +
                   "\n" +
                   "if (!request.isXml()) {\n" +
                   "    false;\n" +
                   "} else {\n" +
                   "    response.initialize(Packages.com.l7tech.common.util.XmlUtil.stringToDocument(\"<someXml/>\"));\n" +
                   "    true;\n" +
                   "}"),
        RUBY("ruby", "JRuby",
             "example script goes here");

        protected final String bsfLanguageName;
        protected final String guiLabel;
        protected final String sampleScript;

        Language(String languageId, String label, String sampleScript) {
            this.bsfLanguageName = languageId;
            this.guiLabel = label;
            this.sampleScript = sampleScript;
        }

        public String getGuiLabel() {
            return guiLabel;
        }

        public String getBsfLanguageName() {
            return bsfLanguageName;
        }

        public String getSampleScript() {
            return sampleScript;
        }

        public static EnumTranslator getEnumTranslator() {
            return new EnumTranslator() {
                public Object stringToObject(String s) throws IllegalArgumentException {
                    return Language.valueOf(s);
                }

                public String objectToString(Object o) throws ClassCastException {
                    return o.toString();
                }
            };
        }
    }

    private Language language;
    private String scriptBase64;

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String decodeScript() {
        try {
            return scriptBase64 == null ? null : (scriptBase64.length() < 1 ? "" : new String(HexUtils.decodeBase64(scriptBase64), "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void encodeScript(String script) {
        try {
            this.scriptBase64 = script == null ? null : HexUtils.encodeBase64(script.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getScriptBase64() {
        return scriptBase64;
    }

    public void setScriptBase64(String scriptBase64) {
        this.scriptBase64 = scriptBase64;
    }


    //
    // Metadata
    //
    private static final String META_INITIALIZED = ScriptAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;        

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Custom Script");
        meta.put(AssertionMetadata.LONG_NAME, "Custom Script Assertion");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new WspEnumTypeMapping(Language.class, "language")
        )));

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Script" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
