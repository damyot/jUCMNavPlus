package seg.jUCMNav.importexport.html;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.EList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import seg.jUCMNav.Messages;
import seg.jUCMNav.importexport.utils.EscapeUtils;
import seg.jUCMNav.views.wizards.importexport.ExportWizard;
import ucm.map.UCMmap;
import ucm.map.impl.PluginBindingImpl;
import ucm.map.impl.StubImpl;
import urncore.IURNDiagram;

/**
 * The XML parser used to parse the XML menu file and add new menus to the file
 * 
 * @author pchen
 * 
 */
public class HTMLMenuParser {
	private static HTMLMenuParser parser = null;

	private String reportRoot = ""; //$NON-NLS-1$  // dir that holds index.html (and pages/)
	private Document xmlDocument = null;
	private ArrayList<Element> selectedMaps = new ArrayList<Element>();

	private final static String BRANCH = "branch"; //$NON-NLS-1$
	private final static String BRANCH_ID = "id"; //$NON-NLS-1$
	private final static String BRANCH_LINK = "branchLink"; //$NON-NLS-1$
	private final static String BRANCH_TEXT = "branchText"; //$NON-NLS-1$

	private final static String LEAF = "leaf"; //$NON-NLS-1$
	private final static String LEAF_TEXT = "leafText"; //$NON-NLS-1$
	private final static String LINK = "link"; //$NON-NLS-1$
	private final static String BASE_X = "baseX"; //$NON-NLS-1$
	private final static String BASE_Y = "baseY"; //$NON-NLS-1$

	/**
	 * Initialize HTMLMenuParser
	 *
	 * @param _xmlPath the directory that will hold the generated index.html
	 *                 (the report root; HTMLReport.PAGES_LOCATION sits underneath)
	 */
	private HTMLMenuParser(String _xmlPath) {
		this.reportRoot = _xmlPath;
	}

	public static HTMLMenuParser getParser(String _xmlPath) {
		if (parser == null) {
			parser = new HTMLMenuParser(_xmlPath);
		} else {
			parser.reportRoot = _xmlPath;
		}
		return parser;
	}

	/**
	 * Reset the XML document.
	 * 
	 */
	public void resetDocument() {
		xmlDocument = null;
		selectedMaps = new ArrayList<Element>();
	}

	/**
	 * Bootstrap the in-memory menu DOM. Previously this parsed
	 * htmltemplates/tree.xml from the classpath and copied it to disk; the
	 * resulting tree.xml was later rendered by an XSLT in the browser. That
	 * pipeline is being removed (XSLTProcessor is deprecated in modern
	 * browsers, and the static file would no longer render). The DOM model
	 * itself is still useful for organizing the menu hierarchy; we keep it,
	 * but build it from scratch instead of from a template, and emit HTML
	 * directly from writeToFile().
	 */
	public void parseMenu() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			xmlDocument = builder.newDocument();

			Element tree = xmlDocument.createElement("tree"); //$NON-NLS-1$
			xmlDocument.appendChild(tree);

			// Top-level diagram-type branches (populated later by addMenu)
			tree.appendChild(emptyBranch(HTMLMenuItem.TYPE_UCM));
			tree.appendChild(emptyBranch(HTMLMenuItem.TYPE_GRL));
			tree.appendChild(emptyBranch(HTMLMenuItem.TYPE_FM));

			// Definitions branch, pre-populated with the five standard pages.
			// organizeMenus() replaces the leaf text with localized strings;
			// keep these untranslated keys so that match still works.
			Element def = xmlDocument.createElement(BRANCH);
			def.setAttribute(BRANCH_ID, "DEF"); //$NON-NLS-1$
			def.setAttribute(BRANCH_LINK, "notRedirect"); //$NON-NLS-1$
			def.setAttribute(BASE_X, "0"); //$NON-NLS-1$
			def.setAttribute(BASE_Y, "0"); //$NON-NLS-1$
			Element defText = xmlDocument.createElement(BRANCH_TEXT);
			defText.setTextContent("Definitions"); //$NON-NLS-1$
			def.appendChild(defText);
			def.appendChild(buildLeaf(HTMLMenuItem.TYPE_UCM_DEF,  "UCM_Definitions.html", "215", "2"));    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			def.appendChild(buildLeaf(HTMLMenuItem.TYPE_GRL_DEF,  "GRL_Definitions.html", "215", "2"));    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			def.appendChild(buildLeaf(HTMLMenuItem.TYPE_FM_DEF,   "FM_Definitions.html",  "215", "2"));    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			def.appendChild(buildLeaf(HTMLMenuItem.TYPE_UCM_SCEN, "UCM_Scenarios.html",   "215", "2"));    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			def.appendChild(buildLeaf("Title Page",               "main.html",            "215", "2"));    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			tree.appendChild(def);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	private Element emptyBranch(String id) {
		Element b = xmlDocument.createElement(BRANCH);
		b.setAttribute(BRANCH_ID, id);
		b.setAttribute(BRANCH_LINK, "notRedirect"); //$NON-NLS-1$
		b.setAttribute(BASE_X, "0"); //$NON-NLS-1$
		b.setAttribute(BASE_Y, "0"); //$NON-NLS-1$
		Element bt = xmlDocument.createElement(BRANCH_TEXT);
		bt.setTextContent(id);
		b.appendChild(bt);
		return b;
	}

	private Element buildLeaf(String text, String link, String bx, String by) {
		Element leaf = xmlDocument.createElement(LEAF);
		Element lt = xmlDocument.createElement(LEAF_TEXT); lt.setTextContent(text);
		Element lk = xmlDocument.createElement(LINK);      lk.setTextContent(link);
		Element x  = xmlDocument.createElement(BASE_X);    x.setTextContent(bx);
		Element y  = xmlDocument.createElement(BASE_Y);    y.setTextContent(by);
		leaf.appendChild(lt); leaf.appendChild(lk); leaf.appendChild(x); leaf.appendChild(y);
		return leaf;
	}

	/**
	 * Add new menu into the XML menu file
	 * 
	 * @param htmlMenuItem
	 */
	public void addMenu(HTMLMenuItem htmlMenuItem) {
		
		if (xmlDocument == null) {
			parseMenu();
		}

		NodeList branchList = xmlDocument.getDocumentElement().getChildNodes();
		int len = branchList.getLength();
		
		Element branch = null;
		for (int i = 0; i < len; i++) {
			Node childNode = branchList.item(i);
			
				if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				branch = (Element) childNode;

				String bid = branch.getAttribute(BRANCH_ID);
				if (bid.equals(htmlMenuItem.getType())) {
					break;
				}
			}
		}
		
		
		if (branch != null) {
			
			if (htmlMenuItem.getType().equals(HTMLMenuItem.TYPE_GRL)) {
				Element leaf = xmlDocument.createElement(LEAF);

				Element leafText = xmlDocument.createElement(LEAF_TEXT);
				leafText.setTextContent(htmlMenuItem.getLeafText());

				Element link = xmlDocument.createElement(LINK);
				link.setTextContent(htmlMenuItem.getLink());

				Element baseX = xmlDocument.createElement(BASE_X);
				baseX.setTextContent(String.valueOf(htmlMenuItem.getBaseX()));

				Element baseY = xmlDocument.createElement(BASE_Y);
				baseY.setTextContent(String.valueOf(htmlMenuItem.getBaseY()));

				leaf.appendChild(leafText);
				leaf.appendChild(link);
				leaf.appendChild(baseX);
				leaf.appendChild(baseY);
				branch.appendChild(leaf);
			} else if (htmlMenuItem.getType().equals(HTMLMenuItem.TYPE_FM)){
				//--------------------
				
				Element leaf = xmlDocument.createElement(LEAF);

				Element leafText = xmlDocument.createElement(LEAF_TEXT);
				leafText.setTextContent(htmlMenuItem.getLeafText());

				Element link = xmlDocument.createElement(LINK);
				link.setTextContent(htmlMenuItem.getLink());

				Element baseX = xmlDocument.createElement(BASE_X);
				baseX.setTextContent(String.valueOf(htmlMenuItem.getBaseX()));

				Element baseY = xmlDocument.createElement(BASE_Y);
				baseY.setTextContent(String.valueOf(htmlMenuItem.getBaseY()));

				leaf.appendChild(leafText);
				leaf.appendChild(link);
				leaf.appendChild(baseX);
				leaf.appendChild(baseY);
				branch.appendChild(leaf);
				//---------------------------
			} else if (htmlMenuItem.getType().equals(HTMLMenuItem.TYPE_UCM)) {
				addUCMMenu(htmlMenuItem, branch);
			}
		}
	}

	
	/**
	 * Add new ucm menu to the given node's children
	 * 
	 * @param htmlMenuItem
	 * @param parent
	 */
	private void addUCMMenu(HTMLMenuItem htmlMenuItem, Element parent) {
		NodeList branchList = parent.getElementsByTagName(BRANCH);
		NodeList leafList = parent.getElementsByTagName(LEAF);

		boolean isExisting = false;
		int len = branchList.getLength();

		for (int i = 0; i < len; i++) {
			Element element = (Element) branchList.item(i);
			String branchLink = element.getAttribute(BRANCH_LINK);

			if (htmlMenuItem.getLink().equals(branchLink)) {
				element.setAttribute(BASE_X, String.valueOf(htmlMenuItem.getBaseX()));
				element.setAttribute(BASE_Y, String.valueOf(htmlMenuItem.getBaseY()));
				isExisting = true;
				continue;
			}
		}

		if (!isExisting) {
			len = leafList.getLength();
			for (int i = 0; i < len; i++) {
				Element element = (Element) leafList.item(i);
				String link = element.getElementsByTagName(LINK).item(0).getTextContent();

				if (htmlMenuItem.getLink().equals(link)) {
					element.getElementsByTagName(BASE_X).item(0).setTextContent(String.valueOf(htmlMenuItem.getBaseX()));
					element.getElementsByTagName(BASE_Y).item(0).setTextContent(String.valueOf(htmlMenuItem.getBaseY()));
					isExisting = true;
					continue;
				}
			}
		}

		if (!isExisting) {
			IURNDiagram diagram = htmlMenuItem.getDiagram();
			Iterator nodeIter = null;
			ArrayList<UCMmap> childDiagramList = new ArrayList<UCMmap>();

			EList nodes = diagram.getNodes();
			if (!nodes.isEmpty()) {
				nodeIter = nodes.iterator();

				while (nodeIter.hasNext()) {
					Object obj = nodeIter.next();

					if (obj instanceof StubImpl) {
						StubImpl stub = (StubImpl) obj;

						EList bindings = stub.getBindings();
						Iterator bindIter = null;
						if (!bindings.isEmpty()) {
							bindIter = bindings.iterator();

							//int j = 0;
							//int shift = 18;
							while (bindIter.hasNext()) {
								obj = bindIter.next();
								if (obj instanceof PluginBindingImpl) {
									PluginBindingImpl pluginBinding = (PluginBindingImpl) obj;
									UCMmap childMap = pluginBinding.getPlugin();

									childDiagramList.add(childMap);
								}
							}
							// end of while
						}
					}
				}
				// end of while
			}

			if (childDiagramList.size() > 0) {
				Element branch = xmlDocument.createElement(BRANCH);
				branch.setAttribute(BRANCH_ID, htmlMenuItem.getLeafText());
				branch.setAttribute(BRANCH_LINK, htmlMenuItem.getLink());
				branch.setAttribute(BASE_X, String.valueOf(htmlMenuItem.getBaseX()));
				branch.setAttribute(BASE_Y, String.valueOf(htmlMenuItem.getBaseY()));

				Element branchText = xmlDocument.createElement(BRANCH_TEXT);
				branchText.setTextContent(htmlMenuItem.getLeafText());

				branch.appendChild(branchText);
				parent.appendChild(branch);

				if (parent.getAttribute(BRANCH_ID).equals(HTMLMenuItem.TYPE_UCM)) {
					selectedMaps.add(branch);
				} else {
					for (int n = 0; n < selectedMaps.size(); n++) {
						Element selectedNode = (Element) selectedMaps.get(n);
						if (selectedNode.getTagName().equals(BRANCH)) {
							if (branch.getAttribute(BRANCH_LINK).equals(selectedNode.getAttribute(BRANCH_LINK))) {
								branch.setAttribute(BASE_X, selectedNode.getAttribute(BASE_X));
								branch.setAttribute(BASE_Y, selectedNode.getAttribute(BASE_Y));
								selectedNode.getParentNode().removeChild(selectedNode);
								break;
							}
						} else if (selectedNode.getTagName().equals(LEAF)) {
							if (branch.getAttribute(BRANCH_LINK).equals(selectedNode.getElementsByTagName(LINK).item(0).getTextContent())) {
								branch.setAttribute(BASE_X, selectedNode.getElementsByTagName(BASE_X).item(0).getTextContent());
								branch.setAttribute(BASE_Y, selectedNode.getElementsByTagName(BASE_Y).item(0).getTextContent());
								selectedNode.getParentNode().removeChild(selectedNode);
								break;
							}
						}
					}
				}

				for (int i = 0; i < childDiagramList.size(); i++) {
					IURNDiagram childDiagram = (IURNDiagram) childDiagramList.get(i);

					// prepare the html menu item
					htmlMenuItem.reset();

					String childDiagramName = ExportWizard.getDiagramName(childDiagram);
					htmlMenuItem.setDiagramName(childDiagramName);
					htmlMenuItem.setType(HTMLMenuItem.TYPE_UCM);
					htmlMenuItem.setLeafText(childDiagramName.substring(childDiagramName.lastIndexOf("-") + 1)); //$NON-NLS-1$
					htmlMenuItem.setLink(childDiagramName + ".html"); //$NON-NLS-1$
					htmlMenuItem.setBaseX(-1);
					htmlMenuItem.setBaseY(-1);
					htmlMenuItem.setDiagram(childDiagram);

					addUCMMenu(htmlMenuItem, branch);
				}
			} else {
				Element leaf = xmlDocument.createElement(LEAF);

				Element leafText = xmlDocument.createElement(LEAF_TEXT);
				leafText.setTextContent(htmlMenuItem.getLeafText());

				Element link = xmlDocument.createElement(LINK);
				link.setTextContent(htmlMenuItem.getLink());

				Element baseX = xmlDocument.createElement(BASE_X);
				baseX.setTextContent(String.valueOf(htmlMenuItem.getBaseX()));

				Element baseY = xmlDocument.createElement(BASE_Y);
				baseY.setTextContent(String.valueOf(htmlMenuItem.getBaseY()));

				leaf.appendChild(leafText);
				leaf.appendChild(link);
				leaf.appendChild(baseX);
				leaf.appendChild(baseY);
				parent.appendChild(leaf);

				if (parent.getAttribute(BRANCH_ID).equals(HTMLMenuItem.TYPE_UCM)) {
					selectedMaps.add(leaf);
				} else {
					for (int n = 0; n < selectedMaps.size(); n++) {
						Element selectedNode = (Element) selectedMaps.get(n);
						if (selectedNode.getTagName().equals(BRANCH)) {
							if (leaf.getElementsByTagName(LINK).item(0).getTextContent().equals(selectedNode.getAttribute(BRANCH_LINK))) {
								leaf.getElementsByTagName(BASE_X).item(0).setTextContent(selectedNode.getAttribute(BASE_X));
								leaf.getElementsByTagName(BASE_Y).item(0).setTextContent(selectedNode.getAttribute(BASE_Y));
								selectedNode.getParentNode().removeChild(selectedNode);
								break;
							}
						} else if (selectedNode.getTagName().equals(LEAF)) {
							if (leaf.getElementsByTagName(LINK).item(0).getTextContent().equals(
									selectedNode.getElementsByTagName(LINK).item(0).getTextContent())) {
								leaf.getElementsByTagName(BASE_X).item(0).setTextContent(selectedNode.getElementsByTagName(BASE_X).item(0).getTextContent());
								leaf.getElementsByTagName(BASE_Y).item(0).setTextContent(selectedNode.getElementsByTagName(BASE_Y).item(0).getTextContent());
								selectedNode.getParentNode().removeChild(selectedNode);
								break;
							}
						}
					}
				}
				// end if parent is the branch "UCM" and else
			}
		}
		// end if (!existing)
	}

	/**
	 * Emit a self-contained, modern index.html at the report root, containing
	 * a flexbox layout with the menu inlined as a sidebar and an iframe named
	 * "content" for the per-diagram pages. Previously this wrote tree.xml,
	 * which the browser was supposed to render via xmlTree.xsl -- but
	 * <?xml-stylesheet?>+XSLTProcessor is deprecated in Chrome/Edge (planned
	 * removal) and silently fails under file:// security. Emitting plain HTML
	 * makes the report double-click-openable in any modern browser, no XSLT,
	 * no jQuery, no frameset.
	 */
	public void writeToFile() {
		organizeMenus();

		StringBuilder sb = new StringBuilder(8192);
		sb.append("<!DOCTYPE html>\n"); //$NON-NLS-1$
		sb.append("<html lang=\"en\">\n<head>\n"); //$NON-NLS-1$
		sb.append("<meta charset=\"utf-8\">\n"); //$NON-NLS-1$
		sb.append("<title>URN HTML Report</title>\n"); //$NON-NLS-1$
		sb.append("<style>\n"); //$NON-NLS-1$
		sb.append("  html,body{margin:0;height:100%;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;font-size:13px;}\n"); //$NON-NLS-1$
		sb.append("  #layout{display:flex;height:100vh;}\n"); //$NON-NLS-1$
		sb.append("  #sidebar{flex:0 0 280px;overflow:auto;padding:10px 12px;background:#f6f8fa;border-right:1px solid #d0d7de;}\n"); //$NON-NLS-1$
		sb.append("  #sidebar details{margin:6px 0;}\n"); //$NON-NLS-1$
		sb.append("  #sidebar summary{cursor:pointer;font-weight:600;padding:2px 0;}\n"); //$NON-NLS-1$
		sb.append("  #sidebar ul{list-style:none;margin:2px 0 2px 14px;padding:0;}\n"); //$NON-NLS-1$
		sb.append("  #sidebar li{padding:1px 0;white-space:nowrap;}\n"); //$NON-NLS-1$
		sb.append("  #sidebar a{color:#0969da;text-decoration:none;}\n"); //$NON-NLS-1$
		sb.append("  #sidebar a:hover{text-decoration:underline;}\n"); //$NON-NLS-1$
		sb.append("  #sidebar img{vertical-align:middle;margin-right:4px;width:16px;height:16px;}\n"); //$NON-NLS-1$
		sb.append("  #content{flex:1 1 auto;border:0;width:100%;height:100%;}\n"); //$NON-NLS-1$
		sb.append("</style>\n"); //$NON-NLS-1$
		sb.append("</head>\n<body>\n"); //$NON-NLS-1$
		sb.append("<div id=\"layout\">\n"); //$NON-NLS-1$
		sb.append("<nav id=\"sidebar\">\n"); //$NON-NLS-1$

		// Walk the four top-level branches in the canonical order so the
		// sidebar reads UCM, GRL, FM, Definitions regardless of insertion order.
		String pagesPrefix = HTMLReport.PAGES_LOCATION.replace(File.separatorChar, '/');
		appendSection(sb, HTMLMenuItem.TYPE_UCM, pagesPrefix);
		appendSection(sb, HTMLMenuItem.TYPE_GRL, pagesPrefix);
		appendSection(sb, HTMLMenuItem.TYPE_FM,  pagesPrefix);
		appendSection(sb, "DEF",                 pagesPrefix); //$NON-NLS-1$

		sb.append("</nav>\n"); //$NON-NLS-1$
		sb.append("<iframe id=\"content\" name=\"content\" src=\""); //$NON-NLS-1$
		sb.append(pagesPrefix);
		sb.append("main.html\" title=\"Diagram content\"></iframe>\n"); //$NON-NLS-1$
		sb.append("</div>\n</body>\n</html>\n"); //$NON-NLS-1$

		File indexFile = new File(reportRoot + "index.html"); //$NON-NLS-1$
		FileWriter w = null;
		try {
			w = new FileWriter(indexFile);
			w.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (w != null) {
				try { w.close(); } catch (IOException ignore) {}
			}
		}
	}

	/** Append one top-level branch as a <details> block. */
	private void appendSection(StringBuilder sb, String branchId, String pagesPrefix) {
		Element branch = findTopBranch(branchId);
		if (branch == null) return;
		String title = textOfChild(branch, BRANCH_TEXT);
		sb.append("<details open><summary>"); //$NON-NLS-1$
		sb.append(EscapeUtils.escapeHTML(title));
		sb.append("</summary>\n<ul>\n"); //$NON-NLS-1$
		appendChildren(sb, branch, pagesPrefix, branchId);
		sb.append("</ul>\n</details>\n"); //$NON-NLS-1$
	}

	/**
	 * Recursively emit <li> nodes for direct children of the given DOM element.
	 * sectionId tells us which leaf-icon to use (UCM, GRL, FM, or DEF).
	 */
	private void appendChildren(StringBuilder sb, Element parent, String pagesPrefix, String sectionId) {
		NodeList kids = parent.getChildNodes();
		for (int i = 0; i < kids.getLength(); i++) {
			Node k = kids.item(i);
			if (k.getNodeType() != Node.ELEMENT_NODE) continue;
			Element e = (Element) k;
			String tag = e.getTagName();
			if (LEAF.equals(tag)) {
				String text = textOfChild(e, LEAF_TEXT);
				String link = textOfChild(e, LINK);
				sb.append("<li>"); //$NON-NLS-1$
				appendIcon(sb, sectionId, link, pagesPrefix);
				sb.append("<a target=\"content\" href=\""); //$NON-NLS-1$
				sb.append(pagesPrefix).append(escAttr(link));
				sb.append("\">"); //$NON-NLS-1$
				sb.append(EscapeUtils.escapeHTML(text));
				sb.append("</a></li>\n"); //$NON-NLS-1$
			} else if (BRANCH.equals(tag)) {
				// Nested UCM hierarchy (stub-binding sub-maps). The branch
				// itself links to a diagram (branchLink) AND has children.
				String text = textOfChild(e, BRANCH_TEXT);
				String link = e.getAttribute(BRANCH_LINK);
				sb.append("<li><details><summary>"); //$NON-NLS-1$
				appendIcon(sb, sectionId, link, pagesPrefix);
				if (link != null && link.length() > 0 && !"notRedirect".equals(link)) { //$NON-NLS-1$
					sb.append("<a target=\"content\" href=\""); //$NON-NLS-1$
					sb.append(pagesPrefix).append(escAttr(link));
					sb.append("\">"); //$NON-NLS-1$
					sb.append(EscapeUtils.escapeHTML(text));
					sb.append("</a>"); //$NON-NLS-1$
				} else {
					sb.append(EscapeUtils.escapeHTML(text));
				}
				sb.append("</summary>\n<ul>\n"); //$NON-NLS-1$
				appendChildren(sb, e, pagesPrefix, sectionId);
				sb.append("</ul>\n</details></li>\n"); //$NON-NLS-1$
			}
		}
	}

	private void appendIcon(StringBuilder sb, String sectionId, String link, String pagesPrefix) {
		String icon;
		if ("DEF".equals(sectionId)) { //$NON-NLS-1$
			if ("UCM_Definitions.html".equals(link)) icon = "ucmdef16.gif"; //$NON-NLS-1$ //$NON-NLS-2$
			else if ("UCM_Scenarios.html".equals(link)) icon = "ucmscen16.gif"; //$NON-NLS-1$ //$NON-NLS-2$
			else if ("GRL_Definitions.html".equals(link)) icon = "grldef16.gif"; //$NON-NLS-1$ //$NON-NLS-2$
			else if ("FM_Definitions.html".equals(link)) icon = "feature16.gif"; //$NON-NLS-1$ //$NON-NLS-2$
			else if ("main.html".equals(link)) icon = "icon16.gif"; //$NON-NLS-1$ //$NON-NLS-2$
			else icon = "icon16.gif"; //$NON-NLS-1$
		} else if (HTMLMenuItem.TYPE_UCM.equals(sectionId)) {
			icon = "ucm16.gif"; //$NON-NLS-1$
		} else if (HTMLMenuItem.TYPE_GRL.equals(sectionId)) {
			icon = "grl16.gif"; //$NON-NLS-1$
		} else { // FM
			icon = "fmd16.gif"; //$NON-NLS-1$
		}
		sb.append("<img src=\"").append(pagesPrefix).append(icon).append("\" alt=\"\">"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Element findTopBranch(String id) {
		NodeList kids = xmlDocument.getDocumentElement().getChildNodes();
		for (int i = 0; i < kids.getLength(); i++) {
			Node k = kids.item(i);
			if (k.getNodeType() != Node.ELEMENT_NODE) continue;
			Element e = (Element) k;
			if (BRANCH.equals(e.getTagName()) && id.equals(e.getAttribute(BRANCH_ID))) {
				return e;
			}
		}
		return null;
	}

	private String textOfChild(Element parent, String tag) {
		NodeList list = parent.getElementsByTagName(tag);
		if (list.getLength() == 0) return ""; //$NON-NLS-1$
		String t = list.item(0).getTextContent();
		return t == null ? "" : t; //$NON-NLS-1$
	}

	private String escAttr(String s) {
		if (s == null) return ""; //$NON-NLS-1$
		// EscapeUtils.escapeHTML handles &<> and quotes; safe inside attr context.
		return EscapeUtils.escapeHTML(s);
	}

	/**
	 * Organize menus - Add 'No Maps' item if there is no map under UCM or/and GRL - Add 'No MSC Scenarios' item if scenario output is selected and there is no
	 * MSC scenario
	 * 
	 */
	private void organizeMenus() {
		if (xmlDocument == null) {
			parseMenu();
		}

		NodeList branchList = xmlDocument.getDocumentElement().getChildNodes();
		int len = branchList.getLength();
		Element branch = null;
		
		for (int i = 0; i < len; i++) {
			Node childNode = branchList.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				branch = (Element) childNode;
				String emptyText = ""; //$NON-NLS-1$
				String bid = branch.getAttribute(BRANCH_ID);
				// the branch corresponds to the UCM menu
				if (bid.equals(HTMLMenuItem.TYPE_UCM)) {
					// get the child node corresponding to the branch text (i.e. the menu name),
					// fetch the menu name from the properties and assign this name as the text content of the node
					NodeList childNodeList = childNode.getChildNodes();
					int len2 = childNodeList.getLength();
					for (int j=0; j < len2; j++) {
						Node childNode2 = childNodeList.item(j);
						if ((childNode2.getNodeName()).equals(BRANCH_TEXT)) {
							childNode2.setTextContent(Messages.getString("HTMLMenuParser.UCMDiagrams")); //$NON-NLS-1$
							break;
						}
					}
					// set the default "no UCMs" message
					emptyText = Messages.getString("HTMLMenuParser.NoUCMs"); //$NON-NLS-1$
				} else if (bid.equals(HTMLMenuItem.TYPE_GRL)) {
					// the branch corresponds to the GRL menu
					// get the child node corresponding to the branch text (i.e. the menu name),
					// fetch the menu name from the properties and assign this name as the text content of the node
					NodeList childNodeList = childNode.getChildNodes();
					int len2 = childNodeList.getLength();
					for (int j=0; j < len2; j++) {
						Node childNode2 = childNodeList.item(j);
						if ((childNode2.getNodeName()).equals(BRANCH_TEXT)) {
							childNode2.setTextContent(Messages.getString("HTMLMenuParser.GRLDiagrams")); //$NON-NLS-1$
							break;
						}
					}
					// set the default "no GRLs" message
					emptyText = Messages.getString("HTMLMenuParser.NoGRLs"); //$NON-NLS-1$
				} else if (bid.equals(HTMLMenuItem.TYPE_FM)) {
					// the branch corresponds to the FM menu
					// get the child node corresponding to the branch text (i.e. the menu name),
					// fetch the menu name from the properties and assign this name as the text content of the node
					NodeList childNodeList = childNode.getChildNodes();
					int len2 = childNodeList.getLength();
					for (int j=0; j < len2; j++) {
						Node childNode2 = childNodeList.item(j);
						if ((childNode2.getNodeName()).equals(BRANCH_TEXT)) {
							childNode2.setTextContent(Messages.getString("HTMLMenuParser.FMDiagrams")); //$NON-NLS-1$
							break;
						}
					}
					// set the default "no FMs" message
					emptyText = Messages.getString("HTMLMenuParser.NoFMs"); //$NON-NLS-1$
				} else if (bid.equals(HTMLMenuItem.TYPE_MSC)) {
					// the branch corresponds to the MSC menu
					// get the child node corresponding to the branch text (i.e. the menu name),
					// fetch the menu name from the properties and assign this name as the text content of the node
					NodeList childNodeList = childNode.getChildNodes();
					int len2 = childNodeList.getLength();
					for (int j=0; j < len2; j++) {
						Node childNode2 = childNodeList.item(j);
						if ((childNode2.getNodeName()).equals(BRANCH_TEXT)) {
							childNode2.setTextContent(Messages.getString("HTMLMenuParser.MSCDiagrams")); //$NON-NLS-1$
							break;
						}
					}
					// set the default "no MSCs" message
					emptyText = Messages.getString("HTMLMenuParser.NoMSCs"); //$NON-NLS-1$
				} else {
					// the branch corresponds to the Definitions menu
					// get the child node corresponding to the branch text (i.e. the menu name),
					// fetch the menu name from the properties and assign this name as the text content of the node
					NodeList childNodeList = childNode.getChildNodes();
					int len2 = childNodeList.getLength();
					for (int j=0; j < len2; j++) {
						Node childNode2 = childNodeList.item(j);
						if ((childNode2.getNodeName()).equals(BRANCH_TEXT)) {
							childNode2.setTextContent(Messages.getString("HTMLMenuParser.Definitions")); //$NON-NLS-1$
						} else if ((childNode2.getNodeName()).equals(LEAF)) {
							// the child nodes of the Definitions node are the UCM Definitions page, the UCM Scenarios page,
							// the GRL Definitions page, the FM Definitions as well as the Title Page
							NodeList child2NodeList = childNode2.getChildNodes();
							int len3 = child2NodeList.getLength();
							// get the child node corresponding to the leaf text (i.e. the menu name),
							// fetch the menu name from the properties and assign this name as the text content of the node
							for (int k=0; k < len3; k++) {
								Node childNode3 = child2NodeList.item(k);
								if ((childNode3.getNodeName()).equals(LEAF_TEXT)) {
									if ((childNode3.getTextContent()).equals(HTMLMenuItem.TYPE_UCM_DEF)) {
										childNode3.setTextContent(Messages.getString("HTMLMenuParser.UCMDefinitions")); //$NON-NLS-1$
									} else if ((childNode3.getTextContent()).equals(HTMLMenuItem.TYPE_UCM_SCEN)) {
										childNode3.setTextContent(Messages.getString("HTMLMenuParser.UCMScenarios")); //$NON-NLS-1$
									} else if ((childNode3.getTextContent()).equals(HTMLMenuItem.TYPE_GRL_DEF)) {
										childNode3.setTextContent(Messages.getString("HTMLMenuParser.GRLDefinitions")); //$NON-NLS-1$
									} else if ((childNode3.getTextContent()).equals(HTMLMenuItem.TYPE_FM_DEF)) {
										childNode3.setTextContent(Messages.getString("HTMLMenuParser.FMDefinitions")); //$NON-NLS-1$
									} else {
										childNode3.setTextContent(Messages.getString("HTMLMenuParser.TitlePage")); //$NON-NLS-1$
									}
									break;
								}
							}
						}
					}
				}

				// set the text content of the leaf nodes if need be (i.e. if no diagram is
				// found in one of the categories)
				if (branch.getElementsByTagName(LEAF).getLength() <= 0 && branch.getElementsByTagName(BRANCH).getLength() <= 0) {
					Element leaf = xmlDocument.createElement(LEAF);

					Element leafText = xmlDocument.createElement(LEAF_TEXT);
					leafText.setTextContent(emptyText);

					Element link = xmlDocument.createElement(LINK);
					link.setTextContent("main.html"); //$NON-NLS-1$

					Element baseX = xmlDocument.createElement(BASE_X);
					baseX.setTextContent("0"); //$NON-NLS-1$

					Element baseY = xmlDocument.createElement(BASE_Y);
					baseY.setTextContent("0"); //$NON-NLS-1$

					leaf.appendChild(leafText);
					leaf.appendChild(link);
					leaf.appendChild(baseX);
					leaf.appendChild(baseY);
					branch.appendChild(leaf);
				}
			}
		}
	}

}
