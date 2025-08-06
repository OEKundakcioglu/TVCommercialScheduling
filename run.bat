@echo off
call .\gradlew.bat runGraspLoop  -PyamlConfigPath=yamlConfigGrasp.yaml
call .\gradlew.bat runBeeLoop   -PyamlConfigPath=yamlConfigBee.yaml
@REM call .\gradlew.bat runMipLoop -PyamlConfigPath=yamlConfigDiscrete.yaml
@REM call .\gradlew.bat runMipLoop -PyamlConfigPath=yamlConfigContinuous.yaml
