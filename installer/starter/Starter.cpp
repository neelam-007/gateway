/* $Id$ */

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <string>
#include <iostream>
#include <fstream>

using namespace std;

#define DEFAULT_JRE "jre/bin/javaw.exe"
#define DEFAULT_COMMANDLINE "javaw.exe -jar Program.jar"
#define DEFAULT_CONFIG "l7start.ini"


/** Open the specified file for read.  Returns ifstream, or 0 on error. */
ifstream *openRead(const char *path) 
{
	ifstream *ifs = new ifstream(path);
	if (ifs->is_open())
		return ifs;
	delete ifs;
	return 0;
}

/**
 * Display a generic error message to the user, including
 * the GetLastError() message string in the default language, and then exit.
 */
void exitError() {
	LPVOID lpMsgBuf;
	FormatMessage( 
		FORMAT_MESSAGE_ALLOCATE_BUFFER | 
		FORMAT_MESSAGE_FROM_SYSTEM| 
		FORMAT_MESSAGE_IGNORE_INSERTS,
		NULL,
		GetLastError(),
		MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
		(LPTSTR) &lpMsgBuf,
		0,
		NULL 
	);
	char buf[128];
	_snprintf(buf, 127, "Unable to start the program: %s", lpMsgBuf);
	buf[127] = '\0';

	// Display the string.
	MessageBox( NULL, (LPCTSTR)buf, "Layer 7", MB_OK | MB_ICONINFORMATION );
	// Free the buffer.
	LocalFree( lpMsgBuf );
	exit(1);
}

/**
 * Attempts to open our configuration file.
 * Returns the open file handle, or INVALID_HANDLE_VALUE if we couldn't find a config file.
 */
ifstream *openConfigFile() {
	char fname[MAX_PATH];
	if (GetModuleFileName(0, fname, MAX_PATH) == 0)
		return 0;
	char *ext = strrchr(fname, '.');
	if (ext != 0)
		*ext = '\0';
	ifstream *found;

	{
		string dotini(fname);
		dotini.append(".ini");
		if (0 != (found = openRead(dotini.c_str())))
			return found;
	}

	{
		string startupdotini(fname);
		startupdotini.append("Startup.ini");
		if (0 != (found = openRead(startupdotini.c_str())))
			return found;
	}

	{
		string spacestartupdotini(fname);
		spacestartupdotini.append(" Startup.ini");
		if (0 != (found = openRead(spacestartupdotini.c_str())))
			return found;
	}

	return openRead(DEFAULT_CONFIG);
}

/** Find the JRE path and command line from our config. */
void getConfig(string *jre, string *cmdline) {
	jre->assign(DEFAULT_JRE);
	cmdline->assign(DEFAULT_COMMANDLINE);

	std::ifstream *cfg = openConfigFile();
	if (cfg) {
		std::string line;
		while (getline(*cfg, line, '\n')) {
			if (line.substr(0, 4) == string("jre="))
				jre->assign(line.substr(4));
			else if (line.substr(0, 12) == string("commandline="))
				cmdline->assign(line.substr(12));
		}
		delete cfg;
	}
}


/**
 * A simple wrapper .exe to painlessly start the JVM and some other program.
 * TODO: hang around and watch JVM, remembering the last stack trace it emitted,
 * and display an error dialog if the client proxy process exits abnormally.
 * Drawback: keeping another process around might be a gratuitous waste of resources.
 * But, compared to the JVM, a tiny native process might barely be counted.
 * Other drawback: this complicates the users life if they try to terminate
 * the process from Task Manager, perhaps because it has wedged up.  But, their
 * life is already complicated since they might not know to look for a javaw.exe process
 * rather than the name of whatever program they think they just started.
 */
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance,
			       LPSTR szCmdLine, int iCmdShow) 
{
	std::string jre;
	std::string commandline;
	getConfig(&jre, &commandline);

	PROCESS_INFORMATION pi;
	STARTUPINFO si;
	ZeroMemory(&si, sizeof(STARTUPINFO));
    if (!CreateProcess(jre.c_str(), // module
					   (LPSTR)commandline.c_str(), // Command line 
					   NULL,             // Process handle not inheritable. 
					   NULL,             // Thread handle not inheritable. 
					   FALSE,            // Set handle inheritance to FALSE. 
					   0,                // No creation flags. 
					   NULL,             // Use parent's environment block. 
					   NULL,             // Use parent's starting directory. 
					   &si,              // Pointer to STARTUPINFO structure.
					   &pi )             // No PROCESS_INFORMATION structure.
	  )
	{
		exitError();
	}
	
	return 0;
}
