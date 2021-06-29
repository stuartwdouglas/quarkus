package io.quarkus.logback.runtime.events;

import java.util.Objects;

import org.xml.sax.Attributes;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class AttributesImpl implements Attributes {

    public Attribute[] attributus;

    public AttributesImpl(Attributes at) {
        attributus = new Attribute[at.getLength()];
        for (int i = 0; i < at.getLength(); ++i) {
            attributus[i] = new Attribute(at.getLocalName(i), at.getValue(i), at.getType(i), at.getURI(i), at.getQName(i));
        }
    }

    public AttributesImpl() {

    }

    @Override
    public int getLength() {
        return attributus.length;
    }

    @Override
    public String getURI(int index) {
        return attributus[index].uri;
    }

    @Override
    public String getLocalName(int index) {
        return attributus[index].localName;
    }

    @Override
    public String getQName(int index) {
        return attributus[index].qName;
    }

    @Override
    public String getType(int index) {
        return attributus[index].type;
    }

    @Override
    public String getValue(int index) {
        return attributus[index].value;
    }

    @Override
    public int getIndex(String uri, String localName) {
        for (int i = 0; i < attributus.length; ++i) {
            Attribute at = attributus[i];
            if (Objects.equals(uri, at.uri) && Objects.equals(localName, at.localName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getIndex(String qName) {
        for (int i = 0; i < attributus.length; ++i) {
            Attribute at = attributus[i];
            if (Objects.equals(qName, at.qName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getType(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            return null;
        }
        return attributus[index].type;
    }

    @Override
    public String getType(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        }
        return attributus[index].type;
    }

    @Override
    public String getValue(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            return null;
        }
        return attributus[index].value;
    }

    @Override
    public String getValue(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        }
        return attributus[index].value;
    }

    public static class Attribute {
        public final String localName;
        public final String value;
        public final String type;
        public final String uri;
        public final String qName;

        @RecordableConstructor
        public Attribute(String localName, String value, String type, String uri, String qName) {
            this.localName = localName;
            this.value = value;
            this.type = type;
            this.uri = uri;
            this.qName = qName;
        }
    }
}
