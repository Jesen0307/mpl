@Library('mpl@master') _

MPLPipeline {
    docker_args = '-u root:root --network sonarqube-fresh_sonarnet -v /tmp/jenkins-cache/.m2:/root/.m2 -v /tmp/jenkins-cache/.gradle:/root/.gradle'

    modules = [
        Checkout: [:],
        Build: [:],
        SAST: [
            project_key: 'VulnerableApp2',
            project_name: 'VulnerableApp2',
            sonar_host: 'http://sonarqube:9000',
            sonar_token: 'squ_a76c5e818a392cb07370af0fb874c9e3fe84ec90',
            output_dir: 'target/sonar-reports',
            java_binaries: 'build/classes/java/main,build/classes'
        ],
        SCA: [
            scan_target: '.',
            output_dir: 'target/sca-reports',
            exit_code: '0'
        ]
    ]
}