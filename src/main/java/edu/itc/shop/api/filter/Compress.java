package edu.itc.shop.api.filter;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/** Marks the resource methods whose responses may be gzipped. */
@NameBinding
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface Compress {
}
