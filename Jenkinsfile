@Library('mpl@master') _

MPLPipeline {
    agent {
        docker {
            image 'jesen0307/java-pipeline:latest'
            args '--network sonarqube-fresh_sonarnet'
        }
    }
    modules = [
        Checkout: [:],
        Build: [:],
        SAST: [
            project_key: 'VulnerableApp2',
            sonar_host: 'http://sonarqube:9000',
            sonar_token: 'squ_a76c5e818a392cb07370af0fb874c9e3fe84ec90',
            output_dir: 'target/sonar-reports',
            java_binaries: 'build/classes/java/main,build/classes'
        ],
        Test: [:]
    ]
}