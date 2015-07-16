;NSIS Modern User Interface version 1.63
;based on Basic Example Script, which was Written by Joost Verburg

!ifndef J2RE
  !define J2RE "jre1.6.0_01"  ;Name of directory containing JRE to copy from, usually passed from build script
!endif
!ifndef J2RE_DIR
  ;Windows mapped to drive X:
  !define J2RE_DIR "X:\"
!endif
!ifndef J2RE_PATH
  !define J2RE_PATH "${J2RE_DIR}${J2RE}"   ;Full path to directory containing JRE (at .nsi compile-time)
!endif
!define COMPANY "Layer 7 Technologies"
!define MUI_PRODUCT "SecureSpan XML VPN Client" ;Define your own software name here

!ifndef MUI_VERSION
  ; Do not edit this version number
  ; If you want to build a different version use a flag:
  ;  /DMUI_VERSION=XXX
  !define MUI_VERSION "%%%BUILD_VERSION%%%"
!endif

!ifndef BUILD_DIR
  !define BUILD_DIR "..\..\..\build" ;UneasyRooster\build dir, root of jar files and things
!endif

!ifndef PACKAGE_REL
  !define PACKAGE_REL "..\installer\Client-${MUI_VERSION}"
!endif

!ifndef PACKAGE_DIR
  !define PACKAGE_DIR "${BUILD_DIR}\${PACKAGE_REL}"
!endif

!ifndef OUTPUT_DIR
  !define OUTPUT_DIR "."
!endif

!include "MUI.nsh"

;--------------------------------
;Configuration

  ;General
  OutFile "${OUTPUT_DIR}\${MUI_PRODUCT} ${MUI_VERSION} Installer.exe"

  ;Folder selection page
  InstallDir "$PROGRAMFILES\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"

  ;Remember install folder
  InstallDirRegKey HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" ""

;--------------------------------
;Modern UI Configuration

  !define MUI_LICENSEPAGE
 ; !define MUI_COMPONENTSPAGE
  !define MUI_DIRECTORYPAGE
  !define MUI_STARTMENUPAGE

  !define MUI_ABORTWARNING

  !define MUI_UNINSTALLER
  !define MUI_UNCONFIRMPAGE

  !define MUI_HEADERBITMAP "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

  ;Remember the Start Menu Folder
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU"
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Layer 7 SecureSpan XML VPN Client"

  !define TEMP $R0


;--------------------------------
;Languages

  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Language Strings

  ;Description
  LangString DESC_SecCopyUI ${LANG_ENGLISH} "Copy the SecureSpan XML VPN Client files to the application folder."

;--------------------------------
;Data

  LicenseData "License.txt"

  ReserveFile "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

; checks for previous installations and warns user if detected
Function CheckPreviousInstalls

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT}" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT} HEAD" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  DetailPrint "No previous installation of ${MUI_PRODUCT} detected."
  Goto done

  foundpreviousinstall:
    DetailPrint "existing ${MUI_PRODUCT} installation detected at ${TEMP}"
    MessageBox MB_YESNO "The ${MUI_PRODUCT} appears to be already installed on this system at location ${TEMP}. Do you want to continue anyway?" IDNO abortinstall
      MessageBox MB_OK "Make sure the current version is not running and click OK to continue."
        DetailPrint "Previous version of product detected but user chooses to proceed anyway."
        Goto done

  abortinstall:
    DetailPrint "Installation of ${MUI_PRODUCT} aborted due to previous installation detection."
    Abort

  done:
FunctionEnd


; Ensures that the "SecureSPan XML VPN Client" service is stopped, and unregistered 
Function StopAndUnregisterService

  ; Make sure service is stopped and removed first
  ExecWait 'sc stop "SecureSpan XML VPN Client"' $0
  DetailPrint "Stopping service returned with code $0"
  Sleep  1000

  ExecWait '"$INSTDIR\SSXVCService.exe" -uninstall "SecureSpan XML VPN Client"' $0
  DetailPrint "Removal of service returned with code $0"

FunctionEnd

;--------------------------------
;Installer Sections

Section "SecureSpan XML VPN Client" SecCopyUI
  ; First, let's check that the product was not already installed.
  Call CheckPreviousInstalls

  ; Before copying anything, let's stop the Windows service (if any)
  ExecWait 'sc stop "SecureSpan XML VPN Client"' $0
  DetailPrint "stopped windows service SSXVC. return code $0"
  Sleep 1000

  ;ADD YOUR OWN STUFF HERE!

  SetOutPath "$INSTDIR"
  File "${BUILD_DIR}\..\native\win32\peerident\Release\peerident.dll"
  File "${BUILD_DIR}\..\native\win32\Microsoft.VC80.CRT\Microsoft.VC80.CRT.manifest"
  File "${BUILD_DIR}\..\native\win32\Microsoft.VC80.CRT\msvcp80.dll"
  File "${BUILD_DIR}\..\native\win32\Microsoft.VC80.CRT\msvcr80.dll"
  File "${BUILD_DIR}\..\native\win32\msvcr71.dll"
  File "${MUI_PRODUCT}.exe"
  File "${MUI_PRODUCT}.ini"
  File "${MUI_PRODUCT}.bat"
  File "${MUI_PRODUCT} in Text Mode.bat"
  File "ssxvcconfig.bat"
  File "${PACKAGE_DIR}\Client.jar"
  File /r "${J2RE_PATH}"
  Rename "$INSTDIR\${J2RE}" "$INSTDIR\jre"
  File "${BUILD_DIR}\..\installer\proxy\win32\SSXVCService.exe"
  File "${BUILD_DIR}\..\installer\proxy\win32\enableKerberos.reg"
  File "${PACKAGE_DIR}\logging.properties"

  RMDir /r "$INSTDIR/lib"
  File /r "${PACKAGE_DIR}\lib"

  ;Store install folder, version installed
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" "" $INSTDIR
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT}" "" $INSTDIR
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT}" "version" ${MUI_VERSION}

  !insertmacro MUI_STARTMENU_WRITE_BEGIN

    ;Create shortcuts
    SetShellVarContext all
    CreateDirectory "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}"
    ; other shortcuts are installed based on whether the XML VPN Client is installed as a service or not.
    ;CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT} in Text Mode.lnk" "$INSTDIR\${MUI_PRODUCT} in Text Mode.bat" parameters "$INSTDIR\${MUI_PRODUCT}.exe" 3
    ;CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Uninstall SecureSpan XML VPN Client.lnk" "$INSTDIR\Uninstall.exe"
    SetShellVarContext current

  !insertmacro MUI_STARTMENU_WRITE_END

  ;Register with Add/Remove programs
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "DisplayName" "${MUI_PRODUCT} ${MUI_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "Publisher" "${COMPANY}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "URLInfoAbout" "http://www.layer7tech.com"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "DisplayVersion" "${MUI_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "NoModify" "1"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "NoRepair" "1"

  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  MessageBox MB_YESNO "Would you like the SecureSpan XML VPN Client to run as a Windows Service?" IDNO skipservice
    Call StopAndUnregisterService
    DetailPrint "SecureSpan XML VPN Client service will run using home directory  $INSTDIR"
    ExecWait '"$INSTDIR\ssxvcconfig.bat" /initSvcConf' $0
    IntCmpU $0 0 initOk initFailed initFailed
  initOk:
    ExpandEnvStrings $0 "%systemroot%\System32\Config\SystemProfile"
    ExecWait '"$INSTDIR\SSXVCService.exe" -install "SecureSpan XML VPN Client" "$INSTDIR\jre\bin\client\jvm.dll" -Djava.class.path="$INSTDIR\Client.jar" -Duser.home="$0" -Djava.library.path="$INSTDIR" -server -Dfile.encoding=UTF-8  -Dsun.net.inetaddr.ttl=10 -Dnetworkaddress.cache.ttl=10 -Xms128m -Xmx512m -Xss256k -start com.l7tech.client.Main -out "$INSTDIR\ssxvc_out.log" -err "$INSTDIR\ssxvc_err.log" -description "Layer 7 Technologies SecureSpan XML VPN Client"' $0
    IntCmpU $0 0 regservOk regservFailed regservFailed
  regservOk:
    MessageBox MB_YESNO "Would you like to configure the SecureSpan XML VPN Client now?" IDNO endofserviceinstall
        ExpandEnvStrings $0 "%systemroot%\System32\Config\SystemProfile"
        ExecWait '"$INSTDIR\jre\bin\javaw.exe" -Duser.home="$0" -Dfile.encoding=UTF-8  -Dsun.net.inetaddr.ttl=10 -Dnetworkaddress.cache.ttl=10 -Dcom.l7tech.proxy.listener.maxthreads=300 -jar "$INSTDIR\Client.jar" -config -hideMenus -quitLabel Continue' $0
        DetailPrint "XML VPN Client configuration returned with code $0"
    MessageBox MB_YESNO "Would you like to start the SecureSpan XML VPN Client service now?" IDNO endofserviceinstall
        ExecWait 'sc start "SecureSpan XML VPN Client"' $0
        DetailPrint "XML VPN Client service startup returned with code $0"
    goto endofserviceinstall

  initFailed:
    DetailPrint "initializing service config directory failed with code $0"
    MessageBox MB_OK "Initializing service config directory failed with code $0"
    Abort "Initializing service config directory failed with code $0"

  regservFailed:
    DetailPrint "registration of service failed with code $0"
    MessageBox MB_OK "registration of service failed with code $0"
    Abort "registration of service failed with code $0"

  ; choose shortcuts to installed based on whether it's being installed in service mode or GUI mode
  skipservice:
    SetShellVarContext all
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT}.lnk" "$INSTDIR\${MUI_PRODUCT}.exe" parameters "$INSTDIR\${MUI_PRODUCT}.exe" 0
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT} in Troubleshooting Mode.lnk" "$INSTDIR\${MUI_PRODUCT}.bat" parameters "$INSTDIR\${MUI_PRODUCT}.exe" 1
    SetShellVarContext current
    goto endofinstall

  endofserviceinstall:
    SetShellVarContext all
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Start ${MUI_PRODUCT}.lnk" "sc" 'start "SecureSpan XML VPN Client"' "$INSTDIR\${MUI_PRODUCT}.exe"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Stop ${MUI_PRODUCT}.lnk" "sc" 'stop "SecureSpan XML VPN Client"' "$INSTDIR\${MUI_PRODUCT}.exe"
    ExpandEnvStrings $0 "%systemroot%\System32\Config\SystemProfile"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT} Config.lnk" "$INSTDIR\jre\bin\javaw.exe" '-Dfile.encoding=UTF-8 -Duser.home="$0" -jar Client.jar -config' "$INSTDIR\${MUI_PRODUCT}.exe"
    SetShellVarContext current

  endofinstall:

SectionEnd

;Display the Finish header
;Insert this macro after the sections if you are not using a finish page
!insertmacro MUI_SECTIONS_FINISHHEADER

;--------------------------------
;Descriptions

!insertmacro MUI_FUNCTIONS_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecCopyUI} $(DESC_SecCopyUI)
!insertmacro MUI_FUNCTIONS_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ; Make sure service is stopped and removed first
  ExecWait 'sc stop "SecureSpan XML VPN Client"' $0
  DetailPrint "Stopping service returned with code $0"
  Sleep  1000
  ExecWait '"$INSTDIR\SSXVCService.exe" -uninstall "SecureSpan XML VPN Client"' $0
  DetailPrint "Removal of service returned with code $0"
  Sleep  1000

  ;ADD YOUR OWN STUFF HERE!

  Delete "$INSTDIR\${MUI_PRODUCT}.exe"
  Delete "$INSTDIR\${MUI_PRODUCT}.ini"
  Delete "$INSTDIR\${MUI_PRODUCT}.bat"
  Delete "$INSTDIR\${MUI_PRODUCT} in Text Mode.bat"
  Delete "$INSTDIR\ssxvcconfig.bat"
  Delete "$INSTDIR\Client.jar"
  Delete "$INSTDIR\peerident.dll"
  Delete "$INSTDIR\Microsoft.VC80.CRT.manifest"
  Delete "$INSTDIR\msvcp80.dll"
  Delete "$INSTDIR\msvcr80.dll"
  Delete "$INSTDIR\msvcr71.dll"
  ; DO NOT DELETE OR EDIT THIS LINE -- %%%JARFILE_DELETE_LINES%%%
  RMDir "$INSTDIR\lib"
  RMDir /r "$INSTDIR\jre"
  Delete "$INSTDIR\Uninstall.exe"
  Delete "$INSTDIR\SSXVCService.exe"
  Delete "$INSTDIR\logging.properties"
  Delete "$INSTDIR\enableKerberos.reg"

  ;Remove shortcut
  ReadRegStr ${TEMP} "${MUI_STARTMENUPAGE_REGISTRY_ROOT}" "${MUI_STARTMENUPAGE_REGISTRY_KEY}" "${MUI_STARTMENUPAGE_REGISTRY_VALUENAME}"

  StrCmp ${TEMP} "" noshortcuts

    SetShellVarContext all
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT}.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} in Troubleshooting Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} in Text Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} Config.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Uninstall ${MUI_PRODUCT}.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Start ${MUI_PRODUCT}.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Stop ${MUI_PRODUCT}.lnk"
    RMDir "$SMPROGRAMS\${TEMP}" ;Only if empty, so it won't delete other shortcuts
    SetShellVarContext current
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT}.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} in Troubleshooting Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} in Text Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} Config.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Uninstall ${MUI_PRODUCT}.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Start ${MUI_PRODUCT}.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Stop ${MUI_PRODUCT}.lnk"
    RMDir "$SMPROGRAMS\${TEMP}" ;Only if empty, so it won't delete other shortcuts

  noshortcuts:

  RMDir "$INSTDIR"
  RMDir "$PROGRAMFILES\${COMPANY}"

  DeleteRegKey HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  DeleteRegKey HKCU "Software\${COMPANY}\${MUI_PRODUCT}"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}"

  ;Display the Finish header
  !insertmacro MUI_UNFINISHHEADER

SectionEnd
