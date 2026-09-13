package com.example.backend.repository;

import com.example.backend.entities.Program;
import com.example.backend.entities.ProgramState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProgramRepository extends JpaRepository<Program, Long>, JpaSpecificationExecutor<Program> {

    Optional<Program> findByName(String name);

    boolean existsByName(String name);

    //NEEDED BY PUBLICPROGRAMCONTROLLER (PUBLIC LIST/SEARCH)
    List<Program> findByStateOrderByStartDateAscNameAsc(ProgramState state);
}