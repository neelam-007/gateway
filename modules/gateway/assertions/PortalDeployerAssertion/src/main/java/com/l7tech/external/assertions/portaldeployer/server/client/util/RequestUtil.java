package com.l7tech.external.assertions.portaldeployer.server.client.util;

import java.util.List;
import javax.net.ssl.SSLSocketFactory;

/**
 * A process request interface
 * @author raqri01
 */
public interface RequestUtil {
  RequestResponse processRequest(final String urlstr, final List<NameValuePair> parameters, final List<NameValuePair> headers, String body, String contentType, String method, SSLSocketFactory sslSocketFactory) throws Exception;
}
