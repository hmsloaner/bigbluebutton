package org.bigbluebutton.api.service.impl;

import org.bigbluebutton.api.model.entity.*;
import org.bigbluebutton.api.service.XmlService;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import org.springframework.data.domain.*;
import org.w3c.dom.NodeList;

public class XmlServiceImpl implements XmlService {

    private static Logger logger = LoggerFactory.getLogger(XmlServiceImpl.class);

    private DocumentBuilderFactory factory;
    private DocumentBuilder builder;

    @Override
    public String recordingsToXml(Collection<Recording> recordings) {
        logger.info("Converting {} recordings to xml", recordings.size());
        try {
            setup();
            Document document = builder.newDocument();

            Element rootElement = createElement(document, "recordings", null);
            document.appendChild(rootElement);

            String xml;
            Document secondDoc;
            Node node;

            for(Recording recording: recordings) {
                xml = recordingToXml(recording);
                secondDoc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
                node = document.importNode(secondDoc.getDocumentElement(), true);
                rootElement.appendChild(node);
            }

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String recordingToXml(Recording recording) {
//        logger.info("Converting {} to xml", recording);
        try {
            setup();
            Document document = builder.newDocument();

            Element rootElement = createElement(document,"recording", null);
            document.appendChild(rootElement);
            appendFields(document, rootElement, recording, new String[] {"id", "metadata", "format", "callbackData"}, Type.CHILD);

            Element meta = createElement(document, "meta", null);
            rootElement.appendChild(meta);

            String xml;
            Document secondDoc;
            Node node;

            if(recording.getMetadata() != null) {
                for(Metadata metadata: recording.getMetadata()) {
                    xml = metadataToXml(metadata);
                    secondDoc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
                    node = document.importNode(secondDoc.getDocumentElement(), true);
                    meta.appendChild(node);
                }
            }

            if(recording.getFormat() != null) {
                xml = playbackFormatToXml(recording.getFormat());
                secondDoc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
                node = document.importNode(secondDoc.getDocumentElement(), true);
                rootElement.appendChild(node);
            }

            if(recording.getCallbackData() != null) {
                xml = callbackDataToXml(recording.getCallbackData());
                secondDoc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
                node = document.importNode(secondDoc.getDocumentElement(), true);
                rootElement.appendChild(node);
            }

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String metadataToXml(Metadata metadata) {
//        logger.info("Converting {} to xml", metadata);

        try {
            setup();
            Document document = builder.newDocument();

            Element rootElement = createElement(document, metadata.getKey(), metadata.getValue());
            document.appendChild(rootElement);

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String playbackFormatToXml(PlaybackFormat playbackFormat) {
//        logger.info("Converting {} to xml", playbackFormat);

        try {
            setup();
            Document document = builder.newDocument();

            Element rootElement = createElement(document, "playback", null);
            document.appendChild(rootElement);
            appendFields(document, rootElement, playbackFormat, new String[] {"id", "recording", "thumbnails"}, Type.CHILD);

            if(playbackFormat.getThumbnails() != null && !playbackFormat.getThumbnails().isEmpty()) {
                Element images = createElement(document, "images", null);
                rootElement.appendChild(images);

                List<Thumbnail> thumbnails = new ArrayList<>(playbackFormat.getThumbnails());
                Collections.sort(thumbnails);

                for(Thumbnail thumbnail: thumbnails) {
                    String xml = thumbnailToXml(thumbnail);
                    Document thumbnailDoc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
                    Node node = document.importNode(thumbnailDoc.getDocumentElement(), true);
                    images.appendChild(node);
                }
            }

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String thumbnailToXml(Thumbnail thumbnail) {
//        logger.info("Converting {} to xml", thumbnail);

        try {
            setup();
            Document document = builder.newDocument();

            Element rootElement = createElement(document, "image", thumbnail.getUrl());
            document.appendChild(rootElement);
            appendFields(document, rootElement, thumbnail, new String[] {"id", "url", "playbackFormat"}, Type.ATTRIBUTE);

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String callbackDataToXml(CallbackData callbackData) {
//        logger.info("Converting {} to xml", callbackData);

        try {
            setup();
            Document document = builder.newDocument();

            Element rootElement = createElement(document, "callback", null);
            document.appendChild(rootElement);
            appendFields(document, rootElement, callbackData, new String[] {"id", "recording"}, Type.CHILD);

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String constructResponseFromRecordingsXml(String xml) {
        logger.info("Constructing response from recordings xml");

        try {
            setup();
            Document document = builder.newDocument();

            Element rootElement = createElement(document, "response", null);
            document.appendChild(rootElement);

            Element returnCode = createElement(document, "returncode", "SUCCESS");
            rootElement.appendChild(returnCode);

            Document recordingsDoc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            Node recordingsNode = document.importNode(recordingsDoc.getDocumentElement(), true);
            rootElement.appendChild(recordingsNode);

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String noRecordings() {
        logger.info("Constructing no recordings response");

        try {
            setup();
            Document document = builder.newDocument();

            Element rootElement = createElement(document, "response", null);
            document.appendChild(rootElement);

            Element returnCode = createElement(document, "returncode", "SUCCESS");
            rootElement.appendChild(returnCode);

            Element messageKey = createElement(document, "messageKey", "noRecordings");
            rootElement.appendChild(messageKey);

            Element message = createElement(document, "message", "No recordings found. This may occur if you attempt to retrieve all recordings.");
            rootElement.appendChild(message);

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String constructPaginatedResponse(Page<?> page, int offset, String response) {
        logger.info("Constructing paginated response");

        try {
            setup();

            if(response == null || "".equals(response)) {
                return null;
            }

            Document document = builder.parse(new ByteArrayInputStream(response.getBytes()));
            Element rootElement = document.getDocumentElement();

            Element pagination = createElement(document, "pagination", null);

            String xml;
            Document secondDoc;
            Node node;

            xml = pageableToXml(page.getPageable(), offset);
            secondDoc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            node = document.importNode(secondDoc.getDocumentElement(), true);
            pagination.appendChild(node);

            Element totalElements = createElement(document, "totalElements", String.valueOf(page.getTotalElements()));
            pagination.appendChild(totalElements);

//            Element last = createElement(document, "last", String.valueOf(page.isLast()));
//            pagination.appendChild(last);

//            Element totalPages = createElement(document, "totalPages", String.valueOf(page.getTotalPages()));
//            pagination.appendChild(totalPages);

//            Element first = createElement(document, "first", String.valueOf(page.isFirst()));
//            pagination.appendChild(first);

            Element empty = createElement(document, "empty", String.valueOf(!page.hasContent()));
            pagination.appendChild(empty);

            rootElement.appendChild(pagination);

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String pageableToXml(Pageable pageable, int o) {
        logger.info("Converting {} to xml", pageable);

        try {
            setup();
            Document document = builder.newDocument();

            Element rootElement = createElement(document, "pageable", null);
            document.appendChild(rootElement);

//            Sort sort = pageable.getSort();
//            Element sortElement = createElement(document, "sort", null);
//
//            Element unsorted = createElement(document, "unsorted", String.valueOf(sort.isUnsorted()));
//            sortElement.appendChild(unsorted);
//
//            Element sorted = createElement(document, "sorted", String.valueOf(sort.isSorted()));
//            sortElement.appendChild(sorted);
//
//            Element empty = createElement(document, "empty", String.valueOf(sort.isEmpty()));
//            sortElement.appendChild(empty);
//
//            rootElement.appendChild(sortElement);

            Element offset = createElement(document, "offset", String.valueOf(o));
            rootElement.appendChild(offset);

            Element limit = createElement(document, "limit", String.valueOf(pageable.getPageSize()));
            rootElement.appendChild(limit);

//            Element pageNumber = createElement(document, "pageNumber", String.valueOf(pageable.getPageNumber()));
//            rootElement.appendChild(pageNumber);

            Element paged = createElement(document, "paged", String.valueOf(pageable.isPaged()));
            rootElement.appendChild(paged);

            Element unpaged = createElement(document, "unpaged", String.valueOf(pageable.isUnpaged()));
            rootElement.appendChild(unpaged);

            String result = documentToString(document);
//            logger.info("========== Result ==========");
//            logger.info("{}", result);
//            logger.info("============================");
            return result;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Recording xmlToRecording(String recordId, String xml) {
        try {
            setup();
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            Recording recording = parseRecordingDocument(document);

            if (recording.getRecordId() == null || "".equals(recording.getRecordId()))
                recording.setRecordId(recordId);

            return recording;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Recording parseRecordingDocument(Document recordingDocument) {
        String id = getNodeData(recordingDocument, "id");
        String state = getNodeData(recordingDocument, "state");
        String published = getNodeData(recordingDocument, "published");
        String startTime = getNodeData(recordingDocument, "start_time");
        String endTime = getNodeData(recordingDocument, "end_time");
        String participants = getNodeData(recordingDocument, "participants");
        String externalId = getNodeData(recordingDocument, "externalId");
        String name = getNodeData(recordingDocument, "name");

        if (tagExists(recordingDocument, "meeting")) {
            Element meeting = (Element) recordingDocument.getElementsByTagName("meeting").item(0);
            externalId = meeting.getAttribute("externalId");
            name = meeting.getAttribute("name");
            if (id == null || "".equals(id))
                id = meeting.getAttribute("id");
        }

        Recording recording = new Recording();
        recording.setRecordId(id);
        recording.setMeetingId(externalId);
        recording.setName(name);
        recording.setPublished(Boolean.parseBoolean(published));
        recording.setState(state);

        try {
            recording.setStartTime(
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(startTime)), ZoneOffset.UTC));
            recording
                    .setEndTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(endTime)), ZoneOffset.UTC));
            recording.setParticipants(Integer.parseInt(participants));
        } catch (NumberFormatException e) {
        }

        parseMetadata(recordingDocument, recording);
        PlaybackFormat playback = parsePlaybackFormat(recordingDocument);
        recording.setFormat(playback);
        playback.setRecording(recording);

        logger.info("Finished constructing recording: {}", recording);

        return recording;
    }

    private void parseMetadata(Document recordingDocument, Recording recording) {
        Node meta = recordingDocument.getElementsByTagName("meta").item(0);
        NodeList children = meta.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (!(node instanceof Element))
                continue;

            String key = node.getNodeName();
            String value = node.getTextContent();

            Metadata metadata = new Metadata();
            metadata.setKey(key);
            metadata.setValue(value);

            logger.info("Finished constructing metadata: {}", metadata);

            recording.addMetadata(metadata);
        }
    }

    private PlaybackFormat parsePlaybackFormat(Document recordingDocument) {
        PlaybackFormat playback = new PlaybackFormat();

        String format = getNodeData(recordingDocument, "format");
        playback.setFormat(format);

        String url = getNodeData(recordingDocument, "link");
        playback.setUrl(url);

        String length = getNodeData(recordingDocument, "duration");
        String processingTime = getNodeData(recordingDocument, "processingTime");

        try {
            playback.setLength(Integer.parseInt(length));
            playback.setProcessingTime(Integer.parseInt(processingTime));
        } catch (NumberFormatException e) {

        }

        NodeList images = recordingDocument.getElementsByTagName("image");

        for (int i = 0; i < images.getLength(); i++) {
            Element image = (Element) images.item(i);

            String height = image.getAttribute("height");
            String width = image.getAttribute("width");
            String alt = image.getAttribute("alt");
            String src = image.getTextContent();

            Thumbnail thumbnail = new Thumbnail();

            try {
                thumbnail.setHeight(Integer.parseInt(height));
                thumbnail.setWidth(Integer.parseInt(width));
            } catch (NumberFormatException e) {
            }

            thumbnail.setAlt(alt);
            thumbnail.setUrl(src);
            thumbnail.setSequence(i);

            logger.info("Finished constructing image: {}", image);

            playback.addThumbnail(thumbnail);
        }

        logger.info("Finished constructing playback format: {}", playback);

        return playback;
    }

    private void setup() throws ParserConfigurationException {
        if(factory == null) factory = DocumentBuilderFactory.newInstance();
        if(builder == null) builder = factory.newDocumentBuilder();
    }

    private Element createElement(Document document, String name, String value) {
        Element element = document.createElement(name);
        if(value != null) element.setTextContent(value);
        return element;
    }

    public String documentToString(Document document) {
        String output = null;

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            output = writer.toString();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return output;
    }

    private void appendFields(Document document, Element parent, Object object, String[] ignoredFields, Type type) throws IllegalAccessException {
        Field[] fields = object.getClass().getDeclaredFields();

        for(Field field: fields) {
            if(Arrays.stream(ignoredFields).anyMatch(field.getName()::equals)) continue;
            field.setAccessible(true);
            Object fieldValue = field.get(object);
            if(fieldValue != null) {
                if(fieldValue instanceof LocalDateTime) {
                    fieldValue = localDateTimeToEpoch((LocalDateTime) fieldValue);
                }

                switch(type) {
                    case CHILD:
                        Element child = createElement(document, field.getName(), fieldValue.toString());
                        parent.appendChild(child);
                        break;
                    case ATTRIBUTE:
                        parent.setAttribute(field.getName(), fieldValue.toString());
                        break;
                }
            }
        }

    }

    private String localDateTimeToEpoch(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return String.valueOf(instant.toEpochMilli());
    }

    private boolean tagExists(Document document, String tag) {
        NodeList node = document.getElementsByTagName(tag);
        if (node == null || node.getLength() == 0)
            return false;
        return true;
    }

    private String getNodeData(Document document, String tag) {
        String data = null;
        if (!tagExists(document, tag))
            return data;

        NodeList node = document.getElementsByTagName(tag);
        Element element = (Element) node.item(0);
        Node child = element.getFirstChild();

        if (child instanceof CharacterData) {
            CharacterData characterData = (CharacterData) child;
            data = characterData.getData();
        }

        return data;
    }

    private enum Type {
        CHILD,
        ATTRIBUTE
    }
}
