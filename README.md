# JsonParseHelper

[![Build Status](https://travis-ci.org/thiaguten/json-parse-helper.svg)](https://travis-ci.org/thiaguten/json-parse-helper)

[![Coverage Status](https://coveralls.io/repos/github/thiaguten/json-parse-helper/badge.svg?branch=master)](https://coveralls.io/github/thiaguten/json-parse-helper?branch=master)

JsonParseHelper is a utility class with parse feature for converting MongoDB BSON strict/shell mode into 'standard' JSON format useful for instance for REST services with MongoDB.

## Examples

BsonDocument or Document Shell/Strict mode to "Standard" JSON format:

```json
[BsonString{value='a'}, BsonString{value='b'}, BsonArray{values=[BsonString{value='c'}, BsonString{value='d'}, BsonArray{values=[BsonString{value='e'}, BsonString{value='f'}]}]}]
```

to

```json
[ "a", "b", [ "c", "d", [ "e", "f" ] ] ]
```

or

```json
{ "_id" : { "$oid" : "5662e5798172910f5a925a43" }, "date" : { "$date" : 1449321849768 }, "pattern" : { "$regex" : "\\d", "$options" : "i" }, "long" : { "$numberLong" : "9223372036854775807" } }
```

to

```json
{ "_id" : "5662e5798172910f5a925a43", "date" : "2015-12-05T11:26:23.184", "pattern" : "\\d/i", "long" : 9223372036854775807 }
```

or

```json
{ "_id" : ObjectId("5662e5798172910f5a925a43"), "date" : ISODate("2015-12-05T13:26:23.184Z"), "pattern" : /\d/i, "long" : NumberLong("9223372036854775807") }
```

to 

```json
{ "_id" : "5662e5798172910f5a925a43", "date" : "2015-12-05T11:26:23.184", "pattern" : "\\d/i", "long" : 9223372036854775807 }
```

