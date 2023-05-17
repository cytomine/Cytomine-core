package be.cytomine.service.project;

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.exceptions.ServerException;
import be.cytomine.service.PermissionService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class ProjectClonerService {
    
    @Autowired
    ProjectService projectService;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    ProjectRepresentativeUserService representativeUserService;
    
    @Autowired
    ImageInstanceService imageInstanceService;

    public CommandResponse cloneProject(Project originalProject, String newName, boolean setupIncluded, boolean membersIncluded, boolean imagesIncluded, boolean annotationsIncluded) {

        if (annotationsIncluded && (!setupIncluded || !membersIncluded || !imagesIncluded)) {
            throw new ServerException("Cannot clone annotations if setups, members and images are not included");
        }

        try {
            Project project = new Project();
            project.setName(newName);
            if (setupIncluded) {
                cloneSetup(originalProject, project);
            }
            CommandResponse response = projectService.add(project.toJsonObject());
            Project targetProject = (Project) response.getObject();

            if (membersIncluded) {
                cloneMembers(originalProject, targetProject);
            }

            if (imagesIncluded) {
                cloneImages(originalProject, targetProject, annotationsIncluded);
            }
            return response;
        } catch (Exception e) {
            throw new ServerException("Cannot clone project " + originalProject.getId(), e);
        }

    }

    private void cloneImages(Project originalProject, Project targetProject, boolean annotationsIncluded) throws ClassNotFoundException {
        List<ImageInstance> imageInstanceFromOriginalProject = imageInstanceService.listByProject(originalProject);
        List<Long> usersFromOriginalProject =  securityACLService.getProjectUsersIds(targetProject);
        List annotationsTranfertMap = usersFromOriginalProject.stream().map(x -> Map.of("source", x, "destination", x)).toList();
        for (ImageInstance imageInstance : imageInstanceFromOriginalProject) {
            // In order to clone annotations, members must be migrated too.
            // so we are sure that the new project will have the same users layers as the original one
            imageInstanceService.cloneImage(
                    imageInstance,
                    targetProject,
                    true,
                    annotationsIncluded,
                    annotationsIncluded,
                    usersFromOriginalProject,
                    false,
                    false,
                    annotationsTranfertMap
            );
        }

    }

    public void cloneSetup(Project originalProject, Project targetProject) {
        targetProject.setOntology(originalProject.getOntology());
        targetProject.setBlindMode(originalProject.getBlindMode());
        targetProject.setAreImagesDownloadable(originalProject.getAreImagesDownloadable());
        targetProject.setClosed(originalProject.isClosed());
        targetProject.setHideAdminsLayers(originalProject.isHideAdminsLayers());
        targetProject.setHideUsersLayers(originalProject.isHideUsersLayers());
        targetProject.setMode(originalProject.getMode());
    }

    public void cloneMembers(Project originalProject, Project targetProject) {
        // This could be improved regarding performance, we could write a single read request + batch of insert
        List<String> usernames = securityACLService.getProjectUsers(originalProject);
        for (String username : usernames) {
            List<Integer> masks = permissionService.getPermissionInACL(originalProject, username);
            for (Integer mask : masks) {
                permissionService.addPermission(targetProject, username, mask);
            }
        }
        if (originalProject.getRepresentativeUsers()!=null) {
            for (ProjectRepresentativeUser representativeUser : originalProject.getRepresentativeUsers()) {
                if (representativeUserService.find(targetProject, representativeUser.getUser()).isEmpty()) {
                    representativeUserService.add(representativeUser.toJsonObject().withChange("id", null).withChange("project", targetProject.getId()));
                }
            }
        }
    }
}
