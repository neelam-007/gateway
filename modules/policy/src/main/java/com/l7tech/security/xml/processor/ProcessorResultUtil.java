/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml.processor;

import com.l7tech.security.token.ParsedElement;
import com.l7tech.util.DomUtils;
import com.l7tech.xml.xpath.DomCompiledXpath;
import com.l7tech.xml.xpath.XpathVariableFinder;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for helping assertions interpret the output of WssProcessor.  Helps receivers verify that
 * the appropriate elements were signed and/or encrypted.
 */
public class ProcessorResultUtil {
    public static final int NO_ERROR = 0;
    public static final int FALSIFIED = 1;
    public static final int SERVER_ERROR = 2;

    /**
     * Search for elements in doc that match xpath, and ensure that all matching elements either exist
     * inside elementsFoundByProcessor (using strict matching with ==) or (if allowIfEmpty) have no child nodes.
     * Logging is done to the provided logger, using pastTenseOperationName for human-readable clarity.
     *
     * @param logger     Logger to use for logging activity.  May not be null.
     * @param doc        The document to inspect, such as an undecorated soap message.  May not be null.
     * @param xpath      The already-compiled xpath expression to match against this document, including all namespace information.  Required.
     * @param variableFinder  A variable finder to use to find values for any XPath variables. May be null if xpath.usesVariables() is false. 
     * @param allowIfEmpty If true, matching elements are allowed to not exist in elementsFoundByProcessor if they are empty
     *                     (that is, they have no non-attribute child nodes of any kind.)
     * @param elementsFoundByProcessor List of approved "operated-on" elements.  Elements matching xpath must be in this
     *                                 approved list (unless they are empty and allowIfEmpty is true)
     * @param pastTenseOperationName A human-friendly past-tense name of the operation that was performed on
     *                               elementsFoundByProcessor, ie "signed" or "encrypted".
     * @return The result of the search.
     * @throws com.l7tech.security.xml.processor.ProcessorException if the xpath is invalid, or finds results other than Nodes.
     */
    public static SearchResult searchInResult( final Logger logger,
                                               final Document doc,
                                               final DomCompiledXpath xpath,
                                               final XpathVariableFinder variableFinder,
                                               final boolean allowIfEmpty,
                                               final ParsedElement[] elementsFoundByProcessor,
                                               final String pastTenseOperationName )
            throws ProcessorException
    {
        List<Element> selectedNodes;
        try {
            selectedNodes = xpath.rawSelectElements(doc, variableFinder);
        } catch (JaxenException e) {
            // this is thrown when there is an error in the expression
            // this is therefore a bad policy
            throw new ProcessorException(e);
        }

        // the element is not there so there is nothing to check
        if (selectedNodes.isEmpty()) {
            logger.fine("No elements matching " + xpath + " are present in the undecorated message; " +
                        "assertion therefore fails.");
            return new SearchResult(FALSIFIED, false);
        }

        // to assert this, i must make sure that all of these nodes are part of the nodes
        // that were decrypted by the wss processor
        List<ParsedElement> elements = new ArrayList<ParsedElement>();
        boolean foundany = false;
        for (Object obj : selectedNodes) {
            if (!(obj instanceof Node)) {
                logger.severe("Invalid xpath: The xpath result included a non-Node object of type " + obj.getClass().getName());
                return new SearchResult(SERVER_ERROR, false);
            }

            Node node = (Node) obj;
            if (allowIfEmpty && DomUtils.elementIsEmpty(node)) {
                logger.finer("The element " + xpath + " was found in this message but was empty and so needn't be " + pastTenseOperationName + ".");
                continue;
            }

            foundany = true;

            ParsedElement element = getParsedElementForNode( node, elementsFoundByProcessor );
            if ( element != null ) {
                logger.finest("An element " + xpath + " was found in this " +
                        "message, and was properly " + pastTenseOperationName + ".");
                elements.add(element);
            } else {
                logger.info("An element " + xpath + " was found in this " +
                        "message, but was neither empty nor properly " + pastTenseOperationName + "; assertion therefore fails.");
                return new SearchResult(FALSIFIED, true);
            }
        }

        if (foundany)
            logger.fine("All matching elements were either empty or properly "+pastTenseOperationName+"; assertion therefore succeeds.");
        else
            logger.fine("All matching elements were empty; assertion therefore succeeds.");
        return new SearchResult(NO_ERROR, false, elements);
    }

    /**
     * Find a ParsedElement from the list of elements found by the processor.
     *
     * The node must be a reference to a node in the exact same document as the members of
     * elementsFoundByProcessor.  A match is only detected if it is an exact match -- that
     * is, this method will return false even if some parent element of node is present in
     * the list of elements that were found.
     *
     * @param node the node to check
     * @param elementsFoundByProcessor the list of ParsedElement to see if it is in
     * @return The ParsedElement for the node, or null if not found 
     */
    public static ParsedElement getParsedElementForNode(Node node, final ParsedElement[] elementsFoundByProcessor) {
        ParsedElement element = null;
        for (ParsedElement anElementsFoundByProcessor : elementsFoundByProcessor) {
            if (anElementsFoundByProcessor.asElement() == node) {
                // we got the bugger!
                element = anElementsFoundByProcessor;
                break;
            }
        }
        return element;
    }

    /**
     * Check if a node is present in the list of elements found by the processor.  The node must be a reference
     * to a node in the exact same document as the members of elementsFoundByProcessor.  A match is only
     * detected if it is an exact match -- that is, this method will return false even if some parent element
     * of node is present in the list of elements that were found.
     *
     * @param node the node to check
     * @param elementsFoundByProcessor the list of ParsedElement to see if it is in
     * @return true if node was found in the list.
     */
    public static boolean nodeIsPresent(Node node, final ParsedElement[] elementsFoundByProcessor) {
        return getParsedElementForNode(node, elementsFoundByProcessor) != null;
    }

    public static class SearchResult {
        private final int resultCode;
        private final boolean foundButWasntOperatedOn;
        private final Collection<ParsedElement> elements;

        private SearchResult(int resultCode, boolean foundButWasntOperatedOn) {
            this(resultCode, foundButWasntOperatedOn, Collections.<ParsedElement>emptyList());
        }

        private SearchResult(int resultCode, boolean foundButWasntOperatedOn, Collection<ParsedElement> elements) {
            this.resultCode = resultCode;
            this.foundButWasntOperatedOn = foundButWasntOperatedOn;
            this.elements = Collections.unmodifiableCollection( new ArrayList<ParsedElement>(elements) );
        }

        public int getResultCode() {return resultCode;}
        public boolean isFoundButWasntOperatedOn() {return foundButWasntOperatedOn;}
        public ParsedElement[] getElements() {return elements.toArray(new ParsedElement[elements.size()]);}
    }


}
