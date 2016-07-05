/*-
 * #%L
 * JsonParseHelper
 * %%
 * Copyright (C) 2016 Thiago Gutenberg Carvalho da Costa
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package br.com.thiaguten.json;

import org.bson.*;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

import static br.com.thiaguten.json.JsonParseHelper.parse;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class JsonParseHelperTest {

    private static final Document document = new Document()
            .append("_id", new ObjectId("5662e5798172910f5a925a43"))
            .append("date", new Date(1449321983184L))
            .append("pattern", Pattern.compile("\\d", Pattern.CASE_INSENSITIVE))
            .append("pattern2", Pattern.compile("\\s"))
            .append("long", Long.MAX_VALUE)
            .append("null", null)
            .append("double", 1.0)
            .append("string", "thiago")
            .append("boolean", true)
            .append("doc", new Document("key", "value"))
            .append("list", Arrays.asList("value"))
            .append("map", Collections.singletonMap("key", "value"));

    private static final BsonDocument bsonDocument = new BsonDocument()
            .append("_id", new BsonObjectId(new ObjectId("5662e5798172910f5a925a43")))
            .append("date", new BsonDateTime(1449321983184L))
            .append("pattern", new BsonRegularExpression("\\d", "i"))
            .append("pattern2", new BsonRegularExpression("\\s"))
            .append("long", new BsonInt64(Long.MAX_VALUE))
            .append("null", new BsonNull())
            .append("double", new BsonDouble(1.0))
            .append("string", new BsonString("thiago"))
            .append("boolean", new BsonBoolean(true))
            .append("doc", new BsonDocument("key", new BsonString("value")))
            .append("list", new BsonArray(Arrays.asList(new BsonString("value"))))
            .append("map", new BsonDocument(Arrays.asList(new BsonElement("key", new BsonString("value")))));

    @Test
    public void constantsTest() {
        assertEquals("/", JsonParseHelper.SLASH);
        assertEquals("\\", JsonParseHelper.BACKSLASH);
        assertEquals(", ", JsonParseHelper.COMMA);
        assertEquals(":", JsonParseHelper.COLON);
        assertEquals("\"", JsonParseHelper.DOUBLE_QUOTES);
        assertEquals("{", JsonParseHelper.JSON_OBJECT_START_TOKEN);
        assertEquals("}", JsonParseHelper.JSON_OBJECT_END_TOKEN);
        assertEquals("[", JsonParseHelper.JSON_ARRAY_START_TOKEN);
        assertEquals("]", JsonParseHelper.JSON_ARRAY_END_TOKEN);
        assertEquals(null, JsonParseHelper.NULL);
        assertEquals(0, JsonParseHelper.INVALID_PATTERN_FLAG);
    }

    @Test
    public void illegalInstatiationUtillityClass() throws IllegalAccessException, InstantiationException, NoSuchMethodException {
        try {
            Constructor c = JsonParseHelper.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(c.getModifiers()));
            c.setAccessible(true);
            c.newInstance();
        } catch (InvocationTargetException e) {
            assertThat(e.getCause(), is(instanceOf(AssertionError.class)));
        }
    }

    @Test(expected = RuntimeException.class)
    public void patternTest() {
        JsonParseHelper.PatternFlag.of(JsonParseHelper.INVALID_PATTERN_FLAG);
    }

    @Test
    public void parseStringTest() {
        String json = "[1, \"string\", true]";
        assertEquals(json, parse(json));
        json = null;
        assertEquals(null, parse(json));
        json = "";
        assertEquals(json, parse(json));
    }

    @Test
    public void parseMapTest() {
        String empty = "{}";
        String notEmpty = "{\"key\":\"value\"}";
        assertEquals(empty, JsonParseHelper.parse((Map) null));
        assertEquals(empty, JsonParseHelper.parse(Collections.emptyMap()));
        assertEquals(notEmpty, parse(Collections.singletonMap("key", "value")));
        assertEquals(notEmpty, parse(new Document("key", "value")));
        assertEquals(notEmpty, parse(new BsonDocument(Arrays.asList(new BsonElement("key", new BsonString("value"))))));
    }

    @Test
    public void parseListTest() {
        String empty = "[]";
        String notEmpty = "[\"value\"]";
        assertEquals(empty, JsonParseHelper.parse((List) null));
        assertEquals(empty, parse(Collections.emptyList()));
        assertEquals(notEmpty, parse(Arrays.asList("value")));
        assertEquals(notEmpty, parse(new BsonArray(Arrays.asList(new BsonString("value")))));
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
        assertEquals("[\"a\", \"b\", [\"c\", \"d\", [\"e\", \"f\"]]]", parse(bsonArray3));
    }

    @Test
    public void bsonDocumentTest() {
        assertNotNull(bsonDocument);
        assertEquals("{\"_id\":\"5662e5798172910f5a925a43\", \"date\":\"2015-12-05T11:26:23.184\", \"pattern\":\"\\\\d/i\", \"pattern2\":\"\\\\s\", \"long\":9223372036854775807, \"null\":null, \"double\":1.0, \"string\":\"thiago\", \"boolean\":true, \"doc\":{\"key\":\"value\"}, \"list\":[\"value\"], \"map\":{\"key\":\"value\"}}", parse(bsonDocument));
    }

    @Test
    public void bsonDocumentStrictTest() {
        assertNotNull(bsonDocument);
        assertEquals("{\"_id\":\"5662e5798172910f5a925a43\", \"date\":\"2015-12-05T11:26:23.184\", \"pattern\":\"\\\\d/i\", \"pattern2\":\"\\\\s\", \"long\":9223372036854775807, \"null\":null, \"double\":1.0, \"string\":\"thiago\", \"boolean\":true, \"doc\":{\"key\":\"value\"}, \"list\":[\"value\"], \"map\":{\"key\":\"value\"}}", parse(bsonDocument.toJson()));
    }

    @Test
    public void bsonDocumentShellTest() {
        assertNotNull(bsonDocument);
        assertEquals("{\"_id\":\"5662e5798172910f5a925a43\", \"date\":\"2015-12-05T11:26:23.184\", \"pattern\":\"\\\\d/i\", \"pattern2\":\"\\\\s\", \"long\":9223372036854775807, \"null\":null, \"double\":1.0, \"string\":\"thiago\", \"boolean\":true, \"doc\":{\"key\":\"value\"}, \"list\":[\"value\"], \"map\":{\"key\":\"value\"}}", parse(bsonDocument.toJson(new JsonWriterSettings(JsonMode.SHELL))));
    }

    @Test
    public void documentTest() {
        assertNotNull(document);
        assertEquals("{\"_id\":\"5662e5798172910f5a925a43\", \"date\":\"2015-12-05T11:26:23.184\", \"pattern\":\"\\\\d/i\", \"pattern2\":\"\\\\s\", \"long\":9223372036854775807, \"null\":null, \"double\":1.0, \"string\":\"thiago\", \"boolean\":true, \"doc\":{\"key\":\"value\"}, \"list\":[\"value\"], \"map\":{\"key\":\"value\"}}", parse(document));
    }

    @Test
    public void documentStrictTest() {
        assertNotNull(document);
        assertEquals("{\"_id\":\"5662e5798172910f5a925a43\", \"date\":\"2015-12-05T11:26:23.184\", \"pattern\":\"\\\\d/i\", \"pattern2\":\"\\\\s\", \"long\":9223372036854775807, \"null\":null, \"double\":1.0, \"string\":\"thiago\", \"boolean\":true, \"doc\":{\"key\":\"value\"}, \"list\":[\"value\"], \"map\":{\"key\":\"value\"}}", parse(document.toJson(new JsonWriterSettings(JsonMode.STRICT))));
    }

    @Test
    public void documentShellTest() {
        assertNotNull(document);
        assertEquals("{\"_id\":\"5662e5798172910f5a925a43\", \"date\":\"2015-12-05T11:26:23.184\", \"pattern\":\"\\\\d/i\", \"pattern2\":\"\\\\s\", \"long\":9223372036854775807, \"null\":null, \"double\":1.0, \"string\":\"thiago\", \"boolean\":true, \"doc\":{\"key\":\"value\"}, \"list\":[\"value\"], \"map\":{\"key\":\"value\"}}", parse(document.toJson(new JsonWriterSettings(JsonMode.SHELL))));
    }

}
