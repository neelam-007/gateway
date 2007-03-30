package com.l7tech.policy.assertion.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.StylesheetRoot;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
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
@RequiresXML()
public class XslTransformation extends Assertion implements UsesVariables, UsesResourceInfo {
    private static final Logger logger = Logger.getLogger(XslTransformation.class.getName());

    public static final int APPLY_TO_REQUEST = 1;
    public static final int APPLY_TO_RESPONSE = 2;

    private int direction;
    private String transformName;
    private AssertionResourceInfo resourceInfo = new StaticResourceInfo();
    private int whichMimePart = 0;

    /**
     * Whether this assertion applies to requests or responses soap messages.
     * Typed as an int for serialization purposes.
     * @return {@link #APPLY_TO_REQUEST} or {@link #APPLY_TO_RESPONSE}
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Whether this assertion applies to requests or responses soap messages.
     * Typed as an int for serialization purposes.
     * @param direction {@link #APPLY_TO_REQUEST} or {@link #APPLY_TO_RESPONSE}
     */
    public void setDirection(int direction) {
        this.direction = direction;
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

        if (resourceInfo instanceof SingleUrlResourceInfo) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo) resourceInfo;
            return ExpandVariables.getReferencedNames(suri.getUrl());
        } else if (!(resourceInfo instanceof StaticResourceInfo)) {
            // Try again later, in case the stylesheet hasn't been set yet
            return new String[0];
        }

        StaticResourceInfo sri = (StaticResourceInfo)resourceInfo;
        String xslSrc = sri.getDocument();
        if (xslSrc == null || xslSrc.length() == 0) return new String[0];

        try {
            Templates temp = TransformerFactory.newInstance().newTemplates(new DOMSource(XmlUtil.parse(new StringReader(xslSrc), false)));
            if (temp instanceof StylesheetRoot) {
                ArrayList vars = new ArrayList();
                StylesheetRoot stylesheetRoot = (StylesheetRoot)temp;
                Vector victor = stylesheetRoot.getVariablesAndParamsComposed();
                for (Iterator i = victor.iterator(); i.hasNext();) {
                    ElemVariable var = (ElemVariable)i.next();
                    vars.add(var.getName().getLocalName());
                }
                varsUsed = (String[])vars.toArray(new String[0]);
            } else {
                logger.warning("XSLT was not a " + StylesheetRoot.class.getName() + ", can't get declared variables");
                varsUsed = new String[0];
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't get declared variables from stylesheet", e);
            varsUsed = new String[0];
        }

        return varsUsed;
    }

    private transient String[] varsUsed;
}
