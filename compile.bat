@echo off
cls

echo [1/2] Compilando...
javac -d bin/ -cp "lib/sqlite-jdbc-3.50.3.0.jar;lib/weka.jar" src/com/erp/*.java -Werror -verbose -encoding UTF-8

if %errorlevel% neq 0 (
    echo.
    echo Falha na compilacao
    exit /b
)

echo.
echo [2/2] Empacotando...
jar cfe builds/erp.jar com.erp.Main -C bin/ .

echo.
echo Compilado com sucesso!
echo.