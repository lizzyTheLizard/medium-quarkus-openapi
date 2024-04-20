# Using OpenAPI in Quarkus

[Quarkus](https://quarkus.io/) is a Java framework created especially with Kubernetes-Natives Microservices in mind. It
promises fast boot time, low memory footprint and therewith near instant scale up. Quarkus claims to significantly
increases developer productivity with tooling, pre-built integrations, applications services, and live reload in the
blink of an eye. But does it hold up to its promises?

Laterly, I tried to create a Quarkus WebApplication implementing an OpenAPI specification. I was surprised how difficult
this turned out to be. In this article, I will show you how to implement OpenAPI in Quarkus and what problems I
encountered.

## Initial Setup

Quarkus Applications can be created using the [Quarkus CLI Tooling](https://quarkus.io/guides/cli-tooling). You can
already define the needed dependencies using quarkus extensions. I will use rest-jackson to implement a rest-api and quarkus-smallrye-openapi that let me use SwaggerUI. hibernate-validator is used to support the bean validation generated out of the openapi specification. `--no-code` just creates the project without any example code.

```shell
quarkus create app  site.gutschi.medium:quarkus-openapi \
    --extensions=rest-jackson,quarkus-smallrye-openapi,hibernate-validator
    --no-code
```

With the [Quarkiverse OpenAPI Generator](https://docs.quarkiverse.io/quarkus-openapi-generator/dev/index.html) a Quarkus specific OpenAPI generator exists. This has some advantages, e.g. it integrates well with the Quarkus Build process, and you do not have to configure additional maven plugins. However, while generating client side stubs  (if you want to call an API from your Quarkus application) seems fine, generating server stubs is only experimental and poorly documented. Unlike the client, the server stubs are not generate using the [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) but RedHats [Apicurio](https://www.apicur.io/). Therefore the various options of the OpenAPI Generator are not available.

The  [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) project on the other hand has no specific generator for Quarkus (unlike e.g. Spring Boot), but as Quarkus supports Jax-RS, you the Jax-RS generator supports Quarkus. However, it also did not support [Mutiny](https://smallrye.io/smallrye-mutiny/latest/) (the reactive programming framework used by Quarkus), and as I wanted to have an asynchronous API, this was problematic. The Jax-RS Generator did already support the Java Standard `java.util.concurrent`, but constantly converting between Futures and Mutiny was not an option for me.

However, as the OpenAPI Generator is open source, I could fix the problems myself. As an interface, Mutiny and `java.util.concurrent` works quite similar so I only had to replace the classes and imports with the Mutiny specific ones. The OpenAPI-Generator uses [Mustache](https://mustache.github.io/) to generate the Java-Source-Files, so this qas quite easy. My Pull-Request was quickly answered and merged, so the changes are already included in the latest version 7.5.0.

Therefore, I can now generate my asynchronous Quarkus Server Stubs using the OpenAPI Generator using the following Maven Configuration

```xml
     <plugin>
        <groupId>org.openapitools</groupId>
        <artifactId>openapi-generator-maven-plugin</artifactId>
        <version>${openapi-generator.version}</version>
        <executions>
          <execution>
            <goals>
              <goal>generate</goal>
            </goals>
            <configuration>
              <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
              <generatorName>jaxrs-spec</generatorName>
              <modelNamePrefix>Gen</modelNamePrefix>
              <configOptions>
                <library>quarkus</library>
                <useJakartaEe>true</useJakartaEe>
                <useSwaggerAnnotations>false</useSwaggerAnnotations>
                <useMicroProfileOpenAPIAnnotations>true</useMicroProfileOpenAPIAnnotations>
                <supportAsync>true</supportAsync>
                <useMutiny>true</useMutiny>
                <interfaceOnly>true</interfaceOnly>
                <dateLibrary>java8</dateLibrary>
                <useTags>true</useTags>
              </configOptions>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

Note the needed properties:
* `library` has to be set to `quarkus` to generate the Quarkus specific code.
  * `useJakartaEe` and `useMicroProfileOpenAPIAnnotations` are needed to use the annotations from the Quarkus extensions. Furthermore `useSwaggerAnnotations` has to be set to false, as Quarkus uses the MicroProfile OpenAPI Annotations.
  * `supportAsync` is needed to generate the asynchronous code and `useMutiny` tells the OpenAPI-Geneator to use Mutiny instead of `java.util.concurrent`.
  `interfaceOnly` (generates only the interfaces instead of full classes), `useTags` (use tags in the generated code) and `dateLibrary` (use Java 8 Date Library) are optional but recommended.

Then implementing the OpenAPI-Generated Interface is quite easyFor example, the following code implements the `BlogApi` Interface generated from the OpenAPI Specification:

```java
public class BlogResource implements BlogApi {

  @Override
  public Uni<GenSuccessResponse> createOrUpdatePost(String id, GenPostUpdate genPostUpdate) {
    return Uni.createFrom().failure(new NotAllowedException("Not allowed"));
  }
}
```

When you now start your Quarkus Application using `./mvnw quarkus:dev` you can open the SwaggerUI at `http://localhost:8080/q/swagger-ui/`.

## Conclusion
Implementing OpenAPI in Quarkus was not as easy as it should be. The Quarkus specific OpenAPI Generator is not yet mature and the OpenAPI Generator did not support Mutiny. However, thanks to the Open Source nature of the OpenAPI Generator, and it's simple setup using Mustache, I could fix this issue myself with a few lines of code. 

The updated plugin is easy to use and generates the needed asynchronous Quarkus Server Stubs. I do however suspect there might also be other things missing in the OpenAPI Generator for Quarkus, but the more people use it, the more issues will be found and fixed. So I encourage you to try it out and report any issues you find.

As soon as this generator is mature enough, I do think it makes sense to promote it as its own generator in the OpenAPI Generator project, setting better default settings for quarkus (e.g. remove the swagger and 'non-jakarta' annotations). This would make it easier to use and more people would find it. I will try to do this in the future, but for now, I am happy that I could fix the issues myself and use the OpenAPI Generator in my Quarkus Project.

Also, in my opinion it would make sense for the Quarkiverse Generator to use this generator for server stubs as well, as it already now provides more features and more flexibility that Apicurio. This would make the Quarkiverse Generator more powerful and easier to use. 
