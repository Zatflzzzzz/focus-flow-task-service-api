package org.myProject.focus.flow.service.api.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.myProject.focus.flow.service.api.controllers.TaskController;
import org.myProject.focus.flow.service.api.controllers.helpers.TaskHelper;
import org.myProject.focus.flow.service.api.controllers.helpers.TaskStateHelper;
import org.myProject.focus.flow.service.api.dto.AckDto;
import org.myProject.focus.flow.service.api.dto.TaskDto;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.api.factories.TaskDtoFactory;
import org.myProject.focus.flow.service.store.entities.TaskEntity;
import org.myProject.focus.flow.service.store.entities.TaskStateEntity;
import org.myProject.focus.flow.service.store.entities.enums.Category;
import org.myProject.focus.flow.service.store.entities.enums.Priority;
import org.myProject.focus.flow.service.store.repositories.TaskRepository;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskHelperTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskDtoFactory taskDtoFactory;

    @Mock
    private TaskStateHelper taskStateHelper;

    @Mock
    private TaskHelper taskHelper;

    @InjectMocks
    private TaskController taskController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetTaskById_Success() {

        Long taskId = 1L;
        Long userId = 2L;
        TaskEntity taskEntity = new TaskEntity();
        TaskDto taskDto = new TaskDto();

        when(taskHelper.getTaskOrThrowException(taskId, userId)).thenReturn(taskEntity);
        when(taskDtoFactory.makeTaskDto(taskEntity)).thenReturn(taskDto);

        TaskDto result = taskController.getTaskById(taskId, userId);

        assertNotNull(result);
        assertEquals(taskDto, result);
        verify(taskHelper).getTaskOrThrowException(taskId, userId);
        verify(taskDtoFactory).makeTaskDto(taskEntity);
    }

    @Test
    void testGetTaskById_NotFound() {

        Long taskId = 1L;
        Long userId = 2L;

        when(taskHelper.getTaskOrThrowException(taskId, userId))
                .thenThrow(new CustomAppException(HttpStatus.NOT_FOUND, "Task not found"));

        CustomAppException exception = assertThrows(CustomAppException.class, () ->
                taskController.getTaskById(taskId, userId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("Task not found"));
    }

    @Test
    void testGetTasks_Success() {

        Long taskStateId = 1L;
        Long userId = 2L;
        TaskStateEntity taskState = new TaskStateEntity();
        TaskEntity taskEntity = new TaskEntity();
        TaskDto taskDto = new TaskDto();

        when(taskStateHelper.getTaskStateOrThrowException(taskStateId, userId)).thenReturn(taskState);
        when(taskHelper.getSortedTasks(taskStateId)).thenReturn(List.of(taskEntity));
        when(taskDtoFactory.makeTaskDto(taskEntity)).thenReturn(taskDto);

        List<TaskDto> result = taskController.getTasks(taskStateId, userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskDto, result.get(0));
        verify(taskStateHelper).getTaskStateOrThrowException(taskStateId, userId);
        verify(taskHelper).getSortedTasks(taskStateId);
    }

    @Test
    void testCreateTask_Success() {

        Long taskStateId = 1L;
        Long userId = 2L;
        String title = "Test Task";
        String description = "Description";
        LocalDateTime deadline = LocalDateTime.now();
        Category category = Category.WORK;
        Priority priority = Priority.HIGH;

        TaskStateEntity taskState = new TaskStateEntity();

        TaskEntity taskEntity = new TaskEntity();

        TaskDto taskDto = new TaskDto();

        when(taskStateHelper.getTaskStateOrThrowException(taskStateId, userId)).thenReturn(taskState);
        when(taskRepository.saveAndFlush(any(TaskEntity.class))).thenReturn(taskEntity);
        when(taskDtoFactory.makeTaskDto(taskEntity)).thenReturn(taskDto);

        TaskDto result = taskController.createTask(taskStateId, title, description, deadline, category, priority, userId);

        assertNotNull(result);
        assertEquals(taskDto, result);
        verify(taskStateHelper).getTaskStateOrThrowException(taskStateId, userId);
        verify(taskRepository, atLeastOnce()).saveAndFlush(any(TaskEntity.class));
        verify(taskDtoFactory).makeTaskDto(taskEntity);
    }

    @Test
    void testCreateTask_EmptyTitle() {

        Long taskStateId = 1L;
        Long userId = 2L;

        CustomAppException exception = assertThrows(CustomAppException.class, () ->
                taskController.createTask(taskStateId, " ", "Description", LocalDateTime.now(), Category.WORK, Priority.HIGH, userId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("title cannot be empty"));
    }

    @Test
    void testDeleteTask_Success() {

        Long taskId = 1L;
        Long userId = 2L;
        TaskEntity taskEntity = new TaskEntity();
        AckDto ackDto = AckDto.builder().answer(true).build();

        when(taskHelper.getTaskOrThrowException(taskId, userId)).thenReturn(taskEntity);

        AckDto result = taskController.deleteTask(taskId, userId);

        assertNotNull(result);
        assertTrue(result.getAnswer());
        verify(taskHelper).getTaskOrThrowException(taskId, userId);
        verify(taskHelper).replaceOldTasksPositions(taskEntity);
        verify(taskRepository).delete(taskEntity);
    }
}
