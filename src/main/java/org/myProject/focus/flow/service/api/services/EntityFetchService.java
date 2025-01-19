package org.myProject.focus.flow.service.api.services;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.myProject.focus.flow.service.store.entities.TaskStateEntity;
import org.myProject.focus.flow.service.store.repositories.ProjectRepository;
import org.myProject.focus.flow.service.store.repositories.TaskRepository;
import org.myProject.focus.flow.service.store.repositories.TaskStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
@Transactional
public class EntityFetchService {

    ProjectRepository projectRepository;

    TaskStateRepository taskStateRepository;

    public ProjectEntity getProjectOrThrowException(Long projectId) {

        return projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new CustomAppException(
                                HttpStatus.NOT_FOUND,
                                String.format("Project with id (%s) doesn't exist", projectId)
                        ));
    }

    public List<TaskStateEntity> getSortedTaskStates(Long projectId) {

        TaskStateEntity initialStateEntity = taskStateRepository
                .findTaskStateEntityByLeftTaskStateIsNullAndProjectId(projectId)
                .orElse(null);

        return buildSortedTaskStates(initialStateEntity);
    }

    private List<TaskStateEntity> buildSortedTaskStates(TaskStateEntity startTaskState) {
        return Stream.iterate(
                startTaskState,
                Objects::nonNull,
                taskState -> taskState.getRightTaskState().orElse(null)
        ).collect(Collectors.toList());
    }

    public TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                        String.format("Task state with id \"%s\" not found", taskStateId))
                );
    }
}
