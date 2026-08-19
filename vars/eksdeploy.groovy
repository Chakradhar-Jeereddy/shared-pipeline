// catalogue-ci -> catalogue-deploy -> shared-library(eksdeploy)
// ci shared-library sending appVersion and deploy_to
// We need to receive it.
def call(Map inputs){
 pipeline{
    // These are pre-build sections
    agent{
        node{
            label 'agent1'
        }
    }
    environment {
        COURSE = "Jenkins"
        appVersion = inputs.get("appVersion")
        ACC_ID = inputs.get("acc_id")
        PROJECT = inputs.get("project")
        COMPONENT = inputs.get("component")
        deploy_to = inputs.get("deploy_to")
        REGION = inputs.get("region")
    }
    options {
        timeout(time: 30, unit: 'MINUTES') 
        disableConcurrentBuilds()
    }
    // This is build section
    stages {  
        stage('Deploy') {
            steps {
                script{
                    withAWS(region:'us-east-1',credentials:'aws-auth') {
                        sh """
                            aws eks update-kubeconfig --region ${REGION} --name ${PROJECT}-${deploy_to}
                            kubectl get nodes
                            echo "${deploy_to}, ${appVersion}"
                        """
                    }
                }
            }
        }
    }
    post{
        always{
            echo 'I will always say Hello again!'
            cleanWs()
        }
        success {
            echo 'I will run if success'
        }
        failure {
            echo 'I will run if failure'
        }
        aborted {
            echo 'pipeline is aborted'
        }
    }
 }
}