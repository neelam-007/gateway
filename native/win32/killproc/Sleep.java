/**
 * For testing killproc.
 */
class Sleep
{
    public static void main( String[] args )
        throws InterruptedException
    {
        if ( args.length != 1 )
        {
            System.out.println( "usage: Sleep <millis>" );
            System.exit( 1 );
        }

        long remainingMillis = Long.parseLong( args[ 0 ] );
        while ( remainingMillis > 0 )
        {
            System.out.print( "." );
            long sleepMillis = 1000;
            if ( sleepMillis > remainingMillis )
                sleepMillis = remainingMillis;
            Thread.currentThread().sleep( sleepMillis );
            remainingMillis -= sleepMillis;
        }
    }
}