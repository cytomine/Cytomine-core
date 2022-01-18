/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
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


package be.cytomine

import be.cytomine.image.multidim.ImageGroup
import be.cytomine.image.multidim.ImageGroupHDF5
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageGroupHDF5API
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by laurent on 07.02.17.
 */
class ImageGroupHDF5Tests {

    void testShowPixelSpectra(){
//        ImageGroupHDF5 imageGroupHDF5 = BasicInstanceBuilder.getImageGroupHDF5NotExist(false)
//        imageGroupHDF5.filename = "/data/28/hdf5_35398"
//        imageGroupHDF5 = BasicInstanceBuilder.saveDomain(imageGroupHDF5)
//
//        def result = ImageGroupHDF5API.pixel(imageGroupHDF5.group.id, 0,0, Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
//        assert 200 == result.code
    }

    /*void testDeleteImageGroupHDF5(){
        ImageGroupHDF5 imageGroupHDF5 = BasicInstanceBuilder.getImageGroupHDF5()
        def result = ImageGroupHDF5API.delete(imageGroupHDF5.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 403 == result.code
        def json = JSON.parse(result.data)
        assert json.errors.contains("removed")
        //assert 200 == result.code
    }

    void testAddImageGroupHDF5() {
        ImageGroupHDF5 imageGroupHDF5 = BasicInstanceBuilder.getImageGroupHDF5NotExist(false)
        println imageGroupHDF5.class
        println imageGroupHDF5
        def result = ImageGroupHDF5API.create(((ImageGroupHDF5)imageGroupHDF5).encodeAsJSON(), Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        // invalid because no imageSequence
        assert 403 == result.code
        //assert 400 == result.code
    }


    //This test launch a background task that could take a long time, so it is not really achievable if the test server is closed before
    //TODO mock
/*    void testAddAndConvertImageGroupHDF5(){
        def imgs = []
        imgs << new AbstractImage(filename: "1.jpg", scanner: BasicInstanceBuilder.getScanner(), sample: null, mime: BasicInstanceBuilder.getMime(), path: "/home/laurent/sample/1-6/", width: 15653, height: 11296)
        imgs << new AbstractImage(filename: "2.jpg", scanner: BasicInstanceBuilder.getScanner(), sample: null, mime: BasicInstanceBuilder.getMime(), path: "/home/laurent/sample/1-6/", width: 15653, height: 11296)
        imgs << new AbstractImage(filename: "3.jpg", scanner: BasicInstanceBuilder.getScanner(), sample: null, mime: BasicInstanceBuilder.getMime(), path: "/home/laurent/sample/1-6/", width: 15653, height: 11296)
        imgs.each { BasicInstanceBuilder.saveDomain(it)}
        ImageGroupHDF5 imageGroupHDF5 = BasicInstanceBuilder.getImageGroupHDF5NotExist(false, imgs)
        def result = ImageGroupHDF5API.create(imageGroupHDF5.encodeAsJSON(), Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        assert 200 == result.code
        int resID = result.data.id
        result = ImageGroupHDF5API.show(resID, Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        assert 200 == result.code

    }
*/
    void testShowImageGroupFromId() {
        ImageGroupHDF5 imageGroupHDF5 = BasicInstanceBuilder.getImageGroupHDF5()
        println imageGroupHDF5
        def result = ImageGroupHDF5API.show(imageGroupHDF5.id, Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        assert 404 == result.code
    }

    void testShowImageGroupFromImageGroupId(){
        ImageGroup imageGroup = BasicInstanceBuilder.getImageGroup()
        ImageGroupHDF5 imageGroupHDF5 = BasicInstanceBuilder.getImageGroupHDF5NotExist(false)
        imageGroupHDF5.group = imageGroup
        BasicInstanceBuilder.saveDomain(imageGroupHDF5)

        def result = ImageGroupHDF5API.showFromImageGroup(imageGroup.id ,Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        assert 404 == result.code
    }

}
