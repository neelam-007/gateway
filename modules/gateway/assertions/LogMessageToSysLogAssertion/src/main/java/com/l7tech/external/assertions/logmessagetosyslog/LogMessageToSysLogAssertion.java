package com.l7tech.external.assertions.logmessagetosyslog;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.BuildInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 *
 */
public class LogMessageToSysLogAssertion extends Assertion implements UsesVariables {

    public static final String SYSLOG_LOG_SINK_PREFIX = "syslogwrite";

    public static final String[] sysLogSeverityStrings = new String[]{"EMERGENCY", "ALERT", "CRITICAL", "ERROR", "WARNING", "NOTICE", "INFORMATIONAL", "DEBUG"};

    public static final String CEFHeaderFixed = "CEF:0|Layer7 Technologies Inc.|SecureSpan Gateway|" + BuildInfo.getFormalProductVersion() + "|";

    private static final String META_INITIALIZED = LogMessageToSysLogAssertion.class.getName() + ".metadataInitialized";

    private static Pattern pPipe = Pattern.compile("[|]");

    private String messageText;
    private String sysLogSeverity;
    private Goid syslogGoid;

    private String cefSignatureId;
    private String cefSignatureName;
    private StringBuilder sbCefSyslogMessage;
    private int cefSeverity;
    private boolean isCEFEnabled;
    private Map<String, String> cefExtensionKeyValuePairs;

    public LogMessageToSysLogAssertion() {
        messageText = "";
        syslogGoid = null;
        sysLogSeverity = sysLogSeverityStrings[6];

        cefSignatureId = "";
        cefSignatureName = "";
        cefSeverity = 0;
        cefExtensionKeyValuePairs = new TreeMap<String, String>();
        sbCefSyslogMessage = new StringBuilder();
    }

    private String excapePipe(String valueWithPipes) {
        return valueWithPipes != null ? pPipe.matcher(valueWithPipes).replaceAll("\\\\|") : valueWithPipes;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Goid getSyslogGoid() {
        return syslogGoid;
    }

    public void setSyslogGoid(Goid syslogGoid) {
        this.syslogGoid = syslogGoid;
    }

    public void setSysLogSeverity(String sysLogSeverityName) {
        this.sysLogSeverity = sysLogSeverityName;
    }

    public String getSysLogSeverity() {
        return sysLogSeverity;
    }

    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(getMessageToBeLogged()+getVariablesAtCEFExtensionText());
    }

    public boolean isCEFEnabled() {
        return isCEFEnabled;
    }

    public void setCEFEnabled(boolean CEFEnabled) {
        isCEFEnabled = CEFEnabled;
    }

    public String getCefSignatureId() {
        return cefSignatureId;
    }

    public void setCefSignatureId(String cefSignatureId) {
        this.cefSignatureId = excapePipe(cefSignatureId);
    }

    public String getCefSignatureName() {
        return cefSignatureName;
    }

    public void setCefSignatureName(String cefSignatureName) {
        this.cefSignatureName = excapePipe(cefSignatureName);
    }

    public int getCefSeverity() {
        return cefSeverity;
    }

    public void setCefSeverity(int cefSeverity) {
        this.cefSeverity = cefSeverity;
    }

    public Map<String, String> getCefExtensionKeyValuePairs() {
        return cefExtensionKeyValuePairs;
    }

    public void setCefExtensionKeyValuePairs(Map<String, String> cefExtensionKeyValuePairs) {
        this.cefExtensionKeyValuePairs.clear();
        this.cefExtensionKeyValuePairs.putAll(cefExtensionKeyValuePairs);
    }

    /**
     * Gets all the values of the key==value pairs to return all the used variables
     * @return
     */
    public String getVariablesAtCEFExtensionText() {
        StringBuilder sbCefExtensionText = new StringBuilder();
        for( String value : cefExtensionKeyValuePairs.values()  )
        {
            sbCefExtensionText.append(value);
        }
        return sbCefExtensionText.toString();
    }

    /**
     * The message text. BUT it does not include the CEF extension if CEF format is used. That text may contain
     * variables that need to be extracted. And all included '=' signs need to be escaped with a \. So that is done
     * on the fly when the assertion gets called
     * @return
     */
    public String getMessageToBeLogged()
    {
        if(isCEFEnabled())
        {
            sbCefSyslogMessage = new StringBuilder();
            sbCefSyslogMessage.append(CEFHeaderFixed);
            sbCefSyslogMessage.append(cefSignatureId).append("|");
            sbCefSyslogMessage.append(cefSignatureName).append("|");
            sbCefSyslogMessage.append(cefSeverity).append("|");
            return sbCefSyslogMessage.toString();
        }
        return messageText;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
//        props.put("logMessageToSysLog.tag", new String[] {
//                "Used as prefix for syslog messages when not using CEF format",
//                "SSG"
//        });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Log Message to Syslog Assertion");
        meta.put(AssertionMetadata.LONG_NAME, "Creates a syslog message at the position in the policy");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"audit"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, 999);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.logmessagetosyslog.console.LogMessageToSysLogAssertionDialog");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Log Message to Syslog Assertion Properties");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:LogMessageToSysLog" rather than "set:modularAssertions"
        // 19 Sep 2012: Commented out so it will work with all licenses.
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        // use (fromClass) here and <featureset name="assertion:LogMessageToSysLog"/> in the license to make the assertion
        // available with other licenses that set:Gateway
//        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME,
                "com.l7tech.external.assertions.logmessagetosyslog.server.LogMessageToSysLogModuleLoadListener");
        return meta;
    }
}