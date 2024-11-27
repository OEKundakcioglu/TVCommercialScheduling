#rm -rf newOutput
./gradlew runGraspLoop -PyamlConfigPath=yamlConfigGrasp.yaml
./gradlew runBeeLoop -PyamlConfigPath=yamlConfigBee.yaml
./gradlew runMipLoop -PyamlConfigPath=yamlConfigDiscrete.yaml
./gradlew runMipLoop -PyamlConfigPath=yamlConfigContinuous.yaml
#