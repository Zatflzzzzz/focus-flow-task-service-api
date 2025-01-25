package org.myProject.focus.flow.service.api.controllers.helpers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.store.entities.TaskEntity;
import org.myProject.focus.flow.service.store.repositories.TaskRepository;
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
public class TaskHelper {
    
    TaskRepository taskRepository;

    ValidateRequestsHelper validateRequestsHelper;

    public TaskEntity getTaskOrThrowException(Long taskId, Long userId) {

        TaskEntity task = taskRepository
                .findById(taskId)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                        String.format("Task with id %s not found", taskId))
                );

        validateRequestsHelper.verifyingUserAccessToProject(task.getTaskState().getProject().getUserId(), userId);

        return task;
    }

    public List<TaskEntity> getSortedTasks(Long taskStateId) {

        TaskEntity initialEntity = taskRepository
                .findTaskEntityByHigherPriorityTaskIsNullAndTaskStateId(taskStateId)
                .orElse(null);

        return buildSortedTasks(initialEntity);
    }

    private List<TaskEntity> buildSortedTasks(TaskEntity initialEntity) {

        return Stream
                .iterate(
                        initialEntity,
                        Objects::nonNull,
                        task -> task.getLowerPriorityTask().orElse(null))
                .collect(Collectors.toList());
    }

    public boolean isPositionChanged(
            TaskEntity selectedTask,
            Optional<Long> optionalLowerPriorityTaskId) {

        Optional<Long> optionalOldLowerPriorityTaskId = selectedTask
                .getLowerPriorityTask()
                .map(TaskEntity::getId);

        return optionalOldLowerPriorityTaskId.equals(optionalLowerPriorityTaskId);
    }

    public Optional<TaskEntity> getNewHigherPriorityTask(
            Optional<TaskEntity> optionalNewLowerPriorityTask,
            TaskEntity selectedTask) {
        Optional<TaskEntity> optionalNewHigherPriorityTask;

        if (!optionalNewLowerPriorityTask.isPresent()) {

            optionalNewHigherPriorityTask = taskRepository
                    .findTaskEntityByLowerPriorityTaskIsNullAndTaskStateId(selectedTask.getTaskState().getId());
        } else {

            optionalNewHigherPriorityTask = optionalNewLowerPriorityTask
                    .get()
                    .getHigherPriorityTask();
        }
        return optionalNewHigherPriorityTask;
    }

    public Optional<TaskEntity> getNewLowerPriorityTask(
            Long taskId,
            Optional<Long> optionalLowerPriorityTaskId) {

        return optionalLowerPriorityTaskId
                .map(lowerPriorityTaskId -> {

                    if(taskId.equals(lowerPriorityTaskId)){
                        throw new CustomAppException(HttpStatus.BAD_REQUEST, "higher priority task equal to selected task");
                    }

                    return taskRepository
                            .findById(lowerPriorityTaskId)
                            .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                                    String.format("Task with id %s not found", lowerPriorityTaskId))
                            );
                });
    }

    public void replaceOldTasksPositions(TaskEntity task) {

        Optional<TaskEntity> optionalOldHigherPriorityTaskState = task.getHigherPriorityTask();
        Optional<TaskEntity> optionalOldLowerPriorityTaskState = task.getLowerPriorityTask();

        task.setHigherPriorityTask(null);
        task.setLowerPriorityTask(null);

        taskRepository.saveAndFlush(task);

        optionalOldHigherPriorityTaskState.ifPresent(it -> {

            it.setLowerPriorityTask(optionalOldLowerPriorityTaskState.orElse(null));

            taskRepository.saveAndFlush(it);
        });

        optionalOldLowerPriorityTaskState.ifPresent(it -> {

            it.setHigherPriorityTask(optionalOldHigherPriorityTaskState.orElse(null));

            taskRepository.saveAndFlush(it);
        });
    }

    public TaskEntity updateTaskPosition(
            TaskEntity selectedTask,
            Optional<TaskEntity> optionalNewLowerPriorityTask,
            Optional<TaskEntity> optionalNewHigherPriorityTask) {

        if (optionalNewLowerPriorityTask.isPresent()) {

            TaskEntity newLowerPriorityTask = optionalNewLowerPriorityTask.get();

            newLowerPriorityTask.setHigherPriorityTask(selectedTask);

            selectedTask.setLowerPriorityTask(newLowerPriorityTask);

        } else {
            selectedTask.setLowerPriorityTask(null);
        }

        if (optionalNewHigherPriorityTask.isPresent()) {

            TaskEntity newHigherPriorityTask = optionalNewHigherPriorityTask.get();

            newHigherPriorityTask.setLowerPriorityTask(selectedTask);

            selectedTask.setHigherPriorityTask(newHigherPriorityTask);

        } else {
            selectedTask.setHigherPriorityTask(null);
        }

        optionalNewHigherPriorityTask.ifPresent(taskRepository::saveAndFlush);
        optionalNewLowerPriorityTask.ifPresent(taskRepository::saveAndFlush);

        return taskRepository.saveAndFlush(selectedTask);
    }
}
