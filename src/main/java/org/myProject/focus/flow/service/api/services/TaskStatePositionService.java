package org.myProject.focus.flow.service.api.services;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.myProject.focus.flow.service.store.entities.TaskStateEntity;
import org.myProject.focus.flow.service.store.repositories.TaskStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
@Transactional
public class TaskStatePositionService {

    TaskStateRepository taskStateRepository;

    public void replaceOldTaskStatesPosition(TaskStateEntity taskState) {

        Optional<TaskStateEntity> optionalLeftTaskState = taskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalRightTaskState = taskState.getRightTaskState();

        optionalLeftTaskState.ifPresent(it -> {

            it.setRightTaskState(optionalRightTaskState.orElse(null));

            taskStateRepository.saveAndFlush(it);
        });

        optionalRightTaskState.ifPresent(it -> {

            it.setLeftTaskState(optionalLeftTaskState.orElse(null));

            taskStateRepository.saveAndFlush(it);
        });
    }

    public boolean isPositionUnchanged(
            TaskStateEntity changeTaskState,
            Optional<Long> optionalLeftTaskStateId) {

        Optional<Long> optionalOldLeftTaskStateId = changeTaskState
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        return optionalOldLeftTaskStateId.equals(optionalLeftTaskStateId);
    }

    public Optional<TaskStateEntity> resolveNewLeftTaskState(
            Optional<Long> optionalLeftTaskStateId,
            Long taskStateId,
            ProjectEntity project) {

        return optionalLeftTaskStateId.map(leftTaskStateId -> {

            if (taskStateId.equals(leftTaskStateId)) {
                throw new CustomAppException(HttpStatus.BAD_REQUEST, "Left task state id equals changed task state.");
            }

            TaskStateEntity leftTaskStateEntity = taskStateRepository
                    .findById(leftTaskStateId)
                    .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                            String.format("Task state with id \"%s\" not found", taskStateId))
                    );

            if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) {
                throw new CustomAppException(HttpStatus.BAD_REQUEST, "Task state position can be changed within the same project.");
            }

            return leftTaskStateEntity;
        });
    }

    public Optional<TaskStateEntity> resolveNewRightTaskState(
            Optional<TaskStateEntity> optionalNewLeftTaskState,
            ProjectEntity project) {

        if (!optionalNewLeftTaskState.isPresent()) {
            return taskStateRepository.findTaskStateEntityByLeftTaskStateIsNullAndProjectId(project.getId());
        } else {
            return optionalNewLeftTaskState.get().getRightTaskState();
        }
    }

    public void updateTaskStatePosition(TaskStateEntity changeTaskState,
                                         Optional<TaskStateEntity> optionalNewLeftTaskState,
                                         Optional<TaskStateEntity> optionalNewRightTaskState) {

        if (optionalNewLeftTaskState.isPresent()) {
            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();
            newLeftTaskState.setRightTaskState(changeTaskState);
            changeTaskState.setLeftTaskState(newLeftTaskState);
        } else {
            changeTaskState.setLeftTaskState(null);
        }

        if (optionalNewRightTaskState.isPresent()) {
            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();
            newRightTaskState.setLeftTaskState(changeTaskState);
            changeTaskState.setRightTaskState(newRightTaskState);
        } else {
            changeTaskState.setRightTaskState(null);
        }

        taskStateRepository.saveAndFlush(changeTaskState);
        optionalNewLeftTaskState.ifPresent(taskStateRepository::saveAndFlush);
        optionalNewRightTaskState.ifPresent(taskStateRepository::saveAndFlush);
    }
}
