package com.project.job_finder_ai.entity;

import com.project.job_finder_ai.enums.RunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fetch_run")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FetchRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    private Instant startedAt;
    private Instant finishedAt;
    @Enumerated(value = EnumType.STRING)
    private RunStatus runStatus;
    private int jobsFound;
    private int newJobs;

    @Column(length = 1000)
    private String errorMessage;

}
