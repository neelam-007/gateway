package com.l7tech.gateway.common;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;

import javax.swing.table.DefaultTableCellRenderer;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: rballantyne
 * Date: 5/20/14
 * Time: 2:36 PM
 *
 * This class implements a Syntax.SyntaxErrorHandler that queues log/audit handling for possible later processing.
 *
 * While processing ExpandVariables Selectors it maybe necessary to wait until later to know if the current process is
 * truly a failure case. This SyntaxErrorHandler stores the potential cases and only submits them for logging/auditing
 * if the ultimately turn out to be actual errors.
 *
 */
public class QueuingSyntaxErrorHandler implements Syntax.SyntaxErrorHandler  {

    private List<Pair<String,String>> suspiciousStrings;
    private List<Triple<Integer,String,Integer>> subscriptsOutOfRange;
    private List<String> badVariables;
    private List<Pair<String,Throwable>> badVariablesWithExceptions;

    private final Syntax.SyntaxErrorHandler handler;

    public QueuingSyntaxErrorHandler(Syntax.SyntaxErrorHandler handler) {
        this.handler = handler;
        suspiciousStrings = new LinkedList<>();
        subscriptsOutOfRange = new LinkedList<>();
        badVariables = new LinkedList<>();
        badVariablesWithExceptions = new LinkedList<>();
    }

    @Override
    public String handleSuspiciousToString(String remainingName, String className) {
        suspiciousStrings.add(new Pair<>(remainingName,className));
        return MessageFormat.format(CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING.getMessage(), remainingName, className);
    }

    @Override
    public String handleSubscriptOutOfRange(int subscript, String remainingName, int length) {
        subscriptsOutOfRange.add(new Triple(subscript,remainingName,length));
        return MessageFormat.format( CommonMessages.TEMPLATE_SUBSCRIPT_OUTOFRANGE.getMessage(), Integer.toString(subscript), remainingName, Integer.toString(length));
    }

    @Override
    public String handleBadVariable(String name) {
        badVariables.add(name);
        return MessageFormat.format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), name);
    }

    @Override
    public String handleBadVariable(String s, Throwable t) {
        badVariablesWithExceptions.add(new Pair<>(s,t));
        return MessageFormat.format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), s, ExceptionUtils.getMessage(t));
    }

    public void flushLogAuditEvents() {
        for ( Pair<String,String> s : suspiciousStrings ) {
            handler.handleSuspiciousToString(s.left,s.right);
        }
        for ( Triple<Integer,String,Integer> s : subscriptsOutOfRange ) {
            handler.handleSubscriptOutOfRange(s.left,s.middle,s.right);
        }
        for ( String s : badVariables ) {
            handler.handleBadVariable(s);
        }
        for ( Pair<String,Throwable> s : badVariablesWithExceptions ) {
            handler.handleBadVariable(s.left,s.right);
        }
    }

}
