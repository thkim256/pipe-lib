@Library('accordion-lib')

def option = [:]
//option.logger = [level: "INFO"]
//option.kubernetes = []
accordionTemplate(option) {
    /*
    * 저장소(git,svn)에서 소스코드 또는 사용자가 업로드한 파일을 가져오는 단계
    * */
    stageGetSource()

    /*
    * 소스코드를 빌드(maven,ant)하는 단계
    * 우선순위 : mvn-accordion.xml -> mvn.xml -> build-accordion.xml -> build.xml -> 단계 SKIP
    * */
    stageSourceBuild()

    /*
    * 도커 이미지을 만드는 단계(docker build)
    * */
    stageDockerBuild()

    /*
    * 도커 이미지를 레지스트리에 넣는 단계(docker push)
    * */
    stageDockerPush()

    /*
    * 도커 이미지를 아코디언에 반영하는 단계
    * */
    stageDeploy()

    stage('Log Print') {
        log.error '에러 로그'
        log.warning '경고 로그'
        log.info '기본 로그'
        log.debug '디버그 로그'
    }
}

//{"source":{"type":"GIT","url":"https://github.com/thkim256/hello-golang.git"},"deploy":{"type":"template","name":"sample-maven-war","project":"pipe","image":"127.0.0.1:30001/hello-golang","imageNumber":"4"}}
//{"url":"https://127.0.0.1:30001"}
//http://10.20.200.201:30000
