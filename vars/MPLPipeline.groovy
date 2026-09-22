//
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
      SCA: [:]
    ]
  ])

  pipeline {
    agent {
      docker {
        image MPL.docker_Image
        label MPL.agent_Label
        args MPL.docker_Args
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
    }
    post {
      always {
        MPLPostStepsRun('always')
        
        // Wrap workspace checks in a node block to provide FilePath context
        node(MPL.agent_label ?: 'built-in') {
          script {
            if (fileExists('target/sonar-reports/sonar_raw.json')) {
              archiveArtifacts artifacts: 'target/sonar-reports/sonar_raw.json', fingerprint: true, allowEmptyArchive: true
            }
            if (fileExists('build/libs')) {
              archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true, allowEmptyArchive: true
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