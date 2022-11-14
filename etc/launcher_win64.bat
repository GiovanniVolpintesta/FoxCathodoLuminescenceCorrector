:: SCRIPT TO EXECUTE THE FILE IN WINDOWS

@ECHO OFF

:: cache the current directory and move to the batch directory
:: to ensure the relative paths remain consistent
set CURRENT_DIRECTORY_BKP=%CD%
set CURRENT_BATCH_FILE_PATH=%~dp0
cd %CURRENT_BATCH_FILE_PATH%

set USE_CUSTOM_RUNTIME_IMAGE=1

:: Path of the java binary files.
:: Used if USE_CUSTOM_RUNTIME_IMAGE is 1.
set CUSTOM_RUNTIME_IMAGE_BIN_PATH=".\java_runtime\bin"
set CUSTOM_RUNTIME_IMAGE_LIB_PATH=".\java_runtime\lib"

:: Path of the java binary files.
:: Used if USE_CUSTOM_RUNTIME_IMAGE is not 1.
set JAVA_LIB_PATH="C:\Program Files\Java\openjdk-18.0.2.1\jmods"
set JAVA_NATIVE_LIBRARIES_PATH="C:\Program Files\Java\openjdk-18.0.2.1\bin"

:: Path of the javafx binary files
:: Used if USE_CUSTOM_RUNTIME_IMAGE is not 1.
set JAVAFX_LIB_PATH="C:\Program Files\Java\javafx-sdk-19\lib"
set JAVAFX_NATIVE_LIBRARIES_PATH="C:\Program Files\Java\javafx-sdk-19\bin"

:: Path to the OpenCV jar file built for java bindings
set OPENCV_JAR_PATH=".\opencv\opencv-440.jar"

:: Directory containing the OpenCV native library file built for java bindings
:: NOTE 1: in windows it's the directory containing "opencv_java440.dll"
:: NOTE 2: use the directory containing the correct native library file, depending on the system (x86, x64, etc)
set OPENCV_NATIVE_LIBRARY_DIR=".\opencv\x64"

:: Path of the program jar
set PROGRAM_JAR_PATH=".\FoxCathodoLuminescenceCorrector.jar"

:: If present, the custom runtime image contains Java SE and JavaFX modules.
:: OpenCV modules are not included because its .jar library is not modular, and it cannot be exported to a .jmod file.
:: So, both the program and the OpenCV modules are added indipendently to the paths.

:: Set java binaries path, containing java execurtables (.exe files, with their dependency .dll files)
if %USE_CUSTOM_RUNTIME_IMAGE%==1 (set JAVA_PATH=%CUSTOM_RUNTIME_IMAGE_BIN_PATH%) ^
else (set JAVA_PATH=%JAVA_NATIVE_LIBRARIES_PATH%)

:: Set module path, containing dependency libs (.class, .jar, .jmod files)
if %USE_CUSTOM_RUNTIME_IMAGE%==1 (set MODULE_PATH=%CUSTOM_RUNTIME_IMAGE_LIB_PATH%) ^
else (set MODULE_PATH=%JAVA_LIB_PATH%;%JAVAFX_LIB_PATH%)
set MODULE_PATH=%MODULE_PATH%;%OPENCV_JAR_PATH%;%PROGRAM_JAR_PATH%

:: Set library path, containing dependency native libraries (.dll files)
if %USE_CUSTOM_RUNTIME_IMAGE%==1 (set JAVA_LIBRARY_PATH=%CUSTOM_RUNTIME_IMAGE_BIN_PATH%) ^
else (set JAVA_LIBRARY_PATH=%JAVA_NATIVE_LIBRARIES_PATH%;%JAVAFX_NATIVE_LIBRARIES_PATH%)
set JAVA_LIBRARY_PATH=%JAVA_LIBRARY_PATH%;%OPENCV_NATIVE_LIBRARY_DIR%

set MAIN_MODULE=foxinhead.foxcathodoluminescencecorrector
set MAIN_CLASS=foxinhead.foxcathodoluminescencecorrector.ConverterApplication

echo JAVA_PATH = %JAVA_PATH%
echo MODULE_PATH = %MODULE_PATH%
echo JAVA_LIBRARY_PATH = %JAVA_LIBRARY_PATH%

@ECHO ON

%JAVA_PATH%\java --module-path %MODULE_PATH% --add-modules %MAIN_MODULE% -Djava.library.path=%JAVA_LIBRARY_PATH% %MAIN_CLASS%

@ECHO OFF
:: return to the old current directory
cd %CURRENT_DIRECTORY_BKP%
@ECHO ON