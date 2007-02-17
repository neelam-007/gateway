package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Special AssertionMapping for HardcodedResponse to protect its content from getting mangled.
 */
public class HardcodedResponseAssertionMapping extends AssertionMapping {
    public HardcodedResponseAssertionMapping() {
        super(new HardcodedResponseAssertion(), "HardcodedResponse");
    }

    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) {
        super.populateElement(wspWriter, element, object);
    }

    public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (source == null) { throw new IllegalArgumentException("Source cannot be null");}
        NodeList responseBodyNodes = source.getElementsByTagName("L7p:ResponseBody");
        if (responseBodyNodes.getLength() == 0) {
            super.populateObject(object, source, visitor);
        } else {
            //this is an old style HardCodedResponse Assertion
            Element bodyNode = (Element)responseBodyNodes.item(0);
            Node bodyAttr = bodyNode.getAttributeNode("stringValue");
            String responseBody = bodyAttr.getNodeValue();

            HardcodedResponseAssertion hra = (HardcodedResponseAssertion) object.target;
            hra.responseBodyString(responseBody);
            super.populateObject(object, source, new PermissiveWspVisitor(visitor.getTypeMappingFinder()));
        }
    }
}
