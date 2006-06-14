Summary: Tari Support for Secure Span Gateway
Name: ssg-tarari
Version: 2.6.9.34
Release: 64
Group: Applications/Internet
License: Copyright Layer7 Technologies. Portions copyright Tarari 2003-2006
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com> 
Source0: /tmp/tarari.tar.gz
Source1: /tmp/tarari-kernel-drivers.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
requires: ssg
provides: ssg-tarari

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Tarari Support Package for Secure Span Gateway 
Include RaxJ, base package, crypto, xmlcp, etc
Adds scripts. Version support is specific: 
Release conforms to Tarari version, version conforms to kernel

%clean 
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
mkdir %{buildroot}
cd %{buildroot}
tar -xzf /tmp/tarari.tar.gz
tar -xzf /tmp/tarari-kernel-drivers.tar.gz

%build
mkdir %{buildroot}/etc/init.d/

mv %{buildroot}/etc/tarari-initd %{buildroot}/etc/init.d/tarari

%files 
%defattr(-,root,root)
/etc/init.d/tarari
/usr/local/Tarari/*

%pre

%post

%preun

%changelog 
* Thu Mar 9 2006 JWT 
- Build First version - for 2.6.9-34 and Tarari build 64
