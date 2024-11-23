pipeline {
    environment {
        registry = "medbnk/timesheet2024"
        registryCredential = 'dockerhub'
        dockerImage = ''
        MAVEN_OPTS = '-Dmaven.repo.local=/var/jenkins_home/.m2/repository'
        JAVA_HOME_8 = '/var/jenkins_home/tools/hudson.model.JDK/JDK8/openlogic-openjdk-8u422-b05-linux-x64'
        JAVA_HOME_11 = '/opt/java/openjdk'
        awsCredentialsId = 'aws-credentials'
        SONAR_CREDS = credentials('sonar-credentials')
        NEXUS_CREDS = credentials('nexus-credentials')
        DOCKER_CREDS = credentials('docker-credentials')
    }
    
    agent any
    
    tools {
        maven 'maven'
        dockerTool 'docker'
    }
    
    stages {
        
        stage('Clone Repository') {
            steps {
                cleanWs()
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github-credentials', usernameVariable: 'GITHUB_USERNAME', passwordVariable: 'GITHUB_TOKEN']]) {
                    sh 'git clone https://$GITHUB_USERNAME:$GITHUB_TOKEN@github.com/MohamedBenKedim/timesheetK8s.git'
                }
            }
        }
        
        stage('MVN CLEAN') {
            steps {
                dir('timesheetK8s') {
                    withEnv(["JAVA_HOME=${env.JAVA_HOME_8}", "PATH=${env.JAVA_HOME_8}/bin:${env.PATH}"]) {
                        sh 'mvn clean'
                    }
                }
            }
        }
        
        stage('ARTIFACT CONSTRUCTION') {
            steps {
                dir('timesheetK8s') {
                    withEnv(["JAVA_HOME=${env.JAVA_HOME_8}", "PATH=${env.JAVA_HOME_8}/bin:${env.PATH}"]) {
                        sh 'mvn package -Dmaven.test.skip=true -P test-coverage'
                    }
                }
            }
        }
        
        stage('UNIT TESTS') {
            steps {
                dir('timesheetK8s') {
                    withEnv(["JAVA_HOME=${env.JAVA_HOME_8}", "PATH=${env.JAVA_HOME_8}/bin:${env.PATH}"]) {
                        sh 'mvn test'
                    }
                }
            }
        }
        
        stage('Test and Coverage') {
            steps {
                dir('timesheetK8s') {
                    withEnv(["JAVA_HOME=${env.JAVA_HOME_8}", "PATH=${env.JAVA_HOME_8}/bin:${env.PATH}"]) {
                    sh '''
                        # Run tests with coverage
                        mvn clean test jacoco:prepare-agent install jacoco:report
                    '''
                    }
                }
            }
        }
        
        stage('SonarQube Analysis') {
        steps {
            dir('timesheetK8s') {
                withEnv(["JAVA_HOME=${env.JAVA_HOME_11}", "PATH=${env.JAVA_HOME_11}/bin:${env.PATH}"]) {
                    withSonarQubeEnv('SonarQube') {
                        sh """
                            mvn sonar:sonar \
                                -Dsonar.login=${SONAR_CREDS_USR} \
                                -Dsonar.password=${SONAR_CREDS_PSW} \
                                -Dsonar.host.url=http://172.20.0.3:9000/
                        """
                    }
                }
            }
        }
    }
        
        stage('Test Prometheus Health') {
    steps {
        script {
            try {
                def prometheusHealth = sh(
                    script: '''
                        response=$(curl -s -o /dev/null -w "%{http_code}" http://prometheus:9090/-/healthy)
                        if [ "$response" = "200" ]; then
                            echo "Prometheus is healthy"
                            exit 0
                        else
                            echo "Prometheus health check failed with status: $response"
                            exit 1
                        fi
                    ''',
                    returnStatus: true
                )
                
                if (prometheusHealth == 0) {
                    echo "Prometheus health check passed"
                } else {
                    error "Prometheus health check failed"
                }
                
                // Test metrics collection
                def metricsCheck = sh(
                    script: '''
                        # Check if Prometheus is collecting metrics
                        response=$(curl -s "http://prometheus:9090/api/v1/query?query=up")
                        if echo "$response" | grep -q '"status":"success"'; then
                            echo "Metrics collection is working"
                            exit 0
                        else
                            echo "Metrics collection check failed"
                            exit 1
                        fi
                    ''',
                    returnStatus: true
                )
                
                if (metricsCheck == 0) {
                    echo "Metrics collection check passed"
                } else {
                    error "Metrics collection check failed"
                }
            } catch (Exception e) {
                error "Prometheus tests failed: ${e.getMessage()}"
            }
          }
         }
        }
        stage('Test Grafana Health') {
    steps {
        script {
            try {
                def grafanaHealth = sh(
                    script: '''
                        response=$(curl -s -o /dev/null -w "%{http_code}" http://grafana:3000/api/health)
                        if [ "$response" = "200" ]; then
                            echo "Grafana is healthy"
                            exit 0
                        else
                            echo "Grafana health check failed with status: $response"
                            exit 1
                        fi
                    ''',
                    returnStatus: true
                )
                
                if (grafanaHealth == 0) {
                    echo "Grafana health check passed"
                } else {
                    error "Grafana health check failed"
                }
                
                // Test Grafana datasource
                def datasourceCheck = sh(
                    script: '''
                        response=$(curl -s -u admin:admin http://grafana:3000/api/datasources/name/prometheus)
                        if echo "$response" | grep -q '"name":"prometheus"'; then
                            echo "Prometheus datasource is configured"
                            exit 0
                        else
                            echo "Prometheus datasource check failed"
                            exit 1
                        fi
                    ''',
                    returnStatus: true
                )
                
                if (datasourceCheck == 0) {
                    echo "Datasource check passed"
                } else {
                    error "Datasource check failed"
                }
            } catch (Exception e) {
                error "Grafana tests failed: ${e.getMessage()}"
             }
            }
        }
       }
      
        
        stage("PUBLISH TO NEXUS") {
        steps {
            dir('timesheetK8s') {
                withEnv(["JAVA_HOME=${env.JAVA_HOME_11}", "PATH=${env.JAVA_HOME_11}/bin:${env.PATH}"]) {
                    withCredentials([usernamePassword(credentialsId: 'nexus-credentials', 
                                                    usernameVariable: 'NEXUS_USERNAME', 
                                                    passwordVariable: 'NEXUS_PASSWORD')]) {
                        script {
                            def version = "0.0.3-${BUILD_NUMBER}"
                            sh "mvn versions:set -DnewVersion=${version}"
                            sh """
                                mvn deploy --settings /var/jenkins_home/.m2/settings.xml \
                                    -DaltDeploymentRepository=deploymentRepo::default::http://172.20.0.4:8081/repository/maven-releases/ \
                                    -Dusername=${NEXUS_USERNAME} -Dpassword=${NEXUS_PASSWORD} \
                                    -DskipTests
                            """
                        }
                    }
                }
            }
        }
    }
        
        
        stage('BUILDING OUR IMAGE') {
            steps {
                dir('timesheetK8s') {
                    script {
                        dockerImage = docker.build("${registry}:${BUILD_NUMBER}")
                    }
                }
            }
        }

       stage('DEPLOY OUR IMAGE') {
        steps {
            script {
                withCredentials([usernamePassword(credentialsId: 'docker-credentials', 
                                                usernameVariable: 'DOCKER_USERNAME', 
                                                passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh """
                        docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}
                        docker push ${registry}:latest
                    """
                }
            }
        }
    }
        
        stage('Docker Compose Up') {
            steps {
                dir('timesheetK8s') {
                    script {
                        sh 'docker-compose up -d'
                    }
                }
            }
        }
       
        stage('Test AWS Credentials') 
        {
            steps {
                withCredentials([file(credentialsId: awsCredentialsId, variable: 'AWS_CREDENTIALS_FILE')]) {
                    script {
                        def awsCredentials = readFile(AWS_CREDENTIALS_FILE).trim().split("\n")
                        env.AWS_ACCESS_KEY_ID = awsCredentials.find { it.startsWith("aws_access_key_id") }.split("=")[1].trim()
                        env.AWS_SECRET_ACCESS_KEY = awsCredentials.find { it.startsWith("aws_secret_access_key") }.split("=")[1].trim()
                        env.AWS_SESSION_TOKEN = awsCredentials.find { it.startsWith("aws_session_token") }?.split("=")[1]?.trim()
                        
                        echo "AWS Access Key ID: ${env.AWS_ACCESS_KEY_ID}"
                        // Optional: echo "AWS Session Token: ${env.AWS_SESSION_TOKEN}"
                        
                        echo "AWS Credentials File Loaded"
                    }
                }
            }
        }
        
        stage('TERRAFORM KUBERNETES CLUSTER') {
            environment {
                TF_LOG = 'DEBUG'
            }
            steps {
               withCredentials([file(credentialsId: awsCredentialsId, variable: 'AWS_CREDENTIALS_FILE')])  {
                    script {
                        dir('timesheetK8s/terraform') {
                            sh '''
                                ls -al  # Ensure files are present
                                terraform init 
                                terraform plan
                                terraform apply -auto-approve
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Update kubeconfig') {
            steps {
                script {
                    
                    sh "aws eks update-kubeconfig --name mykubernetes --region us-east-1"
                    env.KUBECONFIG = "/var/jenkins_home/.kube/config"
                    echo "Using KUBECONFIG: ${env.KUBECONFIG}"
                    
                }
            }
        }
        
        stage('DEPLOY TO AWS KUBERNETES') {
            steps {
                 withCredentials([file(credentialsId: awsCredentialsId, variable: 'AWS_CREDENTIALS_FILE')])  {
                    dir('timesheetK8s') {
                        sh '''
                            # Use the generated kubeconfig file
                            kubectl apply -f deployment.yaml
                            kubectl apply -f service.yaml
                        '''
                    }
                }
            }
        }

        stage('Terraform Destroy') {
                steps {
                    withCredentials([file(credentialsId: awsCredentialsId, variable: 'AWS_CREDENTIALS_FILE')]) {
                        script {
                            dir('helloTerraformTest/terraform') {
                                sh '''
                                    terraform destroy -auto-approve
                                '''
                            }
                        }
                    }
                }
            }

    }  
    
}