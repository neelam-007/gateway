package com.l7tech.xml.xpath;

import org.jaxen.saxpath.XPathHandler;
import org.jaxen.saxpath.SAXPathException;

/**
 * An {@link XPathHandler} that ignores all events.  Subclasses can override methods to pay attention to
 * one or more of them.
 * <p/>
 * This is specific to the version of SAXPath built into Jaxen.
 */
public class XPathHandlerAdapter implements XPathHandler {
    public void startXPath() throws SAXPathException {
    }

    public void endXPath() throws SAXPathException {
    }

    public void startPathExpr() throws SAXPathException {
    }

    public void endPathExpr() throws SAXPathException {
    }

    public void startAbsoluteLocationPath() throws SAXPathException {
    }

    public void endAbsoluteLocationPath() throws SAXPathException {
    }

    public void startRelativeLocationPath() throws SAXPathException {
    }

    public void endRelativeLocationPath() throws SAXPathException {
    }

    public void startNameStep(int i, String s, String s1) throws SAXPathException {
    }

    public void endNameStep() throws SAXPathException {
    }

    public void startTextNodeStep(int i) throws SAXPathException {
    }

    public void endTextNodeStep() throws SAXPathException {
    }

    public void startCommentNodeStep(int i) throws SAXPathException {
    }

    public void endCommentNodeStep() throws SAXPathException {
    }

    public void startAllNodeStep(int i) throws SAXPathException {
    }

    public void endAllNodeStep() throws SAXPathException {
    }

    public void startProcessingInstructionNodeStep(int i, String s) throws SAXPathException {
    }

    public void endProcessingInstructionNodeStep() throws SAXPathException {
    }

    public void startPredicate() throws SAXPathException {
    }

    public void endPredicate() throws SAXPathException {
    }

    public void startFilterExpr() throws SAXPathException {
    }

    public void endFilterExpr() throws SAXPathException {
    }

    public void startOrExpr() throws SAXPathException {
    }

    public void endOrExpr(boolean b) throws SAXPathException {
    }

    public void startAndExpr() throws SAXPathException {
    }

    public void endAndExpr(boolean b) throws SAXPathException {
    }

    public void startEqualityExpr() throws SAXPathException {
    }

    public void endEqualityExpr(int i) throws SAXPathException {
    }

    public void startRelationalExpr() throws SAXPathException {
    }

    public void endRelationalExpr(int i) throws SAXPathException {
    }

    public void startAdditiveExpr() throws SAXPathException {
    }

    public void endAdditiveExpr(int i) throws SAXPathException {
    }

    public void startMultiplicativeExpr() throws SAXPathException {
    }

    public void endMultiplicativeExpr(int i) throws SAXPathException {
    }

    public void startUnaryExpr() throws SAXPathException {
    }

    public void endUnaryExpr(int i) throws SAXPathException {
    }

    public void startUnionExpr() throws SAXPathException {
    }

    public void endUnionExpr(boolean b) throws SAXPathException {
    }

    public void number(int i) throws SAXPathException {
    }

    public void number(double v) throws SAXPathException {
    }

    public void literal(String s) throws SAXPathException {
    }

    public void variableReference(String s, String s1) throws SAXPathException {
    }

    public void startFunction(String s, String s1) throws SAXPathException {
    }

    public void endFunction() throws SAXPathException {
    }
}
