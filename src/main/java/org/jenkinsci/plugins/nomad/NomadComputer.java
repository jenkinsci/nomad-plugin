package org.jenkinsci.plugins.nomad;

import hudson.model.Executor;
import hudson.model.Queue;
import hudson.slaves.AbstractCloudComputer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NomadComputer extends AbstractCloudComputer<NomadSlave> {

    private static final Logger LOGGER = Logger.getLogger(NomadComputer.class.getName());

    private final Boolean reusable;

    public NomadComputer(NomadSlave slave) {
        super(slave);

        this.reusable = slave.getReusable();
    }

    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        super.taskAccepted(executor, task);
        if (!reusable) {
            setAcceptingTasks(false);
        }
        LOGGER.log(Level.INFO, " Computer " + this + ": task accepted");
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        super.taskCompleted(executor, task, durationMS);
        LOGGER.log(Level.INFO, " Computer " + this + ": task completed");
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        super.taskCompletedWithProblems(executor, task, durationMS, problems);
        LOGGER.log(Level.WARNING, " Computer " + this + " task completed with problems");
    }

    @Override
    public String toString() {
        return String.format("%s (slave: %s)", getName(), getNode());
    }

}
