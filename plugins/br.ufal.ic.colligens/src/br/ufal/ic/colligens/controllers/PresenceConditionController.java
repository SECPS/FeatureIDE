package br.ufal.ic.colligens.controllers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import scala.Function3;
import tree.Node;
import tree.visitor.VisitorPrinter;
import br.ufal.ic.colligens.activator.Colligens;
import br.ufal.ic.colligens.controllers.core.PluginException;
import br.ufal.ic.colligens.models.FileProxy;
import br.ufal.ic.colligens.models.PlatformException;
import br.ufal.ic.colligens.models.RenderParserError;
import br.ufal.ic.colligens.models.StubsHeader;
import core.GeneralFrontend;
import core.RefactoringFrontend;
import core.presence.condition.PresenceConditionVisitor;
import de.fosd.typechef.error.Position;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.options.FrontendOptions;
import de.fosd.typechef.options.FrontendOptionsWithConfigFiles;
import de.fosd.typechef.options.OptionException;

public class PresenceConditionController {
	private IFile file = null;
	private final int line;
	private final String code;
	private Collection<Node> nodes, nodesCondition2;
	public static final String MARKER_TYPE = Colligens.PLUGIN_ID
			+ ".presencecondition";
	private FileProxy fileProxy = null;

	public PresenceConditionController(IFile file, int line, String code) {
		this.file = file;
		this.line = line;
		this.code = code;
	}

	public void run() throws PluginException, OptionException, IOException {
		StubsHeader stubsHeader = new StubsHeader();
		try {
			stubsHeader.setProject(file.getProject().getName());
			stubsHeader.run();
		} catch (PlatformException e) {
			e.printStackTrace();
			throw new PluginException("");
		}

		fileProxy = new FileProxy((IResource) file);

		FrontendOptions opt = new FrontendOptionsWithConfigFiles();
		ArrayList<String> paramters = new ArrayList<String>();

		paramters.add("--lexNoStdout");
		paramters.add("--parse");

		if (stubsHeader.getIncludes() != null) {
			for (Iterator<String> iterator = stubsHeader.getIncludes()
					.iterator(); iterator.hasNext();) {
				paramters.add("-h");
				paramters.add(iterator.next());
			}
		}
		paramters.add(fileProxy.getNoIncludeFile());

		String[] paramterArray = paramters
				.toArray(new String[paramters.size()]);

		opt.parseOptions(paramterArray);

		RenderParserError renderParserError = new RenderParserError();
		renderParserError.setFile(fileProxy);

		opt.setRenderParserError((Function3<FeatureExpr, String, Position, Object>) renderParserError);

		tree.Node ast = GeneralFrontend.getAST(opt);

		if (ast == null) {
			return;
		}

		nodes = new LinkedList<Node>();
		nodesCondition2 = new LinkedList<Node>();

		ast.accept(new PresenceConditionVisitor());

		this.presenceCondition(ast);

		if (!this.markerLine(nodes)) {
			this.markerLine(nodesCondition2);
		}
	}

	private void presenceCondition(Node node) {
		if (node.getPositionFrom() != null
				&& (node.getPositionFrom().getLine() >= line - 10 && node
						.getPositionFrom().getLine() <= line + 10)) {
			PrintStream out = System.out;
			node.accept(new VisitorPrinter(true));
			System.setOut(out);
			try {
				Runtime.getRuntime().exec(
						"/usr/bin/uncrustify -o "
								+ RefactoringFrontend.outputFilePath
								+ " -c default.cfg -f "
								+ RefactoringFrontend.outputFilePath);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				String codeNode = readFile(RefactoringFrontend.outputFilePath)
						.trim();
				if (codeNode.contains(code.trim())) {
					// System.out.println("Presence Condition: "
					// + node.getPresenceCondition() + COMMA__FROM_LINE
					// + line + "." + node.getPositionFrom().getLine());
					node.accept(new VisitorPrinter(false));
					// System.out.println("\n\n");
					nodes.add(node);
				} else if (Math.abs(node.getPositionFrom().getLine() - line) < 10) {
					nodesCondition2.add(node);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (Node child : node.getChildren()) {
			this.presenceCondition(child);
		}
	}

	private boolean markerLine(Collection<Node> nodes) {
		Node nodeMarke = null;
		for (Iterator<Node> iterator = nodes.iterator(); iterator.hasNext();) {
			Node node = iterator.next();
			if (nodeMarke == null
					|| (Math.abs(node.getPositionFrom().getLine() - line) <= Math
							.abs(nodeMarke.getPositionFrom().getLine() - line))) {
				nodeMarke = node;
			}
		}
		if (nodeMarke == null) {
			return false;
		}
		IMarker marker;
		try {
			marker = file.createMarker(MARKER_TYPE);

			marker.setAttribute(IMarker.MESSAGE, "Presence Condition: "
					+ nodeMarke.getPresenceCondition());
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
			marker.setAttribute(IMarker.LINE_NUMBER, line);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return true;
	}

	private String readFile(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}

	public FileProxy getFileProxy() {
		return fileProxy;
	}

}
