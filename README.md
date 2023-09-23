# Inhomogeneous Beam Brightness Intensity Converter (IBBIC)

Copyright © 2022-2023, Francesco Volpintesta, all rights reserved.

Copyright © 2022-2023, Giovanni Volpintesta, all rights reserved.

By downloading, copying, installing, or using the software, you agree
to the license agreement written in the attached "LICENSE.txt" file.
If you do not agree to that license, do not download, install,
copy or use the software.

## Introduction

This software is written to demonstrate the image analysis process discussed in the article
"Inhomogeneous Beam Brightness Intensity Converter (IBBIC) as aid for cathodoluminescence
microscopy studies and images pre-processing", written by Francesco Volpintesta
and Giovanni Volpintesta.

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

1) Download and extract the program zip file into a directory of your chose. Builds can be found, on GitHub, listed in the "tags" page. You can find a link to the "tags" list under the branch selector or under "Releases" on the right. Each "tag" refers to a build. Download the latest build.
	- NOTE 1: If you are not a developer, but a standard user, the zip file you downloaded should contain only an "IBBIC.jar" file and some ".bat" and text files.
		If this is not the case, you have downloaded the source project, which should be built, instead of a build, which can be executed.
		Try to download a build.	
	- NOTE 2: Only Windows 64-bit packages are provided.
		Both these packages and this user guide should be compatible with Windows 32-bit, but this has not been tested.
		If you are using a different operating system, you should build the package on your own, using the developer guide.

2) Download and install the Java 21 JRE from Bellsoft website (under GPU2 license with Classpath Exception):
	- go to Bellsoft website https://bell-sw.com/pages/downloads/
	- select the "JDK 21 LTS" versions
	- find the most suitable version for your computer configuration (e.g.: Windows, x86, 64 bit)
	- choose the "Standard JRE" package
	- download either the MSI installer (then install it) or the ZIP archive (then unzip it in a proper location).

3) Download and install the OpenJFX 21 SDK from the Gluon website:
	- go to Gluon website: https://gluonhq.com/products/javafx/
	- use the filters to select "21 LTS" as "JavaFX version" (the first filter) and "SDK" as "Type" (the fourth filter) 
	- use the other filters to find the most suitable version for your computer configuration (e.g.: Windows, x64)
	- download the resulting ZIP archive and unzip it in a proper location.

4) Download and install OpenCV 4.4.0 from the following website:
	- go to the OpenCV website: https://opencv.org/releases/
	- search through the releases the version 4.4.0. It could be that the version is not in the first page. Note that not all the versions are in order.
	- click the "Windows" button to be redirected to the installer download page, then install the software.
		
5) Open the file "setup_installation_directories_win64.bat" from the main directory of the IBBIC build and type the actual installation paths of the three libraries between the quotes. Don't edit anything else otherwise the software will not execute.
	- You should not double-click this file. You should open it with a simple text editor, such as Notepad or Notepad++, edit it, and save it again with the same name and extension.
	- For each installed software, the installation path is the directory which contains a subdirectory called "bin".

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
		- build_jar_win64.bat: a script used to build the final jar and to copy inside the build directory the necessary launcher scripts.
		- launcher_win64.bat: a script used to launch the built jar.
		- launcher_bats_filename_list.txt: a list of files that should be copied in the build directory (the launcher scripts, its dependencies and the LICENSE and README files).
	
2) Download and install the Java 21 JRE from Bellsoft website (under GPU2 license with Classpath Exception):
	- go to Bellsoft website https://bell-sw.com/pages/downloads/
	- select the "JDK 21 LTS" versions
	- find the most suitable version for your computer configuration (e.g.: Windows, x86, 64 bit)
	- choose the "Standard JRE" package
	- download either the MSI installer (then install it) or the ZIP archive (then unzip it in a proper location).

3) Download and install the OpenJFX 21 SDK from the Gluon website:
	- go to Gluon website: https://gluonhq.com/products/javafx/
	- use the filters to select "21 LTS" as "JavaFX version" (the first filter) and "SDK" as "Type" (the fourth filter) 
	- use the other filters to find the most suitable version for your computer configuration (e.g.: Windows, x64)
	- download the resulting ZIP archive and unzip it in a proper location.

4) Download and install OpenCV 4.4.0 from the following website:
	- go to the OpenCV website: https://opencv.org/releases/
	- search through the releases the version 4.4.0. It could be that the version is not in the first page. Note that not all the versions are in order.
	- click the "Windows" button to be redirected to the installer download page, then install the software.
		
5) Open the file "setup_installation_directories_win64.bat" inside the "etc" subdirectory
	and type the actual installation paths of the three libraries.
	- If you are working in an operating system different than Windows, use the informations in passage 1
		to understand the purpose of each script, and "translate" them on your own.
		
6) Double-click on the "build_jar_win64.bat" file to build the project.

### How to run:
		
If you correctly completed the install passages, you'll be able to launch the program by simply double-clicking the "launcher_win64.bat" file.
