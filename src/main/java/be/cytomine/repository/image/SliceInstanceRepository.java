package be.cytomine.repository.image;


import be.cytomine.domain.image.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the abstract image entity.
 */
@Repository
public interface SliceInstanceRepository extends JpaRepository<SliceInstance, Long>, JpaSpecificationExecutor<SliceInstance> {

    Optional<SliceInstance> findByBaseSliceAndImage(AbstractSlice abstractSlice, ImageInstance imageInstance);

    List<SliceInstance> findAllByImage(ImageInstance imageInstance);

    List<SliceInstance> findAllByBaseSlice(AbstractSlice abstractSlice);

    @Query("SELECT si " +
            "FROM SliceInstance si INNER JOIN FETCH si.baseSlice as bs " +
            "WHERE si.image = :imageInstance " +
            "AND si.baseSlice.channel = :c " +
            "AND si.baseSlice.zStack = :z " +
            "AND si.baseSlice.time = :t")
    Optional<SliceInstance> findByCZT(ImageInstance imageInstance, int c, int z, int t);

    @Query("SELECT si " +
            "FROM SliceInstance si INNER JOIN FETCH si.baseSlice as bs " +
            "WHERE si.image = :imageInstance " +
            "ORDER BY " +
            "   si.baseSlice.channel ASC, " +
            "   si.baseSlice.zStack ASC, " +
            "   si.baseSlice.time ASC ")
    List<SliceInstance> listByImageInstanceOrderedByCZT(ImageInstance imageInstance);





}
