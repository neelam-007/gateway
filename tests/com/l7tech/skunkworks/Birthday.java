/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import java.math.BigDecimal;
import java.text.NumberFormat;

/**
 * @author mike
 */
public class Birthday {
    public static void main(String[] args) {
        long[] numbers = new long[] {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 30, 40, 50, 100,
            500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000, 5000000, 10000000
        };
        int[] bits = new int[] { 8, 16, 32, 40, 50, 53 };
        double[] fields = new double[bits.length];
        for (int i = 0; i < bits.length; ++i) fields[i] = Math.pow(2, bits[i]);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(100);
        StringBuffer out = new StringBuffer();
        out.append("||Number");
        for (int i = 0; i < fields.length; ++i) {
            out.append("||" + bits[i] + " bits");
        }
        out.append("\n");
        for (int num = 0; num < numbers.length; ++num) {
            long n = numbers[num];
            out.append("|" + n);
            for (int i = 0; i < fields.length; ++i) {
                out.append("|");
                out.append(nf.format(pbirthday(n, fields[i])));
            }
            out.append("\n");
        }

        System.out.println(out);
        //long n = 10000000;
        //System.out.println("For " + n + " samples in field of " + FIELD + ",");
        //System.out.println("probability of remaining collision-free is " + pbirthday(n, FIELD));
    }

    private static final int NONCE_BITS = 53;
    private static final double FIELD = Math.pow(2, NONCE_BITS);

    /**
     * Compute the probability of encountering a birthday-paradox collision in the specified set.
     * Examples:
     * <p>The probability that 23 people chosen at random (but not including those born on leap days) include
     * at least one pair of people who share the same birthday: pbirthday(23, 365) = 50.73%
     * <p>The probability that the least significant 53 bits of a strong hashing algorithm will have
     * produced a hash collision by coincidence after ten million hashes of different inputs is given by:
     * pbirthday(10000000, Math.pow(2, 53)) = 0.55%
     *
     * @param field the overall field size.  Must be less than or equal to 2**32 or else double runs out of precision
     * @return the probability that N different samples from the given field contain no duplicates
     */
    private static double pbirthday(long n, final double field)
    {
        double f1 = field - 1;
        double f2 = field;
        if (!(f1 < f2 && f2 > f1))
            throw new IllegalArgumentException("field is overflowing double");

        double accum = 1;
        while (n > 1) {
            accum *= (field-(--n))/field;
        }
        return 1.0 - accum;
    }
}
