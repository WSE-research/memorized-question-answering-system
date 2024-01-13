package eu.wseresearch.memorizedquestionansweringsystem;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static ApplicationContext applicationContext;

    private Environment env;

    public Application(
            @Autowired Environment env
    ) {
        this.env = env;
    }

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(Application.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${springdoc.version}") String appVersion, //
            @Value("${spring.application.name}") String appName //
    ) {
        String server = "";

        if (env.getProperty("stardog.url") != null){
            server = "Stardog";
        } else {
            server = "Virtuoso";
        }

        return new OpenAPI().info(new Info()
                .title(appName) //
                .version(appVersion) //
                .description(
                        "OpenAPI 3 with Spring Boot provided this API documentation. It uses the current component's settings:<ul>" //
                                + "  <li>qado.triplestore: " + server + "</li>" //
                                + "  <li>qado.question.user: " + env.getProperty("qado.question.user") + "</li>" //
                                + "  <li>cross.origin: " + env.getProperty("cross.origin") + "</li>" //
                                + "  <li>memquestionansweringsystem.sparql.cache.specs: " + env.getProperty("memqasystem.sparql.cache.specs") + "</li>" //
                                + "</ul>") //
                .termsOfService("http://swagger.io/terms/") //
                .license(new License().name("Apache 2.0").url("http://springdoc.org")) //
        );
    }

}
