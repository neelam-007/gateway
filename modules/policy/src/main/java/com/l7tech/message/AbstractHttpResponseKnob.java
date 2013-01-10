/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.message;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Pair;

import java.util.*;

/**
 * An abstract skeleton of an HttpResponseKnob implementation.
 */
public abstract class AbstractHttpResponseKnob implements HttpResponseKnob {
    public static final int CUSTOM_ORDER = 3;
    public static final String CHALLENGE_ORDER_PROP = "io.httpChallengeOrder";

    protected enum ChallengeMode{
        WINDOWS,REVERSE
    }

    protected final OutboundHeaderSupport headerSupport = new OutboundHeaderSupport();

    protected static final Map<String, Integer> supportedChallenges;

    protected ChallengeMode challengeOrder = getChallengeMode(ConfigFactory.getProperty(AbstractHttpResponseKnob.CHALLENGE_ORDER_PROP, "windows"));

    protected ChallengeMode getChallengeMode(String challengeOrder) {
        ChallengeMode mode = ChallengeMode.WINDOWS;
        if(challengeOrder != null && challengeOrder.length() > 0)  {
            try{
               mode = ChallengeMode.valueOf(challengeOrder.toUpperCase());
            }
            catch(IllegalArgumentException iae) {
                // if for whatever reason we can't get the proper setting switch to default
                mode = ChallengeMode.WINDOWS;
            }
        }
        return mode;
    }

    static{
        Map<String, Integer> challengeMap = new HashMap<String, Integer>();
        challengeMap.put("Anonymous", 0);
        challengeMap.put("Basic", 1);
        challengeMap.put("Digest", 2);
        challengeMap.put("NTLM", 4);
        challengeMap.put("Negotiate", 8);
        challengeMap.put("Windows Live ID", 16);
        supportedChallenges = Collections.unmodifiableMap(challengeMap);
    }

    protected final List<Pair<String,Integer>> challengesToSend = new ArrayList<Pair<String, Integer>>();
    protected int statusToSet;

    @Override
    public void setDateHeader( final String name, final long date ) {
        headerSupport.setDateHeader( name, date );
    }

    @Override
    public void addDateHeader( final String name, final long date ) {
        headerSupport.addDateHeader( name, date );
    }

    @Override
    public void setHeader( final String name, final String value ) {
        headerSupport.setHeader( name, value );
    }

    @Override
    public void addHeader( final String name, final String value ) {
        headerSupport.addHeader( name, value );
    }

    @Override
    public String[] getHeaderValues( final String name ) {
        return headerSupport.getHeaderValues( name );
    }

    @Override
    public String[] getHeaderNames() {
        return headerSupport.getHeaderNames( );
    }

    @Override
    public boolean containsHeader( final String name ) {
        return headerSupport.containsHeader( name );
    }

    @Override
    public void removeHeader( final String name ) {
        headerSupport.removeHeader( name );
    }

    @Override
    public void removeHeader(final String name, final Object value) {
        headerSupport.removeHeader( name, value );
    }

    @Override
    public void clearHeaders() {
        headerSupport.clearHeaders();
    }

    @Override
    public void writeHeaders( final GenericHttpRequestParams target ) {
        headerSupport.writeHeaders( target );
    }

    @Override
    public void addChallenge(String value) {
        Pair<String, Integer> pair = Pair.pair(value, supportedChallenges.containsKey(value.trim())? supportedChallenges.get(value.trim()) : CUSTOM_ORDER);
        challengesToSend.add(pair);
    }

    @Override
    public void setStatus(int code) {
        statusToSet = code;
    }

    @Override
    public int getStatus() {
        return statusToSet;
    }

    protected List<Pair<String, Object>> getHeadersToSend() {
        return headerSupport.headersToSend;
    }

    protected static class ChallengeComparator implements Comparator<Pair<String, Integer>> {

        private ChallengeMode mode;

        public ChallengeComparator(ChallengeMode mode) {
            this.mode = mode;
        }

        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.<p>
         * <p/>
         * In the foregoing description, the notation
         * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
         * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
         * <tt>0</tt>, or <tt>1</tt> according to whether the value of
         * <i>expression</i> is negative, zero or positive.<p>
         * <p/>
         * The implementor must ensure that <tt>sgn(compare(x, y)) ==
         * -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
         * implies that <tt>compare(x, y)</tt> must throw an exception if and only
         * if <tt>compare(y, x)</tt> throws an exception.)<p>
         * <p/>
         * The implementor must also ensure that the relation is transitive:
         * <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> implies
         * <tt>compare(x, z)&gt;0</tt>.<p>
         * <p/>
         * Finally, the implementor must ensure that <tt>compare(x, y)==0</tt>
         * implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all
         * <tt>z</tt>.<p>
         * <p/>
         * It is generally the case, but <i>not</i> strictly required that
         * <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking,
         * any comparator that violates this condition should clearly indicate
         * this fact.  The recommended language is "Note: this comparator
         * imposes orderings that are inconsistent with equals."
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         *         first argument is less than, equal to, or greater than the
         *         second.
         * @throws NullPointerException if an argument is null and this
         *                              comparator does not permit null arguments
         * @throws ClassCastException   if the arguments' types prevent them from
         *                              being compared by this comparator.
         */
        @Override
        public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
            switch (mode) {
                case REVERSE:
                    if(o1 == null || o1.left == null) {
                        if(o2 != null && o2.left != null)
                            return 1;
                        else
                            return 0;
                    }
                    else if(o2 == null || o2.left == null) {
                        return -1;
                    }
                    return o2.left.compareToIgnoreCase(o1.left);
                case WINDOWS:
                default:
                    if(o1 == null || o1.right == null) {
                        if(o2 != null && o2.right != null)
                            return 1;
                        else
                            return 0;
                    }
                    else if(o2 == null || o2.right == null) {
                        return -1;
                    }

                    return o2.right - o1.right;
            }

        }

    }

}
