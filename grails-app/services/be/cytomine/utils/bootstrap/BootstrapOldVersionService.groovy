package be.cytomine.utils.bootstrap

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.image.AbstractImage
import be.cytomine.image.AbstractSlice
import be.cytomine.image.CompanionFile
import be.cytomine.image.ImageInstance
import be.cytomine.image.Mime
import be.cytomine.image.SliceInstance
import be.cytomine.image.multidim.ImageGroup
import be.cytomine.image.multidim.ImageGroupHDF5
import be.cytomine.image.multidim.ImageSequence
import be.cytomine.middleware.ImageServer
import be.cytomine.ontology.Track
import be.cytomine.middleware.AmqpQueue
import be.cytomine.image.server.Storage
import be.cytomine.image.UploadedFile
import be.cytomine.processing.ImageFilter
import be.cytomine.project.Project
import be.cytomine.security.SecRole
import be.cytomine.security.SecUser
import be.cytomine.security.SecUserSecRole
import be.cytomine.security.User
import be.cytomine.meta.Configuration
import be.cytomine.utils.Version
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Holders
import groovy.sql.Sql
import org.springframework.security.acls.domain.BasePermission

/**
 * Cytomine
 * User: lrollus
 * This class contains all code when you want to change the database dataset.
 * E.g.: add new rows for a specific version, drop a column, ...
 *
 * The main method ("execChangeForOldVersion") is called by the bootstrap.
 * This method automatically run all initYYYYMMDD() methods from this class where YYYYMMDD is lt version number
 *
 * E.g. init20150115() will be call if the current version is init20150201.
 * init20150101() won't be call because: 20150101 < 20150115 < 20150201.
 *
 * At the end of the execChangeForOldVersion, the current version will be set thanks to the grailsApplication.metadata.'app.version' config
 */
class BootstrapOldVersionService {

    def grailsApplication
    def bootstrapUtilsService
    def dataSource
    def permissionService
    def secUserService
    def storageService
    def tableService
    def mongo
    def noSQLCollectionService

    void execChangeForOldVersion() {
        def methods = this.metaClass.methods*.name.sort().unique()
        Version version = Version.getLastVersion()

        methods.findAll { it.startsWith("initv") }.each { method ->

            method = method.substring("initv".size())

            Short major = Short.parseShort(method.split("_")[0])
            Short minor = Short.parseShort(method.split("_")[1])
            Short patch = Short.parseShort(method.split("_")[2])

            if(major > version.major || (major == version.major && minor > version.minor)
                    || (major == version.major && minor == version.minor && patch > version.patch)) {
                log.info "Run code for v${method.replace("_",".")} update"
                this."initv$method"()
            } else {
                log.info "Skip code for initv$method"
            }

        }
        try {
            Version.setCurrentVersion(grailsApplication.metadata.'app.version')
        } catch(NumberFormatException ex) {
            log.warn "Cannot parse version ${grailsApplication.metadata.'app.version'}, ignore version"
            def forceVersion = Holders.config.cytomine.forceVersion
            log.warn "Check forceVersion $forceVersion"
            Version.setCurrentVersion(forceVersion)
        }
    }


    def initv3_2_0() {
        log.info "Migration to V3.2.0"
        def sql = new Sql(dataSource)

        /****** USERS ******/
        log.info "Migration of users"

        log.info "Users: Add new column isDeveloper"
        sql.executeUpdate("UPDATE sec_user SET is_developer = FALSE WHERE is_developer IS NULL;")
        bootstrapUtilsService.updateSqlColumnConstraint("sec_user", "is_developer", "SET DEFAULT FALSE")

        /******* SOFTWARE ******/
        //log.info "Migration of software"
        //bootstrapUtilsService.dropSqlColumnUniqueConstraint("software")


        /******* JOB ******/
        log.info "Migration of jobs"

        /*log.info "Jobs: Add new column favorite"
        sql.executeUpdate("UPDATE job SET favorite = FALSE WHERE favorite IS NULL;")
        bootstrapUtilsService.updateSqlColumnConstraint("job", "favorite", "SET DEFAULT FALSE")*/

        log.info("Jobs: Update attached files names of job logs to be displayed in webUI")
        sql.executeUpdate("UPDATE attached_file SET filename = 'log.out' " +
                "WHERE domain_class_name = 'be.cytomine.processing.Job' AND filename LIKE '%.out';")


        /****** STORAGE ******/
        log.info "Migration of storages"

        // Move old Long[] storages to one storage (only first one is kept)
        if (bootstrapUtilsService.checkSqlColumnExistence('uploaded_file', 'storages')) {
            log.info "Storage: Update storage references in uploaded_file"
            sql.eachRow("SELECT id, storages FROM uploaded_file") {
                def ufId = it[0]
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(it[1] as byte[]))
                Long[] storages = (Long[]) ois.readObject()
                sql.executeUpdate("UPDATE uploaded_file SET storage_id = ${storages.first()} WHERE id = ${ufId};")
                ois.close()
            }
            bootstrapUtilsService.dropSqlColumn("uploaded_file", "storages")
        }

        log.info "Storage: Remove no more used column"
        bootstrapUtilsService.dropSqlColumn("storage", "base_path")


        /****** IMAGE SERVER ******/
        log.info "Migration of image servers"

        log.info "Image server: Update image server reference in uploaded_file"
        //TODO: use old ImageServerStorage and StorageAbstractImage
        //TODO: manage all old image servers.
        def server = ImageServer.first()
        if (server) {
            UploadedFile.executeUpdate("update UploadedFile uf set uf.imageServer = ? where uf.imageServer is null",
                    [server])

            log.info "Image server: Update base path in image_server with known base path"
            ImageServer.executeUpdate("update ImageServer i set i.basePath = ? where i.basePath is null",
                    [grailsApplication.config.storage_path])
        }
        log.info "Image server: Remove no more used columns"
        bootstrapUtilsService.dropSqlColumn("image_server", "service")
        bootstrapUtilsService.dropSqlColumn("image_server", "class_name")

        /****** UPLOADED FILE (1) ******/
        if (bootstrapUtilsService.checkSqlColumnExistence('uploaded_file', 'image_id')) {
            log.info "Migration of uploaded files (1)"

            log.info "Set uploaded files with valid images and status to 'uploaded' as 'deployed'"
            sql.executeUpdate("UPDATE uploaded_file SET status = 2 WHERE image_id IS NOT NULL and status = 0;");

            log.info "Remove erroneous uploaded files (filename duplicates)"
            sql.executeUpdate("DELETE FROM uploaded_file WHERE size > 0 AND parent_id IS NOT NULL AND image_id IN (SELECT image_id FROM uploaded_file GROUP BY image_id, size HAVING count(*) = 2);")
            sql.executeUpdate("DELETE FROM uploaded_file WHERE size = 0 AND image_id IN (SELECT image_id FROM uploaded_file GROUP BY image_id HAVING COUNT(*) = 2);")
        }


        /****** ABSTRACT SLICE ******/
        if (bootstrapUtilsService.checkSqlColumnExistence('uploaded_file', 'image_id') && AbstractSlice.count() == 0) {
            log.info "Migration of abstract slice"

            log.info "Abstract slice: Add (0,0,0) abstract slice for all abstract images"
            def values = []
            sql.eachRow("select uploaded_file.id, image_id, mime_id, abstract_image.created " +
                    "from uploaded_file " +
                    "left join abstract_image on abstract_image.id = uploaded_file.image_id " +
                    "where image_id is not null") {
                values << [
                        id: "nextval('hibernate_sequence')",
                        created: "'${it.created}'",
                        version: 0,
                        image_id: it.image_id,
                        uploaded_file_id: it.id,
                        mime_id: it.mime_id,
                        channel: 0,
                        z_stack: 0,
                        time: 0
                ]
            }

            if(values.size() > 0) {
                def batchSize = 2000
                def fields = ["id", "created", "version", "image_id", "uploaded_file_id", "mime_id", "channel", "z_stack", "time"]
                def groups = values.collate(batchSize)
                groups.eachWithIndex { def vals, int i ->
                    def formatted = vals.collect { v -> "(" + fields.collect { f -> v[f] }.join(",") + ")"}
                    sql.execute('INSERT INTO abstract_slice (' + fields.join(",") + ') VALUES ' + formatted.join(",") + ';')
                    log.info "- Inserted ${i * batchSize} elements ($i / ${groups.size()})"
                }
            }
        }


        /****** SLICE INSTANCE ******/
        if (SliceInstance.count() == 0) {
            log.info "Migration of slice instances"

            log.info "Slice instance: Add (0,0,0) slice instance for all image instances which are not in an image group"
            def values = []
            sql.eachRow("SELECT image_instance.id as iiid, abstract_slice.id as asid, image_instance.project_id as pid, " +
                    "image_instance.created " +
                    "from image_instance " +
                    "left join abstract_image on abstract_image.id = image_instance.base_image_id " +
                    "left join abstract_slice on abstract_slice.image_id = abstract_image.id " +
                    "left join image_sequence on image_sequence.image_id = image_instance.id " +
                    "where abstract_slice.channel = 0 and abstract_slice.z_stack = 0 and abstract_slice.time = 0 " +
                    "and image_sequence.id is null;") {
                values << [
                        id: "nextval('hibernate_sequence')",
                        created: "'${it.created}'",
                        version: 0,
                        base_slice_id: it.asid,
                        image_id: it.iiid,
                        project_id: it.pid
                ]
            }

            if(values.size() > 0) {
                def batchSize = 2000
                def fields = ["id", "created", "version", "base_slice_id", "image_id", "project_id"]
                def groups = values.collate(batchSize)
                groups.eachWithIndex { def vals, int i ->
                    def formatted = vals.collect { v -> "(" + fields.collect { f -> v[f] }.join(",") + ")" }
                    sql.execute('INSERT INTO slice_instance (' + fields.join(",") + ') VALUES ' + formatted.join(",") + ';')
                    log.info "- Inserted ${i * batchSize} elements ($i / ${groups.size()})"
                }
            }
        }


        /****** ABSTRACT IMAGE ******/
        log.info "Migration of abstract images"

        if (!bootstrapUtilsService.checkSqlColumnExistence("abstract_image", "physical_sizex") &&
                bootstrapUtilsService.checkSqlColumnExistence("abstract_image", "resolution")) {
            bootstrapUtilsService.renameSqlColumn("abstract_image", "resolution","physical_sizex")
        }

        if (bootstrapUtilsService.checkSqlColumnExistence("abstract_image", "physical_sizex")) {
            log.info "Migration of image instances"
            new Sql(dataSource).executeUpdate("UPDATE abstract_image SET physical_size_x = physical_sizex;")
            bootstrapUtilsService.dropSqlColumn("abstract_image", "physical_sizex")

            if (bootstrapUtilsService.checkSqlColumnExistence("abstract_image", "physical_sizey")) {
                log.info "Migration of image instances"
                new Sql(dataSource).executeUpdate("UPDATE abstract_image SET physical_size_y = physical_sizey;")
                bootstrapUtilsService.dropSqlColumn("abstract_image", "physical_sizey")
            } else {
                new Sql(dataSource).executeUpdate("UPDATE abstract_image SET physical_size_y = physical_size_x;")
            }

            if (bootstrapUtilsService.checkSqlColumnExistence("abstract_image", "physical_sizez")) {
                log.info "Migration of image instances"
                new Sql(dataSource).executeUpdate("UPDATE abstract_image SET physical_size_z = physical_sizez;")
                bootstrapUtilsService.dropSqlColumn("abstract_image", "physical_sizez")
            }
        }

        log.info "Abstract image: update new fields depth, duration and channels"
        sql.executeUpdate("update abstract_image set depth = 1 where depth is null;")
        sql.executeUpdate("update abstract_image set duration = 1 where duration is null;")
        sql.executeUpdate("update abstract_image set channels = 1 where channels is null;")

        log.info "Abstract image: remove no more used columns"
        bootstrapUtilsService.dropSqlColumnUniqueConstraint("abstract_image")
        bootstrapUtilsService.dropSqlColumn("abstract_image", "filename")

        bootstrapUtilsService.dropSqlColumn("abstract_image", "path")

        log.info "Abstract image: Remove no more used DB cached thumbs"
        sql.executeUpdate("delete from attached_file where domain_class_name = 'be.cytomine.image.AbstractImage' and filename like '/image/thumb%';")


        /****** IMAGE INSTANCE ******/
        if (!bootstrapUtilsService.checkSqlColumnExistence("image_instance", "physical_sizex") &&
                bootstrapUtilsService.checkSqlColumnExistence("image_instance", "resolution")) {
            bootstrapUtilsService.renameSqlColumn("image_instance", "resolution","physical_sizex")
        }

        if (bootstrapUtilsService.checkSqlColumnExistence("image_instance", "physical_sizex")) {
            log.info "Migration of image instances"
            new Sql(dataSource).executeUpdate("UPDATE image_instance SET physical_size_x = physical_sizex;")
            new Sql(dataSource).executeUpdate("ALTER TABLE image_instance DROP COLUMN physical_sizex CASCADE;")

            if (bootstrapUtilsService.checkSqlColumnExistence("image_instance", "physical_sizey")) {
                log.info "Migration of image instances"
                new Sql(dataSource).executeUpdate("UPDATE image_instance SET physical_size_y = physical_sizey;")
                new Sql(dataSource).executeUpdate("ALTER TABLE image_instance DROP COLUMN physical_sizey CASCADE;")
            } else {
                new Sql(dataSource).executeUpdate("UPDATE image_instance SET physical_size_y = physical_size_x;")
            }

            if (bootstrapUtilsService.checkSqlColumnExistence("image_instance", "physical_sizez")) {
                log.info "Migration of image instances"
                new Sql(dataSource).executeUpdate("UPDATE image_instance SET physical_size_z = physical_sizez;")
                new Sql(dataSource).executeUpdate("ALTER TABLE image_instance DROP COLUMN physical_sizez CASCADE;")
            }
        }

        /****** IMAGE GROUP ******/
        def imageInstancesFromImageGroupToSlices = [:]
        def abstractImagesFromImageGroupToSlices = [:]
        def imageGroupsToImageInstances = [:]

        bootstrapUtilsService.updateSqlColumnConstraint("uploaded_file", "converted", "DROP NOT NULL")
        bootstrapUtilsService.updateSqlColumnConstraint("uploaded_file", "path", "DROP NOT NULL")
        bootstrapUtilsService.updateSqlColumnConstraint("abstract_image", "mime_id", "DROP NOT NULL")

        if (ImageGroup.count() > 0) {
            log.info "Migration of image groups"
            ImageGroup.findAll().each { group ->
                log.info "Image group: Convert group ${group.name}"
                def sequences = ImageSequence.findAllByImageGroup(group)
                if (sequences.size() == 0) {
                    log.info "Empty image group skipped"
                    return
                }
                def duration = sequences.collect { it.time }.unique().size()
                def depth = sequences.collect { it.zStack }.unique().size()
                def channels = sequences.collect { it.channel }.unique().size()
                def width = sequences[0].image.baseImage.width
                def height = sequences[0].image.baseImage.height
                def user = sequences[0].image.user
                Storage storage = Storage.findByUser(user)
                UploadedFile uf = new UploadedFile(originalFilename: group.name, filename: group.name,
                        user: user, storage: storage,
                        ext: "virt", imageServer: server, path: "/", converted: true,
                        contentType: "virtual/stack", size: 0, status: 100).save(flush: true, failOnError: true)

                def image = new AbstractImage(uploadedFile: uf, originalFilename: uf.originalFilename, user: user,
                        duration: duration, depth: depth, channels: channels, width: width, height: height)
                image.save(failOnError: true, flush: true)

                def project = group.project
                def imageInstance = new ImageInstance(baseImage: image, project: project, user: user)
                imageInstance.save(failOnError: true, flush: true)

                imageGroupsToImageInstances << [(group.id): imageInstance.id]

                def hdf5 = ImageGroupHDF5.findByGroupAndStatus(group, 3)
                if (hdf5) {
                    def hdf5Filename = hdf5.filename - grailsApplication.config.storage_path
                    hdf5Filename = hdf5Filename.substring(hdf5Filename.indexOf("/")+1).trim()
                    def profileUf = new UploadedFile(originalFilename: "profile.hdf5", filename: hdf5Filename, parent: uf,
                            user: user, storage: storage, ext: "hdf5", imageServer: server, path: "/", converted: true,
                            contentType: "application/x-hdf5", size: 0, status: 100).save(flush: true, failOnError: true)
                    new CompanionFile(uploadedFile: profileUf, image: image,
                            originalFilename: "profile.hdf5", filename: "profile.hdf5", type: "HDF5").save(flush: true, failOnError: true)
                }

                sql.executeUpdate("update attached_file set domain_class_name = 'be.cytomine.image.ImageInstance', " +
                        "domain_ident = ${imageInstance.id} where domain_ident = ${group.id}")

                sql.eachRow("SELECT ai.created, uf.id as ufid, ai.mime_id, seq.channel, seq.z_stack, seq.time, " +
                        "seq.image_id as iiid , ai.id as aiid " +
                        "FROM image_sequence seq " +
                        "LEFT JOIN image_instance ii ON seq.image_id = ii.id " +
                        "LEFT JOIN abstract_image ai ON ii.base_image_id = ai.id " +
                        "LEFT JOIN uploaded_file uf ON ai.id = uf.image_id " +
                        "WHERE seq.image_group_id = :group", [group: group.id]) {
                    // 1) create abstract slice
                    def absSlice = sql.executeInsert("INSERT INTO abstract_slice(id, created, version, image_id, uploaded_file_id," +
                            " mime_id, channel, z_stack, time) VALUES " +
                            "(nextval('hibernate_sequence'), '${it.created}', 0, ${image.id}, ${it.ufid}, " +
                            "${it.mime_id}, ${it.channel}, ${it.z_stack}, ${it.time})")
                    def absSliceId = absSlice[0][0]
                    abstractImagesFromImageGroupToSlices << [(it.aiid): absSliceId]

                    // 2) create slice_instance
                    def slice = sql.executeInsert("INSERT INTO slice_instance(id, created, version, project_id, " +
                            "image_id, base_slice_id) VALUES " +
                            "(nextval('hibernate_sequence'), '${it.created}', 0, ${project.id}, " +
                            "${imageInstance.id}, ${absSliceId})")
                    def sliceId = slice[0][0]
                    imageInstancesFromImageGroupToSlices << [(it.iiid): [slice: sliceId, image:imageInstance.id]]
                }
            }
        }

        bootstrapUtilsService.dropSqlColumn("abstract_image", "mime_id")


        /****** UPLOADED FILE ******/
        log.info "Migration of uploaded files (2)"
        if (bootstrapUtilsService.checkSqlColumnExistence('uploaded_file', 'image_id')) {
            log.info("Uploaded file: Change direction of UF - AI relation and use the root as AI uploaded file")
            sql.executeUpdate("update abstract_image " +
                    "set uploaded_file_id = cast(ltree2text(subltree(uploaded_file.l_tree, 0, 1)) as bigint) " +
                    "from uploaded_file " +
                    "where abstract_image.id = image_id " +
                    "and uploaded_file_id is null " +
                    "and cast(ltree2text(subltree(uploaded_file.l_tree, 0, 1)) as bigint) IN (SELECT id FROM uploaded_file);")
        }

        log.info "Uploaded file: Remove no more used columns"
        bootstrapUtilsService.dropSqlColumn("uploaded_file", "image_id")
        bootstrapUtilsService.dropSqlColumn("uploaded_file", "path")
        bootstrapUtilsService.dropSqlColumn("uploaded_file", "converted")

        log.info "Uploaded file: Migrate to new status"
        [[old: 1, "new": 104],
         [old: 2, "new": 100],
         [old: 3, "new": 11],
         [old: 4, "new": 31],
         [old: 5, "new": 20],
         [old: 6, "new": 40],
         [old: 7, "new": 20],
         [old: 8, "new": 41],
         [old: 9, "new": 41]].each {
            sql.executeUpdate("UPDATE uploaded_file SET status = ${it["new"]} WHERE status = ${it["old"]}")
        }


        /****** ANNOTATIONS ******/
        log.info "Migration of annotations"
        imageInstancesFromImageGroupToSlices.each { o, n ->
            log.info "Update annotation references to slices for old image instance $o that was linked to image group"
            sql.executeUpdate("update algo_annotation set image_id = ${n.image}, slice_id = ${n.slice} where image_id = ${o};")
            sql.executeUpdate("update user_annotation set image_id = ${n.image}, slice_id = ${n.slice} where image_id = ${o};")
            sql.executeUpdate("update reviewed_annotation set image_id = ${n.image}, slice_id = ${n.slice} where image_id = ${o};")
            sql.executeUpdate("update roi_annotation set image_id = ${n.image}, slice_id = ${n.slice} where image_id = ${o};")
            sql.executeUpdate("update annotation_index set image_id = ${n.image}, slice_id = ${n.slice} where image_id = ${o};")
        }

        log.info "Update algo annotation references to slices for 2D images"
        sql.executeUpdate("update algo_annotation " +
                "set slice_id = slice.id " +
                "from slice_instance slice " +
                "where slice.image_id = algo_annotation.image_id " +
                "and slice_id IS NULL")

        log.info "Update user annotation references to slices for 2D images"
        sql.executeUpdate("update user_annotation " +
                "set slice_id = slice.id " +
                "from slice_instance slice " +
                "where slice.image_id = user_annotation.image_id " +
                "and slice_id IS NULL")

        log.info "Update reviewed annotation references to slices for 2D images"
        sql.executeUpdate("update reviewed_annotation " +
                "set slice_id = slice.id " +
                "from slice_instance slice " +
                "where slice.image_id = reviewed_annotation.image_id " +
                "and slice_id IS NULL")

        log.info "Update ROI annotation references to slices for 2D images"
        sql.executeUpdate("update roi_annotation " +
                "set slice_id = slice.id " +
                "from slice_instance slice " +
                "where slice.image_id = roi_annotation.image_id " +
                "and slice_id IS NULL")


        /****** ANNOTATION INDEX ******/
        if (bootstrapUtilsService.checkSqlColumnExistence('annotation_index', 'image_id')) {
            log.info "Migration of annotation indexes"
            sql.executeUpdate("update annotation_index " +
                    "set slice_id = slice.id " +
                    "from slice_instance slice " +
                    "where slice.image_id = annotation_index.image_id " +
                    "and slice_id IS NULL")

            log.info "Annotation index: Remove no more used column"
            bootstrapUtilsService.dropSqlColumn("annotation_index", "image_id")
        }


        /****** MIME ******/
        log.info "Migration of mime types"

        log.info "Mime type: Update mime reference"
        def pyrTiffMime = Mime.findByMimeType("image/pyrtiff")
        def mimeToRemove = ["image/tiff", "image/tif", "zeiss/zvi"]
        mimeToRemove.each {
            def mime = Mime.findByMimeType(it)
            if (mime) {
                sql.executeUpdate("UPDATE abstract_slice SET mime_id = ${pyrTiffMime.id} WHERE mime_id = ${mime.id}")
                sql.executeUpdate("DELETE FROM mime_image_server WHERE mime_id = ${mime.id}")
                mime.delete()
            }
        }


        /****** CLEANING ******/
        log.info "Cleaning: Remove no more used files"

        if (imageInstancesFromImageGroupToSlices.size() > 0) {
            log.info "Cleaning: Delete old image instances used in image groups"
            sql.executeUpdate("DELETE FROM image_sequence " +
                    ('where image_id IN (' + imageInstancesFromImageGroupToSlices.keySet().join(',') + ')'))
            sql.executeUpdate("DELETE FROM image_instance " +
                    ('where id IN (' + imageInstancesFromImageGroupToSlices.keySet().join(',') + ')'))
            sql.executeUpdate("DELETE FROM image_grouphdf5;")
            sql.executeUpdate("DELETE FROM image_group;")
        }

        log.info "Cleaning: Delete old image references"
        sql.executeUpdate("DELETE FROM storage_abstract_image;")
//        sql.executeUpdate("delete from image_instance where id not in (select image_id from slice_instance);")
//        sql.executeUpdate("delete from abstract_image where id not in (select image_id from abstract_slice);")
        sql.executeUpdate("UPDATE image_instance ii SET deleted = NOW() WHERE NOT EXISTS(SELECT 1 FROM slice_instance si WHERE si.image_id = ii.id);")
        sql.executeUpdate("UPDATE abstract_image ai SET deleted = NOW() WHERE NOT EXISTS(SELECT 1 FROM abstract_slice asl WHERE asl.image_id = ai.id);")

        /****** COUNTERS ******/
        log.info "Migration of counters"

        log.info "Counters: recompute project counters"
        sql.executeUpdate("UPDATE project p SET " +
                "count_images = (select count(*) from image_instance ii where ii.deleted is null and ii.project_id = p.id), " +
                "count_annotations = (select count(*) from user_annotation ua left join image_instance ii on ii.id = ua.image_id where ua.deleted is null and ua.project_id = p.id and ii.deleted is null), " +
                "count_job_annotations = (select count(*) from algo_annotation aa left join image_instance ii on ii.id = aa.image_id where aa.deleted is null and ii.deleted is null and aa.project_id = p.id), " +
                "count_reviewed_annotations = (select count(*) from reviewed_annotation ra left join image_instance ii on ii.id = ra.image_id where ra.deleted is null and ii.deleted is null and ra.project_id = p.id);")

        log.info "Counters: recompute image counters"
        sql.executeUpdate("UPDATE image_instance ii SET " +
                "count_image_annotations = (select count(*) from user_annotation ua where ua.deleted is null and ua.image_id = ii.id), " +
                "count_image_job_annotations = (select count(*) from algo_annotation aa where aa.deleted is null and aa.image_id = ii.id), " +
                "count_image_reviewed_annotations = (select count(*) from reviewed_annotation ra where ra.deleted is null and ra.image_id = ii.id);")


        /****** TRACKS *******/
        if (Track.count() == 0) {
            log.info "Migration of tracks"

            def lastImageId = 0
            def lastGroupId = -1
            def trackId = 0
            sql.eachRow("select p.created, domain_ident, image_id, slice_id, key, value, " +
                    "(select value from property pp where pp.domain_ident = p.domain_ident " +
                    "and pp.key = 'CUSTOM_ANNOTATION_DEFAULT_COLOR') as color, project_id \n" +
                    "from property p\n" +
                    "join algo_annotation a on a.id = p.domain_ident\n" +
                    "where p.key = 'ANNOTATION_GROUP_ID'\n" +
                    "order by image_id asc, value asc;") {
                if (it.image_id != lastImageId || it.value != lastGroupId ) {
                    lastImageId = it.image_id
                    lastGroupId = it.value
                    log.info "Track: Add track ${lastGroupId} for image ${lastImageId}"
                    trackId = sql.executeInsert("INSERT INTO track(id, created, version, name, color, image_id, project_id)" +
                            "VALUES (nextval('hibernate_sequence'), '${it.created}', 0, 'Track #${it.value}', " +
                            "'${it.color}', ${it.image_id}, ${it.project_id});")[0][0]
                }

                sql.executeInsert("INSERT INTO annotation_track(id, created, version, annotation_class_name, " +
                        "annotation_ident, track_id, slice_id) VALUES " +
                        "(nextval('hibernate_sequence'), '${it.created}', 0, 'be.cytomine.ontology.AlgoAnnotation', " +
                        "${it.domain_ident}, ${trackId}, ${it.slice_id});")
            }
        }

        /****** DESCRIPTION ******/
        //TODO
//        log.info("Update reference of attached files that are used in description (only for project)")
//        sql.executeUpdate("update attached_file set domain_class_name = 'be.cytomine.utils.Description', " +
//                "domain_ident = description.id " +
//                "from description " +
//                "where attached_file.domain_ident = description.domain_ident " +
//                "and attached_file.domain_class_name = 'be.cytomine.project.Project';")


        /****** VIEWS ******/
        log.info "Regeneration of DB views"
        sql.executeUpdate("DROP VIEW user_image;")
        tableService.initTable()

        sql.close()
    }


    void initv3_0_2() {
        log.info "3.0.2"
        log.info "ontology permission recalculation"
        def projects = Project.findAllByDeletedIsNullAndOntologyIsNotNull()
        short i = 0
        SpringSecurityUtils.doWithAuth("superadmin", {
            projects.each { project ->
                log.info "project $project"
                secUserService.listUsers(project, false, false).each { user ->
                    permissionService.addPermission(project.ontology, user.username, BasePermission.READ)
                }
                secUserService.listAdmins(project, false).each { admin ->
                    permissionService.addPermission(project.ontology, admin.username, BasePermission.ADMINISTRATION)
                }
                i++
                log.info "$i/${projects.size()}"
            }
        })
    }


    void initv3_0_0() {
        log.info "3.0.0"
        new Sql(dataSource).executeUpdate("ALTER TABLE version DROP COLUMN IF EXISTS number;")

        boolean exists = new Sql(dataSource).rows("SELECT COLUMN_NAME " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'image_filter' and COLUMN_NAME = 'processing_server_id';").size() == 1
        if (exists) {
            new Sql(dataSource).executeUpdate("UPDATE image_filter SET processing_server_id = NULL;")
            new Sql(dataSource).executeUpdate("ALTER TABLE image_filter DROP COLUMN IF EXISTS processing_server_id;")
        }
        def imagingServer = bootstrapUtilsService.createNewImagingServer()
        ImageFilter.findAll().each {
            it.imagingServer = imagingServer
            it.save(flush: true)
        }

        exists = new Sql(dataSource).rows("SELECT COLUMN_NAME " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'processing_server' and COLUMN_NAME = 'url';").size() == 1
        if (exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server DROP COLUMN IF EXISTS url;")
            new Sql(dataSource).executeUpdate("DELETE FROM processing_server;")
        }
        exists = new Sql(dataSource).rows("SELECT COLUMN_NAME " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'processing_server' and COLUMN_NAME = 'username';").size() > 0
        if (!exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server ADD COLUMN IF NOT EXISTS name VARCHAR NOT NULL;")
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server ADD CONSTRAINT unique_name UNIQUE (name);")
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server ADD COLUMN IF NOT EXISTS username VARCHAR NOT NULL;")
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server ADD COLUMN IF NOT EXISTS index INTEGER NOT NULL;")
        }

        new Sql(dataSource).executeUpdate("ALTER TABLE software DROP COLUMN IF EXISTS service_name;")
        new Sql(dataSource).executeUpdate("ALTER TABLE software DROP COLUMN IF EXISTS result_sample;")
        def constraints = new Sql(dataSource).rows("SELECT CONSTRAINT_NAME " +
                "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE TABLE_NAME = 'software' and CONSTRAINT_TYPE = 'UNIQUE';")
        exists = constraints.size() == 1
        if (exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE software DROP CONSTRAINT "+constraints[0].constraint_name+";")
        }

        new Sql(dataSource).executeUpdate("UPDATE software SET deprecated = true WHERE deprecated IS NULL;")
        new Sql(dataSource).executeUpdate("UPDATE software_parameter SET server_parameter = false WHERE server_parameter IS NULL;")

        if(SecUser.findByUsername("rabbitmq")) {
            def rabbitmqUser = SecUser.findByUsername("rabbitmq")
            def superAdmin = SecRole.findByAuthority("ROLE_SUPER_ADMIN")
            if(!SecUserSecRole.findBySecUserAndSecRole(rabbitmqUser,superAdmin)) {
                new SecUserSecRole(secUser: rabbitmqUser,secRole: superAdmin).save(flush:true)
            }
        }

        AmqpQueue.findAllByNameLike("queueSoftware%").each {it.delete(flush: true)}

        bootstrapUtilsService.addDefaultProcessingServer()
        bootstrapUtilsService.addDefaultConstraints()

    }

    void initv2_1_0() {
        log.info "2.1.0"
        new Sql(dataSource).executeUpdate("ALTER TABLE project ADD COLUMN IF NOT EXISTS are_images_downloadable BOOLEAN DEFAULT FALSE;")
        noSQLCollectionService.dropIndex("lastConnection", "date_1")
    }

    void initv2_0_0() {
        log.info "2.0.0"
        new Sql(dataSource).executeUpdate("ALTER TABLE project ALTER COLUMN ontology_id DROP NOT NULL;")

        new Sql(dataSource).executeUpdate("UPDATE sec_user SET language = 'ENGLISH' WHERE language IS NULL;")
        new Sql(dataSource).executeUpdate("ALTER TABLE sec_user ALTER COLUMN language SET DEFAULT 'ENGLISH';")
        new Sql(dataSource).executeUpdate("ALTER TABLE sec_user ALTER COLUMN language SET NOT NULL;")
        new Sql(dataSource).executeUpdate("ALTER TABLE sec_user DROP COLUMN IF EXISTS skype_account;")
        new Sql(dataSource).executeUpdate("ALTER TABLE sec_user DROP COLUMN IF EXISTS sipAccount;")

        new Sql(dataSource).executeUpdate("DROP VIEW user_image;")
        tableService.initTable()

        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        db.annotationAction.update([:], [$rename:[annotation:'annotationIdent']], false, true)
        db.annotationAction.update([:], [$set:[annotationClassName: 'be.cytomine.ontology.UserAnnotation']], false, true)
        db.annotationAction.update([:], [$unset:[annotation:'']], false, true)

        for(User systemUser :User.findAllByUsernameInList(['ImageServer1', 'superadmin', 'admin', 'rabbitmq', 'monitoring'])){
            systemUser.origin = "SYSTEM"
            systemUser.save();
        }

        new Sql(dataSource).executeUpdate("UPDATE sec_user SET origin = 'BOOTSTRAP' WHERE origin IS NULL;")
    }

    void initv1_2_1() {
        log.info "1.2.1"
        List<Configuration> configurations = Configuration.findAllByKeyLike("%.%")

        for(int i = 0; i<configurations.size(); i++){
            configurations[i].key = configurations[i].key.replace(".","_")
            configurations[i].save()
        }

        bootstrapUtilsService.createConfigurations(true)
    }
}