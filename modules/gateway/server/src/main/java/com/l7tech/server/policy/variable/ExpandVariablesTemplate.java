package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.variable.Syntax;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents a "pre-compiled" template first argument as would be passed to {@link ExpandVariables#process}.
 * <p/>
 * This can be used instead of calling process() directly to improve performance in cases where the same template string is used
 * repeatedly with different variable map contents.
 */
public class ExpandVariablesTemplate {
    private final String template;
    private final String staticTemplate;

    /**
     * Create an ExpandVariablesTemplate that will expand the specified template.
     *
     * @param template template to expand.  Required.
     */
    public ExpandVariablesTemplate(@NotNull String template) {
        this.template = template;

        // Optimize a static template as a special case
        String staticTemplate = null;
        String[] refs = Syntax.getReferencedNames(template);
        if (refs.length < 1) {
            staticTemplate = template;
        }
        this.staticTemplate = staticTemplate;

        /*
            TODO optimize processing-of-precompiled-template to avoid repeated regex application/replacement within the process() method
            In the future this could be enhanced further, for example to avoid repeatedly applying a regex against
            the template on every invocation (perhaps by representing the prepared template as a list of static strings
            and a list of variable refs which are then interleaved for each expansion).
         */
    }

    /**
     * Get the result of calling {@link ExpandVariables#process} on the current template with the specified
     * variable map and audit.
     * <p/>
     * This method may be optimized based on the original locked-in template value (for example, a completely
     * static template, with no variable references, will bypass the call to the process method).
     *
     * @param vars variable map.  Required.
     * @param audit audit impl to use for reporting errors.  Required.
     * @return the result of expanding the template with the specified variable map.  Never null.
     * @throws RuntimeException subclass in case of error
     */
    @NotNull
    public String process(@NotNull Map<String, ?> vars, @NotNull Audit audit) {
        if (staticTemplate != null)
            return staticTemplate;
        return ExpandVariables.process(template, vars, audit);
    }
}
