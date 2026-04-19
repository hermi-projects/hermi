package org.hermi.logging.usecase.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FailedLog {
  private String event;
  private String status = "FAILED";
  private String title;
  private String args;
  private String exceptionClass;
  private String exceptionMessage;
  private List<String> stackTrace;

  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getArgs() {
    return args;
  }

  public void setArgs(String args) {
    this.args = args;
  }

  public String getExceptionClass() {
    return exceptionClass;
  }

  public void setExceptionClass(String exceptionClass) {
    this.exceptionClass = exceptionClass;
  }

  public String getExceptionMessage() {
    return exceptionMessage;
  }

  public void setExceptionMessage(String exceptionMessage) {
    this.exceptionMessage = exceptionMessage;
  }

  public List<String> getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(List<String> stackTrace) {
    this.stackTrace = stackTrace;
  }
}
