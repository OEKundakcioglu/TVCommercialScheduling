rm -rf output
./gradlew runMipLoop -PyamlConfigPath=yamlConfigContinuous.yaml
./gradlew runMipLoop -PyamlConfigPath=yamlConfigDiscrete.yaml
./gradlew runGraspLoop -PyamlConfigPath=yamlConfigGrasp.yaml
./gradlew runBeeLoop -PyamlConfigPath=yamlConfigBee.yaml
