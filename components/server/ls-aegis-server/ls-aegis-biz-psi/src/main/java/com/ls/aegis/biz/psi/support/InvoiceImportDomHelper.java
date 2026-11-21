package com.ls.aegis.biz.psi.support;

import cn.hutool.core.util.StrUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 发票 XML DOM 解析工具（从 InvoiceImportSupport 拆出，降低单文件体积）
 */
final class InvoiceImportDomHelper {

    private InvoiceImportDomHelper() {
    }

    static Document parseSecureXml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    static String localNameOf(Node node) {
        if (node == null) {
            return "";
        }
        String ln = node.getLocalName();
        if (StrUtil.isNotBlank(ln)) {
            return ln;
        }
        String nn = StrUtil.blankToDefault(node.getNodeName(), "");
        int idx = nn.indexOf(':');
        return idx >= 0 ? nn.substring(idx + 1) : nn;
    }

    static boolean nameEquals(Node node, String expected) {
        return expected != null && expected.equalsIgnoreCase(localNameOf(node));
    }

    static String firstTextByLocalName(Document doc, String... names) {
        for (String name : names) {
            for (Element el : elementsByLocalName(doc, name)) {
                String t = StrUtil.trim(el.getTextContent());
                if (StrUtil.isNotBlank(t) && !hasElementChild(el)) {
                    return t;
                }
                if (StrUtil.isNotBlank(t) && t.length() < 200) {
                    return t;
                }
                String attr = firstAttr(el, "value", "val", "Value", "text", "content");
                if (StrUtil.isNotBlank(attr)) {
                    return attr;
                }
            }
        }
        return null;
    }

    static String firstAttr(Element el, String... names) {
        for (String name : names) {
            String v = StrUtil.trim(el.getAttribute(name));
            if (StrUtil.isNotBlank(v)) {
                return v;
            }
        }
        return null;
    }

    static String firstChildTextUnder(Document doc, String parentName, String childName) {
        for (Element parent : elementsByLocalName(doc, parentName)) {
            String t = childTextByLocalName(parent, childName);
            if (StrUtil.isNotBlank(t)) {
                return t;
            }
        }
        return null;
    }

    static String childTextByLocalName(Element parent, String... names) {
        for (String name : names) {
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n instanceof Element el && nameEquals(el, name)) {
                    String t = StrUtil.trim(el.getTextContent());
                    if (StrUtil.isNotBlank(t)) {
                        return t;
                    }
                }
            }
            for (Element el : elementsByLocalName(parent, name)) {
                String t = StrUtil.trim(el.getTextContent());
                if (StrUtil.isNotBlank(t)) {
                    return t;
                }
            }
        }
        return null;
    }

    static List<Element> elementsByLocalName(Node root, String... names) {
        List<Element> list = new ArrayList<>();
        collectElements(root, names, list);
        return list;
    }

    private static void collectElements(Node node, String[] names, List<Element> out) {
        if (node == null) {
            return;
        }
        if (node instanceof Element el) {
            for (String name : names) {
                if (nameEquals(el, name)) {
                    out.add(el);
                    break;
                }
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectElements(children.item(i), names, out);
        }
    }

    private static boolean hasElementChild(Element el) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                return true;
            }
        }
        return false;
    }
}
