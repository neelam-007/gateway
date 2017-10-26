package com.l7tech.external.assertions.portaldeployer.server.client.util;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A plain java.net implementation of RequestUtil to minimize 3rd party dependencies
 *
 * @author rraquepo
 */
public class RequestUtilImpl implements RequestUtil {
  private static final Logger logger = LoggerFactory.getLogger(RequestUtilImpl.class.getName());
  private static final String UTF_8 = "UTF-8";

  /**
   * Sends a request to the gateway
   */
  public RequestResponse processRequest(final String urlstr, final List<NameValuePair> parameters, final List<NameValuePair> headers, String body, String contentType, String method, SSLSocketFactory sslSocketFactory) throws Exception {
    return RequestUtilImpl.process(urlstr, parameters, headers, body, contentType, method, sslSocketFactory);
  }

  protected static RequestResponse process(final String urlstr, final List<NameValuePair> parameters, final List<NameValuePair> headers, String body, String contentType, String method, SSLSocketFactory sslSocketFactory) throws Exception {
    logger.debug("Process Request: " + method + " " + urlstr);
    StringBuilder sb = new StringBuilder(urlstr);
    if (parameters != null && parameters.size() > 0) {
      for (NameValuePair param : parameters) {
        if (sb.indexOf("?") >= 0) {
          sb.append("&");
        } else {
          sb.append("?");
        }
        sb.append(URLEncoder.encode(param.getName(), UTF_8));
        sb.append("=");
        if (null != param.getValue()) {
          sb.append(URLEncoder.encode(param.getValue(), UTF_8));
        } else {
          sb.append(param.getValue());
        }
      }
    }

    URL url = new URL(sb.toString());
    URLConnection conn = url.openConnection();
    if (sslSocketFactory != null) {
      ((HttpsURLConnection) conn).setSSLSocketFactory(sslSocketFactory);
    }
    ((HttpsURLConnection) conn).setHostnameVerifier(new HostnameVerifier() {
      public boolean verify(String s, SSLSession sslSession) {
        return true;
      }
    });

    ((HttpURLConnection) conn).setRequestMethod(method);

    if (contentType != null && contentType.trim().length() > 0) {
      conn.setRequestProperty("Content-Type", contentType);
    } else {
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    }
    if (headers != null && headers.size() > 0) {
      for (NameValuePair pair : headers) {
        conn.setRequestProperty(pair.getName(), pair.getValue());
      }
    }
    if (body != null && body.trim().length() > 0) {
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Length", String.valueOf(body.length()));
      conn.connect();
      OutputStreamWriter fout = new OutputStreamWriter(conn.getOutputStream(), UTF_8);
      fout.write(body);
      fout.flush();
      fout.close();
    }

    conn.connect();
    int responseCode = ((HttpURLConnection) conn).getResponseCode();
    logger.debug("Response Status Code:" + responseCode);
    InputStream inputStream;
    if (responseCode >= 200 && responseCode <= 300) {
      inputStream = conn.getInputStream();
    } else {
      inputStream = ((HttpURLConnection) conn).getErrorStream();
    }
    if (inputStream == null) {
      inputStream = ((HttpURLConnection) conn).getErrorStream();
    }
    StringWriter writer = new StringWriter();
    IOUtils.copy(inputStream, writer, UTF_8);
    try {
      ((HttpURLConnection) conn).disconnect();
    } finally {
      conn = null;
    }
    RequestResponse response = new RequestResponse(responseCode, writer.toString());
    return response;
  }//end of processRequest

}
