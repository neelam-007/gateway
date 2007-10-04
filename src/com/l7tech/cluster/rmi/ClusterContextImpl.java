package com.l7tech.cluster.rmi;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.beans.factory.InitializingBean;

import com.l7tech.cluster.ClusterContext;
import com.l7tech.logging.GenericLogAdmin;

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
