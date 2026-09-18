def jdkTool = tool(CFG.'jdk.version' ?: 'JDK 17')
def gradleTool = tool(CFG.'gradle.tool_version' ?: 'Gradle 8')

withEnv([
  "JAVA_HOME=${jdkTool}",
  "PATH+JDK=${jdkTool}/bin",
  "PATH+GRADLE=${gradleTool}/bin"
]) {
  sh 'chmod +x gradlew || true'
  sh 'gradle --console=plain classes printRuntimeClasspath --no-daemon'
}

OUT.build_tool = 'gradle'