@echo off

rem FILE [importTrustedCertificate.bat]
rem
rem LAYER 7 TECHNOLOGIES
rem 02-07-2003, flascelles
rem
rem THIS SCRIPT IMPORTS A SSG CERTIFICATE INTO THE TRUSTED STORE OF THE SSG CONSOLE OR
rem SSG PROXY
rem
rem PREREQUISITES
rem 0. pass the certificate as argument
rem 1. $JAVA_HOME be defined
rem 2. $JAVA_HOME\bin\keytool be there
rem
rem -----------------------------------------------------------------------------
rem
rem Location of the file in Windows :
set STOREDIR=%HOMEDRIVE%%HOMEPATH%\.ssg
set STOREFILE=%STOREDIR%\trustStore
set KEYTOOLBIN="%JAVA_HOME%\bin\keytool"

rem CHECK THAT WE ARE RECIEVING THE CERTIFICATE FILE AS AN ARGUMENT
IF "%1"=="" GOTO usage

IF EXIST %STOREFILE% GOTO import

:createdir
    echo creating directory
    md "%STOREDIR%"
    GOTO import
    
:import
    @echo on
    %KEYTOOLBIN% -import -v -trustcacerts -alias tomcat -file %1 -keystore "%STOREFILE%" -keypass password -storepass password
    @echo off
    GOTO end

:usage
    @echo on
    echo USAGE: %0 filetoimport.cer
    @echo off
    GOTO end

:end
