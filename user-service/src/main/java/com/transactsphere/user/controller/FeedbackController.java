package com.transactsphere.user.controller;

import com.transactsphere.user.model.Feedback;
import com.transactsphere.user.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;

    @PostMapping
    public ResponseEntity<Feedback> submitFeedback(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String username,
            @RequestBody Feedback feedback) {
        feedback.setUserId(userId);
        feedback.setUsername(username);
        Feedback saved = feedbackRepository.save(feedback);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Feedback>> getAllFeedback(
            @RequestHeader("X-User-Roles") String roles) {
        if (roles == null || (!roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_EMPLOYEE"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(feedbackRepository.findAll());
    }
}
