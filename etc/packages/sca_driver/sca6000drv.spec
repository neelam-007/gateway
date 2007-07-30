Summary: SCA 6000 Support for SecureSpan Gateway
Name: SCA6000drv
Version: 1.0
Release: 3
Group: Applications/Internet
License: GPL
URL: http://www.layer7tech.com
Packager: Layer 7 Technologies, <support@layer7tech.com>
Source0: /tmp/sca6000drv.tgz
buildroot: %{_builddir}/%{name}-%{version}
provides: sca6000drv-l7
requires: kernel-smp >= 2.6.9-55.EL
# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Opencryptoki modified version to use Sun SCA6000 card

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
mkdir %{buildroot}
cd %{buildroot}
tar -xzf /tmp/sca6000drv.tgz

%build

%files
%defattr(-,root,root)
/opt
%pre

%post

%preun

%changelog
* Thu Jul 10 2007 JWT
- Rebuild with better version number
* Thu Jun 7 2007 JWT
- Build First version - binaries only
