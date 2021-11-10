CREATE VIEW user_project AS
SELECT distinct project.*, sec_user.id as user_id
FROM project, acl_object_identity, sec_user, acl_sid, acl_entry 
WHERE project.id = acl_object_identity.object_id_identity
AND acl_sid.sid = sec_user.username
AND acl_entry.sid = acl_sid.id
AND acl_entry.acl_object_identity = acl_object_identity.id
AND sec_user.user_id is null
AND mask >= 1 AND project.deleted IS NULL

CREATE VIEW admin_project AS
SELECT distinct project.*, sec_user.id as user_id
FROM project, acl_object_identity, sec_user, acl_sid, acl_entry 
WHERE project.id = acl_object_identity.object_id_identity
AND acl_sid.sid = sec_user.username
AND acl_entry.sid = acl_sid.id
AND acl_entry.acl_object_identity = acl_object_identity.id
AND sec_user.user_id is null
AND mask >= 16 AND project.deleted IS NULL

CREATE VIEW creator_project AS
SELECT distinct project.*, sec_user.id as user_id
FROM project, acl_object_identity, sec_user, acl_sid
WHERE project.id = acl_object_identity.object_id_identity
AND acl_sid.sid = sec_user.username
AND acl_object_identity.owner_sid = acl_sid.id
AND sec_user.user_id is null AND project.deleted IS NULL

CREATE VIEW user_image AS 
SELECT image_instance.*, 
    abstract_image.original_filename as original_filename, 
    project.name as project_name, 
    project.blind_mode as project_blind, 
    sec_user.id as user_image_id, 
    case when MAX(mask) = 16 then true else false end as user_project_manager 
FROM image_instance 
INNER JOIN sec_user ON sec_user.user_id IS NULL 
INNER JOIN project ON project.id = image_instance.project_id AND project.deleted IS NULL 
INNER JOIN abstract_image ON abstract_image.id = image_instance.base_image_id 
INNER JOIN acl_object_identity ON project.id = acl_object_identity.object_id_identity 
INNER JOIN acl_sid ON acl_sid.sid = sec_user.username 
INNER JOIN acl_entry ON acl_entry.sid = acl_sid.id 
    AND acl_entry.acl_object_identity = acl_object_identity.id AND acl_entry.mask >= 1 
WHERE image_instance.deleted IS NULL 
    AND image_instance.parent_id IS NULL
GROUP BY image_instance.id, project.id, sec_user.id, abstract_image.id


CREATE TABLE task (id bigint,progress bigint,project_id bigint,user_id bigint,print_in_activity boolean)
CREATE TABLE task_comment (task_id bigint,comment character varying(255),timestamp bigint)