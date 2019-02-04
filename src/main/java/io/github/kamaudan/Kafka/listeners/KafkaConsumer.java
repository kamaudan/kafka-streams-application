package io.github.kamaudan.Kafka.listeners;

import io.github.kamaudan.Kafka.models.User;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {


    @KafkaListener(topics = "kafka_producer",groupId = "json_group",
            containerFactory = "userKafkaListenerFactory")
    public void consumeJson(User  user){
        System.out.println("Consumed Json Message" + user);

    }

}
