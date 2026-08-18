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
      apiVersion = ""
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
         def packagejson = readJSON file: "package.json"
         apiVersion = packagejson.version
         echo "apiversion: ${apiVersion}"
        }
       }
     }
     stage('Install Dependencies') {
        steps {
          sh """
             npm install
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
     stage('Build catalogue image'){
       steps{
         withAWS(region:'us-east-1',credentials:'aws-auth') {
          sh"""
           aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${acc_id}.dkr.ecr.us-east-1.amazonaws.com
           docker build -t ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${apiVersion} .
           docker push ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${apiVersion}
          """
         }
       }
     }
     stage('Trigger Dev Deploy'){
       when{
        expression { params.Deploy == true }
       }
       steps{
        sh"""
         echo "Deploying"
        """
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