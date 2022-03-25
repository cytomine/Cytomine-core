node {
    stage ('Retrieve sources') {
        checkout([
            $class: 'GitSCM',
            branches: [[name: 'refs/heads/'+env.BRANCH_NAME]],
            extensions: [[$class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: '']],
            userRemoteConfigs: scm.userRemoteConfigs,
        ])
    }

    stage ('Clean') {
        sh 'rm -rf ./ci'
        sh 'mkdir -p ./ci'
    }


    stage ('Compute version name') {
        sh 'scripts/ciBuildVersion.sh ${BRANCH_NAME}'
    }


    stage ('Download and cache dependencies') {
        sh 'scripts/ciDownloadDependencies.sh'
    }


    lock('cytomine-instance-test') {
        stage ('Run external tools (db, amqp,...)') {
           catchError {
                    sh 'docker-compose -f scripts/docker-compose.yml down -v'
                }
                sh 'docker-compose -f scripts/docker-compose.yml up -d'
        }


        stage ('Build and test') {
            catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                sh 'scripts/ciTest.sh'
            }
        }

        stage ('Publish test') {
            step([$class: 'JUnitResultArchiver', testResults: '**/ci/build/test-results/**/*.xml'])
        }

        stage ('Publish coverage') {
            step([$class: 'JacocoPublisher', runAlways: true, execPattern: '**/**.exec',classPattern: '**/classes',sourcePattern: '**/src/main/java',exclusionPattern: '**/src/test*'])
        }

        stage ('Clear cytomine instance') {
            catchError {
                sh 'docker-compose -f scripts/docker-compose.yml down -v'
            }
        }

    }
    stage ('Final') {
        sh 'echo finish'
    }
   stage ('Build war') {
    sh 'scriptsCI/ciBuildWar.sh'
    }

    stage ('Publish war') {
            sh 'scriptsCI/ciPublishWar.sh'

            stage 'Build docker image'
            withCredentials(
                [
                    usernamePassword(credentialsId: 'DOCKERHUB_CREDENTIAL', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_TOKEN')
                ]
                ) {
                    docker.withRegistry('https://index.docker.io/v1/', 'DOCKERHUB_CREDENTIAL') {
                        sh 'scripts/ciBuildDockerImage.sh'
                    }
                }
    }
}
