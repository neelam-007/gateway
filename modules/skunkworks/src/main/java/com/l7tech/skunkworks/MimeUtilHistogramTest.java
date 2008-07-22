/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks;

import javax.mail.internet.HeaderTokenizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Show histograms of boundary lengths
 */
public class MimeUtilHistogramTest {
    private static Random random = new Random();

    private interface BoundaryMaker {
        byte[] makeBoundary();
    }

    private static class OldBoundaryMaker implements BoundaryMaker {
        public byte[] makeBoundary() {
            StringBuffer bb = new StringBuffer();

            for (int i = 0; i < random.nextInt(5); i++) {
                bb.append('-');
            }

            bb.append("=_");

            for (int i = 0; i < random.nextInt(40) + 1; i++) {
                byte printable = (byte)(random.nextInt(127-32)+32);
                if (HeaderTokenizer.MIME.indexOf(printable) < 0)
                    bb.append(new String(new byte[] { printable }));
            }

            return bb.toString().getBytes();
        }
    }

    private static class NewBoundaryMaker implements BoundaryMaker {
        public byte[] makeBoundary() {
            StringBuffer bb = new StringBuffer();

            final int numDashes = random.nextInt(5);
            for (int i = 0; i < numDashes; i++) {
                bb.append('-');
            }

            bb.append("=_");

            final int numPrintables = random.nextInt(38) + 3;
            for (int i = 0; i < numPrintables; i++) {
                byte printable = (byte)(random.nextInt(127-32)+32);
                if (HeaderTokenizer.MIME.indexOf(printable) < 0)
                    bb.append(new String(new byte[] { printable }));
            }

            return bb.toString().getBytes();
        }
    }

    public static void main(String[] args) {

        System.out.println("\n\nOld boundary maker:\n");
        displayHistogram(getCounts(new OldBoundaryMaker()));

        System.out.println("\n\nNew boundary maker:\n");
        displayHistogram(getCounts(new NewBoundaryMaker()));
    }

    private static Map getCounts(BoundaryMaker boundaryMaker) {
        Map seenLengths = new HashMap();
        for (int i = 0; i < 10000; ++i) {
            int len = boundaryMaker.makeBoundary().length;
            final Integer key = Integer.valueOf(len);
            Integer bcur = (Integer)seenLengths.get(key);
            int cur = bcur == null ? 0 : bcur.intValue();
            seenLengths.put(key, new Integer(cur + 1));
        }
        return seenLengths;
    }

    private static void displayHistogram(Map counts) {
        for (int i = 0; i < 50; ++i) {
            Integer bcount = (Integer)counts.get(Integer.valueOf(i));
            int count = bcount == null ? 0 : bcount.intValue();
            System.out.print(i + "\t" + count + "\t");
            int numStars = count / 10;
            for (int b = 0; b < numStars; ++b) {
                System.out.print("*");
            }
            System.out.println("");
        }
    }
}
