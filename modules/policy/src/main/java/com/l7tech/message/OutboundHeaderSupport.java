package com.l7tech.message;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.util.Pair;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Support class that can be used to assist implementation of HasOutboundHeaders.
 */
public class OutboundHeaderSupport implements HasOutboundHeaders {

    //- PUBLIC

    @Override
    public void setDateHeader(String name, long date) {
        headersToSend.add(new Pair<String, Object>(name, date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        headersToSend.add(new Pair<String, Object>(name, date));
    }

    @Override
    public void setHeader(String name, String value) {
        // Clear out any previous value
        for ( Iterator<Pair<String, Object>> i = headersToSend.iterator(); i.hasNext();) {
            Pair<String, Object> pair = i.next();
            if (name.equalsIgnoreCase(pair.left)) i.remove();
        }
        headersToSend.add(new Pair<String, Object>(name, value));
    }

    @Override
    public void addHeader(String name, String value) {
        headersToSend.add(new Pair<String, Object>(name, value));
    }

    @Override
    public String[] getHeaderValues(String name) {
        ArrayList<Pair<String,Object>> tmp = new ArrayList<Pair<String,Object>>();
        for (Pair<String,Object> pair : headersToSend) {
            if (name.compareToIgnoreCase(pair.left) == 0) {
                tmp.add(pair);
            }
        }
        String[] output = new String[tmp.size()];
        int i = 0;
        for (Pair pair : tmp) {
            output[i] = pair.right.toString();
            i++;
        }
        return output;
    }

    @Override
    public String[] getHeaderNames() {
        ArrayList<String> tmp = new ArrayList<String>();
        for (Pair<String,Object> pair : headersToSend) {
            tmp.add(pair.left);
        }

        return tmp.toArray(new String[tmp.size()]); 
    }

    @Override
    public boolean containsHeader(String name) {
        for (Pair<String, Object> pair : headersToSend) {
            if (name.equalsIgnoreCase(pair.left)) return true;
        }

        return false;
    }

    @Override
    public void removeHeader( final String name ) {
        for ( final Iterator<Pair<String, Object>> headerIterator = headersToSend.iterator();
              headerIterator.hasNext(); ) {
            final Pair<String, Object> header = headerIterator.next();
            if ( header.left.equalsIgnoreCase( name )) {
                headerIterator.remove();    
            }
        }
    }

    @Override
    public void removeHeader(final String name, final Object value) {
        for ( final Iterator<Pair<String, Object>> headerIterator = headersToSend.iterator(); headerIterator.hasNext(); ) {
            final Pair<String, Object> header = headerIterator.next();
            if ( header.left.equalsIgnoreCase(name) && header.right.equals(value)) {
                headerIterator.remove();
            }
        }
    }

    @Override
    public void clearHeaders() {
        headersToSend.clear();
    }

    @Override
    public void writeHeaders( final GenericHttpRequestParams target ) {
        if ( target != null ) {
            final Set<String> addedHeaders = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            for ( final Pair<String, Object> pair : headersToSend ) {
                final String headerName = pair.left;
                final Object headerValue = pair.right;
                final HttpHeader header;
                if ( headerValue instanceof Long ) {
                    header = GenericHttpHeader.makeDateHeader( headerName, new Date((Long)headerValue));
                } else if ( headerValue instanceof String ) {
                    header = new GenericHttpHeader( headerName, (String)headerValue );
                } else {
                    header = null;
                }

                if ( header != null ) {
                    if ( addedHeaders.contains( headerName )) {
                        target.addExtraHeader( header ); // don't overwrite a header we added
                    } else {
                        addedHeaders.add( headerName );
                        target.replaceExtraHeader( header );
                    }
                }
            }
        }
    }

    //- PACKAGE

    /**
     * Values are expected to be Long or String
     */
    final List<Pair<String, Object>> headersToSend = new ArrayList<Pair<String, Object>>();
}