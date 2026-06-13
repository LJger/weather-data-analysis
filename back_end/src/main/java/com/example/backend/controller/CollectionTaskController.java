package com.example.backend.controller;

import com.example.backend.dto.CollectionTaskResponse;
import com.example.backend.dto.CreateCollectionTaskRequest;
import com.example.backend.dto.UpdateTaskStatusRequest;
import com.example.backend.service.CollectionTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collection-tasks")
public class CollectionTaskController {

    private final CollectionTaskService collectionTaskService;

    public CollectionTaskController(CollectionTaskService collectionTaskService) {
        this.collectionTaskService = collectionTaskService;
    }

    @PostMapping
    public ResponseEntity<CollectionTaskResponse> createTask(
            @Valid @RequestBody CreateCollectionTaskRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionTaskService.createTask(request));
    }

    @GetMapping
    public ResponseEntity<List<CollectionTaskResponse>> listTasks(
            @RequestParam Integer userId
    ) {
        return ResponseEntity.ok(collectionTaskService.listTasks(userId));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<CollectionTaskResponse> getTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(collectionTaskService.getTask(taskId));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<CollectionTaskResponse> updateStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return ResponseEntity.ok(collectionTaskService.updateStatus(taskId, request));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        collectionTaskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}


