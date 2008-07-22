package com.l7tech.util;

/**
 * Mutable character sequence for padding data.
 *
 * <p>This is not intended for use by multiple threads.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class PaddingCharSequence implements CharSequence {

    //- PUBLIC

    /**
     * Create a character sequence with length repetitions of the given character.
     *
     * @param character The character to pad with
     * @param length    The number of padding characters
     */
    public PaddingCharSequence(char character, int length) {
        this.character = character;
        this.length = length;
    }

    /**
     * Set a new length for the character sequence.
     *
     * @param length the new length
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Modify the length of the character sequence.
     *
     * <p>A negative length will reduce the sequence length.</p>
     *
     * @param length the new length
     */
    public void addLength(int length) {
        this.length += length;
    }

    public char charAt(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException(Integer.toString(index));
        return character;
    }

    public int length() {
        return length;
    }

    public CharSequence subSequence(int start, int end) throws IndexOutOfBoundsException {
        if (start < 0 || start > end) throw new IndexOutOfBoundsException(Integer.toString(start));
        if (end > length) throw new IndexOutOfBoundsException(Integer.toString(start));
        return new PaddingCharSequence(character, end-start);
    }

    public String toString() {
        if (sb == null) {
            sb = new StringBuffer();
        }
        sb.setLength(0);
        sb.ensureCapacity(length);
        sb.append(this);
        return sb.toString();
    }

    //- PRIVATE

    private final char character;
    private StringBuffer sb;
    private int length;
}
