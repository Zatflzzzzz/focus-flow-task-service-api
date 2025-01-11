package org.myProject.focus.flow.service.store.repositories;

import org.myProject.focus.flow.service.store.entities.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
}
