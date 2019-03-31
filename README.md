## Event Router
Transform and re-route message to different topic. Intend to be used with Debezium connector to implement outbox pattern.
https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern/

### MongoDB Event Router

Event or outbox collection:

| Field |  Type  |   Description   | 
|-------|--------|-----------------|
| _id   | string | Event id        |
| type  | string | Event type      |
| aggregateId  | string | Aggregate id      |
| aggregateType  | string | Aggregate type     |
| aggregateType  | string | Aggregate type     |
| payload  | string | Actual event json string |

Example Event document:
```json
{
    "_id" : "1c65b115-1124-42ed-bca1-c6a80b29f1dd",
    "type" : "UserUpdatedEvent",
    "aggregateId" : "cdceb9bd-5065-4c58-9333-119dee03eeb5",
    "aggregateType" : "User",
    "payload" : "{\"id\":\"cdceb9bd-5065-4c58-9333-119dee03eeb5\",\"username\":\"shilva\",\"firstName\":\"Worawat\",\"lastName\":\"Wijarn\",\"aggregateId\":\"cdceb9bd-5065-4c58-9333-119dee03eeb5\",\"aggregateType\":\"User\"}",
}
```

We would need the Debezium MongoDB connector to monitor change in this event collection and send it to Kafka topic.
We also need to re-route message to different topic by using aggregate type. This will ensure that all event of the same type will go into same Kafka topic.
The payload must be json string and will be deserialized to the message. The message key is aggregate id. 
The type field will be put into message header, so consumer can use it to determine the class of target type for deserialization.
For example we use Spring Kafka JsonDeserializer with TYPE_MAPPINGS config to easily deserialize into target class.


### Setup Confluent Kafka

Follow instruction on 
https://docs.confluent.io/current/quickstart/ce-docker-quickstart.html

```
cd examples/cp-all-in-one
docker-compose -f docker-compose.yml up
```

### Setup Kafka Connector and custom SMT

#### Install Debezium mongodb connector

Download connector plugin
```
docker exec -it connect bash
wget https://repo1.maven.org/maven2/io/debezium/debezium-connector-mongodb/0.9.3.Final/debezium-connector-mongodb-0.9.3.Final-plugin.tar.gz
```

Extract plugin and move to plugin directory in kafka connector container
```
tar -zxvf debezium-connector-mongodb-0.9.3.Final-plugin.tar.gz
mv debezium-connector-mongodb /usr/share/java
```

Connect connector container to mongodb cluster network
```
docker network connect mongo-cluster connect
```

#### Install custom SMT

Build smt jar
```
cd smt
gradle clean build
```

Create a directory for custom smt in plugin directory
```
docker exec -it connect bash
cd /usr/share/java
mkdir kafka-smt
```

Copy smt jar to custom smt directory in the connector container
```
docker cp build/libs/kafka-smt-1.0.0-SNAPSHOT.jar connect:/usr/share/java/kafka-smt
```

Restart kafka connector
```
docker restart connect
```

Create Debezium mongodb connector
```
curl -X POST \
  http://localhost:8083/connectors \
  -H 'Content-Type: application/json' \
  -H 'Postman-Token: 29f0441b-f7bc-452b-83c3-e480e81eeb96' \
  -H 'cache-control: no-cache' \
  -d '{
    "name": "user-connector",
    "config": {
        "connector.class" : "io.debezium.connector.mongodb.MongoDbConnector",
        "value.converter": "org.apache.kafka.connect.storage.StringConverter",
        "tasks.max" : "1",
        "mongodb.hosts" : "rs0/mongo1:27017",
        "mongodb.name" : "logicdee",
        "database.whitelist" : "lduser",
        "collection.whitelist": "lduser.events",
        "database.history.kafka.bootstrap.servers" : "broker:9092",
        "transforms" : "router",
        "transforms.router.type" : "dev.logicdee.kafka.smt.mongo.EventRouter"
    }
}
'
```

Reference
https://debezium.io/docs/install/