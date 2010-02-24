package com.l7tech.util;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;

/**
 *
 */
public class CliTest {

    @Test
    public void testSimpleArguments() throws Exception {
        Target target = new Target();
        Cli.process( target, new String[]{ "-string", "value", "-int1", "123", "-int2", "456", "-bool1", "true", "-bool2", "-file", "filename" }, null );
        assertEquals( "-string", "value", target.string );
        assertEquals( "-int1", 123, target.int1 );
        assertEquals( "-int2", Integer.valueOf(456), target.int2 );
        assertEquals( "-bool1", true, target.bool1 );
        assertEquals( "-bool2", false, target.bool2 );
        assertNotNull( "-file", target.file );
        assertEquals( "filename", "filename", target.file.getName() );
    }

    @Test
    public void testAliasArguments() throws Exception {
        Target target = new Target();
        Cli.process( target, new String[]{ "-s", "value" }, null );
        assertEquals( "-string", "value", target.string );
    }

    @Test(expected=Cli.CliException.class)
    public void testMissingArgument() throws Exception {
        Target target = new Target();
        Cli.process( target, new String[]{ }, null );
    }

    @Test(expected=Cli.CliException.class)
    public void testInvalidArgument() throws Exception {
        Target target = new Target();
        Cli.process( target, new String[]{ "-int1", "invalid" }, null );
    }

    @Test(expected=Cli.CliException.class)
    public void testUnknownArgument() throws Exception {
        Target target = new Target();
        Cli.process( target, new String[]{ "eee" }, null );
    }

    @Test
    public void testUsage() throws Exception {
        Target target = new Target();
        StringBuilder builder = new StringBuilder();
        Cli.usage( target, builder );

        String usage = builder.toString();
        assertTrue( "Usage present", usage.contains("Description is multi-line.") );
        assertTrue( "Usage ordered", usage.endsWith("A string argument.\n") );
        assertFalse( "New line processing failed", usage.contains("\nDescription is multi-line.") );
    }

    private static final class Target {
        @Cli.Arg(name={"-string", "-s"}, description="A string argument.")
        private String string;

        @Cli.Arg(name="-int1", description="An optional int argument.\nDescription is multi-line.", required=false)
        private int int1;

        @Cli.Arg(name="-int2", description="An optional int argument", required=false)
        private Integer int2;

        @Cli.Arg(name="-bool1", description="An optional boolean argument", required=false)
        private boolean bool1;

        @Cli.Arg(name="-bool2", description="An optional boolean flag", value="false")
        private Boolean bool2;

        @Cli.Arg(name="-file", description="An optional file argument", required=false)
        private File file;
    }
}
