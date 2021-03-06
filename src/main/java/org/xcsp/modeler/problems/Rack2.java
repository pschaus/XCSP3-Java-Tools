/**
 * AbsCon - Copyright (c) 2017, CRIL-CNRS - lecoutre@cril.fr
 * 
 * All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the CONTRAT DE LICENCE DE LOGICIEL LIBRE CeCILL which accompanies this
 * distribution, and is available at http://www.cecill.info
 */
package org.xcsp.modeler.problems;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.xcsp.common.IVar.Var;
import org.xcsp.modeler.ProblemAPI;

//java abscon.Resolution problems.acad.Rack2 -data=/home/lecoutre/instances/Rack/r2b.json -ev -f=cop => 1100 in 40s
public class Rack2 implements ProblemAPI {
	int nRacks;
	RackModel[] rackModels;
	CardType[] cardTypes;

	public class RackModel {
		public int power, nConnectors, price;

		public RackModel(int power, int nConnectors, int price) {
			this.power = power;
			this.nConnectors = nConnectors;
			this.price = price;
		}
	}

	public class CardType {
		public int power, demand;

		public CardType(int power, int demand) {
			this.power = power;
			this.demand = demand;
		}
	}

	@Override
	public void model() {
		rackModels = addObject(rackModels, new RackModel(0, 0, 0), 0); // we add first a dummy model (0,0,0)
		int nModels = rackModels.length, nTypes = cardTypes.length;
		int[] powers = Stream.of(rackModels).mapToInt(r -> r.power).toArray();
		int[] connectors = Stream.of(rackModels).mapToInt(r -> r.nConnectors).toArray();
		int[] prices = Stream.of(rackModels).mapToInt(r -> r.price).toArray();
		int[] cardPowers = Stream.of(cardTypes).mapToInt(r -> r.power).toArray();
		int maxCapacity = IntStream.of(connectors).max().getAsInt();

		Var[] r = array("r", size(nRacks), dom(range(nModels)), "r[i] is the model used for the ith rack");
		Var[][] c = array("c", size(nRacks, nTypes), (i, j) -> dom(range(0, Math.min(maxCapacity, cardTypes[j].demand))),
				"c[i][j] is the number of cards of type j put in the ith rack");
		Var[] rpw = array("rpw", size(nRacks), dom(powers), "rpw[i] is the power of the ith rack");
		Var[] rcn = array("rcn", size(nRacks), dom(connectors), "rcn[i] is the number of connectors of the ith rack");
		Var[] rpr = array("rpr", size(nRacks), dom(prices), "rpr[i] is the price of the ith rack");

		forall(range(nRacks), i -> extension(vars(r[i], rpw[i]), number(powers))).note("linking the ith rack with its power");
		forall(range(nRacks), i -> extension(vars(r[i], rcn[i]), number(connectors))).note("linking the ith rack with its number of connectors");
		forall(range(nRacks), i -> extension(vars(r[i], rpr[i]), number(prices))).note("linking the ith rack with its price");

		forall(range(nRacks), i -> sum(c[i], LE, rcn[i])).note("connector-capacity constraints");
		forall(range(nRacks), i -> sum(c[i], cardPowers, LE, rpw[i])).note("power-capacity constraints");
		forall(range(nTypes), i -> sum(columnOf(c, i), EQ, cardTypes[i].demand)).note("demand constraints");

		block(() -> {
			decreasing(r);
			intension(or(ne(r[0], r[1]), ge(c[0][0], c[1][0])));
		}).tag(SYMMETRY_BREAKING);

		minimize(SUM, rpr);
	}
}
