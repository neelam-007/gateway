Summary: Tarari Support for SecureSpan Gateway
Name: ssg-tarari
Version: 3.7
Release: 1
Group: Applications/Internet
License: Copyright Layer7 Technologies. Portions copyright Tarari 2003-2006
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com>
Source0: /tmp/tarari.tar.gz
Source1: /tmp/tarari-kernel-drivers.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
requires: ssg >= 3.6
provides: ssg-tarari

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Tarari Support Package for SecureSpan Gateway
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
mkdir -p %{buildroot}/ssg/tomcat/shared/lib
cp %{buildroot}/usr/local/Tarari/lib/tarari_raxj.jar %{buildroot}/ssg/tomcat/shared/lib/

rm -rf %{buildroot}/usr/local/Tarari/test/
rm -rf %{buildroot}/usr/local/Tarari/src
rm -rf %{buildroot}/usr/local/Tarari/include
rm -rf %{buildroot}/usr/local/Tarari/docs

%files
%defattr(-,root,root)
/etc/init.d/tarari
/usr/local/Tarari/
/usr/local/Tarari/*

%defattr(-,gateway,gateway)
/ssg/tomcat/shared/lib/tarari_raxj.jar

%pre

%post
if [ -e "/etc/init.d/tarari" ]; then
    /bin/chmod -f +x /etc/init.d/tarari
fi

%preun

%changelog
* Thu Mar 9 2006 JWT
- Build First version - for 2.6.9-34 and Tarari build 64
- ME: build for any kernels contained in tarari-kernel-drivers.tar.gz. Change descriptions and chmod the startup script
