// Module: Sonarqube
// Orchestrates SonarQube analysis by auto-detecting Maven vs Gradle projects.

if (fileExists('pom.xml')) {
    echo '[Sonarqube] Detected Maven project (pom.xml found). Routing to Sonarqube/Maven...'
    MPLModule('Maven')
} else if (fileExists('build.gradle') || fileExists('build.gradle.kts')) {
    echo '[Sonarqube] Detected Gradle project (build.gradle found). Routing to Sonarqube/Gradle...'
    MPLModule('Gradle')
} else {
    error '[Sonarqube] Failed to detect build system! Neither pom.xml nor build.gradle/build.gradle.kts were found.'
}