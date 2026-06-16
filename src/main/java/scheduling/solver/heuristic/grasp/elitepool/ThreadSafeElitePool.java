package scheduling.solver.heuristic.grasp.elitepool;

import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scheduling.solver.heuristic.grasp.GraspSolution;

class ThreadSafeElitePool extends ElitePool {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    ThreadSafeElitePool(int maxSize, int numCommercials) {
        super(maxSize, numCommercials);
    }

    @Override
    public void add(GraspSolution candidate) {
        lock.writeLock().lock();
        try {
            super.add(candidate);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public GraspSolution getRandomGuide(Random random) {
        lock.readLock().lock();
        try {
            return super.getRandomGuide(random);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return super.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
