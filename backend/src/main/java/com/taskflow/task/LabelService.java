package com.taskflow.task;

import com.taskflow.common.exception.DuplicateResourceException;
import com.taskflow.task.dto.CreateLabelRequest;
import com.taskflow.task.dto.LabelResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LabelService {

    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;

    public LabelService(LabelRepository labelRepository, LabelMapper labelMapper) {
        this.labelRepository = labelRepository;
        this.labelMapper = labelMapper;
    }

    @Transactional
    public LabelResponse createLabel(CreateLabelRequest request) {
        if (labelRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("A label named '" + request.name() + "' already exists");
        }
        Label saved = labelRepository.save(new Label(request.name().trim(), request.color()));
        return labelMapper.toResponse(saved);
    }

    public List<LabelResponse> listLabels() {
        return labelRepository.findAll().stream()
                .map(labelMapper::toResponse)
                .toList();
    }
}
