package com.com.ai_quiz_app.backend;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("some URL, any url starting with this should be in this class")
public class FrontIntegration {

    @PostMapping("the link the post message is sent to")
    public String handleAnswer(@RequestBody StudentData data) { // the object that is received from front
        
    	// 'data' now holds the ID (101) and Answer from front
        System.out.println("Received: " + data.getAnswer());
        
        return "SUCCESS"; // This string goes back to the HTML
    }
    
    public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}