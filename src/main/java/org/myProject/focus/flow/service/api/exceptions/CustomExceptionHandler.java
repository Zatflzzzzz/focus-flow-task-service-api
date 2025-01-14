package org.myProject.focus.flow.service.api.exceptions;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Log4j2
@ControllerAdvice
public class CustomExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDto> handleAllExceptions(Exception ex) {
    log.error("Exception occurred: ", ex);

    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    String message = ex.getMessage();

    if (ex instanceof CustomAppException customAppException) {
      status = customAppException.getStatus();
      message = customAppException.getMessage();
    }

    return ResponseEntity
            .status(status)
            .body(ErrorDto.builder()
                    .error(status.getReasonPhrase())
                    .errorDescription(message)
                    .build());
  }
}
