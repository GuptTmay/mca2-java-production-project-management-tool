package com.taskflow.task;

import com.taskflow.common.exception.DuplicateResourceException;
import com.taskflow.task.dto.CreateLabelRequest;
import com.taskflow.task.dto.LabelResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock
    private LabelRepository labelRepository;
    @Mock
    private LabelMapper labelMapper;

    private LabelService labelService;

    @BeforeEach
    void setUp() {
        labelService = new LabelService(labelRepository, labelMapper);
    }

    @Test
    void createLabel_rejectsDuplicateName() {
        CreateLabelRequest request = new CreateLabelRequest("Bug", "#EF4444");
        when(labelRepository.existsByName("Bug")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> labelService.createLabel(request));
    }

    @Test
    void createLabel_savesTrimmedNameAndReturnsMappedResponse() {
        CreateLabelRequest request = new CreateLabelRequest("Bug", "#EF4444");
        Label saved = new Label("Bug", "#EF4444");
        when(labelRepository.existsByName("Bug")).thenReturn(false);
        when(labelRepository.save(any(Label.class))).thenReturn(saved);
        when(labelMapper.toResponse(saved)).thenReturn(new LabelResponse(UUID.randomUUID(), "Bug", "#EF4444"));

        LabelResponse response = labelService.createLabel(request);

        assertThat(response.name()).isEqualTo("Bug");
        assertThat(response.color()).isEqualTo("#EF4444");
    }

    @Test
    void listLabels_mapsAllLabels() {
        Label label = new Label("Bug", "#EF4444");
        when(labelRepository.findAll()).thenReturn(List.of(label));
        when(labelMapper.toResponse(label)).thenReturn(new LabelResponse(UUID.randomUUID(), "Bug", "#EF4444"));

        List<LabelResponse> responses = labelService.listLabels();

        assertThat(responses).hasSize(1);
    }
}
