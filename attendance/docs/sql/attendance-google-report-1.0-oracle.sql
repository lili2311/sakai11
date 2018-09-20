CREATE TABLE "ATTENDANCE_RECORD_OVERRIDE_T"
(
   "NETID" VARCHAR2(255 CHAR) NOT NULL,
   "SITE_ID" VARCHAR2(255 CHAR) NOT NULL,
   "EVENT_NAME" VARCHAR2(255 CHAR) NOT NULL,
   "STATUS" VARCHAR2(255 CHAR) NOT NULL
);
CREATE UNIQUE INDEX UNIQ_ATT_REC_OVR ON ATTENDANCE_RECORD_OVERRIDE_T(NETID, SITE_ID, EVENT_NAME);

CREATE TABLE nyu_t_attendance_jobs (job varchar2(64), last_success_time NUMBER);