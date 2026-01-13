@echo off
echo Starting ERP System...

set ERP_DB_HOST=192.168.40.35
set ERP_DB_USER=erp_user
set ERP_DB_PASS=122023!@#

cd /d %~dp0
java -jar erp-swing-1.0-SNAPSHOT.jar

pause
