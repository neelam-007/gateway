package com.l7tech.policy.assertion;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.ConfigFactory.getProperty;
import com.l7tech.util.ExceptionUtils;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Functions.then;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.lower;
import static com.l7tech.util.TextUtils.trim;
import static java.util.Collections.unmodifiableList;

import java.io.Serializable;
import java.util.List;

/**
 * Set of rules for forwarding or backwarding http headers or parameters.
 * There can be multiple rules for the same name. Because you can have multiple
 * headers with the same name, you could forward all originals and a custom value
 * for the same name for example.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 8, 2007<br/>
 */
public class HttpPassthroughRuleSet implements Cloneable, Serializable  {

    public static final int ORIGINAL_PASSTHROUGH = 0;
    public static final int CUSTOM_PASSTHROUGH = 1;
    public static final int CUSTOM_AND_ORIGINAL_PASSTHROUGH = 2;
    public static final int BLOCK = 3;

    private static final String PROP_HEADERS_TO_SKIP = "com.l7tech.policy.assertion.HttpPassthroughRuleSet.headersToSkip";
    private static final String HEADERS_TO_SKIP_DEFAULT =
            "keep-alive, connection, server, content-type, date, content-length, transfer-encoding, content-encoding";
    public static final List<String> HEADERS_NOT_TO_IMPLICITLY_FORWARD =
            unmodifiableList( grep( map( list( getProperty( PROP_HEADERS_TO_SKIP, HEADERS_TO_SKIP_DEFAULT ).split( "\\s*,\\s*" ) ), then( trim(), lower() ) ), isNotEmpty() ) );

    private boolean forwardAll;
    private HttpPassthroughRule[] rules;


    public HttpPassthroughRuleSet() {
        this.forwardAll = false;
        this.rules = new HttpPassthroughRule[]{};
    }

    public HttpPassthroughRuleSet(boolean forwardAll, HttpPassthroughRule[] rules) {
        this.forwardAll = forwardAll;
        this.rules = rules;
        if (rules == null) throw new IllegalArgumentException("don't pass null arrays");
    }

    public boolean isForwardAll() {
        return forwardAll;
    }

    public void setForwardAll(boolean forwardAll) {
        this.forwardAll = forwardAll;
    }

    public HttpPassthroughRule[] getRules() {
        return rules;
    }

    public void setRules(HttpPassthroughRule[] rules) {
        this.rules = rules;
    }

    /**
     * remove a customized header/parameter
     * @param name the name of the header/parameter to remove
     */
    public void remove(String name) {
        if (rules != null) {
            int deleted = 0;
            for (int i = 0; i < rules.length; i++) {
                if (rules[i].getName().compareToIgnoreCase(name) == 0) {
                    rules[i] = null;
                    ++deleted;
                }
            }
            if (deleted > 0) {
                HttpPassthroughRule[] tmp = new HttpPassthroughRule[rules.length-deleted];
                int j = 0;
                for (int i = 0; i < tmp.length; i++) {
                    while (rules[j] == null) j++;
                    tmp[i] = rules[j];
                    j++;
                }
                rules = tmp;
            }
        }
    }

    /**
     * Query the rule of a header/parameter based on its name.
     *
     * @param name the name of the header/parameter
     * @return one of ORIGINAL_PASSTHROUGH, CUSTOM_PASSTHROUGH, CUSTOM_AND_ORIGINAL_PASSTHROUGH or BLOCK
     */
    public int ruleForName(String name) {
        if (forwardAll) {
            return ORIGINAL_PASSTHROUGH;
        }
        if (rules != null) {
            boolean custom = false;
            boolean original = false;
                for (int i = 0; i < rules.length; i++) {
                if (rules[i].getName().compareToIgnoreCase(name) == 0) {
                    if (rules[i].isUsesCustomizedValue()) custom = true;
                    else original = true;
                }
            }
            if (custom && original) {
                return CUSTOM_AND_ORIGINAL_PASSTHROUGH;
            } else if (custom) {
                return CUSTOM_PASSTHROUGH;
            } else if (original) {
                return ORIGINAL_PASSTHROUGH;
            } else {
                return BLOCK;
            }
        }
        return BLOCK;
    }

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    @Override
    public HttpPassthroughRuleSet clone() {
        try {
            HttpPassthroughRuleSet hprs = (HttpPassthroughRuleSet) super.clone();

            hprs.rules = new HttpPassthroughRule[ rules.length ];
            for ( int i=0; i<rules.length; i++ ) {
                hprs.rules[i] = rules[i].clone();
            }

            return hprs;
        } catch (CloneNotSupportedException e) {
            throw ExceptionUtils.wrap(e);
        }
    }
}
