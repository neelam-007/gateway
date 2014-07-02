package com.l7tech.server.audit;

import com.l7tech.util.Functions;
/**
 *
 */
public class AuditContextUtils {

    //- PUBLIC

    /**
     * Is the current record a system generated record?
     *
     * @return true if system
     */
    public static boolean isSystem() {
        return systemThreadLocal.get();
    }

    /**
     * Sets whether the current record is a system generated record.
     *
     * @param system true if system.
     */
    public static void setSystem(boolean system) {
        systemThreadLocal.set( system );
    }

    /**
     * Execute some code with the system flag set to true, restoring the flag's previous value
     * once execution completes.
     *
     * @param task a task to execute with the current audit record flagged as system generated. Required.
     */
    public static void doAsSystem(Runnable task) {
        boolean isSystem = AuditContextUtils.isSystem();
        try {
            AuditContextUtils.setSystem( true );
            task.run();
        } finally {
            AuditContextUtils.setSystem( isSystem );
        }
    }


    public static AuditsCollector getAuditsCollector() {
        return auditsCollectorThreadLocal.get();
    }

    /**
     * Run the specified code with a collector to collect administrative audit records.
     *
     * @param collector  a collector to collect administrative audit records.
     */
    public static <T> T doWithAuditsCollector(AuditsCollector collector, Functions.Nullary<T> callable){
        final AuditsCollector oldac = auditsCollectorThreadLocal.get();
        auditsCollectorThreadLocal.set(collector);
        try {
            return callable.call();
        } finally {
            auditsCollectorThreadLocal.set(oldac);
        }
    }


    //- PRIVATE

    private static final ThreadLocal<Boolean> systemThreadLocal = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    private static final ThreadLocal<AuditsCollector> auditsCollectorThreadLocal = new ThreadLocal<AuditsCollector>(){
        @Override
        protected AuditsCollector initialValue() {
            return null;
        }
    };

}
