package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.io.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.xpath.*;

public final class Layer7ApiUtil {
    public static String getSessionIdFromHtmlForm(@NotNull final String html) throws SAXException, XPathExpressionException {
        // sessionId is inside an html form as a hidden field
        final Integer start = html.indexOf("<form ");
        final Integer end = html.indexOf("</form>") + 7;
        final String formXml = html.substring(start, end);
        final Document document = XmlUtil.parse(formXml);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final XPathExpression expression = xPath.compile("/form/input[@name='sessionID']");
        final Node node = (Node) expression.evaluate(document, XPathConstants.NODE);
        return node.getAttributes().getNamedItem("value").getTextContent();
    }

    public static String getTokenFromHtmlForm(final String html) throws SAXException, XPathExpressionException {
        // oauth_token is inside an html form as a hidden field
        final Integer start = html.indexOf("<form ");
        final Integer end = html.indexOf("</form>") + 7;
        final String formXml = html.substring(start, end);
        final Document document = XmlUtil.parse(formXml);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final XPathExpression expression = xPath.compile("/form/input[@name='oauth_token']");
        final Node oathTokenNode = (Node) expression.evaluate(document, XPathConstants.NODE);
        return oathTokenNode.getAttributes().getNamedItem("value").getTextContent();
    }
}
