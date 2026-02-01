package solvers.pipeline;

import runParameters.GraspSettings;
import runParameters.MipRunSettings;

public class PipelineConfig {
    private final GraspSettings graspSettings;
    private final MipRunSettings mipRunSettings;

    public PipelineConfig(GraspSettings graspSettings, MipRunSettings mipRunSettings) {
        this.graspSettings = graspSettings;
        this.mipRunSettings = mipRunSettings;
    }

    public GraspSettings graspSettings() {
        return graspSettings;
    }

    public MipRunSettings mipRunSettings() {
        return mipRunSettings;
    }
}
