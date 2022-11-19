# Inhomogeneous Beam Brightness Intensity Converter (IBBIC)

Copyright © 2022-2023, Giovanni Volpintesta, all rights reserved.

Copyright © 2022-2023, Francesco Volpintesta, all rights reserved.

By downloading, copying, installing, or using the software, you agree
to the license agreement written in the attached "LICENSE.txt" file.
If you do not agree to that license, do not download, install,
copy or use the software.

## Introduction

This software is written to demonstrate the image analysis process discussed in the article
"Inhomogeneous Beam Brightness Intensity Converter (IBBIC) as aid for cathodoluminescence
microscopy studies and images pre-processing", written by Francesco Volpintesta.

This software lets the user open an image and visualize the conversion discussed in the article.

This software also lets the user change some conversion parameters, such as the blur filter radius
and the application of the dark noise correction, visualize the brightness intensity filter (which should
ideally result in an approximation of the radiance gradient of the cathodoluminescence microscopy), and
apply a threshold test to the original and the resulting image to confront how the two images respond to
a change in the threshold value.

There is the possibility to open a whole directory and to apply the same conversion to all the images in
the directory.

Finally, the open image or all the images inside the open directory can be converted and saved in the same
image format. In this case, the conversion parameters will be applied to all the images, but the selected
preview type (obviously) won't influence the saved images, which are always the results of the conversion
discussed in the article.



## User Guide

### How to install:

1) Extract the program zip file into a directory of your chose.
	- NOTE 1: If you are not a developer, but a standard user, the zip file you downloaded should contain only an "IBBIC.jar" file and some ".bat" and text files.
		If this is not the case, you have downloaded the source project, which should be built, instead of a released package, which can be executed.
		Try to download a release package.	
	- NOTE 2: Only Windows 64-bit packages are provided.
		Both these packages and this user guide should be compatible with Windows 32-bit, but this has not been tested.
		If you are using a different operating system, you should build the package on your own, using the developer guide.

2) Download Java from the following website:
	- Oracle JRE (under Oracle Technology Network license): https://www.java.com/it/download/

3) Download a Java FX from any of the following websites:
	- Oracle Java FX: https://wiki.openjdk.org/display/OpenJFX
	- Gluon Java FX: https://openjfx.io/

4) Download OpenCV from the following website:
	- OpenCV: https://opencv.org/releases/
		
5) Install the three downloaded libraries, or extract their compressed archives.
	- Be sure you have downloaded the correct library version for your operating system configuration before proceeding with the installation.

6) Open the file "setup_installation_directories_win64.bat" from this program directory and type the actual installation paths of the three libraries.
	- NOTE: You should not double-click this file.
		You should open it with a simple text editor, such as Notepad or Notepad++, edit it, and save it again with the same extension.

### How to run:
		
If you correctly completed the install passages, you'll be able to launch the program by simply double-clicking the "launcher_win64.bat" file.


## Developer Guide

### How to build:

1) Fork or download the project.
	- Mind that only Windows build scripts are currently included in the project, so you should "translate" them on you own
		if you want to compile the project using another operating system.
	- The source code and the graphic resources are inside the "src" subdirectory.
	- The build scripts are under the "etc" subdirectory and include:
		- setup_installation_directories_win64.bat: a script used to set the library installation paths inside some environment variables.
		- set_environment_variables_win64.bat: a script used to set all the common environment variables used in the build and launch scripts.
		- build_jar_win64: a script used to build the final jar and to copy inside the build directory the necessary launcher scripts.
		- launcher_win64: a script used to launch the built jar.
		- launcher_bats_filename_list.txt: a list of scripts that should be copied in the build directory (the launcher scripts, and its dependencies).
	
2) Download and install/unzip the Java JDK from any of the following websites.
	- Oracle JDK (under Oracle NTFC license): https://www.oracle.com/java/technologies/downloads/
	- Oracle OpenJDK (under GNU GPLv2 license with Classpath Exception): https://openjdk.org/

3) Download and install/unzip the Java FX SDK from any of the following websites.
	- Oracle Java FX: https://wiki.openjdk.org/display/OpenJFX
	- Gluon Java FX: https://openjfx.io/

3) Download and install/unzip OpenCV from the following website.
	- OpenCV: https://opencv.org/releases/
		
5) Open the file "setup_installation_directories_win64.bat" inside the "etc" subdirectory
	and type the actual installation paths of the three libraries.
	- If you are working in an operating system different than Windows, use the informations in passage 1
		to understand the purpose of each script, and "translate" them on your own.
		
6) Double-click on the "build_jar_win64.bat" file to build the project.

### How to run:
		
If you correctly completed the install passages, you'll be able to launch the program by simply double-clicking the "launcher_win64.bat" file.
