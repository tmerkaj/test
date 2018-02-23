
def call( List dependencies) {
	 pipeline {
		agent any
			
		tools { 
			gradle 'gradle_4.4' 
			jdk 'java8' 
		}
		
		stages {	
			stage('svn checkout') {
			  steps {
				// checkout project
				checkout scm
				// checkout project dependencies
				  script {
					def list = [];
					dependencies.eachWithIndex { it, i ->
					  def item = [credentialsId:'${env.HC_SVN_USERID}', local:"$it", remote:'${env.HC_SVN_REPO}/'+"$it"+'/trunk' ]
					  list.add(item);
					}
					def checkoutMap = ['$class': 'SubversionSCM', 'locations': list]
					checkout(checkoutMap)
				}
			  }
			}
			
			stage('compile & build java project') {
			  steps {
				sh 'gradle clean build -x test'
				archiveArtifacts 'build/libs/*'
			  }
			}
			
			stage('Prompt To Deploy TO PROD') {
				steps {
					script {
						try {
							 timeout(time: 10, unit: 'SECONDS') {
								env.DEPLOY_TO_PROD = input message: 'User input required',
								parameters: [choice(name: 'Deploy To Production', choices: 'no\nyes', description: 'Choose "yes" if you want to deploy this build')]
							 }
						} catch(err) { // timeout reached or input false      
							echo "Exception thrown:\n"
							env.DEPLOY_TO_PROD = 'no'
						}
							 
					}
				}
			}
				
			stage('Deploy To Prod') {
				when {
					environment name: 'DEPLOY_TO_PROD', value: 'yes'
				}
				steps {
				   echo "Hello World, Deploying to PROD"
				} 
			}
				
			stage ('Deployment Skipped') {
				when {
					environment name: 'DEPLOY_TO_PROD', value: 'no'
				}
				steps {
				   echo "NOT Deploying to PROD, either user input timed out or user selected NO"
				}
			}
		}
		
	}
}