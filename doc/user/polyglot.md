# Polyglot Programming

TruffleRuby allows you to interface with any other Truffle language to create
polyglot programs -- programs written in more than one language.

This guide describes how to load code written in foreign languages, how to
export and import objects between languages, how to use Ruby objects from
a foreign language, how to use foreign objects from Ruby, how to load
Java types to interface with Java, and how to embed in Java.

If you are using the native configuration, you will need to use the `--polyglot`
flag to get access to other languages. The JVM configuration automatically has
access to other languages.

* [Running Ruby code from another language](#running-ruby-code-from-another-language)
* [Loading code written in foreign languages](#loading-code-written-in-foreign-languages)
* [Exporting Ruby objects to foreign languages](#exporting-ruby-objects-to-foreign-languages)
* [Importing foreign objects to Ruby](#importing-foreign-objects-to-ruby)
* [Using Ruby objects from a foreign language](#using-ruby-objects-from-a-foreign-language)
* [Using foreign objects from Ruby](#using-foreign-objects-from-ruby)
* [Accessing Java objects](#accessing-java-objects)
* [Strings](#strings)
* [Threading and interop](#threading-and-interop)
* [Embedded configuration](#embedded-configuration)

## Running Ruby Code from Another Language

When you `eval` Ruby code from the [Context API](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html)
in another language and mark the `Source` as interactive, the same interactive
top-level binding is used each time. This means that if you set a local variable
in one `eval`, you will be able to use it from the next.

The semantics are the same as the Ruby semantics of calling
`INTERACTIVE_BINDING.eval(code)` for every `Context.eval()` call with an
interactive `Source`. This is similar to most REPL semantics.

## Loading Code Written in Foreign Languages

`Polyglot.eval(id, string)` executes code in a foreign language identified by
its ID.

`Polyglot.eval_file(id, path)` executes code in a foreign language from a file,
identified by its language ID.

`Polyglot.eval_file(path)` executes code in a foreign language from a file,
automatically determining the language.

## Exporting Ruby Objects to Foreign Languages

`Polyglot.export(name, value)` exports a value with a given name.

`Polyglot.export_method(name)` exports a method, defined in the top-level
object.

## Importing Foreign Objects to Ruby

`Polyglot.import(name)` imports and returns a value with a given name.

`Polyglot.import_method(name)` imports a value, which should be `IS_EXECUTABLE`,
with a given name, and defines it in the top-level object.

## Using Ruby Objects from a Foreign Language

Using JavaScript as an example - the left example is JavaScript, the right one
is the corresponding action it takes on the Ruby object expressed in Ruby code.

`object[name/index]` calls `object[name/index]` if the object has a method `[]`,
or reads an instance variable if the name starts with `@`, or returns a bound
method with the name.

`object[name/index] = value` calls `object[name/index] = value` if the object
has a method `[]=`, or sets an instance variable if the name starts with `@`.

`delete object.name` calls `object.delete(name)`.

`delete object[name/index]` calls `object.delete(name)`.

`object.length` calls `object.size`.

`Object.keys(hash)` gives the hash keys as strings.

`Object.keys(object)` gives the methods of an object as functions, unless the
object has a `[]` method, in which case it returns an empty array.

`object(args...)` calls a Ruby `Proc`, `Method`, `UnboundMethod`, etc.

`object.name(args...)` calls a method on the Ruby object.

`new object(args...)` calls `object.new(args...)`.

`"length" in obj` returns `true` for a Ruby `Array`.

`object == null` calls `object.nil?`.

### Notes on creating Ruby objects for use in foreign languages

If you want to pass a Ruby object to another language for fields to be read and
written, a good object to pass is usually a `Struct`, as this will have both the
`object.foo` and `object.foo = value` accessors for you to use from Ruby, and
they will also respond to `object['foo']` and `object['foo'] = value` which
means they will work from other languages sending read and write messages.

## Using Foreign Objects from Ruby

`object[name/index]` will read a member from the foreign object.

`object[name/index] = value` will write a value to the the foreign object.

`object.delete(name/index)` will remove a value from the foreign object.

`object.size` will get the size or length of the foreign object.

`object.keys` will get an array of the members of the foreign object.

`object.call(*args)` will execute the foreign object.

`object.name(*args)` will invoke a method called `name` on the foreign object.

`object.new(*args)` will create a new object from the foreign object (as if it's
some kind of class.)

`object.class` on a Java `Class` object will give you an object on which you
can call instance methods, rather than static methods.

`object.respond_to?(:size)` will tell you if the foreign object has a size or
length.

`object.nil?` will tell you if the foreign object represents the language's
equivalent of `null` or `nil`.

`object.respond_to?(:call)` will tell you if a foreign object can be executed.

`object.respond_to?(:new)` will tell you if a foreign object can be used to
create a new object (if it's a class).

`object.respond_to?(:keys)` will tell you if a foreign object can give you a
list of members.

`object.respond_to?(:class)` will tell you if an object is a Java class.

`Polyglot.as_enumerable(object)` will create a Ruby `Enumerable` from the
foreign object, using its size or length and reading from it.

Where boolean value is expected (e.g. in if conditions) the foreign value is
converted to boolean if possible or considered to be true.

## Accessing Java Objects

TruffleRuby's Java interoperability interface is similar to the interface from the
Nashorn JavaScript implementation, as also implemented by GraalVM JavaScript.

It is easier to use Java interoperability in JVM mode (`--jvm`).
Java interoperability is also supported in native mode but requires more setup.
See [here](https://www.graalvm.org/reference-manual/embed-languages/#build-native-images-from-polyglot-applications)
for more details.

`Java.type('name')` returns a Java class object, given a name such as
`java.lang.Integer` or `int[]`. With the class object, `.new` will create an
instance, `.foo` will call the static method `foo`, `[:FOO]` will read the field
`FOO`, and so on. To access instance methods use `.class`, such as
`MyClass.class.getName`.

To import a Java class as a top-level constant, use `Java.import 'name'`.

`Java.synchronized(object) { }` will use the Java object's monitor to
synchronize.

## Embedding in Java

TruffleRuby is embedded via the Polyglot API, which is part of GraalVM. You will
need to use the GraalVM to use this API.

```java
import org.graalvm.polyglot.*;

class Embedding {
    public static void main(String[] args) {
        Context polyglot = Context.newBuilder().allowAllAccess(true).build();
        Value array = polyglot.eval("ruby", "[1,2,42,4]");
        int result = array.getArrayElement(2).asInt();
        System.out.println(result);
    }
}
```

## Using Ruby Objects from Embedding Java

Ruby objects are represented by the `Value` class when embedded in Java.

### Accessing arrays

```java
boolean hasArrayElements()
Value getArrayElement(long index)
void setArrayElement(long index, Object value)
boolean removeArrayElement(long index)
long getArraySize()
```

### Accessing methods in objects

```java
boolean hasMembers()
boolean hasMember(String identifier)
Value getMember(String identifier)
Set<String> getMemberKeys
void putMember(String identifier, Object value
boolean removeMember(String identifier)
```

### Executing procs, lambdas, and methods

```java
boolean canExecute()
Value execute(Object... arguments)
void executeVoid(Object... arguments)
```

### Instantiating classes

```java
boolean canInstantiate() {
Value newInstance(Object... arguments)
```

### Accessing primitives

```java
boolean isString()
String asString()
boolean isBoolean()
boolean asBoolean()
boolean isNumber()
boolean fitsInByte()
byte asByte()
boolean fitsInShort()
short asShort()
boolean fitsInInt()
int asInt()
boolean fitsInLong()
long asLong()
boolean fitsInDouble()
double asDouble()
boolean fitsInFloat()
float asFloat()
boolean isNull()
```

The [JRuby migration guide](jruby-migration.md) includes some more examples.

## Strings

Ruby strings and symbols are converted to Java strings when they are passed to
foreign languages, and Java strings are converted to Ruby strings when they
are passed into Ruby.

## Threading and Interop

Ruby is designed to be a multi-threaded language and much of the ecosystem
expects threads to be available. This may be incompatible with other Truffle
languages which do not support threading, so you can disable the creation of
multiple threads with the option `--single-threaded`. This option is set by
default unless the Ruby launcher is used, as part of the embedded configuration,
described below.

When this option is enabled, the `timeout` module will warn that the timeouts
are being ignored, and signal handlers will warn that a signal has been caught
but will not run the handler, as both of these features would require starting
new threads.

## Embedded configuration

When used outside of the Ruby launcher, such as from another language's launcher
via the polyglot interface, embedded using the native polyglot library, or
embedded in a Java application via the Graal SDK, TruffleRuby will be
automatically configured to work more cooperatively within another application.
This includes options such as not installing an interrupt signal handler, and
using the IO streams from the Graal SDK. It also turns on the single-threaded
mode, as described above.

It will also warn when you explicitly do things that may not work well when
embedded, such as installing your own signal handlers.

This can be turned off even when embedded, with the `embedded` option
(`--ruby.embedded=false` from another launcher, or
`-Dpolyglot.ruby.embedded=false` from a normal Java application).

It's a separate option, but in an embedded configuration you may want to set
`allowNativeAccess(false)` in your `Context.Builder`, or use the experimental
`--platform-native=false` option, to disable use of the NFI for internal
functionality.

Also, the experimental option `--cexts=false` can disable C extensions.

Note that, unlike for example pure JavaScript, Ruby is more than a
self-contained expression language. It has a large core library that includes
low-level IO and system and native-memory routines which may interfere with
other embedded contexts or the host system.
