CREATE TABLE "ATTENDANCE_RECORD_OVERRIDE_T"
(  "ID" NUMBER(19,0),
   "NETID" VARCHAR2(255 CHAR) NOT NULL,
   "SITE_ID" VARCHAR2(255 CHAR) NOT NULL,
   "EVENT_NAME" VARCHAR2(255 CHAR) NOT NULL,
   "STATUS" VARCHAR2(255 CHAR) NOT NULL
);
CREATE UNIQUE INDEX UNIQ_ATT_REC_OVR ON ATTENDANCE_RECORD_OVERRIDE_T(NETID, SITE_ID, EVENT_NAME);
ALTER TABLE ATTENDANCE_RECORD_OVERRIDE_T ADD (CONSTRAINT ATT_REC_OVR_PK PRIMARY KEY (ID));
CREATE SEQUENCE ATT_REC_OVR_SEQ;
CREATE OR REPLACE TRIGGER ATT_REC_OVR_INSERT
  BEFORE INSERT ON ATTENDANCE_RECORD_OVERRIDE_T
  FOR EACH ROW
BEGIN
  SELECT ATT_REC_OVR_SEQ.nextval
  INTO :new.id
  FROM dual;
END;


CREATE TABLE nyu_t_attendance_jobs (job varchar2(64), last_success_time NUMBER);
