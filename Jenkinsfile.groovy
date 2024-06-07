pipeline {
    agent any
    stages {
        stage('Clone Repository') {
            steps {
                git 'https://github.com/srvkmr699/performance-test.git'
            }
        }
        stage('Run JMeter Tests') {
            steps {
                script {
                    docker.image('jmeter:latest').inside {
                        sh '''
                            jmeter -n -t /var/jenkins_home/workspace/backend_job_jmeter/jmeter_scripts/Facconable.jmx -l /var/jenkins_home/workspace/backend_job_jmeter/jmeter/results/results.jtl \
                            -Jjmeter.save.saveservice.output_format=xml \
                            -e -o /var/jenkins_home/workspace/backend_job_jmeter/jmeter/results/report
                        '''
                    }
                }
            }
        }
        stage('Publish Results') {
            steps {
                perfReport sourceDataFiles: 'jmeter/results/results.jtl', filterRegex: 'jmeter/results/results.jtl'
            }
        }
        stage('Send Data to InfluxDB') {
            steps {
                script {
                    docker.image('influxdb:latest').inside {
                        sh '''
                            influx -host influxdb -username admin -password admin123 \
                            -database jmeter -execute 'import /jmeter/results/results.jtl'
                        '''
                    }
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'jmeter/results/results.jtl'
        }
    }
}
