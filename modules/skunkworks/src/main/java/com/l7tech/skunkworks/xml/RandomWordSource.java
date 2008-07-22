package com.l7tech.skunkworks.xml;

import java.util.Random;
import java.io.UnsupportedEncodingException;

/**
 * Generates random words up to a max given length.
 *
 * @author jbufu
 */
public class RandomWordSource implements StringSource
{
    private Random random;
    private int maxWordLen;

    public RandomWordSource(long seed, int length)
    {
        random = new Random(seed);
        maxWordLen = length;
    }

    public String next()
    {
        // length is at least 1
        int length = 1 + random.nextInt(maxWordLen - 1);
        byte[] randomBytes = new byte[length];

        random.nextBytes(randomBytes);

        // start with a letter
        randomBytes[0] = (byte) (97 + Math.abs(randomBytes[0] % 26));

        for (int i=1; i < length; i++)
        {
            byte b = randomBytes[i];

            int c = Math.abs(b % 36);

            if (c < 26) c += 97; // map (0..25) to 'a' .. 'z'
            else c+= (48 - 26);   // map (26..35) to '0'..'9'

            randomBytes[i] = (byte) c;
        }

        try
        {
            return new String(randomBytes, "US-ASCII");
        }
        catch (UnsupportedEncodingException e)
        {
            return null; // shouldn't happen
        }
    }

    public void reset()
    {
    }
}
