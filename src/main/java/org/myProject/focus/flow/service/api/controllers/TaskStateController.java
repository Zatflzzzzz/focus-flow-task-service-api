package org.myProject.focus.flow.service.api.controllers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.dto.ActDto;
import org.myProject.focus.flow.service.api.dto.TaskStateDto;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.api.factories.TaskDtoStateFactory;
import org.myProject.focus.flow.service.api.services.TaskStatePositionService;
import org.myProject.focus.flow.service.api.services.EntityFetchService;
import org.myProject.focus.flow.service.api.services.ValidateRequestsService;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.myProject.focus.flow.service.store.entities.TaskStateEntity;
import org.myProject.focus.flow.service.store.entities.enums.Layouts;
import org.myProject.focus.flow.service.store.repositories.TaskStateRepository;
import org.springframework.http.HttpStatus;
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

    TaskDtoStateFactory taskDtoStateFactory;

    EntityFetchService entityFetchService;

    ValidateRequestsService validateUserRequestsService;

    TaskStatePositionService taskStatePositionService;
    
    public static final String GET_TASK_STATES = "/api/projects/{project_id}/tasks-states";
    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/tasks-states";
    public static final String UPDATE_TASK_STATE = "/api/tasks-states/{task_state_id}";
    public static final String CHANGE_TASK_STATE_POSITION = "/api/tasks-states/{task_state_id}/position/change";
    public static final String DELETE_TASK_STATE = "/api/tasks-states/{task_state_id}";

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam(value = "user_id") Long userId) {

        ProjectEntity project = entityFetchService.getProjectOrThrowException(projectId);

        validateUserRequestsService.verifyingUserAccessToProject(project.getUserId(), userId);

        return entityFetchService
                .getSortedTaskStates(projectId)
                .stream()
                .map(taskDtoStateFactory::makeTaskStateDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam(name = "task_state_name") String taskStateName,
            @RequestParam(name = "type_of_layout") Layouts layout,
            @RequestParam(name = "user_id") Long userId) {

        if(taskStateName.trim().isEmpty()) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "Task state name cannot be empty");
        }

        ProjectEntity project = entityFetchService.getProjectOrThrowException(projectId);

        validateUserRequestsService.verifyingUserAccessToProject(project.getUserId(), userId);

        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();

        for(TaskStateEntity taskState : project.getTaskStates()) {

            if(taskState.getName().equals(taskStateName)) {
                throw new CustomAppException(HttpStatus.BAD_REQUEST,
                        String.format("Task state with name %s already exists", taskStateName));
            }

            if(!taskState.getRightTaskState().isPresent()){
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

        optionalAnotherTaskState
                .ifPresent(anotherTaskState -> {

                        taskStateEntity.setLeftTaskState(anotherTaskState);

                        anotherTaskState.setRightTaskState(taskStateEntity);

                        taskStateRepository.saveAndFlush(anotherTaskState);
                });

        final TaskStateEntity savedTaskStateEntity = taskStateRepository.saveAndFlush(taskStateEntity);

        return taskDtoStateFactory.makeTaskStateDto(savedTaskStateEntity);
    }

    @PatchMapping(UPDATE_TASK_STATE)
    public TaskStateDto updateTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName,
            @RequestParam(name = "type_of_layout") Layouts layout,
            @RequestParam(name = "user_id") Long userId){

        if(taskStateName.trim().isEmpty()) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "Task state name cannot be empty");
        }

        TaskStateEntity taskState = entityFetchService.getTaskStateOrThrowException(taskStateId);

        validateUserRequestsService.verifyingUserAccessToProject(taskState.getProject().getUserId(), userId);

        taskStateRepository
                .findTaskStateEntityByProjectIdAndNameContaining(
                        taskState.getProject().getUserId(),
                        taskState.getName()
                )
                .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId))
                .ifPresent(_ -> {
                    throw new CustomAppException(HttpStatus.BAD_REQUEST,
                            String.format("Task state with name %s already exists", taskStateName));
                });

        taskState.setName(taskStateName);
        taskState.setTypeOfLayout(layout);

        taskStateRepository.saveAndFlush(taskState);

        return taskDtoStateFactory.makeTaskStateDto(taskState);
    }

    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(
                @PathVariable(name = "task_state_id") Long taskStateId,
                @RequestParam(name = "left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId,
                @RequestParam(name = "user_id") Long userId
            ) {

        TaskStateEntity selectedTaskState = entityFetchService.getTaskStateOrThrowException(taskStateId);

        ProjectEntity project = selectedTaskState.getProject();

        validateUserRequestsService.verifyingUserAccessToProject(project.getUserId(), userId);

        if (taskStatePositionService.isPositionUnchanged(selectedTaskState, optionalLeftTaskStateId)) {
            return taskDtoStateFactory.makeTaskStateDto(selectedTaskState);
        }

        Optional<TaskStateEntity> optionalNewLeftTaskState = taskStatePositionService
                .resolveNewLeftTaskState(optionalLeftTaskStateId, taskStateId, project);

        Optional<TaskStateEntity> optionalNewRightTaskState = taskStatePositionService
                .resolveNewRightTaskState(optionalNewLeftTaskState, project);

        taskStatePositionService.replaceOldTaskStatesPosition(selectedTaskState);

        taskStatePositionService.updateTaskStatePosition(selectedTaskState, optionalNewLeftTaskState, optionalNewRightTaskState);

        return taskDtoStateFactory.makeTaskStateDto(selectedTaskState);
    }

    @DeleteMapping(DELETE_TASK_STATE)
    public ActDto deleteTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "user_id") Long userId){
            
        TaskStateEntity taskState = entityFetchService.getTaskStateOrThrowException(taskStateId);
    
        validateUserRequestsService.verifyingUserAccessToProject(taskState.getProject().getUserId(), userId);

        taskStatePositionService.replaceOldTaskStatesPosition(taskState);

        taskState.setLeftTaskState(null);
        taskState.setRightTaskState(null);

        taskStateRepository.saveAndFlush(taskState);

        taskStateRepository.deleteById(taskStateId);

        return ActDto.builder().answer(true).build();
    }
}
