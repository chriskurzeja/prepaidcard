package uk.co.chriskurzeja.prepaidcard.controllers.utils;

import io.atlassian.fugue.Either;
import org.springframework.http.ResponseEntity;

public class EitherUtils {

    public static <L,R> ResponseEntity<?> eitherToResponse(Either<L,R> result) {
        return result.isRight() ?
                ResponseEntity.ok(result.right().get()) :
                ResponseEntity.badRequest().body(result.left().get());
    }

}
