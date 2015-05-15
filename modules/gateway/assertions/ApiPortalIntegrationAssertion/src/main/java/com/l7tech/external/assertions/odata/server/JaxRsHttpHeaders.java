package com.l7tech.external.assertions.odata.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.message.HttpRequestKnob;
import org.odata4j.producer.resources.HeaderMap;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;
import java.util.logging.Logger;

/**
 * Custom header class that tries to map l7tech HttpRequestKnob to JAX-RS HttpHeader
 *
 * @author rraquepo, 8/22/13
 */
public class JaxRsHttpHeaders implements HttpHeaders {
    private HttpRequestKnob httpRequestKnob;
    protected final Logger logger;
    List<MediaType> mediaTypes = new ArrayList<>();
    private MediaType defaultMediaType;

    JaxRsHttpHeaders(HttpRequestKnob httpRequestKnob) {
        this.httpRequestKnob = httpRequestKnob;
        this.logger = Logger.getLogger(getClass().getName());
        Map<String, String> params = new HashMap<String, String>();
        params.put("charset", "UTF-8");
        MediaType md = new MediaType("application", "atom+xml", params);
        mediaTypes.add(md);//we will always default to xml

        //odata4j will default to json if it see's a json accept header, let's try to support that since it's more efficient in the wire
        String[] values = httpRequestKnob.getHeaderValues("accept");
        for (String value : values) {
            if (value.toLowerCase().indexOf("json") >= 0) {
                MediaType jsonType = new MediaType("application", "json", params);
                defaultMediaType = jsonType;
                break;
            }
        }
        String[] ctypes = httpRequestKnob.getHeaderValues("content-type");
        for (String value : ctypes) {
            if (value.toLowerCase().indexOf("json") >= 0) {
                MediaType jsonType = new MediaType("application", "json", params);
                defaultMediaType = jsonType;
                break;
            }
        }
        if (defaultMediaType == null) {
            defaultMediaType = md;
        }
    }

    @Override
    public List<String> getRequestHeader(String s) {
        return Arrays.asList(httpRequestKnob.getHeaderValues(s));
    }

    @Override
    public MultivaluedMap<String, String> getRequestHeaders() {
        HeaderMap map = new HeaderMap();
        for (String header : httpRequestKnob.getHeaderNames()) {
            String[] values = httpRequestKnob.getHeaderValues(header);
            map.put(header, Arrays.asList(values));
        }
        return map;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return mediaTypes;
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        List<Locale> locales = new ArrayList<>();
        return locales;
    }

    @Override
    public MediaType getMediaType() {
        return defaultMediaType;
    }

    @Override
    public Locale getLanguage() {
        return Locale.ENGLISH;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        Map<String, Cookie> cookies = new HashMap();
        for (HttpCookie l7cookie : httpRequestKnob.getCookies()) {
            Cookie cookie = new Cookie(l7cookie.getCookieName(), l7cookie.getCookieValue());
            cookies.put(l7cookie.getCookieName(), cookie);
        }

        return cookies;
    }
}
