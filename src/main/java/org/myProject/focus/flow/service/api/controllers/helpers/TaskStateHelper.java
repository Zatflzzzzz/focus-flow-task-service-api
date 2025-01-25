package org.myProject.focus.flow.service.api.controllers.helpers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.myProject.focus.flow.service.store.entities.TaskStateEntity;
import org.myProject.focus.flow.service.store.repositories.TaskStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class TaskStateHelper {

    TaskStateRepository taskStateRepository;

    ValidateRequestsHelper validateRequestsHelper;

    public TaskStateEntity getTaskStateOrThrowException(Long taskStateId, Long userId) {

        TaskStateEntity taskState = taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                        String.format("Task state with id \"%s\" not found", taskStateId))
                );

        validateRequestsHelper.verifyingUserAccessToProject(taskState.getProject().getUserId(), userId);

        return taskState;
    }

    public List<TaskStateEntity> getSortedTaskStates(Long projectId) {

        TaskStateEntity initialEntity = taskStateRepository
                .findTaskStateEntityByLeftTaskStateIsNullAndProjectId(projectId)
                .orElse(null);

        return buildSortedTaskStates(initialEntity);
    }

    private List<TaskStateEntity> buildSortedTaskStates(TaskStateEntity initialEntity) {

        return Stream
                .iterate(
                        initialEntity,
                        Objects::nonNull,
                        taskState -> taskState.getRightTaskState().orElse(null))
                .collect(Collectors.toList());
    }

    public boolean isPositionUnchanged(
            TaskStateEntity selectedTaskState,
            Optional<Long> optionalRightTaskStateId) {

        Optional<Long> optionalOldRightTaskStateId = selectedTaskState
                .getRightTaskState()
                .map(TaskStateEntity::getId);

        return optionalOldRightTaskStateId.equals(optionalRightTaskStateId);
    }

    public Optional<TaskStateEntity> resolveNewRightTaskState(
            Optional<Long> optionalRightTaskStateId,
            Long taskStateId,
            ProjectEntity project) {

        return optionalRightTaskStateId.map(rightTaskStateId -> {

            if (taskStateId.equals(rightTaskStateId)) {
                throw new CustomAppException(HttpStatus.BAD_REQUEST, "Right task state id equals changed task state.");
            }

            TaskStateEntity rightTaskStateEntity = taskStateRepository
                    .findById(rightTaskStateId)
                    .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                            String.format("Task state with id \"%s\" not found", rightTaskStateId))
                    );

            if (!project.getId().equals(rightTaskStateEntity.getProject().getId())) {
                throw new CustomAppException(HttpStatus.BAD_REQUEST, "Task state position can be changed within the same project.");
            }

            return rightTaskStateEntity;
        });
    }

    public Optional<TaskStateEntity> resolveNewLeftTaskState(
            Optional<TaskStateEntity> optionalNewRightTaskState,
            ProjectEntity project) {

        if (!optionalNewRightTaskState.isPresent()) {
            return taskStateRepository.findTaskStateEntityByRightTaskStateIsNullAndProjectId(project.getId());
        } else {
            return optionalNewRightTaskState.get().getLeftTaskState();
        }
    }

    public void replaceOldTaskStatesPosition(TaskStateEntity taskState) {

        Optional<TaskStateEntity> optionalLeftTaskState = taskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalRightTaskState = taskState.getRightTaskState();

        taskState.setRightTaskState(null);
        taskState.setLeftTaskState(null);

        taskStateRepository.saveAndFlush(taskState);

        optionalLeftTaskState.ifPresent(it -> {

            it.setRightTaskState(optionalRightTaskState.orElse(null));

            taskStateRepository.saveAndFlush(it);
        });

        optionalRightTaskState.ifPresent(it -> {

            it.setLeftTaskState(optionalLeftTaskState.orElse(null));

            taskStateRepository.saveAndFlush(it);
        });
    }

    public TaskStateEntity updateTaskStatePosition(
            TaskStateEntity selectedTaskState,
            Optional<TaskStateEntity> optionalNewLeftTaskState,
            Optional<TaskStateEntity> optionalNewRightTaskState) {

        if (optionalNewLeftTaskState.isPresent()) {

            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();

            newLeftTaskState.setRightTaskState(selectedTaskState);

            selectedTaskState.setLeftTaskState(newLeftTaskState);
        } else {
            selectedTaskState.setLeftTaskState(null);
        }

        if (optionalNewRightTaskState.isPresent()) {

            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();

            newRightTaskState.setLeftTaskState(selectedTaskState);

            selectedTaskState.setRightTaskState(newRightTaskState);
        } else {
            selectedTaskState.setRightTaskState(null);
        }

        optionalNewRightTaskState.ifPresent(taskStateRepository::saveAndFlush);
        optionalNewLeftTaskState.ifPresent(taskStateRepository::saveAndFlush);

        selectedTaskState = taskStateRepository.saveAndFlush(selectedTaskState);

        return selectedTaskState;
    }
}
