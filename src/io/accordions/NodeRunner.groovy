package io.accordions

import groovy.transform.Field
import io.accordions.logger.Logger
import io.accordions.logger.LoggerFactory

@Field Logger log = new Logger()

def run(option, Closure closure) {
    log = LoggerFactory.get(option.logger)
    log.debug "call"

    node {
        closure.call()
    }
}

return this