package org.myProject.focus.flow.service.store.repositories;

import org.myProject.focus.flow.service.store.entities.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.config.Task;

import java.util.List;
import java.util.Optional;


public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    Optional<TaskEntity> findTaskEntityByHigherPriorityTaskIsNullAndTaskStateId(Long taskState_id);

    Optional<TaskEntity> findTaskEntityByLowerPriorityTaskIsNullAndTaskStateId(Long taskState_id);

    List<TaskEntity> findAllByTaskStateId(Long taskState_id);
}
