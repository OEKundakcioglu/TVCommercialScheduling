package scheduling.solver.mip.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MipCheckpointRecorderTest {

    @Test
    void recordsFirstIncumbent() {
        var recorder = new MipCheckpointRecorder();

        recorder.record(100.0, 0.5);

        var checkPoints = recorder.snapshot();
        assertEquals(1, checkPoints.size());
        assertEquals(100.0, checkPoints.getFirst().getObjective(), 1e-10);
        assertEquals(0.5, checkPoints.getFirst().getTime(), 1e-10);
    }

    @Test
    void recordsStrictlyImprovingIncumbentsInOrder() {
        var recorder = new MipCheckpointRecorder();

        recorder.record(100.0, 0.5);
        recorder.record(125.0, 1.0);
        recorder.record(150.0, 2.0);

        var checkPoints = recorder.snapshot();
        assertEquals(3, checkPoints.size());
        assertEquals(100.0, checkPoints.get(0).getObjective(), 1e-10);
        assertEquals(125.0, checkPoints.get(1).getObjective(), 1e-10);
        assertEquals(150.0, checkPoints.get(2).getObjective(), 1e-10);
        assertEquals(0.5, checkPoints.get(0).getTime(), 1e-10);
        assertEquals(1.0, checkPoints.get(1).getTime(), 1e-10);
        assertEquals(2.0, checkPoints.get(2).getTime(), 1e-10);
    }

    @Test
    void ignoresEqualWorseAndNearDuplicateObjectives() {
        var recorder = new MipCheckpointRecorder();

        recorder.record(100.0, 0.5);
        recorder.record(100.0, 1.0);
        recorder.record(99.0, 1.5);
        recorder.record(100.0 + 1e-7, 2.0);
        recorder.record(100.0 + 2e-6, 2.5);

        var checkPoints = recorder.snapshot();
        assertEquals(2, checkPoints.size());
        assertEquals(100.0, checkPoints.getFirst().getObjective(), 1e-10);
        assertEquals(100.0 + 2e-6, checkPoints.get(1).getObjective(), 1e-10);
        assertEquals(2.5, checkPoints.get(1).getTime(), 1e-10);
    }
}
