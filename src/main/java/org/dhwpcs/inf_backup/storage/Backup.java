package org.dhwpcs.inf_backup.storage;

import net.minecraft.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;

public class Backup {
    public static final Logger LOGGER = LogManager.getLogger("Backup");

    public static final Path REGION_PATH = Path.of("region");
    public static final Path ENTITIES_PATH = Path.of("entities");
    public static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
    public static final Comparator<Pair<Path, BackupInfo>> COMPARATOR = Comparator.comparing(it -> it.getRight().date().getTime());
    public static final StackTraceElement[] EMPTY_STACK_TRACE = {};

    public static final String VERSION = "1.1.0";
}
