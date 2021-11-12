package be.cytomine.service.processing;

import org.springframework.stereotype.Service;

@Service
public class AmqpQueueService {

    public final static String queuePrefixProcessingServer = "queueProcessingServer";
    public final static String channelPrefixProcessingServer = "channelProcessingServer";
    public final static String exchangePrefixProcessingServer = "exchangeProcessingServer";
}
