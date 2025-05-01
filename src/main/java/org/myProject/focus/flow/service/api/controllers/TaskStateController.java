package org.myProject.focus.flow.service.api.controllers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.controllers.helpers.ProjectHelper;
import org.myProject.focus.flow.service.api.controllers.helpers.TaskStateHelper;
import org.myProject.focus.flow.service.api.dto.AckDto;
import org.myProject.focus.flow.service.api.dto.TaskStateDto;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.api.factories.TaskStateDtoFactory;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.myProject.focus.flow.service.store.entities.TaskStateEntity;
import org.myProject.focus.flow.service.store.entities.enums.Layouts;
import org.myProject.focus.flow.service.store.repositories.TaskStateRepository;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskStateController {

    TaskStateRepository taskStateRepository;

    TaskStateDtoFactory taskStateDtoFactory;

    ProjectHelper projectHelper;

    TaskStateHelper taskStateHelper;

    public static final String GET_TASK_STATE = "/api/tasks-states/{task_state_id}";
    public static final String GET_TASK_STATES = "/api/projects/{project_id}/tasks-states";
    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/tasks-states";
    public static final String UPDATE_TASK_STATE = "/api/tasks-states/{task_state_id}";
    public static final String CHANGE_TASK_STATE_POSITION = "/api/tasks-states/{task_state_id}/position/change";
    public static final String DELETE_TASK_STATE = "/api/tasks-states/{task_state_id}";

    @Operation(summary = "Get TaskState by ID", description = "Returns information about a TaskState by its ID.")
    @GetMapping(GET_TASK_STATE)
    public TaskStateDto getTaskStateById(
            @PathVariable("task_state_id") Long taskStateId,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());
        
        TaskStateEntity taskState = taskStateHelper.getTaskStateOrThrowException(taskStateId, userId);

        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    @Operation(summary = "Get all TaskStates in a project", description = "Returns a list of all TaskStates for a specified project.")
    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTasks(
            @PathVariable("project_id") Long projectId,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());
        
        projectHelper.getProjectOrThrowException(projectId, userId);

        return taskStateHelper
                .getSortedTaskStates(projectId)
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Create TaskState", description = "Creates a new TaskState within the specified project.")
    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam(name = "task_state_name") String taskStateName,
            @RequestParam(name = "type_of_layout") Layouts layout,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());
        
        if(taskStateName.trim().isEmpty()) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "Task state name cannot be empty");
        }

        ProjectEntity project = projectHelper.getProjectOrThrowException(projectId, userId);

        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();

        for(TaskStateEntity taskState : project.getTaskStates()) {

            if(taskState.getName().equals(taskStateName)) {
                throw new CustomAppException(HttpStatus.BAD_REQUEST,
                        String.format("Task state with name %s already exists", taskStateName));
            }

            if(taskState.getRightTaskState().isEmpty()){
                optionalAnotherTaskState = Optional.of(taskState);
                break;
            }
        }

        TaskStateEntity taskStateEntity = taskStateRepository.saveAndFlush(
                TaskStateEntity
                        .builder()
                        .name(taskStateName)
                        .typeOfLayout(layout)
                        .project(project)
                        .build()
        );

        optionalAnotherTaskState.ifPresent(anotherTaskState -> {

            taskStateEntity.setLeftTaskState(anotherTaskState);

            anotherTaskState.setRightTaskState(taskStateEntity);

            taskStateRepository.saveAndFlush(anotherTaskState);
        });

        final TaskStateEntity savedTaskStateEntity = taskStateRepository.saveAndFlush(taskStateEntity);

        return taskStateDtoFactory.makeTaskStateDto(savedTaskStateEntity);
    }

    @Operation(summary = "Update TaskState", description = "Updates the name and layout type of a TaskState by its ID.")
    @PatchMapping(UPDATE_TASK_STATE)
    public TaskStateDto updateTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName,
            @RequestParam(name = "type_of_layout") Layouts layout,
            @AuthenticationPrincipal Jwt jwt){

        Long userId = Long.parseLong(jwt.getSubject());

        if(taskStateName.trim().isEmpty()) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "Task state name cannot be empty");
        }

        TaskStateEntity taskState = taskStateHelper.getTaskStateOrThrowException(taskStateId, userId);

        taskStateRepository
                .findTaskStateEntityByProjectIdAndNameContaining(
                        taskState.getProject().getId(),
                        taskState.getName()
                )
                .filter(anotherTaskState -> !anotherTaskState.getName().equals(taskStateName))
                .ifPresent(it -> {
                    throw new CustomAppException(HttpStatus.BAD_REQUEST,
                            String.format("Task state with name %s already exists", taskStateName));
                });

        taskState.setName(taskStateName);
        taskState.setTypeOfLayout(layout);

        taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    @Operation(summary = "Change TaskState position", description = "Changes the position of a TaskState within the list.")
    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(
                @PathVariable(name = "task_state_id") Long taskStateId,
                @RequestParam(name = "right_task_state_id", required = false) Optional<Long> optionalRightTaskStateId,
                @AuthenticationPrincipal Jwt jwt){

        Long userId = Long.parseLong(jwt.getSubject());

        TaskStateEntity selectedTaskState = taskStateHelper.getTaskStateOrThrowException(taskStateId, userId);

        ProjectEntity project = selectedTaskState.getProject();

        if (taskStateHelper.isPositionUnchanged(selectedTaskState, optionalRightTaskStateId)) {
            return taskStateDtoFactory.makeTaskStateDto(selectedTaskState);
        }

        Optional<TaskStateEntity> optionalNewRightTaskState = taskStateHelper
                .resolveNewRightTaskState(optionalRightTaskStateId, taskStateId, project);

        Optional<TaskStateEntity> optionalNewLeftTaskState = taskStateHelper
                .resolveNewLeftTaskState(optionalNewRightTaskState, project);

        taskStateHelper.replaceOldTaskStatesPosition(selectedTaskState);

        selectedTaskState = taskStateHelper.updateTaskStatePosition(selectedTaskState, optionalNewLeftTaskState, optionalNewRightTaskState);

        return taskStateDtoFactory.makeTaskStateDto(selectedTaskState);
    }

    @Operation(summary = "Delete TaskState", description = "Deletes a TaskState by its ID.")
    @DeleteMapping(DELETE_TASK_STATE)
    public AckDto deleteTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @AuthenticationPrincipal Jwt jwt){

        Long userId = Long.parseLong(jwt.getSubject());

        TaskStateEntity taskState = taskStateHelper.getTaskStateOrThrowException(taskStateId, userId);

        taskStateHelper.replaceOldTaskStatesPosition(taskState);

        taskStateRepository.saveAndFlush(taskState);

        taskStateRepository.delete(taskState);

        return AckDto.builder().answer(true).build();
    }
}
