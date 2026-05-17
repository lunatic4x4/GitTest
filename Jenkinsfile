// Jenkinsfile

pipeline {
    agent any 

    tools {
        maven 'M3'
    }

    environment {
        // Define the sub-project directory for easier reference
        PROJECT_DIR = 'simple-token-validator'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                // Run Maven from the root but point it directly to the subproject pom
                sh 'mvn -B -f simple-token-validator/pom.xml clean package'
            }
            post {
                always {
                    junit 'simple-token-validator/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('SAST (SpotBugs)') {
            steps {
                // Force a fresh compile and scan using the explicit file path
                sh 'mvn -B -f simple-token-validator/pom.xml compile spotbugs:check'
            }
            post {
                always {
                    // Force the parser to look directly inside the target subfolder
                    recordIssues(
                        tools: [spotBugs(pattern: 'simple-token-validator/target/spotbugsXml.xml')],
                        qualityGates: [[threshold: 1, type: 'TOTAL', severity: 'HIGH', unstable: true]]
                    )
                }
            }
        }

        stage('SCA (OWASP Dependency-Check)') {
            steps {
                // Bypass the NVD database update to prevent 403 network crashes
                sh 'mvn -B -f simple-token-validator/pom.xml org.owasp:dependency-check-maven:check -DautoUpdate=false -DfailOnError=false'
            }
        }
    }

    post {
        // Global post actions
        always {
            echo 'Pipeline finished.'
            // Clean up workspace to save disk space, especially if NVD data for Dependency-Check gets large locally
            // cleanWs()
        }
        success {
            echo 'Pipeline succeeded!'
            // mail to: 'dev-team@example.com', subject: "Jenkins Build Succeeded: ${currentBuild.fullDisplayName}"
        }
        failure {
            echo 'Pipeline failed!'
            // mail to: 'dev-team@example.com', subject: "Jenkins Build Failed: ${currentBuild.fullDisplayName}"
        }
        // The 'unstable' status is often used when tests pass but quality gates (like SpotBugs) find issues
        // but are not configured to fail the build outright.
        unstable {
            echo 'Pipeline is unstable (e.g., tests passed, but quality issues found).'
        }
    }
}