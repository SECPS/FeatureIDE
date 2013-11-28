package br.ufal.ic.colligens.refactoring.tree;

import br.ufal.ic.colligens.refactoring.tree.visitor.Visitor;

public class VoidSpecifier extends Node {

	@Override
	public void accept(Visitor visitor) {
		visitor.run(this);
	}

}