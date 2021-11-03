package be.cytomine.service.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.repository.image.SliceInstanceRepository;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class SliceInstanceService extends ModelService {

    private CurrentUserService currentUserService;

    private SecurityACLService securityACLService;

    private SliceInstanceRepository sliceInstanceRepository;


    @Override
    public Class currentDomain() {
        return SliceInstance.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new SliceInstance().buildDomainFromJson(json, getEntityManager());
    }

    public Optional<SliceInstance> find(Long id) {
        Optional<SliceInstance> sliceInstance = sliceInstanceRepository.findById(id);
        sliceInstance.ifPresent(cf -> {
            securityACLService.check(cf.container(),READ);
        });
        return sliceInstance;
    }

    public SliceInstance get(Long id) {
        return find(id).orElse(null);
    }



    public Optional<SliceInstance> find(ImageInstance image, int c, int z, int t) {
        Optional<SliceInstance> optionalSliceInstance = sliceInstanceRepository.findByCZT(image, c, z, t);
        optionalSliceInstance.ifPresent(cf -> {
            securityACLService.check(cf.container(), READ);
        });
        return optionalSliceInstance;
    }

    public List<SliceInstance> list(ImageInstance image) {
        securityACLService.check(image.container(), READ);
        return sliceInstanceRepository.listByImageInstanceOrderedByCZT(image);
    }


    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(json.getJSONAttrLong("project"), Project.class, READ);
        securityACLService.checkIsNotReadOnly(json.getJSONAttrLong("project"), Project.class);
        return executeCommand(new AddCommand(currentUser),null, json);

    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(),READ);
        securityACLService.check(jsonNewData.getJSONAttrLong("project"), Project.class, READ);
        securityACLService.checkIsNotReadOnly(domain.container());
        securityACLService.checkIsNotReadOnly(jsonNewData.getJSONAttrLong("project"), Project.class);
        return executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(),READ);
        securityACLService.checkFullOrRestrictedForOwner(domain.container(), ((SliceInstance)domain).getImage().getUser());
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {
        SliceInstance sliceInstance = (SliceInstance)domain;
        Optional<SliceInstance> alreadyExist = sliceInstanceRepository.findByBaseSliceAndImage(sliceInstance.getBaseSlice(), sliceInstance.getImage());
        if (alreadyExist.isPresent() && (!Objects.equals(alreadyExist.get().getId(), domain.getId()))) {
            throw new AlreadyExistException("SliceInstance (C: " + sliceInstance.getBaseSlice().getChannel() + ", Z:" + sliceInstance.getBaseSlice().getZStack() + ", T:" + sliceInstance.getBaseSlice().getTime() + " already exists for ImageInstance " + sliceInstance.getImage().getId());
        }
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        SliceInstance sliceInstance = (SliceInstance) domain;
        deleteDependentAnnotationTrack(sliceInstance, transaction, task);
        deleteDependentAnnotationIndex(sliceInstance, transaction, task);
    }

    private void deleteDependentAnnotationTrack(SliceInstance slice, Transaction transaction, Task task) {
        // TODO:
//        AnnotationTrack.findAllBySlice(slice).each {
//            annotationTrackService.delete(it, transaction, task)
//        }
    }

    private void deleteDependentAnnotationIndex(SliceInstance slice, Transaction transaction, Task task) {
        //TODO:
//        AnnotationIndex.findAllBySlice(slice).each {
//            it.delete()
//        }
    }


    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(),
                ((SliceInstance)domain).getBaseSlice().getChannel(),
                ((SliceInstance)domain).getBaseSlice().getZStack(),
                ((SliceInstance)domain).getBaseSlice().getTime()
                );
    }

}
