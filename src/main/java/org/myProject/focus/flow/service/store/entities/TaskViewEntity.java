package org.myProject.focus.flow.service.store.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.store.entities.enums.Layouts;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "task_view")
public class TaskViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id;

    @Column(nullable = false, unique = true)
    String name;

    Layouts typeOfLayout;

    Long ordinal;

    LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany
    @JoinColumn(name = "task_view_id", referencedColumnName = "id")
    List<TaskEntity> tasks;
}
