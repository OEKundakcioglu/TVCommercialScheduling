package solvers.pipeline;

import solvers.heuristicSolvers.grasp.GraspInformation;
import solvers.mipSolvers.MipInformation;

public class PipelineInformation {
    private final GraspInformation graspInformation;
    private final MipInformation mipInformation;

    public PipelineInformation(GraspInformation graspInformation, MipInformation mipInformation) {
        this.graspInformation = graspInformation;
        this.mipInformation = mipInformation;
    }

    public GraspInformation graspInformation() {
        return graspInformation;
    }

    public MipInformation mipInformation() {
        return mipInformation;
    }
}
