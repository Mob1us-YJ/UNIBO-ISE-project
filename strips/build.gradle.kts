plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

javafx {
    version = "17.0.2"
    modules("javafx.controls", "javafx.fxml")
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform(libs.kotlin.bom))
    // Use the Kotlin JDK 8 standard library.
    implementation(libs.kotlin.stdlib.jvm)

    implementation(libs.tuprolog.solve.classic)
    implementation(libs.tuprolog.parser.theory)

    // JavaFX dependencies
    implementation("org.openjfx:javafx-controls:17.0.2")
    implementation("org.openjfx:javafx-fxml:17.0.2")
    
    // JSON for saving/loading configurations
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")

    // Use the Kotlin test library.
    testImplementation(libs.kotlin.test)
    // Use the Kotlin JUnit integration.
    testImplementation(libs.kotlin.test.junit)
}

application {
    mainClass.set("it.unibo.ise.lab.strips.Main")
}

task<JavaExec>("runConsole") {
    dependsOn("assemble")
    group = "run"
    sourceSets {
        main {
            classpath = runtimeClasspath
        }
    }
    mainClass.set("it.unibo.ise.lab.strips.Main")
    standardInput = System.`in`

    val arguments = mutableListOf<String>().also {
        if (properties.containsKey("verbose") && properties["verbose"] == "true") {
            it.add("--verbose")
        }
        if (properties.containsKey("initialState")) {
            it.addAll(listOf("--initialState", "${properties["initialState"]}"))
        }
        if (properties.containsKey("goal")) {
            it.addAll(listOf("--goal", "${properties["goal"]}"))
        }
        if (properties.containsKey("world")) {
            it.addAll(listOf("--world", "${properties["world"]}"))
        }
        if (properties.containsKey("maxDepth")) {
            it.addAll(listOf("--maxDepth", "${properties["maxDepth"]}"))
        }
    }

    args = arguments

    doFirst {
        println("Running `${mainClass.get()}` with arguments `${arguments.joinToString(" ")}`")
    }
}
