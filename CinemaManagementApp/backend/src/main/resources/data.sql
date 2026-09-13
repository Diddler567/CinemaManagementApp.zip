
-- BCRYPT HASHES:
-- ADMIN/ADMIN123
-- USER1/PASSWORD1!
-- USER2/PASSWORD1!

INSERT INTO users (user_id, username, fullname, password_hash, permanent_role, is_active, failed_auth_count, failed_password_change_count)
VALUES
  (1, 'admin', 'System Administrator', '$2b$10$5f8Jf7HLsI51KqhRsQTzWOdPY.4nuvwRIHTPzImWoPoA2uSuOLyBK', 'ADMIN', TRUE, 0, 0),
  (2, 'user1', 'Demo User One',         '$2b$10$LxL2/LdueI/SqSq0/v5sg.9uw9hjEaqqLM9/zSCMpzKT1XBGMzeB2', 'USER',  TRUE, 0, 0),
  (3, 'user2', 'Demo User Two',         '$2b$10$LxL2/LdueI/SqSq0/v5sg.9uw9hjEaqqLM9/zSCMpzKT1XBGMzeB2', 'USER',  TRUE, 0, 0);

INSERT INTO programs (program_id, name, description, start_date, end_date, creation_date, state, creator_user_id)
VALUES
  (10, 'Demo Program A', 'Demo program in ANNOUNCED state', DATE '2026-01-10', DATE '2026-01-12', CURRENT_DATE, 'ANNOUNCED', 2),
  (11, 'Demo Program B', 'Demo program still in SUBMISSION', DATE '2026-02-01', DATE '2026-02-03', CURRENT_DATE, 'SUBMISSION', 2);

INSERT INTO program_roles (program_id, user_id, role, assigned_at)
VALUES
  (10, 2, 'PROGRAMMER', CURRENT_TIMESTAMP),
  (10, 3, 'STAFF',      CURRENT_TIMESTAMP);

INSERT INTO screenings (
    screening_id, program_id, submitter_user_id, handler_staff_user_id,
    state, creation_date, final_submission_at,
    approval_notes, rejection_reason,
    film_title, film_cast, film_genre, film_duration,
    start_time, end_time, auditorium_name
)
VALUES
  (
    100, 10, 2, 3,
    'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    'Approved in scheduling (demo)', NULL,
    'Demo Film', 'Actor A, Actor B', 'Drama', 90,
    TIMESTAMP '2026-01-10 18:00:00', TIMESTAMP '2026-01-10 19:30:00', 'Auditorium 1'
  );

INSERT INTO screening_reviews (review_id, screening_id, staff_user_id, score, comments, review_at)
VALUES
  (1000, 100, 3, 8, 'Solid demo review.', CURRENT_TIMESTAMP);


-- FIX IDENTITY COUNTERS AFTER MANUAL IDS (H2)
ALTER TABLE users ALTER COLUMN user_id RESTART WITH 4;
ALTER TABLE programs ALTER COLUMN program_id RESTART WITH 12;
ALTER TABLE screenings ALTER COLUMN screening_id RESTART WITH 101;
ALTER TABLE screening_reviews ALTER COLUMN review_id RESTART WITH 1001;

