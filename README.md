# Modules for Truck Manager Software
Part of my diploma thesis using [Kafka](https://kafka.apache.org) to replace existing server as main messaging hub for the application.

Kafka version bundled: Kafka 2.1.1 (for Scala 2.11)

To start Kafka and required Zookeeper run this command

  kafka/start.sh

It automatically starts kafka and creates example company topics.

To stop Kafka run this command

  kafka/stop.sh

To start debugging console consumer on those topics please run following command

  kafka/consumer.sh [name of the topic to consume] 