package br.com.thiaguten.json;

import org.bson.*;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.regex.Pattern;

import static br.com.thiaguten.json.JsonParseHelper.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonParseHelperTest {

    private static Document document;
    private static BsonDocument bsonDocument;

    @BeforeClass
    public static void setUp() {
        bsonDocument = new BsonDocument()
                .append("_id", new BsonObjectId(new ObjectId("5662e5798172910f5a925a43")))
                .append("date", new BsonDateTime(1449321983184L))
                .append("pattern", new BsonRegularExpression("\\d", "i"))
                .append("long", new BsonInt64(Long.MAX_VALUE))
        ;

        document = new Document()
                .append("_id", new ObjectId("5662e5798172910f5a925a43"))
                .append("date", new Date(1449321983184L))
                .append("pattern", Pattern.compile("\\d", Pattern.CASE_INSENSITIVE))
                .append("long", Long.MAX_VALUE)
        ;
    }

    @Test
    public void bsonArrayTest() {
        BsonArray bsonArray = new BsonArray();
        bsonArray.add(new BsonString("e"));
        bsonArray.add(new BsonString("f"));

        BsonArray bsonArray2 = new BsonArray();
        bsonArray2.add(new BsonString("c"));
        bsonArray2.add(new BsonString("d"));
        bsonArray2.add(new BsonArray(bsonArray));

        BsonArray bsonArray3 = new BsonArray();
        bsonArray3.add(new BsonString("a"));
        bsonArray3.add(new BsonString("b"));
        bsonArray3.add(new BsonArray(bsonArray2));

        assertNotNull(bsonArray3);
        assertEquals("[ \"a\", \"b\", [ \"c\", \"d\", [ \"e\", \"f\" ] ] ]", parse(bsonArray3));
    }

    @Test
    public void bsonDocumentTest() {
        assertNotNull(bsonDocument);
        assertEquals("{ \"_id\" : \"5662e5798172910f5a925a43\", \"date\" : \"2015-12-05T11:26:23.184\", \"pattern\" : \"\\\\d/i\", \"long\" : 9223372036854775807 }", parse(bsonDocument));
    }

    @Test
    public void bsonDocumentStrictTest() {
        assertNotNull(bsonDocument);
        assertEquals("{ \"_id\" : \"5662e5798172910f5a925a43\", \"date\" : \"2015-12-05T11:26:23.184\", \"pattern\" : \"\\\\d/i\", \"long\" : 9223372036854775807 }", parse(bsonDocument.toJson()));
    }

    @Test
    public void bsonDocumentShellTest() {
        assertNotNull(bsonDocument);
        assertEquals("{ \"_id\" : \"5662e5798172910f5a925a43\", \"date\" : \"2015-12-05T11:26:23.184\", \"pattern\" : \"\\\\d/i\", \"long\" : 9223372036854775807 }", parse(bsonDocument.toJson(new JsonWriterSettings(JsonMode.SHELL))));
    }

    @Test
    public void documentTest() {
        assertNotNull(document);
        assertEquals("{ \"_id\" : \"5662e5798172910f5a925a43\", \"date\" : \"2015-12-05T11:26:23.184\", \"pattern\" : \"\\\\d/i\", \"long\" : 9223372036854775807 }", parse(document));
    }

    @Test
    public void documentStrictTest() {
        assertNotNull(document);
        assertEquals("{ \"_id\" : \"5662e5798172910f5a925a43\", \"date\" : \"2015-12-05T11:26:23.184\", \"pattern\" : \"\\\\d/i\", \"long\" : 9223372036854775807 }", parse(document.toJson(new JsonWriterSettings(JsonMode.STRICT))));
    }

    @Test
    public void documentShellTest() {
        assertNotNull(document);
        assertEquals("{ \"_id\" : \"5662e5798172910f5a925a43\", \"date\" : \"2015-12-05T11:26:23.184\", \"pattern\" : \"\\\\d/i\", \"long\" : 9223372036854775807 }", parse(document.toJson(new JsonWriterSettings(JsonMode.SHELL))));
    }

}
