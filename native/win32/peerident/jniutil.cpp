
#include "jniutil.h"
#include <Windows.h>

using namespace std;

void
pend_check(JNIEnv* env)
{
	PEND_IF_NOT_NULL(env->ExceptionCheck());
}


void 
cache_class(JNIEnv* env, const string classname, jclass& global, bool weak) 
{
	jclass cls;

	PEND_IF_NULL(cls = env->FindClass(classname.c_str()));
	PEND_IF_NULL(global = static_cast<jclass>(weak ? env->NewWeakGlobalRef(cls) : env->NewGlobalRef(cls)));
}


void
cache_field(JNIEnv* env, jclass cls, const string field_name, const string field_sig, jfieldID& global)
{
	PEND_IF_NULL(global = env->GetFieldID(cls, field_name.c_str(), field_sig.c_str()));
}


// Converts a TSTR, as it may have come from a Windows sytem call, into a UTF-8 string.
string
tstr_to_utf8(const LPTSTR in)
{
#ifdef  UNICODE
	// TSTR is wide characters
	int needed = ::WideCharToMultiByte(CP_UTF8, 0, in, -1, 0, 0, NULL, NULL);
	string out(needed, '\0');
	::WideCharToMultiByte(CP_UTF8, 0, in, -1, &(*out.begin()), out.length(), NULL, NULL);
	return out;
#else
	// TSTR is ANSI characters.  Not supported
#error Must compile in UNICODE mode
#endif
}


// Throws the specified Java exception with the default formatted message (ignoring inserts) for the Windows error code
void 
report_error(JNIEnv* env, jclass exceptionCls, const string prefix, DWORD err)
{
	LPTSTR tstr;
	FormatMessage(
		FORMAT_MESSAGE_ALLOCATE_BUFFER |
		FORMAT_MESSAGE_FROM_SYSTEM |
		FORMAT_MESSAGE_IGNORE_INSERTS,
		NULL,
		err,
		0,
		(LPTSTR)&tstr,
		0,
		NULL);

	auto_LocalFree af0(tstr);

	string utfstr(prefix);
	utfstr.append(": ");
	utfstr.append(tstr_to_utf8(tstr));

	env->ThrowNew(exceptionCls, utfstr.c_str());
}


//
// Debug logging
//
#ifdef _DEBUG

jclass g_clsSystem;
jobject g_objSystemErr;
jmethodID g_methodPrintln;

void
dprint(JNIEnv* env, const string msg) 
{
	auto_local_ref<jstring> str(env);
	str.acquire_not_null(env->NewStringUTF(msg.c_str()));
	env->CallVoidMethod(g_objSystemErr, g_methodPrintln, str.get());
}


void
init_system_err_println(JNIEnv* env, jclass clsSystem)
{

}

void
dprint_open(JNIEnv* env)
{
	jfieldID field_err;
	jobject obj_system_err;
	jclass clsPrintStream;
	char msg[128];

	DWORD pid = ::GetCurrentProcessId();
	sprintf_s(msg, sizeof msg, "Current PID: %d\n\nAttach debugger now, if desired; then, press OK to continue.", pid);
	::MessageBoxA(NULL, msg, "JNI DLL Attached", MB_OK);

	// Save g_clsSystem
	cache_class(env, CN_SYSTEM, g_clsSystem);

	// Save g_objSystemErr
	PEND_IF_NULL(field_err = env->GetStaticFieldID(g_clsSystem, "err", CN_PRINTSTREAM));
	PEND_IF_NULL(obj_system_err = env->GetStaticObjectField(g_clsSystem, field_err));
	PEND_IF_NULL(g_objSystemErr = env->NewGlobalRef(obj_system_err));

	// Save g_methodPrintln
	PEND_IF_NULL(clsPrintStream = env->FindClass(CN_PRINTSTREAM));
	PEND_IF_NULL(g_methodPrintln = env->GetMethodID(clsPrintStream, "println", "(Ljava/lang/String;)V"));
}


void
dprint_close(JNIEnv* env)
{
	env->DeleteGlobalRef(g_objSystemErr); 
	env->DeleteGlobalRef(g_clsSystem);    

	g_objSystemErr = NULL;
	g_clsSystem = NULL;
}

#endif /* _DEBUG */




