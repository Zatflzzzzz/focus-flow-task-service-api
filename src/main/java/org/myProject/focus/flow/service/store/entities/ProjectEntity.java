package org.myProject.focus.flow.service.store.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "project")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id;

    @Column(nullable = false, unique = true)
    String name;

    LocalDateTime createdAt = LocalDateTime.now();
    
    Long userId;

    @OneToMany
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    List<TaskViewEntity> taskLayouts = new ArrayList<>();

}

