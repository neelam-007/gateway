package com.l7tech.policy.assertion.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
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
public class XslTransformation extends Assertion implements UsesVariables, UsesResourceInfo {
    private static final Logger logger = Logger.getLogger(XslTransformation.class.getName());

    public static final int APPLY_TO_REQUEST = 1;
    public static final int APPLY_TO_RESPONSE = 2;

    private int direction;
    private String transformName;
    private AssertionResourceInfo resourceInfo = new StaticResourceInfo();
    private int whichMimePart = 0;

    /**
     * the actual transformation xsl
     * @deprecated use {@link #resourceInfo } directly instead
     */
    public String getXslSrc() {
        if (resourceInfo != null && resourceInfo.getType() == AssertionResourceType.STATIC)
            return ((StaticResourceInfo) resourceInfo).getDocument();

        return null;
    }

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

    /**
     * @deprecated use {@link # resourceInfo } directly instead
     */
    public boolean isFetchXsltFromMessageUrls() {
        return resourceInfo != null && resourceInfo.getType() == AssertionResourceType.MESSAGE_URL;
    }

    /**
     * @deprecated use {@link # resourceInfo } directly instead
     */
    public String[] getFetchUrlRegexes() {
        if (resourceInfo != null && resourceInfo.getType() == AssertionResourceType.MESSAGE_URL) {
            return ((MessageUrlResourceInfo) resourceInfo).getUrlRegexes();
        } else {
            return new String[0];
        }
    }

    /**
     * @deprecated use {@link # resourceInfo } directly instead
     */
    public boolean isFetchAllowWithoutStylesheet() {
        return resourceInfo != null && resourceInfo.getType() == AssertionResourceType.MESSAGE_URL && ((MessageUrlResourceInfo) resourceInfo).isAllowMessagesWithoutUrl();
    }

    public AssertionResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public void setResourceInfo(AssertionResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public String[] getVariablesUsed() {
        if (varsUsed != null) return varsUsed;

        // Try again later, in case the stylesheet hasn't been set yet
        String xslSrc = getXslSrc();
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XslTransformation that = (XslTransformation) o;

        if (direction != that.direction) return false;
        if (whichMimePart != that.whichMimePart) return false;
        if (resourceInfo != null ? !resourceInfo.equals(that.resourceInfo) : that.resourceInfo != null) return false;
        if (transformName != null ? !transformName.equals(that.transformName) : that.transformName != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = direction;
        result = 31 * result + (transformName != null ? transformName.hashCode() : 0);
        result = 31 * result + (resourceInfo != null ? resourceInfo.hashCode() : 0);
        result = 31 * result + whichMimePart;
        return result;
    }

    private transient String[] varsUsed;
}
