package org.example.laptopstore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//public class ResourceNotFoundException extends RuntimeException {
//    public ResourceNotFoundException(String message) {
//        super(message);
//    }
//}

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
