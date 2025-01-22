package org.myProject.focus.flow.service.api.factories;

import org.myProject.focus.flow.service.api.dto.ProjectDto;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectDtoFactory {

    public ProjectDto makeProjectDto(ProjectEntity entity) {

        return ProjectDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .userId(entity.getUserId())
                .build();
    }
}
