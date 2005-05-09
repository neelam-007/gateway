Summary: Tarari Driver Kit
Name: tarari
Version: 4.1
Release: 1
Group: Applications/Internet
Copyright: Copyright Tarari
URL: http://www.tarari.com
Packager: Layer7 Technologies, <support@layer7tech.com> 
Source0: /tmp/Tarari.tgz
buildroot: %{_builddir}/%{name}-%{version}
provides: tarari_driver

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
SSG software distribution on standard system
Does: ssg, network config, profiles
Modifies startup config to run only expected services

%clean 
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
mkdir %{buildroot}
cd %{buildroot}
tar -xzf /tmp/Tarari.tgz

%build
find . -type d | xargs chmod 755

%files 
%defattr(-,root,root)
/usr/local/Tarari/


%changelog 

* Mon May 09 2005 JWT
- Build 1. Package so we can get some version control and dependencies
