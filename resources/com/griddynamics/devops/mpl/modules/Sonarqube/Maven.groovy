// Module: Sonarqube/Maven
// Runs SonarQube scan using the native Maven Sonar plugin.

def sonarHost = CFG.sonar_host ?: 'http://sonarqube:9000'
def sonarToken = CFG.sonar_token ?: 'squ_a76c5e818a392cb07370af0fb874c9e3fe84ec90'
def projectKey = CFG.project_key ?: env.JOB_BASE_NAME
def projectName = CFG.project_name ?: env.JOB_BASE_NAME

dir(CFG.workdir ?: '.') {
    sh 'mkdir -p target/sonar-reports'

    echo "[Sonarqube:Maven] Starting SonarQube analysis for project '${projectName}' on ${sonarHost}"

    def mvnCmd = fileExists('mvnw') ? './mvnw' : 'mvn'

    timeout(time: 20, unit: 'MINUTES') {
        sh """
            ${mvnCmd} sonar:sonar \
              -Dsonar.projectKey=${projectKey} \
              -Dsonar.projectName=${projectName} \
              -Dsonar.host.url=${sonarHost} \
              -Dsonar.token=${sonarToken}
        """
    }
}