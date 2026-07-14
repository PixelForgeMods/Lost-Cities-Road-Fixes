package net.austizz.lostcitiesroadfixes.compat;

import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class LostCitiesMixinPlugin implements IMixinConfigPlugin {
    private static volatile CompatibilityReport loadedReport;

    @Override
    public void onLoad(String mixinPackage) {
        CompatibilityReport report = LostCitiesCompatibility.inspect();
        loadedReport = report;
        if (!report.compatible()) {
            throw new IllegalStateException(report.diagnostic());
        }
        LostCitiesRoadFixes.LOGGER.info("{}", report.diagnostic());
    }

    public static Optional<CompatibilityReport> loadedReport() {
        return Optional.ofNullable(loadedReport);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                         IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                          IMixinInfo mixinInfo) {
    }
}
