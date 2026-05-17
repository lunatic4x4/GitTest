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
                // This will checkout the entire repository (GitTest)
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                // Execute Maven commands within the sub-project directory
                dir(env.PROJECT_DIR) {
                    sh 'java -version'
                    sh 'mvn -version'
                    sh 'mvn -B clean package'
                }
            }
            post {
                always {
                    // Archive JUnit test results
                    // Path is relative to the workspace root, so include the project directory
                    junit "${env.PROJECT_DIR}/target/surefire-reports/*.xml"
                }
            }
        }
        
        // Jenkinsfile (SAST Stage)
        stage('SAST (SpotBugs)') {
            steps {
                dir(env.PROJECT_DIR) {
                    // Force a clean compilation so fresh bytecode exists for SpotBugs to read
                    sh 'mvn clean compile spotbugs:check || true'
                }
            }
            post {
                always {
                    recordIssues(
                        tools: [spotBugs(pattern: '**/target/spotbugsXml.xml')],
                        qualityGates: [
                            [threshold: 1, type: 'TOTAL', severity: 'HIGH', unstable: true]
                        ]
                    )
                }
            }
        }

        stage('SCA (OWASP Dependency-Check)') {
            steps {
                dir(env.PROJECT_DIR) {
                    sh 'mvn org.owasp:dependency-check-maven:check || true'
                }
            }
            post {
                always {
                    // Force global lookup pattern to prevent path parsing errors
                    dependencyCheckPublisher(pattern: '**/target/dependency-check-report.xml')
                    archiveArtifacts artifacts: '**/target/dependency-check-report.html', allowEmptyArchive: true
                }
            }
        }

        // Optional: Archive the built JAR
        stage('Archive Application') {
            steps {
                dir(env.PROJECT_DIR) {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
                }
            }
        }
    } // End of stages

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