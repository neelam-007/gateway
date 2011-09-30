package com.l7tech.server.policy.assertion.sla;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.sla.ThroughputQuotaQueryAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.sla.CounterInfo;
import com.l7tech.server.sla.CounterManager;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.HibernateException;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Server-side implementation of throughput quota query assertion.  Looks up a counter by its counter name,
 * and sets variables regarding its contents.
 */
public class ServerThroughputQuotaQueryAssertion extends AbstractServerAssertion<ThroughputQuotaQueryAssertion> {
    @Inject
    private CounterManager counterManager;

    private final String[] variablesUsed;

    private final String counterNameRaw;
    private final String nameVar;
    private final String secVar;
    private final String minVar;
    private final String hrVar;
    private final String dayVar;
    private final String mntVar;
    private final String lastupdateVar;

    public ServerThroughputQuotaQueryAssertion(@NotNull final ThroughputQuotaQueryAssertion assertion) {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.counterNameRaw = assertion.getCounterName();

        this.nameVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_NAME);
        this.secVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_SEC);
        this.minVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_MIN);
        this.hrVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_HR);
        this.dayVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_DAY);
        this.mntVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_MNT);
        this.lastupdateVar = prefix(ThroughputQuotaQueryAssertion.COUNTER_LASTUPDATE);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        String counterName = ExpandVariables.process(counterNameRaw, context.getVariableMap(variablesUsed, getAudit()), getAudit());

        CounterInfo info;
        try {
            info = counterManager.getCounterInfo(counterName);
        } catch (DataAccessException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Unable to query counter: " + ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (HibernateException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Unable to query counter: " + ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.SERVER_ERROR;
        }

        if (info == null) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Specified counter name does not exist in the database");
            return AssertionStatus.FAILED;
        }

        context.setVariable(nameVar, info.getName());
        context.setVariable(secVar, info.getSec());
        context.setVariable(minVar, info.getMin());
        context.setVariable(hrVar, info.getHr());
        context.setVariable(dayVar, info.getDay());
        context.setVariable(mntVar, info.getMnt());
        context.setVariable(lastupdateVar, String.valueOf(info.getLastUpdate()));
        return AssertionStatus.NONE;
    }

    private String prefix(String name) {
        final String prefix = assertion.getVariablePrefix();
        return prefix == null ? name : prefix + "." + name;
    }
}
