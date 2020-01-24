package com.canyie.dreamland.manager.utils;

import android.util.Xml;

import org.xml.sax.helpers.XMLReaderFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author canyie
 * @date 2019/12/17.
 */
public final class XMLUtils {
    private XMLUtils() {
    }

    public static void skipCurrentTag(XmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG
                || parser.getDepth() > outerDepth)) ;
    }

    public static Map<String, Object> readMapXml(File file) throws IOException, XmlPullParserException {
        return readMapXml(new FileReader(file));
    }

    public static Map<String, Object> readMapXml(InputStream in) throws IOException, XmlPullParserException {
        return readMapXml(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public static Map<String, Object> readMapXml(Reader in) throws IOException, XmlPullParserException {
        Map<String, Object> map = new HashMap<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new BufferedReader(in));
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                String key = parser.getAttributeValue(null, "key");
                Object val = readValue(parser, tag);
                map.put(key, val);
            } else if (eventType == XmlPullParser.END_TAG) {
                throw new XmlPullParserException("Unexpected end tag: " + parser.getName());
            } else if (eventType == XmlPullParser.TEXT) {
                throw new XmlPullParserException("Unexpected text in <" + parser.getName() + ">: " + parser.getText());
            }
            eventType = parser.next();
        }
        return map;
    }

    private static Object readValue(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        Object result;
        if ("null".equals(tag)) {
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.END_TAG) {
                    if (tag.equals(parser.getName())) {
                        return null;
                    }
                    throw new XmlPullParserException("Unexpected end tag in <null>: " + parser.getName());
                } else if (eventType == XmlPullParser.START_TAG) {
                    throw new XmlPullParserException("Unexpected start tag in <null>: " + parser.getName());
                } else if (eventType == XmlPullParser.TEXT) {
                    throw new XmlPullParserException("Unexpected text in <null>: " + parser.getName());
                }
            }
            throw new XmlPullParserException("Unexpected end of document in <null>");
        } else if ((result = readBasicValue(parser, tag)) != null) {
        } else if ("set".equals(tag)) {
            result = readSetValue(parser);
        } else {
            throw new XmlPullParserException("Unknown tag: " + tag);
        }
        return result;
    }

    // The "basic type" is int, float, bool, and String (reference type).
    private static Object readBasicValue(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        switch (tag) {
            case "int":
            case "float":
            case "bool":
            case "string":
                break;
            default:
                return null;
        }
        String text = null;
        int eventType;
        while ((eventType = parser.next()) != parser.END_DOCUMENT) {
            if (eventType == XmlPullParser.TEXT) {
                text = parser.getText();
            } else if (eventType == XmlPullParser.END_TAG) {
                if (tag.equals(parser.getName())) {
                    if (text == null) {
                        throw new XmlPullParserException("Need value in <" + tag + ">");
                    }
                    try {
                        switch (tag) {
                            case "int":
                                return Integer.valueOf(text);
                            case "float":
                                return Float.valueOf(text);
                            case "bool":
                                return ConversionUtils.str2BooleanStrict(text);
                            case "string":
                                return text;
                            default:
                                throw new AssertionError("Unreachable");
                        }
                    } catch (IllegalArgumentException e) {
                        throw new XmlPullParserException("<" + tag + "> required a valid value, but got " + text);
                    }
                }
                throw new XmlPullParserException("Unexpected end tag in <" + tag + ">: " + parser.getName());
            } else if (eventType == XmlPullParser.START_TAG) {
                throw new XmlPullParserException("Unexpected start tag in <" + tag + ">: " + parser.getName());
            }
        }
        throw new XmlPullParserException("Unexpected end of document in <" + tag + ">");
    }

    private static Set<Object> readSetValue(XmlPullParser parser) throws IOException, XmlPullParserException {
        HashSet<Object> set = new HashSet<>();
        int eventType = parser.getEventType();
        do {
            if (eventType == XmlPullParser.START_TAG) {
                set.add(readValue(parser, parser.getName()));
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("set".equals(parser.getName())) {
                    return set;
                }
                throw new XmlPullParserException("Unexpected end tag in <set>: " + parser.getName());
            }
            eventType = parser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);
        throw new XmlPullParserException("Document ended before <set> end tag");
    }
}

