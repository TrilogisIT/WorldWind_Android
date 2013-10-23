/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.xml.AbstractXMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEvent;
import gov.nasa.worldwind.util.xml.XMLEventParserContext;
import gov.nasa.worldwind.util.xml.XMLParserException;
import java.util.ArrayList;

/**
 * Parses KML <i>coordinates</i> elements.
 * 
 * @author tag
 * @version $Id: KMLCoordinatesParser.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLCoordinatesParser extends AbstractXMLEventParser {
	public KMLCoordinatesParser() {
	}

	public KMLCoordinatesParser(String namespaceURI) {
		super(namespaceURI);
	}

	public Position.PositionList parse(XMLEventParserContext ctx, XMLEvent doubleEvent, Object... args) throws XMLParserException {
		String s = ctx.getStringParser().parseString(ctx, doubleEvent);
		if (s == null || s.length() < 3) // "a,b" is the smallest possible coordinate string
		return null;

		ArrayList<Position> positions = new ArrayList<Position>();

		KMLCoordinateTokenizer tokenizer = new KMLCoordinateTokenizer(s);

		while (tokenizer.hasMoreTokens()) {
			try {
				positions.add(tokenizer.nextPosition());
			} catch (NumberFormatException e) {
				continue; // TODO: issue warning?
			} catch (NullPointerException e) {
				continue; // TODO: issue warning?
			} catch (Exception e) {
				continue; // TODO: issue warning
			}
		}

		return new Position.PositionList(positions);
	}
}
