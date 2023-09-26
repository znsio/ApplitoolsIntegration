package com.znsio.applitools.integration;

class CommandLineResponse {
    private int exitCode;
    private String stdOut;
    private String errOut;

    int getExitCode() {
        return exitCode;
    }

    void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    String getStdOut() {
        return stdOut;
    }

    void setStdOut(String stdOut) {
        this.stdOut = stdOut;
    }

    String getErrOut() {
        return errOut;
    }

    void setErrOut(String errOut) {
        this.errOut = errOut;
    }

    @Override
    public String toString() {
        return "CommandLineResponse [exitCode=" + exitCode + ", stdOut=" + stdOut + ", errOut=" + errOut + "]";
    }
}
