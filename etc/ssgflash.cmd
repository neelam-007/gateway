:: -----------------------------------------------------------------------------
:: LAYER 7 TECHNOLOGIES
:: November 2006
:: Launches the SSG Flasher Utility
:: -----------------------------------------------------------------------------
@echo off

setlocal

pushd "%~dp0\.."
set SSG_ROOT="%CD%"
set JAVA_HOME=%SSG_ROOT%\jdk
popd

echo "using %SSG_ROOT% as the SSG_ROOT\n"
%JAVA_HOME%\bin\java -Dcom.l7tech.server.home=%SSG_ROOT% -jar SSGFlasher.jar  "%1" "%2" "%3" "%4" "%5" "%6" "%7" "%8" "%9" "%10" "%11" "%12"
