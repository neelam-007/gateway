package com.l7tech.common.http.prov.apache;

import java.util.Map;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import org.apache.commons.httpclient.params.HttpParams;

/**
 * HttpParams implementation that caches values.
 *
 * <p>This class can be used to reduce contention for the default params when
 * shared between many threads.</p>
 *
 * @author Steve Jones
 */
public class CachingHttpParams implements HttpParams {

    //- PUBLIC

    public CachingHttpParams(HttpParams httpParams) {
        delegate = httpParams;
        cache = new ConcurrentHashMap();
    }

    public boolean getBooleanParameter(String name, boolean defaultValue) {
        Object param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        return ((Boolean)param).booleanValue();
    }

    public HttpParams getDefaults() {
        return delegate.getDefaults();
    }

    public double getDoubleParameter(String name, double defaultValue) {
        Object param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        return ((Double)param).doubleValue();
    }

    public int getIntParameter(String name, int defaultValue) {
        Object param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        return ((Integer)param).intValue();
    }

    public long getLongParameter(String name, long defaultValue) {
        Object param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        return ((Long)param).longValue();
    }

    public Object getParameter(String name) {
        return getCachedParameter(name);
    }

    public boolean isParameterFalse(String name) {
        return !getBooleanParameter(name, false);
    }

    public boolean isParameterSet(String name) {
        return getParameter(name) != null;
    }

    public boolean isParameterSetLocally(String name) {
        return delegate.isParameterSetLocally(name);
    }

    public boolean isParameterTrue(String name) {
        return getBooleanParameter(name, false);
    }

    public void setBooleanParameter(String name, boolean value) {
        throw new UnsupportedOperationException("Read only HttpParams");
    }

    public void setDefaults(HttpParams params) {
        throw new UnsupportedOperationException("Read only HttpParams");
    }

    public void setDoubleParameter(String name, double value) {
        throw new UnsupportedOperationException("Read only HttpParams");
    }

    public void setIntParameter(String name, int value) {
        throw new UnsupportedOperationException("Read only HttpParams");
    }

    public void setLongParameter(String name, long value) {
        throw new UnsupportedOperationException("Read only HttpParams");
    }

    public void setParameter(String name, Object value) {
        throw new UnsupportedOperationException("Read only HttpParams");
    }

    //- PRIVATE

    private static final Object NULL_OBJECT = new Object();

    private HttpParams delegate;
    private Map cache;

    private Object getCachedParameter(final String name) {
        Object value = cache.get(name);

        if (value == null) {
            value = delegate.getParameter(name);
            if (name != null && value != null)
                cache.put(name, value);
            else if (value == null && name != null)
                cache.put(name, NULL_OBJECT);
        } else if (value == NULL_OBJECT) {
            value = null;
        }

        return value;
    }
}
