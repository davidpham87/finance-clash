.tables
.show
.headers on

SELECT * FROM user;
SELECT * FROM quizz;
SELECT * FROM quizz_series;

SELECT * FROM questions;

SELECT name FROM sqlite_master WHERE type = "table";
