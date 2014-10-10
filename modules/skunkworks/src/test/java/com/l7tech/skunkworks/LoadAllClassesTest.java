package com.l7tech.skunkworks;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A test that tries to load every class in the tree.
 */
public class LoadAllClassesTest {

    final Set<String> classnames = new LinkedHashSet<>();

    FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
        final Pattern classRegex = Pattern.compile( "/(com/l7tech/.*)\\.class$" );

        @Override
        public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
            String path = file.toAbsolutePath().toString();

            Matcher m = classRegex.matcher( path );
            if ( m.find() ) {
                String f = m.group( 1 );
                f = f.replace( '/', '.' );
                classnames.add( f );
            }

            return super.visitFile( file, attrs );
        }
    };

    @Test
    @Ignore( "Enable to catalog all com.l7tech class files under the project directory and try to load them as classes" )
    public void loadAllClasses() throws Exception {
        // Scan build/ subdirectory for all files with names containing "/com/l7tech/" and ending in ".class"
        System.out.println( "Finding class files..." );
        Files.walkFileTree( new File( "." ).toPath(), visitor );

        List<String> sorted = new ArrayList<>( classnames );
        Collections.sort( sorted );

        System.out.println( "Found " + sorted.size() + " classes. Attempting to load classes..." );
        ClassLoader cl = getClass().getClassLoader();
        for ( String cn : sorted ) {
            try {
                cl.loadClass( cn );
            } catch ( ClassNotFoundException e ) {
               System.out.println( "NOT FOUND: " + cn );
            }
        }
    }
}
