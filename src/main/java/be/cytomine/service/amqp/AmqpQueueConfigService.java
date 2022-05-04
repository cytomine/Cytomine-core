package be.cytomine.service.amqp;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.middleware.AmqpQueueConfig;
import be.cytomine.domain.middleware.AmqpQueueConfigType;
import be.cytomine.repository.middleware.AmqpQueueConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AmqpQueueConfigService {

    
    @Autowired
    AmqpQueueConfigRepository amqpQueueConfigRepository;

    public void initAmqpQueueConfigDefaultValues() {

        if(amqpQueueConfigRepository.findByName("durable").isEmpty()) {
            AmqpQueueConfig aqcDurable = new AmqpQueueConfig();
            aqcDurable.setName("durable");
            aqcDurable.setDefaultValue("true");
            aqcDurable.setIndex(20);
            aqcDurable.setIsInMap(false);
            aqcDurable.setType(AmqpQueueConfigType.Boolean);
            amqpQueueConfigRepository.save(aqcDurable);
        }

        if(amqpQueueConfigRepository.findByName("exclusive").isEmpty()) {
            AmqpQueueConfig aqcExclusive = new AmqpQueueConfig();
            aqcExclusive.setName("exclusive");
            aqcExclusive.setDefaultValue("false");
            aqcExclusive.setIndex(40);
            aqcExclusive.setIsInMap(false);
            aqcExclusive.setType(AmqpQueueConfigType.Boolean);
            amqpQueueConfigRepository.save(aqcExclusive);
        }

        if(amqpQueueConfigRepository.findByName("autoDelete").isEmpty()) {
            AmqpQueueConfig aqcAutoDelete = new AmqpQueueConfig();
            aqcAutoDelete.setName("autoDelete");
            aqcAutoDelete.setDefaultValue("false");
            aqcAutoDelete.setIndex(60);
            aqcAutoDelete.setIsInMap(false);
            aqcAutoDelete.setType(AmqpQueueConfigType.Boolean);
            amqpQueueConfigRepository.save(aqcAutoDelete);
        }

        if(amqpQueueConfigRepository.findByName("parametersMap").isEmpty()) {
            AmqpQueueConfig aqcMap = new AmqpQueueConfig();
            aqcMap.setName("parametersMap");
            aqcMap.setDefaultValue(null);
            aqcMap.setIndex(80);
            aqcMap.setIsInMap(false);
            aqcMap.setType(AmqpQueueConfigType.String);
            amqpQueueConfigRepository.save(aqcMap);
        }
    }    
}
