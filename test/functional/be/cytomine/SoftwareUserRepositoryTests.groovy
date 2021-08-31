package be.cytomine

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.SoftwareAPI
import be.cytomine.test.http.SoftwareUserRepositoryAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class SoftwareUserRepositoryTests {

    void testListSoftwareUserRepositoryWithCredential() {
        def result = SoftwareUserRepositoryAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testAddSoftwareUserRepositoryCorrect() {
        def repoToAdd = BasicInstanceBuilder.getSoftwareUserRepositoryNotExist()
        def result = SoftwareUserRepositoryAPI.create(repoToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idSoftware = result.data.id

        result = SoftwareUserRepositoryAPI.show(idSoftware, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddSoftwareUserRepositoryAlreadyExist() {
        BasicInstanceBuilder.getSoftwareUserRepository()
        def repoToAdd = BasicInstanceBuilder.getSoftwareUserRepository()
        def result = SoftwareUserRepositoryAPI.create(repoToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testUpdateSoftwareUserRepositoryCorrect() {
        def repo = BasicInstanceBuilder.getSoftwareUserRepositoryNotExist(true)
        def data = UpdateData.createUpdateSet(repo,[provider: ["OLDNAME", "NEWNAME"]])
        def resultBase = SoftwareUserRepositoryAPI.update(repo.id, data.postData, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resultBase.code
        def json = JSON.parse(resultBase.data)
        assert json instanceof JSONObject

        int idSoftwareUserRepository = json.softwareuserrepository.id

        def showResult = SoftwareUserRepositoryAPI.show(idSoftwareUserRepository, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        assert json.provider == "NEWNAME"
    }

    void testDeleteSoftwareUserRepository() {
        def repoToDelete = BasicInstanceBuilder.getSoftwareUserRepositoryNotExist(true)
        def id = repoToDelete.id
        def result = SoftwareUserRepositoryAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = SoftwareUserRepositoryAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code
    }

    void testDeleteSoftwareUserRepositoryWithSoftware() {
        def repoToDelete = BasicInstanceBuilder.getSoftwareUserRepositoryNotExist(true)
        def id = repoToDelete.id

        def software = BasicInstanceBuilder.getSoftwareNotExist(true)
        software.setSoftwareUserRepository(repoToDelete)
        software = BasicInstanceBuilder.saveDomain(software)

        def softwareResult = SoftwareAPI.show(software.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == softwareResult.code
        def json = JSON.parse(softwareResult.data)
        assert json.softwareUserRepository == repoToDelete.id

        def result = SoftwareUserRepositoryAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = SoftwareUserRepositoryAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        softwareResult = SoftwareAPI.show(software.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == softwareResult.code
        json = JSON.parse(softwareResult.data)
        assert json.softwareUserRepository instanceof JSONObject.Null
    }
}