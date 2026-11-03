package se.cloudneutral.orders.client.shell;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

import se.cloudneutral.orders.client.shell.support.AnsiPrintWriter;

@Component
public class AdminCommands {
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Command(value = "Exit the shell",
            name = {"quit"},
            alias = {"q"})
    public void quit(CommandContext ctx) {
        ctx.outputWriter().println("Quitting");
        SpringApplication.exit(applicationContext, () -> 0);
        System.exit(0);
    }

    @Command(value = "Show system information",
            name = {"system-info"},
            alias = {"si"})
    public void systemInfo(CommandContext ctx) {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

        PrintWriter pw = AnsiPrintWriter.wrap(ctx.outputWriter());
        pw.println("#(bright_white)>> OS#(default)");
        pw.println(" Arch: %s | OS: %s | Version: %s".formatted(os.getArch(), os.getName(), os.getVersion()));
        pw.println(" Available processors: %d".formatted(os.getAvailableProcessors()));
        pw.println(" Load avg: %f".formatted(os.getSystemLoadAverage()));

        RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
        pw.println("#(bright_white)>> Runtime#(default)");
        pw.println(" Uptime: %s".formatted(r.getUptime()));
        pw.println(
                " VM name: %s | Vendor: %s | Version: %s".formatted(r.getVmName(), r.getVmVendor(), r.getVmVersion()));

        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        pw.println("#(bright_white)>> Threads#(default)");
        pw.println(" CPU time: %d".formatted(t.getCurrentThreadCpuTime()));
        pw.println(" User time: %d".formatted(t.getCurrentThreadUserTime()));
        pw.println(" Peak threads: %d".formatted(t.getPeakThreadCount()));
        pw.println(" Thread #: %d".formatted(t.getThreadCount()));
        pw.println(" Total started threads: %d".formatted(t.getTotalStartedThreadCount()));

        Arrays.stream(t.getAllThreadIds()).sequential().forEach(value -> {
            pw.println(" Thread (%d): %s %s".formatted(value,
                    t.getThreadInfo(value).getThreadName(),
                    t.getThreadInfo(value).getThreadState().toString()
            ));
        });

        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        pw.println("#(bright_white)>> Memory#(default)");
        pw.println(" Heap: %s".formatted(m.getHeapMemoryUsage().toString()));
        pw.println(" Non-heap: %s".formatted(m.getNonHeapMemoryUsage().toString()));
    }


}


