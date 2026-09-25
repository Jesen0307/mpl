// Module: Sonarqube
// Handles Maven and Gradle SonarQube scans and exports all security-tagged issues to sonar.json

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

    // Export all security-tagged issues (Vulnerabilities + Security Code Smells) for DefectDojo import
    echo "[Sonarqube] Fetching security-tagged issues and saving to ${outputDir}/sonar.json..."
    sh """
        python3 -c '
import json, urllib.request, urllib.error, base64

host = "${sonarHost}"
token = "${sonarToken}"
project_key = "${projectKey}"
out_path = "${outputDir}/sonar.json"

auth_header = "Basic " + base64.b64encode(f"{token}:".encode()).decode()

def fetch_api(endpoint):
    page = 1
    page_size = 500
    all_items = []
    base_response = {}

    while True:
        url = f"{host}{endpoint}&ps={page_size}&p={page}"
        req = urllib.request.Request(url)
        req.add_header("Authorization", auth_header)
        
        try:
            with urllib.request.urlopen(req) as resp:
                data = json.loads(resp.read().decode())
        except urllib.error.HTTPError as e:
            print(f"[Sonarqube] API error {e.code} on {url}")
            break

        if page == 1:
            base_response = data

        items = data.get("issues", [])
        all_items.extend(items)

        paging = data.get("paging", {})
        total = paging.get("total", data.get("total", 0))

        if not items or page * page_size >= total:
            break
        page += 1

    return base_response, all_items

# Fetch vulnerabilities OR security-tagged findings
sec_tags = "security,cwe,owasp-a1,owasp-a2,owasp-a3,owasp-a4,owasp-a5,owasp-a6,owasp-a7,owasp-a8,owasp-a9,owasp-a10,sans-top25"
base_payload, issues = fetch_api(f"/api/issues/search?componentKeys={project_key}&tags={sec_tags}&statuses=OPEN,CONFIRMED,REOPENED")

# Preserve native API response structure required by DefectDojo parser
base_payload["issues"] = issues
base_payload["total"] = len(issues)

with open(out_path, "w") as f:
    json.dump(base_payload, f, indent=2)

print(f"[Sonarqube] Successfully exported {len(issues)} security-tagged issues to {out_path}")
'
    """
}