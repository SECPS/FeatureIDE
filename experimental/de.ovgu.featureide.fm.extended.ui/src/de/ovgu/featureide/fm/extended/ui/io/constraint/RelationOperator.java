/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.extended.ui.io.constraint;

public enum RelationOperator {
	EQUAL("="),
	GREATER_EQUAL(">="),
	LESSER_EQUAL("<=");
	
	private String symbol;
	
	private RelationOperator(String symbol) {
		this.symbol = symbol;
	}
	
	@Override
	public String toString() {
		return symbol;
	}
	
	public static RelationOperator parseOperator(String string) {
		RelationOperator op = null;
		
		if (string.equals(EQUAL.toString())) {
			op = EQUAL;
		} else if (string.equals(GREATER_EQUAL.toString())) {
			op = GREATER_EQUAL;
		} else if (string.equals(LESSER_EQUAL.toString())) {
			op = LESSER_EQUAL;
		}
		
		return op;
	}
}
