package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xml.RemoveElement;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author jbufu
 */
public class ServerRemoveElement extends AbstractMessageTargetableServerAssertion<RemoveElement> {

    private final boolean inserting;
    private final RemoveElement.ElementLocation elementLocation;
    private final String existingElementVariableName;
    private final String newElementVariableName;
    private final String[] varsUsed;

    public ServerRemoveElement( final RemoveElement assertion ) {
        super(assertion);
        this.elementLocation = assertion.getInsertedElementLocation();
        this.inserting = elementLocation != null;
        this.existingElementVariableName = assertion.getElementFromVariable();
        this.newElementVariableName = assertion.getElementToInsertVariable();
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        if ( !message.isXml() ) {
            logAndAudit(AssertionMessages.REMOVE_ELEMENT_NOT_XML);
            return AssertionStatus.NOT_APPLICABLE;
        }

        try {
            final Map<String, Object> vars = context.getVariableMap(varsUsed, getAudit());
            final Object existingElementsObj = ExpandVariables.processSingleVariableAsObject("${" + existingElementVariableName + "}", vars, getAudit());
            Element[] existingElements = asElementArrayValue(existingElementsObj, existingElementVariableName, false);

            if (inserting) {
                return doInsert(context, vars, message, existingElements);
            }

            if ( existingElements.length > 0 ) {
                Document document = message.getXmlKnob().getDocumentWritable();
                for( Element e : existingElements ) {
                    if ( document == e.getOwnerDocument() && e.getParentNode() != null ) {
                        e.getParentNode().removeChild(e);
                    }
                }
            }

            return AssertionStatus.NONE;
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, existingElementVariableName);
            return AssertionStatus.FAILED;
        } catch (SAXException e) {
            logAndAudit(AssertionMessages.REMOVE_ELEMENT_NOT_XML);
            return AssertionStatus.NOT_APPLICABLE;
        }
    }

    private Element[] asElementArrayValue(Object contextVariableValue, String varNameForErrorLogging, boolean allowFragmentParsing) {
        Element[] elements = new Element[0];
        if ( contextVariableValue instanceof Element[] ) {
            elements = (Element[]) contextVariableValue;
        } else if ( contextVariableValue instanceof Element ) {
            elements = new Element[]{ (Element) contextVariableValue};
        } else if ( contextVariableValue instanceof Collection) {
            @SuppressWarnings({"unchecked"}) Collection<Element> collection = (Collection<Element>) contextVariableValue;
            if (!collection.isEmpty()) {
                try {
                    elements = collection.toArray(elements);
                } catch (ArrayStoreException e) {
                    logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, varNameForErrorLogging, "Element");
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }
            }
        } else if (allowFragmentParsing && contextVariableValue instanceof CharSequence) {
            CharSequence charSequence = (CharSequence) contextVariableValue;
            try {
                Document doc = XmlUtil.stringToDocument(charSequence.toString());
                elements = new Element[] { doc.getDocumentElement() };
            } catch (SAXException e) {
                logAndAudit(AssertionMessages.INSERT_ELEMENT_BAD_FRAGMENT);
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        }
        return elements;
    }

    private AssertionStatus doInsert(PolicyEnforcementContext context, Map<String, Object> vars, Message message, Element[] existingElements) throws NoSuchVariableException, SAXException, IOException {
        Document targetDoc = message.getXmlKnob().getDocumentWritable();

        if (existingElements.length < 1) {
            logAndAudit(AssertionMessages.INSERT_ELEMENT_EXISTING_NOT_FOUND);
            return AssertionStatus.FAILED;
        } else if (existingElements.length > 1 ) {
            logAndAudit(AssertionMessages.INSERT_ELEMENT_EXISTING_TOO_MANY);
            return AssertionStatus.FAILED;
        }

        Element existing = existingElements[0];
        if (existing == null) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Existing element was null");
            return AssertionStatus.SERVER_ERROR;
        }

        Document containerTargetDoc = existing.getOwnerDocument();
        if (containerTargetDoc == null) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Existing element did not belong to a document");
            return AssertionStatus.SERVER_ERROR;
        }

        if (containerTargetDoc != targetDoc) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Existing element does not belong to the target message document");
            return AssertionStatus.SERVER_ERROR;
        }

        final Object newElementsObj = ExpandVariables.processSingleVariableAsObject("${" + newElementVariableName + "}", vars, getAudit());
        Element[] newElements = asElementArrayValue(newElementsObj, newElementVariableName, true);
        for (Element newElement : newElements) {
            insertElement(existing, newElement,  elementLocation);
        }

        return AssertionStatus.NONE;
    }

    private void insertElement(Element existingElement, Element newElement, RemoveElement.ElementLocation elementLocation) {
        assert existingElement != null;

        if (newElement == null) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "New element was null");
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }

        if (newElement.getOwnerDocument() != existingElement.getOwnerDocument()) {
            newElement = (Element) existingElement.getOwnerDocument().importNode(newElement,  true);
        }

        switch (elementLocation) {
            case FIRST_CHILD:
                Element firstKid = DomUtils.findFirstChildElement(existingElement);
                if (firstKid == null) {
                    existingElement.appendChild(newElement);
                } else {
                    existingElement.insertBefore(newElement,  firstKid);
                }
                break;

            case LAST_CHILD:
                existingElement.appendChild(newElement);
                break;

            case PREVIOUS_SIBLING:
            case NEXT_SIBLING:
                Node parentNode = existingElement.getParentNode();
                if (!(parentNode instanceof Element)) {
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unable to insert as sibling of existing element because existing element has no parent element");
                    throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
                }

                Element parentElement = (Element) parentNode;

                if (RemoveElement.ElementLocation.PREVIOUS_SIBLING == elementLocation) {
                    parentElement.insertBefore(newElement, existingElement);
                } else {
                    Node oldNextSibNode = existingElement.getNextSibling();
                    if (oldNextSibNode == null) {
                        parentElement.appendChild(newElement);
                    } else {
                        parentElement.insertBefore(newElement, oldNextSibNode);
                    }
                }

                break;
            default:
                throw new IllegalStateException("Unknown elementLocation: " + elementLocation);
        }
    }


}
