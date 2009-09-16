package com.l7tech.external.assertions.comparison;

import com.l7tech.util.ComparisonOperator;
import com.l7tech.util.Functions;
import com.l7tech.external.assertions.comparison.wsp.EqualityRenamedToComparison;
import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Processes the value resulting from the evaluation of an expression through any number of {@link Predicate}s.
 *
 * If the use of a {@link DataTypePredicate} is desired, it must come first in the {@link #predicates} array.
 *
 * Context variables are supported in {@link #leftValue} at runtime using ${var} syntax.  
 *  
 * @see com.l7tech.server.message.PolicyEnforcementContext#getVariable(String)
 * @see com.l7tech.server.message.PolicyEnforcementContext#setVariable(String, Object)
 */
public class ComparisonAssertion extends Assertion implements UsesVariables {
    private String leftValue;
    private Predicate[] predicates = new Predicate[0];
    public static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.comparison.ComparisonAssertion");

    /**
     * Returns the variables referenced in {@link #leftValue}, as well as any referenced in the
     * {@link BinaryPredicate#getRightValue()} values of our {@link #predicates}. (Only {@link BinaryPredicate} supports the
     * use of variables on the right-hand side of the equation)
     */
    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        StringBuilder sb = new StringBuilder(leftValue == null ? "" : leftValue);
        for (Predicate predicate : predicates) {
            if (predicate instanceof BinaryPredicate) {
                BinaryPredicate bp = (BinaryPredicate) predicate;
                String rv = bp.getRightValue();
                if (rv != null) sb.append(rv);
            }
        }
        return Syntax.getReferencedNames(sb.toString());
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

    private boolean check() {
        if (predicates == null || predicates.length == 0) {
            predicates = new Predicate[] { new BinaryPredicate() };
            return true;
        } else
            return predicates.length == 1 && predicates[0] instanceof BinaryPredicate;
    }

    private BinaryPredicate compat() {
        return ((BinaryPredicate)predicates[0]);
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public String getExpression2() {
        if (check()) return compat().getRightValue();
        return null;
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public void setExpression2(String expression2) {
        if (check()) compat().setRightValue(expression2);
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public ComparisonOperator getOperator() {
        if (check()) return compat().getOperator();
        return null;
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public void setOperator(ComparisonOperator operator) {
        if (check()) compat().setOperator(operator);
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public boolean isNegate() {
        return check() && compat().isNegated();
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public void setNegate(boolean negate) {
        if (check()) compat().setNegated(negate);
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public boolean isCaseSensitive() {
        return check() && compat().isCaseSensitive();
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public void setCaseSensitive(boolean caseSensitive) {
        if (check()) compat().setCaseSensitive(caseSensitive);
    }

    private final static String baseName = "Compare Expression";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ComparisonAssertion>(){
        @Override
        public String getAssertionName( final ComparisonAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuffer name = new StringBuffer(baseName).append(": ");
            name.append(assertion.getExpression1()).append(" ");

            Predicate [] predicatesLocal = assertion.getPredicates();
            for (int i = 0; i < predicatesLocal.length; i++) {
                Predicate pred = predicatesLocal[i];
                name.append(pred.toString());

                if (i == predicatesLocal.length-2)
                    name.append(" and ");
                else if (i < predicatesLocal.length-1)
                    name.append(", ");
            }

            return name.toString();
            
        }
    };
    
    @Override
    public AssertionMetadata meta() {
        clearCachedMetadata(getClass().getName());
        DefaultAssertionMetadata meta = super.defaultMeta();

        // Request to appear in "misc" ("Service Availability") palette folder
        meta.put(PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");
        
        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Evaluate an expression against a series of rules during the runtime processing of a policy.");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

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
        
        return meta;
    }

    @Override
    public Object clone()  {
        final ComparisonAssertion clone = new ComparisonAssertion();
        try {
            clone.setExpression1(getExpression1());

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
}
