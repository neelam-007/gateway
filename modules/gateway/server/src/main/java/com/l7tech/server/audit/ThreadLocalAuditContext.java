package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AuditContext implementation that delegates to a thread-local copy.
 *
 * <p>You need to set the targetId of the bean to delegate to. Be sure that it
 * is not a singleton!</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ThreadLocalAuditContext implements AuditContext, ApplicationContextAware {

    //- PUBLIC

    public void setTargetId(final String id) {
        this.targetId = id;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void addDetail(AuditDetail detail, Object source) {
        threadLocalDelegate.get().addDetail(detail, source);
    }

    public void addDetail(AuditDetail detail, Object source, Throwable exception) {
        threadLocalDelegate.get().addDetail(detail, source, exception);
    }

    public void flush() {
        threadLocalDelegate.get().flush();
    }

    public Map<Object, List<AuditDetail>> getDetails() {
        return threadLocalDelegate.get().getDetails();
    }

    public Set getHints() {
        return threadLocalDelegate.get().getHints();
    }

    public void setCurrentRecord(AuditRecord record) {
        threadLocalDelegate.get().setCurrentRecord(record);
    }

    public boolean isUpdate() {
        return threadLocalDelegate.get().isUpdate();
    }

    public void setUpdate(boolean update) {
        threadLocalDelegate.get().setUpdate(update);
    }

    public boolean isSystem() {
        return threadLocalDelegate.get().isSystem();
    }

    public void setSystem(boolean system) {
        threadLocalDelegate.get().setSystem(system);
    }

    public String[] getContextVariablesUsed() {
        return threadLocalDelegate.get().getContextVariablesUsed();
    }

    public void setContextVariables(Map<String, Object> variables) {
        threadLocalDelegate.get().setContextVariables(variables);
    }

    //- PRIVATE

    private String targetId;
    private ApplicationContext applicationContext;
    private ThreadLocal<AuditContext> threadLocalDelegate = new ThreadLocal<AuditContext>(){
        protected AuditContext initialValue() {
            return (AuditContext) applicationContext.getBean(targetId, AuditContext.class);
        }
    };
}
