package com.taskflow.task;

import com.taskflow.task.dto.CreateLabelRequest;
import com.taskflow.task.dto.LabelResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public LabelResponse create(@Valid @RequestBody CreateLabelRequest request) {
        return labelService.createLabel(request);
    }

    @GetMapping
    public List<LabelResponse> list() {
        return labelService.listLabels();
    }
}
