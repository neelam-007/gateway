:: Copyright (C) 2006 Layer 7 Technologies Inc.
::
:: Compatibility: Windows XP, Windows Server 2003.

:: This file must not be moved from its location in <SSG home folder>\bin
:: because runtime determination of the SSG home folder depends on its assumed
:: location.

:: Compute the SSG home folder absolute path (without trailing backslash!).
pushd %~dp0\..
set SSG_HOME=%CD%
popd

set JAVA_HOME=%SSG_HOME%\jdk
set TOMCAT_HOME=%SSG_HOME%\tomcat

:: Compute the short name for the temporary folder path. This is because
:: SafeNet Luna cmu.exe doesn't like path with spaces.
pushd "%SSG_HOME%\bin"
for /F "tokens=1" %%i in ('GetShortName.cmd "%TOMCAT_HOME%\temp"') do set catalina_tmpdir_shortname=%%i
popd

:: Unset environment variables that we don't want to inherit from outside.
set CATALINA_HOME=
set CATALINA_BASE=
set CATALINA_OPTS=
set CATALINA_TMPDIR=%catalina_tmpdir_shortname%
set JSSE_HOME=
set JPDA_TRANSPORT=
set JPDA_ADDRESS=

:: Compute system physical RAM amount for tunning JVM.
pushd "%SSG_HOME%"
for /F "tokens=1" %%i in ('bin\sysmem.exe "--unit=M" --roundoff TotalPhys') do set system_ram=%%i
popd
set /a java_ram=%system_ram%*2/3
if %java_ram% GTR 1024 (
  set java_ram=1024
)

:: REMINDER: Changes to %JAVA_OPTS% will not propagate automatically to
::           %JVMOPTIONS% in service.cmd. You must edit the same changes there
::           manually.
set JAVA_OPTS=^
-Dcom.l7tech.server.home="%SSG_HOME%" ^
-Dfile.encoding=UTF-8 ^
-Djava.awt.headless=true ^
-Djava.library.path="%SSG_HOME%\lib" ^
-Djava.net.preferIPv4Stack=true ^
-Djavax.xml.transform.TransformerFactory=org.apache.xalan.processor.TransformerFactoryImpl ^
-Dsun.net.inetaddr.ttl=30 ^
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger ^
-Dsun.rmi.dgc.server.gcInterval=3600000 ^
-Dsun.rmi.dgc.client.gcInterval=3600000 ^
-server ^
-Xmx%java_ram%M ^
-Xrs ^
-Xss256k ^
-XX:CompileThreshold=1500

:: If a JNI DLL is dependent on another DLL, that second DLL must be in a folder
:: on the Windows PATH environment variable.
:: Our practice is to put all DLLs in %SSG_HOME%\lib.
set DLL_DIR=%SSG_HOME%\lib
set PATH=%PATH%;%JAVA_HOME%\bin;%SSG_HOME%\bin;%SSG_HOME%\lib
