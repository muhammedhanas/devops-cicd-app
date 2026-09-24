pipeline {
    agent any

    environment {
        APP_NAME   = 'devops-cicd-app'
        REGISTRY = 'localhost:5000'
        IMAGE      = "${REGISTRY}/${APP_NAME}:${BUILD_NUMBER}"
        NEXUS_URL  = 'http://nexus:8081'
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
                    echo "===== MAVEN BUILD ====="

                    docker exec maven sh -c 'rm -rf /workspace && mkdir -p /workspace'

                    docker cp . maven:/workspace/

                    docker exec \
                      -w /workspace \
                      maven \
                      mvn clean package

                    rm -rf target
                    docker cp maven:/workspace/target ./target
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    withCredentials([
                        string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {
                        sh '''
                            echo "===== SONARQUBE ANALYSIS ====="

                            docker cp . maven:/workspace/

                            docker exec \
                              -e SONAR_HOST_URL="$SONAR_HOST_URL" \
                              -e SONAR_TOKEN="$SONAR_TOKEN" \
                              -w /workspace \
                              maven \
                              mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
                              -Dsonar.host.url="$SONAR_HOST_URL" \
                              -Dsonar.login="$SONAR_TOKEN" \
                              -Dsonar.projectKey=devops-cicd-app \
                              -Dsonar.projectName=devops-cicd-app
                        '''
                    }
                }
            }
        }

        stage('Upload Artifact to Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    sh '''
                        echo "===== NEXUS UPLOAD ====="

                        curl --fail \
                          -u "$NEXUS_USER:$NEXUS_PASSWORD" \
                          --upload-file target/devops-cicd-app-1.0.0.jar \
                          "$NEXUS_URL/repository/maven-releases/com/devops/devops-cicd-app/1.0.${BUILD_NUMBER}/devops-cicd-app-1.0.${BUILD_NUMBER}.jar"
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    echo "===== DOCKER BUILD ====="

                    docker build \
                      -t "$IMAGE" \
                      -t "$REGISTRY/$APP_NAME:latest" \
                      .
                '''
            }
        }

        stage('Trivy Scan') {
            steps {
                sh '''
                    echo "===== TRIVY SECURITY SCAN ====="

                    trivy image \
                      --server http://trivy:4954 \
                      --exit-code 1 \
                      --severity CRITICAL \
                      "$IMAGE"
                '''
            }
        }

        stage('Push Image') {
            steps {
                sh '''
                    echo "===== PUSH TO PRIVATE REGISTRY ====="

                    docker push "$IMAGE"
                    docker push "$REGISTRY/$APP_NAME:latest"
                '''
            }
        }

        stage('Kubeaudit') {
            steps {
                sh '''
                    echo "===== KUBERNETES SECURITY AUDIT ====="

                    kubeaudit manifests k8s/ || true
                '''
            }
        }

        stage('Deploy to KinD') {
            steps {
                sh '''
                    echo "===== DEPLOY TO KIND ====="

                    kubectl apply \
                      -n dev \
                      -f k8s/deployment.yaml

                    KIND_IMAGE="docker-registry:5000/$APP_NAME:${BUILD_NUMBER}"

                    kubectl set image \
                      deployment/devops-cicd-app \
                      devops-cicd-app="$KIND_IMAGE" \
                      -n dev

                    kubectl rollout status \
                      deployment/devops-cicd-app \
                      -n dev \
                      --timeout=120s

                    kubectl get pods -n dev -o wide
                '''
            }
        }
    }

    post {
        success {
            echo '=========================================='
            echo 'DEVSECOPS CI/CD PIPELINE SUCCESSFUL'
            echo '=========================================='
        }

        failure {
            echo '=========================================='
            echo 'DEVSECOPS CI/CD PIPELINE FAILED'
            echo 'CHECK THE FAILED STAGE ABOVE'
            echo '=========================================='
        }

        always {
            echo "Build Number: ${BUILD_NUMBER}"
            echo "Image: ${IMAGE}"
        }
    }
}
