/**
 * Secret scanning module executing Trufflehog (Git repository scan)
 */
def outputDir = 'target/secret-reports'

sh "mkdir -p ${outputDir}"

echo "[SecretScanning] Running Trufflehog git scanner"

// Run Trufflehog git scan on the local repo outputting JSON format
// returnStatus: true prevents Jenkins from failing prematurely before archiving reports
def statusCode = sh(
    script: """#!/bin/bash
        trufflehog git file://\$PWD \\
          --json \\
          --no-verification > ${outputDir}/trufflehog.json
    """,
    returnStatus: true
)

if (statusCode != 0) {
    echo "[SecretScanning] Trufflehog found potential secrets (exit code: ${statusCode}). Report saved to ${outputDir}/trufflehog.json"
} else {
    echo "[SecretScanning] No exposed secrets found."
}