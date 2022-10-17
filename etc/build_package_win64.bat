:: Path of the java binary files.
set JAVA_PATH="C:\Program Files\Java\openjdk-18.0.2.1\bin"

set OPENCV_LIB_PATH="C:\Program Files\opencv-4.4.0\build\java\opencv-440.jar"
set OPENCV_NATIVE_LIBRARY_DIR="C:\Program Files\opencv-4.4.0\build\java\x64"

:: set JAVAFX_NATIVE_LIBRARY_DIR="C:\Program Files\Java\javafx-sdk-17.0.1\bin"
set JAVAFX_LIB_PATH="C:\Program Files\Java\javafx-sdk-17.0.1\lib"

set PROJECT_RESOURCES_DIR="src\main\resources"
set LAUNCHER_PATH="etc\launcher_win64.bat"

set BUILD_DIR=".\build\win64"
set OPENCV_BUILD_PATH=".\opencv"
set OPENCV_LIB_BUILD_PATH=".\opencv"
set OPENCV_NATIVE_LIBRARY_DIR_BUILD_PATH=".\opencv\x64"
set LAUNCHER_BUILD_PATH=".\launcher.bat"

:: cache the current directory and move to the batch directory
:: to ensure the relative paths remain consistent
set CURRENT_DIRECTORY_BKP=%CD%
set CURRENT_BATCH_FILE_PATH=%~dp0
cd %CURRENT_BATCH_FILE_PATH%

:: move in the package output directory
cd ..
mkdir %BUILD_DIR%
cd %BUILD_DIR%

:: clear build directory
set BUILD_DIR_ABSOLUTE_PATH=%CD%
cd ..
rmdir /S /Q %BUILD_DIR_ABSOLUTE_PATH%
mkdir %BUILD_DIR_ABSOLUTE_PATH%
cd %BUILD_DIR_ABSOLUTE_PATH%
mkdir temp\classes

dir /B /S ..\..\src\*.java >> temp\sourceFileList.txt

:: compile java files into class files
%JAVA_PATH%\javac -d temp\classes --module-path %OPENCV_LIB_PATH%;%JAVAFX_LIB_PATH% @temp\sourceFileList.txt

:: copy resources into class files directory
xcopy ..\..\%PROJECT_RESOURCES_DIR% temp\classes /S /I /Y /R /G /F /C /V

:: Delete the test directories.
:: Warning: this will remove each directory named "tests",
:: whatever is its parent directory or its content
cd temp\classes
dir /A:D /B /S tests >> ..\classDirsToDelete.txt

cd ..
FOR /F "tokens=*" %%a in (classDirsToDelete.txt) do rmdir /S /Q "%%a"
cd ..

:: create the jar, packing class files and resources
%JAVA_PATH%\jar --create --verbose --file FoxCathodoLuminescenceCorrector.jar ^
--main-class foxinhead.foxcathodoluminescencecorrector.ConverterApplication ^
-C .\temp\classes .

:: copy the opencv library and its native library
mkdir %OPENCV_BUILD_PATH%
xcopy %OPENCV_LIB_PATH% %OPENCV_LIB_BUILD_PATH% /S /I /Y /R /G /F /C /V
xcopy %OPENCV_NATIVE_LIBRARY_DIR% %OPENCV_NATIVE_LIBRARY_DIR_BUILD_PATH% /S /I /Y /R /G /F /C /V

:: copy the launcher batch file
copy /V /Y ..\..\%LAUNCHER_PATH% %LAUNCHER_BUILD_PATH%

:: TODO: BUILD CUSTOM RUNTIME IMAGE
:: TODO: TEST THE LAUNCHER WITH THE RUNTIME IMAGE
:: TODO: EXPORT JAVA ENVIRONMENT VARIABLE IN A DIFFERENT BATCH FILE, CALLED BY THE BUILD PACKAGE BATCH AND THE LAUNCHER

:: remove temp dir
rmdir /S /Q temp

:: return to the old current directory
cd %CURRENT_DIRECTORY_BKP%
