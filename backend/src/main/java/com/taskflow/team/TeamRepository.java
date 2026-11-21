package com.taskflow.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    @Query("select tm.team from TeamMember tm where tm.user.id = :userId")
    Page<Team> findAllByMemberUserId(@Param("userId") UUID userId, Pageable pageable);
}
