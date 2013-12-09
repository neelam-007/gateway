package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.DefaultSyntaxErrorHandler;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Represents a "pre-compiled" template first argument as would be passed to {@link ExpandVariables#process}.
 * <p/>
 * This can be used instead of calling process() directly to improve performance in cases where the same template string is used
 * repeatedly with different variable map contents.
 */
public class ExpandVariablesTemplate {
    private final String staticTemplate;
    private final List<Either<String,Syntax>> components = new ArrayList<>();
    private final boolean strict;
    private final Functions.Unary<String,String> valueFilter;

    /**
     * Create an ExpandVariablesTemplate that will expand the specified template.
     *
     * @param template template to expand.  Required.
     */
    public ExpandVariablesTemplate(@NotNull String template) {
        this(template, null);
    }

    /**
     * Create an ExpandVariablesTemplate that will expand the specified template.
     *
     * @param template template to expand.  Required.
     * @param valueFilter    A filter to call on each substituted value (or null for no filtering)
     */
    public ExpandVariablesTemplate(@NotNull String template, @Nullable Functions.Unary<String,String> valueFilter) {
        // Optimize a static template as a special case
        String staticTemplate = null;
        String[] refs = Syntax.getReferencedNames(template);
        if (refs.length < 1) {
            staticTemplate = template;
        }
        this.staticTemplate = staticTemplate;
        this.valueFilter = valueFilter;
        this.strict = ExpandVariables.strict();

        Matcher matcher = Syntax.regexPattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: " + matchingCount);
            }

            matcher.appendReplacement(sb, "");
            final String beforeStr = sb.toString();
            if (beforeStr.length() > 0)
                components.add(Either.<String,Syntax>left(beforeStr));
            sb.setLength(0);

            final Syntax syntax = Syntax.parse(matcher.group(1), ExpandVariables.defaultDelimiter());
            components.add(Either.<String, Syntax>right(syntax));
        }

        matcher.appendTail(sb);
        final String tail = sb.toString();
        if (tail.length() > 0)
            components.add(Either.<String,Syntax>left(tail));
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

        StringBuilder sb = new StringBuilder();
        for (Either<String, Syntax> comp : components) {
            if (comp.isLeft()) {
                sb.append(comp.left());
            } else {
                Syntax syntax = comp.right();
                Object[] newVals = ExpandVariables.getAndFilter(vars, syntax, audit, strict);
                String replacement;
                if (newVals == null || newVals.length == 0) {
                    replacement = "";
                } else {
                    // TODO support formatters for other data types!
                    Syntax.SyntaxErrorHandler handler = new DefaultSyntaxErrorHandler(audit);
                    replacement = syntax.format(newVals, Syntax.getFormatter( newVals[0] ), handler, strict);
                }

                replacement = valueFilter != null ? valueFilter.call(replacement) : replacement;
                replacement = Matcher.quoteReplacement(replacement); // bugzilla 3022 and 6813

                sb.append(replacement);
            }
        }
        return sb.toString();
    }
}
