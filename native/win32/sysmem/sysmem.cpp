/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 *
 * @file sysmem.cpp
 * @author rmak
 *
 * Compatibility: Windows 2000, Windows XP, Windows Server 2003
 */

// -------------------------------------------------------- System Include Files

#define WIN32_LEAN_AND_MEAN 1   // Because we are not using MFC.

#include <iostream>     // Declares std::cout.
#include <ostream>      // Declares std::endl.
#include <sstream>      // Declares std::ostringstream.
#include <string>       // Declares std:string.

#include <windows.h>

using namespace std;


//-------------------------------------------------------------- Local Constants

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


const string formatQuotient
(
	const unsigned long numerator,
	const unsigned long denominator,
	const bool			roundoff
)
{
	ostringstream oss;
	if ( numerator % denominator == 0 )
	{
		oss << numerator / denominator;
	}
	else
	{
		double result = (double) numerator / (double) denominator;
		if ( roundoff )
		{
			oss << (unsigned long)( result + 0.5 );
		}
		else
		{
			oss << fixed << noshowpoint << result;
		}
	}
	return oss.str();
}


static void printUsage()
{
    //       0        1         2         3         4         5         6         7         8
    //       12345678901234567890123456789012345678901234567890123456789012345678901234567890
    cout << "Copyright (C) 2006 Layer 7 Technologies Inc." << endl
         << "Purpose: Prints system memory information." << endl
         << "Usage  : sysmem [<option>]... <what>" << endl
		 << "Options: --help" << endl
		 << "             Prints this help message." << endl
		 << "         --roundoff" << endl
		 << "             Round to nearest integer." << endl
		 << "         --unit=K|M|G" << endl
         << "             Size unit (kilobytes, megabytes or gigabytes). (Default: bytes)" << endl
		 << "<what> : TotalPhys" << endl
		 << "             Total size of physical memory." << endl
		 << "         AvailPhys" << endl
		 << "             Size of physical memory available." << endl
		 << "         TotalPageFile" << endl
		 << "             Size of the committed memory limit. This is physical memory plus the size of the page file, minus a small overhead." << endl
		 << "         AvailPageFile" << endl
		 << "             Size of available memory to commit. The limit is TotalPageFile." << endl
         << "Exit   : " << EC_OK << " if success" << endl
         << "         " << EC_CMD_LINE_OPTION << " if error with command line option" << endl
         << "         " << EC_CMD_LINE_ARGS << " if error with command line argument" << endl
         << "         " << EC_FAIL << " if fail to query for processes" << endl
         ;
} // printUsage


int main( int argc, char * argv[] )
{
    // Sets option defaults.
    bool roundoff = false;
	unsigned long sizeFactor = 1UL;

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


        if ( strcmp( option, "help" ) == 0 )
        {
            printUsage();
            return EC_OK;
        }
        else
        if ( strcmp( option, "roundoff" ) == 0 )
        {
            roundoff = true;
        }
        else
        if ( strncmp( option, "unit", optionLength ) == 0 )
        {
            if ( optionValue == NULL )
            {
                cout << "!!Missing value for option --" << option << endl;
                return EC_CMD_LINE_OPTION;
            }
			else
            if ( strcmp( optionValue, "K" ) == 0 )
			{
				sizeFactor = 1024UL;
			}
			else
            if ( strcmp( optionValue, "M" ) == 0 )
			{
				sizeFactor = 1024UL * 1024UL;
			}
			else
            if ( strcmp( optionValue, "G" ) == 0 )
			{
				sizeFactor = 1024UL * 1024UL * 1024UL;
			}
			else
            {
                cout << "!!Invalid value for option --" << option << endl;
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
    const string what = argv[ iArg ];

	::MEMORYSTATUSEX memoryStatusEx;
	memoryStatusEx.dwLength = sizeof( memoryStatusEx );
	if ( ::GlobalMemoryStatusEx( & memoryStatusEx ) == 0 )
	{
		cout << "!!" << getWin32ErrorMessage( ::GetLastError() );
		return EC_FAIL;
	}

	if ( what.compare( "TotalPhys" ) == 0 )
	{
		cout << formatQuotient( memoryStatusEx.ullTotalPhys, sizeFactor, roundoff ) << endl;
	}
	else
	if ( what.compare( "AvailPhys" ) == 0 )
	{
		cout << formatQuotient( memoryStatusEx.ullAvailPhys, sizeFactor, roundoff ) << endl;
	}
	else
	if ( what.compare( "TotalPageFile" ) == 0 )
	{
		cout << formatQuotient( memoryStatusEx.ullTotalPageFile, sizeFactor, roundoff ) << endl;
	}
	else
	if ( what.compare( "AvailPageFile" ) == 0 )
	{
		cout << formatQuotient( memoryStatusEx.ullAvailPageFile, sizeFactor, roundoff ) << endl;
	}
	else
	{
		cout << "Unknown memory item: " << what << endl;
		return EC_CMD_LINE_ARGS;
	}

    return EC_OK;
} // main
