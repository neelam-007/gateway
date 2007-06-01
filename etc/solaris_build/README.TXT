Instructions to build & install package:
1. Linux side
	a. prepare a standard linux tarball, output file is ssg-b????.tar.gz
        b. prepare a tarball of the entire solaris_build dir:
   		cd UneasyRooster/etc/
   		tar -czvf solaris_build.tar.gz solaris_build
	c. copy both tarballs to the solaris build system
		scp ssg-b????.tar.gz user@solaris:
		scp solaris_build.tar.gz user@solaris:

2. Solaris side

* Must have gnu tar: package name is SUNWgtar on the dvd. Sun Tar mangles the filenames and the resulting file will not run

* Create a working directory "working":
	cd ~
	mkdir working
	*   On solaris this makes /export/home/username/working
  
* Unpack the solaris build kit:
	cd working
	gunzip ../solaris_build.tar.gz
        tar -xf ../solaris_build.tar
        mv ~/ssg-bNNNN.tar.gz . ( Where nnnn is the build number)

eg: 
[jay@jaysol working]$ ls -l
total 245170
drwxr-xr-x   6 jay      other        512 May 31 16:25 solaris_build
-rw-r--r--   1 jay      other  125448735 May 29 17:44 ssg-b3404.tar.gz


* update VERSION value
	vi solaris_build/pkgbuild/pkginfo
        edit VERSION value with "release number" accordingly (eg. VERSION="4.0m1"), and save the file
* run the build script
	./build.sh
	

3. Build script 
	1. makes the overlay tarball
	2. unpacks the main tarball 
	3. unpacks the overlay tarball, replacing the startup scripts.
	4. Then build runs and creates a package:
		 solaris_build/pkgbuild/L7TECHssg.pkg
	5. copy this file to repository
	
Theory of operation:

The script extracts the ssg tarball into pkgroot, moves some files around then creates listings for use with pkgmk.

This will produce a Sun package file in solaris_build/pkgbuild/L7TECHssg.pkg, this file is suitable for using with pkgadd.

You can install the package using 'pkgadd -d L7TECHssg.pkg', (the .pkg extension is not standard and is just used for developer's clarity)


Instruction to remove installed L7TECHssg package: pkgrm L7TECHssg

Bugs:
-> depends on Java 1.5, not 1.6. This may or may not be a problem
-> This filename contains spaces: "ssg/tomcat/webapps/ROOT/ssg/webadmin/help/securespan manager help system.log"
