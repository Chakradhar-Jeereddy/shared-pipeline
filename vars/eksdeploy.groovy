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
                        // wait(readyness check) - checks if pod is ready, deployment reached the desired count, pv mounted, service got IP.
                        // timeout (helm marks the release as filed if readyness check wont respond in time)
                        // atomic, rollback the release if it fails.
                        // set -e (exit script immediately if command fails)
                        sh """
                            set -e
                            aws eks update-kubeconfig --region ${REGION} --name ${PROJECT}-${deploy_to}
                            kubectl get nodes
                            # echo "${deploy_to}, ${appVersion}"
                            sed -i "s/IMAGE_VERSION/${appVersion}/g" values.yaml
                            helm upgrade --install ${component} . -f values-${deploy_to}.yaml -n ${project} --rollback-on-failure --wait --timeout=5m
                        """
                    }
                }
            }
        }
        stage('Funtional Testing'){
            steps{
                script{
                    when{
                        expression { deploy_to == "dev"}
                    }
                    sh"""
                        echo "functional test in dev environment"
                    """
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