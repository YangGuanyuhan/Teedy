pipeline {
    agent any
    
    environment {
        DOCKER_HUB_CREDENTIALS = credentials('dockerhub_credentials')
        DOCKER_IMAGE = 'chr1sty/teedy-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        REGISTRY = 'https://registry.hub.docker.com'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/master']], 
                    userRemoteConfigs: [[url: 'https://github.com/YangGuanyuhan/Teedy.git']]
                )
            }
        }
        
        stage('Build & Package') {
            steps {
                sh 'mvn clean -DskipTests package'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test -Dmaven.test.failure.ignore=true'
            }
        }
        
        stage('Code Quality') {
            steps {
                sh 'mvn pmd:pmd jacoco:report'
            }
        }
        
        stage('Building image') {
            when {
                branch 'master'
            }
            steps {
                script {
                    // assume Dockerfile locate at root 
                    docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }
        
        stage('Upload image') {
            when {
                branch 'master'
            }
            steps {
                script {
                    docker.withRegistry("${REGISTRY}", 'DOCKER_HUB_CREDENTIALS') {
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push()
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push('latest')
                    }
                }
            }
        }
        
        stage('Run containers') {
            when {
                branch 'master'
            }
            steps {
                script {
                    // stop then remove containers if exists
                    sh 'docker stop teedy-container-8081 || true'
                    sh 'docker rm teedy-container-8081 || true'
                    
                    // run Container
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run('--name teedy-container-8081 -d -p 8081:8080')
                    
                    // Optional: list all teedy-containers
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
        }
        failure {
            echo 'Pipeline failed! Check logs above.'
        }
    }
}

