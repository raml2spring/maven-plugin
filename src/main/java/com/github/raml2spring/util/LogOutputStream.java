package com.github.raml2spring.util;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.maven.plugin.logging.Log;

import java.io.PrintStream;

public class LogOutputStream extends PrintStream {

    private Log log;

    LogOutputStream(Log log) {
        super(new NullOutputStream());
        this.log = log;
    }

    public void println(String text) {
        log.info(text);
    }
}
