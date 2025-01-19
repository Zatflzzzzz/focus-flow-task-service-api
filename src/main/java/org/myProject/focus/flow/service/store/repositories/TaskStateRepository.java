package org.myProject.focus.flow.service.store.repositories;

import org.myProject.focus.flow.service.store.entities.TaskStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.stream.Stream;

public interface TaskStateRepository extends JpaRepository<TaskStateEntity, Long> {

    Optional<TaskStateEntity> findTaskStateEntityByLeftTaskStateIsNullAndProjectId(Long projectId);

    Optional<TaskStateEntity> findTaskStateEntityByProjectIdAndNameContaining(Long projectId, String taskStateName);
}
