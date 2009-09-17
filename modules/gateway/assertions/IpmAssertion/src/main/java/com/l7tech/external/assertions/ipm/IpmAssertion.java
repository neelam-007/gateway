package com.l7tech.external.assertions.ipm;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Policy assertion bean for an assertion that performs expansion of messages per an IPM template.
 */
public class IpmAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(IpmAssertion.class.getName());

    private String templateb64 = null;
    private String sourceVariableName = "requestXpath.result";
    private String targetVariableName = "ipmResult";
    private boolean useResponse = false;

    private transient String template;

    /**
     * Set the template string.
     *
     * @param template the template string.  May be null.
     */
    public void template(String template) {
        try {
            this.template = template;
            templateb64 = template == null ? null : HexUtils.encodeBase64(template.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Get the current template value.
     * <p/>
     * Does not use conventional getter name to hide it from serialization.
     *
     * @return the template string.  May be null.
     */
    public String template() {
        try {
            if (template != null)
                return template;
            if (templateb64 == null || templateb64.length() < 1) {
                return null;
            } else {
                template = new String(HexUtils.decodeBase64(templateb64), "UTF-8");
                return template;
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "bad template base 64: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    /**
     * @return base64-encoded template string
     * @deprecated only for serialization
     */
    public String getTemplateb64() {
        return templateb64;
    }

    /**
     * @param templateb64 the new base64-encoded template string
     * @deprecated only for serialization
     */
    public void setTemplateb64(String templateb64) {
        this.templateb64 = templateb64;
        this.template = null;
    }

    public String getSourceVariableName() {
        return sourceVariableName;
    }

    /** @param sourceVariableName name of variable from which to read DATA_BUFF contents.  Required. */
    public void setSourceVariableName(String sourceVariableName) {
        if (sourceVariableName == null) throw new NullPointerException();
        this.sourceVariableName = sourceVariableName;
    }

    /** @return name of variable in which to store output, or null to store in request or response.  Required. */
    public String getTargetVariableName() {
        return targetVariableName;
    }

    /** @param targetVariableName name of variable in which to store output, or null to store in request or response.  Required. */
    public void setTargetVariableName(String targetVariableName) {
        this.targetVariableName = targetVariableName;
    }

    public boolean isUseResponse() {
        return useResponse;
    }

    public void setUseResponse(boolean useResponse) {
        this.useResponse = useResponse;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();
        ret.add(sourceVariableName);
        return ret.toArray(new String[ret.size()]);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return targetVariableName == null ? new VariableMetadata[0] : new VariableMetadata[] {
                new VariableMetadata(targetVariableName, false, false, targetVariableName, true, DataType.STRING),
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = IpmAssertion.class.getName() + ".metadataInitialized";

    public static final String PARAM_IPM_OUTPUTBUFFER = "ipmOutputBuffer";
    public static final String PARAM_IPM_MAXBUFFERS = "ipmMaxBuffers";
    public static final String PARAM_IPM_SHAREBYTEBUFFERS = "ipmSharedByteBuffers";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put("ipm.outputBuffer", new String[] {
                "Maximum number of characters that can be output by the IPM To XML assertion in a single conversion.",
                "131071"
        });
        props.put("ipm.maxBuffers", new String[] {
                "Maximum number of character output buffers that can exist at any one time.  This limits both memory usage and the number of conversions that can run concurrently.",
                "120"
        });
        props.put("ipm.sharedByteBuffers", new String[] {
                "If set to \"true\", IPM expansion directly into a message will use byte buffers that are shared with the rest of the Gateway.  Otherwise, IPM expansion will use its own private buffers.  Enabling this option can reduce memory usage.",
                "false"
        });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);


        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "IPM To XML");
        meta.put(AssertionMetadata.DESCRIPTION, "Expands an IPM data buffer into XML using a configured template.  " +
                                              "The IPM data is read from a variable, and the output is saved in " +
                                              "a different variable.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/ipm/console/resources/unpack16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/ipm/console/resources/unpack16.gif");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
