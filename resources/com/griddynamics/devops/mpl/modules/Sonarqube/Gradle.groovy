// Module: Sonarqube/Gradle
// Runs SonarQube scan using sonar-scanner with Gradle binary output paths.

def sonarHost = CFG.sonar_host ?: 'http://sonarqube:9000'
def sonarToken = CFG.sonar_token ?: 'squ_a76c5e818a392cb07370af0fb874c9e3fe84ec90'
def projectKey = CFG.project_key ?: env.JOB_BASE_NAME
def projectName = CFG.project_name ?: env.JOB_BASE_NAME

dir(CFG.workdir ?: '.') {
    sh 'mkdir -p target/sonar-reports'

    // Dynamically locate Gradle compiled classes
    def javaBinaries = 'build/classes/java/main,build/classes'

    echo "[Sonarqube:Gradle] Starting SonarQube scan for project '${projectName}' on ${sonarHost}"
    echo "[Sonarqube:Gradle] Using java binaries target: ${javaBinaries}"

    timeout(time: 20, unit: 'MINUTES') {
        sh """
            sonar-scanner \
              -Dsonar.projectKey=${projectKey} \
              -Dsonar.projectName=${projectName} \
              -Dsonar.sources=src \
              -Dsonar.host.url=${sonarHost} \
              -Dsonar.token=${sonarToken} \
              -Dsonar.scanner.skipJreProvisioning=true \
              -Dsonar.java.binaries=${javaBinaries}
        """
    }
}