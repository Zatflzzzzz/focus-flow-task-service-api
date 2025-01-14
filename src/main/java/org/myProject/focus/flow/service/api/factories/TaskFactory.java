package org.myProject.focus.flow.service.api.factories;

import org.myProject.focus.flow.service.api.dto.TaskDto;
import org.myProject.focus.flow.service.api.dto.TaskViewDto;
import org.myProject.focus.flow.service.store.entities.TaskEntity;
import org.myProject.focus.flow.service.store.entities.TaskViewEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskFactory {

    public TaskDto makeTaskViewDto(TaskEntity entity) {

        return TaskDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .priority(entity.getPriority())
                .updatedAt(entity.getUpdatedAt())
                .deadline(entity.getDeadline())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
