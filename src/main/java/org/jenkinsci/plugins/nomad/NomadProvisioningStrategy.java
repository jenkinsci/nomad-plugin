
package org.jenkinsci.plugins.nomad;

import hudson.Extension;
import hudson.model.Label;
import hudson.model.LoadStatistics.LoadStatisticsSnapshot;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.NodeProvisioner.PlannedNode;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 *
 * Idea picked from yet-another-docker-pluign @kostyasha
 *
 * @author antweiss
 */
@Extension
public class NomadProvisioningStrategy extends NodeProvisioner.Strategy {
     private static final Logger LOGGER = getLogger(NomadProvisioningStrategy.class.getName());

    /**
     * Do asap provisioning
     */
    @Nonnull
    @Override
    public NodeProvisioner.StrategyDecision apply(@Nonnull NodeProvisioner.StrategyState strategyState) {
        final Label label = strategyState.getLabel();
        LoadStatisticsSnapshot snapshot = strategyState.getSnapshot();
        for ( Cloud nomadCloud : Jenkins.getActiveInstance().clouds ){
            if ( nomadCloud instanceof NomadCloud ) {

                LOGGER.log(Level.FINE, "Available executors={0} connecting executors={1} AdditionalPlannedCapacity={2} pending ={3}",
                        new Object[]{snapshot.getAvailableExecutors(), snapshot.getConnectingExecutors(), strategyState.getAdditionalPlannedCapacity(),((NomadCloud)nomadCloud).getPending() });
                int availableCapacity = snapshot.getAvailableExecutors() +
                        snapshot.getConnectingExecutors() +
                        strategyState.getAdditionalPlannedCapacity() +
                        ((NomadCloud)nomadCloud).getPending();

                int currentDemand = snapshot.getQueueLength();

                LOGGER.log(Level.FINE, "Available capacity="+availableCapacity+" currentDemand=" +currentDemand);

                if (availableCapacity < currentDemand) {
                    Collection<PlannedNode> plannedNodes = nomadCloud.provision(label, currentDemand - availableCapacity);
                    LOGGER.log(Level.FINE, "Planned "+plannedNodes.size()+" new nodes");

                    strategyState.recordPendingLaunches(plannedNodes);
                    availableCapacity += plannedNodes.size();
                    LOGGER.log(Level.FINE, "After provisioning, available capacity="+availableCapacity+" currentDemand="+ currentDemand);
                }

                if (availableCapacity >= currentDemand) {
                    LOGGER.log(Level.FINE, "Provisioning completed");
                    return NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED;
                } else {
                    LOGGER.log(Level.FINE, "Provisioning not complete, consulting remaining strategies");
                    return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
                }
            }
        }
        LOGGER.log(Level.FINE,"Provisioning not complete, consulting remaining strategies");
        return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
    }
}

