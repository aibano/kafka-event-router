## Custom SMT
* Transform and re-route message to different topic depends on event aggregate type.

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

Restart kafka connector
```
docker restart connect
```

Reference
https://debezium.io/docs/install/