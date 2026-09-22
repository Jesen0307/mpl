//
// Maven Build module
//
def settings = CFG.'maven.settings_path' ? "-s '${CFG.'maven.settings_path'}'" : ''

sh 'git fetch --unshallow || true; git fetch origin master:refs/remotes/origin/master || true'
sh "mvn -B ${settings} -DskipTests -Dspotless.apply.skip=true -Dspotless.check.skip=true compile --no-transfer-progress"
sh "mkdir -p build && mvn -B ${settings} -Dspotless.apply.skip=true -Dspotless.check.skip=true dependency:build-classpath -Dmdep.outputFile=build/runtimeClasspath.txt --no-transfer-progress"

OUT.build_tool = 'maven'
OUT.classpath_file = 'build/runtimeClasspath.txt'