@echo off
echo Downloading dependencies...

if not exist "lib" mkdir lib

REM Download SQLite JDBC
if not exist "lib\sqlite-jdbc-3.42.0.0.jar" (
    echo Downloading SQLite JDBC...
    curl.exe -L "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar" -o "lib\sqlite-jdbc-3.42.0.0.jar"
)

REM Download JavaFX SDK JARs
if not exist "lib\javafx-controls-17.0.2-win.jar" (
    echo Downloading JavaFX Controls Win...
    curl.exe -L "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/17.0.2/javafx-controls-17.0.2-win.jar" -o "lib\javafx-controls-17.0.2-win.jar"
)

if not exist "lib\javafx-fxml-17.0.2-win.jar" (
    echo Downloading JavaFX FXML Win...
    curl.exe -L "https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/17.0.2/javafx-fxml-17.0.2-win.jar" -o "lib\javafx-fxml-17.0.2-win.jar"
)

if not exist "lib\javafx-graphics-17.0.2-win.jar" (
    echo Downloading JavaFX Graphics Win...
    curl.exe -L "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/17.0.2/javafx-graphics-17.0.2-win.jar" -o "lib\javafx-graphics-17.0.2-win.jar"
)

if not exist "lib\javafx-base-17.0.2-win.jar" (
    echo Downloading JavaFX Base Win...
    curl.exe -L "https://repo1.maven.org/maven2/org/openjfx/javafx-base/17.0.2/javafx-base-17.0.2-win.jar" -o "lib\javafx-base-17.0.2-win.jar"
)

echo.
echo Compiling Java sources...
if not exist "out" mkdir out
javac -d out -cp "lib\*" src\main\java\com\smartcoffee\*.java
copy src\main\resources\main_view.fxml out\

echo.
echo Running application...
java --module-path lib --add-modules javafx.controls,javafx.fxml -cp "out;lib\*" com.smartcoffee.MainApp
pause
