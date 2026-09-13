package com.example.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


@RestController
public class HelloController {
  @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> home() {
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_PLAIN)
        .body("OK - backend running!");
  }
}
