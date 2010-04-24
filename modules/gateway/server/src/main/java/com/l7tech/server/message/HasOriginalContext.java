package com.l7tech.server.message;

import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;

/**
 * Interface implemented by objects that might possess an original PolicyEnforcementContext.
 */
public interface HasOriginalContext {
    /**
     * @return the original context's request, or null.
     */
    Message getOriginalRequest();

    /**
     * @return the original contxt's response, or null.
     */
    Message getOriginalResponse();

    /**
     * @param name the variable to look up
     * @return the value of the original context's variable, or null if there is no original context.
     * @throws NoSuchVariableException if there is an original context but it contains no such variable
     */
    Object getOriginalContextVariable(String name) throws NoSuchVariableException;

    /**
     * @return the original context, or null.
     */
    PolicyEnforcementContext getOriginalContext();
}
