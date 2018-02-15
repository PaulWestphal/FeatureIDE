/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package org.prop4j.solvers.impl.javasmt.sat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.prop4j.Node;
import org.prop4j.solver.AbstractSatSolver;
import org.prop4j.solver.IMusExtractor;
import org.prop4j.solver.ISatProblem;
import org.prop4j.solver.ISatResult;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

import de.ovgu.featureide.fm.core.FMCorePlugin;

/**
 * Sat Solver implemented with JavaSmt to solve sat requests. The kind of solver is specified by
 *
 * @author Joshua Sprey
 */
public class JavaSmtSatSolver extends AbstractSatSolver implements IMusExtractor {

	protected Configuration config;
	protected LogManager logManager;
	protected ShutdownManager shutdownManager;
	protected SolverContext context;

	protected JavaSmtSatSolverStack pushstack;

	protected List<BooleanFormula> clauses;
	protected HashMap<BooleanFormula, Node> formulaToNode;
	protected HashMap<Node, BooleanFormula> nodeToFormula;

	protected Prop4JToJavaSmtTranslator translator;

	public static final String SOLVER_TYPE = "solver_type";

	/**
	 * @param node
	 * @param solver The solver that should be used to solve the query's
	 */
	public JavaSmtSatSolver(ISatProblem problem, Solvers solver, Map<String, Object> configuration) {
		super(problem);
		try {
			pushstack = new JavaSmtSatSolverStack();
			config = Configuration.defaultConfiguration();
			logManager = BasicLogManager.create(config);
			shutdownManager = ShutdownManager.create();
			context = SolverContextFactory.createSolverContext(config, logManager, shutdownManager.getNotifier(), solver);
			translator = new Prop4JToJavaSmtTranslator(context);
			clauses = new ArrayList<>();
			nodeToFormula = new HashMap<>();
			formulaToNode = new HashMap<>();
			for (int i = 0; i < problem.getRoot().getChildren().length; i++) {
				final Node node = problem.getRoot().getChildren()[i];
				final BooleanFormula formula = translator.getFormula(node);
				clauses.add(formula);
				nodeToFormula.put(node, formula);
				formulaToNode.put(formula, node);
			}
			setConfiguration(configuration);
		} catch (final InvalidConfigurationException e) {

		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.ISolver#isSatisfiable()
	 */
	@Override
	public ISatResult isSatisfiable() {
		try (ProverEnvironment prover = context.newProverEnvironment()) {
			final List<BooleanFormula> usedConstraint = clauses;
			if (!pushstack.isEmpty()) {
				final List<BooleanFormula> pushedNodes = pushstack.getFormulasAsList();
				usedConstraint.addAll(pushedNodes);
			}
			for (final BooleanFormula booleanFormula : usedConstraint) {
				prover.addConstraint(booleanFormula);
			}
			final boolean isSat = !prover.isUnsat();
			return isSat ? ISatResult.TRUE : ISatResult.FALSE;
		} catch (final SolverException e) {
			return ISatResult.TIMEOUT;
		} catch (final InterruptedException e) {
			return ISatResult.TIMEOUT;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.ISolver#setConfiguration(java.util.Map)
	 */
	@Override
	public List<String> setConfiguration(Map<String, Object> config) {
		if (config == null) {
			return null;
		}
		final HashSet<String> list = new HashSet<>();
		for (final String configID : config.keySet()) {
			final Object value = config.get(configID);
			if (value == null) {
				continue;
			}
			switch (configID) {
			case SOLVER_TYPE:
				try {
					if (value instanceof Solvers) {
						final Solvers solverType = (Solvers) value;
						context = SolverContextFactory.createSolverContext(this.config, logManager, shutdownManager.getNotifier(), solverType);
						list.add(SOLVER_TYPE);
					}
				} catch (final InvalidConfigurationException e) {}

				break;
			default:
				break;
			}
		}
		return new ArrayList<>(list);
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.ISolver#pop()
	 */
	@Override
	public Node pop() {
		final Node popedNode = pushstack.pop();
		FMCorePlugin.getDefault().logInfo("Popped node: " + popedNode);
		return popedNode;
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.ISolver#pop(int)
	 */
	@Override
	public List<Node> pop(int count) {
		final List<Node> nodes = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			nodes.add(pop());
		}
		return nodes;
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.ISolver#push(org.prop4j.Node)
	 */
	@Override
	public void push(Node formula) {
		final BooleanFormula formulaJavaSmt = translator.getFormula(formula);
//		FMCorePlugin.getDefault().logInfo("Pushed node: " + formula + " with the formula " + formulaJavaSmt.toString());
		pushstack.push(formula, formulaJavaSmt);
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.ISolver#push(org.prop4j.Node[])
	 */
	@Override
	public void push(Node... formulas) {
		for (final Node node : formulas) {
			push(node);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.ISolver#getSoulution()
	 */
	@Override
	public Object[] getSoulution() {
		try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
			final List<BooleanFormula> usedConstraint = clauses;
			if (!pushstack.isEmpty()) {
				final List<BooleanFormula> pushedNodes = pushstack.getFormulasAsList();
				usedConstraint.addAll(pushedNodes);
			}
			for (final BooleanFormula booleanFormula : usedConstraint) {
				prover.addConstraint(booleanFormula);
			}
			if (!prover.isUnsat()) {
				final Model model = prover.getModel();
				final Iterator<ValueAssignment> iterator = model.iterator();
				final List<Integer> solution = new ArrayList<>();
				while (iterator.hasNext()) {
					final ValueAssignment value = iterator.next();
					if (value.getValue().toString().equals("true")) {
						solution.add(getProblem().getIndexOfVariable(value.getName().toString()));
					} else {
						solution.add(-getProblem().getIndexOfVariable(value.getName().toString()));
					}
				}
				return solution.toArray();
			} else {
				return null;
			}
		} catch (final SolverException e) {
			return null;
		} catch (final InterruptedException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.ISolver#findSolution()
	 */
	@Override
	public Object[] findSolution() {
		return getSoulution();
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.IMusExtractor#getMinimalUnsatisfiableSubset()
	 */
	@Override
	public Set<Node> getMinimalUnsatisfiableSubset() throws IllegalStateException {
		try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_UNSAT_CORE)) {
			final List<BooleanFormula> usedConstraint = clauses;
			if (!pushstack.isEmpty()) {
				final List<BooleanFormula> pushedNodes = pushstack.getFormulasAsList();
				usedConstraint.addAll(pushedNodes);
			}
			for (final BooleanFormula booleanFormula : usedConstraint) {
				prover.addConstraint(booleanFormula);
			}
			if (prover.isUnsat()) {
				final List<BooleanFormula> formula = prover.getUnsatCore();
				final Set<Node> explanation = new HashSet<>();
				for (int i = 0; i < formula.size(); i++) {
					if (formulaToNode.containsKey(formula.get(i))) {
						explanation.add(formulaToNode.get(formula.get(i)));
					}
					if (pushstack.getNodeOfFormula(formula.get(i)) != null) {
						explanation.add(pushstack.getNodeOfFormula(formula.get(i)));
					}
				}
				return explanation;
			}
		} catch (final SolverException e) {
			return null;
		} catch (final InterruptedException e) {
			return null;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.IMusExtractor#getMinimalUnsatisfiableSubsetIndexes()
	 */
	@Override
	public Set<Integer> getMinimalUnsatisfiableSubsetIndexes() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.IMusExtractor#getAllMinimalUnsatisfiableSubsets()
	 */
	@Override
	public List<Set<Node>> getAllMinimalUnsatisfiableSubsets() throws IllegalStateException {
		return Collections.singletonList(getMinimalUnsatisfiableSubset());
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.solver.IMusExtractor#getAllMinimalUnsatisfiableSubsetIndexes()
	 */
	@Override
	public List<Set<Integer>> getAllMinimalUnsatisfiableSubsetIndexes() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

}
