package com.l7tech.server.stepdebug;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.stepdebug.DebugContextVariableData;
import com.l7tech.gateway.common.stepdebug.DebugResult;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Holds debug information retrieved from the {@link PolicyEnforcementContext}
 */
public class DebugPecData {

    private final Audit audit;
    private final Set<DebugContextVariableData> contextVariables;
    private String policyResult;

    private static final String[] builtInMessageContextVars = {
        "mainpart",
        "contentType"
    };

    private static final String[] httpBuiltInMessageContextVars = {
        "http.allheadervalues"
    };

    /**
     * Creates <code>DebugPecData</code>.
     *
     * @param audit the audit
     */
    public DebugPecData(@NotNull Audit audit) {
        this.audit = audit;
        this.contextVariables = Collections.synchronizedSet(new TreeSet<DebugContextVariableData>());
        this.policyResult = null;
    }

    /**
     * Updates <code>DebugPecData</code> with data from the given PEC.
     *
     * @param pec the PEC
     * @param userContextVariables the user added context variable names
     */
    public void update(@NotNull PolicyEnforcementContext pec, @NotNull Set<String> userContextVariables) {
        contextVariables.clear();

        // Built-in context variables.
        //
        Message request = pec.getRequest();
        if (request.isInitialized()) {
            contextVariables.add(this.convertToDebugContextVariableData(pec, BuiltinVariables.PREFIX_REQUEST, request, false));
        }

        Message response = pec.getResponse();
        if (response.isInitialized()) {
            contextVariables.add(this.convertToDebugContextVariableData(pec, BuiltinVariables.PREFIX_RESPONSE, response, false));
        }

        // None built-in context variables.
        //
        for (Map.Entry<String, Object> entry : pec.getAllVariables().entrySet()) {
            contextVariables.add(this.convertToDebugContextVariableData(pec, entry.getKey(), entry.getValue(), false));
        }

        // User added context variables.
        //
        for (String name : userContextVariables) {
            String expr = Syntax.SYNTAX_PREFIX + name + Syntax.SYNTAX_SUFFIX;
            Map<String, Object> vars = pec.getVariableMap(new String[]{Syntax.parse(name, Syntax.DEFAULT_MV_DELIMITER).remainingName}, audit);
            Object value = ExpandVariables.processSingleVariableAsObject(expr, vars, audit);
            contextVariables.add(this.convertToDebugContextVariableData(pec, name, value, true));
        }
    }

    /**
     * Sets the policy result.
     *
     * @param pec The PEC
     * @param currentLine the current line (equivalent to last assertion line executed)
     */
    public void setPolicyResult(@NotNull PolicyEnforcementContext pec, @Nullable Collection<Integer> currentLine) {
        AssertionStatus status = pec.getPolicyResult();
        if (status != null) {
            if (AssertionStatus.NONE.equals(status)) {
                // Successful.
                //
                policyResult = DebugResult.SUCCESSFUL_POLICY_RESULT_MESSAGE;
            } else  {
                // Error.
                // Add error message and assertion number.
                //
                StringBuilder sb = new StringBuilder(DebugResult.ERROR_POLICY_RESULT_MESSAGE);
                sb.append(". ");
                sb.append(status.getMessage());
                sb.append(": assertion number ");
                if (currentLine != null) {
                    boolean firstRun = true;
                    for (Integer number : currentLine) {
                        if (!firstRun) {
                            sb.append(".");
                        }
                        firstRun = false;
                        sb.append(number.toString());
                    }
                } else {
                    // Unexpected. But still handle it.
                    //
                    sb.append("unknown");
                }
                policyResult = sb.toString();
            }
        } else {
            policyResult = null;
        }
    }

    public void reset(@NotNull Set<String> userContextVariables) {
        contextVariables.clear();
        policyResult = null;

        // Only retain user added context variables only.
        //
        for (String name : userContextVariables) {
            this.addUserContextVariable(name);
        }
    }

    public void addUserContextVariable(@NotNull String name) {
        DebugContextVariableData data = new DebugContextVariableData(name, null, null, true);
        // Display an empty value for uninitialized user added context variables.
        //
        data.setIsDisplayNullValue(false);
        data.setIsDisplayDoubleQuotes(false);
        contextVariables.add(data);
    }

    public void removeUserContextVariable(@NotNull String name) {
        for (Iterator<DebugContextVariableData> itr = contextVariables.iterator(); itr.hasNext();) {
            DebugContextVariableData data = itr.next();
            if (data.getIsUserAdded() && data.getName().equals(name)) {
                itr.remove();
                break;
            }
        }
    }

    /**
     * Returns context variables that are currently set.
     *
     * @return the context variables
     */
    @NotNull
    public Set<DebugContextVariableData> getContextVariables() {
        // Return a copy.
        //
        synchronized (contextVariables) {
            return Collections.unmodifiableSet(new TreeSet<>(contextVariables));
        }
    }

    /**
     * Returns the policy evaluation result.
     *
     * @return the policy evaluation result
     */
    @Nullable
    public String getPolicyResult() {
        return policyResult;
    }

    @NotNull
    private DebugContextVariableData convertToDebugContextVariableData(@NotNull PolicyEnforcementContext pec, @NotNull String name, @Nullable Object value, boolean isUserAdded) {
        DebugContextVariableData result;

        if (value != null) {
            if (value instanceof Message) {
                result = this.processContextVariableMessageType(pec, name, (Message) value, isUserAdded);
            } else if (value instanceof List) {
                result = this.processContextVariableListType(pec, name, (List) value, isUserAdded);
            } else if (value instanceof Object[]) {
                result = processContextVariableArrayType(pec, name, (Object[]) value, isUserAdded);
            } else {
                result = this.processContextVariableOtherType(name, value, isUserAdded);
            }
        } else {
            result = new DebugContextVariableData(name, null, null, isUserAdded);
        }

        return result;
    }

    @NotNull
    private DebugContextVariableData processContextVariableMessageType(@NotNull PolicyEnforcementContext pec, @NotNull String name, @NotNull Message msg, boolean isUserAdded) {
        DebugContextVariableData result = new DebugContextVariableData(name, null, msg.getClass().getSimpleName(), isUserAdded);
        result.setIsDisplayNullValue(false);

        for (String messageContextVar : builtInMessageContextVars) {
            String childName = name + "." + messageContextVar;
            Object childValue = ExpandVariables.processSingleVariableAsObject("${" + childName + "}", pec.getVariableMap(new String[]{childName}, audit), audit);
            DebugContextVariableData child = this.convertToDebugContextVariableData(pec, messageContextVar, childValue, isUserAdded);
            child.setParentName(name);
            result.addChild(child);
        }

        if (msg.isHttpRequest() || msg.isHttpResponse()) {
            for (String messageContextVar : httpBuiltInMessageContextVars) {
                String childName = name + "." + messageContextVar;
                Object childValue = ExpandVariables.processSingleVariableAsObject("${" + childName + "}", pec.getVariableMap(new String[]{childName}, audit), audit);
                DebugContextVariableData child = this.convertToDebugContextVariableData(pec, messageContextVar, childValue, isUserAdded);
                child.setParentName(name);
                result.addChild(child);
            }
        }

        return result;
    }

    @NotNull
    private DebugContextVariableData processContextVariableListType(@NotNull PolicyEnforcementContext pec, @NotNull String name, @NotNull List value, boolean isUserAdded) {
        return this.processContextVariableArrayType(pec, name, value.toArray(), isUserAdded);
    }

    @NotNull
    private DebugContextVariableData processContextVariableArrayType(@NotNull PolicyEnforcementContext pec, @NotNull String name, @NotNull Object[] value, boolean isUserAdded) {
        DebugContextVariableData result = new DebugContextVariableData(name, "size = " + value.length, value.getClass().getSimpleName(), isUserAdded);
        result.setIsDisplayDoubleQuotes(false);

        for (int index = 0; index < value.length; index++) {
            Object item = value[index];
            DebugContextVariableData child = this.convertToDebugContextVariableData(pec, "["+ index +"]", item, isUserAdded);
            child.setParentName(name);
            child.setChildIndex(index);
            result.addChild(child);
        }

        return result;
    }

    @NotNull
    private DebugContextVariableData processContextVariableOtherType(@NotNull String name, @NotNull Object value, boolean isUserAdded) {
        return new DebugContextVariableData(name, value.toString(), value.getClass().getSimpleName(), isUserAdded);
    }
}