package com.example.backend.repository;

import com.example.backend.entities.ProgramRole;
import com.example.backend.entities.ProgramRoleId;
import com.example.backend.entities.ProgramRoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgramRoleRepository extends JpaRepository<ProgramRole, ProgramRoleId> {

    boolean existsByProgram_ProgramIDAndUser_UserID(Long programID, Long userID);

    boolean existsByProgram_ProgramIDAndUser_UserIDAndRole(Long programID, Long userID, ProgramRoleType role);

    Optional<ProgramRole> findByProgram_ProgramIDAndUser_UserID(Long programID, Long userID);

    List<ProgramRole> findByProgram_ProgramID(Long programID);

    List<ProgramRole> findByProgram_ProgramIDAndRole(Long programID, ProgramRoleType role);

    void deleteByProgram_ProgramIDAndRole(Long programID, ProgramRoleType role);
}
