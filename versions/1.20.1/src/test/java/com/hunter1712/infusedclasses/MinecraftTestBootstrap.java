package com.hunter1712.infusedclasses;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * 1.20.1's {@code Registries} static init reaches into {@code BuiltInRegistries}, which
 * refuses to run unbootstrapped, and the bootstrap itself needs a declared game version
 * (its data-fixer bootstrap reads {@code SharedConstants}). Loading the registry
 * decisions in a plain JVM therefore starts here, once per test session.
 */
public class MinecraftTestBootstrap implements LauncherSessionListener {
	@Override
	public void launcherSessionOpened(LauncherSession session) {
		SharedConstants.setVersion(DetectedVersion.BUILT_IN);
		Bootstrap.bootStrap();
	}
}
