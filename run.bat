@echo off
call ./gradlew runMipSolver -PinstancePath="instances\density=MEDIUM_nInventory=20_nHours=5_seed=9.json" -PoutputPath="August7/outputMip/continuous\density=MEDIUM_nInventory=20_nHours=5_seed=9.json\solution.json" -PmodelType="Continuous" -PcheckPointTimes="30,60,120,300,600,900,1800,3600,7200" -PlogPath="August7/outputMip/continuous\density=MEDIUM_nInventory=20_nHours=5_seed=9.json\mip.log"
@REM call .\gradlew.bat runGrasp -PinstancePath="instances/1.json" -PoutputPath="testCLI/solution.json" -PtimeLimit=1 -PsearchMode="BEST_IMPROVEMENT" -PalphaType="FIXED" -Palpha=0.25 -PskipProbability=0.0 -Pseed=42
@REM call .\gradlew runBeeColony -PinstancePath="instances/1.json" -PoutputPath="output/bee_solution.json" -PtimeLimit=1 -PpopulationSize=50 -Palpha=0.8 -PnIter=10 -PT0=100.0 -Pseed=42



@REM call .\gradlew.bat runGraspLoop  -PyamlConfigPath=yamlConfigGrasp.yaml
@REM call .\gradlew.bat runBeeLoop   -PyamlConfigPath=yamlConfigBee.yaml
@REM call .\gradlew.bat runMipLoop -PyamlConfigPath=yamlConfigDiscrete.yaml
@REM call .\gradlew.bat runMipLoop -PyamlConfigPath=yamlConfigContinuous.yaml
