package com.l7tech.server.ems;

import org.restlet.Filter;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
     * A Filter that strips any query string from every request URL.
 */
public class StripQueryFilter extends Filter {
    public StripQueryFilter(Context parentContext, Restlet restlet) {
        super(parentContext, restlet);
    }

    @Override
    protected int beforeHandle(Request request, Response response) {
        request.getResourceRef().setQuery(null);
        return Filter.CONTINUE;
    }
}
