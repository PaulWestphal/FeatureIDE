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
package org.prop4j.analyses.impl.general;

import java.util.ArrayList;
import java.util.List;

import org.prop4j.analyses.GeneralSolverAnalysis;
import org.prop4j.solver.ContradictionException;
import org.prop4j.solver.ISolver;
import org.prop4j.solver.impl.SolverUtils;
import org.prop4j.solverOld.ISatSolver;

import de.ovgu.featureide.fm.core.base.util.RingList;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Finds false optional features.
 *
 * @author Sebastian Krieter
 * @author Joshua Sprey
 */
public class ImplicationAnalysis extends GeneralSolverAnalysis<List<int[]>> {

	private List<int[]> pairs;

	public ImplicationAnalysis(ISolver solver, List<int[]> pairs) {
		super(solver);
		this.pairs = pairs;
	}

	public ImplicationAnalysis(ISolver solver) {
		super(solver);
	}

	public void initParis(List<int[]> pairs) {
		this.pairs = pairs;
	}

	@Override
	public List<int[]> analyze(IMonitor monitor) {
		final List<int[]> resultList = new ArrayList<>();

		if (pairs == null) {
			return resultList;
		}

		final RingList<int[]> solutionList = new RingList<>(Math.min(pairs.size(), ISatSolver.MAX_SOLUTION_BUFFER));
		monitor.checkCancel();
		final int[] model1 = SolverUtils.getIntModel(solver.findSolution());

		if (model1 != null) {
			solutionList.add(model1);

			pairLoop: for (final int[] pair : pairs) {
				monitor.checkCancel();
				solutionLoop: for (final int[] is : solutionList) {
					// get assumed fa feature and check if it appears in every solution
					if (isInSolution(is, pair[1])) {
						continue solutionLoop;
					}
					continue pairLoop;
				}
				for (final int i : pair) {
					try {
						solver.push(getLiteralFromIndex(-i));
					} catch (final ContradictionException e) {
						// Is unsatisfiable => false optional
						resultList.add(pair);
					}
				}
				switch (solver.isSatisfiable()) {
				case FALSE:
					resultList.add(pair);
					break;
				case TIMEOUT:
					break;
				case TRUE:
					solutionList.add(SolverUtils.getIntModel(solver.getSoulution()));
					break;
				}
				for (int i = 0; i < pair.length; i++) {
					solver.pop();
				}
			}
		}
		return resultList;
	}

	private static int countNegative(int[] model) {
		int count = 0;
		for (int i = 0; i < model.length; i++) {
			count += model[i] >>> (Integer.SIZE - 1);
		}
		return count;
	}

	private boolean isInSolution(int[] solution, int value) {
		for (final int i : solution) {
			if (i == value) {
				return true;
			}
		}
		return false;
	}
}