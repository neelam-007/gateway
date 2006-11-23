Summary: Logo for splash. 
Name: layer7-logo
Version: 1
Release: 1
Group: Applications/Internet
License: Copyright Layer7 Technologies
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com>
Source0: splash.xpm.gz
buildroot: %{_builddir}/%{name}-%{version}
provides: system-logos

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Provides a system logo for the grub screen

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
mkdir %{buildroot}

%build
mkdir -p %{buildroot}/boot/grub
mv splash.xpm.gz %{buildroot}/boot/grub

%files
%defattr(-,root,root)
/boot


%pre

%post

%preun

%changelog
* Thu Nov 23 2006 JWT
- Build First version
