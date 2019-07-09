pipeline {
    
    agent any
    
    tools {
        maven 'Maven 3.5.2'
    }

    stages {
        stage ('Initialize') {
            steps {  
                script {
                    def causes = currentBuild.getBuildCauses('com.cloudbees.jenkins.GitHubPushCause').shortDescription
                    if(!causes) {
                        causes = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').shortDescription
                    }
                    currentBuild.displayName = "#${BUILD_NUMBER} ${GIT_BRANCH} ${causes}"
                }
      			sh '''
               		echo "PATH = ${PATH}"
               		echo "M2_HOME = ${M2_HOME}"
               	'''
            }
        }

        stage ('Test') {
            steps {
                sh 'mvn clean test' 
            }
        }

        stage ('Package') {
        	steps {
        		sh 'mvn clean package'
        	}
        }

        stage ('Verify') {
        	steps {
                wrap([$class: 'Xvfb', additionalOptions: '', assignedLabels: '', autoDisplayName: true, debug: true, displayNameOffset: 0, installationName: 'default', parallelBuild: true, screen: '']) {
                    sh 'mvn clean verify'
                }
        	}
        }
    }
    post {
        always {
            emailext body: '${currentBuild.absoluteUrl}', subject: '${GIT_BRANCH} -build Faild', to: 'c.orsinger@tu-braunschweig.de'
        }
        //unsuccessful {
        // One or more steps need to be included within each condition's block.
        //}
    } 
}
