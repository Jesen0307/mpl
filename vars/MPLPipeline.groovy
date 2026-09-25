// Copyright (c) 2018 Grid Dynamics International, Inc. All Rights Reserved
// https://www.griddynamics.com
//
// Classification level: Public
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// $Id: $
// @Project:     MPL
// @Description: Shared Jenkins Modular Pipeline Library
//

/**
 * Basic MPL pipeline executing inside containerized environment
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 */
def call(body) {
  def MPL = MPLPipelineConfig(body, [
    agent_label: '',
    docker_image: 'jesen0307/java-pipeline:latest',
    docker_args: '-u root:root -v /tmp/jenkins-cache/.m2:/root/.m2 -v /tmp/jenkins-cache/.gradle:/root/.gradle',
    defectdojo_url: 'http://defectdojo-nginx:8080',
    defectdojo_credentials_id: 'DEFECTDOJO_API_KEY',
    product_name: 'VulnerableApp',
    engagement_name: 'CI/CD Pipeline',
    modules: [
      Checkout: [:],
      Build: [:],
      SAST: [:],
      SCA: [:],
      SecretScanning: [:]
    ]
  ])

  // Retrieve configuration variables directly from the MPL config map
  def dockerImage = MPL.config.docker_image
  def agentLabel = MPL.config.agent_label
  def dockerArgs = MPL.config.docker_args
  def ddUrl = MPL.config.defectdojo_url
  def ddCredsId = MPL.config.defectdojo_credentials_id
  def ddProduct = MPL.config.product_name
  def ddEngagement = MPL.config.engagement_name

  pipeline {
    agent {
      docker {
        image dockerImage
        label agentLabel
        args dockerArgs
      }
    }
    options {
      skipDefaultCheckout(true)
    }
    stages {
      stage( 'Checkout' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
      stage( 'Build' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
      stage( 'SAST' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
      stage( 'SCA' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
      stage( 'SecretScanning' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
    }
    post {
      always {
        MPLPostStepsRun('always')
        script {
          // Keep existing artifact archiving untouched
          if (fileExists('target/sonar-reports/sonar.json')) {
            archiveArtifacts artifacts: 'target/sonar-reports/sonar.json', fingerprint: true, allowEmptyArchive: true
          }
          if (fileExists('target/sca-reports/trivy.json')) {
            archiveArtifacts artifacts: 'target/sca-reports/trivy.json', fingerprint: true, allowEmptyArchive: true
          }
          if (fileExists('target/secret-reports/trufflehog.json')) {
            archiveArtifacts artifacts: 'target/secret-reports/trufflehog.json', fingerprint: true, allowEmptyArchive: true
          }
          if (fileExists('build/libs')) {
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true, allowEmptyArchive: true
          }

          // DefectDojo Automatic Scan Ingestion
          withCredentials([string(credentialsId: ddCredsId, variable: 'DD_TOKEN')]) {
            
            // 1. Upload SonarQube SAST Report
            if (fileExists('target/sonar-reports/sonar.json')) {
              echo "Uploading SonarQube report to DefectDojo..."
              sh """
                curl -s -X POST "${ddUrl}/api/v2/import-scan/" \\
                  -H "Authorization: Token ${DD_TOKEN}" \\
                  -H "Content-Type: multipart/form-data" \\
                  -F "active=true" \\
                  -F "verified=true" \\
                  -F "scan_type=SonarQube Scan" \\
                  -F "product_name=${ddProduct}" \\
                  -F "engagement_name=${ddEngagement}" \\
                  -F "auto_create_context=true" \\
                  -F "close_old_findings=true" \\
                  -F "file=@target/sonar-reports/sonar.json"
              """
            }

            // 2. Upload Trivy SCA Report
            if (fileExists('target/sca-reports/trivy.json')) {
              echo "Uploading Trivy report to DefectDojo..."
              sh """
                curl -s -X POST "${ddUrl}/api/v2/import-scan/" \\
                  -H "Authorization: Token ${DD_TOKEN}" \\
                  -H "Content-Type: multipart/form-data" \\
                  -F "active=true" \\
                  -F "verified=true" \\
                  -F "scan_type=Trivy Scan" \\
                  -F "product_name=${ddProduct}" \\
                  -F "engagement_name=${ddEngagement}" \\
                  -F "auto_create_context=true" \\
                  -F "close_old_findings=true" \\
                  -F "file=@target/sca-reports/trivy.json"
              """
            }

            // 3. Upload TruffleHog Secrets Report
            if (fileExists('target/secret-reports/trufflehog.json')) {
              echo "Uploading TruffleHog report to DefectDojo..."
              sh """
                curl -s -X POST "${ddUrl}/api/v2/import-scan/" \\
                  -H "Authorization: Token ${DD_TOKEN}" \\
                  -H "Content-Type: multipart/form-data" \\
                  -F "active=true" \\
                  -F "verified=true" \\
                  -F "scan_type=Trufflehog Scan" \\
                  -F "product_name=${ddProduct}" \\
                  -F "engagement_name=${ddEngagement}" \\
                  -F "auto_create_context=true" \\
                  -F "close_old_findings=true" \\
                  -F "file=@target/secret-reports/trufflehog.json"
              """
            }
          }
        }
      }
      success {
        MPLPostStepsRun('success')
      }
      failure {
        MPLPostStepsRun('failure')
      }
    }
  }
}