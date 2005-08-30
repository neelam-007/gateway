:: Copyright (C) 2005 Layer 7 Technologies Inc.
::
:: Compatibility: Windows XP, Windows Server 2003.

:: Install/Uninstall the SecureSpan Gateway Windows service.

@echo off

setlocal

call "%~dp0\ssgruntimedefs.cmd"

:: Service name cannot have spaces, but display name and description can.
set SERVICE_NAME=SSG

:: The service installer/uninstaller/starter/stopper application.
set PRUNSRV=%SSG_HOME%\bin\SSG.exe
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
:: parameters of types REG_DWORD and REG_MULTI_SZ can only be passwed by command
:: line options.
set PR_DISPLAYNAME=SecureSpan Gateway
set PR_DESCRIPTION=Layer 7 Technologies SecureSpan Gateway
set PR_INSTALL=%PRUNSRV%
set PR_STARTUP=auto
set PR_JVM=%JAVA_HOME%\jre\bin\server\jvm.dll
set PR_STARTMODE=jvm
set PR_STOPMODE=jvm
set PR_STDOUTPUT=%TOMCAT_HOME%\logs\catalina.out
set PR_STDERROR=%TOMCAT_HOME%\logs\catalina.err
set PR_LOGPATH=%SSG_HOME%\logs
set PR_LOGPREFIX=ssg_service.log
:: Log level can be error, info, warn or debug.
set PR_LOGLEVEL=debug

set PR_CLASSPATH=%TOMCAT_HOME%\bin\bootstrap.jar
setlocal ENABLEDELAYEDEXPANSION
for %%i in ("%TOMCAT_HOME%\common\classpath\*.jar") do if "!PR_CLASSPATH!"=="" (set PR_CLASSPATH=%%i) else (set PR_CLASSPATH=!PR_CLASSPATH!;%%i)

set JVMOPTIONS=^
-Dcatalina.home=%TOMCAT_HOME%;^
-Dcom.l7tech.server.home=%SSG_HOME%;^
-Dfile.encoding=UTF-8;^
-Djava.awt.headless=true;^
-Djava.endorsed.dirs=%TOMCAT_HOME%\common\endorsed;^
-Djava.io.tmpdir=%TOMCAT_HOME%\temp;^
-Djava.rmi.server.hostname=%rmi_server_full_hostname%;^
-Dnetworkaddress.cache.ttl=30;^
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger;^
-Xrs;^
-XX:CompileThreshold=1500;^
-XX:MaxNewSize=%maxnewsize%M;^
-XX:NewSize=%maxnewsize%M;^
-XX:+DisableExplicitGC

"%PRUNSRV%" //IS//%SERVICE_NAME% ^
--JvmOptions "%JVMOPTIONS%" ^
--JvmMs %java_ram% ^
--JvmMx %java_ram% ^
--JvmSs 256 ^
--StartClass org.apache.catalina.startup.Bootstrap --StartParams start ^
--StopClass  org.apache.catalina.startup.Bootstrap --StopParams  stop  --StopTimeout 12

if not errorlevel 1 goto installed
echo ERROR: Failed to install "%SERVICE_NAME%" service.
goto end
:installed
echo Installed service "%SERVICE_NAME%".
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
