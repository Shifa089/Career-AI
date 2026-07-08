-- A session must never hold two questions with the same number. This guards the concurrent
-- prefetch path (next question is pre-generated on a background thread while the candidate answers
-- the current one): if two writers race to create question N, the unique constraint rejects the
-- loser instead of producing a duplicate.
ALTER TABLE interview_questions
    ADD CONSTRAINT uq_interview_questions_session_number UNIQUE (session_id, question_number);
