/*
 * Copyright 2020-2020, João Vitor de Sá Hauck
 * 
 * This file is part of Indexador e Processador de Evidencias Digitais (IPED).
 *
 * IPED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IPED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IPED.  If not, see <http://www.gnu.org/licenses/>.
 */
package dpf.ap.gpinf.telegramextractor;

import dpf.sp.gpinf.indexer.parsers.IndexerDefaultParser;
import dpf.sp.gpinf.indexer.parsers.jdbc.SQLite3DBParser;
import dpf.sp.gpinf.indexer.parsers.jdbc.SQLite3Parser;

import iped3.search.IItemSearcher;

import iped3.util.ExtraProperties;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class TelegramParser extends SQLite3DBParser {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2981296547291818873L;
	public static final MediaType TELEGRAM_DB=MediaType.parse("application/x-telegram-db");
	public Set<MediaType> getSupportedTypes(ParseContext context){
	    return MediaType.set(TELEGRAM_DB);
	}
	public static final MediaType TELEGRAM_CHAT = MediaType.parse("application/x-telegram-chat");
	
	private SQLite3Parser sqliteParser = new SQLite3Parser();
	public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
	throws IOException,SAXException{
		Connection conn= getConnection(stream,metadata,context);
		Extractor e=new Extractor(conn);
		IItemSearcher searcher = context.get(IItemSearcher.class);
		e.setSearcher(searcher);
		e.performExtraction();
		ReportGenerator r=new ReportGenerator();
		r.setSearcher(searcher);
		EmbeddedDocumentExtractor extractor = context.get(EmbeddedDocumentExtractor.class,
                new ParsingEmbeddedDocumentExtractor(context));
		
		for(Chat c:e.getChatList()) {
			//System.out.println("teste telegram");
			byte[] bytes=r.generateChatHtml(c);
			Metadata chatMetadata = new Metadata();
			String title="Telegram_";
			if(c.isGroup()) {
				title+="Group";
			}else {
				title+="Chat";
			}
			title+="_"+c.getName();
			chatMetadata.set(TikaCoreProperties.TITLE, title);
	        chatMetadata.set(IndexerDefaultParser.INDEXER_CONTENT_TYPE, TELEGRAM_CHAT.toString());
	        chatMetadata.set(ExtraProperties.ITEM_VIRTUAL_ID, Long.toString(c.getId()));
	         
	        ByteArrayInputStream chatStream = new ByteArrayInputStream(bytes);
	        extractor.parseEmbedded(chatStream, handler, chatMetadata, false);
	         
		
			
		}
		try {
			conn.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}