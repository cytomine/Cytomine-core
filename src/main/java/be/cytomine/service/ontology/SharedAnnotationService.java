package be.cytomine.service.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.*;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.dto.CropParameter;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.transaction.Transactional;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
public class SharedAnnotationService extends ModelService {

    @Autowired
    private SharedAnnotationRepository sharedAnnotationRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AnnotationDomainRepository annotationDomainRepository;


    @Autowired
    private SecUserService secUserService;

    @Autowired
    private SecRoleRepository secRoleRepository;

    @Autowired
    private SecUserSecRoleRepository secUserSecRoleRepository;

    private ImageServerService imageServerService;

    @Autowired
    public void setImageServerService(ImageServerService imageServerService) {
        this.imageServerService = imageServerService;
    }

    @Override
    public Class currentDomain() {
        return SharedAnnotation.class;
    }

    /**
     * List all sharedAnnotation, Only for admin
     */
    public List<SharedAnnotation> list() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return sharedAnnotationRepository.findAll();
    }

    public SharedAnnotation get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<SharedAnnotation> find(Long id) {
        Optional<SharedAnnotation> optionalSharedAnnotation = sharedAnnotationRepository.findById(id);
        optionalSharedAnnotation.ifPresent(sharedAnnotation -> securityACLService.check(sharedAnnotation.container(),READ));
        return optionalSharedAnnotation;
    }


    public List<SharedAnnotation> listComments(AnnotationDomain annotation) {
        User user = (User)currentUserService.getCurrentUser();
        List<SharedAnnotation> sharedAnnotations = sharedAnnotationRepository
                .findAllByAnnotationIdentOrderByCreatedDesc(annotation.getId());

        sharedAnnotations = sharedAnnotations.stream().filter(x -> x.getSender()==user || x.getReceivers().contains(user))
                .distinct()
                .collect(Collectors.toList());

        return sharedAnnotations;
    }


    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    public CommandResponse add(JsonObject jsonObject) {
        User sender = (User)currentUserService.getCurrentUser();
        securityACLService.checkUser(sender);

        AnnotationDomain annotation = annotationDomainRepository.findById(jsonObject.getJSONAttrLong("annotationIdent"))
                        .orElseThrow(() -> new ObjectNotFoundException("Annotation", jsonObject.getJSONAttrStr("annotationIdent")));

        jsonObject.putIfAbsent("sender", sender.getId());

        String cid = UUID.randomUUID().toString();

        //create annotation crop (will be send with comment)
        File annotationCrop = null;
        try {
            CropParameter cropParameter = new CropParameter();
            cropParameter.setFormat("png");
            cropParameter.setAlphaMask(true);
            cropParameter.setGeometry(annotation.getWktLocation());
            cropParameter.setComplete(true);
            cropParameter.setMaxSize(512);

            BufferedImage bufferedImage;
            try {
                InputStream is = new ByteArrayInputStream(imageServerService.crop(annotation, cropParameter));
                bufferedImage = ImageIO.read(is);
            } catch(Exception e) {
                bufferedImage = null;
            }

            log.info("Image " + bufferedImage);

            if (bufferedImage != null) {
                annotationCrop = File.createTempFile("temp", "."+cropParameter.getFormat());
                annotationCrop.deleteOnExit();
                ImageIO.write(bufferedImage, cropParameter.getFormat(), annotationCrop);
            }
        } catch (FileNotFoundException e) {
            annotationCrop = null;
        } catch (IOException e) {
            log.error("Cannot retrieve crop", e);
            throw new WrongArgumentException("Cannot retrieve crop:" + e);
        }

        Map<String, Object> attachments = new LinkedHashMap<>();
        if (annotationCrop != null) {
            attachments.put(cid, annotationCrop);
        }

        //do receivers email list
        List<String> receiversEmail = new ArrayList<>();
        List<User> receivers = new ArrayList<>();

        if (jsonObject.containsKey("receivers")) {
            receiversEmail = jsonObject.getJSONAttrListLong("receivers")
                    .stream()
                    .map(x -> getEntityManager().find(User.class, x))
                    .map(User::getEmail)
                    .collect(Collectors.toList());
        } else if (jsonObject.containsKey("emails")) {
            receiversEmail = Arrays.stream(jsonObject.getJSONAttrStr("emails").split(",")).collect(Collectors.toList());

            for (String email : receiversEmail) {
                if (secUserService.findByEmail(email).isEmpty()) {
                    JsonObject guestUser = JsonObject.of(
                            "username", email,
                            "firstname", "firstname",
                            "lastname", "lastname",
                            "email", email,
                            "password", "passwordExpired"
                    );

                    secUserService.add(guestUser);
                    User user = (User) secUserService.findByUsername(email).get();
                    SecRole secRole = secRoleRepository.findByAuthority("ROLE_GUEST").get();

                    SecUserSecRole secUserSecRole = new SecUserSecRole();
                    secUserSecRole.setSecUser(user);
                    secUserSecRole.setSecRole(secRole);
                    secUserSecRole = secUserSecRoleRepository.save(secUserSecRole);

                    secUserService.addUserToProject(user, annotation.getProject(), false);

                    user.setPasswordExpired(true);
                    getEntityManager().persist(user);
                    throw new CytomineMethodNotYetImplementedException("todo");
                    //TODO:
//                    ForgotPasswordToken forgotPasswordToken = new ForgotPasswordToken(
//                            user : user,
//                            tokenKey: UUID.randomUUID().toString(),
//                            expiryDate: new Date() + 1
//                        ).save()
//                    notificationService.notifyWelcome(sender, user, forgotPasswordToken)
                }
                receivers.add((User)secUserService.findByUsername(email).get());
            }
            jsonObject.put("receivers", receivers.stream().map(x -> x.getId()).collect(Collectors.toList()));
        }

        securityACLService.checkFullOrRestrictedForOwner(annotation, annotation.user());
        CommandResponse result =  executeCommand(new AddCommand(sender), null, jsonObject);

        throw new CytomineMethodNotYetImplementedException("todo");
        //TODO
//        if (result!=null) {
//            log.info("send mail to " + receiversEmail);
//            try {
//                notificationService.notifyShareAnnotation(sender, receiversEmail, json, attachments, cid)
//            } catch (MiddlewareException e) {
//                if(Environment.getCurrent() == Environment.DEVELOPMENT){
//                    e.printStackTrace()
//                } else {
//                    throw e
//                }
//            }
//        }
//
//        return result;
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
        securityACLService.checkFullOrRestrictedForOwner(domain.container(),((SharedAnnotation)domain).getSender());
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }


    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new SharedAnnotation().buildDomainFromJson(json, getEntityManager());
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain){

    }

    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        SharedAnnotation sharedAnnotation = (SharedAnnotation)domain;
        return Arrays.asList(String.valueOf(sharedAnnotation.getSender().getId()), String.valueOf(sharedAnnotation.getAnnotationIdent()), sharedAnnotation.getAnnotationClassName());
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        throw new RuntimeException("Update is not implemented for shared annotation");
    }

    @Override
    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
    }

}
