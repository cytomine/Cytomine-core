package be.cytomine.api.controller;

import be.cytomine.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@ResponseBody
@RequestMapping("api")
public class WebSocketController extends RestCytomineController {

    @Autowired
    private WebSocketService webSocketService;

    @GetMapping("/sendNotification")
    public ResponseEntity sendOneWebSocket(@RequestParam(value="userID")String userID) throws IOException {
        webSocketService.sendNotification(userID);
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/sendNotifications")
    public ResponseEntity sendMultipleWebSocket() throws IOException {
        webSocketService.sendGlobalNotification();
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/spamUser")
    public ResponseEntity spamUser(@RequestParam(value="userID")String userID, @RequestParam(value="n") int n) {
        webSocketService.spamUser(userID, n);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getUserMessage")
    public Map<String, String> getUserMessage(@RequestParam(value="userID")String userID) {
        HashMap<String, String> message = new HashMap<>();
        message.put("data", webSocketService.getUserMessage(userID));
        return message;
    }
}
