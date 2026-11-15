package com.project.job_finder_ai.exception;

public class PipelineBusyException extends RuntimeException {
    public PipelineBusyException() {
        super("A pipeline run is already in progress");
    }
    public PipelineBusyException(String message) {
           super(message);
       }
}