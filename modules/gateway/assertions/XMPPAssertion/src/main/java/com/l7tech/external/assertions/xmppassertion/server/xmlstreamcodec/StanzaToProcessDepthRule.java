package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.xml.sax.Attributes;

import java.io.Serializable;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 08/03/12
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class StanzaToProcessDepthRule implements StanzaToProcessRule, Serializable {
    private DepthComparisonOperator depthComparisonOperator = DepthComparisonOperator.EQUAL;
    private int depthValue = 1;
    private StanzaRuleApplicability applicability = StanzaRuleApplicability.END;

    public StanzaToProcessDepthRule() {
    }

    public StanzaToProcessDepthRule(DepthComparisonOperator depthComparisonOperator,
                                    int depthValue,
                                    StanzaRuleApplicability applicability)
    {
        this.depthComparisonOperator = depthComparisonOperator;
        this.depthValue = depthValue;
        this.applicability = applicability;
    }

    public DepthComparisonOperator getDepthComparisonOperator() {
        return depthComparisonOperator;
    }

    public void setDepthComparisonOperator(DepthComparisonOperator depthComparisonOperator) {
        this.depthComparisonOperator = depthComparisonOperator;
    }

    public int getDepthValue() {
        return depthValue;
    }

    public void setDepthValue(int depthValue) {
        this.depthValue = depthValue;
    }

    public StanzaRuleApplicability getApplicability() {
        return applicability;
    }

    public void setApplicability(StanzaRuleApplicability applicability) {
        this.applicability = applicability;
    }

    @Override
    public boolean processStartElement(Stack<String> elements,
                                       String uri,
                                       String localName,
                                       String qName,
                                       Attributes atts)
    {
        if(applicability != StanzaRuleApplicability.START) {
            return false;
        }

        return depthComparisonOperator.evaluate(elements.size() + 1, depthValue);
    }

    @Override
    public boolean processEndElement(Stack<String> elements,
                                     String uri,
                                     String localName,
                                     String qName)
    {
        if(applicability != StanzaRuleApplicability.END) {
            return false;
        }

        return depthComparisonOperator.evaluate(elements.size(), depthValue);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Element Depth ");
        sb.append(depthComparisonOperator.getDisplayValue());
        sb.append(" ");
        sb.append(depthValue);
        sb.append(" (");

        switch(applicability) {
            case START:
                sb.append("Open");
                break;
            case END:
                sb.append("Close");
                break;
            case BOTH:
                sb.append("Open and Close");
                break;
        }

        sb.append(")");

        return sb.toString();
    }
}
