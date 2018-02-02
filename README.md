# OpenTracing Linkerd Plugin

Plugin uses [TracerResolver](https://github.com/opentracing-contrib/java-tracerresolver)

## Installation

1. Build plugin
    ```    
    mvn clean package
    ```

1. Set `L5D_HOME` e.g. `export L5D_HOME=/apps/linkerd-1.3.x`

1. Copy built fat jar _linkerd-plugin-0.0.1.jar_ to linkerd plugins folder
    ```
    cp target/linkerd-plugin-0.0.1.jar ${L5D_HOME}/plugins/
    ```

1. Copy `TracerResolver` implementation to  ${L5D_HOME}/plugins/   

1. Add configuration to linkerd config file e.g. `${L5D_HOME}/config/linkerd.yaml`
    ```yaml
    telemetry:
    - kind: io.opentracing.tracer
 
1. Initialize `TracerResolver` implementation e.g. set tracer specific environment variables

1. Start linkerd
    ```
    ${L5D_HOME}/linkerd-1.3.x-exec ${L5D_HOME}/config/linkerd.yaml
    ```


### Jaeger example

1. Start Jaeger docker image
    ```bash
    docker run -d -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 -p5775:5775/udp -p6831:6831/udp -p6832:6832/udp \
     -p5778:5778 -p16686:16686 -p14268:14268 -p9411:9411 jaegertracing/all-in-one:latest
    ```

1. Navigate to http://localhost:16686 to access the Jaeger UI

1. Download and copy Jaeger TracerResolver jars to `${L5D_HOME}/plugins/`
    ```bash
    mvn dependency:get -Dartifact=com.uber.jaeger:jaeger-core:0.23.0:pom -Ddest=.
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.7:copy-dependencies -f jaeger-core-0.23.0.pom -DincludeScope=compile -DoutputDirectory=.
    ```
1. Set Jaeger environment variables 
    ```bash
    export JAEGER_SERVICE_NAME=linkerd
    ```
1. Start linkerd    
  