package com.codepoetics.mtuples;

import org.junit.Test;

import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class MTupleTest {

    public interface Person {

        static MTuple<Person> build(Consumer<Person> buildWith) {
            return MTupleBuilder.build(Person.class, buildWith);
        }

        void with(String name, int age);

        Extractor<Person, String> name = result -> (name, age) -> result.accept(name);
        Extractor<Person, Integer> age = result -> (name, age) -> result.accept(age);
    }

    @Test
    public void canExtractValues() {
        MTuple<Person> person = Person.build(p -> p.with("Theodor", 41));

        assertThat(person.extract(Person.name), equalTo("Theodor"));
    }

    @Test
    public void equality() {
        MTuple<Person> person1 = Person.build(p -> p.with("Rosa", 39));
        MTuple<Person> person2 = Person.build(p -> p.with("Rosa", 39));
        MTuple<Person> person3 = Person.build(p -> p.with("Walter", 39));

        assertThat(person1, equalTo(person2));
        assertThat(person2, not(equalTo(person3)));
    }

    @Test
    public void hashCodes() {
        MTuple<Person> person1 = Person.build(p -> p.with("Antonio", 67));
        MTuple<Person> person2 = Person.build(p -> p.with("Antonio", 67));
        MTuple<Person> person3 = Person.build(p -> p.with("Victor", 23));

        assertThat(person1.hashCode(), equalTo(person2.hashCode()));
        assertThat(person2.hashCode(), not(equalTo(person3.hashCode())));
    }

    @Test
    public void valuesByParameterName() {
        MTuple<Person> person = Person.build(p -> p.with("Herbert", 23));

        assertThat(person.get("age"), equalTo(23));
    }

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
}
