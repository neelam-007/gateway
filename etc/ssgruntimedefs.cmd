:: Copyright (C) 2005 Layer 7 Technologies Inc.
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
for /F "tokens=1" %%i in ('GetShortName.cmd "%TOMCAT_HOME%\temp"') do set catalina_tmpdir_shortname=%%i

:: Unset environment variables that we don't want to inherit from outside.
set CATALINA_HOME=
set CATALINA_BASE=
set CATALINA_OPTS=
set CATALINA_TMPDIR=%catalina_tmpdir_shortname%
set JSSE_HOME=
set JPDA_TRANSPORT=
set JPDA_ADDRESS=

:: Compute system physical RAM amount for tunning JVM.
for /F "delims=: tokens=1,2" %%i in ('systeminfo') do if "%%i"=="Total Physical Memory" set tmpstr1=%%j
for /F "tokens=1" %%i in ("%tmpstr1%") do set tmpstr2=%%i
set system_ram=%tmpstr2:,=%
set /a java_ram=%system_ram%*2/3
set /a maxnewsize=%java_ram%/2


set JAVA_OPTS=^
-Dcom.l7tech.server.home="%SSG_HOME%" ^
-Dfile.encoding=UTF-8 ^
-Djava.awt.headless=true ^
-Djava.library.path="%SSG_HOME%\lib" ^
-Dnetworkaddress.cache.ttl=30 ^
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger ^
-server ^
-Xms%java_ram%M ^
-Xmx%java_ram%M ^
-Xrs ^
-Xss256k ^
-XX:CompileThreshold=1500 ^
-XX:NewSize=%maxnewsize%M ^
-XX:MaxNewSize=%maxnewsize%M ^
-XX:+DisableExplicitGC

:: Tune for single or multi processor machine.
if %NUMBER_OF_PROCESSORS%==1 (
    set JAVA_OPTS=%JAVA_OPTS% -XX:+DisableExplicitGC
) else (
    set JAVA_OPTS=%JAVA_OPTS% -XX:+DisableExplicitGC -XX:+UseParallelGC
)

REM 2005-09-13 rmak
REM Setting of RMI server host name has been moved to etc\conf\system.properties.
REM
REM :: Set the fully qualified host name of the RMI server.
REM for /F "tokens=1" %%i in ('hostname') do set short_hostname=%%i
REM for /F "tokens=1,2" %%i in ('nslookup %short_hostname%') do if "%%i"=="Name:" set rmi_server_full_hostname=%%j
REM if exist "%SSG_HOME%\etc\conf\cluster_hostname" (
REM     for /F "usebackq tokens=1" %%i in ("%SSG_HOME%\etc\conf\cluster_hostname") do set rmi_server_full_hostname=%%i
REM )
REM set JAVA_OPTS=%JAVA_OPTS% -Djava.rmi.server.hostname=%rmi_server_full_hostname%

:: Append to the system search path.
set PATH=%PATH%;%JAVA_HOME%\bin;%SSG_HOME%\bin
