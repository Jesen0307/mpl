// Module: SCA

def outputDir = CFG.output_dir ?: 'target/sca-reports'
def exitCode = CFG.exit_code ?: '0'
def scanTarget = CFG.scan_target ?: '.'

// Build severity argument only if specified in Jenkinsfile; otherwise omit it to scan ALL
def severityArg = CFG.severity ? "--severity ${CFG.severity}" : ""

dir(CFG.workdir ?: '.') {
    sh "mkdir -p ${outputDir}"

    timeout(time: 15, unit: 'MINUTES') {
        echo "[SCA] Running Trivy FS scan (all severities)..."
        sh """
            trivy fs --scanners vuln . \
              ${severityArg} \
              --exit-code ${exitCode} \
              --format json \
              --output ${outputDir}/trivy.json \
              ${scanTarget}
        """
    }
}