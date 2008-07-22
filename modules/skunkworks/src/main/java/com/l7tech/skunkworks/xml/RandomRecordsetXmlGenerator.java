package com.l7tech.skunkworks.xml;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

/**
 * @author jbufu
 */
public class RandomRecordsetXmlGenerator
{
    private static Random random = new Random();

    private long targetSize = 0x0000000080000000L; // 2gb
//    private long targetSize = 0x0000000006400000L; // 100m
//    private long targetSize = 0x0000000060000000L;

    private int maxNesting = 10;
    private int maxChilds = 30; // for non-root nodes
    private int maxAttributes = 7;

    private int maxAttrLen = 15;
    private RandomListSource attrNameSource;

    private int maxAttrValueLen = 30;
    private StringSource attrValueSource;

    private int maxElemNameLen = 15;
    private RandomListSource elemNameSource;

    private int maxElemLen = 1000;
    private StringSource elemSource;

    private long size = 0;
    private PrintStream out = System.out;

    public RandomRecordsetXmlGenerator()
    {
        attrNameSource = new RandomListSource(random.nextLong(), maxAttributes, maxAttrLen);
        attrValueSource = new RandomWordSource(random.nextLong(), maxAttrValueLen);

        elemNameSource = new RandomListSource(random.nextLong(), maxChilds, maxElemNameLen);
        elemSource = new RandomWordSource(random.nextLong(), maxElemLen);
    }

    public void generate()
    {
        size = 0;

        print("<document>");

        while (size < targetSize)
            element(0);

        print("\n</document>");

        out.flush();
        out.close();
    }

    private void element(int nesting)
    {
        if (size >= targetSize || nesting >= maxNesting)
            return;

        String name = elemNameSource.next();
        print("\n<" + name);
        attributes(nesting);
        print(">");

        if (random.nextBoolean())
            print("\n" + elemSource.next());

        int childs = random.nextInt(maxChilds);
        for (int c=0; c < childs; c++)
            element(nesting + 1);

        print("\n</" + name + ">");
    }

    private void print(String s)
    {
        size += s.length();
        out.print(s);
    }

    private void attributes(int nesting)
    {
        int attrs = random.nextInt(maxAttributes);

        if (attrs > 0)
            print(" level=\"" + nesting + "\"");

        List<String> attributes = attrNameSource.getSubList(attrs -1);

        for (String attr : attributes)
            print(" " + attr + "=\"" + attrValueSource.next() + "\"");
    }

    public static void main(String[] args) throws FileNotFoundException
    {
        RandomRecordsetXmlGenerator lxg = new RandomRecordsetXmlGenerator();

        if (args != null && args.length > 0 && args[0].length() > 0)
            lxg.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(args[0]))));

        lxg.generate();
    }

    private void setOut(PrintStream printStream)
    {
        out = printStream;
    }

    public class RandomListSource
    {
        Random random;
        int listLength;
        String[] listSource;

        public RandomListSource(long seed, int length, int maxWordLength)
        {
            random = new Random(seed);
            listLength = length;
            listSource = new String[listLength];

            StringSource randomWords = new RandomWordSource(random.nextLong(), maxWordLength);
            for (int i = 0; i < listLength; i++)
                listSource[i] = randomWords.next(); // should enforce uniqueness
        }

        /**
         * Returns a sub-list of unique elements.
         */
        public List<String> getSubList(int length)
        {
            List<String> result = new ArrayList<String>();

            String temp;
            for (int i = 0; i < length; i++)
            {
                int index = random.nextInt(listLength - i);
                result.add(listSource[index]);

                // swap current <-> position after maxRandom above
                temp = listSource[index];
                listSource[index] = listSource[listLength - i - 1];
                listSource[listLength - i - 1] = temp;
            }

            return result;
        }

        public String next()
        {
            return listSource[random.nextInt(listLength)];
        }
    }
}
