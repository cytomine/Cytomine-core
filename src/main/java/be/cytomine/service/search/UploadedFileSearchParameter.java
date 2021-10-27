package be.cytomine.service.search;

import be.cytomine.utils.filters.SearchParameterEntry;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
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
