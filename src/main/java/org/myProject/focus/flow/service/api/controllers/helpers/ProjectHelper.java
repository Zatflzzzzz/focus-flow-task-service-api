package org.myProject.focus.flow.service.api.controllers.helpers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.myProject.focus.flow.service.store.repositories.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
@Transactional
public class ProjectHelper {

    ProjectRepository projectRepository;

    ValidateRequestsHelper validateRequestsHelper;

    public ProjectEntity getProjectOrThrowException(Long projectId, Long userId) {

        ProjectEntity project = projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new CustomAppException(
                                HttpStatus.NOT_FOUND,
                                String.format("Project with id (%s) doesn't exist", projectId)
                        ));

        validateRequestsHelper.verifyingUserAccessToProject(project.getUserId(), userId);

        return project;
    }
}
