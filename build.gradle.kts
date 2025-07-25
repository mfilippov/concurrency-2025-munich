plugins {
    kotlin("jvm") version "2.2.0"
    java
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.lincheck:lincheck:3.1.1")
}

repositories {
    mavenCentral()
}

sourceSets.main {
    java.srcDir("src")
}

sourceSets.test {
    java.srcDir("test")
}

tasks {
    test {
        maxHeapSize = "10g"
        jvmArgs("-XX:+EnableDynamicAgentLoading")
    }
}

kotlin {
    jvmToolchain(21)
}
