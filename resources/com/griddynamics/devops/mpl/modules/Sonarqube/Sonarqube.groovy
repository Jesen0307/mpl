// Module: Sonarqube
// Handles both Maven and Gradle SonarQube scans in a single file.

def sonarHost = CFG.sonar_host ?: 'http://sonarqube:9000'
def sonarToken = CFG.sonar_token ?: 'squ_a76c5e818a392cb07370af0fb874c9e3fe84ec90'
def projectKey = CFG.project_key ?: env.JOB_BASE_NAME
def projectName = CFG.project_name ?: env.JOB_BASE_NAME

dir(CFG.workdir ?: '.') {
    sh 'mkdir -p target/sonar-reports'

    timeout(time: 20, unit: 'MINUTES') {
        if (fileExists('pom.xml')) {
            echo "[Sonarqube] Running SonarQube analysis for Maven project '${projectName}'..."
            def mvnCmd = fileExists('mvnw') ? './mvnw' : 'mvn'
            sh """
                ${mvnCmd} sonar:sonar \
                  -Dsonar.projectKey=${projectKey} \
                  -Dsonar.projectName=${projectName} \
                  -Dsonar.host.url=${sonarHost} \
                  -Dsonar.token=${sonarToken}
            """
        } else if (fileExists('build.gradle') || fileExists('build.gradle.kts')) {
            echo "[Sonarqube] Running SonarQube analysis for Gradle project '${projectName}'..."
            def javaBinaries = 'build/classes/java/main,build/classes'
            sh """
                sonar-scanner \
                  -Dsonar.projectKey=${projectKey} \
                  -Dsonar.projectName=${projectName} \
                  -Dsonar.sources=src \
                  -Dsonar.host.url=${sonarHost} \
                  -Dsonar.token=${sonarToken} \
                  -Dsonar.scanner.skipJreProvisioning=false \
                  -Dsonar.java.binaries=${javaBinaries}
            """
        } else {
            error '[Sonarqube] Failed to detect build system! Neither pom.xml nor build.gradle were found.'
        }
    }
}