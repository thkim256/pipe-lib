import groovy.transform.Field
import io.accordions.BuildConfig
import io.accordions.NodeRunner
import io.accordions.PodRunner
import io.accordions.RegistryInfo
import io.accordions.logger.Logger
import io.accordions.logger.LoggerFactory
import io.accordions.util.HttpUtil
import io.accordions.util.ObjectUtil

@Field Logger log
@Field HttpUtil httpUtil
@Field BuildConfig config = new BuildConfig()
@Field RegistryInfo registry = new RegistryInfo()

@Field boolean existRegistry = false

//@Field BuildConfig originConfig = new BuildConfig()
//@Field RegistryInfo originRegistry = new RegistryInfo()

def call(option = [:], Closure closure) {
    try {
        log = LoggerFactory.get(option.logger)

        // utilities
        httpUtil = new HttpUtil()
        httpUtil.log = this.log

        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = this

        def runner = option.runner ?: "pod"
        this.slave = runner != "master"

        log.info "begin::initialize"
        try {
            log.debug "build description setting"

            log.debug "ACCORDION_BUILD_CONFIG setting"
            this.originConfig = ObjectUtil.toObject("${ACCORDION_BUILD_CONFIG}")
            this.config = ObjectUtil.toObject("${ACCORDION_BUILD_CONFIG}")

            this.config.dockerfile = ""
            currentBuild.description = "${ObjectUtil.toJson(this.config)}"
            this.config.dockerfile = this.originConfig.dockerfile
        } catch (e) {
            throw new Exception("ACCORDION_BUILD_CONFIG load fail", e)
        }

        this.existRegistry = false
        try {
            this.originRegistry = ObjectUtil.toObject("${ACCORDION_DOCKER_REGISTRY}")
            this.registry = ObjectUtil.toObject("${ACCORDION_DOCKER_REGISTRY}")
            this.registry.password = "***"
            existRegistry = true
        } catch (ignored) {
        }


        if (this.originConfig.deploy == null ||
                ObjectUtil.empty(this.originConfig.deploy.project.name) ||
                ObjectUtil.empty(this.originConfig.deploy.app.name)) {
            throw new Exception("Please required value [config.deploy.app.name, config.deploy.project.name]")
        }

        log.info "config.source : ${this.originConfig.source}"
        log.info "config.deploy : ${this.originConfig.deploy}"
        log.info "config.image  : ${this.originConfig.image}"

        if (existRegistry) {
            log.info "registry      : name:${this.registry.name}, host:${this.registry.host}, username:${this.registry.username}"
        }

        log.debug "logger       : level:${log.level}, ansiColor:${log.color}"
        log.debug "runner       : ${runner}"

        log.info "end::initialize"

        if (this.slave) {
            new PodRunner().run(option, closure)
        } else {
            new NodeRunner().run(option, closure)
        }
    } catch (err) {
        log.error(err.getMessage(), err)
        if (log.isDebugEnabled()) {
            err.printStackTrace()
        }

        currentBuild.result = 'FAILURE'
    }
}

def stage(def stageName, def containerName, Closure closure) {
    def hasContainerName = ObjectUtil.nonEmpty(containerName)
    if (this.slave && hasContainerName) {
        container(containerName) {
            stage(stageName) {
                closure.call()
            }
        }
    } else {
        if (hasContainerName) {
            log.debug "container setting ignored (stageName: ${stageName}, containerName: ${containerName})"
        }
        stage(stageName) {
            closure.call()
        }
    }
}

def checkoutGitSource(def param = [:]) {
    def url = param.url
    def branch = param.branch ?: "master"
    def dir = param.dir ?: "."
    def credential = param.credential ?: ""
    checkout([$class                           : 'GitSCM',
              branches                         : [[name: "${branch}"]],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [],
              submoduleCfg                     : [],
              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                   relativeTargetDir: "${dir}"]],
              userRemoteConfigs                : [[credentialsId: "${credential}",
                                                   url          : "${url}"]]])
}

def checkoutSvnSource(def param = [:]) {
    def url = param.url
    def dir = param.dir ?: "."
    def credential = param.credential ?: ""
    checkout([$class                : 'SubversionSCM',
              additionalCredentials : [],
              excludedCommitMessages: '',
              excludedRegions       : '',
              excludedRevprop       : '',
              excludedUsers         : '',
              filterChangelog       : false,
              ignoreDirPropChanges  : false,
              includedRegions       : '',
              locations             : [[credentialsId        : "${credential}",
                                        depthOption          : 'infinity',
                                        ignoreExternalsOption: true,
                                        local                : "${dir}",
                                        remote               : "${url}"]],
              workspaceUpdater      : [$class: 'UpdateUpdater']])
}

def stageGetSource(def stageName = "Get Source", def containerName = "") {
    log.info "call"

    stage(stageName, containerName) {
        def projectName = this.originConfig.deploy.project.name
        def appName = this.originConfig.deploy.app.name

        def source = this.originConfig.source
        if (source == null ||
                ObjectUtil.empty(source.type)) {
            def msg = "Please required value [config.source]"
            log.error msg
            throw new Exception(msg)
        }

        Closure closure
        if (source.type == 'GIT') {
            def args = [:]
            args.url = source.url
            if (ObjectUtil.nonEmpty(source.ref)) {
                args.branch = source.ref
            }
            if (ObjectUtil.nonEmpty(source.credential)) {
                args.credential = source.credential
            }

            closure = {
                checkoutGitSource(args)
            }
        } else if (source.type == 'SVN') {
            def args = [:]
            args.url = source.url
            if (ObjectUtil.nonEmpty(source.credential)) {
                args.credential = source.credential
            }
            closure = {
                checkoutSvnSource(args)
            }

        } else if (source.type == 'UPLOAD') {
            def appType = getFolderName()
            def url = "$ACCORDION_URL/projects/${projectName}/${appType}s/${appName}/storage/${source.file}"
            def downloadShell = "set +x; wget --header='Authorization: Bearer ${ACCORDION_TOKEN}' ${url}"

            if (ObjectUtil.isFileType(source.file, 'zip')) {
                closure = {
                    sh "${downloadShell}"
                    sh "unzip ${source.file} -d ./"
                    sh "ls -al"
                    sh "rm -rf ${source.file}"
                    sh "ls -al"
                }
            } else if (ObjectUtil.isFileType(source.file, 'tar')) {
                closure = {
                    sh "${downloadShell}"
                    sh "ls -al"
                    sh "tar -xvf ${source.file}"
                    sh "rm -rf ${source.file}"
                    sh "ls -al"
                }
            } else if (ObjectUtil.isFileType(source.file, 'war') || ObjectUtil.isFileType(source.file, 'ear')) {
                closure = {
                    sh "${downloadShell}"
                }
            } else {
                def msg = "Upload File Format not support"
                log.error msg
                throw new Exception(msg)
            }

        } else {
            def msg = "Not support sourceType [${source.type}]"
            log.error msg
            throw new Exception(msg)
        }

        closure.call()
    }
}

def stageSourceBuild(def stageName = "", def containerName = "") {
    log.info "call"
    def source = this.originConfig.source
    if (source != null && source.type != null && source.type == 'UPLOAD') {
        log.warning "sourceType[upload] build skip"
        return
    }

    def _stageName
    def _containerName
    def cmd
    if (fileExists("./pom-accordion.xml")) {
        _stageName = 'Build a Maven project'
        _containerName = 'maven'
        cmd = "mvn -f pom-accordion.xml clean package"
    } else if (fileExists("./pom.xml")) {
        _stageName = 'Build a Maven project'
        _containerName = 'maven'
        cmd = "mvn clean package"
    } else if (fileExists("./build-accordion.xml")) {
        _stageName = 'Build a Ant project'
        _containerName = 'ant'
        cmd = "ant -f build-accordion.xml"
    } else if (fileExists("./build.xml")) {
        _stageName = 'Build a Ant project'
        _containerName = 'ant'
        cmd = "ant -f build.xml"
    } else {
        log.warning "Not support project build"
        return
    }

    if (log.isDebugEnabled()) {
        cmd += " -debug"
    }
    stage(ObjectUtil.safeValue(stageName, _stageName), ObjectUtil.safeValue(containerName, _containerName)) {
        sh cmd
    }
}

def stageMultiArchiveDockerBuild(def stageName = "MultiArchive Docker Build", def containerName = "kaniko") {
    stageDockerBuild(stageName, containerName, [archiveMoveClosure: {
        if (log.isDebugEnabled() && this.originConfig.source.type != "UPLOAD") {
            log.debug "user input 'source contextPath' ignored"
        }
        sh """
find $WORKSPACE -name '*.war' -type f -exec mv {} $WORKSPACE/docker/deployment \\;
find $WORKSPACE -name '*.ear' -type f -exec mv {} $WORKSPACE/docker/deployment \\;
"""
    }])
}

def stageDockerBuild(def stageName = "Docker Build", def containerName = "kaniko", def option = [:]) {
    log.info "call"

    stage(stageName, containerName) {
        def deploy = this.originConfig.deploy
        def image = this.originConfig.image
        if (ObjectUtil.empty(image.name) ||
                ObjectUtil.empty(image.tag)) {
            def msg = "Please required value [config.image.name, config.image.tag]"
            log.error msg
            throw new Exception(msg)
        }

        def dockerfile = this.originConfig.dockerfile

        def wasType = deploy.type == "TOMCAT" || deploy.type == "WILDFLY"
        def dockerfilePath = wasType ? './docker' : './'

        log.debug "wasType : ${wasType}"
        if (wasType) {
            def source = this.originConfig.source
            if (source == null) {
                def msg = "Please required value [Source]"
                log.error msg
                throw new Exception(msg)
            }

            def typeString = deploy.type.toLowerCase()
            def mvRunner = libraryResource "accordion-${typeString}-runner.sh"
            def basicDockerfile = libraryResource "accordion-${typeString}-Dockerfile"

            Closure archiveMoveClosure = option.archiveMoveClosure ?: {
                if (source.type == "UPLOAD") {
                    try {
                        sh "mv $WORKSPACE/*.war $WORKSPACE/docker/deployment"
                    } catch (ignored) {
                    }
                    if (deploy.type == "WILDFLY") {
                        try {
                            sh "mv $WORKSPACE/*.ear $WORKSPACE/docker/deployment"
                        } catch (ignored) {
                        }
                    }
                } else {
                    def contextPath = ObjectUtil.safeValue(source.contextPath, "ROOT")
                    sh """
find $WORKSPACE -name *.war -type f -exec mv {} $WORKSPACE/docker/deployment/${contextPath}.war \\;
find $WORKSPACE -name *.ear -type f -exec mv {} $WORKSPACE/docker/deployment/${contextPath}.ear \\;
"""
                }
            }

            sh """
rm -rf docker
mkdir -p .config
mkdir -p .lib
mkdir -p docker/deployment
"""
            writeFile file: "accordion-${typeString}-runner.sh", text: mvRunner
            writeFile file: "accordion-${typeString}-Dockerfile", text: basicDockerfile

            if (log.isDebugEnabled()) {
                sh 'ls -la'
            }

            sh """
mv $WORKSPACE/accordion-${typeString}-runner.sh $WORKSPACE/docker/accordion-${typeString}-runner.sh
mv $WORKSPACE/accordion-${typeString}-Dockerfile $WORKSPACE/docker/Dockerfile
"""
            sh """
mv $WORKSPACE/.config docker/deployment/config
mv $WORKSPACE/.lib docker/deployment/lib
"""
            archiveMoveClosure.call()

            if (log.isDebugEnabled()) {
                sh """
ls -al docker/
ls -al docker/deployment/
ls -al docker/deployment/config/
ls -al docker/deployment/lib/
"""
            }
        } else {
            if (ObjectUtil.empty(dockerfile) &&
                    !fileExists("./Dockerfile")) {
                log.warning "Dockerfile not found"
                def basicDockerfile

                if (fileExists("./requirements.txt")) {
                    log.debug "requirements.txt file exists"

                    basicDockerfile = libraryResource 'accordion-python-Dockerfile'
                    writeFile file: "${dockerfilePath}/Dockerfile", text: basicDockerfile
                } else if (fileExists("./index.php")) {
                    log.debug "index.php file exists"

                    basicDockerfile = libraryResource 'accordion-php-Dockerfile'
                    writeFile file: "${dockerfilePath}/Dockerfile", text: basicDockerfile
                } else if (fileExists("./Gemfile")) {
                    log.debug "Gemfile file exists"

                    basicDockerfile = libraryResource 'accordion-ruby-Dockerfile'
                    writeFile file: "${dockerfilePath}/Dockerfile", text: basicDockerfile
                } else if (fileExists("./package.json")) {
                    log.debug "package.json file exists"

                    basicDockerfile = libraryResource 'accordion-node-Dockerfile'
                    writeFile file: "${dockerfilePath}/Dockerfile", text: basicDockerfile
                }
            }
        }

        if (ObjectUtil.nonEmpty(dockerfile)) {
            log.info "accordion user Dockerfile load"
            log.debug "Dockerfile :\n${dockerfile}"

            writeFile file: "${dockerfilePath}/Dockerfile", text: dockerfile
        }

        log.info "kaniko use docker build skip"

        //sh "docker build --label accordions.io/creator --label accordions.io/tag=${image.tag} -t ${image.name}:${image.tag} ${dockerfilePath}"
        //if (log.isDebugEnabled()) {
            //try {
                //log.debug "docker inspect..."
                //sh "docker inspect ${image.name}:${image.tag}"
            //} catch (ignored) {
            //}
        //}
    }
}

def stageDockerPush(def stageName = "Docker Push", def containerName = "kaniko") {
    log.info "call"

    stage(stageName, containerName) {
        def deploy = this.originConfig.deploy
        def image = this.originConfig.image
        if (ObjectUtil.empty(image.name) ||
                ObjectUtil.empty(image.tag)) {
            def msg = "Please required value [config.image.name, config.image.tag]"
            log.error msg
            throw new Exception(msg)
        }
        if (ObjectUtil.empty(originRegistry.host)) {
            def msg = "Please required value [registry.host]"
            log.error msg
            throw new Exception(msg)
        }

        def wasType = deploy.type == "TOMCAT" || deploy.type == "WILDFLY"
        def dockerfilePath = wasType ? './docker' : './'

sh """
set +x;
echo \"{\\\"auths\\\":{\\\"${originRegistry.host}/\\\":{\\\"auth\\\":\\\"`echo -n ${ObjectUtil.safeValue(originRegistry.username)}:${ObjectUtil.safeValue(originRegistry.password)} | base64`\\\"}}}\" > /kaniko/.docker/config.json
executor --insecure --skip-tls-verify --label accordions.io/creator --label accordions.io/tag=${image.tag} --context=dir://${dockerfilePath} --dockerfile=${dockerfilePath}/Dockerfile --destination=${image.name}:${image.tag}
"""
    }
}

def stageDeploy(def stageName = "Deploy", def containerName = "") {
    log.info "call"
    def projectName = this.originConfig.deploy.project.name
    def appName = this.originConfig.deploy.app.name
    def appType = getFolderName()

    lock {
        stage(stageName, containerName) {
            echo httpUtil.post(
                    url: "$ACCORDION_URL/projects/${projectName}/${appType}s/${appName}/builds/$BUILD_NUMBER/deploy",
                    headers: ["Authorization": " Bearer $ACCORDION_TOKEN"],
                    body: "${ObjectUtil.toJson(this.originConfig)}"
            )
        }
    }
}

def getFolderName() {
    def array = pwd().split("/")
    return array[array.length - 2];
}

return this
