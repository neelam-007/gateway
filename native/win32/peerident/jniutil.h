#pragma once
#ifndef _Included_jniutil_h
#define _Included_jniutil_h

#include <exception>
#include <string>
#include <jni.h>
#include <Windows.h>

using std::string;

#define CN_INT              "I"
#define CN_STRING			"Ljava/lang/String;"
#define CN_SYSTEM           "Ljava/lang/System;"
#define CN_PRINTSTREAM      "Ljava/io/PrintStream;"
#define CN_EX_OUT_OF_MEMORY "Ljava/lang/OutOfMemoryError;"
#define CN_EX_ACCESSCONTROL "Ljava/security/AccessControlException;"
#define CN_EX_IO			"Ljava/io/IOException;"


// Exception thrown if a JNI exception is pending and there is no better way to report this
class java_exception_pending : public std::exception {};


// Throws java_exception_pending if the specified expression is zero.
#define PEND_IF_NULL(x) do { if (0 == (x)) throw java_exception_pending(); } while(0)

// Throws java_exception_pending if the specified expression is not zero.
#define PEND_IF_NOT_NULL(x) do { if (0 != (x)) throw java_exception_pending(); } while(0)


// Utility abstract superclass for classes that auto-release resources identified by a pointer
// For this to work, each subclass MUST provide a virtual destructor that calls release().
template<class ptrT, ptrT nullT = NULL>
class auto_free {
public:
	// Create an auto_free instance that initially owns no resource.
	explicit auto_free() : m_ptr(nullT) {}

	// Create an auto_free instance that initially owns the specified resource.
	explicit auto_free(ptrT ptr) : m_ptr(ptr) {}

	// Ensure that resource is released upon destruction.
	// Caller MUST augment this with their own vritual destructor that calls release().
	// We can't do it here because by the time this destructor is invoked it's too late
	// to call the release_impl virtual function.
	virtual ~auto_free() {}

	// Take ownership of the specified resource, first releasing any curruently-owned resource.
	void acquire(ptrT ptr) {
		release();
		m_ptr = ptr;
	}

	// Release the resource, if necessary, and clear the reference.
	virtual void release() = 0;

	// Detach the resource from this auto_free instance so that it will not be freed.
	ptrT detach() {
		ptrT ret = m_ptr;
		m_ptr = nullT;
		return ret;
	}

	// Peek at the resource without giving up ownership of it.
	ptrT get() {
		return m_ptr;
	}

protected:
	// Pointer the resource we are managing
	ptrT m_ptr;

private:
	// Can't be copied
	auto_free(const auto_free& o) { }

	// Can't be assigned to
	auto_free& operator=(const auto_free& o) { return *this; }
};


// Utility class that can automatically delete a local reference to a Java object as soon as
// its owning auto_local_ref instance goes out of scope.
// The reference will be freed using the provided JNI environment, which must live
// at least as long as this auto_local_ref instance.
template<class jobjectT>
class auto_local_ref : public auto_free<jobjectT> {
public:
	explicit auto_local_ref(JNIEnv* env) : m_env(env), auto_free() {};
	explicit auto_local_ref(JNIEnv* env, jobjectT obj) : m_env(env), auto_free(obj) {};
	virtual ~auto_local_ref() { release(); };
	virtual void release() {
		if (NULL != m_ptr) {
			if (NULL != m_env)
				m_env->DeleteLocalRef(m_ptr);
			m_ptr = NULL;
		}
	}

	// Take ownership of the specified reference, throwing java_exception_pending if it is NULL.
	void acquire_not_null(jobjectT obj) {
		PEND_IF_NULL(obj);
		acquire(obj);
	}

private:
	JNIEnv* m_env;
};


// Utility class that calls LocalFree on its owned pointer when destroyed
class auto_LocalFree : public auto_free<HLOCAL> {
public:
	explicit auto_LocalFree() : auto_free() {};
	explicit auto_LocalFree(HLOCAL ptr) : auto_free(ptr) {};
	virtual ~auto_LocalFree() { release(); };
	virtual void release() {
		if (NULL != m_ptr) {
			::LocalFree(m_ptr);
			m_ptr = NULL;
		}
	}
};


// Throw java_exception_pending if the specified env has a Java exception pending.
void pend_check(JNIEnv* env);


// Find the specified class and cache it in the specified global variable.
// If weak is true, we will store a weak global ref; otherwise we'll store a strong global ref
// Caller must ensure that the global ref is eventually deleted.
// Throws java_exception_pending on failure.
void cache_class(JNIEnv* env, const string classname, jclass& global, bool weak=false);


// Find the specified field ID and cache it in the specified global variable.
// Throws java_exception_pending on failure.
void cache_field(JNIEnv* env, jclass cls, const string field_name, const string field_sig, jfieldID& global);


// Converts a TSTR, as it may have come from a Windows sytem call, into a UTF-8 string.
string tstr_to_utf8(const LPTSTR in);


// Throws the specified Java exception with the default formatted message (ignoring inserts) for the Windows error code
void report_error(JNIEnv* env, jclass exceptionCls, const string prefix, DWORD err);


#ifdef _DEBUG

// Initialize debug printing support, which caches some global references.
// Must be called only once, when the DLL is first loaded.
// Caller must ensure that close_debug gets called when the DLL is about to be unloaded.
void dprint_open(JNIEnv* env);

// Shut down debug printing support, freeing all cached global references.
// Must be called only once, when the DLL is about to be unloaded.
void dprint_close(JNIEnv* env);

// Print the specified message to System.err
// May throw java_exception_pending if the printing fails
void dprint(JNIEnv* env, const string msg);

// Maximum size of a single message that can be printed with dprintf
#define DPRINTF_BUFFER_SIZE 4096

// Print the specified formatted message to System.err
// May throw java_exception_pending if the printing fails
#define dprintf(env, format, ...) \
	do {char dprintf_buffer[DPRINTF_BUFFER_SIZE]; \
		sprintf_s(dprintf_buffer, sizeof dprintf_buffer, format, __VA_ARGS__); \
		dprint(env, dprintf_buffer); } while(0)

#else /* _DEBUG */

// Make the debug calls noops
#define DPRINT_DO_NOTHING do {0;} while(0)
#define dprint_open(...)            DPRINT_DO_NOTHING
#define dprint_close(...)           DPRINT_DO_NOTHING
#define dprint(...)                 DPRINT_DO_NOTHING
#define dprintf(env, format, ...)   DPRINT_DO_NOTHING

#endif /* _DEBUG */


#endif /* _Included_jniutil_h */
