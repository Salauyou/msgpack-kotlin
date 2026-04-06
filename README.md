## msgpack-kotlin

- Fresh take on old good [msgpack](https://msgpack.org]): support of Kotlin data classes and immutable collections
- Normalization module for [Jackson](https://github.com/FasterXML/jackson): transformation without custom deserializers

Example: deep internation in Jackson deserialization
```kotlin
// mark classes that need internation
@Normalize
data class Person(val name: String, val surname: String, val height: Int)

data class Team(val members: Map<String, Person>)

// create interner
val interner = object : Normalizer {
  private val cache = ConcurrentHashMap<Any, Any>()
  override fun normalize(value: Any) = cache.computeIfAbsent(value) { _ -> value }
}

// register NormalizeModule with interner
val objectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerNormalizeModule(interner)

val json = """{
  "members" : {
    "alex": { "name": "Alex", "surname": "Smith", "height": 193 },
    "john": { "name": "John", "surname": "Smith", "height": 178 },
    "natalie": { "name": "Natalie", "surname": "Darth", "height": 178 }
  }
}"""

// internation will apply on Person instances and all nested properties recursively
val team = objectMapper.readValue(json, Team::class.java)
```
