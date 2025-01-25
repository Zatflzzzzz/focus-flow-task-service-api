package org.myProject.focus.flow.service.api.helpers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.myProject.focus.flow.service.api.controllers.helpers.TaskStateHelper;
import org.myProject.focus.flow.service.api.controllers.helpers.ValidateRequestsHelper;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.myProject.focus.flow.service.store.entities.TaskStateEntity;
import org.myProject.focus.flow.service.store.repositories.TaskStateRepository;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskStateHelperTest {

    @Mock
    private TaskStateRepository taskStateRepository;

    @Mock
    private ValidateRequestsHelper validateRequestsHelper;

    @InjectMocks
    private TaskStateHelper taskStateHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetTaskStateOrThrowException_TaskStateExists() {

        Long taskStateId = 1L;
        Long userId = 2L;

        TaskStateEntity taskStateEntity = new TaskStateEntity();

        ProjectEntity projectEntity = new ProjectEntity();

        projectEntity.setUserId(userId);
        taskStateEntity.setProject(projectEntity);

        when(taskStateRepository.findById(taskStateId)).thenReturn(Optional.of(taskStateEntity));

        TaskStateEntity result = taskStateHelper.getTaskStateOrThrowException(taskStateId, userId);

        assertNotNull(result);
        assertEquals(taskStateEntity, result);
        verify(validateRequestsHelper).verifyingUserAccessToProject(userId, userId);
    }

    @Test
    void testGetTaskStateOrThrowException_TaskStateNotFound() {

        Long taskStateId = 1L;
        Long userId = 2L;

        when(taskStateRepository.findById(taskStateId)).thenReturn(Optional.empty());

        CustomAppException exception = assertThrows(CustomAppException.class, () ->
                taskStateHelper.getTaskStateOrThrowException(taskStateId, userId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("Task state with id \"1\" not found"));
    }

    @Test
    void testGetSortedTaskStates() {
        Long projectId = 1L;

        TaskStateEntity firstTaskState = new TaskStateEntity();
        TaskStateEntity secondTaskState = new TaskStateEntity();

        firstTaskState.setRightTaskState(secondTaskState);
        secondTaskState.setLeftTaskState(firstTaskState);

        when(taskStateRepository.findTaskStateEntityByLeftTaskStateIsNullAndProjectId(projectId))
                .thenReturn(Optional.of(firstTaskState));

        List<TaskStateEntity> result = taskStateHelper.getSortedTaskStates(projectId);

        assertEquals(2, result.size());
        assertEquals(firstTaskState, result.get(0));
        assertEquals(secondTaskState, result.get(1));
    }

    @Test
    void testIsPositionUnchanged_True() {

        TaskStateEntity taskStateEntity = new TaskStateEntity();
        TaskStateEntity rightTaskState = new TaskStateEntity();

        rightTaskState.setId(2L);

        taskStateEntity.setRightTaskState(rightTaskState);

        Optional<Long> optionalRightTaskStateId = Optional.of(2L);

        boolean result = taskStateHelper.isPositionUnchanged(taskStateEntity, optionalRightTaskStateId);

        assertTrue(result);
    }

    @Test
    void testIsPositionUnchanged_False() {

        TaskStateEntity taskStateEntity = new TaskStateEntity();
        TaskStateEntity rightTaskState = new TaskStateEntity();

        rightTaskState.setId(2L);
        taskStateEntity.setRightTaskState(rightTaskState);

        Optional<Long> optionalRightTaskStateId = Optional.of(3L);

        boolean result = taskStateHelper.isPositionUnchanged(taskStateEntity, optionalRightTaskStateId);

        assertFalse(result);
    }

    @Test
    void testResolveNewRightTaskState_ValidCase() {

        Long taskStateId = 1L;
        Long rightTaskStateId = 2L;

        ProjectEntity projectEntity = new ProjectEntity();

        projectEntity.setId(1L);

        TaskStateEntity rightTaskStateEntity = new TaskStateEntity();

        rightTaskStateEntity.setId(rightTaskStateId);
        rightTaskStateEntity.setProject(projectEntity);

        when(taskStateRepository.findById(rightTaskStateId)).thenReturn(Optional.of(rightTaskStateEntity));

        Optional<TaskStateEntity> result = taskStateHelper.resolveNewRightTaskState(Optional.of(rightTaskStateId), taskStateId, projectEntity);

        assertTrue(result.isPresent());
        assertEquals(rightTaskStateEntity, result.get());
    }

    @Test
    void testResolveNewRightTaskState_ThrowsException_SameTaskId() {

        Long taskStateId = 1L;
        Long rightTaskStateId = 1L;
        ProjectEntity projectEntity = new ProjectEntity();
        Optional<Long> optionalRightTaskStateId = Optional.of(rightTaskStateId);

        CustomAppException exception = assertThrows(CustomAppException.class, () ->
                taskStateHelper.resolveNewRightTaskState(optionalRightTaskStateId, taskStateId, projectEntity)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("Right task state id equals changed task state."));
    }

    @Test
    void testReplaceOldTaskStatesPosition() {

        TaskStateEntity taskState = new TaskStateEntity();
        TaskStateEntity leftTaskState = new TaskStateEntity();
        TaskStateEntity rightTaskState = new TaskStateEntity();

        taskState.setLeftTaskState(rightTaskState);
        taskState.setRightTaskState(leftTaskState);

        leftTaskState.setRightTaskState(taskState);
        rightTaskState.setLeftTaskState(taskState);

        taskStateHelper.replaceOldTaskStatesPosition(taskState);

        verify(taskStateRepository).saveAndFlush(taskState);
        verify(taskStateRepository).saveAndFlush(leftTaskState);
        verify(taskStateRepository).saveAndFlush(rightTaskState);

        assertFalse(taskState.getLeftTaskState().isPresent(), "taskState's leftTaskState should be empty");
        assertFalse(taskState.getRightTaskState().isPresent(), "taskState's rightTaskState should be empty");
        assertTrue(leftTaskState.getRightTaskState().isPresent(), "leftTaskState's rightTaskState should not be empty");
        assertTrue(rightTaskState.getLeftTaskState().isPresent(), "rightTaskState's leftTaskState should not be empty");
    }

    @Test
    void testUpdateTaskStatePosition() {
        TaskStateEntity selectedTaskState = new TaskStateEntity();
        TaskStateEntity newLeftTaskState = new TaskStateEntity();
        TaskStateEntity newRightTaskState = new TaskStateEntity();

        Optional<TaskStateEntity> optionalNewLeftTaskState = Optional.of(newLeftTaskState);
        Optional<TaskStateEntity> optionalNewRightTaskState = Optional.of(newRightTaskState);

        TaskStateEntity result = taskStateHelper.updateTaskStatePosition(
                selectedTaskState, optionalNewLeftTaskState, optionalNewRightTaskState);

        verify(taskStateRepository).saveAndFlush(newLeftTaskState);
        verify(taskStateRepository).saveAndFlush(newRightTaskState);
        verify(taskStateRepository).saveAndFlush(selectedTaskState);

        assertEquals(newLeftTaskState, selectedTaskState.getLeftTaskState().orElse(null));
        assertEquals(newRightTaskState, selectedTaskState.getRightTaskState().orElse(null));
        assertEquals(selectedTaskState, newLeftTaskState.getRightTaskState().orElse(null));
        assertEquals(selectedTaskState, newRightTaskState.getLeftTaskState().orElse(null));
    }
}
