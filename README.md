# Callisto Balance Calculator Service

Callisto Balance Calculator Service is a part of Callisto project (Check section [All Callisto Repositories](#headAllRepo) for links to all Callisto repositories)

## 1. Building the project

### Github Package dependencies
In order to pull in Github package dependencies you will need a Github Personal Access Token.
This token will need the minimum of 'packages:read' permissions.

Assign the value of the token to an environment variable with the name GITHUB_TOKEN

Then run the following to build the project

```sh
$ mvn -s ./timecard_settings.xml clean install
```

## 2. Running project locally

#### Ports

- Standalone service runs on port 9090.
- LocalDev solution runs on port 50210.
- Java Debugger runs on port 50211.

  Check `docker-compose.yml` for any port mappings.

## <a name="headLocalDev"></a> 3. Running project locally as part of LocalDev environment.

1. Download LocalDev repository from https://github.com/UKHomeOffice/callisto-localdev and run it locally as described in Scenario 1.

2. From the LocalDev project root, stop service by running `docker compose stop balance-calculator` command.

3. Pull Callisto Balance Calculator Service repository and from its root directory, run command `docker compose up -d`

After successful start, you should be able to work with code and all changes will be reflected within LocalDev environment.

### 3.1 Attaching Debugger

The service can also be debugged. To do so in a JetBrains IDE, a debugger has to be added (Run -> Edit Configurations.. -> Add Remote JVM Debug), with the port matching the external port mapped in the docker compose file of the service to debug. For example, that would be 5005 for this service.

## 4. Devtools Hot Deployment in local environment

Devtools allows you to reload the application after making any changes to the project files.
However, it may need stage of building project manually (InteliJ IDEA: Build/Build Project)
or IntelliJ IDEA has 2 properties that will allow you to execute `Build Project` automatically. To enable that :

1.  Go to `Preferences/Build,Execution,Deployment/Compiler` and select option
    `Build project automatically`
2.  [Optional] Go to `Preferences/Advanced Settings` and select `Allow auto-make to start even if developed application is currently running`

## 5. Actuator Kafka Error Endpoint

We have a counter configured for actuator which will give a metric on how many times deserialization has failed for kafka event messages.  
This can be accessed at

```sh
/actuator/metrics/balance.calculator.messages
```

## <a name="headAllRepo"></a> All Callisto repositories

- https://github.com/UKHomeOffice/callisto-accruals-restapi
- https://github.com/UKHomeOffice/callisto-balance-calculator
- https://github.com/UKHomeOffice/callisto-person-restapi
- https://github.com/UKHomeOffice/callisto-timecard-restapi
- https://github.com/UKHomeOffice/callisto-accruals-person-consumer
- https://github.com/UKHomeOffice/callisto-auth-keycloak
- https://github.com/UKHomeOffice/callisto-build-github
- https://github.com/UKHomeOffice/callisto-kafka-commons
- https://github.com/UKHomeOffice/callisto-devops
- https://github.com/UKHomeOffice/callisto-docs
- https://github.com/UKHomeOffice/callisto-helm-charts
- https://github.com/UKHomeOffice/callisto-ingress-nginx
- https://github.com/UKHomeOffice/callisto-jparest
- https://github.com/UKHomeOffice/callisto-localdev
- https://github.com/UKHomeOffice/callisto-postman-collections
- https://github.com/UKHomeOffice/callisto-service-template
- https://github.com/UKHomeOffice/callisto-ui
- https://github.com/UKHomeOffice/callisto-ui-nginx
