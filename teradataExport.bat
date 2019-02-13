@echo off

set SERVER=myserver
set USER=myuser
set PWD=mypwd
set DB=mydb
set TABLE=mytable

java -cp bin;lib\tdgssconfig.jar;lib\terajdbc4.jar it.finsoft.TeradataExport "jdbc:teradata://%SERVER%/USER=%USER%,PASSWORD=%PWD%,DATABASE=%DB%"  "SELECT * FROM %TABLE%"
