package de.prob.model.classicalb;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.model.representation.Guard;

public class ClassicalBGuard extends Guard {

	public ClassicalBGuard(final String code) throws BException {
		super(new ClassicalB(code));
	}

}
