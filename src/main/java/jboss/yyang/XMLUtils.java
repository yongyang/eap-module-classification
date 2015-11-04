package jboss.yyang;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:yyang@redhat.com">Yong Yang</a>
 */
public class XMLUtils {

    private final static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private final static TransformerFactory tf = TransformerFactory.newInstance();

    private final static XPathFactory xpf = XPathFactory.newInstance();

    static {
        tf.setAttribute("indent-number", 2);
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);
        dbf.setIgnoringComments(false);
    }


    private XMLUtils() {

    }

    public static List<Element> getChildElements(Element parentElement) {
        NodeList nodeList = parentElement.getChildNodes();
        List<Element> childElements = new ArrayList<Element>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(0);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                childElements.add((Element)childNode);
            }
        }
        return childElements;
    }

    public static Element getChildElementByTagName(Element parentElement, String childTag) {
        for (Node temp = parentElement.getFirstChild(); temp != null; temp = temp.getNextSibling())
            if (temp.getNodeType() == Node.ELEMENT_NODE && childTag.equals(temp.getNodeName())) {
                return (Element)temp;
            }
        return null;
    }

    public static List<Element> getChildElementsByTagName(Element parentElement, String childTag) {
        NodeList nodelist = parentElement.getChildNodes();
        List<Element> nodes = new ArrayList<Element>();
        for (int i = 0; i < nodelist.getLength(); i++) {
            Node temp = nodelist.item(i);
            if (temp.getNodeType() == Node.ELEMENT_NODE && temp.getNodeName().equals(childTag)) {
                nodes.add((Element)temp);
            }
        }
        return nodes;
    }

    public static String getChildElementValueByTagName(Element parentElement, String childTag) {
        if (childTag.equals(parentElement.getNodeName())) {
            return getNodeValue(parentElement);
        }
        for (Node temp = parentElement.getFirstChild(); temp != null; temp = temp.getNextSibling()) {
            if (temp.getNodeType() == Node.ELEMENT_NODE && childTag.equals(temp.getNodeName())) {
                return getNodeValue(temp);
            }
        }
        return null;
    }

    public static List<Element> getElementsByTagName(Element element, String tagName) {
        ArrayList<Element> children = new ArrayList<Element>();
        if (element != null && tagName != null) {
            NodeList nodes = element.getElementsByTagName(tagName);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node child = nodes.item(i);
                children.add((Element)child);
            }
        }
        return children;
    }

    public static List<Element> getElementsByTagNames(Element element, String[] tagNames) {
        List<Element> children = new ArrayList<Element>();
        if (element != null && tagNames != null) {
            List tagList = Arrays.asList(tagNames);
            NodeList nodes = element.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node child = nodes.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE && tagList.contains(((Element)child).getTagName())) {
                    children.add((Element)child);
                }
            }
        }
        return children;
    }

    public static String getNodeValue(Node node) {
        if (node == null) {
            return null;
        }
        else if (node instanceof Text) {
            return node.getNodeValue().trim();
        }
        else if (node instanceof Element) {
            node.normalize();
            Node temp = node.getFirstChild();
            if (temp != null && (temp instanceof Text))
                return temp.getNodeValue().trim();
            else
                return "";
        }
        else {
            return node.getNodeValue().trim();
        }
    }

    public static String getAttributeValue(Node node, String attribute) {
        Node _node = node.getAttributes().getNamedItem(attribute);
        return getNodeValue(_node);
    }

    /**
     * get the xml root document of the xml descriptor
     *
     * @param file the xml descriptor url
     * @return XML document
     * @throws IOException                  if failed to create XML document
     * @throws ParserConfigurationException e
     * @throws SAXException                 e
     */
    public static Document loadDocument(File file) throws IOException, ParserConfigurationException, SAXException {
        return dbf.newDocumentBuilder().parse(file);
    }

    /**
     * parse document from xml String
     *
     * @param xml xml string
     * @throws IOException                  if failed to create XML document
     * @throws ParserConfigurationException e
     * @throws SAXException                 e
     */
    public static Document loadDocument(String xml) throws IOException, ParserConfigurationException, SAXException {
        return dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    /**
     * new document
     */
    public static Document newDocument() {
        try {
            return dbf.newDocumentBuilder().newDocument();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 为 parent Node 增加一个 TextElement
     *
     * @param parent 父节点
     * @param name   element name
     * @param value  element value
     * @return 增加的 element
     */
    public static Element addTextElement(Node parent, String name, String value) {
        return addTextElement(parent, name, value, null);
    }

    /**
     * 为 parent Node 增加一个 TextElement，可定义该Element 的属性。
     *
     * @param parent parent node
     * @param name   node name
     * @param value  node value
     * @param attrs  atrributs
     */
    public static Element addTextElement(Node parent, String name, String value, Attr[] attrs) {
        Element element;
        if (parent instanceof Document) {
            element = ((Document)parent).createElement(name);
        }
        else {
            element = parent.getOwnerDocument().createElement(name);
        }

        if (attrs != null && attrs.length > 0) {
            for (Attr attr : attrs) {
                element.setAttributeNode(attr);
            }
        }

        if (value != null) {
            element.setTextContent(value);
        }
        parent.appendChild(element);
        return element;
    }



    /**
     * Add a CDATA element.
     *
     * @param parent parent node
     * @param name   node name
     * @param data   node data
     */
    public static Element addCDATAElement(Node parent, String name, String data) {
        return addCDATAElement(parent, name, data, null);
    }

    /**
     * Add a CDATA element with attributes.
     * <p/>
     * NOTE: Serializing a XML document via TRAX changes "\r\n" to "\r\r\n" in
     * a CDATA section. Serializing with the Xalan XMLSerializer works fine.
     *
     * @param parent parent node
     * @param name   node name
     * @param data   node data
     * @param attrs  attributes
     */
    public static Element addCDATAElement(Node parent, String name, String data, Attr[] attrs) {
        Element element;
        CDATASection cdata;
        if (parent instanceof Document) {
            element = ((Document)parent).createElement(name);
            /*
                * Creates a <code>CDATASection</code> node whose value is the
                * specified.
                */
            cdata = ((Document)parent).createCDATASection(data);
        }
        else {
            element = parent.getOwnerDocument().createElement(name);
            cdata = parent.getOwnerDocument().createCDATASection(data);
        }

        if (attrs != null && attrs.length > 0) {
            for (Attr attr : attrs) {
                element.setAttributeNode(attr);
            }
        }

        element.appendChild(cdata);
        parent.appendChild(element);
        return element;
    }

    /**
     * 为 parent Node 增加一个空的 Element
     *
     * @param parent 父节点
     * @param name   element name
     * @param attrs  element 的 attributes
     * @return 增加的 element
     */
    public static Element addElement(Node parent, String name, Attr[] attrs) {
        Element element;
        if (parent instanceof Document) {
            element = ((Document)parent).createElement(name);
        }
        else {
            element = parent.getOwnerDocument().createElement(name);
        }
        if (attrs != null && attrs.length > 0) {
            for (Attr attr : attrs) {
                element.setAttributeNode(attr);
            }
        }
        parent.appendChild(element);
        return element;
    }

    public static Element addElement(Element parent, Element childElement){
        Node childNode = parent.getOwnerDocument().importNode(childElement,true);
        parent.appendChild(childNode);
        return parent;
    }
    /**
     * 创建 Attribute
     *
     * @param document xml document
     * @param name     node name
     * @param value    node value
     * @return Attr
     */
    public static Attr createAttribute(Document document, String name, String value) {
        Attr attr = document.createAttribute(name);
        attr.setTextContent(value);
        return attr;
    }

    /**
     * 将document转成字符串
     *
     * @param node node
     */
    public static String toXMLString(Node node) {
        if (node == null) {
            return "";
        }
        try {
            Transformer tran = tf.newTransformer();
            tran.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter swriter = new StringWriter();
            Source src = new DOMSource(node);
            Result res = new StreamResult(swriter);
            tran.transform(src, res);
            return swriter.getBuffer().toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void toWriter(Document doc, Writer writer) {
        if (doc == null || writer == null) {
            return;
        }
        try {
            Transformer tran = tf.newTransformer();
            tran.setOutputProperty(OutputKeys.INDENT, "yes");
            tran.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            Source src = new DOMSource(doc);
            Result res = new StreamResult(writer);
            tran.transform(src, res);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object evaluateXPath(Element doc, String xpath, QName returnType) throws XPathExpressionException {
        XPath path = xpf.newXPath();
        return path.evaluate(xpath, doc, returnType);
    }

    public static XPathExpression compileXPathExpression(String expression) throws XPathExpressionException {
        XPath path = xpf.newXPath();
        return path.compile(expression);
    }

    public static Object evaluateXPathExpression(Element parent, XPathExpression xPathExpression, QName returnType) throws XPathExpressionException {
        return xPathExpression.evaluate(parent,returnType);
    }

}
