package io.accordions.util

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

class ObjectUtil {
    static def toObject(def json) {
        def jsonSlurper = new JsonSlurperClassic()
        def obj = jsonSlurper.parseText(json)
        return obj
    }

    static def toJson(def obj) {
        return JsonOutput.toJson(obj)
    }

    static boolean empty(def s) {
        if (s && s != "") {
            return false
        }
        return true
    }

    static boolean nonEmpty(def s) {
        return !empty(s)
    }

    static def safeValue(def s, def d = '') {
        if (empty(s)) {
            return d
        } else {
            return s
        }
    }

    static boolean isFileType(def fileName, def checkExt) {
        if (empty(fileName) || empty(checkExt)) {
            return false
        }
        int index = fileName.lastIndexOf(".")
        if (index == -1) {
            return false
        }
        def ext = fileName.substring(index + 1)
        return ext.toUpperCase() == checkExt.toUpperCase()
    }
}
