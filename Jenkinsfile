@Library('mpl@master') _

MPLPipeline {
    modules = [
        Checkout: [:],
        Build: [:],
        SAST: [
            project_key: 'VulnerableApp2',
            host_url: 'http://sonarqube:9000',
            token: 'squ_a76c5e818a392cb07370af0fb874c9e3fe84ec90',
            output_dir: 'target/sonar-reports',
            java_binaries: 'target/classes'
        ],
        Test: [:]
    ]
}
