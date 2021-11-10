package be.cytomine.service.image;


import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.dto.SliceCoordinate;
import be.cytomine.dto.SliceCoordinates;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class SliceCoordinatesServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SliceCoordinatesService sliceCoordinatesService;

    @Test
    public void get_slice_coordinates_are_ordered() {
        AbstractImage image = builder.given_an_abstract_image();

        List<Integer> channels = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> zStacks = new ArrayList<>(Arrays.asList(10, 20));
        List<Integer> times = new ArrayList<>(Arrays.asList(100, 200, 300, 400));

        Collections.shuffle(channels);
        Collections.shuffle(zStacks);
        Collections.shuffle(times);

        buildSlices(image, channels, zStacks, times);

        SliceCoordinates sliceCoordinates = sliceCoordinatesService.getSliceCoordinates(image);

        assertThat(sliceCoordinates.getChannels()).containsExactly(1, 2, 3); //order matter
        assertThat(sliceCoordinates.getZStacks()).containsExactly(10, 20); //order matter
        assertThat(sliceCoordinates.getTimes()).containsExactly(100, 200, 300, 400); //order matter
    }


    @Test
    public void get_slice_coordinates_reference() {
        AbstractImage image = builder.given_an_abstract_image();

        List<Integer> channels = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> zStacks = new ArrayList<>(Arrays.asList(10, 20));
        List<Integer> times = new ArrayList<>(Arrays.asList(100, 200, 300, 400));

        buildSlices(image, channels, zStacks, times);

        SliceCoordinate sliceCoordinate = sliceCoordinatesService.getReferenceSliceCoordinate(image);

        assertThat(sliceCoordinate.getChannel()).isEqualTo(2);
        assertThat(sliceCoordinate.getZStack()).isEqualTo(20);
        assertThat(sliceCoordinate.getTime()).isEqualTo(300);
    }



    @Test
    public void get_reference_slice() {
        AbstractImage image = builder.given_an_abstract_image();

        List<Integer> channels = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> zStacks = new ArrayList<>(Arrays.asList(10, 20));
        List<Integer> times = new ArrayList<>(Arrays.asList(100, 200, 300, 400));

        buildSlices(image, channels, zStacks, times);

        AbstractSlice slice = sliceCoordinatesService.getReferenceSlice(image);

        assertThat(slice.getChannel()).isEqualTo(2);
        assertThat(slice.getZStack()).isEqualTo(20);
        assertThat(slice.getTime()).isEqualTo(300);
    }


    private void buildSlices(AbstractImage image, List<Integer> channels, List<Integer> zStacks, List<Integer> times) {
        for (Integer channel : channels) {
            for (Integer zStack : zStacks) {
                for (Integer time : times) {
                    buildSlice(image, channel, zStack, time);
                }
            }
        }
    }


    private AbstractSlice buildSlice(AbstractImage image, int c, int z, int t) {
        AbstractSlice slice = builder.given_an_abstract_slice();
        slice.setImage(image);
        slice.setChannel(c);
        slice.setZStack(z);
        slice.setTime(t);
        return builder.persistAndReturn(slice);
    }
}
