package com.hunter1712.infusedclasses.client;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The client config file on disk: {@code config/infusedclasses.json} under the Fabric config
 * dir. Read once at client startup; a missing file is written with the defaults; any field
 * that did not parse falls back to its own default with a single log line. The disk half
 * only — what the text means is {@link HudConfig#parse}.
 */
public final class HudConfigFile {
	private static final String FILE_NAME = "infusedclasses.json";
	private static final Logger LOGGER = LoggerFactory.getLogger("infusedclasses");

	private HudConfigFile() {
	}

	/** Loads the client config, creating the file with its defaults on first run. */
	public static HudConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		if (!Files.isRegularFile(path)) {
			writeDefaults(path);
			return HudConfig.defaults();
		}
		try {
			HudConfig.Parsed parsed = HudConfig.parseDetail(Files.readString(path, StandardCharsets.UTF_8));
			if (parsed.fellBack()) {
				LOGGER.warn("infusedclasses: {} has missing or invalid fields; defaults applied for them", path);
			}
			return parsed.config();
		} catch (IOException e) {
			LOGGER.warn("infusedclasses: could not read {}; using the default HUD config", path, e);
			return HudConfig.defaults();
		}
	}

	private static void writeDefaults(Path path) {
		try {
			Path dir = path.getParent();
			if (dir != null) {
				Files.createDirectories(dir);
			}
			Files.writeString(path, HudConfig.defaults().toJson(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("infusedclasses: could not write {}; using the default HUD config", path, e);
		}
	}
}
