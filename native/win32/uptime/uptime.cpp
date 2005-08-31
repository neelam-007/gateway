/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 * @file uptime.cpp
 * @author rmak
 *
 * Compatibility: Windows 2000, Windows XP, Windows Server 2003
 */

#define WIN32_LEAN_AND_MEAN 1   // Because we are not using MFC.

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pdh.h>
#include <pdhmsg.h>


/**
 * Data structure to hold PDH error info for display purpose.
 */
typedef struct
{
    PDH_STATUS      errorCode;      ///< the error code value
    const char *    name;           ///< same as the enum name
    const char *    description;    ///< extracted from pdhmsg.h
} PdhError;

/**
 * Database of PDH error info extracted from pdhmsg.h.
 * Using our own hardcode English descriptions instead of using
 * FormatMessage because that returns non-PDH specific descriptions.
 */
static PdhError PDH_ERRORS[] =
{
    { PDH_CSTATUS_VALID_DATA, "PDH_CSTATUS_VALID_DATA", "The returned data is valid." },
    { PDH_CSTATUS_NEW_DATA, "PDH_CSTATUS_NEW_DATA", "The return data value is valid and different from the last sample." },
    { PDH_CSTATUS_NO_MACHINE, "PDH_CSTATUS_NO_MACHINE", "Unable to connect to specified machine or machine is off line." },
    { PDH_CSTATUS_NO_INSTANCE, "PDH_CSTATUS_NO_INSTANCE", "The specified instance is not present." },
    { PDH_MORE_DATA, "PDH_MORE_DATA", "There is more data to return than would fit in the supplied buffer. Allocate a larger buffer and call the function again." },
    { PDH_CSTATUS_ITEM_NOT_VALIDATED, "PDH_CSTATUS_ITEM_NOT_VALIDATED", "The data item has been added to the query, but has not been validated nor accessed. No other status information on this data item is available." },
    { PDH_RETRY, "PDH_RETRY", "The selected operation should be retried." },
    { PDH_NO_DATA, "PDH_NO_DATA", "No data to return." },
    { PDH_CALC_NEGATIVE_DENOMINATOR, "PDH_CALC_NEGATIVE_DENOMINATOR", "A counter with a negative denominator value was detected." },
    { PDH_CALC_NEGATIVE_TIMEBASE, "PDH_CALC_NEGATIVE_TIMEBASE", "A counter with a negative timebase value was detected." },
    { PDH_CALC_NEGATIVE_VALUE, "PDH_CALC_NEGATIVE_VALUE", "A counter with a negative value was detected." },
    { PDH_DIALOG_CANCELLED, "PDH_DIALOG_CANCELLED", "The user cancelled the dialog box." },
    { PDH_END_OF_LOG_FILE, "PDH_END_OF_LOG_FILE", "The end of the log file was reached." },
    { PDH_ASYNC_QUERY_TIMEOUT, "PDH_ASYNC_QUERY_TIMEOUT", "Time out while waiting for asynchronous counter collection thread to end." },
    { PDH_CANNOT_SET_DEFAULT_REALTIME_DATASOURCE, "PDH_CANNOT_SET_DEFAULT_REALTIME_DATASOURCE", "Cannot change default real-time data source. There are real-time query sessions collecting counter data." },
    { PDH_CSTATUS_NO_OBJECT, "PDH_CSTATUS_NO_OBJECT", "The specified object is not found on the system." },
    { PDH_CSTATUS_NO_COUNTER, "PDH_CSTATUS_NO_COUNTER", "The specified counter could not be found." },
    { PDH_CSTATUS_INVALID_DATA, "PDH_CSTATUS_INVALID_DATA", "The returned data is not valid." },
    { PDH_MEMORY_ALLOCATION_FAILURE, "PDH_MEMORY_ALLOCATION_FAILURE", "A PDH function could not allocate enough temporary memory to complete the operation. Close some applications or extend the pagefile and retry the function." },
    { PDH_INVALID_HANDLE, "PDH_INVALID_HANDLE", "The handle is not a valid PDH object." },
    { PDH_INVALID_ARGUMENT, "PDH_INVALID_ARGUMENT", "A required argument is missing or incorrect." },
    { PDH_FUNCTION_NOT_FOUND, "PDH_FUNCTION_NOT_FOUND", "Unable to find the specified function." },
    { PDH_CSTATUS_NO_COUNTERNAME, "PDH_CSTATUS_NO_COUNTERNAME", "No counter was specified." },
    { PDH_CSTATUS_BAD_COUNTERNAME, "PDH_CSTATUS_BAD_COUNTERNAME", "Unable to parse the counter path. Check the format and syntax of the specified path." },
    { PDH_INVALID_BUFFER, "PDH_INVALID_BUFFER", "The buffer passed by the caller is invalid." },
    { PDH_INSUFFICIENT_BUFFER, "PDH_INSUFFICIENT_BUFFER", "The requested data is larger than the buffer supplied. Unable to return the requested data." },
    { PDH_CANNOT_CONNECT_MACHINE, "PDH_CANNOT_CONNECT_MACHINE", "Unable to connect to the requested machine." },
    { PDH_INVALID_PATH, "PDH_INVALID_PATH", "The specified counter path could not be interpreted." },
    { PDH_INVALID_INSTANCE, "PDH_INVALID_INSTANCE", "The instance name could not be read from the specified counter path." },
    { PDH_INVALID_DATA, "PDH_INVALID_DATA", "The data is not valid." },
    { PDH_NO_DIALOG_DATA, "PDH_NO_DIALOG_DATA", "The dialog box data block was missing or invalid." },
    { PDH_CANNOT_READ_NAME_STRINGS, "PDH_CANNOT_READ_NAME_STRINGS", "Unable to read the counter and/or explain text from the specified machine." },
    { PDH_LOG_FILE_CREATE_ERROR, "PDH_LOG_FILE_CREATE_ERROR", "Unable to create the specified log file." },
    { PDH_LOG_FILE_OPEN_ERROR, "PDH_LOG_FILE_OPEN_ERROR", "Unable to open the specified log file." },
    { PDH_LOG_TYPE_NOT_FOUND, "PDH_LOG_TYPE_NOT_FOUND", "The specified log file type has not been installed on this system." },
    { PDH_NO_MORE_DATA, "PDH_NO_MORE_DATA", "No more data is available." },
    { PDH_ENTRY_NOT_IN_LOG_FILE, "PDH_ENTRY_NOT_IN_LOG_FILE", "The specified record was not found in the log file." },
    { PDH_DATA_SOURCE_IS_LOG_FILE, "PDH_DATA_SOURCE_IS_LOG_FILE", "The specified data source is a log file." },
    { PDH_DATA_SOURCE_IS_REAL_TIME, "PDH_DATA_SOURCE_IS_REAL_TIME", "The specified data source is the current activity." },
    { PDH_UNABLE_READ_LOG_HEADER, "PDH_UNABLE_READ_LOG_HEADER", "The log file header could not be read." },
    { PDH_FILE_NOT_FOUND, "PDH_FILE_NOT_FOUND", "Unable to find the specified file." },
    { PDH_FILE_ALREADY_EXISTS, "PDH_FILE_ALREADY_EXISTS", "There is already a file with the specified file name." },
    { PDH_NOT_IMPLEMENTED, "PDH_NOT_IMPLEMENTED", "The function referenced has not been implemented." },
    { PDH_STRING_NOT_FOUND, "PDH_STRING_NOT_FOUND", "Unable to find the specified string in the list of performance name and explain text strings." },
    { PDH_UNABLE_MAP_NAME_FILES, "PDH_UNABLE_MAP_NAME_FILES", "Unable to map to the performance counter name data files. The data will be read from the registry and stored locally." },
    { PDH_UNKNOWN_LOG_FORMAT, "PDH_UNKNOWN_LOG_FORMAT", "The format of the specified log file is not recognized by the PDH DLL." },
    { PDH_UNKNOWN_LOGSVC_COMMAND, "PDH_UNKNOWN_LOGSVC_COMMAND", "The specified Log Service command value is not recognized." },
    { PDH_LOGSVC_QUERY_NOT_FOUND, "PDH_LOGSVC_QUERY_NOT_FOUND", "The specified Query from the Log Service could not be found or could not be opened." },
    { PDH_LOGSVC_NOT_OPENED, "PDH_LOGSVC_NOT_OPENED", "The Performance Data Log Service key could not be opened. This may be due to insufficient privilege or because the service has not been installed." },
    { PDH_WBEM_ERROR, "PDH_WBEM_ERROR", "An error occurred while accessing the WBEM data store." },
    { PDH_ACCESS_DENIED, "PDH_ACCESS_DENIED", "Unable to access the desired machine or service. Check the permissions and authentication of the log service or the interactive user session against those on the machine or service being monitored." },
    { PDH_LOG_FILE_TOO_SMALL, "PDH_LOG_FILE_TOO_SMALL", "The maximum log file size specified is too small to log the selected counters. No data will be recorded in this log file. Specify a smaller set of counters to log or a larger file size and retry this call." },
    { PDH_INVALID_DATASOURCE, "PDH_INVALID_DATASOURCE", "Cannot connect to ODBC DataSource Name." },
    { PDH_INVALID_SQLDB, "PDH_INVALID_SQLDB", "SQL Database does not contain a valid set of tables for Perfmon, use PdhCreateSQLTables." },
    { PDH_NO_COUNTERS, "PDH_NO_COUNTERS", "No counters were found for this Perfmon SQL Log Set." },
    { PDH_SQL_ALLOC_FAILED, "PDH_SQL_ALLOC_FAILED", "Call to SQLAllocStmt failed with %1." },
    { PDH_SQL_ALLOCCON_FAILED, "PDH_SQL_ALLOCCON_FAILED", "Call to SQLAllocConnect failed with %1." },
    { PDH_SQL_EXEC_DIRECT_FAILED, "PDH_SQL_EXEC_DIRECT_FAILED", "Call to SQLExecDirect failed with %1." },
    { PDH_SQL_FETCH_FAILED, "PDH_SQL_FETCH_FAILED", "Call to SQLFetch failed with %1." },
    { PDH_SQL_ROWCOUNT_FAILED, "PDH_SQL_ROWCOUNT_FAILED", "Call to SQLRowCount failed with %1." },
    { PDH_SQL_MORE_RESULTS_FAILED, "PDH_SQL_MORE_RESULTS_FAILED", "Call to SQLMoreResults failed with %1." },
    { PDH_SQL_CONNECT_FAILED, "PDH_SQL_CONNECT_FAILED", "Call to SQLConnect failed with %1." },
    { PDH_SQL_BIND_FAILED, "PDH_SQL_BIND_FAILED", "Call to SQLBindCol failed with %1." },
    { PDH_CANNOT_CONNECT_WMI_SERVER, "PDH_CANNOT_CONNECT_WMI_SERVER", "Unable to connect to the WMI server on requested machine." },
    { PDH_PLA_COLLECTION_ALREADY_RUNNING, "PDH_PLA_COLLECTION_ALREADY_RUNNING", "Collection \"%1!s!\" is already running." },
    { PDH_PLA_ERROR_SCHEDULE_OVERLAP, "PDH_PLA_ERROR_SCHEDULE_OVERLAP", "The specified start time is after the end time." },
    { PDH_PLA_COLLECTION_NOT_FOUND, "PDH_PLA_COLLECTION_NOT_FOUND", "Collection \"%1!s!\" does not exist." },
    { PDH_PLA_ERROR_SCHEDULE_ELAPSED, "PDH_PLA_ERROR_SCHEDULE_ELAPSED", "The specified end time has already elapsed." },
    { PDH_PLA_ERROR_NOSTART, "PDH_PLA_ERROR_NOSTART", "Collection \"%1!s!\" did not start, check the application event log for any errors." },
    { PDH_PLA_ERROR_ALREADY_EXISTS, "PDH_PLA_ERROR_ALREADY_EXISTS", "Collection \"%1!s!\" already exists." },
    { PDH_PLA_ERROR_TYPE_MISMATCH, "PDH_PLA_ERROR_TYPE_MISMATCH", "There is a mismatch in the settings type." },
    { PDH_PLA_ERROR_FILEPATH, "PDH_PLA_ERROR_FILEPATH", "The information specified does not resolve to a valid path name." },
    { PDH_PLA_SERVICE_ERROR, "PDH_PLA_SERVICE_ERROR", "The \"Performance Logs & Alerts\" service did not respond." },
    { PDH_PLA_VALIDATION_ERROR, "PDH_PLA_VALIDATION_ERROR", "The information passed is not valid." },
    { PDH_PLA_VALIDATION_WARNING, "PDH_PLA_VALIDATION_WARNING", "The information passed is not valid." },
    { PDH_PLA_ERROR_NAME_TOO_LONG, "PDH_PLA_ERROR_NAME_TOO_LONG", "The name supplied is too long." },
    { PDH_INVALID_SQL_LOG_FORMAT, "PDH_INVALID_SQL_LOG_FORMAT", "SQL log format is incorrect. Correct format is \"SQL:<DSN-name>!<LogSet-Name>\"." },
    { PDH_COUNTER_ALREADY_IN_QUERY, "PDH_COUNTER_ALREADY_IN_QUERY", "Performance counter in PdhAddCounter() call has already been added in the performance query. This counter is ignored." },
    { PDH_BINARY_LOG_CORRUPT, "PDH_BINARY_LOG_CORRUPT", "Unable to read counter information and data from input binary log files." },
    { PDH_LOG_SAMPLE_TOO_SMALL, "PDH_LOG_SAMPLE_TOO_SMALL", "At least one of the input binary log files contain fewer than two data samples." },
    { PDH_OS_LATER_VERSION, "PDH_OS_LATER_VERSION", "The version of the operating system on the computer named %1 is later than that on the local computer. This operation is not available from the local computer." },
    { PDH_OS_EARLIER_VERSION, "PDH_OS_EARLIER_VERSION", "%1 supports %2 or later. Check the operating system version on the computer named %3." },
    { PDH_INCORRECT_APPEND_TIME, "PDH_INCORRECT_APPEND_TIME", "The output file must contain earlier data than the file to be appended." },
    { PDH_UNMATCHED_APPEND_COUNTER, "PDH_UNMATCHED_APPEND_COUNTER", "Both files must have identical counters in order to append." },
    { PDH_SQL_ALTER_DETAIL_FAILED, "PDH_SQL_ALTER_DETAIL_FAILED", "Cannot alter CounterDetail table layout in SQL database." },
    { PDH_QUERY_PERF_DATA_TIMEOUT, "PDH_QUERY_PERF_DATA_TIMEOUT", "System is busy. Timeout when collecting counter data. Please retry later or increase \"CollectTime\" registry value." }
};

/** Number of elements in PDH_ERRORS[]. */
static int NUM_PDH_ERRORS = sizeof( PDH_ERRORS ) / sizeof( PdhError );


static const char * const getPdhErrorCodeName( PDH_STATUS errorCode )
{
    for ( int i = 0; i < NUM_PDH_ERRORS; ++ i )
    {
        if ( errorCode == PDH_ERRORS[ i ].errorCode )
        {
            return PDH_ERRORS[ i ].name;
        }
    }

    return NULL;
}


static const char * const getPdhErrorCodeDescription( PDH_STATUS errorCode )
{
    for ( int i = 0; i < NUM_PDH_ERRORS; ++ i )
    {
        if ( errorCode == PDH_ERRORS[ i ].errorCode )
        {
            return PDH_ERRORS[ i ].description;
        }
    }

    return NULL;
}


/**
 * Gets the system uptime in seconds.
 *
 * This method uses the Windows SDK performance data helper (PDH) functions.
 * They are more complicated than GetTickCount(). But GetTickCount() returns
 * the number of milliseconds in a DWORD which wraps around every 49.7 days.
 *
 * @return ERROR_SUCCESS if success; otherwise a Windows system error code
 *         or a PDH error code.
 */
PDH_STATUS getSystemUptimeSeconds
(
    const char * const  hostname,
                        ///< [in] name of remote machine to query; use NULL or
                        ///<      empty string for local machine
    LONGLONG *          systemUptimeSeconds,
                        ///< [out] system uptime in seconds
    const char **       errorMessage
                        ///< [out] a message string to supplement the return
                        ///<       code when failure; NULL if return code is ERROR_SUCCESS
)
{
    // Creates and initializes a unique PDH query structure.
    PDH_HQUERY hQuery = NULL;
    PDH_STATUS pdhStatus = PdhOpenQuery( NULL,  // Reserved. Must be NULL.
                                         0,     // User-defined value.
                                         & hQuery );
    if ( pdhStatus != ERROR_SUCCESS )
    {
        * errorMessage = "Failed to create PDH query structure.";
        goto cleanup;
    }

    // Creates the counter and adds it to the query.
    PDH_HCOUNTER hCounter = NULL;
    const char * const LOCALHOST_COUNTER_PATH = "\\System\\System Up Time";
    if ( hostname == NULL || strlen( hostname ) == 0 )
    {
        pdhStatus = PdhAddCounter( hQuery,
                                   LOCALHOST_COUNTER_PATH,
                                   0,               // User-defined value.
                                   & hCounter );
    }
    else
    {
        const size_t length = 2 + strlen( hostname ) + strlen( LOCALHOST_COUNTER_PATH );
        char * counterPath = (char *) calloc( length + 1, sizeof( char ) );
        strcpy( counterPath, "\\\\" );
        strcat( counterPath, hostname );
        strcat( counterPath, LOCALHOST_COUNTER_PATH );
        pdhStatus = PdhAddCounter( hQuery,
                                   counterPath,  // Counter path.
                                   0,               // User-defined value.
                                   & hCounter );
        free( counterPath );
    }
    if ( pdhStatus != ERROR_SUCCESS )
    {
        * errorMessage = "Error while adding counter to PDH query.";
        goto cleanup;
    }

    // Collects the current raw data value for all counters in the query.
    pdhStatus = PdhCollectQueryData( hQuery );
    if ( pdhStatus != ERROR_SUCCESS )
    {
        * errorMessage = "Error collecting PDH query data.";
        goto cleanup;
    }

    // Formats the return counter value.
    PDH_FMT_COUNTERVALUE counterValue;
    pdhStatus = PdhGetFormattedCounterValue( hCounter,
                                             PDH_FMT_LARGE | PDH_FMT_NOSCALE,
                                             NULL,      // Resulting counter type (optional).
                                             & counterValue);
    if ( pdhStatus != ERROR_SUCCESS )
    {
        * errorMessage = "Error formatting PDH counter value";
        goto cleanup;
    }

    * systemUptimeSeconds = counterValue.largeValue;
    * errorMessage = NULL;  // No error so far, thus no message.

cleanup:
    if ( hQuery != NULL )
    {
        PdhCloseQuery( hQuery );
    }

    return pdhStatus;
} // getSystemUptimeSeconds


static void printUsage()
{
    printf(
        "Copyright (C) 2005 Layer 7 Technologies Inc.\n"
        "Purpose: Prints how long the system has been running.\n"
        "Usage  : uptime [options]\n"
        "Options: --cygwin    Prints in Cygwin-like format (default).\n"
        "                     e.g., \" 13:00:25 up 23:11\"\n"
        "                           \" 11:22:20 up 28 days, 18:57\"\n"
        "         --seconds   Prints number of seconds.\n"
        "                     e.g., \"79296\"\n"
        "         --hostname=RemoteHostname\n"
        "                     Query remote machine instead of local machine\n"
        "         --quiet     Don't print error message if failure.\n"
        "Exit   : 0 if success\n"
        "         1 if error with command line option\n"
        "         2 if error with command line argument\n"
        "         3 if fail to query for system up time\n"
    );
} // printUsage


/**
 * Prints how long the system has been running.
 *
 * For reference, Cygwin uptime output are like:
 * <pre>
 *  13:00:25 up 23:11,  1 user,  load average: 0.00, 0.00, 0.00
 *  11:47:39 up 5 days, 18:40,  0 users,  load average: 0.00, 0.00, 0.00
 * </pre>
 */
int main( int argc, char * argv[] )
{
    // Available output format options.
    const char * const OPT_CYGWIN  = "cygwin";
    const char * const OPT_SECONDS = "seconds";

    // Sets default output format.
    const char * outputFormat = OPT_CYGWIN;

    // Sets default hostname to NULL for local machine.
    const char * hostname = NULL;

    // Sets default quiet mode.
    bool quiet = false;

    // Parses command line options.
    int iArg = 1;
    while (    iArg < argc
            && strncmp( argv[ iArg ], "--", 2 ) == 0 )
    {
        const char * const option = argv[ iArg ] + 2;
        if ( strcmp( option, OPT_CYGWIN ) == 0 )
        {
            outputFormat = OPT_CYGWIN;
        }
        else
        if ( strcmp( option, OPT_SECONDS ) == 0 )
        {
            outputFormat = OPT_SECONDS;
        }
        else
        if ( strncmp( option, "hostname=", 9 ) == 0 )
        {
            hostname = option + 9;
        }
        else
        if ( strcmp( option, "quiet" ) == 0 )
        {
            quiet = true;
        }
        else
        {
            printf( "!!Unknown option: --%s\n", option );
            printUsage();
            return 1;
        }
        ++ iArg;
    }

    // Parses command line arguments.
    const int numArg = argc - iArg;
    if ( numArg != 0 )
    {
        printf( "!!Wrong number of arguments (0 expected, %d encountered)\n", numArg );
        printUsage();
        return 2;
    }

    const char * errorMessage = NULL;
    LONGLONG systemUptimeSeconds = 0;
    PDH_STATUS pdhStatus = getSystemUptimeSeconds( hostname,
                                                   & systemUptimeSeconds,
                                                   & errorMessage );
    if ( pdhStatus != ERROR_SUCCESS )
    {
        if ( ! quiet )
        {
            const char * const name        = getPdhErrorCodeName( pdhStatus );
            const char * const description = getPdhErrorCodeDescription( pdhStatus );
            printf( "!!%s (PDH error code = %s [0x%08X]: %s)\n",
                    errorMessage,
                    name == NULL ? "" : name,
                    pdhStatus,
                    description == NULL ? "" : description );
        }
        return 3;
    }

    if ( outputFormat == OPT_CYGWIN )
    {
        // Uses the 64 bit version of time functions so this will work up to year 3000.
        __time64_t now;
        _time64( & now );
        struct tm * nowtm = _localtime64( & now );

        LONGLONG t = systemUptimeSeconds;
        LONGLONG upSecs = t % 60;
        t /= 60;
        LONGLONG upMins = t  % 60;
        t /= 60;
        LONGLONG upHours = t % 24;
        t /= 24;
        LONGLONG upDays = t;

        printf( " %02d:%02d:%02d up ", nowtm->tm_hour, nowtm->tm_min, nowtm->tm_sec );
        if ( upDays > 0 )
        {
            printf( "%I64d days, ", upDays );
        }
        printf( "%02I64d:%02I64d\n", upHours, upMins );
    }
    else
    if ( outputFormat == OPT_SECONDS )
    {
        printf( "%I64d\n", systemUptimeSeconds );
    }

    return 0;
} // main
