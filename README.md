MTuples
=======

Very lightweight record types for Java 8.

Maven
=====

```xml
<dependency>
    <groupId>com.codepoetics</groupId>
    <artifactId>mtuples</artifactId>
    <version>0.2</version>
</dependency>
```

FAQ
===

_Knowing you, this is yet another attempt at doing something like Scala's case classes in Java._

That's right. `MTuple`s are immutable collections of values, with a strongly-typed protocol for creating new tuples and reading values out of existing tuples. They have sensible `equals` and `hashCode` and `toString` implementations.

_OK, so what's the trick this time?_

An `MTuple<T>` is an array of `Object[]`, together with a `java.lang.reflection.Method` belonging to the type `T` that can receive that array as its argument list.

To read the values of an `MTuple<T>`, you pass it an instance of `T` and it calls the method on that instance, passing in the `Object[]` array.

Like this (notice that we're supplying a lambda as our implementation of `Person`):

```java
public interface Person {

    static MTuple<Person> build(Consumer<Person> buildWith) {
        return MTupleBuilder.build(Person.class, buildWith);
    }

    void with(String name, int age);
}

public void readValues() {
    MTuple<Person> person = Person.build(p -> p.with("Theodor", 41));

    person.accept((name, age) ->
        System.out.println(name + " is " + age + "years old."))
}
```

_That looks weird._

It is a _bit_ weird. The `build` syntax maybe needs some explaining. `MTupleBuilder` creates a proxy implementing `T`, and passes it to the `Consumer<T>` you pass in. You call a method on the proxy in the `Consumer<T>`, and the method call gets recorded and turned into an `MTuple<T>` which the builder then hands back to you. It's a bit origami-like. But the next bit's worse.

_Go on._

Suppose we wanted to _read_ a single value from the `MTuple<T>`. We _could_ do this:

```java
AtomicReference<String> theName = new AtomicReference<>();
MTuple<Person> person = Person.build(p -> p.with("Theodor", 41));

person.accept((name, age) -> theName.set(name))

String name = theName.get();
```

But who wants to do _that_? There's a better way, which is to define an `Extractor<T, V>` which looks like this:

```java
Extractor<Person, String> theName = result -> (name, age) -> result.accept(name);

String name = person.extract(theName);
```

Because the `with` method on `Person` returns `void`, we can't return a value directly from the lambda we pass in. So instead we pass a `Function<Consumer<V>, T>` to the `extract` method. It gives us a `Consumer<V>` (which we've called `result` here), to which we supply the value we want to return as a result; we give it an implementation of `Person` which it then calls with the `MTuple<Person>`'s arguments. Our lambda has to call `result.accept` to "send" a value back out to the caller.

Under the hood, it's using an `AtomicReference<V>` just like in the previous code snippet.

_That is sick._

Yes, but you get used to it surprisingly quickly.

Anyway, all of this is very nice, but what about polymorphism? Scala's case classes do pattern matching - can we do something like that?

Well...

```java
public interface Message {
    static MTuple<Message> build(Consumer<Message> buildWith) {
        return MTupleBuilder.build(Message.class, buildWith);
    }

    void itemCreated(String id, String name);
    void itemDeleted(String id);

    // Anonymous classes act like pattern matchers...
    Extractor<Message, String> id = result -> new Message() {
        @Override
        public void itemCreated(String id, String name) {
            result.accept(id);
        }

        @Override
        public void itemDeleted(String id) {
            result.accept(id);
        }
    };
}

@Test
public void multipleMethods() {
    MTuple<Message> createdMessage = Message.build(m -> m.itemCreated("123", "Foo"));
    MTuple<Message> deletedMessage = Message.build(m -> m.itemDeleted("123"));

    assertThat(createdMessage.extract(Message.id), equalTo(deletedMessage.extract(Message.id)));
}
```

_I'm not talking to you any more. Go away._

Fine. Go and spend more time with your _beans_...
