package org.sakaiproject.archive.impl;

import org.jsoup.Jsoup;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.*;
import java.util.*;
import java.util.stream.*;
import java.util.regex.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.concurrent.atomic.AtomicBoolean;

public class LessonsRejigger {

    private final AtomicBoolean insidePage = new AtomicBoolean(false);
    private final List<BufferedSAXEvent> bufferedItems = new LinkedList<>();

    public boolean rewriteLessons(String path) {
        try {
            XMLReader xr = new XMLFilterImpl(XMLReaderFactory.createXMLReader()) {
                final AtomicBoolean hasKaltura = new AtomicBoolean(false);

                public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                    if ("page".equals(qName)) {
                        insidePage.set(true);
                    } else if (insidePage.get()) {
                        bufferedItems.add(BufferedSAXEvent.startElement(uri, localName, qName, atts));
                        return;
                    }

                    super.startElement(uri, localName, qName, atts);
                }

                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if ("page".equals(qName)) {
                        if (!insidePage.get()) {
                            throw new RuntimeException("Assertion failed: did not expect to find nested pages here.");
                        }

                        insidePage.set(false);
                        emitBufferedItems();
                    } else if (insidePage.get()) {
                        bufferedItems.add(BufferedSAXEvent.endElement(uri, localName, qName));
                        return;
                    }

                    super.endElement(uri, localName, qName);
                }

                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (insidePage.get()) {
                        bufferedItems.add(BufferedSAXEvent.characters(ch.clone(), start, length));
                        return;
                    }

                    super.characters(ch, start, length);
                }

                private void emitBufferedItems() throws SAXException {
                    LinkedList<Item> items = groupEvents();
                    bufferedItems.clear();

                    // Multimedia items linking to Sakai resources can be rewritten as rich text
                    // TEXT items.
                    for (Item item : items) {
                        if (item.type == ItemType.MULTIMEDIA &&
                            item.events.get(0).atts.getValue("sakaiid").toLowerCase(Locale.ROOT).matches("^/.*\\.(jpg|jpeg|png|gif|bmp|svg|jfif|pjpeg|pjp|ico|cur|tif|tiff|webp)$")) {
                            // Transmogrify into rich text.  That's right: transmogrify.
                            BufferedSAXEvent elt = item.events.get(0);
                            AttributesImpl updatedAttributes = new AttributesImpl(elt.atts);

                            String contentPath = updatedAttributes.getValue("sakaiid");
                            String altText = updatedAttributes.getValue("alt");

                            updatedAttributes.setValue(updatedAttributes.getIndex("sakaiid"), "");
                            updatedAttributes.setValue(updatedAttributes.getIndex("type"), "5"); // text
                            updatedAttributes.setValue(updatedAttributes.getIndex("html"),
                                                       String.format("<p><img style=\"max-width: 100%\" alt=\"%s\" src=\"https://newclasses.nyu.edu/access/content%s\"></p>",
                                                                     altText,
                                                                     contentPath));

                            elt.atts = updatedAttributes;
                            item.type = ItemType.TEXT;
                        }
                    }

                    // <text><break><text> => <text w/ hr>
                    for (int i = 1; i < (items.size() - 1); i++) {
                        if (items.get(i - 1).type.equals(ItemType.TEXT) &&
                            items.get(i).type.equals(ItemType.BREAK) &&
                            items.get(i + 1).type.equals(ItemType.TEXT)) {
                            // Mergey mergey
                            Item victim = items.remove(i + 1);
                            items.remove(i); // drop the break

                            Item assimilator = items.get(i - 1);

                            BufferedSAXEvent startNode = assimilator.events.get(0);
                            AttributesImpl mergedAttributes = new AttributesImpl(startNode.atts);
                            int attIdx = mergedAttributes.getIndex("html");
                            mergedAttributes.setValue(attIdx,
                                                      mergedAttributes.getValue(attIdx) +
                                                      "<hr>" +
                                                      victim.events.get(0).atts.getValue("html"));

                            startNode.atts = mergedAttributes;

                            // compensate for the nodes we just removed to pick up runs of
                            // <text><break><text><break>...
                            i -= 1;
                        }
                    }

                    // Adjacent text items can be merged too
                    // <text><break><text> => <text w/ hr>
                    for (int i = 0; i < (items.size() - 1); i++) {
                        if (items.get(i).type.equals(ItemType.TEXT) &&
                            items.get(i + 1).type.equals(ItemType.TEXT)) {
                            // Mergey mergey
                            Item victim = items.remove(i + 1);
                            Item assimilator = items.get(i);

                            BufferedSAXEvent startNode = assimilator.events.get(0);
                            AttributesImpl mergedAttributes = new AttributesImpl(startNode.atts);
                            int attIdx = mergedAttributes.getIndex("html");
                            mergedAttributes.setValue(attIdx,
                                                      mergedAttributes.getValue(attIdx) +
                                                      victim.events.get(0).atts.getValue("html"));

                            startNode.atts = mergedAttributes;

                            i -= 1;
                        }
                    }

                    // Any remaining breaks are not required
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).type.equals(ItemType.BREAK)) {
                            items.remove(i);
                            i -= 1;
                        }
                    }

                    // Nameless text items get a name
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).type.equals(ItemType.TEXT)) {
                            Item text = items.get(i);
                            BufferedSAXEvent elt = text.events.get(0);

                            if (elt.atts.getValue("name").isEmpty()) {
                                String generatedName = Jsoup.parse(elt.atts.getValue("html")).text();

                                if (generatedName.length() > 30) {
                                    generatedName = generatedName.substring(0, 30) + "...";
                                }

                                if (generatedName.isEmpty()) {
                                    generatedName = "Embedded Item";
                                }

                                AttributesImpl updatedAttributes = new AttributesImpl(elt.atts);
                                updatedAttributes.setValue(updatedAttributes.getIndex("name"), generatedName);
                                elt.atts = updatedAttributes;
                            }
                        }
                    }


                    for (Item item : items) {
                        for (BufferedSAXEvent event : item.events) {
                            if (event.type.equals(EventType.START)) {
                                super.startElement(event.uri, event.localName, event.qName, event.atts);
                            } else if (event.type.equals(EventType.END)) {
                                super.endElement(event.uri, event.localName, event.qName);
                            } else if (event.type.equals(EventType.CHARS)) {
                                super.characters(event.ch, event.start, event.length);
                            }
                        }
                    }
                }

                // Group our SAX events into top-level <item> elements
                private LinkedList<Item> groupEvents() {
                    LinkedList<BufferedSAXEvent> queue = new LinkedList(bufferedItems);
                    LinkedList<Item> result = new LinkedList<Item>();

                    while (!queue.isEmpty()) {
                        BufferedSAXEvent head = queue.removeFirst();

                        if (EventType.START.equals(head.type) && "item".equals(head.qName)) {
                            Item item = null;

                            if ("14".equals(head.atts.getValue("type"))) {
                                item = new Item(ItemType.BREAK);
                            } else if ("5".equals(head.atts.getValue("type"))) {
                                item = new Item(ItemType.TEXT);
                            } else if ("7".equals(head.atts.getValue("type"))) {
                                item = new Item(ItemType.MULTIMEDIA);
                            } else {
                                item = new Item(ItemType.OTHER);
                            }

                            item.events.add(head);

                            // Read up until the closing item event
                            boolean foundClose = false;
                            while (!queue.isEmpty()) {
                                BufferedSAXEvent elt = queue.removeFirst();
                                if (EventType.START.equals(elt.type) && "item".equals(elt.qName)) {
                                    throw new RuntimeException("Assertion failed: did not expect nested items");
                                }

                                item.events.add(elt);

                                if (EventType.END.equals(elt.type) && "item".equals(elt.qName)) {
                                    // All done
                                    foundClose = true;
                                    break;
                                }
                            }

                            if (!foundClose) {
                                throw new RuntimeException("Assertion failed: Ran out of input before finding our closing item");
                            }

                            result.add(item);
                        } else {
                            throw new RuntimeException("Assertion failed: expected a list of top-level items here");
                        }
                    }

                    return result;
                }
            };

            // Pass through invalid 'sakai:' prefixes
            xr.setFeature("http://xml.org/sax/features/namespaces", false);

            Source src = new SAXSource(xr, new InputSource(path));
            Result res = new StreamResult(new FileOutputStream(path + ".rewritten"));
            TransformerFactory.newInstance().newTransformer().transform(src, res);

            // Overwrite the original path
            new File(path + ".rewritten").renameTo(new File(path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private enum EventType {
        START,
        END,
        CHARS,
    }

    private static class BufferedSAXEvent {
        public EventType type;

        // elements
        public String uri;
        public String localName;
        public String qName;
        public Attributes atts;

        // char data
        public char[] ch;
        public int start;
        public int length;

        private BufferedSAXEvent(EventType type) {
            this.type = type;
        }

        public static BufferedSAXEvent startElement(String uri, String localName, String qName, Attributes atts) {
            BufferedSAXEvent result = new BufferedSAXEvent(EventType.START);

            result.uri = uri;
            result.localName = localName;
            result.qName = qName;
            result.atts = new AttributesImpl(atts);

            return result;
        }

        public static BufferedSAXEvent endElement(String uri, String localName, String qName) {
            BufferedSAXEvent result = new BufferedSAXEvent(EventType.END);

            result.uri = uri;
            result.localName = localName;
            result.qName = qName;

            return result;
        }

        public static BufferedSAXEvent characters(char[] ch, int start, int length) {
            BufferedSAXEvent result = new BufferedSAXEvent(EventType.CHARS);

            result.ch = ch;
            result.start = start;
            result.length = length;

            return result;
        }
    }

    private enum ItemType {
        TEXT,
        BREAK,
        MULTIMEDIA,
        OTHER,
    }

    private static class Item {
        public ItemType type;
        public List<BufferedSAXEvent> events;

        public Item(ItemType type) {
            this.type = type;
            this.events = new ArrayList<>();
        }
    }


    public static void main(String[] args) {
        new LessonsRejigger().rewriteLessons(args[0]);
    }
}
