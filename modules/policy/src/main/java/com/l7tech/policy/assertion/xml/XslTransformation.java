package com.l7tech.policy.assertion.xml;

import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.HardwareAccelerated;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.xslt.XsltUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * An assertion for XSLT transformations of soap messages.
 *
 * XSL transformation can be applied either to request messages or response messages.
 * The actual xsl containing the transformation is held by the assertion and the source of
 * the transformation is the soap message. The output of the transformation replaces the source
 * message once the transformation is complete.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 10, 2004<br/>
 * $Id$<br/>
 *
 */
@RequiresXML
@HardwareAccelerated( type=HardwareAccelerated.Type.XSLT )
public class XslTransformation extends MessageTargetableAssertion implements UsesVariables, UsesResourceInfo, SetsVariables {
    public static final String DEFAULT_PREFIX = "xslt";
    public static final String VARIABLE_NAME = "messages";
    private static final Logger logger = Logger.getLogger(XslTransformation.class.getName());

    @Deprecated
    public static final int APPLY_TO_REQUEST = 1;
    @Deprecated
    public static final int APPLY_TO_RESPONSE = 2;
    @Deprecated
    public static final int APPLY_TO_OTHER = -1;

    private String transformName;
    private String msgVarPrefix;
    private AssertionResourceInfo resourceInfo = new StaticResourceInfo();
    private int whichMimePart = 0;
    private String xsltVersion;

    public XslTransformation() {
        super(true);
    }

    /**
     * Whether this assertion applies to requests or responses soap messages.
     * Typed as an int for serialization purposes.
     * @deprecated use {@link com.l7tech.policy.assertion.MessageTargetable} properties instead
     * @return {@link #APPLY_TO_REQUEST} or {@link #APPLY_TO_RESPONSE}
     */
    @SuppressWarnings({ "deprecation" })
    @Deprecated
    public int getDirection() {
        if (getTarget() == TargetMessageType.REQUEST) return APPLY_TO_REQUEST;
        if (getTarget() == TargetMessageType.RESPONSE) return APPLY_TO_RESPONSE;
        return -1;
    }

    /**
     * Whether this assertion applies to requests or responses soap messages.
     * Typed as an int for serialization purposes.
     * @deprecated use {@link com.l7tech.policy.assertion.MessageTargetable} properties instead
     * @param direction {@link #APPLY_TO_REQUEST} or {@link #APPLY_TO_RESPONSE}
     */
    @SuppressWarnings({ "deprecation" })
    @Deprecated
    public void setDirection(int direction) {
        switch(direction) {
            case APPLY_TO_REQUEST:
                this.setTarget( TargetMessageType.REQUEST );
                break;
            case APPLY_TO_RESPONSE:
                this.setTarget( TargetMessageType.RESPONSE );
                break;
            case -1:
                this.setTarget( TargetMessageType.OTHER );
                break;
        }
    }

    public String getTransformName() {
        return transformName;
    }

    public void setTransformName(String name) {
        transformName = name;
    }

    public String getMsgVarPrefix() {
        return (msgVarPrefix == null || msgVarPrefix.trim().isEmpty())? DEFAULT_PREFIX : msgVarPrefix;
    }

    public void setMsgVarPrefix(String msgVarPrefix) {
        this.msgVarPrefix = (msgVarPrefix == null || msgVarPrefix.trim().isEmpty())? DEFAULT_PREFIX : msgVarPrefix;
    }

    /**
     * @return the zero-based index of the MIME part to which the transformation should be applied
     */
    public int getWhichMimePart() {
        return whichMimePart;
    }

    /**
     * @param whichMimePart the zero-based index of the MIME part to which the transformation should be applied
     */
    public void setWhichMimePart(int whichMimePart) {
        this.whichMimePart = whichMimePart;
    }

    public AssertionResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public void setResourceInfo(AssertionResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
        this.varsUsed = null;
    }

    public String getXsltVersion() {
        return xsltVersion;
    }

    /**
     * Specify an XSL language version that the stylesheet should be assumed to be written in.
     *
     * @param xsltVersion the language version, or null to assume XSLT 1.0.
     */
    public void setXsltVersion(String xsltVersion) {
        this.xsltVersion = xsltVersion;
    }

    @Override
    public VariablesUsed doGetVariablesUsed() {
        if (varsUsed != null) return super.doGetVariablesUsed().withVariables( varsUsed );

        List<String> vars = new ArrayList<String>();
        if (resourceInfo instanceof SingleUrlResourceInfo) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo) resourceInfo;
            vars.addAll( Arrays.asList( Syntax.getReferencedNames(suri.getUrl()) ) );
        } else if ( resourceInfo instanceof StaticResourceInfo ) {
            StaticResourceInfo sri = (StaticResourceInfo)resourceInfo;
            String xslSrc = sri.getDocument();
            if (xslSrc == null || xslSrc.length() == 0) return super.doGetVariablesUsed();

            try {
                final List<String> xsltVars = XsltUtil.getVariablesUsedByStylesheet(xslSrc, xsltVersion);
                vars.addAll(xsltVars);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't get declared variables from stylesheet: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        this.varsUsed = vars.toArray(new String[vars.size()]);

        return super.doGetVariablesUsed().withVariables( varsUsed );
    }

    private final static String baseName = "Apply XSL Transformation";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<XslTransformation>(){
        @Override
        public String getAssertionName( final XslTransformation assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuilder nodeName = new StringBuilder(baseName);

            final String tname = assertion.getTransformName();
            if (tname != null && tname.length() > 0 && tname.trim().length() > 0) {
                nodeName.append(" - ").append(tname);
            }
            return AssertionUtils.decorateName(assertion, nodeName);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String []{"xml"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "XSL Transformation of a SOAP message.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        
        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.XslTransformationPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "XSL Transformation Properties");

        return meta;

    }

    @Override
    public VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
            new VariableMetadata(getMsgVarPrefix() + "." + VARIABLE_NAME, false, false, null, false, DataType.STRING),
            new VariableMetadata(getMsgVarPrefix() + "." + VARIABLE_NAME + ".first", false, false, null, false, DataType.STRING),
            new VariableMetadata(getMsgVarPrefix() + "." + VARIABLE_NAME + ".last", false, false, null, false, DataType.STRING)
        );
    }

    private transient volatile String[] varsUsed;
}
