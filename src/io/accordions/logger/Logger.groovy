package io.accordions.logger

import groovy.transform.Field
import io.accordions.util.ObjectUtil
import org.codehaus.groovy.runtime.StackTraceUtils

@Field def level
@Field boolean color

def setLevel(String level) {
    if (level) {
        this.level = LOG_LEVEL.valueOf(level.toUpperCase())
    }
}

def setColor(boolean color) {
    if (color) {
        try {
            ansiColor('xterm') {}
            this.color = color
        } catch (ignored) {
        }
    }
}

enum LOG_LEVEL {
    IGNORE(""),
    ERROR("31"),
    WARNING("33"),
    INFO("34"),
    DEBUG("36");

    def ansi

    LOG_LEVEL(def ansi) {
        this.ansi = ansi
    }

    def getMsg(msg) {
        return "[${this}] ${msg}"
    }

    def getAnsiMsg(msg) {
        return "\033[${this.ansi}m${getMsg(msg)}\033[0m"
    }
}

def error(msg, Exception err = null) {
    if (err) {
        _print(msg, LOG_LEVEL.ERROR, StackTraceUtils.sanitize(err).stackTrace[0])
    } else {
        _print(msg, LOG_LEVEL.ERROR)
    }
}

def warning(msg) {
    _print(msg, LOG_LEVEL.WARNING)
}

def info(msg) {
    _print(msg, LOG_LEVEL.INFO)
}

def debug(msg) {
    _print(msg, LOG_LEVEL.DEBUG)
}


def _print(msg, LOG_LEVEL logLevel) {
    def marker = new Throwable()
    def e = StackTraceUtils.sanitize(marker).stackTrace[2] // _print , log method skip
    _print(msg, logLevel, e)
}

def _print(msg, LOG_LEVEL logLevel, StackTraceElement e) {
    if (this.level >= logLevel) {
        def _msg = "${e.className}.${e.methodName}:${e.lineNumber} "
        if (ObjectUtil.nonEmpty(msg)) {
            _msg += msg
        }
        echo this.color ? logLevel.getAnsiMsg(_msg) : logLevel.getMsg(_msg)
    }
}

boolean isDebugEnabled() {
    return this.level == LOG_LEVEL.DEBUG
}

return this

