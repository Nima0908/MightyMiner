package com.jelly.MightyMinerV2.Feature;

import com.jelly.MightyMinerV2.Util.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeatureManager {
    private static FeatureManager instance;
    private final List<IFeature> features = new ArrayList<>();

    public static FeatureManager getInstance() {
        if (instance == null) {
            instance = new FeatureManager();
        }
        return instance;
    }

    public void EnableAll() {
        for (IFeature feature : features) {
            if (feature.shouldStartAtLaunch() && !feature.isEnabled()) {
                feature.start();
                LogUtil.send("Enabled Feature:" + feature.getName(), LogUtil.ELogType.DEBUG);
            }

        }
    }

    public void DisableAll() {
        for (IFeature feature : features) {
            if (feature.isEnabled()) {
                feature.stop();
                LogUtil.send("Disabled Feature:" + feature.getName(), LogUtil.ELogType.DEBUG);
            }
        }
    }

    public boolean shouldPauseMacroExecution() {
        return features.stream().anyMatch(feature -> {
            if (feature.isToggle() && feature.isRunning()) {
                return feature.shouldPauseMacroExecution();
            }
            return false;
        });
    }

    public void addFeature(IFeature feature) {
        features.add(feature);
    }

    public void removeFeature(IFeature feature) {
        features.remove(feature);
    }

    public void resetAllStates() {
        features.forEach(IFeature::resetFailsafeAfterStop);
    }

    public void disableCurrentRunning(IFeature sender) {
        features.stream().filter(IFeature::isRunning).forEach(feature -> {
            if (feature == sender) return;
            feature.stop();
            LogUtil.send("Disabled Feature:" + feature.getName(), LogUtil.ELogType.DEBUG);
        });
    }

    public void disableAllExcept(IFeature... sender) {
        features.stream().filter(IFeature::isRunning).forEach(feature -> {
            if (Arrays.asList(sender).contains(feature)) return;
            feature.stop();
            LogUtil.send("Disabled Feature:" + feature.getName(), LogUtil.ELogType.DEBUG);
        });
    }

    public List<IFeature> getCurrentRunning() {
        List<IFeature> running = new ArrayList<>();
        features.stream().filter(IFeature::isRunning).forEach(running::add);
        return running;
    }
}
