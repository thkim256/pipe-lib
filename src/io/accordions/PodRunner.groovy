package io.accordions

import groovy.transform.Field
import io.accordions.logger.Logger
import io.accordions.logger.LoggerFactory
import io.accordions.util.ObjectUtil

@Field Logger log = new Logger()

def run(option, Closure closure) {
    log = LoggerFactory.get(option.logger)
    log.debug "call"

    def kubernetes = option.kubernetes ?: [:]
    log.debug "option.kubernetes : ${kubernetes}"

    kubernetes.yaml = "${ACCORDION_KUBERNETES_SLAVE_YAML}"
    kubernetes.yaml = """
apiVersion: v1
kind: Pod
spec:
  affinity:
    nodeAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
      - preference:
          matchExpressions:
          - key: accordion-role
            operator: In
            values:
            - infra
        weight: 100
  containers:
    - name: jnlp
      image: jenkins/jnlp-slave:3.35-5-alpine
    - name: maven
      image: maven:3.6.0-jdk-8-alpine
      tty: true
      command:
        - cat
      volumeMounts:
        - mountPath: /root/.m2
          name: vol-m2
    - name: ant
      image: accordion/ant:1.14
      tty: true
      command:
        - cat
    - name: kaniko
      image: gcr.io/kaniko-project/executor:debug-v0.17.0
      tty: true
      command:
        - cat
  volumes:
    - name: vol-m2
      nfs:
        path: ${ACCORDION_KUBERNETES_NFS_PATH}/jenkins/.m2
        server: ${ACCORDION_KUBERNETES_NFS_SERVER}
    """
    if (ObjectUtil.empty(kubernetes.showRawYaml)) {
        kubernetes.showRawYaml = false
    }

    podTemplate(kubernetes) {
        node(kubernetes.label ?: POD_LABEL) {
            closure.call()
        }
    }
}

return this
