package com.l7tech.skunkworks.tarari;

import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.rax.cursor.RaxCursor;
import com.tarari.xml.rax.cursor.RaxCursorFactory;
import com.tarari.xml.rax.fastxpath.XPathCompiler;
import com.tarari.xml.rax.fastxpath.XPathProcessor;
import com.tarari.xml.rax.fastxpath.XPathResult;
import com.tarari.xml.rax.fastxpath.XPathLoader;
import com.tarari.xml.XmlSource;
import com.tarari.xml.xpath10.expr.Expression;
import com.tarari.xml.xpath10.parser.ExpressionParser;
import com.tarari.xml.xpath10.parser.XPathParseContext;
import com.tarari.xml.xpath10.XPathContext;
import com.tarari.xml.xpath10.object.XObject;

import java.io.FileInputStream;
import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.XMLConstants;

/**
 * Test for Tarari XPath
 *
 * An example use for this is:
 *
 *    GetTopStories_1K_REQUEST.xml "//*[local-name()='DummyElement']"
 *    GetTopStories_1K_REQUEST.xml "/*[namespace-uri()='http://schemas.xmlsoap.org/soap/envelope/' and local-name()='Envelope']/*[namespace-uri()='http://schemas.xmlsoap.org/soap/envelope/' and local-name()='Body']/*[namespace-uri()='http://www.xignite.com/services/' and local-name()='GetTopStories']"
 *    GetTopStories_1K_REQUEST..xml "/soap:Envelope/soap:Body/tns:GetTopStories"
 *
 * Fast XPaths do not use namespaces, so in the last case the message must use
 * the same prefixes to match (and the namespaces are ignored).
 */
public class TarariXPathTest {

    public static void main(String[] args) throws Exception {
        if ( args.length != 2 && args.length != 3 ) {
            System.out.println("Usage:\n\tTarariXPathTest <xml-file> <xpath> [<compiledxpaths-file>]");
        } else {
            String filename = args[0];
            String expression = args[1];

            System.out.println("Creating RAX document.");
            RaxDocument document = RaxDocument.createDocument(new XmlSource(new FileInputStream(filename)));

            System.out.println("Trying as Fast XPath");
            try {
                if ( args.length == 3 ) {
                    String precompiledFile = args[2];
                    XPathCompiler.reset();
                    XPathCompiler.compile( new String[]{expression} );

                    // These steps do not seem to do anything ... (works without)
                    XPathCompiler.write( precompiledFile );
                    XPathLoader.preCompiledLoad( precompiledFile );
                } else {
                    XPathLoader.unload();
                    XPathLoader.load( new String[]{expression} );
                }

                XPathProcessor xpathProcessor = new XPathProcessor(document);
                XPathResult xpathResult = xpathProcessor.processXPaths();
                int count = xpathResult.getCount( 1 );

                System.out.println( "Result count: " + count );
            } catch (Exception e) {
                e.printStackTrace();
            }

            // now try the direct XPath
            System.out.println("Trying as Direct XPath");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware( true );
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom = db.parse( new File(filename) );
            try {
                ExpressionParser expressionParser = new ExpressionParser();

                // Configure the namespace map for Direct XPath 1.0
                XPathParseContext parseContext = expressionParser.getParseContext();
                NamedNodeMap docAttributes = dom.getDocumentElement().getAttributes();
                for ( int i=0; i<docAttributes.getLength(); i++ ) {
                    Attr attrNode = (Attr) docAttributes.item(i);
                    String attPrefix = attrNode.getPrefix();
                    String attNsUri = attrNode.getNamespaceURI();
                    String attLocalName = attrNode.getLocalName();
                    String attValue = attrNode.getValue();

                    if (attValue != null && attValue.trim().length() > 0) {
                        if ("xmlns".equals(attPrefix) && XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attNsUri)) {
                            System.out.println("Declaring namespace: " + attLocalName + " = " + attValue);
                            parseContext.declareNamespace(attLocalName, attValue);
                        } else if ("xmlns".equals(attLocalName)) {
                            // For our test we just use namespaces from the document so
                            // warn if there is a namespace that cannot be used in the
                            // XPath. The test document should be re-written to use a
                            // prefix for this namespace to allow it to be used in the
                            // XPath expression.
                            System.out.println("WARNING default namespace cannot be XPathed (no prefix): " + attValue);
                        }
                    }
                }
                
                Expression expr = expressionParser.parseExpression(expression);
                XPathContext xpathContext = new XPathContext();
                RaxCursorFactory raxCursorFactory = new RaxCursorFactory();
                RaxCursor cursor = raxCursorFactory.createCursor("", document);
                xpathContext.setNode(cursor);

                final XObject xo = expr.toXObject(xpathContext);
                int resultType = xo.getType();
                switch (resultType) {
                    case XObject.TYPE_BOOLEAN:
                        System.out.println( "BOOLEAN: " + xo.toBooleanValue() );
                        break;
                    case XObject.TYPE_NUMBER:
                        System.out.println( "NUMBER: " + xo.toNumberValue() );
                        break;
                    case XObject.TYPE_STRING:
                        System.out.println( "STRING: " + xo.toStringValue() );
                        break;
                    case XObject.TYPE_NODESET:
                        System.out.println( "NODESET: " + xo.toNodeSet().size() );
                        break;
                    default:
                        throw new Exception("Tarari direct XPath produced unsupported result type " + resultType);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
