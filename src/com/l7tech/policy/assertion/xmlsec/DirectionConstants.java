package com.l7tech.policy.assertion.xmlsec;

/**
 * Interface <code>DirectionConstants</code> contains direction
 * constants in, out, inout that and is implemented by XML assertions.
 * The valies specify in what direction the specific XML assertion
 * should be applied.
 *
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 *
 * todo, remove this
 */
public interface DirectionConstants {
    public static int IN = 1;
    public static int OUT = 2;
    public static int INOUT = IN  | OUT;
}
