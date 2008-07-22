package com.l7tech.skunkworks.xml;


import java.util.Random;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;


/**
 * @author jbufu
 */
public class FullRandomXmlGenerator
{
    private static Random random = new Random();

//    private long targetSize = 0x0000000080000000L; // 2gb
//    private long targetSize = 0x0000000006400000L; // 100m
    private long targetSize = 0x0000000060000000L;
    private int maxNesting = 10;
    private int maxChilds = 20; // for non-root nodes
    private int maxAttributes = 7;

    private int maxAttrLen = 15;
    private StringSource attrSource;

    private int maxElemNameLen = 15;
    private StringSource elemNameSource;

    private int maxElemLen = 100;
    private StringSource elemSource;

    private long size = 0;
    private PrintStream out = System.out;

    public FullRandomXmlGenerator()
    {
        attrSource = new RandomWordSource(random.nextLong(), maxAttrLen);
        elemNameSource = new RandomWordSource(random.nextLong(), maxElemNameLen);
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

        for (int i=1; i<attrs; i++)
            print(" " + attrSource.next() + "_" + i + "=\"" + attrSource.next() + "\"");
    }

    public static void main(String[] args) throws FileNotFoundException
    {
        FullRandomXmlGenerator lxg = new FullRandomXmlGenerator();

        if (args != null && args.length > 0 && args[0].length() > 0)
            lxg.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(args[0]))));

        lxg.generate();
    }

    private void setOut(PrintStream printStream)
    {
        out = printStream;
    }
}
