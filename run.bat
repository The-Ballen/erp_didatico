@echo off
cls
java --enable-native-access=ALL-UNNAMED -cp "builds/erp.jar;lib/sqlite-jdbc-3.50.3.0.jar" com.erp.Main
