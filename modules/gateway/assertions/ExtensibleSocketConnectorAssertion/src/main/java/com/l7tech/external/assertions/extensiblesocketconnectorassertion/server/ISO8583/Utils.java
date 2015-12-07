package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 04/06/13
 * Time: 11:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class Utils {

    public static byte[] concatByteArray(byte[] firstArray, byte[] secondArray) {

        if (firstArray == null && secondArray != null)
            return secondArray;
        else if (firstArray != null && secondArray == null)
            return firstArray;
        else if (firstArray == null && secondArray == null)
            return new byte[]{};

        byte[] result = new byte[firstArray.length + secondArray.length];

        System.arraycopy(firstArray, 0, result, 0, firstArray.length);
        System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);

        return result;
    }
}
