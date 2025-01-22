package org.myProject.focus.flow.service.api.controllers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.controllers.helpers.TaskHelper;
import org.myProject.focus.flow.service.api.controllers.helpers.TaskStateHelper;
import org.myProject.focus.flow.service.api.controllers.helpers.ValidateRequestsHelper;
import org.myProject.focus.flow.service.api.dto.AckDto;
import org.myProject.focus.flow.service.api.dto.TaskDto;
import org.myProject.focus.flow.service.api.dto.TaskStateDto;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.api.factories.TaskDtoFactory;
import org.myProject.focus.flow.service.store.entities.TaskEntity;
import org.myProject.focus.flow.service.store.entities.TaskStateEntity;
import org.myProject.focus.flow.service.store.entities.enums.Category;
import org.myProject.focus.flow.service.store.entities.enums.Priority;
import org.myProject.focus.flow.service.store.repositories.TaskRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskController {

    TaskRepository taskRepository;

    TaskDtoFactory taskDtoFactory;

    TaskStateHelper taskStateHelper;
    
    TaskHelper taskHelper;

    public static final String GET_TASK = "/api/tasks/{task_id}";
    public static final String GET_TASKS = "/api/task-state/{task_state_id}/tasks";
    public static final String CREATE_TASK = "/api/task-state/{task_state_id}/tasks";
    public static final String UPDATE_TASK = "/api/tasks/{task_id}";
    public static final String CHANGE_TASK_POSITION = "/api/tasks/{task_id}/position/change";
    public static final String DELETE_TASK = "/api/tasks/{task_id}";

    @GetMapping(GET_TASK)
    public TaskDto getTaskById(
            @PathVariable("task_id") Long taskId,
            @RequestParam("user_id") Long userId) {

        TaskEntity task = taskHelper.getTaskOrThrowException(taskId, userId);

        return taskDtoFactory.makeTaskDto(task);
    }

    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(
            @PathVariable("task_state_id") Long taskStateId,
            @RequestParam("user_id") Long userId) {

        TaskStateEntity task = taskStateHelper.getTaskStateOrThrowException(taskStateId, userId);

        return taskHelper
                .getSortedTasks(taskStateId)
                .stream()
                .map(taskDtoFactory::makeTaskDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK)
    public TaskDto createTask(
            @PathVariable("task_state_id") Long taskStateId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam LocalDateTime deadline,
            @RequestParam Category category,
            @RequestParam Priority priority,
            @RequestParam("user_id") Long userId){

        if(title.trim().isEmpty()){
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "title cannot be empty");
        }

        if (Objects.isNull(description)) description = "";

        TaskStateEntity taskState = taskStateHelper.getTaskStateOrThrowException(taskStateId, userId);

        Optional<TaskEntity> oldLowerPriorityTask = taskRepository
                .findTaskEntityByLowerPriorityTaskIsNullAndTaskStateId(taskStateId);

        TaskEntity task = taskRepository.saveAndFlush(
                TaskEntity
                        .builder()
                        .title(title)
                        .description(description)
                        .deadline(deadline)
                        .category(category)
                        .priority(priority)
                        .taskState(taskState)
                        .build()
        );

        oldLowerPriorityTask.ifPresent(anotherTask -> {

            task.setHigherPriorityTask(anotherTask);

            anotherTask.setLowerPriorityTask(task);

            taskRepository.saveAndFlush(anotherTask);
        });

        final TaskEntity savedTask = taskRepository.saveAndFlush(task);

        return taskDtoFactory.makeTaskDto(savedTask);
    }

    @PatchMapping(UPDATE_TASK)
    public TaskDto updateTask(
            @PathVariable("task_id") Long taskId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam LocalDateTime deadline,
            @RequestParam Category category,
            @RequestParam Priority priority,
            @RequestParam("user_id") Long userId){

        if(title.trim().isEmpty()){
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "title cannot be empty");
        }

        TaskEntity task = taskHelper.getTaskOrThrowException(taskId, userId);

        task.setTitle(title);
        task.setDescription(description);
        task.setDeadline(deadline);
        task.setCategory(category);
        task.setPriority(priority);

        taskRepository.saveAndFlush(task);

        return taskDtoFactory.makeTaskDto(task);
    }

    @PatchMapping(CHANGE_TASK_POSITION)
    public TaskDto changeTaskPosition(
            @PathVariable("task_id") Long taskId,
            @RequestParam("lower_task_id") Optional<Long> optionalLowerPriorityTaskId,
            @RequestParam("user_id") Long userId){

        TaskEntity selectedTask = taskHelper.getTaskOrThrowException(taskId, userId);

        if(taskHelper.isPositionChanged(selectedTask, optionalLowerPriorityTaskId)) {
            return taskDtoFactory.makeTaskDto(selectedTask);
        }

        Optional<TaskEntity> optionalNewLowerPriorityTask = taskHelper.
                getNewLowerPriorityTask(taskId, optionalLowerPriorityTaskId);

        Optional<TaskEntity> optionalNewHigherPriorityTask = taskHelper.
                getNewHigherPriorityTask(optionalNewLowerPriorityTask, selectedTask);

        taskHelper.replaceOldTasksPositions(selectedTask);

        selectedTask = taskHelper.updateTaskPosition(selectedTask, optionalNewLowerPriorityTask, optionalNewHigherPriorityTask);

        return taskDtoFactory.makeTaskDto(selectedTask);
    }

    @DeleteMapping(DELETE_TASK)
    public AckDto deleteTask(
            @PathVariable("task_id") Long taskId,
            @RequestParam("user_id") Long userId) {

        TaskEntity task = taskHelper.getTaskOrThrowException(taskId, userId);

        taskHelper.replaceOldTasksPositions(task);

        taskRepository.delete(task);

        return AckDto.builder().answer(true).build();
    }
}
