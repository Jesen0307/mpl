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