package com.example.kafkaconnectloader.controller;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

@Slf4j
@RestController
    public class AvroDataLoaderController {

    @Value("classpath:person.avsc")
    private Resource schemaFile;

        @PostMapping("/loadData")
        public String loadDataToKafka() {
            Properties props = new Properties();
            props.put("bootstrap.servers", "localhost:9092");
            props.put("key.serializer", StringSerializer.class.getName());
            props.put("value.serializer", KafkaAvroSerializer.class.getName());
            props.put("schema.registry.url", "http://localhost:8081");

            try (KafkaProducer<String, GenericRecord> producer = new KafkaProducer<>(props)) {

                Schema.Parser parser = new Schema.Parser();
                Schema schema = parser.parse(schemaFile.getInputStream());

                GenericRecord avroRecord = new GenericData.Record(schema);
                avroRecord.put("firstName", UUID.randomUUID().toString());
                avroRecord.put("lastName", UUID.randomUUID().toString());
                avroRecord.put("age", new Random().nextInt(120));

                ProducerRecord<String, GenericRecord> record = new ProducerRecord<>("test", UUID.randomUUID().toString(), avroRecord);

                producer.send(record, (recordMetadata, e) -> {
                    if (e != null) {
                        log.error("Failed to produce to Kafka", e);
                    } else {
                        log.info("Produced record to topic {} partition {} @ offset {} ",
                                recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
                    }
                });

                return "Data sent successfully to Kafka";

            } catch (Exception e) {
                log.error("Failed to produce to Kafka", e);
                return "Failed to send data to Kafka.";

            }
        }
    }
