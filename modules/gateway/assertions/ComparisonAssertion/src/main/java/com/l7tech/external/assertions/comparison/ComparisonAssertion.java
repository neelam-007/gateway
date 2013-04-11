package com.l7tech.external.assertions.comparison;

import com.l7tech.external.assertions.comparison.wsp.EqualityRenamedToComparison;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.*;
import com.l7tech.util.ComparisonOperator;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Processes the value resulting from the evaluation of an expression through any number of {@link Predicate}s.
 * <p/>
 * If the use of a {@link DataTypePredicate} is desired, it must come first in the {@link #predicates} array.
 * <p/>
 * Context variables are supported in {@link #leftValue} at runtime using ${var} syntax.
 *
 * @see com.l7tech.server.message.PolicyEnforcementContext#getVariable(String)
 * @see com.l7tech.server.message.PolicyEnforcementContext#setVariable(String, Object)
 */
public class ComparisonAssertion extends Assertion implements UsesVariables {
    /**
     * The maximum user definable field length.  Any text exceeding this length will be truncated.
     */
    private static final int MAX_USER_DEFINABLE_FIELD_LENGTH = 60;
    private static final String META_INITIALIZED = ComparisonAssertion.class.getName() + ".metadataInitialized";
    public static final List<DataType> DATA_TYPES = Collections.unmodifiableList(Arrays.asList(
        DataType.UNKNOWN,
        DataType.STRING,
        DataType.INTEGER,
        DataType.DECIMAL,
        DataType.BOOLEAN,
        DataType.DATE_TIME
    ));

    private String leftValue;
    private Predicate[] predicates = new Predicate[0];
    private MultivaluedComparison multivaluedComparison = MultivaluedComparison.ALL;
    private boolean expressionIsVariable = true;
    public static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.comparison.ComparisonAssertion");

    /**
     * Returns the variables referenced in {@link #leftValue}, as well as any referenced in the
     * {@link BinaryPredicate#getRightValue()} values of our {@link #predicates}. (Only {@link BinaryPredicate} supports the
     * use of variables on the right-hand side of the equation)
     */
    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        final List<String> values = new ArrayList<String>();
        values.add(leftValue);
        for (final Predicate predicate : predicates) {
            if (predicate instanceof BinaryPredicate) {
                BinaryPredicate bp = (BinaryPredicate) predicate;
                values.add(bp.getRightValue());
            }
        }
        return Syntax.getReferencedNames(values.toArray(new String[values.size()]));
    }

    public ComparisonAssertion() {
    }

    public Predicate[] getPredicates() {
        return predicates;
    }

    /**
     * The predicates for this assertion.  Any {@link DataTypePredicate} included in {@link #predicates} <b>must</b>
     * come first in the array.
     */
    public void setPredicates(Predicate... predicates) {
        for (int i = 0; i < predicates.length; i++) {
            Predicate predicate = predicates[i];
            if (predicate instanceof DataTypePredicate && i > 0) {
                throw new IllegalArgumentException("DataType predicate found at index " + i + "; it must be the first predicate");
            }
        }
        this.predicates = predicates;
    }

    public String getExpression1() {
        return leftValue;
    }

    public void setExpression1(String expression1) {
        this.leftValue = expression1;
    }

    @NotNull
    public MultivaluedComparison getMultivaluedComparison() {
        return multivaluedComparison;
    }

    public void setMultivaluedComparison(final MultivaluedComparison multivaluedComparison) {
        this.multivaluedComparison = multivaluedComparison == null ?
                MultivaluedComparison.ALL :
                multivaluedComparison;
    }

    @Deprecated // This actually meant to compare all values of multivalued variables and fail if it does not exist.
    public void setFailIfVariableNotFound(boolean failIfVariableNotFound) {
        this.expressionIsVariable = failIfVariableNotFound;
    }

    private boolean check() {
        if (predicates == null || predicates.length == 0) {
            predicates = new Predicate[]{new BinaryPredicate()};
            return true;
        } else
            return predicates.length == 1 && predicates[0] instanceof BinaryPredicate;
    }

    private BinaryPredicate compat() {
        return ((BinaryPredicate) predicates[0]);
    }

    /**
     * @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}
     */
    @Deprecated
    public String getExpression2() {
        if (check()) return compat().getRightValue();
        return null;
    }

    /**
     * @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}
     */
    @Deprecated
    public void setExpression2(String expression2) {
        if (check()) compat().setRightValue(expression2);
    }

    /**
     * @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}
     */
    @Deprecated
    public ComparisonOperator getOperator() {
        if (check()) return compat().getOperator();
        return null;
    }

    /**
     * @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}
     */
    @Deprecated
    public void setOperator(ComparisonOperator operator) {
        if (check()) compat().setOperator(operator);
    }

    /**
     * @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}
     */
    @Deprecated
    public boolean isNegate() {
        return check() && compat().isNegated();
    }

    /**
     * @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}
     */
    @Deprecated
    public void setNegate(boolean negate) {
        if (check()) compat().setNegated(negate);
    }

    /**
     * @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return check() && compat().isCaseSensitive();
    }

    /**
     * @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}
     */
    @Deprecated
    public void setCaseSensitive(boolean caseSensitive) {
        if (check()) compat().setCaseSensitive(caseSensitive);
    }

    private final static String baseName = "Compare Expression";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ComparisonAssertion>() {
        @Override
        public String getAssertionName(final ComparisonAssertion assertion, final boolean decorate) {
            if (!decorate) return baseName;

            StringBuilder name = new StringBuilder("Compare ");
            // in the upgrade case from Escalor or fangtooth the isExpressionIsVariable might be set to true even if it is an expression. So we need to check to see if it is a single variable
            name.append(assertion.isExpressionIsVariable() && (assertion.getExpression1() == null || assertion.getExpression1().isEmpty() || Syntax.isOnlyASingleVariableReferenced(assertion.getExpression1()))
                    ? "Variable" : "Expression");
            name.append(": ");
            String expression1 = assertion.getExpression1();
            if(expression1.length() > MAX_USER_DEFINABLE_FIELD_LENGTH){
                expression1 = expression1.substring(0, MAX_USER_DEFINABLE_FIELD_LENGTH) + "...";
            }
            name.append(expression1).append(" ");

            Predicate[] predicatesLocal = assertion.getPredicates();
            for (int i = 0; i < predicatesLocal.length; i++) {
                Predicate pred = predicatesLocal[i];
                name.append(pred.toString());

                if (i == predicatesLocal.length - 2)
                    name.append(" and ");
                else if (i < predicatesLocal.length - 1)
                    name.append(", ");
            }

            if(assertion.isExpressionIsVariable()){
                name.append("; ");
                name.append(resources.getString("multivaluedComparison.label"));
                name.append(" ");
                String labelKey = "multivaluedComparison." + assertion.getMultivaluedComparison().name() + ".text";
                name.append(resources.getString(labelKey));
            }


            return name.toString();

        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Request to appear in "misc" ("Service Availability") palette folder
        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Evaluate an expression against a series of rules during the runtime processing of a policy.");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "com.l7tech.external.assertions.comparison.console.ComparisonAssertionAdvice");

        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.comparison.console.ComparisonPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Compare Expression Properties");
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Comparison" rather than "set:modularAssertions"
        meta.put(FEATURE_SET_NAME, "(fromClass)");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/check16.gif");
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(WSP_EXTERNAL_NAME, "ComparisonAssertion");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(ComparisonOperator.class, "operator"),
                new Java5EnumTypeMapping(MultivaluedComparison.class, "multivaluedComparison"),
                new ArrayTypeMapping(new Predicate[0], "predicates"),
                new AbstractClassTypeMapping(Predicate.class, "predicate"),
                new BeanTypeMapping(BinaryPredicate.class, "binary"),
                new BeanTypeMapping(CardinalityPredicate.class, "cardinality"),
                new BeanTypeMapping(StringLengthPredicate.class, "stringLength"),
                new BeanTypeMapping(RegexPredicate.class, "regex"),
                new BeanTypeMapping(DataTypePredicate.class, "dataType"),
                new WspEnumTypeMapping(DataType.class, "type"),
                new BeanTypeMapping(EmptyPredicate.class, "empty")
        )));

        meta.put(WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put(EqualityRenamedToComparison.equalityCompatibilityMapping.getExternalName(), EqualityRenamedToComparison.equalityCompatibilityMapping);
        }});

        meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.comparison.server.ServerComparisonAssertion");
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.comparison.server.ComparisonAssertionModuleLifecycle");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public ComparisonAssertion clone() {
        ComparisonAssertion clone = (ComparisonAssertion) super.clone();
        try {
            //we have an array of predicates which need to be manually cloned, clone each predicates
            Predicate[] clonePreds = new Predicate[predicates.length];
            for (int i = 0; i < predicates.length; i++) {
                clonePreds[i] = (Predicate) predicates[i].clone();
            }
            clone.setPredicates(clonePreds);
        } catch (CloneNotSupportedException cnse) {
            //can this happen? Not sure what we want to do with this, doing what Assertion.clone() does (which may be bad)
            throw new RuntimeException(cnse);
        }

        return clone;
    }

    /**
     * Returns true if the expression is to be treated as a single variable.
     * @return true if the expression is to be treated as a single variable.
     */
    public boolean isExpressionIsVariable() {
        return expressionIsVariable;
    }

    public void setExpressionIsVariable(boolean expressionIsVariable) {
        this.expressionIsVariable = expressionIsVariable;
    }
}
