/**
 * Secret scanning module executing Trufflehog
 */
def outputDir = 'target/secret-reports'

sh "mkdir -p ${outputDir}"

echo "[SecretScanning] Running Trufflehog secret scanner..."

// Run Trufflehog filesystem scan outputting JSON format
// returnStatus: true prevents Jenkins from failing prematurely before archiving reports
def statusCode = sh(
    script: """#!/bin/bash
        trufflehog filesystem . \\
          --json \\
          --no-verification \\
          --exclude-dirs="build,target,.git,node_modules" > ${outputDir}/trufflehog_raw.json
    """,
    returnStatus: true
)

if (statusCode != 0) {
    echo "[SecretScanning] Trufflehog found potential secrets (exit code: ${statusCode}). Report saved to ${outputDir}/trufflehog_raw.json"
} else {
    echo "[SecretScanning] No exposed secrets found."
}