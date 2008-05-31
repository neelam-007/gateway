#define WIN32_LEAN_AND_MEAN 1

#include <string>
#include <vector>

#include <Windows.h>
#include <Winsock.h>
#include <Iphlpapi.h>
#include <WtsApi32.h>
#include <math.h>

#include "com_l7tech_common_security_socket_Win32LocalTcpPeerIdentifier.h"
#include "jniutil.h"

using namespace std;


//
// Constants
//

#define INITIAL_BUFFER_SIZE 17
#define MAX_BUFFER_SIZE 0x10000000

#define CN_SELF				"Lcom/l7tech/common/security/socket/Win32LocalTcpPeerIdentifier;"
#define FIELD_PID			"pid"
#define FIELD_SID			"sid"
#define FIELD_USERNAME		"username"
#define FIELD_DOMAIN		"domain"
#define FIELD_PROGRAM		"program"


//
// Globals
//

// cached strong global refs
JavaVM *g_cached_jvm;
jclass g_exOutOfMemoryError;
jclass g_exAccessControlException;
jclass g_exIOException;

// cached weak global refs
jclass g_thisClass;

// cached field IDs
jfieldID g_fieldPid;
jfieldID g_fieldSid;
jfieldID g_fieldUsername;
jfieldID g_fieldDomain;
jfieldID g_fieldProgram;


///
/// Module classes
///

// Utility class that calls WTSFreeMemory on its owned pointer when destroyed
class auto_WTSFreeMemory : public auto_free<LPVOID> {
public:
	explicit auto_WTSFreeMemory() : auto_free() {};
	explicit auto_WTSFreeMemory(LPVOID ptr) : auto_free(ptr) {};
	virtual ~auto_WTSFreeMemory() { release(); };
protected:
	virtual void release_impl(LPVOID ptr) {
		::WTSFreeMemory(ptr);
	}
};


//
// Functions
//

// Populate all global variables that cache Java classes, method IDs, and field IDs.
// Throw the C++ exception java_exception_pending if it fails.
void
init_globals(JavaVM* jvm)
{
	JNIEnv* env;

	g_cached_jvm = jvm;
	PEND_IF_NOT_NULL(jvm->GetEnv((void**)&env, JNI_VERSION_1_2));

	dprint_open(env);

	// Cache the classes and fields we are going to use
	cache_class(env, CN_EX_OUT_OF_MEMORY, g_exOutOfMemoryError);
	cache_class(env, CN_EX_ACCESSCONTROL, g_exAccessControlException);
	cache_class(env, CN_EX_IO, g_exIOException);

	// Cache our own class in a weak ref so it can be unloaded (which will in turn trigger our JNI_OnUnload callback)
	cache_class(env, CN_SELF, g_thisClass, true);

	// Cache our field IDs
	cache_field(env, g_thisClass, FIELD_PID, CN_INT, g_fieldPid);
	cache_field(env, g_thisClass, FIELD_SID, CN_INT, g_fieldSid);
	cache_field(env, g_thisClass, FIELD_USERNAME, CN_STRING, g_fieldUsername);
	cache_field(env, g_thisClass, FIELD_DOMAIN, CN_STRING, g_fieldDomain);
	cache_field(env, g_thisClass, FIELD_PROGRAM, CN_STRING, g_fieldProgram);

	dprintf(env, "%s", "Native library initialized");
}


//
// Called by the JVM when the DLL is loaded.
// This function initializes our cached global references and field/method IDs.
//
JNIEXPORT jint JNICALL
JNI_OnLoad
  (JavaVM* jvm, LPVOID reserved)
{
	// wrap so we don't propagate native exceptions back up into the JVM
	try {
		init_globals(jvm);
		return JNI_VERSION_1_2;
	} catch (...) {
		return JNI_ERR;
	}
}


//
// Called by the JVM when the DLL is about to be unloaded.
// This releases our cached global refs.
//
JNIEXPORT void JNICALL 
JNI_OnUnload
  (JavaVM *jvm, void *reserved)
{
	JNIEnv *env;
	if (jvm->GetEnv((void**)&env, JNI_VERSION_1_2))
		return; // JNI version too old

	dprint_close(env);

	env->DeleteGlobalRef(g_exOutOfMemoryError);
	env->DeleteGlobalRef(g_exAccessControlException);
	env->DeleteGlobalRef(g_exIOException);
	env->DeleteWeakGlobalRef(g_thisClass);

	g_exOutOfMemoryError = NULL;
	g_exAccessControlException = NULL;
	g_exIOException = NULL;
	g_thisClass = NULL;
}


// Throws either IOException or AccessControlException, depending on the specified Windows error code
void 
report_error_auto(JNIEnv* env, const string prefix, DWORD err) 
{
	jclass ex = err == ERROR_ACCESS_DENIED ? g_exAccessControlException : g_exIOException;
	report_error(env, ex, prefix, err);
}


// Fetch the system TCP table into the specified byte vector.
// Returns true if the TCP table was loaded successfully.
// Returns false if a Java exception is pending.
// May throw C++ exceptions: bad_alloc, java_exception_pending.
bool 
get_tcp_table(JNIEnv* env, vector<BYTE>& bytes)
{
	DWORD bytes_next_size = max(INITIAL_BUFFER_SIZE, bytes.capacity());

	for (;;) {
		bytes.reserve(bytes_next_size);
		if (bytes.capacity() < bytes_next_size) throw bad_alloc();
		DWORD bytes_actual_size = bytes.capacity();

		DWORD r = ::GetExtendedTcpTable(&(*bytes.begin()), &bytes_actual_size, FALSE, AF_INET, TCP_TABLE_OWNER_PID_ALL, 0);

		switch (r) {
			case NO_ERROR:
				// the bytes vector now contains a full MIB_TCPTABLE_OWNER_PID structure
				return true;
			case ERROR_INSUFFICIENT_BUFFER:
				if (bytes_next_size < MAX_BUFFER_SIZE) {
					// Allocate a larger buffer and try again
					bytes_next_size = max((DWORD)::ceil(1.6 * (bytes_next_size + 1)), bytes_actual_size);
					bytes.reserve(bytes_next_size);
					if (bytes.capacity() < bytes_next_size) throw bad_alloc();
					break; // retry with larger buffer
				}
				throw bad_alloc();
			case ERROR_INVALID_PARAMETER:
				env->ThrowNew(g_exIOException, "Invalid parameter");
				return NULL;
			default:
				env->ThrowNew(g_exIOException, "Unknown error");
				return NULL;
		}
	}

	return NULL;
}


// Scan the provided TCP table for a matching row.
// Returns the matching pid, or -1 if no matching pid was found.
int 
scan_for_pid(PMIB_TCPTABLE_OWNER_PID table, jlong sockip, jboolean incloop, jint sockport, jlong peerip, jint peerport) 
{
	DWORD wantRemoteAddr = htonl((DWORD)sockip);
	DWORD wantRemotePort = htons((u_short)sockport);
	DWORD wantLocalAddr = htonl((DWORD)peerip);
	DWORD wantLocalPort = htons((u_short)peerport);

	int rows = table->dwNumEntries;
	for (int i = 0; i < rows; ++i) {
		PMIB_TCPROW_OWNER_PID row = &table->table[i];

		// Ignore connections not in the ESTABLISHED state
		if (row->dwState != MIB_TCP_STATE_ESTAB)
			continue;

		if (row->dwRemotePort != wantRemotePort)
			continue;

		if (row->dwLocalPort != wantLocalPort)
			continue;

		if (row->dwLocalAddr != wantLocalAddr)
			continue;

		if (wantRemoteAddr) {
			if (row->dwRemoteAddr != wantRemoteAddr)
				continue;
		} else if (incloop) {
			struct in_addr inaddr;
			inaddr.S_un.S_addr = row->dwRemoteAddr;
			if (inaddr.S_un.S_un_b.s_b1 != 127)
				continue;
		} else
			continue;

		return row->dwOwningPid;		
	}

	return -1;
}


// Query a terminal server session on the current terminal server for a string value.
// Returns a new jstring local reference, or NULL if a java exception is pending.
// errprefix should describe what we were trying to fetch ie "session username"
jstring 
query_wts_string(JNIEnv* env, DWORD sessionId, WTS_INFO_CLASS query, const char* errprefix) 
{
	LPTSTR querybuf(NULL);
	DWORD querybufsize(0);
	jstring ret(NULL);
	auto_WTSFreeMemory auto_wts;

	dprintf(env, "    performing query for %s", errprefix);
	if (0 == ::WTSQuerySessionInformation(WTS_CURRENT_SERVER_HANDLE, sessionId, query, &querybuf, &querybufsize)) {
		DWORD err = GetLastError();
		if (err == ERROR_SUCCESS) {
			// This breaks the WTSQuerySessionInformaion contract, but it appears to happen when querying WTSApplicationName.
			// Possibly when "the session specified in the SessionId parameter is a session at the physical console".
			// We'll treat this like an empty application name.
			querybuf = TEXT("");
			/* FALLTHROUGH without registering querybuf with auto_wts */
		} else {
			report_error_auto(env, errprefix, err);
			return NULL;
		}
	} else
		auto_wts.acquire(querybuf);

	dprintf(env, "    converting to UTF8 for %s", errprefix);

	return env->NewStringUTF(tstr_to_utf8(querybuf).c_str());
}


// Attempts to identify the native peer using the provided socket information.
// If succesful, sets appropriate fields on the java object obj and returns JNI_TRUE.
// Returns JNI_FALSE if unable to find a matching local process for the specified connection info.
// If this returns JNI_FALSE a Java exception may or may not be pending.
// May also throw C++ exceptions, in which case a Java exception may or may not be pending:
//    java_exception_pending    if a Java exception is pending
//    bad_alloc                 out of memory
jboolean 
NativeIdentifyPeer_impl(JNIEnv* env, 
						jobject obj, 
						jlong sockip, 
						jboolean incloop, 
						jint sockport, 
						jlong peerip, 
						jint peerport)
{
	if (0 != env->EnsureLocalCapacity(8))
		return JNI_FALSE;

	vector<BYTE> tcpvec(INITIAL_BUFFER_SIZE);

	// Load system TCP table
	if (!get_tcp_table(env, tcpvec))
		return JNI_FALSE;

	dprintf(env, "Scanning tcp table for a matching local connection");

	// Scan for pid owning matching connection
	PMIB_TCPTABLE_OWNER_PID tcptable = (PMIB_TCPTABLE_OWNER_PID)&(*tcpvec.begin());
	int pid = scan_for_pid(tcptable, sockip, incloop, sockport, peerip, peerport);
	if (-1 == pid) {
		// No match in table.
		return JNI_FALSE;
	}

	dprintf(env, "Looking for session ID for pid %d", pid);

	// Find session ID
	DWORD sid;
	if (0 == ::ProcessIdToSessionId(pid, &sid)) {
		report_error_auto(env, "Unable to look up session ID for remote process", GetLastError());
		return JNI_FALSE;
	}

	dprintf(env, "Found session id %d; getting username", sid);

	// Query the values we need to store
	jstring username = query_wts_string(env, sid, WTSUserName, "session username");
	if (NULL == username)
		return JNI_FALSE;

	dprint(env, "getting domain");

	jstring domain = query_wts_string(env, sid, WTSDomainName, "session domain");
	if (NULL == domain)
		return JNI_FALSE;

	dprint(env, "getting program");

	jstring program = query_wts_string(env, sid, WTSApplicationName, "session application name");
	if (NULL == program)
		return JNI_FALSE;

	dprint(env, "storing values");

	// Store the values
	env->SetIntField(obj, g_fieldPid, pid);
	env->SetIntField(obj, g_fieldSid, sid);
	env->SetObjectField(obj, g_fieldUsername, username);
	env->SetObjectField(obj, g_fieldDomain, domain);
	env->SetObjectField(obj, g_fieldProgram, program);

	dprint(env, "returning true");

	return JNI_TRUE;
}


//
// Main entry point into native code.
// Attempts to identify a peer.  See this native method's javadoc for the full spec.
//
JNIEXPORT jboolean JNICALL 
Java_com_l7tech_common_security_socket_Win32LocalTcpPeerIdentifier_nativeIdentifyTcpPeer
  (JNIEnv* env, jobject obj, jlong sockip, jboolean incloop, jint sockport, jlong peerip, jint peerport)
{
	// Wrap to prevent any native exceptions from leaking out and messing up the JVM
	try {
		return NativeIdentifyPeer_impl(env, obj, sockip, incloop, sockport, peerip, peerport);
	} catch (bad_alloc) {
		if (!env->ExceptionCheck()) env->ThrowNew(g_exOutOfMemoryError, "Out of memory");
		return JNI_FALSE;
	} catch (java_exception_pending) {
		if (!env->ExceptionCheck()) env->ThrowNew(g_exIOException, "Unexpected java_exception_pending"); // shouldn't happen
		return JNI_FALSE;
	} catch (...) {
		if (!env->ExceptionCheck()) env->ThrowNew(g_exIOException, "Unexpected native exception");
		return JNI_FALSE;
	}
}
