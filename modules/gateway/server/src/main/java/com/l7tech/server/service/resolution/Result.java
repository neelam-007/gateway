/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.service.PublishedService;

import java.util.Collections;
import java.util.Set;

/**
 * <p>
 * The result of running a {@link ServiceResolver} against a {@link com.l7tech.message.Message}.
 * </p><p>
 * Service Resolvers should either return {@link #NO_MATCH} if none of the services known to the resolver
 * match the request, {@link #NOT_APPLICABLE} if the resolver cannot usefully resolve requests of this type
 * (e.g. wrong transport) or return a new Result holding the {@link Set} of matching {@link PublishedService}s.
 * </p>
 * @author alex
 */
public class Result {
    private static final Set<PublishedService> NO_SERVICES = Collections.emptySet();

    public static final Result NO_MATCH = new Result(NO_SERVICES);
    public static final Result NOT_APPLICABLE = new Result(NO_SERVICES);

    private final Set<PublishedService> matches;

    public Set<PublishedService> getMatches() {
        return matches;
    }

    Result(Set<PublishedService> matches) {
        if (matches == null) throw new NullPointerException("We need some stinkin' matches");
        this.matches = matches;
    }

    public boolean equals(Object o) {
        if (o == NO_MATCH || o == NOT_APPLICABLE) return (this == o);
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Result result = (Result) o;

        if (matches != null ? !matches.equals(result.matches) : result.matches != null) return false;

        return true;
    }

    public int hashCode() {
        if (matches == NO_SERVICES) return super.hashCode();
        return (matches != null ? matches.hashCode() : 0);
    }
}
