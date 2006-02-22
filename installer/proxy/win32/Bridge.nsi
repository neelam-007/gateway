;$Id$
;NSIS Modern User Interface version 1.63
;based on Basic Example Script, which was Written by Joost Verburg

!define J2RE "jre1.5.0_02"  ;Name of directory containing JRE
;Windows mapped drive X:
!define J2RE_PATH "X:\${J2RE}"   ;Full path to directory containing JRE (at .nsi compile-time)
!define COMPANY "Layer 7 Technologies"
!define MUI_PRODUCT "SecureSpan Bridge" ;Define your own software name here

; Edit this to set the version number in the build (is auto-edited by build.xml's OFFICIAL-build target)
!define MUI_VERSION "HEAD"

!define BUILD_DIR "..\..\..\build" ;UneasyRooster\build dir, root of jar files and things

!include "MUI.nsh"

;--------------------------------
;Configuration

  ;General
  OutFile "${MUI_PRODUCT} ${MUI_VERSION} Installer.exe"

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
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Layer 7 SecureSpan Bridge"

  !define TEMP $R0


;--------------------------------
;Languages

  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Language Strings

  ;Description
  LangString DESC_SecCopyUI ${LANG_ENGLISH} "Copy the SecureSpan Bridge files to the application folder."

;--------------------------------
;Data

  LicenseData "License.txt"

  ReserveFile "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

;--------------------------------
;Installer Sections

Section "SecureSpan Bridge" SecCopyUI
  ; Before copying anything, let's stop the Windows service (if any)
  ExecWait 'sc stop SSB' $0
  DetailPrint "stoped windows service SSB. return code $0"

  ;ADD YOUR OWN STUFF HERE!

  SetOutPath "$INSTDIR"
  File "${BUILD_DIR}\..\native\win32\systray4j.dll"
  File "${BUILD_DIR}\..\installer\proxy\win32\SSBService.exe"
  File "${MUI_PRODUCT}.exe"
  File "${MUI_PRODUCT}.ini"
  File "${MUI_PRODUCT}.bat"
  File "${MUI_PRODUCT} in Text Mode.bat"
  File "ssbconfig.bat"
  File "${BUILD_DIR}\Bridge.jar"
  File /r "${J2RE_PATH}"

  SetOutPath "$INSTDIR/lib"
  ; DO NOT DELETE OR EDIT THIS LINE - %%%JARFILE_FILE_LINES%%%
  SetOutPath "$INSTDIR"

  ;Store install folder
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" "" $INSTDIR

  !insertmacro MUI_STARTMENU_WRITE_BEGIN

    ;Create shortcuts
    CreateDirectory "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT}.lnk" "$INSTDIR\${MUI_PRODUCT}.exe" parameters "$INSTDIR\${MUI_PRODUCT}.exe"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT} Config.lnk" "$INSTDIR\${J2RE}\bin\javaw.exe" '-Dfile.encoding=UTF-8  -Dsun.net.inetaddr.ttl=10 -Dnetworkaddress.cache.ttl=10 -Dcom.l7tech.proxy.listener.maxthreads=300 -jar "$INSTDIR\Bridge.jar" -config -hideMenus -quitLabel Continue' "$INSTDIR\${MUI_PRODUCT}.exe"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT} in Troubleshooting Mode.lnk" "$INSTDIR\${MUI_PRODUCT}.bat" parameters "$INSTDIR\${MUI_PRODUCT}.exe"
    ;CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT} in Text Mode.lnk" "$INSTDIR\${MUI_PRODUCT} in Text Mode.bat" parameters "$INSTDIR\${MUI_PRODUCT}.exe" 3
    ;CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Uninstall SecureSpan Bridge.lnk" "$INSTDIR\Uninstall.exe"

  !insertmacro MUI_STARTMENU_WRITE_END

  ;Register with Add/Remove programs
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "DisplayName" "${MUI_PRODUCT}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "Publisher" "${COMPANY}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "URLInfoAbout" "http://www.layer7tech.com"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "DisplayVersion" "${MUI_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "NoModify" "1"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "NoRepair" "1"

  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  MessageBox MB_YESNO "Would you like the SecureSpan Bridge to run as a Windows Service?" IDNO endofinstall
    ReadEnvStr $0 HOMEDRIVE
    ReadEnvStr $1 HOMEPATH
    StrCpy $2 "$0$1"
    DetailPrint "SecureSpan Bridge service will run using home directory $2"
    ; create service, this is actually using a renamed version of JavaService.exe
    ExecWait '"$INSTDIR\SSBService.exe" -install SSB "$INSTDIR\jre1.5.0_02\bin\client\jvm.dll" -Djava.class.path="$INSTDIR\Bridge.jar" -Duser.home="$2" -start com.l7tech.proxy.Main -out "$INSTDIR\ssb_out.log" -err "$INSTDIR\ssb_err.log"' $0
    DetailPrint "creation of service returned with code $0"
    MessageBox MB_YESNO "Would you like to configure the SecureSpan Bridge now?" IDNO endofinstall
        ExecWait '"$INSTDIR\${J2RE}\bin\javaw.exe" -Dfile.encoding=UTF-8  -Dsun.net.inetaddr.ttl=10 -Dnetworkaddress.cache.ttl=10 -Dcom.l7tech.proxy.listener.maxthreads=300 -jar "$INSTDIR\Bridge.jar" -config -hideMenus -quitLabel Continue' $0
        DetailPrint "bridge configuration returned with code $0"
    MessageBox MB_YESNO "Would you like to start the SecureSpan Bridge service now?" IDNO endofinstall
        ExecWait 'sc start SSB' $0
        DetailPrint "bridge service startup returned with code $0"
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
  ExecWait 'sc stop SSB' $0
  DetailPrint "Stopping service returned with code $0"
  ExecWait '"$INSTDIR\SSBService.exe" -uninstall SSB' $0
  DetailPrint "Removal of service returned with code $0"

  ;ADD YOUR OWN STUFF HERE!

  Delete "$INSTDIR\${MUI_PRODUCT}.exe"
  Delete "$INSTDIR\${MUI_PRODUCT}.ini"
  Delete "$INSTDIR\${MUI_PRODUCT}.bat"
  Delete "$INSTDIR\${MUI_PRODUCT} in Text Mode.bat"
  Delete "$INSTDIR\ssbconfig.bat"
  Delete "$INSTDIR\Bridge.jar"
  Delete "$INSTDIR\systray4j.dll"
  ; DO NOT DELETE OR EDIT THIS LINE -- %%%JARFILE_DELETE_LINES%%%
  RMDir "$INSTDIR\lib"
  RMDir /r "$INSTDIR\${J2RE}"
  Delete "$INSTDIR\Uninstall.exe"
  Delete "$INSTDIR\SSBService.exe"

  ;Remove shortcut
  ReadRegStr ${TEMP} "${MUI_STARTMENUPAGE_REGISTRY_ROOT}" "${MUI_STARTMENUPAGE_REGISTRY_KEY}" "${MUI_STARTMENUPAGE_REGISTRY_VALUENAME}"

  StrCmp ${TEMP} "" noshortcuts

    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT}.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} in Troubleshooting Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} in Text Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Uninstall ${MUI_PRODUCT}.lnk"
    RMDir "$SMPROGRAMS\${TEMP}" ;Only if empty, so it won't delete other shortcuts

  noshortcuts:

  RMDir "$INSTDIR"
  RMDir "$PROGRAMFILES\${COMPANY}"

  DeleteRegKey /ifempty HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}"

  ;Display the Finish header
  !insertmacro MUI_UNFINISHHEADER

SectionEnd
