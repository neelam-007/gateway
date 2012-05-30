package com.l7tech.gateway.common.audit;

import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.bind.MarshalException;

/**
 * marshalling of an audit detail's properties to a DOM element.
 * Properties includes:
 *      "param" =  String[]  AuditDetail::params
 */
public class AuditDetailPropertiesDomMarshaller {
    private static final String NS = "http://l7tech.com/audit/detail";

    /**
     * Add the specified AuditRecord as a DOM element as a child of the specified parent element.
     *
     * @param factory  DOM factory to use.  Required.
     * @param detail the audit detail to marshall.  Required.
     * @return the element that was created.
     */
    public Element marshal(Document factory, AuditDetail detail) throws MarshalException {

        Element parent = createElement(factory, "detail");

        String[] params = detail.getParams();
        if (params != null && params.length > 0) {
            Element paramsEl = parent.getOwnerDocument().createElementNS(NS, "params");
            for (String param : params) {
                elm(paramsEl, "param", param);
            }
            parent.appendChild(paramsEl);
        }

        return parent;
    }

    private Element createElement(Document factory, String name) {
        Element e = factory.createElementNS(NS, name);
        e.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns", NS);
        return e;
    }

    private Element elm(Element parent, String name, String contents) {
        Document factory = parent.getOwnerDocument();
        Element e = factory.createElementNS(NS, name);
        Text text = factory.createTextNode(contents==null?"":contents);
        e.appendChild(text);
        parent.appendChild(e);
        return e;
    }


}
