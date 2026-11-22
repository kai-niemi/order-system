package se.cloudneutral.orders.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;

import se.cloudneutral.orders.client.hypermedia.ProblemException;

@Configuration
public class ShellConfig {
    @Bean
    public ExitStatusExceptionMapper commandExceptionMapper() {
        return exception -> {
            if (exception instanceof IllegalArgumentException) {
                return new ExitStatus(2, "Invalid argument provided: " + exception.getMessage());
            }
            if (exception instanceof ProblemException problemException) {
                return new ExitStatus(problemException.getStatus().value(), problemException.getMessage());
            }
            exception.printStackTrace(System.err);
            return new ExitStatus(1, "An unexpected error occurred: " + exception.getMessage());
        };
    }
}
