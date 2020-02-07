podTemplate(yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    some-label: some-label-value
spec:
  containers:
  - name: busybox
    image: busybox:1
    command:
    - cat
    tty: true
"""
    ,
containers: [
            containerTemplate(name: 'busybox', image: 'busybox:1.31.1', ttyEnabled: true, command: 'cat'),
    ],
      showRawYaml : true
    ) {
  node(POD_LABEL) {
    container('busybox') {
      sh "hostname"
    }
  }
}
