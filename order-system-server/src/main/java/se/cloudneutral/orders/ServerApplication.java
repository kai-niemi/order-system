package se.cloudneutral.orders;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

import se.cloudneutral.orders.config.IdempotencyProperties;

@SpringBootApplication
@EnableHypermediaSupport(type = {
        EnableHypermediaSupport.HypermediaType.HAL_FORMS,
        EnableHypermediaSupport.HypermediaType.HAL,
})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(IdempotencyProperties.class)
@EnableScheduling
public class ServerApplication {
    //    public static void main(String[] args) {
//        new SpringApplicationBuilder(ServerApplication.class)
//                .logStartupInfo(true)
//                .web(WebApplicationType.SERVLET)
//                .bannerMode(Banner.Mode.CONSOLE)
//                .run(args);
//    }
    public static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    private static void printHelpAndExit(Consumer<PrintWriter> message) {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
        pw.printf("Usage: java -jar order-system-server.jar [options] <profile> [args...]%n%n");
        pw.printf("Undefined options are passed through to spring boot.%n%n");
        pw.printf("Options include:%n");
        pw.printf("--help                    this help%n");
        pw.printf("--profiles [profile,..]   override spring profiles to activate%n");
        pw.printf("@<file>                   read shell commands from file (non-interactive)%n");
        pw.println();

        message.accept(pw);

        pw.println();

        System.exit(0);
    }

    public static void main(String[] args) {
        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));
        LinkedList<String> passThroughArgs = new LinkedList<>();

        Set<String> profiles =
                StringUtils.commaDelimitedListToSet(System.getProperty(SPRING_PROFILES_ACTIVE));

        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.equals("--help")) {
                printHelpAndExit(pw -> {
                });
            } else if (arg.equals("--profiles")) {
                if (argsList.isEmpty()) {
                    printHelpAndExit(pw ->
                            pw.println("Expected list of profile names#(default)"));
                }
                profiles.clear();
                profiles.addAll(StringUtils.commaDelimitedListToSet(argsList.pop()));
            } else {
                if (arg.startsWith("--") || arg.startsWith("@")) {
                    passThroughArgs.add(arg);
                } else {
                    profiles.add(arg);
                }
            }
        }

        System.setProperty(SPRING_PROFILES_ACTIVE, String.join(",", profiles));

        new SpringApplicationBuilder(ServerApplication.class)
                .web(WebApplicationType.SERVLET)
                .bannerMode(Banner.Mode.CONSOLE)
                .logStartupInfo(true)
                .profiles(profiles.toArray(new String[0]))
                .run(passThroughArgs.toArray(new String[] {}));
    }
}


