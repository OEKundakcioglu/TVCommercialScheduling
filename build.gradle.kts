
plugins {
    id ("java")
    id ("application")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}



group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.gurobi:gurobi:12.0.0")
    implementation ("com.google.code.gson:gson:2.10")
    implementation ("org.apache.logging.log4j:log4j-api:2.22.1")
    implementation ("org.apache.logging.log4j:log4j-core:2.22.1")
    implementation ("me.tongfei:progressbar:0.10.0")
    implementation("org.jetbrains:annotations:24.0.0")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.beust:jcommander:1.82")
    implementation("com.google.guava:guava:33.3.1-jre")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")


    project.file("input").mkdirs()
    project.file("output").mkdirs()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass = "mainGraspRun"
}

tasks.register<JavaExec>("runGraspLoop"){
    mainClass.set("mainConsoleLoop")

    classpath = sourceSets["main"].runtimeClasspath

    doFirst{

        val arglist = mutableListOf<String>()
        if (project.hasProperty("yamlConfigPath")){
            arglist.add("--yamlConfigPath=${project.property("yamlConfigPath")}")
        }

        if (project.hasProperty("ycp")){
            arglist.add("--ycp=${project.property("ycp")}")
        }
        println("Arguments: ${arglist.joinToString(" ")}")
        args = arglist
    }
}

tasks.register<JavaExec>("runBeeLoop"){
    mainClass.set("mainConsoleLoopBee")

    classpath = sourceSets["main"].runtimeClasspath

    doFirst{
        val arglist = mutableListOf<String>()
        if (project.hasProperty("yamlConfigPath")){
            arglist.add("--yamlConfigPath=${project.property("yamlConfigPath")}")
        }

        if (project.hasProperty("ycp")){
            arglist.add("--ycp=${project.property("ycp")}")
        }

        args = arglist
    }
}

tasks.register<JavaExec>("runMipLoop"){
    mainClass.set("mainConsoleMipLoop")

    classpath = sourceSets["main"].runtimeClasspath

    doFirst{
        val arglist = mutableListOf<String>()
        if (project.hasProperty("yamlConfigPath")){
            arglist.add("--yamlConfigPath=${project.property("yamlConfigPath")}")
        }

        if (project.hasProperty("ycp")){
            arglist.add("--ycp=${project.property("ycp")}")
        }

        args = arglist
    }
}

tasks.register<JavaExec>("runHeuristic") {
    mainClass.set("mainConsoleSingleInstance")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst {
        // Collect all properties passed from the command line
        val argsList = mutableListOf<String>()

        if (project.hasProperty("isBestMove")){
            argsList.add("--isBestMove=${project.property("isBestMove")}")
        }

        if (project.hasProperty("alphaGeneratorType")){
            argsList.add("--alphaGeneratorType=${project.property("alphaGeneratorType")}")
        }

        if (project.hasProperty("alphaGeneratorRange")){
            argsList.add("--alphaGeneratorRange=${project.property("alphaGeneratorRange")}")
        }

        if (project.hasProperty("instancePath")){
            argsList.add("--instancePath=${project.property("instancePath")}")
        }

        if (project.hasProperty("outputPath")){
            argsList.add("--outputPath=${project.property("outputPath")}")
        }

        if (project.hasProperty("timeLimit")){
            argsList.add("--timeLimit=${project.property("timeLimit")}")
        }

        // Print the arguments to the console
        println("Running with arguments: ${argsList.joinToString(" ")}")

        args = argsList
    }
}

tasks.register<JavaExec>("runMip") {
    mainClass.set("mainConsoleSingleInstanceMip")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst {
        // Collect all properties passed from the command line
        val argsList = mutableListOf<String>()

        if (project.hasProperty("instancePath")){
            argsList.add("--instancePath=${project.property("instancePath")}")
        }

        if (project.hasProperty("outputPath")){
            argsList.add("--outputPath=${project.property("outputPath")}")
        }

        if (project.hasProperty("timeLimit")){
            argsList.add("--timeLimit=${project.property("timeLimit")}")
        }

        // Print the arguments to the console
        println("Running with arguments: ${argsList.joinToString(" ")}")

        args = argsList
    }
}

tasks.register<JavaExec>("runGrasp") {
    mainClass.set("mainGraspRun")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst {
        // Collect all properties passed from the command line
        val argsList = mutableListOf<String>()

        if (project.hasProperty("instancePath")) {
            argsList.add("--instancePath=${project.property("instancePath")}")
        }

        if (project.hasProperty("outputPath")) {
            argsList.add("--outputPath=${project.property("outputPath")}")
        }

        if (project.hasProperty("timeLimit")) {
            argsList.add("--timeLimit=${project.property("timeLimit")}")
        }

        if (project.hasProperty("searchMode")) {
            argsList.add("--searchMode=${project.property("searchMode")}")
        }

        if (project.hasProperty("alphaType")) {
            argsList.add("--alphaType=${project.property("alphaType")}")
        }

        if (project.hasProperty("alpha")) {
            argsList.add("--alpha=${project.property("alpha")}")
        }

        if (project.hasProperty("minAlpha")) {
            argsList.add("--minAlpha=${project.property("minAlpha")}")
        }

        if (project.hasProperty("maxAlpha")) {
            argsList.add("--maxAlpha=${project.property("maxAlpha")}")
        }

        if (project.hasProperty("skipProbability")) {
            argsList.add("--skipProbability=${project.property("skipProbability")}")
        }

        if (project.hasProperty("localSearchMoves")) {
            argsList.add("--moves=${project.property("localSearchMoves")}")
        }

        if (project.hasProperty("seed")) {
            argsList.add("--seed=${project.property("seed")}")
        }


        if (project.hasProperty("verbose")) {
            argsList.add("--verbose=${project.property("verbose")}")
        }

        if (project.hasProperty("parallel")) {
            argsList.add("--parallel")
        }

        if (project.hasProperty("threads")) {
            argsList.add("--threads=${project.property("threads")}")
        }

        // Print the arguments to the console
        println("Running GRASP with arguments: ${argsList.joinToString(" ")}")

        args = argsList
    }
}

tasks.register<JavaExec>("runMipSolver") {
    mainClass.set("mainMipRun")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst {
        val argsList = mutableListOf<String>()

        if (project.hasProperty("instancePath")) {
            argsList.add("--instancePath=${project.property("instancePath")}")
        }

        if (project.hasProperty("outputPath")) {
            argsList.add("--outputPath=${project.property("outputPath")}")
        }

        if (project.hasProperty("modelType")) {
            argsList.add("--modelType=${project.property("modelType")}")
        }

        if (project.hasProperty("checkPointTimes")) {
            argsList.add("--checkPointTimes=${project.property("checkPointTimes")}")
        }

        if (project.hasProperty("logPath")) {
            argsList.add("--logPath=${project.property("logPath")}")
        }

        if (project.hasProperty("verbose")) {
            argsList.add("--verbose=${project.property("verbose")}")
        }

        println("Running MIP Solver with arguments: ${argsList.joinToString(" ")}")
        args = argsList
    }
}

tasks.register<JavaExec>("runBeeColony") {
    mainClass.set("mainBeeColonyRun")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst {
        val argsList = mutableListOf<String>()

        if (project.hasProperty("instancePath")) {
            argsList.add("--instancePath=${project.property("instancePath")}")
        }

        if (project.hasProperty("outputPath")) {
            argsList.add("--outputPath=${project.property("outputPath")}")
        }

        if (project.hasProperty("timeLimit")) {
            argsList.add("--timeLimit=${project.property("timeLimit")}")
        }

        if (project.hasProperty("populationSize")) {
            argsList.add("--populationSize=${project.property("populationSize")}")
        }

        if (project.hasProperty("alpha")) {
            argsList.add("--alpha=${project.property("alpha")}")
        }

        if (project.hasProperty("nIter")) {
            argsList.add("--nIter=${project.property("nIter")}")
        }

        if (project.hasProperty("T0")) {
            argsList.add("--T0=${project.property("T0")}")
        }

        if (project.hasProperty("seed")) {
            argsList.add("--seed=${project.property("seed")}")
        }

        if (project.hasProperty("verbose")) {
            argsList.add("--verbose=${project.property("verbose")}")
        }

        println("Running Bee Colony with arguments: ${argsList.joinToString(" ")}")
        args = argsList
    }
}

