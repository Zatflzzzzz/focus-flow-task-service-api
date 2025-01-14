package org.myProject.focus.flow.service.store.repositories;

import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.stream.Stream;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    Optional<ProjectEntity> findByName(String name);

    Stream<ProjectEntity> streamAllBy();

    Stream<ProjectEntity> streamAllByUserId(Long userId);

    Stream<ProjectEntity> streamAllByNameContainingIgnoreCaseAndUserId(String name, Long userId);
}
