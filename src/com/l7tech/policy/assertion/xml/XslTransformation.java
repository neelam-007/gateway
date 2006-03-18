package com.l7tech.policy.assertion.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
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
public class XslTransformation extends Assertion implements UsesVariables {
    private static final Logger logger = Logger.getLogger(XslTransformation.class.getName());

    public static final int APPLY_TO_REQUEST = 1;
    public static final int APPLY_TO_RESPONSE = 2;

    private int direction;
    private String xslSrc;
    private String transformName;
    private boolean fetchXsltFromMessageUrls;
    private String[] fetchUrlRegexes = new String[0];
    private int whichMimePart = 0;
    private boolean fetchAllowWithoutStylesheet;

    /**
     * the actual transformation xsl
     */
    public String getXslSrc() {
        return xslSrc;
    }

    /**
     * the actual transformation xsl
     */
    public void setXslSrc(String xslSrc) {
        this.xslSrc = xslSrc;
    }

    /**
     * Whether this assertion applies to requests or responses soap messages.
     * Typed as an int for serialization purposes.
     * @return APPLY_TO_REQUEST OR APPLY_TO_RESPONSE
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Whether this assertion applies to requests or responses soap messages.
     * Typed as an int for serialization purposes.
     * @param direction APPLY_TO_REQUEST OR APPLY_TO_RESPONSE
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

    public boolean isFetchXsltFromMessageUrls() {
        return fetchXsltFromMessageUrls;
    }

    public void setFetchXsltFromMessageUrls(boolean fetchXsltFromMessageUrls) {
        this.fetchXsltFromMessageUrls = fetchXsltFromMessageUrls;
    }

    public String[] getFetchUrlRegexes() {
        return fetchUrlRegexes;
    }

    public void setFetchUrlRegexes(String[] fetchUrlRegexes) {
        this.fetchUrlRegexes = fetchUrlRegexes;
    }

    public boolean isFetchAllowWithoutStylesheet() {
        return fetchAllowWithoutStylesheet;
    }

    public void setFetchAllowWithoutStylesheet(boolean stylesheetUrlRequired) {
        this.fetchAllowWithoutStylesheet = stylesheetUrlRequired;
    }

    public String[] getVariablesUsed() {
        if (varsUsed != null) return varsUsed;

        // Try again later, in case the stylesheet hasn't been set yet
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
