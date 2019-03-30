## Custom SMT
Transform and re-route message to different topic depends on event aggregate type.

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
docker exect -it connect bash
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
cd /usr/share/java
mkdir kafka-smt
```

Copy smt jar to custom smt directory in the connector container
```
docker cp smt/build/libs/smt-1.0.0-SNAPSHOT.jar connect:/usr/share/java/kafka-smt
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
        "collection.whitelist": "lduser.outbox",
        "database.history.kafka.bootstrap.servers" : "broker:9092",
        "transforms" : "router",
        "transforms.router.type" : "dev.logicdee.kafka.smt.mongo.EventRouter"
    }
}
'
```

Restart kafka connector
```
docker restart connect
```

Reference
https://debezium.io/docs/install/