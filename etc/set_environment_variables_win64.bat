@ECHO OFF

set CURRENT_BATCH_FILE_PATH=%~dp0
set SETUP_DIRECTORY_FILE=%~f1

:: Set environment variables for libraries intallation paths.
:: Set variables: JAVA_INSTALL_DIR, JAVAFX_INSTALL_DIR, OPENCV_INSTALL_DIR
call "%SETUP_DIRECTORY_FILE%"
@ECHO OFF

:: For developers: to build and launch the jar use the JDK installation path
:: instead than the JRE installation path.
set JDK_INSTALLATION_DIR=%JAVA_INSTALL_DIR%

set JAVA_PATH=%JDK_INSTALLATION_DIR%\bin
set JAVA_LIB_PATH=%JDK_INSTALLATION_DIR%\jmods

set OPENCV_LIB_PATH=%OPENCV_INSTALL_DIR%\java\opencv-440.jar
set OPENCV_NATIVE_LIBRARY_DIR=%OPENCV_INSTALL_DIR%\java\x64

set JAVAFX_NATIVE_LIBRARIES_PATH=%JAVAFX_INSTALL_DIR%\bin
set JAVAFX_LIB_PATH=%JAVAFX_INSTALL_DIR%\lib

set JAR_NAME=IBBIC.jar

@ECHO ON
