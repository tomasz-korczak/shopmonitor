package smon;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Group;
import com.github.rvesse.airline.help.Help;
import com.github.rvesse.airline.parser.errors.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import smon.cli.*;
import smon.util.DatabaseInUse;

@SpringBootApplication
@Cli(name = "smon-cli",
        description = "Main shop monitor commandline interface",
        defaultCommand = Help.class,
        commands = {Help.class, ScanItemsCommand.class, SearchItemsCommand.class},
        groups = {
                @Group(name = "shop",
                        description = "Shops definition management",
                        commands = {ListShopsCommand.class, ImportCommand.class, ExportCommand.class, DiscoverCommand.class}),
                @Group(name = "report",
                        description = "Various statistics reporting ",
                        commands = {ReportingCommand.class})
        })
@Slf4j
public class Application implements CommandLineRunner, ExitCodeGenerator {

    /** process exit codes, so the scheduler can detect failures */
    public static final int EXIT_OK = 0;
    public static final int EXIT_FAILURE = 1;
    public static final int EXIT_USAGE = 2;


    @Autowired
    private ApplicationContext applicationContext;

    private int exitCode = EXIT_OK;

    public static void main(String... args) throws Exception {
        ConfigurableApplicationContext context;
        try {
            context = SpringApplication.run(Application.class, args);
        } catch (Throwable t) {
            // M7: another run has the H2 file open; this run cannot do anything, so say so in one line.
            // Printed to stderr directly: logging is set up by the same call that failed.
            if (DatabaseInUse.isDatabaseInUse(t)) {
                System.err.println("smon-cli: another run is still using ./data/smon, this run was skipped; nothing was saved or mailed.");
                System.exit(EXIT_FAILURE);
            }
            throw t;
        }
        System.exit(SpringApplication.exit(context));
    }

    @Override
    public void run(String... args) {
        com.github.rvesse.airline.Cli<Runnable> cli = new com.github.rvesse.airline.Cli<>(Application.class);
        Runnable cmd;
        try {
            cmd = cli.parse(args);
        } catch (ParseException e) {
            log.error("Invalid command line: {}", e.getMessage());
            exitCode = EXIT_USAGE;
            return;
        }
        // airline creates the command, so it is not a Spring bean; this fills its @Autowired fields.
        // Only our commands: airline's Help has an @Inject field that airline fills itself, Spring would fail on it.
        if (cmd instanceof BaseCommand) {
            applicationContext.getAutowireCapableBeanFactory().autowireBean(cmd);
        }
        try {
            cmd.run();
        } catch (UsageException e) {
            log.error("Invalid command line: {}", e.getMessage());
            exitCode = EXIT_USAGE;
        } catch (Exception e) {
            log.error("Program execution failed: {}", e.toString());
            log.debug("Program execution failed!", e);
            exitCode = EXIT_FAILURE;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

}
