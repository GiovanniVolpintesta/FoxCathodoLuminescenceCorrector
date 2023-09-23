@ECHO OFF

set PROJECT_RESOURCES_DIR="src\main\resources"
set /p BUILD_VERSION=<"..\version.txt"
set BUILD_DIR=".\builds\IBBIC-%BUILD_VERSION%-win64"

:: cache the current directory and move to the batch directory
:: to ensure the relative paths remain consistent
set CURRENT_DIRECTORY_BKP=%CD%
set CURRENT_BATCH_FILE_PATH=%~dp0
cd %CURRENT_BATCH_FILE_PATH%

:: setup library environment variables
call ".\set_environment_variables_win64.bat" ".\setup_installation_directories_to_build_win64.bat"

:: move back to project directory
cd ..

:: create the current build directory
mkdir %BUILD_DIR%

:: move to the current build directory and store its absolute path
cd %BUILD_DIR%
set BUILD_DIR_ABSOLUTE_PATH=%CD%

:: move back to all builds directory
cd ..

:: clear the current build directory
rmdir /S /Q %BUILD_DIR_ABSOLUTE_PATH%
mkdir %BUILD_DIR_ABSOLUTE_PATH%

:: move to the current build directory
cd %BUILD_DIR_ABSOLUTE_PATH%

:: create temp directory and tempo class directory for the current build
mkdir temp\classes

:: compile java files into class files
dir /B /S ..\..\src\*.java >> temp\sourceFileList.txt
@ECHO ON
%JAVA_PATH%\javac -d temp\classes --module-path %OPENCV_LIB_PATH%;%JAVAFX_LIB_PATH% @temp\sourceFileList.txt
@ECHO OFF

:: copy resources into class files directory
xcopy ..\..\%PROJECT_RESOURCES_DIR% temp\classes /S /I /Y /R /G /F /C /V

:: Delete test directories.
:: Warning: this will remove each directory named "tests",
:: whatever is its parent directory or its content
cd temp\classes
dir /A:D /B /S tests >> ..\classDirsToDelete.txt

:: move back to temp directory
cd ..
FOR /F "tokens=*" %%a in (classDirsToDelete.txt) do rmdir /S /Q "%%a"
:: move back to the current build directory
cd ..

:: create the jar, packing class files and resources
@ECHO ON
%JAVA_PATH%\jar --create --verbose --file IBBIC.jar ^
--main-class com.volpintesta.IBBIC.ConverterApplication ^
-C .\temp\classes .
@ECHO OFF

:: copy the launcher batch file
:: TODO: copy all necessary bat files
FOR /F "tokens=*" %%a in (..\..\etc\launcher_bats_filename_list.txt) do copy /V /Y "..\..\etc\%%a" .

:: remove temp dir
rmdir /S /Q temp

:: return to the old current directory
cd %CURRENT_DIRECTORY_BKP%

echo Build finished (see logs above).
set /p TEMP_INPUT_KEY=Hit ENTER to continue...

@ECHO ON
