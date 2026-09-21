// resources/com/griddynamics/mpl/modules/SAST/Sonarqube.groovy
/**
 * SonarQube SAST Module for MPL
 * Executes SonarScanner CLI, polls for task completion, and exports security issues to JSON.
 */

def projectKey  = CFG.'project_key' ?: env.JOB_BASE_NAME
def hostUrl     = CFG.'host_url'    ?: 'http://localhost:9000'
def token       = CFG.'token'       ?: env.SONAR_TOKEN
def outputDir   = CFG.'output_dir'  ?: '.'
def javaBin     = CFG.'java_binaries' ?: ''

if (!token) {
    error "[Sonarqube] ERROR: SonarQube token is required. Pass 'token' in CFG or set SONAR_TOKEN env var."
}

def toolName   = CFG.'tool_name' ?: 'SonarScanner'
def scannerHome = tool (name: toolName, type: 'hudson.plugins.sonar.SonarRunnerInstallation')
def scannerBin  = "${scannerHome}/bin/sonar-scanner"

// 1. Prepare the embedded Python export script
def pyExportScript = '''
import json
import sys
import urllib.request

host, token, project_key, out_path = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
page_size = 500
result = {"issues": [], "hotspots": []}

def api_request(path):
    url = f"{host}{path}"
    req = urllib.request.Request(url)
    req.add_header("Authorization", "Basic " + __import__("base64").b64encode(f"{token}:".encode()).decode())
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)

page = 1
while True:
    try:
        data = api_request(f"/api/issues/search?componentKeys={project_key}&impactSoftwareQualities=SECURITY&ps={page_size}&p={page}")
    except Exception as e:
        print(f"[sonarqube-scan] WARNING: could not fetch issues: {e}", file=sys.stderr)
        break
    issues = data.get("issues", [])
    for i in issues:
        result["issues"].append({
            "key": i.get("key"),
            "rule": i.get("rule"),
            "component": i.get("component"),
            "severity": i.get("severity"),
            "message": i.get("message"),
            "line": i.get("line"),
            "textRange": i.get("textRange"),
        })
    total = data.get("total", 0)
    if not issues or page * page_size >= total:
        break
    page += 1

with open(out_path, "w") as f:
    json.dump(result, f)
print(f"[sonarqube-scan] Exported {len(result['issues'])} security issues to {out_path}")
'''

// 2. Execute the pipeline scanning and polling logic
dir(CFG.'workspace_root' ?: '.') {
    sh "mkdir -p '${outputDir}'"

    // Construct SonarScanner arguments
    def scanArgs = [
        "-Dsonar.projectKey=${projectKey}",
        "-Dsonar.projectName=${projectKey}",
        "-Dsonar.sources=src",
        "-Dsonar.host.url=${hostUrl}",
        "-Dsonar.token=${token}",
        "-Dsonar.scanner.skipJreProvisioning=true"
    ]

    // Determine Java Binaries
    def binaries = javaBin
    if (!binaries) {
        if (fileExists('target/classes')) {
            binaries = 'target/classes'
        } else if (fileExists('build/classes/java/main')) {
            binaries = 'build/classes/java/main'
        }
    }
    if (binaries) {
        echo "[Sonarqube] Using java binaries: ${binaries}"
        scanArgs.add("-Dsonar.java.binaries=${binaries}")
    } else {
        echo "[Sonarqube] WARNING: No compiled Java classes found."
    }

    // Determine Classpath
    if (fileExists('build/runtimeClasspath.txt')) {
        scanArgs.add("-Dsonar.java.libraries=build/runtimeClasspath.txt")
    } else if (fileExists('build')) {
        def cp = sh(script: "find build -name '*.jar' 2>/dev/null | tr '\\n' ':'", returnStdout: true).trim()
        if (cp) {
            scanArgs.add("-Dsonar.java.libraries=${cp}")
        }
    }

    echo "[Sonarqube] Starting SonarQube scan for project '${projectKey}' on ${hostUrl}"

    // Run Scanner using global tool binary with timeout
    timeout(time: 20, unit: 'MINUTES') {
        sh "${scannerBin} ${scanArgs.join(' ')} > '${outputDir}/sonar_scanner_stdout.log' 2>&1"
    }

    // Extract Compute Engine Task ID
    def taskId = sh(
        script: "grep -oE 'api/ce/task\\?id=[a-fA-F0-9-]+' '${outputDir}/sonar_scanner_stdout.log' | head -1 | sed 's/.*id=//' || true",
        returnStdout: true
    ).trim()

    if (!taskId) {
        echo "[Sonarqube] WARNING: Could not find CE task ID in log. Status cannot be polled."
    } else {
        echo "[Sonarqube] Polling CE task: ${taskId}"
        
        // Poll Compute Engine Task until complete
        timeout(time: 15, unit: 'MINUTES') {
            waitUntil {
                def statusJson = sh(
                    script: "curl -sf -u '${token}:' '${hostUrl}/api/ce/task?id=${taskId}' || echo '{}'",
                    returnStdout: true
                ).trim()

                def status = withEnv(["STATUS_JSON=${statusJson}"]){
                sh(
                    script: """python3 -c "import json,sys; print(json.loads('''${statusJson}''').get('task',{}).get('status','UNKNOWN'))" """,
                    returnStdout: true
                ).trim()
                }
                echo "[Sonarqube] Analysis status: ${status}"

                if (status == 'SUCCESS') {
                    return true
                } else if (status in ['FAILED', 'CANCELED']) {
                    error "[Sonarqube] Analysis task failed with status: ${status}"
                }
                return false
            }
        }
    }

    // Export findings to JSON using embedded Python script
    echo "[Sonarqube] Exporting security findings..."
    writeFile file: "${outputDir}/export_issues.py", text: pyExportScript
    sh "python3 '${outputDir}/export_issues.py' '${hostUrl}' '${token}' '${projectKey}' '${outputDir}/sonar_raw.json'"
    sh "rm -f '${outputDir}/export_issues.py'"
}