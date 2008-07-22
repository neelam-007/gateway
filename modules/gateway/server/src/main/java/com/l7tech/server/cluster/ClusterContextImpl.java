package com.l7tech.server.cluster;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.beans.factory.InitializingBean;

import com.l7tech.gateway.common.cluster.ClusterContext;
import com.l7tech.gateway.common.logging.GenericLogAdmin;

/**
 * Cluster context implementation.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ClusterContextImpl extends ApplicationObjectSupport implements ClusterContext, InitializingBean {

    //- PUBLIC

    /**
     *
     */
    public ClusterContextImpl(GenericLogAdmin logAdmin) {
        this.genericLogAdmin = logAdmin;
    }

    /**
     *
     */
    public GenericLogAdmin getLogAdmin() throws SecurityException {
        return genericLogAdmin;
    }

    /**
     *
     */
    public void afterPropertiesSet() throws Exception {
        checkServices();
    }

    //- PRIVATE

    /**
     *
     */
    private final GenericLogAdmin genericLogAdmin;

    /**
     *
     */
    private void checkServices() {
        if(genericLogAdmin==null) {
            throw new IllegalStateException("genericLogAdmin not initialized!");
        }
    }
}
