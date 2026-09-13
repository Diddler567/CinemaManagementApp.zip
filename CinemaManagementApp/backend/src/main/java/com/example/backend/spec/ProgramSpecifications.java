package com.example.backend.spec;

import com.example.backend.entities.Program;
import com.example.backend.entities.ProgramRole;
import com.example.backend.entities.Screening;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.time.LocalDate;

public class ProgramSpecifications {

    private static boolean hasText(String s) {
        return s != null && !s.trim().isBlank();
    }

    private static String likePattern(String s) {
        return "%" + s.trim().toLowerCase() + "%";
    }

    public static Specification<Program> visibleToUser(Long userId) {
        return (root, query, cb) -> {
            

            Predicate announced = cb.equal(root.get("state"), com.example.backend.entities.ProgramState.ANNOUNCED);
            Predicate submission = cb.equal(root.get("state"), com.example.backend.entities.ProgramState.SUBMISSION);

            Predicate creatorIsUser = cb.equal(root.get("creator").get("userID"), userId);

            //PROGRAMROLE EXISTS (PROGRAMMER/STAFF)
            Subquery<Long> roleExists = query.subquery(Long.class);
            Root<ProgramRole> pr = roleExists.from(ProgramRole.class);
            roleExists.select(cb.literal(1L));
            roleExists.where(
                cb.equal(pr.get("program").get("programID"), root.get("programID")),
                cb.equal(pr.get("user").get("userID"), userId)
            );

            //SUBMITTER EXISTS: EXISTS SCREENING WHERE SCREENING.PROGRAM = PROGRAM AND SCREENING.SUBMITTER.USERID = USERID
            Subquery<Long> submitterExists = query.subquery(Long.class);
            Root<Screening> s = submitterExists.from(Screening.class);
            submitterExists.select(cb.literal(1L));
            submitterExists.where(
                cb.equal(s.get("program").get("programID"), root.get("programID")),
                cb.equal(s.get("submitter").get("userID"), userId)
            );

            return cb.or(
                announced,
                submission,
                creatorIsUser,
                cb.exists(roleExists),
                cb.exists(submitterExists)
            );
        };
    }


    public static Specification<Program> stateEquals(com.example.backend.entities.ProgramState state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    public static Specification<Program> startDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from);
    }

    public static Specification<Program> endDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endDate"), from);
    }

    public static Specification<Program> startDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), to);
    }

    public static Specification<Program> endDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), to);
    }

    public static Specification<Program> nameContains(String q) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), likePattern(q));
    }

    public static Specification<Program> descriptionContains(String q) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("description")), likePattern(q));
    }

    // EXISTS SCREENING WHERE FILMTITLE LIKE
    public static Specification<Program> existsScreeningWithFilmTitle(String filmTitle) {
        return (root, query, cb) -> {
            Subquery<Long> sq = query.subquery(Long.class);
            Root<Screening> s = sq.from(Screening.class);
            sq.select(cb.literal(1L));
            sq.where(
                    cb.equal(s.get("program").get("programID"), root.get("programID")),
                    cb.like(cb.lower(s.get("filmTitle")), likePattern(filmTitle))
            );
            return cb.exists(sq);
        };
    }

    //EXISTS SCREENING WHERE AUDITORIUMNAME LIKE
    public static Specification<Program> existsScreeningWithAuditorium(String auditorium) {
        return (root, query, cb) -> {
            Subquery<Long> sq = query.subquery(Long.class);
            Root<Screening> s = sq.from(Screening.class);
            sq.select(cb.literal(1L));
            sq.where(
                    cb.equal(s.get("program").get("programID"), root.get("programID")),
                    cb.like(cb.lower(s.get("auditoriumName")), likePattern(auditorium))
            );
            return cb.exists(sq);
        };
    }

    
    public static Specification<Program> freeTextAny(String q) {
        return (root, query, cb) -> {
            String pat = likePattern(q);

            Predicate pName = cb.like(cb.lower(root.get("name")), pat);
            Predicate pDesc = cb.like(cb.lower(root.get("description")), pat);

            //EXISTS SCREENING WHERE FILMTITLE OR AUDITORIUM MATCHES
            Subquery<Long> sq = query.subquery(Long.class);
            Root<Screening> s = sq.from(Screening.class);
            sq.select(cb.literal(1L));
            sq.where(
                    cb.equal(s.get("program").get("programID"), root.get("programID")),
                    cb.or(
                            cb.like(cb.lower(s.get("filmTitle")), pat),
                            cb.like(cb.lower(s.get("auditoriumName")), pat)
                    )
            );

            return cb.or(pName, pDesc, cb.exists(sq));
        };
    }

    public static boolean hasTextPublic(String s) {
        return hasText(s);
    }
}

