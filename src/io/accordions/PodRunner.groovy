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
