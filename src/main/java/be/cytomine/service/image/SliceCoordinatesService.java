package be.cytomine.service.image;

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.dto.SliceCoordinate;
import be.cytomine.dto.SliceCoordinates;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.AbstractSliceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SliceCoordinatesService {

    @Autowired
    AbstractSliceRepository abstractSliceRepository;

    public SliceCoordinates getSliceCoordinates(AbstractImage image) {
        List<AbstractSlice> slices = abstractSliceRepository.findAllByImage(image);
        SliceCoordinates sliceCoordinates = new SliceCoordinates(
                slices.stream().map(AbstractSlice::getChannel).distinct().sorted().collect(Collectors.toList()),
                slices.stream().map(AbstractSlice::getZStack).distinct().sorted().collect(Collectors.toList()),
                slices.stream().map(AbstractSlice::getTime).distinct().sorted().collect(Collectors.toList())
        );
        return sliceCoordinates;
    }


    public SliceCoordinate getReferenceSliceCoordinate(AbstractImage image) {
        SliceCoordinates sliceCoordinates = getSliceCoordinates(image);
        SliceCoordinate referenceSliceCoordinates = new SliceCoordinate(
                sliceCoordinates.getChannels().get((int) Math.floor(sliceCoordinates.getChannels().size()/2)),
                sliceCoordinates.getZStacks().get((int) Math.floor(sliceCoordinates.getZStacks().size()/2)),
                sliceCoordinates.getTimes().get((int) Math.floor(sliceCoordinates.getTimes().size()/2))
        );
        return referenceSliceCoordinates;
    }

    public AbstractSlice getReferenceSlice(AbstractImage abstractImage) {
        SliceCoordinate sliceCoordinate = getReferenceSliceCoordinate(abstractImage);
        return abstractSliceRepository.findByImageAndChannelAndZStackAndTime(abstractImage, sliceCoordinate.getChannel(), sliceCoordinate.getZStack(), sliceCoordinate.getTime())
                .orElseThrow(() -> new ObjectNotFoundException("AbstractSlice", "image:" + abstractImage.getId() + "," + sliceCoordinate.getChannel()+ ":" + sliceCoordinate.getZStack() + ":" + sliceCoordinate.getTime()));
    }

}
