/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 * @file killproc.cpp
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
#include <string.h>     // Declares strchr, strcmp, stricmp, strlen, strncmp.

#include <windows.h>
#include <psapi.h>      // Declares process status API.

using namespace std;


//-------------------------------------------------------------- Local Constants

/** Number of seconds to wait for process to end by itself before kill. */
const int DEFAULT_WAIT_MILLIS = 0;

/** Application exit codes. */
enum ExitCode
{
    EC_OK               = 0,    ///< Success.
    EC_CMD_LINE_OPTION  = 1,    ///< Command line option error.
    EC_CMD_LINE_ARGS    = 2,    ///< Command line argument error.
    EC_FAIL             = 3     ///< Processing failed.
};


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
 * Find a process by matching its executable path.
 *
 * @return handle of process matched, which must be closed by caller using
 *         CloseHandle when done; NULL if process not found
 * @throw std::string with error description if failure to enumerate
 *        processes in system
 */
HANDLE getProcessByExePath
(
    const string & pattern,     ///< [in]
    const bool     ignoreCase,  ///< [in] whether to ignore case when matching
                                ///<      executable path
    string &       exeFull      ///< [out] the full executable path matched
)
{
    // Gets ID of all processes.
    DWORD numProcesses = 0;
    DWORD ids[ 1024 ];
    DWORD numBytesReturned;
    if ( ::EnumProcesses( ids, sizeof( ids ), & numBytesReturned ) == 0 )
    {
        // This is serious enough to throw an exception.
        ostringstream oss;
        oss << "Cannot enumerate processes in system: "
            << getWin32ErrorMessage( ::GetLastError() );
        throw oss.str();
    }

    // Calculates how many process IDs were returned.
    numProcesses = numBytesReturned / sizeof( DWORD );

    // Find by matching executable path.
    bool found = false;
    HANDLE hProcess = NULL;
    for ( unsigned int i = 0; i < numProcesses && ! found; ++ i )
    {
        // Gets a handle to the process.
        hProcess = ::OpenProcess( PROCESS_QUERY_INFORMATION |
                                  PROCESS_VM_READ |
                                  PROCESS_TERMINATE |
                                  SYNCHRONIZE,
                                  FALSE,
                                  ids[ i ] );
        if ( hProcess != NULL )
        {
            // Gets a handle to the first module of the process.
            DWORD bytesNeeded = 0;
            HMODULE hModule = NULL;
            if ( ::EnumProcessModules( hProcess,
                                       & hModule,
                                       sizeof( hModule ),
                                       & bytesNeeded ) )
            {
                // Gets the process executable full path.
                char fullPath[ MAX_PATH ];
                if ( ::GetModuleFileNameEx( hProcess,
                                            hModule,
                                            fullPath,
                                            sizeof( fullPath ) ) )
                {
                    if ( ignoreCase )
                    {
                        found = ::stricmp( pattern.c_str(), fullPath ) == 0;
                    }
                    else
                    {
                        found = pattern.compare( fullPath ) == 0;
                    }

                    if ( found )
                    {
                        exeFull = fullPath;
                        break;      // Note: This will leave the handle unclosed.
                    }
                }
            }

            ::CloseHandle( hProcess );
        }
    }

    return found ? hProcess : NULL;
} // getProcessByExePath


static void printUsage()
{
    //       0        1         2         3         4         5         6         7         8
    //       12345678901234567890123456789012345678901234567890123456789012345678901234567890
    cout << "Copyright (C) 2005 Layer 7 Technologies Inc." << endl
         << "Purpose: Kill a process by matching its executable path." << endl
         << "         If there are multiple matches, only the first one found will be killed." << endl
         << "Usage  : process [options] exePath" << endl
         << "Options: --ignoreCase" << endl
         << "                     Perform case-insensitive match on the path." << endl
         << "                     (Default: case-sensitive)" << endl
         << "         --help      Prints this help message." << endl
         << "         --quiet     Don't print error message if failure. Just set exit code." << endl
         << "         --wait=millis" << endl
         << "                     Wait specified number of milliseconds for process to end" << endl
         << "                     by itself before kill." << endl
         << "                     (Default: " << DEFAULT_WAIT_MILLIS << ")" << endl
         << "Exit   : " << EC_OK << " if success" << endl
         << "         " << EC_CMD_LINE_OPTION << " if error with command line option" << endl
         << "         " << EC_CMD_LINE_ARGS << " if error with command line argument" << endl
         << "         " << EC_FAIL << " if fail to query for processes" << endl
         ;
} // printUsage


/**
 * Kill a process by matching its executable path.
 */
int main( int argc, char * argv[] )
{
    // Sets option defaults.
    bool ignoreCase = false;
    bool quiet      = false;
    int  waitMillis = DEFAULT_WAIT_MILLIS;

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


        if ( strcmp( option, "ignoreCase" ) == 0 )
        {
            ignoreCase = true;
        }
        else
        if ( strcmp( option, "help" ) == 0 )
        {
            printUsage();
            return EC_OK;
        }
        else
        if ( strcmp( option, "quiet" ) == 0 )
        {
            quiet = true;
        }
        else
        if ( strncmp( option, "wait", optionLength ) == 0 )
        {
            if ( optionValue == NULL )
            {
                cout << "!!Missing value for option --" << option << endl;
                return EC_CMD_LINE_OPTION;
            }
            if ( sscanf( optionValue, "%d", & waitMillis ) != 1 )
            {
                cout << "!!Invalid wait time: --" << option << endl;
                return EC_CMD_LINE_OPTION;
            }
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
    const int numArg = argc - iArg;
    if ( numArg != 1 )
    {
        printUsage();
        return EC_CMD_LINE_ARGS;
    }
    const string exePattern = argv[ iArg ];

    HANDLE hProcess = NULL;
    string exeFull;
    try
    {
        hProcess = getProcessByExePath( exePattern, ignoreCase, exeFull );
    }
    catch ( const string & e )
    {
        if ( ! quiet )
        {
            cout << "!!" << e << endl;
        }
        return EC_FAIL;
    }

    if ( hProcess == NULL )
    {
        if ( ! quiet )
        {
            cout << "!!Process not found: " << exePattern << endl;
        }
        return EC_FAIL;
    }
    else
    {
        if ( ::WaitForSingleObject( hProcess, waitMillis ) == WAIT_OBJECT_0 )
        {
            if ( ! quiet )
            {
                cout << "Process ended before kill: " << exeFull << endl;
            }
        }
        else
        {
            if ( ::TerminateProcess( hProcess, 0 ) == 0 )
            {
                if ( ! quiet )
                {
                    cout << "!!Cannot terminate process: "
                         << getWin32ErrorMessage( ::GetLastError() ) << endl;
                    ::CloseHandle( hProcess );
                    return EC_FAIL;
                }
            }
            else
            {
                if ( ! quiet )
                {
                    cout << "Killed process: " << exeFull << endl;
                }
            }
        }
        ::CloseHandle( hProcess );
    }

    return EC_OK;
} // main
