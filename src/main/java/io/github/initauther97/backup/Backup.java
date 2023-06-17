package io.github.initauther97.backup;

import net.minecraft.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Comparator;

public class Backup {
    public static final Logger LOGGER = LogManager.getLogger("Backup");

    public static final Path REGION_PATH = Path.of("region");

    public static final Comparator<Pair<Path, BackupInfo>> COMPARATOR = Comparator.comparing(it -> it.getRight().date().getTime());
}
