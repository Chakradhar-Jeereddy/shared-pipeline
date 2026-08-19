// call is the default function name

def call(Map inputs){   
   pipeline{
    // These are pre-build sections
    agent{
     node{
      label "agent1"
     }
    }
    parameters {
           string(name: 'PERSON', defaultValue: 'Mr Chakradhar', description: 'Who should I say hello to?')
           booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')
    }
    environment{
      appVersion = ""
      acc_id = "406682759639"
      project = inputs.get("project")
      component = inputs.get("component")
    }
    options{
     disableConcurrentBuilds()
     timeout(time: 10, unit: 'MINUTES')
    }
    // This is build section
    stages{
     stage('Read Version'){
       steps{
        script{
         def fileContent = readFile 'version'
         appVersion = fileContent.version
         echo "appVersion: ${appVersion}"
        }
       }
     }
     stage('Install Dependencies') {
        steps {
          sh """
             pip3 install -r requirements.txt
          """
        }
     }
     stage('Unit Test') {
        steps {
            sh """
                echo test
            """
        }
     }
     stage('Build image'){
       steps{
         echo "Building ${component} image"
         withAWS(region:'us-east-1',credentials:'aws-auth') {
          sh"""
           aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${acc_id}.dkr.ecr.us-east-1.amazonaws.com
           docker build -t ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appVersion} .
           docker push ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appVersion}
          """
         }
       }
     }
     stage('Trigger Dev Deploy'){
       when{
        expression { params.Deploy == true }
       }
       steps{
         build job: "../${component}-deploy", 
         wait: false,  // wait for completion
         propagate: false,  // Propogate status
         parameters: [ 
            string(name: 'appVersion', value: "${appVersion}"),
            string(name: 'deploy_to', value: "dev")
         ]
       }
     }
    }
    post{
     always{
      cleanWs()
      echo "Always say hi"
     }
     success{
      echo "Passed the deployment"
     }
     failure{
      echo "Deployment failed"
     }
     aborted{
      echo "deployment aborted"
     }
    }
   }
}