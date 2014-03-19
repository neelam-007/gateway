package com.l7tech.server.stepdebug;

import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 *
 */
public class DebugManagerStub implements DebugManager {

    public DebugManagerStub(@NotNull AuditFactory auditFactory) {
    }

    @NotNull
    @Override
    public DebugContext createDebugContext(@NotNull Goid entityGoid, @NotNull PolicyType policyType) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull
    @Override
    public Option<String> startDebug(@NotNull String taskId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stopDebug(@NotNull String taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stepOver(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stepInto(@NotNull String taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stepOut(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void resume(@NotNull String taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void terminateDebug(@NotNull String taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void toggleBreakpoint(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeAllBreakpoints(@NotNull String taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addUserContextVariable(@NotNull String taskId, @NotNull String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserContextVariable(@NotNull String taskId, @NotNull String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nullable
    @Override
    public DebugContext getDebugContext(@NotNull String taskId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onMessageArrived(@NotNull PolicyEnforcementContext pec, @NotNull Goid entityGoid) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onMessageFinished(@NotNull PolicyEnforcementContext pec) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
