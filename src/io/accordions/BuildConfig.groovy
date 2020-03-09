package io.accordions

import groovy.transform.Field

@Field Source source = new Source()
@Field Deploy deploy = new Deploy()
@Field Image image = new Image()
@Field String dockerfile = ""

class Source {
    def type
    def url
    def ref
    def file
    def contextPath
    def credential
}

class Deploy {
    def type
    class App {
        def name
    }

    class Project {
        def name
    }
    App app
    Project project

    def summary
    def description
}

class Image {
    def name
    def tag
    def registryName
}

return this

