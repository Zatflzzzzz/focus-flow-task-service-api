package org.myProject.focus.flow.service.api.controllers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.controllers.helpers.ProjectHelper;
import org.myProject.focus.flow.service.api.controllers.helpers.TaskStateHelper;
import org.myProject.focus.flow.service.api.controllers.helpers.ValidateRequestsHelper;
import org.myProject.focus.flow.service.api.dto.AckDto;
import org.myProject.focus.flow.service.api.dto.TaskStateDto;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.api.factories.TaskStateDtoFactory;
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

    TaskStateDtoFactory taskStateDtoFactory;

    ValidateRequestsHelper validateRequestsService;

    ProjectHelper projectHelper;

    TaskStateHelper taskStateHelper;

    public static final String GET_TASK_STATES = "/api/projects/{project_id}/tasks-states";
    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/tasks-states";
    public static final String UPDATE_TASK_STATE = "/api/tasks-states/{task_state_id}";
    public static final String CHANGE_TASK_STATE_POSITION = "/api/tasks-states/{task_state_id}/position/change";
    public static final String DELETE_TASK_STATE = "/api/tasks-states/{task_state_id}";

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTasks(
            @PathVariable("project_id") Long projectId,
            @RequestParam("user_id") Long userId) {

        ProjectEntity project = projectHelper.getProjectOrThrowException(projectId);

        validateRequestsService.verifyingUserAccessToProject(project.getUserId(), userId);

        return taskStateHelper
                .getSortedTaskStates(projectId)
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
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

        ProjectEntity project = projectHelper.getProjectOrThrowException(projectId);

        validateRequestsService.verifyingUserAccessToProject(project.getUserId(), userId);

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

        optionalAnotherTaskState.ifPresent(anotherTaskState -> {

            taskStateEntity.setLeftTaskState(anotherTaskState);

            anotherTaskState.setRightTaskState(taskStateEntity);

            taskStateRepository.saveAndFlush(anotherTaskState);
        });

        final TaskStateEntity savedTaskStateEntity = taskStateRepository.saveAndFlush(taskStateEntity);

        return taskStateDtoFactory.makeTaskStateDto(savedTaskStateEntity);
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

        TaskStateEntity taskState = taskStateHelper.getTaskStateOrThrowException(taskStateId);

        validateRequestsService.verifyingUserAccessToProject(taskState.getProject().getUserId(), userId);

        taskStateRepository
                .findTaskStateEntityByProjectIdAndNameContaining(
                        taskState.getProject().getId(),
                        taskState.getName()
                )
                .filter(anotherTaskState -> !anotherTaskState.getName().equals(taskStateName))
                .ifPresent(_ -> {
                    throw new CustomAppException(HttpStatus.BAD_REQUEST,
                            String.format("Task state with name %s already exists", taskStateName));
                });

        taskState.setName(taskStateName);
        taskState.setTypeOfLayout(layout);

        taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(
                @PathVariable(name = "task_state_id") Long taskStateId,
                @RequestParam(name = "right_task_state_id", required = false) Optional<Long> optionalRightTaskStateId,
                @RequestParam(name = "user_id") Long userId){

        TaskStateEntity selectedTaskState = taskStateHelper.getTaskStateOrThrowException(taskStateId);

        ProjectEntity project = selectedTaskState.getProject();

        validateRequestsService.verifyingUserAccessToProject(project.getUserId(), userId);

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

    @DeleteMapping(DELETE_TASK_STATE)
    public AckDto deleteTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "user_id") Long userId){
            
        TaskStateEntity taskState = taskStateHelper.getTaskStateOrThrowException(taskStateId);

        validateRequestsService.verifyingUserAccessToProject(taskState.getProject().getUserId(), userId);

        taskStateHelper.replaceOldTaskStatesPosition(taskState);

        taskStateRepository.saveAndFlush(taskState);

        taskStateRepository.delete(taskState);

        return AckDto.builder().answer(true).build();
    }
}
