package org.jenkinsci.plugins.nomad.Api;

import org.jenkinsci.plugins.nomad.NomadConstraintTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ConstraintGroup {
    private final List<Constraint> constraints = new ArrayList<Constraint>();

    public ConstraintGroup(
            List<NomadConstraintTemplate> constraintTemplate
    ) {
        Iterator<NomadConstraintTemplate> constraintIterator = constraintTemplate.iterator();
        while (constraintIterator.hasNext()) {
            NomadConstraintTemplate nextTemplate = constraintIterator.next();
            constraints.add(new Constraint(nextTemplate));
        }
    }

    public List<Constraint> getConstraints() {
        Iterator<Constraint> constraintIterator = constraints.iterator();

        List<Constraint> Constraints = new ArrayList<Constraint>();

        while (constraintIterator.hasNext()) {
            Constraint nextConstraint = constraintIterator.next();
            Constraints.add(nextConstraint);
        }
        return Constraints;
    }
}