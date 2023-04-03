# Callisto Balance Calculator Service

## Building the project

```sh
$ mvn clean install
```

## Actuator Kafka Error Endpoint
We have a counter configured for actuator which will give a metric on how many times deserilasation has failed for kafka event messages.  
This can be accessed at

```sh
/actuator/metrics/balance.calculator.messages
```