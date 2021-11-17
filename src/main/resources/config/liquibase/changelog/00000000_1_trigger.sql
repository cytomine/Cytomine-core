--changeset lrollus (generated):1636385276500-1
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 select count(trigger_name) from information_schema.triggers WHERE trigger_name = 'beforeinsertuserannotationtrigger'
CREATE OR REPLACE FUNCTION beforeInsertUserAnnotation() RETURNS TRIGGER AS $incUserAnnBefore$
            DECLARE
currentImage  image_instance%ROWTYPE;
            currentProject  project%ROWTYPE;
            currentIndex  annotation_index%ROWTYPE;
BEGIN
SELECT * INTO currentImage FROM image_instance where id = NEW.image_id FOR UPDATE;
SELECT * INTO currentProject FROM project where id = NEW.project_id FOR UPDATE;
SELECT * INTO currentIndex FROM annotation_index WHERE user_id = NEW.user_id AND slice_id = NEW.slice_id;
RETURN NEW;
END ;
            $incUserAnnBefore$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS beforeInsertUserAnnotationTrigger on user_annotation;
CREATE TRIGGER beforeInsertUserAnnotationTrigger BEFORE INSERT ON user_annotation FOR EACH ROW EXECUTE PROCEDURE beforeInsertUserAnnotation();
