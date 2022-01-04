package be.cytomine.service.search;

import be.cytomine.utils.filters.SearchParameterEntry;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class UploadedFileSearchParameter {

    SearchParameterEntry storage;

    SearchParameterEntry user;

    SearchParameterEntry originalFilename;

    public Optional<SearchParameterEntry> findStorage() {
        return Optional.ofNullable(storage);
    }

    public Optional<SearchParameterEntry> findUser() {
        return Optional.ofNullable(user);
    }

    public Optional<SearchParameterEntry> findOriginalFilename() {
        return Optional.ofNullable(originalFilename);
    }

    public List<SearchParameterEntry> toList() {
        List<SearchParameterEntry> list = new ArrayList<>();
        findStorage().ifPresent(list::add);
        findUser().ifPresent(list::add);
        findOriginalFilename().ifPresent(list::add);
        return list;
    }
}
