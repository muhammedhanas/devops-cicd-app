pipeline {
    agent any

    environment {
        APP_NAME = 'devops-cicd-app'
        REGISTRY = 'docker-registry:5000'
        IMAGE = "${REGISTRY}/${APP_NAME}:${BUILD_NUMBER}"
        NEXUS_URL = 'http://nexus:8081'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Maven Build') {
            steps {
                sh '''
                    docker exec maven rm -rf /workspace/*
                    docker cp . maven:/workspace/
                    docker exec -w /workspace maven mvn clean package
                    docker cp maven:/workspace/target ./target
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    withCredentials([string(
                        credentialsId: 'sonar-token',
                        variable: 'SONAR_TOKEN'
                    )]) {
                        sh '''
                            docker cp . maven:/workspace/
                            docker exec \
                              -e SONAR_HOST_URL="$SONAR_HOST_URL" \
                              -e SONAR_TOKEN="$SONAR_TOKEN" \
                              -w /workspace maven \
                              mvn sonar:sonar \
                              -Dsonar.host.url="$SONAR_HOST_URL" \
                              -Dsonar.token="$SONAR_TOKEN"
                        '''
                    }
                }
            }
        }

        stage('Upload Artifact to Nexus') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASSWORD'
                )]) {
                    sh '''
                        curl -u "$NEXUS_USER:$NEXUS_PASSWORD" \
                          --upload-file target/devops-cicd-app-1.0.0.jar \
                          "$NEXUS_URL/repository/maven-releases/com/devops/devops-cicd-app/1.0.0/devops-cicd-app-1.0.0.jar"
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build -t "$IMAGE" .
                '''
            }
        }

		stage('Kubeaudit') {
			steps {
				sh '''
					kubeaudit manifests k8s/ || true
				'''
			}
		}

        stage('Push Image') {
            steps {
                sh '''
                    docker push "$IMAGE"
                    docker tag "$IMAGE" "$REGISTRY/$APP_NAME:latest"
                    docker push "$REGISTRY/$APP_NAME:latest"
                '''
            }
        }

        stage('Kubeaudit') {
            steps {
                sh '''
                    kubeaudit manifests k8s/ || true
                '''
            }
        }

        stage('Deploy to KinD') {
            steps {
                sh '''
                    kubectl apply -n dev -f k8s/deployment.yaml

                    kubectl set image deployment/devops-cicd-app \
                      devops-cicd-app="$IMAGE" \
                      -n dev

                    kubectl rollout status deployment/devops-cicd-app \
                      -n dev \
                      --timeout=120s
                '''
            }
        }
    }

    post {
        success {
            echo 'DEVSECOPS PIPELINE SUCCESSFUL'
        }

        failure {
            echo 'DEVSECOPS PIPELINE FAILED'
        }
    }
}
