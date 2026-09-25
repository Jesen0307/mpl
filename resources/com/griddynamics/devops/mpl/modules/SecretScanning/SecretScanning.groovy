/**
 * Secret scanning module executing Trufflehog (Git repository scan)
 */
def outputDir = 'target/secret-reports'

sh "mkdir -p ${outputDir}"

// Verify that the repository has Git history available before running
if (!fileExists('.git')) {
    echo "[SecretScanning] WARNING: No .git directory found in ${env.WORKSPACE}. Trufflehog git scan requires a valid Git repository."
    return
}

echo "[SecretScanning] Running Trufflehog git scanner on workspace: ${env.WORKSPACE}"

// Run Trufflehog git scan including both verified and unverified secrets
// returnStatus: true prevents Jenkins from failing prematurely before archiving reports
def statusCode = sh(
    script: """#!/bin/bash
        trufflehog git file://${env.WORKSPACE} \\
          --json > ${outputDir}/trufflehog.json
    """,
    returnStatus: true
)

if (statusCode != 0) {
    echo "[SecretScanning] Trufflehog found potential secrets (exit code: ${statusCode}). Report saved to ${outputDir}/trufflehog.json"
} else {
    echo "[SecretScanning] No exposed secrets found."
}