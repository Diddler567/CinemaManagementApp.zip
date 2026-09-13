package com.example.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloControllerTest {

    @Test
    void homeReturnsOkAndBody() {
        HelloController controller = new HelloController();
        var response = controller.home();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("OK - backend running!", response.getBody());
        assertEquals("text/plain", response.getHeaders().getContentType().getType() + "/" + response.getHeaders().getContentType().getSubtype());
    }
}
