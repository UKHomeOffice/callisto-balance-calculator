# Callisto Balance Calculator Service

## Building the project

### Github Package dependencies
In order to pull in Github package dependencies you will need a Github Personal Access Token.
This token will need the minimum of 'packages:read' permissions.

Update your .m2/settings.xml file to contain the <servers><server> tags like timecard_settings.xml
The token will need to live within your local .m2/settings.xml file as the password

For more info see:
[https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)

Then run the following to build the project

```sh
$ mvn clean install
```

## Actuator Kafka Error Endpoint
We have a counter configured for actuator which will give a metric on how many times deserialization has failed for kafka event messages.  
This can be accessed at

```sh
/actuator/metrics/balance.calculator.messages
```