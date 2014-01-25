@echo off

java -jar %~dp0\..\lib\${project.artifactId}-${project.version}.jar %*
