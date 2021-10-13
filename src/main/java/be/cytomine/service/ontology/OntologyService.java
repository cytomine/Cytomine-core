package be.cytomine.service.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.ValidationError;
import be.cytomine.domain.command.*;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.CytomineException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.service.ModelService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OntologyService extends ModelService {

    private final OntologyRepository ontologyRepository;

    @Override
    public CommandResponse add(JsonObject jsonObject) {
        return null;
    }

    @Override
    public Class currentDomain() {
        return null;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return null;
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData) {
        return null;
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        return null;
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain){
        Ontology ontology = (Ontology)domain;
        if(ontology!=null && ontology.getName()!=null) {
            if(ontologyRepository.findByName(ontology.getName()).stream().anyMatch(x -> !Objects.equals(x.getId(), ontology.getId())))  {
                throw new AlreadyExistException("Ontology " + ontology.getName() + " already exist!");
            }
        }
    }



}
