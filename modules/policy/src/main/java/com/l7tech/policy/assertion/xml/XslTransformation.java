package com.l7tech.policy.assertion.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.annotation.HardwareAccelerated;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.policy.variable.Syntax;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.StylesheetRoot;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class XslTransformation extends MessageTargetableAssertion implements UsesVariables, UsesResourceInfo {
    private static final Logger logger = Logger.getLogger(XslTransformation.class.getName());

    @Deprecated
    public static final int APPLY_TO_REQUEST = 1;
    @Deprecated
    public static final int APPLY_TO_RESPONSE = 2;
    @Deprecated
    public static final int APPLY_TO_OTHER = -1;

    private String transformName;
    private AssertionResourceInfo resourceInfo = new StaticResourceInfo();
    private int whichMimePart = 0;

    /**
     * Whether this assertion applies to requests or responses soap messages.
     * Typed as an int for serialization purposes.
     * @deprecated use {@link com.l7tech.policy.assertion.MessageTargetable} properties instead
     * @return {@link #APPLY_TO_REQUEST} or {@link #APPLY_TO_RESPONSE}
     */
    @SuppressWarnings({ "deprecation" })
    @Deprecated
    public int getDirection() {
        if (target == TargetMessageType.REQUEST) return APPLY_TO_REQUEST;
        if (target == TargetMessageType.RESPONSE) return APPLY_TO_RESPONSE;
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
                this.target = TargetMessageType.REQUEST;
                break;
            case APPLY_TO_RESPONSE:
                this.target = TargetMessageType.RESPONSE;
                break;
            case -1:
                this.target = TargetMessageType.OTHER;
                break;
        }
    }

    public String getTransformName() {
        return transformName;
    }

    public void setTransformName(String name) {
        transformName = name;
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

    public String[] getVariablesUsed() {
        if (varsUsed != null) return varsUsed;

        List<String> vars = new ArrayList<String>();
        vars.addAll(Arrays.asList(super.getVariablesUsed()));

        if (resourceInfo instanceof SingleUrlResourceInfo) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo) resourceInfo;
            return Syntax.getReferencedNames(suri.getUrl());
        } else if (!(resourceInfo instanceof StaticResourceInfo)) {
            // Try again later, in case the stylesheet hasn't been set yet
            return new String[0];
        }

        StaticResourceInfo sri = (StaticResourceInfo)resourceInfo;
        String xslSrc = sri.getDocument();
        if (xslSrc == null || xslSrc.length() == 0) return new String[0];

        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setURIResolver( new URIResolver(){
                public Source resolve( String href, String base ) throws TransformerException {
                    return new StreamSource(new StringReader("<a xsl:version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>"));
                }
            } );
            Templates temp = tf.newTemplates(new DOMSource( XmlUtil.parse(new StringReader(xslSrc), false)));
            if (temp instanceof StylesheetRoot) {
                StylesheetRoot stylesheetRoot = (StylesheetRoot)temp;
                Vector<ElemVariable> victor = stylesheetRoot.getVariablesAndParamsComposed();
                for (ElemVariable var : victor) {
                    vars.add(var.getName().getLocalName());
                }
            } else {
                logger.warning("XSLT was not a " + StylesheetRoot.class.getName() + ", can't get declared variables");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't get declared variables from stylesheet", e);
        }

        this.varsUsed = vars.toArray(new String[vars.size()]);

        return varsUsed;
    }

    private transient volatile String[] varsUsed;
}
