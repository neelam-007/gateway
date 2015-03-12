package com.l7tech.server.task;

import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.polback.PolicyBacked;
import com.l7tech.objectmodel.polback.PolicyBackedMethod;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

/**
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class ScheduledTaskManagerImpl
        extends HibernateEntityManager<ScheduledTask, EntityHeader> implements ScheduledTaskManager {


    private final PolicyBackedServiceRegistry policyBackedServiceRegistry;

    public ScheduledTaskManagerImpl(PolicyBackedServiceRegistry policyBackedServiceRegistry) {
        this.policyBackedServiceRegistry = policyBackedServiceRegistry;
        this.policyBackedServiceRegistry.registerPolicyBackedServiceTemplate( PolicyBackedScheduledTask.class );
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return ScheduledTask.class;
    }


    @PolicyBacked
    public interface PolicyBackedScheduledTask {

        @PolicyBackedMethod
        void run( );
    }

    protected void runPolicy(ScheduledTask scheduledTask){
        // Invocation
        PolicyBackedScheduledTask task = policyBackedServiceRegistry.getImplementationProxy( PolicyBackedScheduledTask.class, scheduledTask.getPolicyGoid() );

        try {
            task.run();
            // Policy succeeded; can check return values here, if any
        } catch ( RuntimeException e ) {
            // Policy failed
        }
    }

}
