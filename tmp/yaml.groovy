podTemplate(yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    some-label: some-label-value
spec:
  containers:
  - name: busybox
    image: busybox
    command:
    - cat
    tty: true
"""
    ,
containers: [
            containerTemplate(name: 'ant', image: 'accordion/ant', ttyEnabled: true, command: 'cat'),
            containerTemplate(name: 'maven', image: 'maven:3.6.0-jdk-8-alpine', ttyEnabled: true, command: 'cat'),
            containerTemplate(name: 'docker', image: "docker:18.06.3-ce", ttyEnabled: true, command: 'cat', privileged: true, instanceCap: 1)
    ],
    volumes : [
            nfsVolume(serverAddress: "10.20.200.201", serverPath: "/nfs/data/jenkins/.m2", mountPath: '/root/.m2'),
            nfsVolume(serverAddress: "10.20.200.201", serverPath: "/nfs/data/jenkins/workspace", mountPath: '/root/workspace'),
            hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
      ],
      showRawYaml : true
    ) {
  node(POD_LABEL) {
    container('maven') {
      sh "echo $HOME"
      sh """
      mvn org.apache.maven.plugins:maven-dependency-plugin:2.1:get \
    -DrepoUrl=url \
    -Dartifact=io.kubernetes:client-java:6.0.1
      """
      sh 'echo $HOME'
      sh "echo ~"
      sh 'ls -la $HOME'
      sh "ls -la -R ~/.m2/repository/io/kubernetes/client-java"
      sh "ls -la -R /root/.m2/repository/io/kubernetes/client-java"
    }
  }
}
