package com.l7tech.util;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.*;
import java.nio.channels.FileLock;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;

/**
 * Utility class for working with resources.
 *
 * <p>This is intended to contain helper methods for tasks such as closing
 * io streams, jdbc objects (statements, connections) or working with resource
 * bundles, etc.</p>
 *
 * <p>You would not normally create instances of this class.</p>
 *
 * @author steve
 */
public final class ResourceUtils {

    //- PUBLIC

    /**
     * Close a ResultSet without throwing any exceptions.
     *
     * <p>Note that the exception may still be logged.</p>
     *
     * @param resultSet the result set to close (may be null)
     */
    public static void closeQuietly(final ResultSet resultSet) {
        if(resultSet!=null) {
            try {
                resultSet.close();
            }
            catch(SQLException se) {
                logger.log(Level.INFO, "SQL error when closing result set '"+message(se)+"'", debug(se));
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected error when closing result set.", e);
            }
        }
    }

    /**
     * Close a Statement without throwing any exceptions.
     *
     * <p>Note that the exception may still be logged.</p>
     *
     * @param statement the statement to close (may be null)
     */
    public static void closeQuietly(final Statement statement) {
        if(statement!=null) {
            try {
                statement.close();
            }
            catch(SQLException se) {
                logger.log(Level.INFO, "SQL error when closing statement '"+message(se)+"'", debug(se));
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected error when closing statement.", e);
            }
        }
    }

    /**
     * Close a SQL connection without throwing any exceptions.
     *
     * <p>Note that the exception may still be logged.</p>
     *
     * @param connection the Connection to close (may be null)
     */
     public static void closeQuietly(final Connection connection) {
        if(connection!=null) {
            try {
                connection.close();
            }
            catch(SQLException se) {
                logger.log(Level.INFO, "SQL error when closing connection '"+message(se)+"'", debug(se));
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected error when closing connection.", e);
            }
        }
    }

    /**
     * Close a {@link java.io.Closeable} without throwing any exceptions.
     *
     * @param closeable the object to close.
     */
    public static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch(IOException ioe) {
                logger.log(Level.INFO, "IO error when closing closeable '"+message(ioe)+"'", debug(ioe));
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Unexpected error when closing object", e);
            }
        }
    }

    /**
     * Close one or more {@link java.io.Closeable}s without throwing any exceptions.
     *
     * @param closeables the object(s) to close.
     */
    public static void closeQuietly(java.io.Closeable... closeables) {
        for (java.io.Closeable closeable : closeables) {
            closeQuietly(closeable);
        }
    }

    public static void closeQuietly(Context context) {
        if (context == null) return;

        try {
            context.close();
        } catch (NamingException e) {
            logger.log(Level.INFO, "JNDI error when closing JNDI Context '"+message(e)+"'", debug(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when closing JNDI Context", e);
        }
    }

    public static void closeQuietly(NamingEnumeration answer) {
        if (answer == null) return;

        try {
            answer.close();
        } catch (NamingException e) {
            logger.log(Level.INFO, "JNDI error when closing JNDI NamingEnumeration '"+message(e)+"'", debug(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when closing JNDI NamingEnumeration", e);
        }
    }

    public static void closeQuietly(FileLock lock) {
        if (lock == null) return;

        try {
            lock.release();
        } catch (IOException e) {
            logger.log(Level.INFO, "IO error when releasing FileLock '"+message(e)+"'", debug(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when releasing FileLock.", e);
        }
    }

    public static void closeQuietly(Socket socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException e) {
            logger.log(Level.INFO, "IOException when closing Socket '"+message(e)+"'", debug(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when closing Socket", e);
        }
    }

    public static void closeQuietly(ZipFile zipFile) {
        if (zipFile == null) return;
        try {
            zipFile.close();
        } catch (IOException e) {
            logger.log(Level.INFO, "IOException when closing ZipFile '"+message(e)+"'", debug(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when closing ZipFile", e);
        }
    }

    public static void closeQuietly( final XMLStreamReader reader ) {
        if (reader == null) return;
        try {
            reader.close();
        } catch ( XMLStreamException e) {
            logger.log(Level.INFO, "XMLStreamException when closing XMLStreamReader '"+message(e)+"'", debug(e));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when closing XMLStreamReader", e);
        }
    }

    /**
     * Dispose the given items if Disposable.
     *
     * <p>Null items are ignored.</p>
     *
     * @param possiblyDisposables The items to dispose (may be null)
     */
    public static void dispose( final Object... possiblyDisposables ) {
        if ( possiblyDisposables != null ) {
            for ( final Object possiblyDisposable : possiblyDisposables ) {
                if ( possiblyDisposable instanceof Disposable ) {
                    try {
                        ((Disposable)possiblyDisposable).dispose();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unexpected error when disposing", e);
                    }
                }
            }
        }
    }

    /**
     * Dispose the given items if Disposable.
     * 
     * <p>Null items are ignored.</p>
     *
     * @param possiblyDisposables The items to dispose (may be null)
     */
    public static void dispose( final Iterable possiblyDisposables ) {
        if ( possiblyDisposables != null ) {
            for ( final Object possiblyDisposable : possiblyDisposables ) {
                if ( possiblyDisposable instanceof Disposable ) {
                    try {
                        ((Disposable)possiblyDisposable).dispose();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unexpected error when disposing", e);
                    }
                }
            }
        }
    }

    /**
     * Check if the given URLs address the same resource.
     *
     * <p>Currently this checks for equal URLs which does a basic equality check
     * using resolved host names, ignoring user info and including default port
     * numbers.</p>
     *
     * @param url1 The URL of the first resource (required)
     * @param url2 The URL of the second resource (required)
     * @return true if the URLs are the same resource
     */
    public static boolean isSameResource( final String url1, final String url2 ) {
        boolean same;

        try {
            same = new URL(url1).equals( new URL(url2) );
        } catch (MalformedURLException e) {
            same = false;
        }

        return same;
    }

    /**
     * Get the password authentication from the given url (if any)
     *
     * @param authUrl The URL to extract authentication information from (required)
     * @return The PasswordAuthentication or null if not found
     */
    public static PasswordAuthentication getPasswordAuthentication( final String authUrl ) {
        PasswordAuthentication auth = null;

        try {
            URL url = new URL( authUrl );
            String userInfo = url.getUserInfo();
            if ( userInfo != null && userInfo.indexOf(':') > -1 ) {
                String login = userInfo.substring(0, userInfo.indexOf(':'));
                String passwd = userInfo.substring(userInfo.indexOf(':')+1, userInfo.length());
                auth = new PasswordAuthentication( login, passwd.toCharArray() );
            }
        } catch (MalformedURLException e) {
            // no creds
        }

        return auth;
    }

    /**
     * Add password authentication to the given URL.
     *
     * @param url The url to process (required)
     * @param auth The authentication information (may be null)
     * @return The url with authentication information
     */
    public static String addPasswordAuthentication( final String url,
                                                    final PasswordAuthentication auth ) {
        String urlWithAuth = url;

        if ( auth != null ) {
            Pattern pattern = Pattern.compile( REGEX_HTTP_URL );
            Matcher matcher = pattern.matcher( url );
            if ( matcher.find() ) {
                MatchResult result = matcher.toMatchResult();
                StringBuilder resultBuilder = new StringBuilder();
                resultBuilder.append( url.substring( 0, result.start(1) ));
                resultBuilder.append( auth.getUserName() );
                resultBuilder.append( ':' );
                resultBuilder.append( auth.getPassword() );
                resultBuilder.append( '@' );
                resultBuilder.append( url.substring( result.end(1), url.length() ));
                urlWithAuth = resultBuilder.toString();
            }
        }

        return urlWithAuth;
    }

    /**
     * Relativize one URL against another.
     *
     * <p>Similar to URI.relativize but it relativizes more aggressively.</p>
     *
     * @param baseUri The base URI to relativize against (required)
     * @param uri The reference URI (required)
     * @return The relativized URI (which may be the given reference URI)
     */
    public static URI relativizeUri( final URI baseUri, final URI uri ) {
        int dirs = 0;
        String relativePath = "";

        if ( isPathProcessableUri( baseUri ) &&
             isPathProcessableUri( uri ) &&
             baseUri.getScheme().equals( uri.getScheme() ) &&
             ((baseUri.getRawUserInfo()==null && uri.getRawUserInfo()==null) || (baseUri.getRawUserInfo()!=null && baseUri.getRawUserInfo().equals( uri.getRawUserInfo() ))) &&
             ((baseUri.getHost()==null && uri.getHost()==null) || (baseUri.getHost()!=null && baseUri.getHost().equals( uri.getHost() ) ) ) &&
             baseUri.getPort() == uri.getPort()) {
            final String basePath = baseUri.getRawPath();
            final String path = uri.getRawPath();

            int dirIndex = basePath.lastIndexOf( '/' );
            while ( dirIndex >= 0 && !path.startsWith(basePath.substring( 0, dirIndex+1 )) && (dirIndex = basePath.lastIndexOf( '/', dirIndex-1 )) >= 0 ) {
                dirs++;
                relativePath += "../";
            }
        }

        return URI.create( relativePath + getBaseUri( baseUri, dirs ).relativize( uri ).toString() );
    }

    //- PRIVATE

    /**
     * The logger for the class
     */
    private static final Logger logger = Logger.getLogger(ResourceUtils.class.getName());

    private static final String REGEX_HTTP_URL = "^[hH][tT][tT][pP][sS]?://([\\p{Graph} ]{1,255}@|)[a-zA-Z0-9\\._-]{1,255}[/:]";

    /**
     * Trim the last path component for a file/http/https uri
     *
     * <p>>The returned URI is suitable for use with <code>relativize</code></p>
     */
    private static URI getBaseUri( final URI uri,
                           final int dirStripCount ) {
        URI baseUri = uri;

        if ( isPathProcessableUri( uri ) ) {
            String path = uri.getRawPath();

            int index = path.length()+1;
            for ( int i=0; i<=dirStripCount; i++ ) {
                if ( index < 0 ) break;
                index = path.lastIndexOf('/', index-1);
            }

            if ( index >= 0) {
                try {
                    baseUri = new URI( uri.getScheme(), uri.getRawUserInfo(), uri.getHost(), uri.getPort(), uri.getRawPath().substring( 0, index ), null, null );
                } catch ( URISyntaxException e ) {
                    logger.log( Level.WARNING, "Error generating base URI", e );
                }
            }
        }

        return baseUri;
    }

    private static boolean isPathProcessableUri( final URI uri ) {
        return uri.isAbsolute() && uri.getRawPath() != null &&
                ( uri.getScheme().equalsIgnoreCase( "file" ) ||
                  uri.getScheme().equalsIgnoreCase( "http" ) ||
                  uri.getScheme().equalsIgnoreCase( "https" ) );
    }

    private static String message( final Throwable throwable ) {
        return ExceptionUtils.getMessage( throwable );
    }

    private static Throwable debug( final Throwable throwable ) {
        return ExceptionUtils.getDebugException( throwable );
    }
}
