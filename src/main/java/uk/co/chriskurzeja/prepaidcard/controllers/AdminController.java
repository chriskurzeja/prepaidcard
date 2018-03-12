package uk.co.chriskurzeja.prepaidcard.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.chriskurzeja.prepaidcard.card.UserService;

import static uk.co.chriskurzeja.prepaidcard.controllers.utils.EitherUtils.eitherToResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(path = "users", method = RequestMethod.GET)
    ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(userService.getUserIds());
    }

    @RequestMapping(path = "users/user/cardId/{userId}", method = RequestMethod.GET)
    ResponseEntity<?> getCardIdForUser(@RequestParam String userId) {
        return eitherToResponse(userService.getCardIdForUser(userId));
    }

}
