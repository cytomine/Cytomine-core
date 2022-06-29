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
           catchError (buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    sh 'docker-compose -f scripts/docker-compose-test.yml down -v'
                }
                sh 'docker-compose -f scripts/docker-compose-test.yml up -d'
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
            catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                sh 'docker-compose -f scripts/docker-compose-test.yml down -v'
            }
        }

    }
    stage ('Final') {
        sh 'echo finish'
    }
   stage ('Build jar') {
    sh 'scripts/ciBuildJar.sh'
    }

    withFolderProperties{
        // if PRIVATE is define in jenkins, the war and the docker image are send to the private cytomine repository.
        // otherwise, public repo for war and public dockerhub repo for docker image
        echo("Private: ${env.PRIVATE}")

        if (env.PRIVATE && env.PRIVATE.equals("true")) {
            stage 'Publish jar (private)'
            sh 'scripts/ciPublishJarPrivate.sh'

            stage 'Build docker image (private)'
            withCredentials(
                [
                    usernamePassword(credentialsId: 'CYTOMINE_DOCKER_REGISTRY', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_TOKEN')
                ]
                ) {
                    docker.withRegistry('http://repository.cytom.in:5004/v2/', 'CYTOMINE_DOCKER_REGISTRY') {
                        sh 'scripts/ciBuildDockerImagePrivate.sh'
                    }
                }
        } else {
            stage 'Publish jar'
            sh 'scripts/ciPublishJar.sh'

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

}
