:: Copyright (C) 2006 Layer 7 Technologies Inc.
::
:: Install script for Observer for CA Unicenter WSDM.
::
:: Compatibility: Windows XP, Windows Server 2003.

@echo off

pushd %~dp0

setlocal

:: Get SSG home folder from registry.
::
:: On Windows Server 2003, the folder path is in %%k because reg query returns
::     (Default)   REG_SZ  C:\Program Files\Layer 7 Technologies\SecureSpan Gateway
:: On Windows XP, the folder path is in %%l because reg query returns
::     <NO NAME>   REG_SZ  C:\Program Files\Layer 7 Technologies\SecureSpan Gateway
set REG_COLUMN=
for /F "tokens=1,2,3" %%i in ('reg query "HKLM\SOFTWARE\Layer 7 Technologies\SecureSpan Gateway" /ve') do if "%%j"=="REG_SZ" (set REG_COLUMN=3) else (if "%%k"=="REG_SZ" set REG_COLUMN=4)
if "%REG_COLUMN%"=="3" (
    for /F "tokens=1,2*" %%i in ('reg query "HKLM\SOFTWARE\Layer 7 Technologies\SecureSpan Gateway" /ve') do if "%%j"=="REG_SZ" set SSG_HOME_DEFAULT=%%k
) else if "%REG_COLUMN%"=="4" (
    for /F "tokens=1,2,3*" %%i in ('reg query "HKLM\SOFTWARE\Layer 7 Technologies\SecureSpan Gateway" /ve') do if "%%k"=="REG_SZ" set SSG_HOME_DEFAULT=%%l
) else (
    echo !! WARNING: SecureSpan Gateway home folder path not found in registry. It may have been corrupted. You can still proceed by typing in the full path.
)

:: Give the user a chance to override.
set SSG_HOME=
set /p SSG_HOME="SecureSpan Gateway home folder [%SSG_HOME_DEFAULT%]: "
if defined SSG_HOME (
    set SSG_HOME=%SSG_HOME:"=%
) else (
    set SSG_HOME=%SSG_HOME_DEFAULT%
)

:: Verify that the SSG home folder exist.
if not exist "%SSG_HOME%" (
    echo !! SecureSpan Gateway folder not found: %SSG_HOME%
    echo !! Please ensure the folder has not been moved or deleted.
    goto end
)

:: Edit WsdmSOMMA_Basic.properties.
set SSG_HOME_ESC=%SSG_HOME:\=\\%
if exist WsdmSOMMA_Basic.properties.EDIT del WsdmSOMMA_Basic.properties.EDIT
for /F "tokens=1,* delims==" %%i in (ssg\tomcat\webapps\ROOT\WEB-INF\classes\WsdmSOMMA_Basic.properties) do if "%%i"=="log.file.path" (echo log.file.path=%SSG_HOME_ESC%\\logs\\CaWsdmObserver >> WsdmSOMMA_Basic.properties.EDIT) else (echo %%i=%%j >> WsdmSOMMA_Basic.properties.EDIT)
move /Y WsdmSOMMA_Basic.properties.EDIT ssg\tomcat\webapps\ROOT\WEB-INF\classes\WsdmSOMMA_Basic.properties

:: Copy files with forced overwrite.
xcopy /F /Y ssg\tomcat\webapps\ROOT\WEB-INF\CaWsdmObserverContext.xml "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\"
xcopy /F /Y ssg\tomcat\webapps\ROOT\WEB-INF\lib\axis-1.3.jar "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\lib\"
xcopy /F /Y ssg\tomcat\webapps\ROOT\WEB-INF\lib\CaWsdmObserver.jar "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\lib\"
xcopy /F /Y ssg\tomcat\webapps\ROOT\WEB-INF\lib\ca_wsdm-3.50-core.jar "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\lib\"
xcopy /F /Y ssg\tomcat\webapps\ROOT\WEB-INF\lib\ca_wsdm-3.50-handler_common.jar "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\lib\"
xcopy /F /Y ssg\tomcat\webapps\ROOT\WEB-INF\lib\ca_wsdm-3.50-wsdm35mmi-axis-stubskel.jar "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\lib\"
xcopy /F /Y ssg\tomcat\webapps\ROOT\WEB-INF\lib\tmxmltoolkit.jar "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\lib\"

:: Copy files without forced overwrite.
if exist "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\classes\WsdmSOMMA_Basic.properties" (
    echo The file "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\classes\WsdmSOMMA_Basic.properties" already exists. It will not be overwritten.
) else (
    xcopy /F /Y ssg\tomcat\webapps\ROOT\WEB-INF\classes\WsdmSOMMA_Basic.properties "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\classes"
)

if exist "%SSG_HOME%\etc\conf\CaWsdmObserver.properties" (
    echo The file "%SSG_HOME%\etc\conf\CaWsdmObserver.properties" already exists. It will not be overwritten.
) else (
    xcopy /F /Y ssg\etc\conf\CaWsdmObserver.properties "%SSG_HOME%\etc\conf\"
)

echo.
echo Installation of Observer for CA Unicenter WSDM complete.
echo You may need to review and update the configuration in:
echo     "%SSG_HOME%\etc\conf\CaWsdmObserver.properties"
echo     "%SSG_HOME%\tomcat\webapps\ROOT\WEB-INF\classes\WsdmSOMMA_Basic.properties"

:end
popd
