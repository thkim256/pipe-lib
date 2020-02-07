package io.accordions.logger

class LoggerFactory {
    static Logger get(def opt = "") {
        Logger log = new Logger()
        def level = LOG_LEVEL.INFO.toString()
        def ansiColor = true
        if (opt) {
            if (opt.level instanceof String) {
                level = opt.level
            }
            if (opt.ansiColor instanceof boolean) {
                ansiColor = opt.ansiColor
            }
        }
        log.setLevel(level)
        log.setColor(ansiColor)
        return log
    }
}
