package org.myProject.focus.flow.service.api.factories;

import org.myProject.focus.flow.service.api.dto.TaskViewDto;
import org.myProject.focus.flow.service.store.entities.TaskViewEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskViewFactory {

    public TaskViewDto makeTaskViewDto(TaskViewEntity entity) {

        return TaskViewDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .ordinal(entity.getOrdinal())
                .typeOfLayout(entity.getTypeOfLayout())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
