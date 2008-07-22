package com.l7tech.skunkworks.xml;

import java.util.Random;

/**
 * Represents an XML stanza, terminated by a newline.
 */
class XmlStanzaStringSource implements StringSource {
    private final long randSeed;
    private final long nestingCount;

    private Random rand;
    private boolean opening;
    private char c;
    private long depth;

    public XmlStanzaStringSource(long randSeed, long nestingCount) {
        this.randSeed = randSeed;
        this.nestingCount = nestingCount;

        reset();
    }

    public String next() {
        String ret;
        if (opening) {
            if (depth > nestingCount) {
                opening = false;
                ret = "" + c--;
                if (c < 'A') c = 'Z';
            } else {
                char attr1 = randChar(rand);
                char attr2 = (char)(attr1 + 1);
                if (attr2 > 'Z') attr2 = 'A';
                ret = "<" + c++ + " " + attr1 + "=\"" + randChar(rand) + "\" " + attr2 + "=\"" + randChar(rand) + "\"" + ">";
                if (c > 'Z') c = 'A';
                depth++;
            }
        } else {
            if (depth == 0) {
                depth--;
                return "\n";
            }
            if (depth < 0)
                return null;
            depth--;
            ret = "</" + c-- + ">";
            if (c < 'A') c = 'Z';
        }

        return ret;
    }

    private char randChar(Random rand) {
        return (char)('A' + rand.nextInt(26));
    }

    public void reset() {
        rand = new Random(randSeed);
        c = randChar(rand);
        opening = true;
        depth = 0;
    }
}
