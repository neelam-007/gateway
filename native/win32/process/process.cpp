/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 * @file process.cpp
 * @author rmak
 *
 * Compatibility: Windows 2000, Windows XP, Windows Server 2003
 * Limitation: ASCII only.
 */

// -------------------------------------------------------- System Include Files

#define WIN32_LEAN_AND_MEAN 1   // Because we are not using MFC.

#include <iostream>     // Declares std::cout.
#include <ostream>      // Declares std::endl.
#include <sstream>      // Declares std::ostringstream.
#include <string>       // Declares std:string.
#include <string.h>     // Declares strchr, strcmp, strlen, strncmp.
#include <vector>       // Declares std::vector.

#include <windows.h>
#include <psapi.h>      // Declares process status API.

using namespace std;


//-------------------------------------------------------------- Local Constants

/** Default value for unavailable info fields. */
const char * const DEFAULT_NULL_VALUE = "unknown";

/** Available info fields. */
typedef enum
{
    FIELD_PROCESS_ID = 0,
    FIELD_EXE_BASE,
    FIELD_EXE_FULL
} Field;

/** Names corresponding to each ::Field member; for command line options. */
const char * const FIELD_NAMES[] =
{
    "processId",
    "exeBase",
    "exeFull"
};

/** Number of elments in ::FIELD_NAMES. */
const int NUM_FIELD_NAMES = sizeof( FIELD_NAMES ) / sizeof( FIELD_NAMES[ 0 ] );

/** Application exit codes. */
enum ExitCode
{
    EC_OK               = 0,    ///< Success.
    EC_CMD_LINE_OPTION  = 1,    ///< Command line option error.
    EC_CMD_LINE_ARGS    = 2,    ///< Command line argument error.
    EC_FAIL             = 3     ///< Processing failed.
};


// ------------------------------------------------------ Local Type Definitions

/** A ProcessInfo contains strings of info fields for one process. */
typedef vector<string> ProcessInfo;


/**
 * Returns the error message corresponding to a Win32 error code.
 */
const string getWin32ErrorMessage( const DWORD errorCode )
{
    ostringstream msg;

    char * buf = NULL;
    if ( ::FormatMessage( FORMAT_MESSAGE_ALLOCATE_BUFFER |
                          FORMAT_MESSAGE_FROM_SYSTEM |
                          FORMAT_MESSAGE_IGNORE_INSERTS,
                          NULL,
                          errorCode,
                          MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
                          (LPTSTR) & buf,
                          0,
                          NULL ) )
    {
        const size_t len = strlen( buf );
        if ( buf[ len - 2 ] == 0x0D && buf[ len - 1 ] == 0x0A )
        {
            // Removes CR-LF at end.
            buf[ len - 2 ] = 0;
            buf[ len - 1 ] = 0;
        }
        msg << buf << " (Win32 error code = " << errorCode << ")";
    }
    else
    {
        msg << "No error description. (Win32 error code = " << errorCode << ")";
    }

    ::LocalFree( buf );

    return msg.str();   // Note: return will call string copy constructor.
} // getWin32ErrorMessage


/**
 * Gets the process info of a single process.
 */
void getProcessInfo
(
    const DWORD           processId,    ///< [in] process ID
    const vector<Field> & fields,       ///< [in] fields
    ProcessInfo &         processInfo,  ///< [out] process info
    const char *          nullValue     ///< [in] string to use for unknown field values
)
{
    if ( nullValue == NULL )
    {
        nullValue = "";
    }

    processInfo.resize( fields.size() );
    for ( unsigned int i = 0; i < processInfo.size(); ++ i )
    {
        processInfo[ i ] = nullValue;
    }

    HANDLE hProcess = NULL;
    HMODULE hModule = NULL;

    // Gets a handle to the process.
    hProcess = ::OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ,
                              FALSE,
                              processId );
    if ( hProcess != NULL )
    {
        // Gets a handle to the first module of the process.
        DWORD bytesNeeded = 0;
        (void) ::EnumProcessModules( hProcess,
                                     & hModule,
                                     sizeof( hModule ),
                                     & bytesNeeded );
    }

    for ( unsigned int i = 0; i < fields.size(); ++ i )
    {
        switch ( fields[ i ] )
        {
            case FIELD_PROCESS_ID:
            {
                ostringstream oss;
                oss << processId;
                processInfo[ i ] = oss.str();
                break;
            }

            case FIELD_EXE_BASE:
            {
                if ( hProcess != NULL && hModule != NULL )
                {
                    char baseName[ MAX_PATH ];
                    if ( ::GetModuleBaseName( hProcess,
                                              hModule,
                                              baseName,
                                              sizeof( baseName ) ) )
                    {
                        processInfo[ i ] = baseName;
                    }
                }
                break;
            }

            case FIELD_EXE_FULL:
            {
                if ( hProcess != NULL && hModule != NULL )
                {
                    char fullPath[ MAX_PATH ];
                    if ( ::GetModuleFileNameEx( hProcess,
                                                hModule,
                                                fullPath,
                                                sizeof( fullPath ) ) )
                    {
                        processInfo[ i ] = fullPath;
                    }
                }
                break;
            }

            default:
            {
                if ( hProcess != NULL )
                {
                    ::CloseHandle( hProcess );
                }

                ostringstream oss;
                oss << "Internal Error: switch case not handled: " << fields[ i ];
                throw oss.str();
                break;
            }
        }
    }

    if ( hProcess != NULL )
    {
        ::CloseHandle( hProcess );
    }
} // getProcessInfo


/**
 * Gets the process info of multiple processes.
 */
void getMultipleProcessInfo
(
    vector<DWORD> &       processIds,   ///< [in/out] process IDs to query; if specified
                                        ///<      with no elements, then all running
                                        ///<      processes will be queried and this
                                        ///<      vector will be populated with those
                                        ///<      process IDs
    const vector<Field> & fields,       ///< [in] fields to query
    vector<ProcessInfo> & processInfos, ///< [out] process infos with queried fields
    const char * const    nullValue     ///< [in] string to use for unavailable field
                                        ///<      values; NULL implies empty string
)
{
    if ( processIds.size() == 0 )
    {
        // Gets ID of all processes.
        DWORD numProcesses = 0;
        DWORD ids[ 1024 ];
        DWORD numBytesReturned;
        if ( ::EnumProcesses( ids, sizeof( ids ), & numBytesReturned ) == 0 )
        {
            throw getWin32ErrorMessage( ::GetLastError() );
        }

        // Calculates how many process IDs were returned.
        numProcesses = numBytesReturned / sizeof( DWORD );

        processIds.resize( numProcesses );
        for ( unsigned int i = 0; i < numProcesses; ++ i )
        {
            processIds[ i ] = ids[ i ];
        }
    }

    processInfos.resize( processIds.size() );
    for ( unsigned int i = 0; i < processIds.size(); ++ i )
    {
        getProcessInfo( processIds[ i ], fields, processInfos[ i ], nullValue );
    }
} // getMultipleProcessInfo


static void printUsage()
{
    //       0        1         2         3         4         5         6         7         8
    //       12345678901234567890123456789012345678901234567890123456789012345678901234567890
    cout << "Copyright (C) 2005 Layer 7 Technologies Inc." << endl
         << "Purpose: Prints process info." << endl
         << "Usage  : process [options] [process ID]..." << endl
         << "         If process ID(s) are not specified, then all processes are printed." << endl
         << "Options: --fields=field[,field]..." << endl
         << "                     Prints the specified fields instead of default fields." << endl
         << "                     Valid fields are:" << endl
         << "                         processId  process ID" << endl
         << "                         exeBase    basename of process executable" << endl
         << "                         exeFull    full path of process executable" << endl
         << "                     (Default: \"procId,exeFull\")" << endl
         << "         --help      Prints this help message." << endl
         << "         --nullValue=string" << endl
         << "                     String to use for unavailable values." << endl
         << "                     (Default: \"" << DEFAULT_NULL_VALUE << "\"" << endl
         << "         --quiet     Don't print error message if failure. Just set exit code." << endl
         << "         --separator=string" << endl
         << "                     Use the specified separator string between fields." << endl
         << "                     (Default: tab)" << endl
         << "Exit   : " << EC_OK << " if success" << endl
         << "         " << EC_CMD_LINE_OPTION << " if error with command line option" << endl
         << "         " << EC_CMD_LINE_ARGS << " if error with command line argument" << endl
         << "         " << EC_FAIL << " if fail to query for processes" << endl
         ;
} // printUsage


/**
 * Prints process info.
 */
int main( int argc, char * argv[] )
{
    // Sets option defaults.
    bool         quiet = false;
    const char * separator = "\t";
    const char * nullValue = DEFAULT_NULL_VALUE;
    vector<Field>    fields;
    fields.push_back( FIELD_PROCESS_ID );
    fields.push_back( FIELD_EXE_FULL );

    // Parses command line options.
    int iArg = 1;
    while (    iArg < argc
            && strncmp( argv[ iArg ], "--", 2 ) == 0 )
    {
        const char * const option       = argv[ iArg ] + 2;         // Option name.
        size_t             optionLength = strlen( option );         // Length of option name.
        const char *       optionValue  = strchr( option, '=' );    // Option value, if any.
        if ( optionValue != NULL )
        {
            optionLength = optionValue - option;
            ++ optionValue;     // Option value actually starts after the = character.
        }

        if ( strncmp( option, "fields", optionLength ) == 0 )
        {
            if ( optionValue == NULL )
            {
                cout << "!!Missing value for option --" << option << endl;
                return EC_CMD_LINE_OPTION;
            }

            // Parses the comma-separated list of fields.
            fields.clear();
            const char * fieldStart = optionValue;
            while ( fieldStart[ 0 ] != '\0' )
            {
                size_t fieldLength;
                char * comma = strchr( fieldStart, ',' );
                if ( comma == NULL )
                {
                    fieldLength = strlen( fieldStart );
                }
                else
                {
                    fieldLength = comma - fieldStart;
                }

                if ( fieldLength != 0 )
                {
                    int found = -1;
                    for ( int i = 0; i < NUM_FIELD_NAMES; ++ i )
                    {
                        if ( strncmp( fieldStart, FIELD_NAMES[ i ], fieldLength ) == 0 )
                        {
                            found = i;
                        }
                    }
                    if ( found == -1 )
                    {
                        cout << "!!Unknown field: " << fieldStart << endl;
                        return EC_CMD_LINE_OPTION;
                    }
                    fields.push_back( (Field) found );
                }

                fieldStart += fieldLength;
                if ( comma != NULL )
                {
                    ++ fieldStart;
                }
            }

            if ( fields.size() == 0 )
            {
                cout << "!!Must specify at least one field." << endl;
                return EC_CMD_LINE_OPTION;
            }
        }
        else
        if ( strcmp( option, "help" ) == 0 )
        {
            printUsage();
            return EC_OK;
        }
        else
        if ( strncmp( option, "nullValue", optionLength ) == 0 )
        {
            if ( optionValue == NULL )
            {
                cout << "!!Missing value for option --" << option << endl;
                return EC_CMD_LINE_OPTION;
            }
            nullValue = optionValue;
        }
        else
        if ( strcmp( option, "quiet" ) == 0 )
        {
            quiet = true;
        }
        else
        if ( strncmp( option, "separator", optionLength ) == 0 )
        {
            if ( optionValue == NULL )
            {
                cout << "!!Missing value for option --" << option << endl;
                return EC_CMD_LINE_OPTION;
            }

            if ( strlen( optionValue ) == 0 )
            {
                cout << "!!Field separator string cannot be empty." << endl;
                return EC_CMD_LINE_OPTION;
            }
            separator = optionValue;
        }
        else
        {
            cout << "!!Unknown option: --" << option << endl;
            printUsage();
            return EC_CMD_LINE_OPTION;
        }
        ++ iArg;
    }

    // Parses command line arguments.
    // const int numArg = argc - iArg;
    vector<DWORD> processIds;
    for( ; iArg < argc; ++ iArg )
    {
        int id;
        if ( sscanf( argv[ iArg ], "%d", & id ) != 1 || id > 0xffff )
        {
            cout << "!!Invalid process ID: " << argv[ iArg ] << endl;
            return EC_CMD_LINE_ARGS;
        }
        processIds.push_back( id );
    }

    vector<ProcessInfo> processInfos;
    try
    {
        getMultipleProcessInfo( processIds, fields, processInfos, nullValue );
    }
    catch ( const string e )
    {
        if ( ! quiet )
        {
            cout << "!!" << e << endl;
        }
        return EC_FAIL;
    }

    // Prints out the result.
    for ( unsigned int i = 0; i < processInfos.size(); ++ i )
    {
        const ProcessInfo & processInfo = processInfos[ i ];
        for ( unsigned int j = 0; j < processInfo.size(); ++ j )
        {
            const string & value = processInfo[ j ];
            if ( j != 0 )
            {
                cout << separator;
            }
            cout << value;
        }
        cout << endl;
    }

    return EC_OK;
} // main
