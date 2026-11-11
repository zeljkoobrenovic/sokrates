package nl.obren.sokrates.cli;

import nl.obren.sokrates.cli.skills.SkillsInstaller;
import nl.obren.sokrates.common.utils.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.errors.GitAPIException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/** The installSkills command. Moved out of {@link CommandLineInterface}, which keeps the option plumbing and the analyze pipeline. */
class SkillsCommands {
    private static final Log LOG = LogFactory.getLog(SkillsCommands.class);
    private final CommandLineInterface cli;
    private final Commands commands;

    SkillsCommands(CommandLineInterface cli, Commands commands) {
        this.cli = cli;
        this.commands = commands;
    }

    void installSkills(String[] args) throws ParseException, IOException {
        Options options = commands.getInstallSkillsOptions();
        CommandLine cmd = new DefaultParser().parse(options, args);
        if (cmd.hasOption(commands.getHelp().getOpt())) {
            CommandLineInterface.helpMode = true;
            commands.usage(Commands.INSTALL_SKILLS, options, Commands.INSTALL_SKILLS_DESCRIPTION);
            return;
        }
        String source = StringUtils.defaultIfBlank(cmd.getOptionValue(commands.getSource().getOpt()), SkillsInstaller.DEFAULT_SOURCE);
        String ref = StringUtils.defaultIfBlank(cmd.getOptionValue(commands.getRef().getOpt()), SkillsInstaller.DEFAULT_REF);
        File cache = cmd.hasOption(commands.getCacheFolder().getOpt()) ? new File(cmd.getOptionValue(commands.getCacheFolder().getOpt())) : SkillsInstaller.defaultCacheFolder();
        List<File> targets = skillTargets(cmd);
        SkillsInstaller installer = new SkillsInstaller();
        List<File> skills = fetchSkills(installer, source, ref, cache);
        if (skills == null || cmd.hasOption(commands.getListOnly().getOpt())) {
            return;
        }
        boolean copy = cmd.hasOption(commands.getCopy().getOpt());
        for (File target : targets) {
            List<File> installed = installer.installInto(skills, target, copy);
            LOG.info((copy ? "Copied " : "Linked ") + installed.size() + " skills into " + target.getPath());
        }
        LOG.info("Done. Ask your agent to \"use the sokrates skill\" in a repository, or run an analysis with -ai claude|codex|gemini.");
    }

    /** The -target folders, else the project's skill folders with -project, else the agents' default folders. */
    private List<File> skillTargets(CommandLine cmd) {
        List<File> targets = new ArrayList<>();
        if (cmd.hasOption(commands.getTarget().getOpt())) {
            for (String folder : cmd.getOptionValues(commands.getTarget().getOpt())) {
                targets.add(new File(folder));
            }
        } else if (cmd.hasOption(commands.getProject().getOpt())) {
            targets.addAll(SkillsInstaller.projectTargets(new File(".")));
        } else {
            targets.addAll(SkillsInstaller.defaultTargets());
        }
        return targets;
    }

    /** The skills of the fetched source, logged; null (after logging) when the fetch fails or the source has none. */
    private static List<File> fetchSkills(SkillsInstaller installer, String source, String ref, File cache) {
        File root;
        try {
            root = installer.fetch(source, ref, cache);
        } catch (GitAPIException | IOException e) {
            LOG.error("Could not fetch the skills from " + source + ": " + e.getMessage());
            return null;
        }
        List<File> skills = SkillsInstaller.findSkills(root);
        if (skills.isEmpty()) {
            LOG.error("No skills (folders with a SKILL.md under skills/) found in " + root.getPath());
            return null;
        }
        LOG.info(skills.size() + " skills in " + root.getPath() + ": " + skills.stream().map(File::getName).collect(Collectors.joining(", ")));
        return skills;
    }
}
