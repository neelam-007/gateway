Summary: Tarari Support for SecureSpan Gateway
Name: opencryptoki
Version: l7
Release: 1
Group: Applications/Internet
License: GPL
URL: http://www.layer7tech.com
Packager: Layer 7 Technologies, <support@layer7tech.com>
Source0: /tmp/opencrypto.tgz
buildroot: %{_builddir}/%{name}-%{version}
provides: opencrypto-l7

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
tar -xzf /tmp/opencrypto.tgz

%build

%files
%defattr(-,root,root)
/usr
%pre

%post

%preun

%changelog
* Thu Jun 7 2007 JWT
- Build First version - binaries only
