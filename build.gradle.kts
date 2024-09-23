import org.gradle.jvm.tasks.Jar
plugins {
    id ("java")
    id ("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    implementation("com.gurobi:gurobi:11.0.2")
    implementation ("com.google.code.gson:gson:2.10")
    implementation ("org.apache.logging.log4j:log4j-api:2.22.1")
    implementation ("org.apache.logging.log4j:log4j-core:2.22.1")
    implementation ("me.tongfei:progressbar:0.10.0")
    implementation("org.jetbrains:annotations:24.0.0")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.beust:jcommander:1.82")


    project.file("input").mkdirs()
    project.file("output").mkdirs()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass = "mainHeuristic"
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(listOf("compileJava", "processResources")) // We need this for Gradle optimization to work
        archiveClassifier.set("standalone") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
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




