plugins {
    id("java")
    id("checkstyle")
    id("net.ltgt.errorprone") version "5.1.0"
    id("com.diffplug.spotless") version "8.3.0"
    id("io.freefair.lombok") version "9.2.0"
    id("com.github.akazver.mapstruct") version "1.0.9"
    id("net.ltgt.nullaway") version "3.0.0"
    id("com.github.spotbugs") version "6.4.8"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

group = "tv-commercial-scheduling"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.1")
    implementation("info.picocli:picocli:4.7.6")
    implementation("org.slf4j:slf4j-api:2.0.17")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    // Source: https://mvnrepository.com/artifact/com.gurobi/gurobi
    implementation("com.gurobi:gurobi:13.0.0")

    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    errorprone("com.google.errorprone:error_prone_core:2.36.0")
    errorprone("com.uber.nullaway:nullaway:0.13.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

checkstyle {
    toolVersion = "13.3.0"
    configDirectory.set(file("config/checkstyle"))
    configProperties["org.checkstyle.google.suppressionfilter.config"] =
        file("config/checkstyle/checkstyle-suppressions.xml").absolutePath
}

tasks.withType<Checkstyle> {
    logging.captureStandardError(LogLevel.LIFECYCLE)
    logging.captureStandardOutput(LogLevel.LIFECYCLE)
}

spotless {
    java {
        googleJavaFormat().aosp()
        formatAnnotations()
    }
}

spotbugs {
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
    excludeFilter.set(file("config/spotbugs/exclusions.xml"))
}

nullaway {
    annotatedPackages.add("scheduling.dto")
    annotatedPackages.add("scheduling.model")
    annotatedPackages.add("scheduling.solver")
    annotatedPackages.add("scheduling.mapping")
    annotatedPackages.add("scheduling.solver.mip")
}

tasks.register("lint") {
    dependsOn("checkstyleMain", "checkstyleTest", "spotlessCheck", "compileJava", "spotbugsMain")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runGrasp") {
    mainClass.set("scheduling.GraspMain")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runBeeColony") {
    mainClass.set("scheduling.BeeColonyMain")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runMip") {
    mainClass.set("scheduling.MipMain")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runRelaxedMip") {
    mainClass.set("scheduling.RelaxedMIPMain")
    classpath = sourceSets["main"].runtimeClasspath
}
