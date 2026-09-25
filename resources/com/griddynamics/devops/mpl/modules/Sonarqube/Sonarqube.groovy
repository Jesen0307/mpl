// Module: Sonarqube
// Handles Maven and Gradle SonarQube scans and exports all security issues to sonar.json

def sonarHost   = CFG.sonar_host ?: 'http://sonarqube:9000'
def sonarToken  = CFG.sonar_token ?: 'squ_a76c5e818a392cb07370af0fb874c9e3fe84ec90'
def projectKey  = CFG.project_key ?: env.JOB_BASE_NAME
def projectName = CFG.project_name ?: env.JOB_BASE_NAME
def outputDir   = CFG.output_dir ?: 'target/sonar-reports'

dir(CFG.workdir ?: '.') {
    sh "mkdir -p ${outputDir}"

    timeout(time: 20, unit: 'MINUTES') {
        if (fileExists('pom.xml')) {
            echo "[Sonarqube] Running SonarQube analysis for Maven project '${projectName}'..."
            def mvnCmd = fileExists('mvnw') ? './mvnw' : 'mvn'
            sh """
                ${mvnCmd} sonar:sonar \
                  -Dsonar.projectKey=${projectKey} \
                  -Dsonar.projectName='${projectName}' \
                  -Dsonar.host.url=${sonarHost} \
                  -Dsonar.token=${sonarToken}
            """
        } else if (fileExists('build.gradle') || fileExists('build.gradle.kts')) {
            echo "[Sonarqube] Running SonarQube analysis for Gradle project '${projectName}'..."
            def javaBinaries = 'build/classes/java/main,build/classes'
            sh """
                sonar-scanner \
                  -Dsonar.projectKey=${projectKey} \
                  -Dsonar.projectName='${projectName}' \
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

    // Export all security issues with pagination
    echo "[Sonarqube] Fetching all security issues and saving to ${outputDir}/sonar.json..."
    sh """
        python3 -c '
import json, urllib.request, base64

host = "${sonarHost}"
token = "${sonarToken}"
project_key = "${projectKey}"
out_path = "${outputDir}/sonar.json"

page_size = 500
page = 1
all_issues = []

auth_header = "Basic " + base64.b64encode(f"{token}:".encode()).decode()

while True:
    url = f"{host}/api/issues/search?componentKeys={project_key}&impactSoftwareQualities=SECURITY&ps={page_size}&p={page}"
    req = urllib.request.Request(url)
    req.add_header("Authorization", auth_header)
    
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
    
    issues = data.get("issues", [])
    all_issues.extend(issues)
    
    total = data.get("total", 0)
    if not issues or page * page_size >= total:
        break
    page += 1

result = {"total": len(all_issues), "issues": all_issues}
with open(out_path, "w") as f:
    json.dump(result, f, indent=2)

print(f"[Sonarqube] Successfully exported {len(all_issues)} security issues to {out_path}")
'
    """
}