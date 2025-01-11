package org.myProject.focus.flow.service.store.repositories;

import org.myProject.focus.flow.service.store.entities.TaskViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskViewRepository extends JpaRepository<TaskViewEntity, Long> {
}
