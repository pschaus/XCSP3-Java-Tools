/*
 * Copyright (c) 2016 XCSP3 Team (contact@xcsp.org)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.xcsp.checker;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.xcsp.common.Condition;
import org.xcsp.common.Condition.ConditionIntvl;
import org.xcsp.common.Condition.ConditionRel;
import org.xcsp.common.Condition.ConditionSet;
import org.xcsp.common.Condition.ConditionVal;
import org.xcsp.common.Constants;
import org.xcsp.common.Types.TypeConditionOperatorRel;
import org.xcsp.common.Types.TypeConditionOperatorSet;
import org.xcsp.common.Types.TypeCtr;
import org.xcsp.common.Types.TypeExpr;
import org.xcsp.common.Types.TypeFlag;
import org.xcsp.common.Types.TypeObjective;
import org.xcsp.common.Types.TypeOperatorRel;
import org.xcsp.common.Types.TypeRank;
import org.xcsp.common.predicates.XNode;
import org.xcsp.common.predicates.XNodeParent;
import org.xcsp.parser.XCallbacks2;
import org.xcsp.parser.entries.XConstraints.XCtr;
import org.xcsp.parser.entries.XConstraints.XSlide;
import org.xcsp.parser.entries.XVariables.XVarInteger;

/**
 * This class is used to test if XCSP3 instances are valid according to the scope of the current XCSP3 competition of constraint solvers.
 * 
 * @author Christophe Lecoutre
 */
public class CompetitionScope implements XCallbacks2 {

	// ************************************************************************
	// ***** Main (and other static stuff)
	// ************************************************************************

	private static final String INVALID = "invalid";

	public static void main(String[] args) throws Exception {
		Boolean miniTrack = (args.length > 0 && args[0].equals("-mini")) ? Boolean.TRUE : (args.length > 0 && args[0].equals("-main")) ? Boolean.FALSE : null;
		args = miniTrack != null ? Arrays.copyOfRange(args, 1, args.length) : args;
		if (args.length != 1) {
			System.out.println("Usage: " + CompetitionScope.class.getName() + " [-mini | -main]  <instanceFilename | directoryName> ");
			System.out.println("  if -mini, then only instances that are valid for the mini track are displayed");
			System.out.println("  if -main, then only instances that are valid for the main track are displayed");
			System.out.println(
					"  if neither -mini nor -main, then all instances are displayed followed by two boolean values (the first one for the main track)");
		} else
			new CompetitionScope(miniTrack, args[0]);
	}

	// ************************************************************************
	// ***** Implementation object (bridge pattern)
	// ************************************************************************

	private Implem implem = new Implem(this);

	@Override
	public Implem implem() {
		return implem;
	}

	// ************************************************************************
	// ***** Fields and Constructors
	// ************************************************************************

	/**
	 * Indicates if the current instance that is being checked must respect the rules of the mini track (true) or the main track (false).
	 */
	private boolean testMiniTrack;

	/**
	 * Indicates if only one instance is checked or a full directory
	 */
	private boolean multiMode;

	/**
	 * 
	 * @param f
	 *            an XCSP3 file/instance to be tested
	 * @param testMiniTrack
	 *            {@code true} iff the test must be performed for the mini track
	 * @return {@code true} if the instance is valid, {@code false} if the instance is not valid, and {@code null} is a crash occurs.
	 */
	private Boolean check(File f, boolean testMiniTrack) {
		assert f.isFile() && (f.getName().endsWith(".xml") || f.getName().endsWith(".lzma"));
		this.testMiniTrack = testMiniTrack;
		try {
			loadInstance(f.getAbsolutePath());
		} catch (Throwable e) {
			return e.getMessage().equals(INVALID) ? Boolean.FALSE : null;
		}
		return Boolean.TRUE;
	}

	private final String[] largeValidInstances = { "Nonogram-069-table.xml.lzma", "Nonogram-122-table.xml.lzma", "KnightTour-12-ext07.xml.lzma", "MagicSquare-6-table.xml.lzma" };
	private final String[] largeValidSeries = { "pigeonsPlus" };
	private boolean usePredefined = true; // hard coding

	private void recursiveChecking(File file, Boolean miniTrack) {
		assert file.isDirectory();
		File[] files = file.listFiles();
		Arrays.sort(files);
		for (File f : files)
			if (f.isDirectory())
				recursiveChecking(f, miniTrack);
			else if (f.getName().endsWith(".xml") || f.getName().endsWith(".lzma")) {
				boolean b = usePredefined && (Stream.of(largeValidInstances).anyMatch(s -> f.getAbsolutePath().endsWith(s))
						|| Stream.of(largeValidSeries).anyMatch(s -> f.getAbsolutePath().contains(s)));
				// System.out.println("test" + f.getAbsolutePath());
				if (miniTrack == Boolean.TRUE && (b || check(f, true) == Boolean.TRUE))
					System.out.println(f.getAbsolutePath());
				else if (miniTrack == Boolean.FALSE && (b || check(f, false) == Boolean.TRUE))
					System.out.println(f.getAbsolutePath());
				else if (miniTrack == null)
					System.out.println(f.getAbsolutePath() + "\t" + (b || check(f, false) == Boolean.TRUE) + "\t" + (b || check(f, true) == Boolean.TRUE));
			}
	}

	/**
	 * Builds an object used for checking the validity of one (or several) XCSP3 instances with respect to the scope of the current competition.
	 * 
	 * @param miniTrack
	 *            Indicates how the tests are performed: if {@code true}, only for the mini-track, if {@code false}, only for the main track, if
	 *            {@code null} for both tracks.
	 * @param name
	 *            the name of a file or directory
	 * @throws Exception
	 *             exception thrown if a problem is encountered
	 */
	public CompetitionScope(Boolean miniTrack, String name) throws Exception {
		implem().rawParameters(); // to keep initial formulations
		File file = new File(name);
		if (!file.exists())
			System.out.println("File (or directory) not found : " + name);
		else {
			multiMode = !file.isFile();
			if (file.isFile())
				loadInstance(name);
			else
				recursiveChecking(file, miniTrack);
		}
	}

	// ************************************************************************
	// ***** Redefining Callback Functions for Performing Tests
	// ************************************************************************

	@Override
	public Object unimplementedCase(Object... objects) {
		if (!multiMode) {
			System.out.println("NOT VALID (wrt the competition)");
			System.out.println("\n\n**********************");
			StackTraceElement[] t = Thread.currentThread().getStackTrace();
			System.out.println("  Method " + t[2].getMethodName());
			System.out.println("  Class " + t[2].getClassName());
			System.out.println("  Line " + t[2].getLineNumber());
			System.out.println("**********************");
			System.out.println(Stream.of(objects).filter(o -> o != null).map(o -> o.toString()).collect(Collectors.joining("\n")));
		}
		throw new RuntimeException(INVALID);
	}

	private void unimplementedCaseIf(boolean test, Object... objects) {
		if (test)
			unimplementedCase(objects);
	}

	@Override
	public void buildVarInteger(XVarInteger x, int minValue, int maxValue) {
		unimplementedCaseIf(minValue < Constants.MIN_SAFE_INT || maxValue > Constants.MAX_SAFE_INT, x.id); // includes -/+ infinity
	}

	@Override
	public void buildVarInteger(XVarInteger x, int[] values) {}

	private boolean basicOperandsForMini(XNode<XVarInteger>[] sons) {
		assert sons.length == 2;
		return (sons[0].type == TypeExpr.VAR && sons[1].type == TypeExpr.LONG) || (sons[0].type == TypeExpr.LONG && sons[1].type == TypeExpr.VAR)
				|| (sons[0].type == TypeExpr.VAR && sons[1].type == TypeExpr.VAR);
	}

	private boolean complexOperandForMini(XNode<XVarInteger> node) {
		return node.type.isArithmeticOperator() && node.type != TypeExpr.POW && basicOperandsForMini(((XNodeParent<XVarInteger>) node).sons);
	}

	private boolean checkIntensionForMini(XNodeParent<XVarInteger> tree) {
		return tree.type.isRelationalOperator() && tree.sons.length == 2
				&& (basicOperandsForMini(tree.sons) || (complexOperandForMini(tree.sons[0]) && tree.sons[1].type == TypeExpr.VAR)
						|| (complexOperandForMini(tree.sons[1]) && tree.sons[0].type == TypeExpr.VAR));
	}

	@Override
	public void buildCtrIntension(String id, XVarInteger[] scope, XNodeParent<XVarInteger> tree) {
		unimplementedCaseIf(testMiniTrack && !checkIntensionForMini(tree), id);
	}

	@Override
	public void buildCtrExtension(String id, XVarInteger x, int[] values, boolean positive, Set<TypeFlag> flags) {
		unimplementedCaseIf(values.length == 0 || flags.contains(TypeFlag.STARRED_TUPLES), id); // * is irrelevant in an unary table
	}

	@Override
	public void buildCtrExtension(String id, XVarInteger[] list, int[][] tuples, boolean positive, Set<TypeFlag> flags) {
		unimplementedCaseIf(tuples.length == 0 || (testMiniTrack && !positive && flags.contains(TypeFlag.STARRED_TUPLES)), id);
	}

	@Override
	public void buildCtrRegular(String id, XVarInteger[] list, Object[][] transitions, String startState, String[] finalStates) {
		unimplementedCaseIf(testMiniTrack, id);
		// determinism should be tested (TODO), but for the moment, all automatas from available instances are deterministic
	}

	@Override
	public void buildCtrMDD(String id, XVarInteger[] list, Object[][] transitions) {
		unimplementedCaseIf(testMiniTrack, id);
		// restrictions as given in the call 2017 should be tested (TODO), but for the moment all available instances respect them
	}

	@Override
	public void buildCtrAllDifferent(String id, XVarInteger[] list) {}

	@Override
	public void buildCtrAllDifferentExcept(String id, XVarInteger[] list, int[] except) {
		unimplementedCaseIf(testMiniTrack || except.length != 1, id);
	}

	@Override
	public void buildCtrAllDifferentList(String id, XVarInteger[][] lists) {
		unimplementedCase(id); // should we accept it for the standard track in future competitions?
	}

	@Override
	public void buildCtrAllDifferentMatrix(String id, XVarInteger[][] matrix) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrAllDifferent(String id, XNodeParent<XVarInteger>[] trees) {
		assert trees != null && trees.length > 0 && Stream.of(trees).anyMatch(t -> t == null) : "bad formed trees";
		unimplementedCaseIf(testMiniTrack || Stream.of(trees).anyMatch(t -> t.type == TypeExpr.VAR), id); // either variables or only non trivial
																											// trees
	}

	@Override
	public void buildCtrAllEqual(String id, XVarInteger[] list) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrOrdered(String id, XVarInteger[] list, TypeOperatorRel operator) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrOrdered(String id, XVarInteger[] list, int[] lengths, TypeOperatorRel operator) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrLex(String id, XVarInteger[][] lists, TypeOperatorRel operator) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrLexMatrix(String id, XVarInteger[][] matrix, TypeOperatorRel operator) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	private void checkCondition(String id, Condition condition) {
		if (condition instanceof ConditionSet)
			unimplementedCaseIf(testMiniTrack || !(condition instanceof ConditionIntvl) || ((ConditionIntvl) condition).operator != TypeConditionOperatorSet.IN,
					id);
	}

	@Override
	public void buildCtrSum(String id, XVarInteger[] list, Condition condition) {
		checkCondition(id, condition);
	}

	@Override
	public void buildCtrSum(String id, XVarInteger[] list, int[] coeffs, Condition condition) {
		checkCondition(id, condition);
	}

	@Override
	public void buildCtrSum(String id, XVarInteger[] list, XVarInteger[] coeffs, Condition condition) {
		checkCondition(id, condition);
	}

	@Override
	public void buildCtrSum(String id, XNodeParent<XVarInteger>[] trees, int[] coeffs, Condition condition) {
		assert trees != null && trees.length > 0 && Stream.of(trees).anyMatch(t -> t == null) : "bad formed trees";
		unimplementedCaseIf(testMiniTrack || Stream.of(trees).anyMatch(t -> t.type == TypeExpr.VAR), id); // either variables or only non trivial
																											// trees
		checkCondition(id, condition);
	}

	@Override
	public void buildCtrCount(String id, XVarInteger[] list, int[] values, Condition condition) {
		unimplementedCaseIf(testMiniTrack, id);
		checkCondition(id, condition);
	}

	@Override
	public void buildCtrCount(String id, XVarInteger[] list, XVarInteger[] values, Condition condition) {
		unimplementedCase(id); // values cannot be variables for the competition
	}

	@Override
	public void buildCtrNValues(String id, XVarInteger[] list, Condition condition) {
		unimplementedCaseIf(testMiniTrack || condition instanceof ConditionSet, id);
		TypeConditionOperatorRel op = ((ConditionRel) condition).operator;
		boolean notAllEqual = op == TypeConditionOperatorRel.GT && condition instanceof ConditionVal && ((ConditionVal) condition).k == 1;
		unimplementedCaseIf(op != TypeConditionOperatorRel.EQ && !notAllEqual, id);
	}

	@Override
	public void buildCtrNValuesExcept(String id, XVarInteger[] list, int[] except, Condition condition) {
		unimplementedCaseIf(testMiniTrack || condition instanceof ConditionSet || except.length != 1, id);
		unimplementedCaseIf(((ConditionRel) condition).operator != TypeConditionOperatorRel.EQ, id);
	}

	@Override
	public void buildCtrCardinality(String id, XVarInteger[] list, boolean closed, int[] values, XVarInteger[] occurs) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrCardinality(String id, XVarInteger[] list, boolean closed, int[] values, int[] occurs) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrCardinality(String id, XVarInteger[] list, boolean closed, int[] values, int[] occursMin, int[] occursMax) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrCardinality(String id, XVarInteger[] list, boolean closed, XVarInteger[] values, XVarInteger[] occurs) {
		unimplementedCase(id); // values cannot be variables for the competition
	}

	@Override
	public void buildCtrCardinality(String id, XVarInteger[] list, boolean closed, XVarInteger[] values, int[] occurs) {
		unimplementedCase(id); // values cannot be variables for the competition
	}

	@Override
	public void buildCtrCardinality(String id, XVarInteger[] list, boolean closed, XVarInteger[] values, int[] occursMin, int[] occursMax) {
		unimplementedCase(id); // values cannot be variables for the competition
	}

	@Override
	public void buildCtrMaximum(String id, XVarInteger[] list, Condition condition) {
		unimplementedCaseIf(testMiniTrack || condition instanceof ConditionSet, id);
		unimplementedCaseIf(((ConditionRel) condition).operator != TypeConditionOperatorRel.EQ, id);
	}

	@Override
	public void buildCtrMaximum(String id, XVarInteger[] list, int startIndex, XVarInteger index, TypeRank rank, Condition condition) {
		unimplementedCase(id); // variant, with index, not accepted for the competition
	}

	@Override
	public void buildCtrMinimum(String id, XVarInteger[] list, Condition condition) {
		unimplementedCaseIf(testMiniTrack || condition instanceof ConditionSet, id);
		unimplementedCaseIf(((ConditionRel) condition).operator != TypeConditionOperatorRel.EQ, id);
	}

	@Override
	public void buildCtrMinimum(String id, XVarInteger[] list, int startIndex, XVarInteger index, TypeRank rank, Condition condition) {
		unimplementedCase(id); // variant, with index, not accepted for the competition
	}

	@Override
	public void buildCtrElement(String id, XVarInteger[] list, XVarInteger value) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrElement(String id, XVarInteger[] list, int value) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, TypeRank rank, XVarInteger value) {
		unimplementedCaseIf(startIndex != 0 || rank != TypeRank.ANY, id);

	}

	@Override
	public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, TypeRank rank, int value) {
		unimplementedCaseIf(startIndex != 0 || rank != TypeRank.ANY, id);
	}

	@Override
	public void buildCtrElement(String id, int[] list, int startIndex, XVarInteger index, TypeRank rank, XVarInteger value) {
		unimplementedCaseIf(startIndex != 0 || rank != TypeRank.ANY, id); // now, this new variant is accepted for the competition
	}

	@Override
	public void buildCtrChannel(String id, XVarInteger[] list, int startIndex) {
		unimplementedCaseIf(testMiniTrack || startIndex != 0, id);
	}

	@Override
	public void buildCtrChannel(String id, XVarInteger[] list1, int startIndex1, XVarInteger[] list2, int startIndex2) {
		unimplementedCaseIf(testMiniTrack || startIndex1 != 0 || startIndex2 != 0, id);
	}

	@Override
	public void buildCtrChannel(String id, XVarInteger[] list, int startIndex, XVarInteger value) {
		unimplementedCaseIf(testMiniTrack || startIndex != 0, id);
	}

	@Override
	public void buildCtrNoOverlap(String id, XVarInteger[] origins, int[] lengths, boolean zeroIgnored) {
		unimplementedCaseIf(testMiniTrack || IntStream.of(lengths).anyMatch(v -> v == 0), id);
	}

	@Override
	public void buildCtrNoOverlap(String id, XVarInteger[] origins, XVarInteger[] lengths, boolean zeroIgnored) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrNoOverlap(String id, XVarInteger[][] origins, int[][] lengths, boolean zeroIgnored) {
		unimplementedCaseIf(testMiniTrack || Stream.of(lengths).anyMatch(t -> IntStream.of(t).anyMatch(v -> v == 0)), id);
	}

	@Override
	public void buildCtrNoOverlap(String id, XVarInteger[][] origins, XVarInteger[][] lengths, boolean zeroIgnored) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrCumulative(String id, XVarInteger[] origins, int[] lengths, int[] heights, Condition condition) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrCumulative(String id, XVarInteger[] origins, int[] lengths, XVarInteger[] heights, Condition condition) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrCumulative(String id, XVarInteger[] origins, XVarInteger[] lengths, int[] heights, Condition condition) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrCumulative(String id, XVarInteger[] origins, XVarInteger[] lengths, XVarInteger[] heights, Condition condition) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrCumulative(String id, XVarInteger[] origins, int[] lengths, XVarInteger[] ends, int[] heights, Condition condition) {
		unimplementedCase(id); // ends not accepted for the competition
	}

	@Override
	public void buildCtrCumulative(String id, XVarInteger[] origins, int[] lengths, XVarInteger[] ends, XVarInteger[] heights, Condition condition) {
		unimplementedCase(id); // ends not accepted for the competition
	}

	@Override
	public void buildCtrCumulative(String id, XVarInteger[] origins, XVarInteger[] lengths, XVarInteger[] ends, int[] heights, Condition condition) {
		unimplementedCase(id); // ends not accepted for the competition
	}

	@Override
	public void buildCtrCumulative(String id, XVarInteger[] origins, XVarInteger[] lengths, XVarInteger[] ends, XVarInteger[] heights, Condition condition) {
		unimplementedCase(id); // ends not accepted for the competition
	}

	@Override
	public void buildCtrInstantiation(String id, XVarInteger[] list, int[] values) {
		unimplementedCaseIf(testMiniTrack, id);
	}

	@Override
	public void buildCtrCircuit(String id, XVarInteger[] list, int startIndex) {
		unimplementedCaseIf(testMiniTrack || startIndex != 0, id);
	}

	@Override
	public void buildCtrCircuit(String id, XVarInteger[] list, int startIndex, int size) {
		unimplementedCase(id); // size not accepted for the competition
	}

	@Override
	public void buildCtrCircuit(String id, XVarInteger[] list, int startIndex, XVarInteger size) {
		unimplementedCase(id); // size not accepted for the competition
	}

	@Override
	public void beginSlide(XSlide s) {
		boolean simpleSlide = s.template instanceof XCtr && (((XCtr) s.template).type == TypeCtr.intension || ((XCtr) s.template).type == TypeCtr.extension);
		unimplementedCaseIf(testMiniTrack || s.lists.length != 1 || !simpleSlide, s);
	}

	@Override
	public void buildObjToMinimize(String id, XVarInteger x) {}

	@Override
	public void buildObjToMaximize(String id, XVarInteger x) {}

	@Override
	public void buildObjToMinimize(String id, XNodeParent<XVarInteger> tree) {
		unimplementedCase(id); // not accepted for the competition
	}

	@Override
	public void buildObjToMaximize(String id, XNodeParent<XVarInteger> tree) {
		unimplementedCase(id); // not accepted for the competition
	}

	@Override
	public void buildObjToMinimize(String id, TypeObjective type, XVarInteger[] list) {
		unimplementedCaseIf(type == TypeObjective.PRODUCT || type == TypeObjective.LEX, id);
	}

	@Override
	public void buildObjToMaximize(String id, TypeObjective type, XVarInteger[] list) {
		unimplementedCaseIf(type == TypeObjective.PRODUCT || type == TypeObjective.LEX, id);
	}

	@Override
	public void buildObjToMinimize(String id, TypeObjective type, XVarInteger[] list, int[] coeffs) {
		unimplementedCaseIf(type == TypeObjective.PRODUCT || type == TypeObjective.LEX, id);
	}

	@Override
	public void buildObjToMaximize(String id, TypeObjective type, XVarInteger[] list, int[] coeffs) {
		unimplementedCaseIf(type == TypeObjective.PRODUCT || type == TypeObjective.LEX, id);
	}
}
