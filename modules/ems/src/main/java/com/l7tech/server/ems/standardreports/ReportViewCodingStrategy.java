package com.l7tech.server.ems.standardreports;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.request.target.resource.SharedResourceRequestTarget;
import org.apache.wicket.request.target.coding.IRequestTargetUrlCodingStrategy;

/**
 * URL Coding strategy for viewing reports.
 */
public class ReportViewCodingStrategy implements IRequestTargetUrlCodingStrategy {

    private final String mountPath;

    public ReportViewCodingStrategy( final String path ) {
        this.mountPath = path + "/";
    }

    public IRequestTarget decode(RequestParameters requestParameters) {
        String path = requestParameters.getPath();
        if ( path.startsWith(mountPath) ) {
            path = path.substring(mountPath.length());
        }
        final String[] parts = path.split( "/", 2 );
        String id = "_";
        String file = "report.html";
        if ( parts.length == 2 ) {
            id = parts[0];
            if ( parts[1].length()>0 ) {
                file = parts[1];
            }
        } else if ( parts.length == 1 ) {
            id = parts[0];
        }

        final ValueMap parameters = new ValueMap();
        parameters.put("reportId", id);
        parameters.put("file", file);
        requestParameters.setParameters(parameters);
        requestParameters.setResourceKey("org.apache.wicket.Application/reportViewResource");
        return new SharedResourceRequestTarget(requestParameters);
    }

    public CharSequence encode(IRequestTarget requestTarget) {
        return null;
    }

    public String getMountPath() {
        return mountPath;
    }

    public boolean matches(String path) {
        return path.startsWith(mountPath);
    }

    public boolean matches(IRequestTarget requestTarget) {
        return false;
    }
}
