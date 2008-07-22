package com.l7tech.server.config.exceptions;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: May 13, 2008
 * Time: 3:01:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClusteringConfigException extends Exception {
    public ClusteringConfigException() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ClusteringConfigException(String message) {
        super(message);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ClusteringConfigException(String message, Throwable cause) {
        super(message, cause);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ClusteringConfigException(Throwable cause) {
        super(cause);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
