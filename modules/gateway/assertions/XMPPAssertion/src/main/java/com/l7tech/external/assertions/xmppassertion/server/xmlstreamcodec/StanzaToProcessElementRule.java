package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.xml.sax.Attributes;

import java.io.Serializable;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 08/03/12
 * Time: 2:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class StanzaToProcessElementRule implements StanzaToProcessRule, Serializable {
    private String localName;
    private String namespace;
    private StanzaRuleApplicability applicability;

    public StanzaToProcessElementRule() {
    }

    public StanzaToProcessElementRule(String localName,
                                      String namespace,
                                      StanzaRuleApplicability applicability)
    {
        this.localName = localName;
        this.namespace = namespace;
        this.applicability = applicability;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
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

        if(this.localName.equals(localName)) {
            if(this.namespace == null) {
                return namespace == null;
            } else {
                return this.namespace.equals(namespace);
            }
        } else {
            return false;
        }
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

        if(this.localName.equals(localName)) {
            if(this.namespace == null) {
                return namespace == null;
            } else {
                return this.namespace.equals(namespace);
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Element \"");
        sb.append(localName);
        sb.append("\" (");

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
