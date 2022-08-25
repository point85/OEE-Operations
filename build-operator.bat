rem build operator web war
call mvn -v
call mvn clean
call mvn package
rem build distribution war file
call ant -f build.xml copy-war