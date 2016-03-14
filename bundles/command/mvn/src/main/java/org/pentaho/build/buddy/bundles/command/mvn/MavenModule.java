package org.pentaho.build.buddy.bundles.command.mvn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bryan on 2/26/16.
 */
public class MavenModule {
    private static final Logger logger = LoggerFactory.getLogger(MavenModule.class);
    private final List<MavenModule> subModules;
    private final File pom;
    private final MavenModule parent;

    private MavenModule(File pom, List<MavenModule> subModules, MavenModule parent) {
        this.subModules = subModules;
        this.pom = pom;
        this.parent = parent;
    }

    public static MavenModule buildModule(File pom) throws IOException {
        return buildModule(pom, null);
    }

    private static MavenModule buildModule(File pom, MavenModule parent) throws IOException {
        List<MavenModule> submodules = new ArrayList<>();
        MavenModule mavenModule = new MavenModule(pom, Collections.unmodifiableList(submodules), parent);
        submodules.addAll(buildChildModules(pom, mavenModule));
        return mavenModule;
    }

    private static List<MavenModule> buildChildModules(File pom, MavenModule parent) throws IOException {
        logger.debug("Parsing pom: " + pom);
        Document document;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        Node modules = null;
        NodeList childNodes = document.getFirstChild().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if ("modules".equals(item.getNodeName())) {
                modules = item;
                break;
            }
        }
        if (modules == null) {
            return Collections.emptyList();
        }
        NodeList moduleNodes = modules.getChildNodes();
        int moduleNodesLength = moduleNodes.getLength();
        List<MavenModule> childModules = new ArrayList<>(moduleNodesLength);
        for (int i = 0; i < moduleNodesLength; i++) {
            Node item = moduleNodes.item(i);
            if ("module".equals(item.getNodeName())) {
                childModules.add(buildModule(new File(new File(pom.getParent(), item.getTextContent()), "pom.xml"), parent));
            }
        }
        return Collections.unmodifiableList(childModules);
    }

    public List<MavenModule> getSubModules() {
        return subModules;
    }

    public File getPom() {
        return pom;
    }

    public File getBase() {
        return pom.getParentFile();
    }

    public String getName() {
        return getBase().getName();
    }

    public MavenModule getParent() {
        return parent;
    }

    public MavenModule getMostSpecificModule(String... path) {
        MavenModule current = this;
        for (String s : path) {
            MavenModule next = current.getSubmoduleByName(s);
            if (next == null) {
                break;
            }
            current = next;
        }
        return current;
    }

    public String getPath() {
        if (parent == null) {
            return getName();
        }
        return parent.getPath() + "/" + getName();
    }

    public MavenModule getSubmoduleByName(String name) {
        for (MavenModule subModule : subModules) {
            if (name.equals(subModule.getName())) {
                return subModule;
            }
        }
        return null;
    }

    public List<MavenModule> getAllDescendentModules() {
        List<MavenModule> result = new ArrayList<>();
        for (MavenModule subModule : subModules) {
            result.add(subModule);
            result.addAll(subModule.getAllDescendentModules());
        }
        return result;
    }

    @Override
    public String toString() {
        return "MavenModule{" +
                "subModules=" + subModules +
                ", pom=" + pom +
                '}';
    }
}
