package org.myProject.focus.flow.service.api.controllers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.dto.ActDto;
import org.myProject.focus.flow.service.api.dto.ProjectDto;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.myProject.focus.flow.service.api.factories.ProjectDtoFactory;
import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.myProject.focus.flow.service.store.repositories.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectController {

    ProjectRepository projectRepository;

    ProjectDtoFactory projectDtoFactory;

    public static final String FETCH_PROJECT = "/api/projects";
    public static final String CREATE_OR_UPDATE_PROJECT = "/api/projects";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";

    @GetMapping(FETCH_PROJECT)
    public List<ProjectDto> fetchProject(
            @RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName,
            @RequestParam(value = "user_id") Long userId) {

        optionalPrefixName = optionalPrefixName.filter(prefixName -> !prefixName.trim().isEmpty());

        Stream<ProjectEntity> projectStream = optionalPrefixName
                .map(prefixName -> projectRepository.streamAllByNameContainingIgnoreCaseAndUserId(prefixName, userId))
                .orElseGet(() -> projectRepository.streamAllByUserId(userId));

        return projectStream
                .map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }

    @PutMapping(CREATE_OR_UPDATE_PROJECT)
    public ProjectDto createOrUpdateProject(
            @RequestParam(value = "project_id", required = false) Optional<Long> optionalProjectId,
            @RequestParam(value = "project_name", required = false) Optional<String> optionalProjectName,
            @RequestParam(value = "user_id") Long userId) {

        optionalProjectName = optionalProjectName.filter(projectName -> !projectName.trim().isEmpty());

        boolean isCreate = !optionalProjectId.isPresent();

        if(isCreate && !optionalProjectName.isPresent()) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "Project name cannot be empty");
        }

        final ProjectEntity project = optionalProjectId
                .map(this::getProjectOrThrowException)
                .orElseGet(() -> ProjectEntity.builder().userId(userId).build());

        optionalProjectName
                .ifPresent(projectName -> {

                    validateUserAccess(project, userId);

                    projectRepository
                            .findByName(projectName)
                            .filter(anotherProject -> !Objects.equals(anotherProject.getId(), project.getId()))
                            .ifPresent(anotherProject -> {
                                throw new CustomAppException(HttpStatus.BAD_REQUEST,
                                        String.format("Project %s already exists", projectName));
                            });

                    project.setName(projectName);
                });

        final ProjectEntity savedProject = projectRepository.saveAndFlush(project);
        System.out.println(project);
        return projectDtoFactory.makeProjectDto(savedProject);
    }

    @DeleteMapping(DELETE_PROJECT)
    public ActDto deleteProject(
            @PathVariable("project_id") Long projectId,
            @RequestParam(value = "user_id") Long userId) {

        ProjectEntity project = getProjectOrThrowException(projectId);

        validateUserAccess(project, userId);

        projectRepository.deleteById(projectId);

        return ActDto.makeDefault(true);
    }

    private ProjectEntity getProjectOrThrowException(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new CustomAppException(
                                HttpStatus.NOT_FOUND,
                                String.format("Project with %s doesn't exist", projectId)
                        ));
    }

    private void validateUserAccess(ProjectEntity project, Long userId) {
        if (!Objects.equals(project.getUserId(), userId)) {
            throw new CustomAppException(HttpStatus.FORBIDDEN,
                    String.format("You does not have access to project with name %s", project.getName()));
        }
    }

}
