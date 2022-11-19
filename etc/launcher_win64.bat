:: SCRIPT TO EXECUTE THE FILE IN WINDOWS

@ECHO OFF

:: cache the current directory and move to the batch directory
:: to ensure the relative paths remain consistent
set CURRENT_DIRECTORY_BKP=%CD%
set CURRENT_BATCH_FILE_PATH=%~dp0

:: setup library environment variables
call "set_environment_variables_win64.bat"
@ECHO OFF

cd %CURRENT_BATCH_FILE_PATH%

set ABSOLUTE_JAR_PATH="%CD%\%JAR_NAME%"
set MODULE_PATH=%JAVA_LIB_PATH%;%JAVAFX_LIB_PATH%;%OPENCV_LIB_PATH%;%ABSOLUTE_JAR_PATH%

:: Set library path, containing dependency native libraries (.dll files)
set JAVA_LIBRARY_PATH=%JAVA_PATH%;%JAVAFX_NATIVE_LIBRARIES_PATH%;%OPENCV_NATIVE_LIBRARY_DIR%

set MAIN_MODULE=com.volpintesta.IBBIC
set MAIN_CLASS=com.volpintesta.IBBIC.ConverterApplication

echo JAVA_PATH = %JAVA_PATH%
echo MODULE_PATH = %MODULE_PATH%
echo JAVA_LIBRARY_PATH = %JAVA_LIBRARY_PATH%

:: return to the current directory
cd %CURRENT_DIRECTORY_BKP%

@ECHO ON

%JAVA_PATH%\java --module-path %MODULE_PATH% --add-modules %MAIN_MODULE% -Djava.library.path=%JAVA_LIBRARY_PATH% %MAIN_CLASS%
@ECHO OFF
set /p TEMP_INPUT_KEY=Hit ENTER to continue...
@ECHO ON
