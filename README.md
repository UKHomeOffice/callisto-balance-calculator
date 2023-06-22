# Callisto Balance Calculator Service

## Building the project

### Github Package dependencies
In order to pull in Github package dependencies you will need a Github Personal Access Token.
This token will need the minimum of 'packages:read' permissions.

Assign the value of the token to an environment variable with the name GITHUB_TOKEN

Then run the following to build the project

```sh
$ mvn -s ./timecard_settings.xml clean install
```
## Actuator Kafka Error Endpoint
We have a counter configured for actuator which will give a metric on how many times deserialization has failed for kafka event messages.  
This can be accessed at

```sh
/actuator/metrics/balance.calculator.messages
```