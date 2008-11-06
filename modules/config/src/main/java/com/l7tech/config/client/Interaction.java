package com.l7tech.config.client;

import com.l7tech.config.client.options.OptionType;
import com.l7tech.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Pattern;

/**
 * Base class for user interactions.
 * 
 * @author steve
 */
public abstract class Interaction implements Closeable {

    //- PUBLIC
    
    /**
     * Perform user interaction.
     * 
     * @return true on success
     * @throws java.io.IOException if an IO error occurs
     */
    public abstract boolean doInteraction() throws IOException;
  
    /**
     * Close this interaction and any underlying resources.
     * 
     * @throws java.io.IOException
     */
    public void close() throws IOException {
        ResourceUtils.closeQuietly(reader);
        ResourceUtils.closeQuietly(writer);
    }        
    
    //- PROTECTED
    
    protected final Console console;
    protected final BufferedReader reader;
    protected final PrintWriter writer;
    
    protected Interaction() {
        this( System.console(),
              new InputStreamReader( System.in ){@Override public void close() throws IOException {}},
              new OutputStreamWriter( System.out ){@Override public void close() throws IOException {}} );
    }
    
    protected Interaction( final Console console,
                           final Reader reader,
                           final Writer writer ) {
        this.console = console;
        this.reader = ensureBuffered(reader);
        this.writer = ensurePrinter(writer);      
    }
    
    protected void println() {
        writer.println();
        writer.flush();
    }
    
    protected void println( final String text ) {
        writer.println( text );
        writer.flush();
    }
    
    protected void print( final String text ) {
        writer.print( text );
        writer.flush();
    }
    
    protected void print( final Number number ) {
        writer.print( number.toString() );
        writer.flush();
    }
    
    protected void promptContinue() throws IOException {
        print( "Press [Enter] to continue.");
        exitOnQuit(fallbackReadLine( console, reader ));                
    }
    
    protected boolean promptConfirm( final String text, final boolean defaultValue ) throws IOException {   
        boolean confirmed = false;
        boolean entered = false;
        
        while ( !entered ) {
            print(text);
            print(" [");
            print( defaultValue ? "Yes" : "No" );
            print("]: ");
            
            String read = fallbackReadLine( console, reader );
            exitOnQuit(read);             
            if ( Pattern.matches(OptionType.BOOLEAN.getDefaultRegex(), read) ) {
                entered = true;
                confirmed = 
                     read.toLowerCase().startsWith("t") || 
                     read.toLowerCase().startsWith("y");
            } else if ( read.trim().isEmpty() ) {
                entered = true;
                confirmed = defaultValue;
            } else {
                println("Please enter Yes or No.");
                println();
            }
        }
         
        return confirmed;
    }
    
    protected boolean handleInput( final String inputText ) {
        boolean handled = false;
        
        if ( !handled ) {
            handled = exitOnQuit( inputText );
        }
        
        return handled;
    }
    
    protected boolean exitOnQuit(final String perhapsQuit) {
        boolean handled = false;
        
        if ("quit".equals(perhapsQuit)) {
            handled = true;
            doQuit();
        }
        
        return handled;
    }

    protected void doQuit() {
        System.exit(1);        
    }
    
    protected String fallbackReadLine(final Console console, final BufferedReader reader) throws IOException {
        String line;

        if (console != null) {
            line = console.readLine();
        } else {
            line = reader.readLine();
        }

        handleInput(line);

        return line;
    }

    protected String fallbackReadPassword(final Console console, final BufferedReader reader) throws IOException {
        String line;
        if (console != null) {
            line = new String(console.readPassword());
        } else {
            line = reader.readLine();
        }

        handleInput(line);

        return line;
    } 
    
    //- PRIVATE
    
    private BufferedReader ensureBuffered( final Reader reader ) {
        BufferedReader bufferedReader = null;
        
        if ( reader != null ) {
            if ( reader instanceof BufferedReader ) {
                bufferedReader = (BufferedReader) reader;
            } else {
                bufferedReader = new BufferedReader( reader );
            }
        }
        
        return bufferedReader;
    }

    private PrintWriter ensurePrinter( final Writer writer ) {
        PrintWriter printWriter = null;
        
        if ( writer instanceof PrintWriter ) {
            printWriter = (PrintWriter) writer;
        } else {
            printWriter = new PrintWriter( writer );
        }
        
        return printWriter;
    }      
}
