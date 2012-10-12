package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.TestKeys;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.io.PermissiveX509TrustManager;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.xml.xpath.*;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class OAuthToolkitTestUtility {
    public static String getAccessTokenFromJsonResponse(@NotNull final GenericHttpResponse response) throws Exception {
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        return responseBody.substring(responseBody.indexOf("\"access_token\":\"") + 16, responseBody.indexOf("\","));
    }

    /**
     * Use if client cert auth is not required.
     */
    public static SSLSocketFactory getSSLSocketFactory() throws Exception {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{}, new X509TrustManager[]{new PermissiveX509TrustManager()}, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    /**
     * Use if SSL with client cert auth is required.
     */
    public static SSLSocketFactory getSSLSocketFactoryWithKeyManager() throws Exception {
        final Pair<X509Certificate, PrivateKey> certAndKey = TestKeys.getCertAndKey("RSA_1024");
        final SingleCertX509KeyManager keyManager = new SingleCertX509KeyManager(new X509Certificate[]{certAndKey.left}, certAndKey.right);
        final TrustManager trustManager = new PermissiveX509TrustManager();
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{keyManager}, new TrustManager[]{trustManager}, new SecureRandom());
        return sslContext.getSocketFactory();
    }

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

    public static String getTokenFromHtmlForm(@NotNull final String html) throws SAXException, XPathExpressionException {
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
