:: Copyright (C) 2006 Layer 7 Technologies Inc.
::
:: Compatibility: Windows XP, Windows Server 2003.

:: Install/Uninstall the SecureSpan Gateway Windows service.

@echo off

setlocal
pushd %~dp0\..\..\..\..
set SSGBINDIR=%CD%\bin
popd

call "%SSGBINDIR%\ssgruntimedefs.cmd"

::the partitionihg wizard creates this file for new partitions.
::If it doesn't exist, we'll just use the defaults (for the default_ partition)
if exist partition_config.cmd call "partition_config.cmd"

:: Service name cannot have spaces, but display name and description can.
if "%SERVICE_NAME%"=="" set SERVICE_NAME=SSG
:: The service installer/uninstaller/starter/stopper application.
set PRUNSRV=%SSGBINDIR%\SSG.exe
if exist "%PRUNSRV%" goto okExe
echo ERROR: The file "%PRUNSRV%" was not found.
goto end
:okExe

if ""%1"" == ""install""   goto doInstall
if ""%1"" == ""uninstall"" goto doUninstall

echo Usage: service command
echo Commands:
echo   install          Installs the SecureSpan Gateway service.
echo   uninstall        Uninstalls the SecureSpan Gateway service.
goto end


:doInstall
:: Parameters are passed to PRUNSRV either by command line option or by
:: environment variables prefixed with PR_. However, due to some suspected bug,
:: parameters of types REG_DWORD and REG_MULTI_SZ can only be passed by command
:: line options.

if "%PR_DISPLAYNAME%"=="" set PR_DISPLAYNAME=SecureSpan Gateway
if "%PR_DESCRIPTION%"=="" set PR_DESCRIPTION=Layer 7 Technologies SecureSpan Gateway
set PR_INSTALL=%PRUNSRV%
set PR_STARTUP=auto
set PR_JVM=%JAVA_HOME%\jre\bin\server\jvm.dll
set PR_STARTMODE=jvm
set PR_STOPMODE=jvm
if "%PR_STDOUTPUT%"=="" set PR_STDOUTPUT=%TOMCAT_HOME%\logs\catalina.out
if "%PR_STDERROR%"=="" set PR_STDERROR=%TOMCAT_HOME%\logs\catalina.err
set PR_LOGPATH=%SSG_HOME%\logs
if "%PR_LOGPREFIX%"=="" set PR_LOGPREFIX=ssg_service.log
:: Log level can be error, info, warn or debug.
set PR_LOGLEVEL=debug

set PR_CLASSPATH=%TOMCAT_HOME%\bin\bootstrap.jar
setlocal ENABLEDELAYEDEXPANSION
for %%i in ("%TOMCAT_HOME%\common\classpath\*.jar") do if "!PR_CLASSPATH!"=="" (set PR_CLASSPATH=%%i) else (set PR_CLASSPATH=!PR_CLASSPATH!;%%i)

:: Unfortunately, we cannot use %JAVA_OPTS% composed in ssgruntimedefs.cmd
:: because PRUNSRV wants them semicolon-separated instead of space-separated.
set JVMOPTIONS=^
-Dcatalina.home=%TOMCAT_HOME%;^
-Dcom.l7tech.server.home=%SSG_HOME%;^
-Dfile.encoding=UTF-8;^
-Djava.awt.headless=true;^
-Djava.endorsed.dirs=%TOMCAT_HOME%\common\endorsed;^
-Djava.io.tmpdir=%catalina_tmpdir_shortname%;^
-Djava.library.path=%SSG_HOME%\lib;^
-Djava.net.preferIPv4Stack=true;^
-Djavax.xml.transform.TransformerFactory=org.apache.xalan.processor.TransformerFactoryImpl;^
-Dsun.net.inetaddr.ttl=30;^
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger;^
-Dsun.rmi.dgc.server.gcInterval=3600000;^
-Dsun.rmi.dgc.client.gcInterval=3600000;^
-Xrs;^
-XX:CompileThreshold=1500

if not "%PARTITIONNAMEPROPERTY%"=="" set JVMOPTIONS=%JVMOPTIONS%;-Dcom.l7tech.server.partitionName=%PARTITIONNAMEPROPERTY%

:: WARNING: %DLL_DIR% can contain only one folder. That's because of the
::          unfortunate choice by PRUNSRV to use ";" as a separator in the
::          --Environment option. Fortunately, Tomcat doesn't need anything on
::          the PATH.


"%PRUNSRV%" //IS//%SERVICE_NAME% ^
--Environment "PATH=%DLL_DIR%" ^
--JvmOptions "%JVMOPTIONS%" ^
--JvmMx %java_ram% ^
--JvmSs 256 ^
--StartClass org.apache.catalina.startup.Bootstrap --StartParams="%STARTPARAMS%" ^
--StopClass org.apache.catalina.startup.Bootstrap --StopParams="%STOPPARAMS%"  --StopTimeout 12

if not errorlevel 1 goto installed
echo ERROR: Failed to install "%SERVICE_NAME%" service.
goto end
:installed
echo Installed service "%SERVICE_NAME%".
set PARTITIONNAMEPROPERTY=""
set SERVICE_NAME=""
set PR_DISPLAYNAME=""
set PR_LOGPREFIX=""
set PR_STDOUTPUT=""
set PR_STDERROR=""

goto end


:doUninstall
"%PRUNSRV%" //DS//%SERVICE_NAME%
if not errorlevel 1 goto uninstalled
echo ERROR: Failed to uninstall "%SERVICE_NAME%" service.
goto end
:uninstalled
echo Uninstalled service "%SERVICE_NAME%".
goto end


:end
