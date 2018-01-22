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


  