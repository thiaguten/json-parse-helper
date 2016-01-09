/*
 * #%L
 * %%
 * Copyright (C) 2015 - 2016 Thiago Gutenberg Carvalho da Costa.
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Thiago Gutenberg Carvalho da Costa. nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.thiaguten.json;

import org.bson.*;
import org.bson.codecs.BsonArrayCodec;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.json.JsonReader;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * This class helps with some parse feature for converting MongoDB BSON strict/shell mode into 'standard' JSON format.
 * <p>
 * <ul>
 * <li>
 * <pre>
 * {@code [BsonString{value='a'}, BsonString{value='b'}, BsonArray{values=[BsonString{value='c'}, BsonString{value='d'}, BsonArray{values=[BsonString{value='e'}, BsonString{value='f'}]}]}] }
 * to
 * {@code [ "a", "b", [ "c", "d", [ "e", "f" ] ] ] }
 * </pre>
 * </li>
 * <li>
 * <pre>
 * {@code { "_id" : { "$oid" : "5662e5798172910f5a925a43" }, "date" : { "$date" : 1449321849768 }, "pattern" : { "$regex" : "\\d", "$options" : "i" }, "long" : { "$numberLong" : "9223372036854775807" } } }
 * to
 * {@code { "_id" : "5662e5798172910f5a925a43", "date" : "2015-12-05T11:26:23.184", "pattern" : "\\d/i", "long" : 9223372036854775807 } }
 * </pre>
 * </li>
 * <li>
 * <pre>
 * {@code { "_id" : ObjectId("5662e5798172910f5a925a43"), "date" : ISODate("2015-12-05T13:26:23.184Z"), "pattern" : /\d/i, "long" : NumberLong("9223372036854775807") } }
 * to
 * {@code { "_id" : "5662e5798172910f5a925a43", "date" : "2015-12-05T11:26:23.184", "pattern" : "\\d/i", "long" : 9223372036854775807 } }
 * </pre>
 * </li>
 * </ul>
 *
 * @author Thiago Gutenberg Carvalho da Costa
 * @since 1.0.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class JsonParseHelper {

    private static final String SLASH = "/";
    private static final String BACKSLASH = "\\";
    private static final String COMMA = ", ";
    private static final String COLON = " : ";
    private static final String DOUBLE_QUOTES = "\"";
    private static final String JSON_OBJECT_START_TOKEN = "{ ";
    private static final String JSON_OBJECT_END_TOKEN = " }";
    private static final String JSON_ARRAY_START_TOKEN = "[ ";
    private static final String JSON_ARRAY_END_TOKEN = " ]";
    private static final String NULL = null;

    private JsonParseHelper() {
        throw new AssertionError();
    }

    /**
     * Parses strict mode representations of BSON types conform to the <a href="http://www.json.org">JSON RFC spec</a>
     * and MongoDB shell BSON document query results into 'standard' JSON format without strict BSON representations or BSON MongoDB types.
     *
     * @param json the JSON to parse
     * @return 'standard' JSON format
     */
    public static String parse(final String json) {
        if (json != null) {
            String j = json.trim();
            if (j.startsWith(JSON_OBJECT_START_TOKEN)) {
                return parse(parseBsonDocument(json)); // type safe
            } else if (j.startsWith(JSON_ARRAY_START_TOKEN)) {
                return parse(parseBsonArray(json)); // type safe
            } else {
                return json;
            }
        }
        return NULL;
    }

    /**
     * Parses {@link BsonArray} into 'standard' JSON format without strict BSON representations or BSON MongoDB types.
     *
     * @param list {@link BsonArray} or {@link List}
     * @return 'standard' JSON format
     */
    public static String parse(final List list) {
        StringBuilder builder = new StringBuilder(JSON_ARRAY_START_TOKEN);
        if (list != null && !list.isEmpty()) {
            parse(list, builder);
        }
        return builder.append(JSON_ARRAY_END_TOKEN).toString();
    }

    /**
     * Parses {@link BsonDocument} or {@link Document} into 'standard' JSON format without strict BSON representations or BSON MongoDB types.
     *
     * @param map {@link BsonDocument} or {@link Document}
     * @return 'standard' JSON format
     */
    public static String parse(final Map map) {
        StringBuilder builder = new StringBuilder(JSON_OBJECT_START_TOKEN);
        if (map != null && !map.isEmpty()) {
            parse(map, builder);
        }
        return builder.append(JSON_OBJECT_END_TOKEN).toString();
    }

    // PRIVATE METHODS

    private static BsonArray parseBsonArray(final String json) {
        return new BsonArrayCodec(CodecRegistries.fromProviders(new BsonValueCodecProvider())).decode(new JsonReader(json), DecoderContext.builder().build());
    }

    private static BsonDocument parseBsonDocument(final String json) {
        return BsonDocument.parse(json);
    }

    private static void parse(final List list, final StringBuilder builder) {
        AtomicInteger count = new AtomicInteger(1);
        for (Object value : list) {
            checkInstance(value, builder);
            if (count.getAndIncrement() < list.size()) {
                builder.append(COMMA);
            }
        }
    }

    private static void parse(final Map map, final StringBuilder builder) {
        AtomicInteger count = new AtomicInteger(1);
        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            builder.append(DOUBLE_QUOTES).append(entry.getKey()).append(DOUBLE_QUOTES).append(COLON);
            checkInstance(entry.getValue(), builder);
            if (count.getAndIncrement() < entries.size()) {
                builder.append(COMMA);
            }
        }
    }

    private static void checkInstance(Object value, StringBuilder builder) {
        if (value instanceof BsonValue) {
            BsonValue bsonValue = (BsonValue) value;
            switch (bsonValue.getBsonType()) {
                case REGULAR_EXPRESSION:
                    BsonRegularExpression bsonRegularExpression = bsonValue.asRegularExpression();
                    String pattern = BACKSLASH + bsonRegularExpression.getPattern();
                    String options = bsonRegularExpression.getOptions();
                    String consolidate = options != null && !options.trim().isEmpty() ? pattern + SLASH + options : pattern;
                    builder.append(DOUBLE_QUOTES).append(consolidate).append(DOUBLE_QUOTES);
                    break;
                case NULL:
                    builder.append(NULL);
                    break;
                case OBJECT_ID:
                    builder.append(DOUBLE_QUOTES).append(bsonValue.asObjectId().getValue().toHexString()).append(DOUBLE_QUOTES);
                    break;
                case DATE_TIME:
                    builder.append(DOUBLE_QUOTES).append(LocalDateTime.ofInstant(Instant.ofEpochMilli(bsonValue.asDateTime().getValue()), ZoneId.systemDefault())).append(DOUBLE_QUOTES);
                    break;
                case STRING:
                    builder.append(DOUBLE_QUOTES).append(bsonValue.asString().getValue()).append(DOUBLE_QUOTES);
                    break;
                case INT32:
                    builder.append(bsonValue.asInt32().getValue());
                    break;
                case INT64:
                    builder.append(bsonValue.asInt64().getValue());
                    break;
                case DOUBLE:
                    builder.append(bsonValue.asDouble().getValue());
                    break;
                case BOOLEAN:
                    builder.append(bsonValue.asBoolean().getValue());
                    break;
                case ARRAY:
                    builder.append(parse(bsonValue.asArray()));
                    break;
                case DOCUMENT:
                    builder.append(parse(bsonValue.asDocument()));
                    break;
                default:
                    // NOP
            }
        } else {
            if (value instanceof ObjectId) {
                builder.append(DOUBLE_QUOTES).append(((ObjectId) value).toHexString()).append(DOUBLE_QUOTES);
            }
            if (value instanceof List) {
                builder.append(parse((List) value));
            }
            if (value instanceof Map) {
                builder.append(parse((Map) value));
            }
            if (value instanceof Date) {
                Date date = (Date) value;
                builder.append(DOUBLE_QUOTES).append(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())).append(DOUBLE_QUOTES);
            }
            if (value instanceof String) {
                builder.append(DOUBLE_QUOTES).append(value).append(DOUBLE_QUOTES);
            }
            if (value instanceof Character) {
                builder.append(DOUBLE_QUOTES).append(value).append(DOUBLE_QUOTES);
            }
            if (value instanceof Pattern) {

                Pattern pattern = ((Pattern) value);
                String p = BACKSLASH + pattern.pattern();
                String flags = PatternFlag.of(pattern.flags()).getFlagAsString();
                String consolidate = flags != null && !flags.trim().isEmpty() ? p + SLASH + flags : p;

                builder.append(DOUBLE_QUOTES).append(consolidate).append(DOUBLE_QUOTES);
            }
            if (value instanceof Number) {
                builder.append(value);
            }
            if (value instanceof Boolean) {
                builder.append(value);
            }
            if (value instanceof AtomicBoolean) {
                builder.append(value);
            }
            if (value == null) {
                builder.append(NULL);
            }
        }
    }

    enum PatternFlag {

        D(Pattern.UNIX_LINES, "d"),
        I(Pattern.CASE_INSENSITIVE, "i"),
        C(Pattern.COMMENTS, "c"),
        M(Pattern.MULTILINE, "m"),
        S(Pattern.DOTALL, "s"),
        U(Pattern.UNICODE_CASE, "u"),
        UU(Pattern.UNICODE_CHARACTER_CLASS, "U");

        private final int flag;
        private final String flagAsString;

        PatternFlag(int flag, String flagAsString) {
            this.flag = flag;
            this.flagAsString = flagAsString;
        }

        public static PatternFlag of(int flag) {
            for (PatternFlag patternFlag : values()) {
                if (patternFlag.getFlag() == flag) {
                    return patternFlag;
                }
            }
            throw new RuntimeException("pattern flag not suported");
        }

        public int getFlag() {
            return flag;
        }

        public String getFlagAsString() {
            return flagAsString;
        }
    }

}
