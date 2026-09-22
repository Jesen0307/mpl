// Clean containerized execution replacing tool() and withEnv()
sh 'chmod +x gradlew || true'

sh '''
  if [ -f "./gradlew" ]; then
    ./gradlew --console=plain classes printRuntimeClasspath --no-daemon
  else
    gradle --console=plain classes printRuntimeClasspath --no-daemon
  fi
'''

OUT.build_tool = 'gradle'