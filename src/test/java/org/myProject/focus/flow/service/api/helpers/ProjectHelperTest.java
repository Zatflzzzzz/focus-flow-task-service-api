package org.myProject.focus.flow.service.api.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.myProject.focus.flow.service.api.controllers.helpers.ProjectHelper;
import org.myProject.focus.flow.service.api.controllers.helpers.ValidateRequestsHelper;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.myProject.focus.flow.service.store.repositories.ProjectRepository;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectHelperTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ValidateRequestsHelper validateRequestsHelper;

    @InjectMocks
    private ProjectHelper projectHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetProjectOrThrowException_ProjectExists() {
        // Arrange
        Long projectId = 1L;
        Long userId = 2L;
        ProjectEntity projectEntity = new ProjectEntity();
        projectEntity.setUserId(userId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(projectEntity));

        ProjectEntity result = projectHelper.getProjectOrThrowException(projectId, userId);

        assertNotNull(result);
        assertEquals(projectEntity, result);
        verify(validateRequestsHelper).verifyingUserAccessToProject(userId, userId);
    }

    @Test
    void testGetProjectOrThrowException_ProjectNotFound() {

        Long projectId = 1L;
        Long userId = 2L;

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomAppException exception = assertThrows(CustomAppException.class, () ->
                projectHelper.getProjectOrThrowException(projectId, userId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("Project with id (1) doesn't exist"));
    }

    @Test
    void testGetProjectOrThrowException_InvalidUserAccess() {

        Long projectId = 1L;
        Long userId = 2L;
        Long projectOwnerId = 3L;
        ProjectEntity projectEntity = new ProjectEntity();
        projectEntity.setUserId(projectOwnerId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(projectEntity));
        doThrow(new CustomAppException(HttpStatus.FORBIDDEN, "Access denied"))
                .when(validateRequestsHelper)
                .verifyingUserAccessToProject(projectOwnerId, userId);

        // Act & Assert
        CustomAppException exception = assertThrows(CustomAppException.class, () ->
                projectHelper.getProjectOrThrowException(projectId, userId)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("Access denied"));
    }
}
