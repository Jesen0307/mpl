/**
 * Common build module
 */

boolean hasMaven = fileExists('pom.xml')
boolean hasGradle = fileExists('build.gradle') || fileExists('build.gradle.kts')

if (hasMaven) {
  MPLModule('Maven Build', CFG)
} else if (hasGradle) {
  MPLModule('Gradle Build', CFG)
} else {
  error("No build file found. Please add a pom.xml or build.gradle file to the project.")
}
