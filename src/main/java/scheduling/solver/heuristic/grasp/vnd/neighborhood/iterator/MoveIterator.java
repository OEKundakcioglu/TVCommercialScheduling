package scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator;

import java.util.Iterator;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import scheduling.solver.heuristic.grasp.move.Move;

@RequiredArgsConstructor
public class MoveIterator implements Iterator<Move> {

    private final NestedRandomIterator tuples;
    private final Function<int[], Move> moveFactory;

    @Override
    public boolean hasNext() {
        return tuples.hasNext();
    }

    @Override
    public Move next() {
        return moveFactory.apply(tuples.next());
    }
}
