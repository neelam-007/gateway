package com.l7tech.external.assertions.odatavalidation.server;

import org.apache.olingo.odata2.api.edm.EdmLiteral;
import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.expression.*;

import java.util.*;

/**
 * Created by yuri on 04/07/14.
 */
public class OdataParserUtil {

    private OdataParserUtil() {}

    public static <K, V> String[] map2Array(Map<K, V> map) {
        if(map == null) throw new IllegalArgumentException("Map is null");

        List<String> list = new ArrayList<>();
        for(Map.Entry<K, V> entry : map.entrySet()){
             list.add(entry.toString());
        }
        return list.toArray(new String[list.size()]);
    }

    public static Set<String> getExpressionParts(CommonExpression expression) throws OdataValidationException {
        Set<String> expressions = new HashSet<>();
        if(expression != null) {
            try {
                OdataRequestInfoExpressionVisitor visitor = new OdataRequestInfoExpressionVisitor();
                expression.accept(visitor);
                expressions.addAll(visitor.parts());
            } catch (ExceptionVisitExpression | ODataApplicationException e) {
                throw new OdataValidationException("Invalid Expression", e);
            }
        }

        return expressions;
    }

    private static class OdataRequestInfoExpressionVisitor implements ExpressionVisitor {

        private final Set<String> partsSet = new HashSet<>();
        /**
         * Visits a filter expression
         *
         * @param filterExpression The visited filter expression node
         * @param expressionString The $filter expression string used to build the filter expression tree
         * @param expression       The expression node representing the first <i>operator</i>,<i>method</i>,<i>literal</i> or <i>property</i> of the
         *                         expression tree
         * @return The overall result of evaluating the whole filter expression tree
         */
        @Override
        public Object visitFilterExpression(FilterExpression filterExpression, String expressionString, Object expression) {
            return filterExpression;
        }

        /**
         * Visits a binary expression
         *
         * @param binaryExpression The visited binary expression node
         * @param operator         The operator used in the binary expression
         * @param leftSide         The result of visiting the left expression node
         * @param rightSide        The result of visiting the right expression node
         * @return Returns the result from evaluating operator, leftSide and rightSide
         */
        @Override
        public Object visitBinary(BinaryExpression binaryExpression, BinaryOperator operator, Object leftSide, Object rightSide) {
            partsSet.add(operator.toString());
            return binaryExpression;
        }

        /**
         * Visits a orderby expression
         *
         * @param orderByExpression The visited orderby expression node
         * @param expressionString  The $orderby expression string used to build the orderby expression tree
         * @param orders            The result of visiting the orders of the orderby expression
         * @return The overall result of evaluating the orderby expression tree
         */
        @Override
        public Object visitOrderByExpression(OrderByExpression orderByExpression, String expressionString, List<Object> orders) {
            return orderByExpression;
        }

        /**
         * Visits a order expression
         *
         * @param orderExpression The visited order expression node
         * @param filterResult    The result of visiting the filter expression contained in the order
         * @param sortOrder       The sort order
         * @return The overall result of evaluating the order
         */
        @Override
        public Object visitOrder(OrderExpression orderExpression, Object filterResult, SortOrder sortOrder) {
            partsSet.add(sortOrder.toString());
            return orderExpression;
        }

        /**
         * Visits a literal expression
         *
         * @param literal    The visited literal expression node
         * @param edmLiteral The detected EDM literal (value and type)
         * @return The value of the literal
         */
        @Override
        public Object visitLiteral(LiteralExpression literal, EdmLiteral edmLiteral) {
            partsSet.add(literal.getUriLiteral());
            return literal;
        }

        /**
         * Visits a method expression
         *
         * @param methodExpression The visited method expression node
         * @param method           The method used in the method expression
         * @param parameters       The result of visiting the parameters of the method
         * @return Returns the result from evaluating the method and the method parameters
         */
        @Override
        public Object visitMethod(MethodExpression methodExpression, MethodOperator method, List<Object> parameters) {
            if(method != null) {
                partsSet.add(method.toUriLiteral());
                if(parameters != null) {
                    for(Object param : parameters) {
                        partsSet.add(param != null ? param.toString() : "null");
                    }
                }
            }
            return methodExpression;
        }

        /**
         * Visits a member expression (e.g. <path property>/<member property>)
         *
         * @param memberExpression The visited member expression node
         * @param path             The result of visiting the path property expression node (the left side of the property operator)
         * @param property         The result of visiting the member property expression node
         * @return Returns the <b>value</b> of the corresponding property (which may be a single EDM value or a structured EDM value)
         */
        @Override
        public Object visitMember(MemberExpression memberExpression, Object path, Object property) {
            partsSet.add(path.toString());
            partsSet.add(property.toString());
            return memberExpression;
        }

        /**
         * Visits a property expression
         *
         * @param propertyExpression The visited property expression node
         * @param uriLiteral         The URI literal of the property
         * @param edmProperty        The EDM property matching the property name used in the expression String
         * @return Returns the <b>value</b> of the corresponding property ( which may be a single EDM value or a structured EDM value)
         */
        @Override
        public Object visitProperty(PropertyExpression propertyExpression, String uriLiteral, EdmTyped edmProperty) {
            partsSet.add(uriLiteral);
            return uriLiteral;
        }

        /**
         * Visits a unary expression
         *
         * @param unaryExpression The visited unary expression node
         * @param operator        The operator used in the unary expression
         * @param operand         The result of visiting the operand expression node
         * @return Returns the result from evaluating operator and operand
         */
        @Override
        public Object visitUnary(UnaryExpression unaryExpression, UnaryOperator operator, Object operand) {
            partsSet.add(operator.toUriLiteral());
            return unaryExpression;
        }

        public Set<String> parts() {
            return partsSet;
        }

    }
}
